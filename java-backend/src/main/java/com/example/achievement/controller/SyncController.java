package com.example.achievement.controller;

import com.example.achievement.config.DingTalkProperties;
import com.example.achievement.entity.SyncLog;
import com.example.achievement.service.DingTalkClient;
import com.example.achievement.service.SyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SyncService syncService;
    private final DingTalkProperties dingTalkProperties;
    private final DingTalkClient dingTalkClient;

    /**
     * 获取所有可同步的 sheet 列表（含当前数据量、最近同步时间）
     */
    @GetMapping("/sheets")
    public ResponseEntity<List<Map<String, Object>>> getSheets() {
        return ResponseEntity.ok(syncService.getSheetList());
    }

    /**
     * 获取钉钉数据仓库中实际 sheet 列表
     */
    @GetMapping("/dingtalk-sheets")
    public ResponseEntity<List<Map<String, String>>> getDingTalkSheets() {
        return ResponseEntity.ok(syncService.getDingTalkSheets());
    }

    /**
     * 同步单个 sheet（覆盖导入）
     */
    @PostMapping("/sheets/{sheetId}/sync")
    public ResponseEntity<Map<String, Object>> syncSheet(@PathVariable String sheetId) {
        if (!dingTalkProperties.isConfigured()) {
            return ResponseEntity.badRequest().body(Map.of("error", "钉钉未配置"));
        }
        return ResponseEntity.ok(syncService.syncSheet(sheetId));
    }

    /**
     * 回滚到某次同步前的状态
     */
    @PostMapping("/rollback/{logId}")
    public ResponseEntity<Map<String, Object>> rollback(@PathVariable String logId) {
        return ResponseEntity.ok(syncService.rollback(logId));
    }

    /**
     * 获取同步日志列表
     */
    @GetMapping("/logs")
    public ResponseEntity<List<SyncLog>> getLogs(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(syncService.getLogs(limit));
    }

    /** 调试：预览 sheet 字段 */
    @GetMapping("/preview/{sheetId}")
    public ResponseEntity<Map<String, Object>> previewSheet(
            @PathVariable String sheetId,
            @RequestParam(defaultValue = "dw") String base) {
        String baseId = "dw".equals(base) ? dingTalkProperties.getDataWarehouseBaseId() : dingTalkProperties.getBaseId();
        List<Map<String, String>> rows = dingTalkClient.listRecordsWithFieldNames(sheetId, baseId);
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("count", rows.size());
        if (!rows.isEmpty()) {
            result.put("sampleFields", new ArrayList<>(rows.get(0).keySet()));
            result.put("sampleRow", rows.get(0));
        }
        return ResponseEntity.ok(result);
    }
}
