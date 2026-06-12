package com.example.achievement.service;

import com.example.achievement.config.DingTalkProperties;
import com.example.achievement.entity.SyncLog;
import com.example.achievement.repository.SyncLogRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncService {

    private final JdbcTemplate jdbcTemplate;
    private final SyncLogRepository syncLogRepository;
    private final DingTalkClient dingTalkClient;
    private final DingTalkProperties props;
    private final DingTalkDataService dingTalkDataService;
    private final DingTalkSyncService dingTalkSyncService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Sheet ID -> 显示名称映射
    public static final Map<String, String> DATA_WAREHOUSE_SHEETS = new LinkedHashMap<>();
    static {
        DATA_WAREHOUSE_SHEETS.put("O0DHU7u", "签约合同");
        DATA_WAREHOUSE_SHEETS.put("LfxeQF7", "签约派单");
        DATA_WAREHOUSE_SHEETS.put("BRNEkOg", "确权明细");
        DATA_WAREHOUSE_SHEETS.put("IjclEN7", "研发成果");
        DATA_WAREHOUSE_SHEETS.put("SXS7oOg", "产品信息");
        DATA_WAREHOUSE_SHEETS.put("Jwe8QNe", "产品套餐");
        DATA_WAREHOUSE_SHEETS.put("drgfZh4", "部门预算");
        DATA_WAREHOUSE_SHEETS.put("ZwUu7oL", "项目订单成本(旧-DW)");
    }

    /** 主 Base 中的 sheet */
    public static final Map<String, String> MAIN_BASE_SHEETS = new LinkedHashMap<>();
    static {
        MAIN_BASE_SHEETS.put("rVbHRpZ", "项目订单成本");
    }

    // Table names mapping
    public static final Map<String, String> SHEET_TO_TABLE = new LinkedHashMap<>();
    static {
        SHEET_TO_TABLE.put("O0DHU7u", "dt_signing_contracts");
        SHEET_TO_TABLE.put("LfxeQF7", "dt_signing_orders");
        SHEET_TO_TABLE.put("BRNEkOg", "dt_revenue_details");
        SHEET_TO_TABLE.put("IjclEN7", "dt_achievements");
        SHEET_TO_TABLE.put("SXS7oOg", "dt_products");
        SHEET_TO_TABLE.put("Jwe8QNe", "dt_product_packages");
        SHEET_TO_TABLE.put("drgfZh4", "dt_department_budgets");
        SHEET_TO_TABLE.put("ZwUu7oL", "dt_project_order_costs");
        SHEET_TO_TABLE.put("rVbHRpZ", "dt_project_order_costs");
    }

    /**
     * 获取所有可同步的 sheet 列表（含状态）
     */
    public List<Map<String, Object>> getSheetList() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, String> entry : DATA_WAREHOUSE_SHEETS.entrySet()) {
            list.add(buildSheetItem(entry.getKey(), entry.getValue()));
        }
        for (Map.Entry<String, String> entry : MAIN_BASE_SHEETS.entrySet()) {
            list.add(buildSheetItem(entry.getKey(), entry.getValue()));
        }
        return list;
    }

    private Map<String, Object> buildSheetItem(String sheetId, String sheetName) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("sheetId", sheetId);
        item.put("sheetName", sheetName);
        String tableName = SHEET_TO_TABLE.get(sheetId);
        item.put("tableName", tableName);
        Integer count = 0;
        try {
            count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
        } catch (Exception ignored) {}
        item.put("recordCount", count);
        List<SyncLog> recentLogs = syncLogRepository.findBySheetIdAndStatusOrderByCreatedAtDesc(sheetId, "SUCCESS");
        if (!recentLogs.isEmpty()) {
            SyncLog lastLog = recentLogs.get(0);
            item.put("lastSyncTime", lastLog.getCreatedAt() != null ? lastLog.getCreatedAt().toString() : null);
            item.put("lastSyncCount", lastLog.getRecordCountAfter());
        }
        return item;
    }

    /**
     * 同步单个 sheet（覆盖导入）
     */
    public Map<String, Object> syncSheet(String sheetId) {
        String tableName = SHEET_TO_TABLE.get(sheetId);
        String sheetName = DATA_WAREHOUSE_SHEETS.getOrDefault(sheetId, sheetId);
        if (tableName == null) {
            return Map.of("error", "未知 sheetId: " + sheetId);
        }

        SyncLog syncLog = new SyncLog();
        syncLog.setId(UUID.randomUUID().toString());
        syncLog.setSheetId(sheetId);
        syncLog.setSheetName(sheetName);
        syncLog.setTableName(tableName);
        syncLog.setOperation("SYNC");
        syncLog.setCreatedAt(LocalDateTime.now());

        try {
            // 1. 保存快照（同步前的数据）
            int beforeCount = saveSnapshot(syncLog, tableName);
            syncLog.setRecordCountBefore(beforeCount);

            // 2. 从钉钉读取数据
            boolean isMainBase = MAIN_BASE_SHEETS.containsKey(sheetId);
            String baseId = isMainBase ? props.getBaseId() : props.getDataWarehouseBaseId();
            List<Map<String, String>> dataRows = dingTalkClient.listRecordsWithFieldNames(sheetId, baseId);
            if (dataRows == null || dataRows.isEmpty()) {
                syncLog.setStatus("FAILED");
                syncLog.setErrorMessage("钉钉返回空数据");
                syncLogRepository.save(syncLog);
                return Map.of("error", "钉钉返回空数据", "logId", syncLog.getId());
            }

            // 3. 执行覆盖同步
            int count;
            if (isMainBase) {
                count = dingTalkDataService.syncMainBaseSheetData(sheetId, dataRows);
            } else {
                count = dingTalkDataService.syncSheetDataPublic(sheetId, dataRows);
            }
            syncLog.setRecordCountAfter(count);
            syncLog.setStatus("SUCCESS");
            syncLogRepository.save(syncLog);

            log.info("同步完成: sheet={}, 前={}, 后={}", sheetName, beforeCount, count);
            return Map.of(
                    "success", true,
                    "sheetName", sheetName,
                    "recordCountBefore", beforeCount,
                    "recordCountAfter", count,
                    "logId", syncLog.getId()
            );
        } catch (Exception e) {
            log.error("同步失败: sheet={}, error={}", sheetName, e.getMessage(), e);
            syncLog.setStatus("FAILED");
            syncLog.setErrorMessage(e.getMessage());
            syncLogRepository.save(syncLog);
            return Map.of("error", e.getMessage(), "logId", syncLog.getId());
        }
    }

    /**
     * 回滚到某次同步前的状态
     */
    public Map<String, Object> rollback(String logId) {
        SyncLog syncLog = syncLogRepository.findById(logId).orElse(null);
        if (syncLog == null) {
            return Map.of("error", "日志不存在: " + logId);
        }
        if (!"SUCCESS".equals(syncLog.getStatus())) {
            return Map.of("error", "只能回滚成功的同步操作，当前状态: " + syncLog.getStatus());
        }
        if (syncLog.getSnapshot() == null || syncLog.getSnapshot().isEmpty()) {
            return Map.of("error", "该同步记录没有保存快照，无法回滚");
        }

        String tableName = syncLog.getTableName();
        try {
            // 解析快照
            List<Map<String, Object>> snapshotRows = objectMapper.readValue(
                    syncLog.getSnapshot(), new TypeReference<List<Map<String, Object>>>() {});

            // 记录回滚日志
            SyncLog rollbackLog = new SyncLog();
            rollbackLog.setId(UUID.randomUUID().toString());
            rollbackLog.setSheetId(syncLog.getSheetId());
            rollbackLog.setSheetName(syncLog.getSheetName());
            rollbackLog.setTableName(tableName);
            rollbackLog.setOperation("ROLLBACK");
            rollbackLog.setCreatedAt(LocalDateTime.now());

            // 保存回滚前的快照
            int currentCount = saveSnapshot(rollbackLog, tableName);
            rollbackLog.setRecordCountBefore(currentCount);

            // 清空目标表
            jdbcTemplate.execute("DELETE FROM " + tableName);

            // 恢复快照数据
            if (!snapshotRows.isEmpty()) {
                restoreFromSnapshot(tableName, snapshotRows);
            }
            rollbackLog.setRecordCountAfter(snapshotRows.size());
            rollbackLog.setStatus("SUCCESS");
            syncLogRepository.save(rollbackLog);

            // 标记原日志为已回滚
            syncLog.setStatus("ROLLED_BACK");
            syncLogRepository.save(syncLog);

            log.info("回滚完成: table={}, 前={}, 后={}", tableName, currentCount, snapshotRows.size());
            return Map.of(
                    "success", true,
                    "tableName", tableName,
                    "recordCountBefore", currentCount,
                    "recordCountAfter", snapshotRows.size(),
                    "rollbackLogId", rollbackLog.getId()
            );
        } catch (Exception e) {
            log.error("回滚失败: table={}, error={}", tableName, e.getMessage(), e);
            return Map.of("error", "回滚失败: " + e.getMessage());
        }
    }

    /**
     * 获取同步日志列表
     */
    public List<SyncLog> getLogs(int limit) {
        List<SyncLog> logs = syncLogRepository.findAllByOrderByCreatedAtDesc();
        return logs.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * 获取钉钉数据仓库中所有 sheet 的实际名称
     */
    public List<Map<String, String>> getDingTalkSheets() {
        return dingTalkClient.listSheets(props.getDataWarehouseBaseId());
    }

    // ===== 内部方法 =====

    private int saveSnapshot(SyncLog syncLog, String tableName) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM " + tableName);
            String json = objectMapper.writeValueAsString(rows);
            syncLog.setSnapshot(json);
            return rows.size();
        } catch (Exception e) {
            log.warn("保存快照失败: {}", e.getMessage());
            syncLog.setSnapshot("[]");
            return 0;
        }
    }

    private void restoreFromSnapshot(String tableName, List<Map<String, Object>> rows) {
        if (rows.isEmpty()) return;

        // 获取列名（从第一行）
        Map<String, Object> firstRow = rows.get(0);
        String columns = String.join(", ", firstRow.keySet());
        String placeholders = firstRow.keySet().stream().map(k -> "?").collect(Collectors.joining(", "));
        String sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ")";

        for (Map<String, Object> row : rows) {
            Object[] values = row.values().toArray();
            jdbcTemplate.update(sql, values);
        }
    }
}
