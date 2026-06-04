package com.example.achievement.service;

import com.example.achievement.entity.Achievement;
import com.example.achievement.entity.AchievementVersionRecord;
import com.example.achievement.enums.AchievementStatus;
import com.example.achievement.repository.AchievementRepository;
import com.example.achievement.repository.AchievementVersionRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DingTalkAchievementSyncService {

    private final DingTalkClient dingTalkClient;
    private final AchievementRepository achievementRepository;
    private final AchievementVersionRecordRepository versionRecordRepository;

    /**
     * Sync achievements from DingTalk sheet CiOtnAu to local achievements table.
     * Dedup by name + productId.
     * Handles "已变更" (CHANGED) status: find existing achievement by name+productId,
     * diff fields, update existing record, and save version history.
     */
    public Map<String, Object> syncFromDingTalk() {
        Map<String, Object> result = new LinkedHashMap<>();
        log.info("开始从钉钉同步成果数据...");

        // 1. Fetch records with Chinese field names already mapped
        List<Map<String, String>> rows = dingTalkClient.listRecordsWithFieldNames("CiOtnAu");
        if (rows == null || rows.isEmpty()) {
            result.put("imported", 0);
            result.put("skipped", 0);
            result.put("updated", 0);
            result.put("total", 0);
            return result;
        }
        log.info("钉钉返回 {} 条成果记录", rows.size());

        int imported = 0;
        int skipped = 0;
        int updated = 0;

        // Unified upsert loop: every DingTalk record is the source of truth
        for (Map<String, String> row : rows) {
            try {
                String name = row.getOrDefault("成果名称", "").trim();
                String productName = row.getOrDefault("关联产品", "").trim();
                String productId = extractProductId(productName);

                if (name.isEmpty()) {
                    skipped++;
                    continue;
                }

                String statusStr = row.getOrDefault("成果状态", "预注册");
                AchievementStatus dtStatus = mapStatus(statusStr);
                boolean isChanged = (dtStatus == null); // "已变更" record
                AchievementStatus targetStatus = isChanged ? AchievementStatus.PRE_REGISTER : dtStatus;

                // Only compare status if the DingTalk field has a meaningful value
                String rawStatusStr = row.get("成果状态");
                boolean hasDtStatus = rawStatusStr != null && !rawStatusStr.trim().isEmpty();

                // Skip DELETED records entirely
                if (targetStatus == AchievementStatus.DELETED) {
                    skipped++;
                    continue;
                }

                // Find existing by name + productId
                List<Achievement> existing = achievementRepository.findByNameAndProductId(name, productId);
                if (existing != null && !existing.isEmpty()) {
                    // Exists: full field diff + update (including status)
                    Achievement exist = existing.get(0);
                    log.debug("去重匹配: name=\"{}\", productId=\"{}\", found={}, diff+update", name, productId, existing.size());
                    boolean changed = applyChange(exist, row, productId, productName, targetStatus, isChanged, hasDtStatus);
                    if (changed) {
                        updated++;
                        log.info("成果更新成功: name=\"{}\", productId=\"{}\"", name, productId);
                    } else {
                        log.debug("成果无差异: name=\"{}\", productId=\"{}\", skip", name, productId);
                        skipped++;
                    }
                } else {
                    // Not found: import as new
                    log.info("导入新记录: name=\"{}\", productId=\"{}\", dtStatusStr=\"{}\", targetStatus={}", name, productId, statusStr, targetStatus);
                    Achievement a = mapToAchievement(row, name, productId, productName);
                    a.setStatus(targetStatus);
                    achievementRepository.save(a);
                    imported++;
                }
            } catch (Exception e) {
                log.warn("处理记录跳过: {}", e.getMessage());
                skipped++;
            }
        }

        result.put("imported", imported);
        result.put("skipped", skipped);
        result.put("updated", updated);
        result.put("total", rows.size());
        log.info("成果同步完成: 导入 {} 条, 更新 {} 条, 跳过 {} 条, 总计 {} 条", imported, updated, skipped, rows.size());
        return result;
    }

    /**
     * Compare DingTalk source data with local achievements.
     * Returns: onlyInDingTalk, onlyInLocal, fieldDiff lists.
     */
    public Map<String, Object> diffWithDingTalk() {
        Map<String, Object> result = new LinkedHashMap<>();
        log.info("开始对比钉钉与本地数据...");

        List<Map<String, String>> rows = dingTalkClient.listRecordsWithFieldNames("CiOtnAu");
        if (rows == null || rows.isEmpty()) {
            result.put("onlyInDingTalk", Collections.emptyList());
            result.put("onlyInLocal", Collections.emptyList());
            result.put("fieldDiff", Collections.emptyList());
            return result;
        }

        // Build DingTalk key set (name+productId), exclude "已变更" records
        Set<String> dtKeys = new HashSet<>();
        Map<String, Map<String, String>> dtNormalRows = new LinkedHashMap<>();
        for (Map<String, String> row : rows) {
            String statusStr = row.getOrDefault("成果状态", "");
            if (statusStr.contains("已变更") || statusStr.contains("CHANGED")) continue;
            String name = row.getOrDefault("成果名称", "").trim();
            String productId = extractProductId(row.getOrDefault("关联产品", "").trim());
            if (name.isEmpty()) continue;
            String key = name + "||" + productId;
            dtKeys.add(key);
            dtNormalRows.put(key, row);
        }

        // Build local key set
        List<Achievement> localAll = achievementRepository.findAll();
        Set<String> localKeys = new HashSet<>();
        Map<String, Achievement> localMap = new LinkedHashMap<>();
        for (Achievement a : localAll) {
            if (a.getStatus() == AchievementStatus.DELETED) continue;
            String key = a.getName() + "||" + (a.getProductId() != null ? a.getProductId() : "");
            localKeys.add(key);
            localMap.put(key, a);
        }

        // onlyInDingTalk: keys in DT but not local
        List<Map<String, String>> onlyInDingTalk = new ArrayList<>();
        for (String key : dtKeys) {
            if (!localKeys.contains(key)) {
                Map<String, String> row = dtNormalRows.get(key);
                Map<String, String> info = new LinkedHashMap<>();
                info.put("name", row.get("成果名称"));
                info.put("productId", extractProductId(row.getOrDefault("关联产品", "")));
                info.put("status", row.getOrDefault("成果状态", ""));
                onlyInDingTalk.add(info);
            }
        }

        // onlyInLocal: keys in local but not DT
        List<Map<String, String>> onlyInLocal = new ArrayList<>();
        for (String key : localKeys) {
            if (!dtKeys.contains(key)) {
                Achievement a = localMap.get(key);
                Map<String, String> info = new LinkedHashMap<>();
                info.put("id", a.getId());
                info.put("name", a.getName());
                info.put("productId", a.getProductId());
                info.put("status", a.getStatus().name());
                onlyInLocal.add(info);
            }
        }

        result.put("onlyInDingTalk", onlyInDingTalk);
        result.put("onlyInLocal", onlyInLocal);
        result.put("onlyInDingTalkCount", onlyInDingTalk.size());
        result.put("onlyInLocalCount", onlyInLocal.size());
        result.put("dtNormalCount", dtKeys.size());
        result.put("localCount", localKeys.size());

        log.info("对比完成: 钉钉正常{}条, 本地{}条, 钉钉独有{}条, 本地独有{}条",
                dtKeys.size(), localKeys.size(), onlyInDingTalk.size(), onlyInLocal.size());
        return result;
    }

    private Achievement mapToAchievement(Map<String, String> row, String name, String productId, String productName) {
        Achievement a = new Achievement();
        a.setId(UUID.randomUUID().toString());
        a.setName(name);
        a.setProductId(productId);
        a.setProductName(productName);
        a.setOrganizationName(row.getOrDefault("所属机构", ""));
        a.setDepartmentName(row.getOrDefault("部门", ""));
        a.setVersion(row.getOrDefault("成果版本", ""));
        a.setHasBaseline(row.getOrDefault("是否有基线", "无基线"));
        a.setApplicationScenario(row.getOrDefault("应用场景", ""));
        a.setRequirementProposer(row.getOrDefault("成果需求提出人", ""));
        a.setDescription(row.getOrDefault("成果目标描述", ""));
        a.setAchievementForm(row.getOrDefault("成果形态", ""));
        a.setFunctionListFile(row.getOrDefault("成果对应功能清单", ""));
        a.setAcceptor(row.getOrDefault("验收人（可多人）", ""));
        a.setAcceptanceMethod(row.getOrDefault("成果验收方式", ""));
        a.setPlannedAcceptanceDate(parseDate(row.get("计划验收日期")));
        a.setActualAcceptanceDate(parseDate(row.get("实际验收日期")));
        a.setDemoUrl(row.getOrDefault("DEMO地址", ""));
        a.setDeliverables(row.getOrDefault("成果验收提交物", ""));
        a.setCodeRepositoryUrl(row.getOrDefault("代码仓库/在线文档地址", ""));
        a.setSaleType(row.getOrDefault("成果可售类型", ""));
        a.setChangeReason(row.getOrDefault("异常变更原因（当期未验收的措施）", ""));
        a.setRelatedProjectName(row.getOrDefault("关联项目", ""));
        a.setRelatedOrderName(row.getOrDefault("关联订单", ""));
        a.setRelatedOrderId(row.getOrDefault("关联订单编号", ""));
        a.setPackageIds(row.getOrDefault("关联套餐", ""));
        a.setEstimatedAcceptanceMonth(row.getOrDefault("预估验收年月", ""));
        a.setCreatedBy(row.getOrDefault("创建人", "system"));
        a.setUpdatedBy(row.getOrDefault("更新人", "system"));

        // Status mapping
        String statusStr = row.getOrDefault("成果状态", "预注册");
        AchievementStatus status = mapStatus(statusStr);
        if (status == null) status = AchievementStatus.PRE_REGISTER;
        a.setStatus(status);
        LocalDateTime now = LocalDateTime.now();
        switch (status) {
            case PRE_REGISTER:
                a.setPreRegisterTime(now);
                break;
            case REGISTER:
                a.setRegisterTime(now);
                break;
            case RECORDED:
                a.setRecordTime(now);
                break;
        }

        // Last update time from DingTalk
        String lastUpdate = row.get("最后更新时间");
        if (lastUpdate != null && !lastUpdate.isEmpty()) {
            try {
                long ms = Long.parseLong(lastUpdate.trim());
                a.setUpdatedAt(LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault()));
            } catch (NumberFormatException ignored) {}
        }
        if (a.getUpdatedAt() == null) a.setUpdatedAt(now);
        if (a.getCreatedAt() == null) a.setCreatedAt(now);

        return a;
    }

    /**
     * Map DingTalk status string to AchievementStatus enum.
     * Returns null for "已变更" / "CHANGED" entries, which require special handling.
     */
    private AchievementStatus mapStatus(String statusStr) {
        if (statusStr == null) return AchievementStatus.PRE_REGISTER;
        String s = statusStr.trim();
        if (s.contains("已变更") || s.contains("CHANGED")) return null; // null = changed, not a regular status
        if (s.contains("预注册")) return AchievementStatus.PRE_REGISTER;
        if (s.contains("已登记") || s.contains("登记") || s.contains("RECORDED")) return AchievementStatus.RECORDED;
        if (s.contains("已注册") || s.contains("注册") || s.contains("REGISTERED") || s.contains("REGISTER")) return AchievementStatus.REGISTER;
        if (s.contains("已下架") || s.contains("下架") || s.contains("OFFLINE")) return AchievementStatus.OFFLINE;
        if (s.contains("已删除") || s.contains("删除") || s.contains("DELETED")) return AchievementStatus.DELETED;
        return AchievementStatus.PRE_REGISTER;
    }

    /**
     * Apply field changes from a DingTalk row to an existing Achievement.
     * Compares each field (including status), updates only changed fields.
     * For "已变更" records, increments changeVersion and saves a version record.
     *
     * @param targetStatus the status from DingTalk (PRE_REGISTER for "已变更" records)
     * @param isChanged    true if this is a "已变更" record (generates version record)
     * @return true if any changes were applied, false if no actual changes
     */
    private boolean applyChange(Achievement exist, Map<String, String> row, String productId,
                                 String productName, AchievementStatus targetStatus, boolean isChanged,
                                 boolean hasDtStatus) {
        List<String> changedFields = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Compare status (only if DingTalk has a meaningful status value)
         if (hasDtStatus && exist.getStatus() != targetStatus) {
            exist.setStatus(targetStatus);
            changedFields.add("status");
            // Set corresponding timestamp
            switch (targetStatus) {
                case PRE_REGISTER:
                    if (exist.getPreRegisterTime() == null) exist.setPreRegisterTime(now);
                    break;
                case REGISTER:
                    if (exist.getRegisterTime() == null) exist.setRegisterTime(now);
                    break;
                case RECORDED:
                    if (exist.getRecordTime() == null) exist.setRecordTime(now);
                    break;
            }
        }

        // Compare each field from DingTalk row with existing Achievement
        compareAndSet(exist::getOrganizationName, exist::setOrganizationName, row.get("所属机构"), "organizationName", changedFields);
        compareAndSet(exist::getDepartmentName, exist::setDepartmentName, row.get("部门"), "departmentName", changedFields);
        compareAndSet(exist::getVersion, exist::setVersion, row.get("成果版本"), "version", changedFields);
        compareAndSet(exist::getHasBaseline, exist::setHasBaseline, row.get("是否有基线"), "hasBaseline", changedFields);
        compareAndSet(exist::getApplicationScenario, exist::setApplicationScenario, row.get("应用场景"), "applicationScenario", changedFields);
        compareAndSet(exist::getRequirementProposer, exist::setRequirementProposer, row.get("成果需求提出人"), "requirementProposer", changedFields);
        compareAndSet(exist::getDescription, exist::setDescription, row.get("成果目标描述"), "description", changedFields);
        compareAndSet(exist::getAchievementForm, exist::setAchievementForm, row.get("成果形态"), "achievementForm", changedFields);
        compareAndSet(exist::getFunctionListFile, exist::setFunctionListFile, row.get("成果对应功能清单"), "functionListFile", changedFields);
        compareAndSet(exist::getAcceptor, exist::setAcceptor, row.get("验收人（可多人）"), "acceptor", changedFields);
        compareAndSet(exist::getAcceptanceMethod, exist::setAcceptanceMethod, row.get("成果验收方式"), "acceptanceMethod", changedFields);
        compareDateAndSet(exist::getPlannedAcceptanceDate, exist::setPlannedAcceptanceDate, row.get("计划验收日期"), "plannedAcceptanceDate", changedFields);
        compareDateAndSet(exist::getActualAcceptanceDate, exist::setActualAcceptanceDate, row.get("实际验收日期"), "actualAcceptanceDate", changedFields);
        compareAndSet(exist::getDemoUrl, exist::setDemoUrl, row.get("DEMO地址"), "demoUrl", changedFields);
        compareAndSet(exist::getDeliverables, exist::setDeliverables, row.get("成果验收提交物"), "deliverables", changedFields);
        compareAndSet(exist::getCodeRepositoryUrl, exist::setCodeRepositoryUrl, row.get("代码仓库/在线文档地址"), "codeRepositoryUrl", changedFields);
        compareAndSet(exist::getSaleType, exist::setSaleType, row.get("成果可售类型"), "saleType", changedFields);
        compareAndSet(exist::getChangeReason, exist::setChangeReason, row.get("异常变更原因（当期未验收的措施）"), "changeReason", changedFields);
        // Compare productName too
        if (!eq(productName, exist.getProductName())) {
            exist.setProductName(productName);
            changedFields.add("productName");
        }
        compareAndSet(exist::getRelatedProjectName, exist::setRelatedProjectName, row.get("关联项目"), "relatedProjectName", changedFields);
        compareAndSet(exist::getRelatedOrderName, exist::setRelatedOrderName, row.get("关联订单"), "relatedOrderName", changedFields);
        compareAndSet(exist::getRelatedOrderId, exist::setRelatedOrderId, row.get("关联订单编号"), "relatedOrderId", changedFields);
        compareAndSet(exist::getPackageIds, exist::setPackageIds, row.get("关联套餐"), "packageIds", changedFields);
        compareAndSet(exist::getEstimatedAcceptanceMonth, exist::setEstimatedAcceptanceMonth, row.get("预估验收年月"), "estimatedAcceptanceMonth", changedFields);
        compareAndSet(exist::getCreatedBy, exist::setCreatedBy, row.get("创建人"), "createdBy", changedFields);
        compareAndSet(exist::getUpdatedBy, exist::setUpdatedBy, row.get("更新人"), "updatedBy", changedFields);

        if (changedFields.isEmpty()) {
            log.debug("记录 {} 无实际字段变化，跳过", exist.getName());
            return false;
        }

        exist.setUpdatedAt(LocalDateTime.now());
        achievementRepository.save(exist);

        // Only generate version record for "已变更" records
        if (isChanged) {
            String oldVersion = exist.getChangeVersion();
            String newVersion = incrementChangeVersion(oldVersion);
            exist.setChangeVersion(newVersion);
            achievementRepository.save(exist);

            AchievementVersionRecord versionRecord = new AchievementVersionRecord();
            versionRecord.setId(UUID.randomUUID().toString());
            versionRecord.setAchievementId(exist.getId());
            versionRecord.setAchievementName(exist.getName());
            versionRecord.setProductExternalVersion(exist.getProductExternalVersion() != null ? exist.getProductExternalVersion() : "");
            versionRecord.setFromVersion(oldVersion != null ? oldVersion : "");
            versionRecord.setToVersion(newVersion);
            versionRecord.setChangedFields(String.join(", ", changedFields));
            versionRecord.setChangeDescription("钉钉同步: " + String.join(", ", changedFields));
            versionRecord.setOperator("dingtalk-sync");
            versionRecord.setChangeTime(LocalDateTime.now());
            versionRecordRepository.save(versionRecord);
            log.info("已变更成果 {} 更新成功: {} -> {}, fields={}", exist.getName(), oldVersion, newVersion, changedFields);
        } else {
            log.info("成果 {} 字段更新: fields={}", exist.getName(), changedFields);
        }
        return true;
    }

    // ---- Functional interfaces for field comparison ----

    @FunctionalInterface
    private interface Getter<T> {
        T get();
    }

    @FunctionalInterface
    private interface Setter<T> {
        void set(T val);
    }

    // ---- Field comparison helpers ----

    /**
     * Compare a String field from DingTalk row with the existing value in the entity.
     * If different, update the entity field and record the change.
     */
    private <T> void compareAndSet(Getter<T> getter, Setter<T> setter, String newVal, String fieldName, List<String> changedFields) {
        T oldVal = getter.get();
        String normalizedNew = (newVal == null || newVal.trim().isEmpty()) ? null : newVal.trim();
        String normalizedOld;
        if (oldVal == null) {
            normalizedOld = null;
        } else if (oldVal instanceof String) {
            String s = (String) oldVal;
            normalizedOld = s.trim().isEmpty() ? null : s;
        } else {
            normalizedOld = String.valueOf(oldVal);
        }

        if (!eq(normalizedNew, normalizedOld)) {
            @SuppressWarnings("unchecked")
            T casted = (T) normalizedNew;
            setter.set(casted);
            changedFields.add(fieldName);
        }
    }

    /**
     * Compare a LocalDate field from DingTalk row with the existing value in the entity.
     * Parse the DingTalk value as epoch millis, then compare.
     */
    private void compareDateAndSet(Getter<LocalDate> getter, Setter<LocalDate> setter, String newVal, String fieldName, List<String> changedFields) {
        LocalDate oldVal = getter.get();
        LocalDate newDate = parseDate(newVal);
        if (!eq(newDate, oldVal)) {
            setter.set(newDate);
            changedFields.add(fieldName);
        }
    }

    /**
     * Null-safe equality check. Treats null and empty string as equal.
     */
    private boolean eq(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    /**
     * Increment the change version string.
     * Pattern: "Vx.y.z" -> increment z. If no dots, append ".1".
     * null or empty -> "V1.0.1".
     */
    private String incrementChangeVersion(String ver) {
        if (ver == null || ver.isEmpty()) return "V1.0.1";
        int lastDot = ver.lastIndexOf('.');
        if (lastDot > 0) {
            try {
                int patch = Integer.parseInt(ver.substring(lastDot + 1));
                return ver.substring(0, lastDot + 1) + (patch + 1);
            } catch (NumberFormatException e) {
                return ver + ".1";
            }
        }
        return ver + ".1";
    }

    private String extractProductId(String productSelectValue) {
        // productSelectValue from DingTalkClient is already the extracted name
        // We need to look up productId from productName in the products table
        // But productId field in Achievement is a free-form text field, just use the name
        if (productSelectValue == null || productSelectValue.isEmpty()) return "";
        return productSelectValue;
    }

    private LocalDate parseDate(String val) {
        if (val == null || val.trim().isEmpty()) return null;
        try {
            long ms = Long.parseLong(val.trim());
            return Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate();
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
