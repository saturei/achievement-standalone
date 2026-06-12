package com.example.achievement.service;

import com.example.achievement.entity.*;
import com.example.achievement.repository.*;
import com.example.achievement.config.DingTalkProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DingTalkDataService {

    private final DtSigningContractRepository signingContractRepository;
    private final DtSigningOrderRepository signingOrderRepository;
    private final DtRevenueDetailRepository revenueDetailRepository;
    private final DtAchievementRepository achievementRepository;
    private final DtProductRepository productRepository;
    private final DtProductPackageRepository productPackageRepository;
    private final DtDepartmentBudgetRepository departmentBudgetRepository;
    private final DtProjectOrderCostRepository projectOrderCostRepository;
    private final TargetRepository targetRepository;
    private final JdbcTemplate jdbcTemplate;
    private final DingTalkClient client;
    private final DingTalkProperties props;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> MANAGED_SHEET_IDS = new LinkedHashSet<>(Arrays.asList(
            "O0DHU7u", "LfxeQF7", "BRNEkOg", "IjclEN7", "SXS7oOg", "Jwe8QNe", "drgfZh4"
    ));

    /** 主 Base 中的 sheet（使用 baseId 而非 dataWarehouseBaseId） */
    private static final Set<String> MAIN_BASE_SHEET_IDS = new LinkedHashSet<>(Arrays.asList(
            "rVbHRpZ"
    ));

    /** 当前年份（用于没有明确年份字段的聚合） */
    private static final int DEFAULT_YEAR = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);

    // ======================== 公开方法 ========================

    public Map<String, Object> syncAll() {
        Map<String, Object> result = new LinkedHashMap<>();
        log.info("开始同步所有钉钉数据表... (API调用计数: {})", client.getCallCount());

        if (client.isNearLimit()) {
            int remaining = client.remainingCalls();
            result.put("warning", String.format("API调用次数接近上限(剩余%d次), 请减少同步频率", remaining));
            log.warn("钉钉API接近上限，剩余 {} 次", remaining);
        }

        // 保存损益表原始数据给后续聚合使用
        List<Map<String, String>> plDataRows = null;

        for (String sheetId : MANAGED_SHEET_IDS) {
            try {
                log.info("同步表格 sheetId={}", sheetId);
                List<Map<String, String>> dataRows = client.listRecordsWithFieldNames(sheetId, props.getDataWarehouseBaseId());
                if (dataRows == null || dataRows.isEmpty()) {
                    log.warn("表格 {} 数据为空，跳过同步", sheetId);
                    result.put(sheetId + "_count", 0);
                    result.put(sheetId + "_status", "empty_skipped");
                    continue;
                }

                // 保留损益表原始数据
                if ("drgfZh4".equals(sheetId)) {
                    plDataRows = dataRows;
                }

                int count = syncSheetData(sheetId, dataRows);
                result.put(sheetId + "_count", count);
                result.put(sheetId + "_status", "ok");
            } catch (Exception e) {
                log.error("同步表格 {} 失败: {}", sheetId, e.getMessage(), e);
                result.put(sheetId + "_count", 0);
                result.put(sheetId + "_status", "error: " + e.getMessage());
            }
        }

        // 同步主 Base 的 sheet
        for (String sheetId : MAIN_BASE_SHEET_IDS) {
            try {
                log.info("同步主Base表格 sheetId={}", sheetId);
                List<Map<String, String>> dataRows = client.listRecordsWithFieldNames(sheetId, props.getBaseId());
                if (dataRows == null || dataRows.isEmpty()) {
                    log.warn("表格 {} 数据为空，跳过同步", sheetId);
                    result.put(sheetId + "_count", 0);
                    result.put(sheetId + "_status", "empty_skipped");
                    continue;
                }
                int count = syncMainBaseSheetData(sheetId, dataRows);
                result.put(sheetId + "_count", count);
                result.put(sheetId + "_status", "ok");
            } catch (Exception e) {
                log.error("同步表格 {} 失败: {}", sheetId, e.getMessage(), e);
                result.put(sheetId + "_count", 0);
                result.put(sheetId + "_status", "error: " + e.getMessage());
            }
        }

        // ---- dt_* 表已同步完毕，开始聚合到 targets 表 ----
        try {
            syncSigningTargets();
            result.put("signing_targets", "ok");
        } catch (Exception e) {
            log.error("签约目标聚合失败: {}", e.getMessage(), e);
            result.put("signing_targets", "error: " + e.getMessage());
        }

        try {
            syncAchievementTargets();
            result.put("achievement_targets", "ok");
        } catch (Exception e) {
            log.error("研发成果目标聚合失败: {}", e.getMessage(), e);
            result.put("achievement_targets", "error: " + e.getMessage());
        }

        try {
            syncTargetsFromPL(plDataRows);
            result.put("pl_targets", "ok");
        } catch (Exception e) {
            log.error("损益表目标聚合失败: {}", e.getMessage(), e);
            result.put("pl_targets", "error: " + e.getMessage());
        }

        int totalCount = result.values().stream()
                .filter(v -> v instanceof Integer)
                .mapToInt(v -> (Integer) v)
                .sum();
        result.put("total_count", totalCount);
        result.put("api_calls_used", client.getCallCount());
        result.put("api_calls_remaining", client.remainingCalls());
        log.info("同步完成，总计 {} 条数据, API调用: {}", totalCount, client.getCallCount());
        return result;
    }

    public Map<String, Object> getSyncStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("signing_contract", signingContractRepository.count());
        status.put("signing_order", signingOrderRepository.count());
        status.put("revenue_detail", revenueDetailRepository.count());
        status.put("achievement", achievementRepository.count());
        status.put("product", productRepository.count());
        status.put("product_package", productPackageRepository.count());
        status.put("department_budget", departmentBudgetRepository.count());
        status.put("api_calls_used", client.getCallCount());
        status.put("api_calls_remaining", client.remainingCalls());
        status.put("monthly_limit", DingTalkClient.MONTHLY_LIMIT);
        return status;
    }

    private int syncSheetData(String sheetId, List<Map<String, String>> dataRows) {
        return syncSheetDataPublic(sheetId, dataRows);
    }

    public int syncSheetDataPublic(String sheetId, List<Map<String, String>> dataRows) {
        switch (sheetId) {
            case "O0DHU7u": return syncSigningContracts(dataRows);
            case "LfxeQF7": return syncSigningOrders(dataRows);
            case "BRNEkOg": return syncRevenueDetails(dataRows);
            case "IjclEN7": return syncAchievements(dataRows);
            case "SXS7oOg": return syncProducts(dataRows);
            case "Jwe8QNe": return syncProductPackages(dataRows);
            case "drgfZh4": return syncDepartmentBudgets(dataRows);
            case "ZwUu7oL": return syncProjectOrderCosts(dataRows);
            default:
                log.warn("未知表格ID: {}", sheetId);
                return 0;
        }
    }

    public int syncMainBaseSheetData(String sheetId, List<Map<String, String>> dataRows) {
        switch (sheetId) {
            case "rVbHRpZ": return syncProjectOrderCosts(dataRows);
            default:
                log.warn("未知主Base表格ID: {}", sheetId);
                return 0;
        }
    }

    private int syncSigningContracts(List<Map<String, String>> rows) {
        List<DtSigningContract> entities = new ArrayList<>();
        for (Map<String, String> row : rows) {
            try {
                DtSigningContract e = new DtSigningContract();
                e.setId(UUID.randomUUID().toString());
                e.setContractId(row.getOrDefault("合同编号", ""));
                e.setCustomerName(row.getOrDefault("客户简称", ""));
                e.setContractName(row.getOrDefault("合同名称", ""));
                e.setSigningAmountWan(parseDec(row.get("合同签约金额认定（万元）")));
                e.setAccountingType(row.getOrDefault("合同核算类型", ""));
                e.setSigningRiskLevel(row.getOrDefault("签约风险等级", ""));
                e.setRecognitionRiskLevel(row.getOrDefault("确权风险等级", ""));
                e.setOperator(row.getOrDefault("经营岗", ""));
                e.setDepartment(row.getOrDefault("所属部门", ""));
                e.setSigningQuarter(row.getOrDefault("签约认定季度", ""));
                e.setLinkedPackage(row.getOrDefault("关联套餐", ""));
                e.setLinkedOpportunity(row.getOrDefault("关联商机", ""));
                e.setSigningDate(row.getOrDefault("签约日期", null));
                e.setRawJson(toJson(row));
                entities.add(e);
            } catch (Exception ex) {
                log.warn("映射签约合同跳过: {}", ex.getMessage());
            }
        }
        signingContractRepository.deleteAll();
        signingContractRepository.saveAll(entities);
        log.info("签约合同同步完成: {} 条", entities.size());
        return entities.size();
    }

    private int syncSigningOrders(List<Map<String, String>> rows) {
        List<DtSigningOrder> entities = new ArrayList<>();
        for (Map<String, String> row : rows) {
            try {
                DtSigningOrder e = new DtSigningOrder();
                e.setId(UUID.randomUUID().toString());
                e.setOrderId(row.getOrDefault("系统订单编号", ""));
                e.setContractId(row.getOrDefault("合同编号", ""));
                e.setCustomerName(row.getOrDefault("所属客户", ""));
                e.setOrderAmountWan(parseDec(row.get("派单金额（万元）")));
                e.setPlannedRecognitionAmount(parseDec(row.get("计划确权金额")));
                e.setAccountingType(row.getOrDefault("合同核算类型", ""));
                e.setContractStatus(row.getOrDefault("合同状态", ""));
                e.setOperator(row.getOrDefault("经营岗", ""));
                e.setDepartment(row.getOrDefault("所属部门", ""));
                e.setSigningQuarter(row.getOrDefault("签约认定季度", ""));
                e.setLinkedPackage(row.getOrDefault("关联套餐", ""));
                e.setOrderCreateDate(row.getOrDefault("订单创建日期", null));
                e.setRemark(row.getOrDefault("备注", ""));
                e.setRawJson(toJson(row));
                entities.add(e);
            } catch (Exception ex) {
                log.warn("映射签约派单跳过: {}", ex.getMessage());
            }
        }
        signingOrderRepository.deleteAll();
        signingOrderRepository.saveAll(entities);
        log.info("签约派单同步完成: {} 条", entities.size());
        return entities.size();
    }

    private int syncRevenueDetails(List<Map<String, String>> rows) {
        List<DtRevenueDetail> entities = new ArrayList<>();
        for (Map<String, String> row : rows) {
            try {
                DtRevenueDetail e = new DtRevenueDetail();
                e.setId(UUID.randomUUID().toString());
                e.setOrderId(row.getOrDefault("订单编号", ""));
                e.setContractId(row.getOrDefault("订单所属合同", ""));
                e.setContractName(row.getOrDefault("合同名称", ""));
                e.setCustomerName(row.getOrDefault("所属客户", ""));
                e.setRevenueAmountWan(parseDec(row.get("确权收入（万元）")));
                e.setConfirmedRevenue(parseDec(row.get("确认收入")));
                e.setCostCarryover(parseDec(row.get("结转成本")));
                e.setDeliveryMargin(parseDec(row.get("交付毛利")));
                e.setRecognitionMonth(row.getOrDefault("确权认定时间", ""));
                e.setProfitLossSubject(row.getOrDefault("损益科目", ""));
                e.setRecognitionType(row.getOrDefault("类型", ""));
                e.setAccountingType(row.getOrDefault("核算类型", ""));
                e.setOperator(row.getOrDefault("姓名", ""));
                e.setOrgUnit(row.getOrDefault("经营机构", ""));
                e.setRawJson(toJson(row));
                entities.add(e);
            } catch (Exception ex) {
                log.warn("映射确权明细跳过: {}", ex.getMessage());
            }
        }
        revenueDetailRepository.deleteAll();
        revenueDetailRepository.saveAll(entities);
        log.info("确权明细同步完成: {} 条", entities.size());
        return entities.size();
    }

    private int syncAchievements(List<Map<String, String>> rows) {
        List<DtAchievement> entities = new ArrayList<>();
        for (Map<String, String> row : rows) {
            try {
                DtAchievement e = new DtAchievement();
                e.setId(UUID.randomUUID().toString());
                e.setAchievementName(row.getOrDefault("成果名称", ""));
                e.setLinkedProduct(row.getOrDefault("关联产品", ""));
                e.setLinkedPackages(row.getOrDefault("关联套餐", ""));
                e.setLinkedOrderId(row.getOrDefault("关联订单编号", ""));
                e.setLinkedOrder(row.getOrDefault("关联订单", ""));
                e.setLinkedProject(row.getOrDefault("关联项目", ""));
                e.setAchievementForm(row.getOrDefault("成果形态", ""));
                e.setAchievementVersion(row.getOrDefault("成果版本", ""));
                e.setPlannedAcceptDate(row.getOrDefault("计划验收日期", null));
                e.setActualAcceptDate(row.getOrDefault("实际验收日期", null));
                e.setAcceptMethod(row.getOrDefault("成果验收方式", ""));
                e.setAcceptors(row.getOrDefault("验收人（可多人）", ""));
                e.setDepartment(row.getOrDefault("部门", ""));
                e.setOrgUnit(row.getOrDefault("所属机构", ""));
                e.setHasBaseline(row.getOrDefault("是否有基线", ""));
                e.setAppScenario(row.getOrDefault("应用场景", ""));
                e.setRequester(row.getOrDefault("成果需求提出人", ""));
                e.setGoalDescription(row.getOrDefault("成果目标描述", ""));
                e.setRawJson(toJson(row));
                entities.add(e);
            } catch (Exception ex) {
                log.warn("映射成果跳过: {}", ex.getMessage());
            }
        }
        achievementRepository.deleteAll();
        achievementRepository.saveAll(entities);
        log.info("成果同步完成: {} 条", entities.size());
        return entities.size();
    }

    private int syncProducts(List<Map<String, String>> rows) {
        List<DtProduct> entities = new ArrayList<>();
        for (Map<String, String> row : rows) {
            try {
                DtProduct e = new DtProduct();
                e.setId(UUID.randomUUID().toString());
                e.setProductId(row.getOrDefault("产品ID", ""));
                e.setProductName(row.getOrDefault("产品名称", ""));
                e.setProductDepartment(row.getOrDefault("产品所属部门", ""));
                e.setProductManager(row.getOrDefault("产品经理", ""));
                e.setProductOwner(row.getOrDefault("产品负责人", ""));
                e.setAnnualBudgetWan(parseDec(row.get("产品年度预算（万元）")));
                e.setLinkedProjects(row.getOrDefault("关联研发项目", ""));
                e.setCostAccumulated(parseDec(row.get("累计成本")));
                e.setRawJson(toJson(row));
                entities.add(e);
            } catch (Exception ex) {
                log.warn("映射产品跳过: {}", ex.getMessage());
            }
        }
        productRepository.deleteAll();
        productRepository.saveAll(entities);
        log.info("产品同步完成: {} 条", entities.size());
        return entities.size();
    }

    private int syncProductPackages(List<Map<String, String>> rows) {
        List<DtProductPackage> entities = new ArrayList<>();
        for (Map<String, String> row : rows) {
            try {
                DtProductPackage e = new DtProductPackage();
                e.setId(UUID.randomUUID().toString());
                e.setPackageId(row.getOrDefault("产品套餐编号", ""));
                e.setLinkedProductId(row.getOrDefault("关联产品编号", ""));
                e.setPackageName(row.getOrDefault("产品套餐名称", ""));
                e.setStatus(row.getOrDefault("状态", ""));
                e.setStandardPriceWan(parseDec(row.get("标准客单价（万元）")));
                e.setStandardMarginRate(parseDec(row.get("标准实施毛利率")));
                e.setImplCycle(row.getOrDefault("实施周期（月）", ""));
                e.setImplScale(row.getOrDefault("实施规模（人天）", ""));
                e.setResponsiblePerson(row.getOrDefault("负责人", ""));
                e.setRawJson(toJson(row));
                entities.add(e);
            } catch (Exception ex) {
                log.warn("映射套餐跳过: {}", ex.getMessage());
            }
        }
        productPackageRepository.deleteAll();
        productPackageRepository.saveAll(entities);
        log.info("套餐同步完成: {} 条", entities.size());
        return entities.size();
    }

    private int syncDepartmentBudgets(List<Map<String, String>> rows) {
        List<DtDepartmentBudget> entities = new ArrayList<>();
        for (Map<String, String> row : rows) {
            try {
                DtDepartmentBudget e = new DtDepartmentBudget();
                e.setId(UUID.randomUUID().toString());
                e.setDeptKey(row.getOrDefault("主KEY", ""));
                e.setDeptName(row.getOrDefault("部门名称", ""));
                e.setDeptHead(row.getOrDefault("部门负责人", ""));
                e.setYear(row.getOrDefault("部门年度", ""));
                e.setBudgetTotal(parseDec(row.get("部门预算合计")));
                e.setActualCostTotal(parseDec(row.get("实际成本合计")));
                e.setBudgetLabor(parseDec(row.get("项目预算人工")));
                e.setBudgetTravel(parseDec(row.get("项目预算差旅")));
                e.setBudgetOther(parseDec(row.get("项目预算其它成本")));
                e.setActualLabor(parseDec(row.get("项目实际人工")));
                e.setActualTravel(parseDec(row.get("项目实际差旅")));
                e.setActualOther(parseDec(row.get("项目实际其它成本")));
                e.setParentDept(row.getOrDefault("钉钉架构上所属部门", ""));
                e.setStatus(row.getOrDefault("状态", ""));
                e.setRawJson(toJson(row));
                entities.add(e);
            } catch (Exception ex) {
                log.warn("映射部门预算跳过: {}", ex.getMessage());
            }
        }
        departmentBudgetRepository.deleteAll();
        departmentBudgetRepository.saveAll(entities);
        log.info("部门预算同步完成: {} 条", entities.size());
        return entities.size();
    }

    private int syncProjectOrderCosts(List<Map<String, String>> rows) {
        String now = java.time.LocalDateTime.now().toString().substring(0, 19);
        List<String> dingTalkOrderIds = new ArrayList<>();
        int inserted = 0;
        int updated = 0;

        // 1. 收集钉钉订单ID，更新或插入记录
        for (Map<String, String> row : rows) {
            try {
                String orderId = row.getOrDefault("订单编号", "");
                if (orderId.isEmpty()) {
                    log.warn("映射项目订单成本跳过: 订单编号为空");
                    continue;
                }
                dingTalkOrderIds.add(orderId);

                // 查找已存在记录（按 order_id）
                Optional<DtProjectOrderCost> existing = projectOrderCostRepository.findAll().stream()
                        .filter(e -> e.getSyncStatus() == null || "ACTIVE".equals(e.getSyncStatus()))
                        .filter(e -> orderId.equals(e.getOrderId()))
                        .findFirst();

                DtProjectOrderCost e;
                if (existing.isPresent()) {
                    e = existing.get();
                    updated++;
                } else {
                    e = new DtProjectOrderCost();
                    e.setId(UUID.randomUUID().toString());
                    inserted++;
                }

                e.setOrderName(row.getOrDefault("订单名称", ""));
                e.setParentOrderId(row.getOrDefault("订单", ""));
                e.setOrderCategory(row.getOrDefault("订单类别", ""));
                e.setOrderStatus(row.getOrDefault("订单状态", ""));
                e.setProjectId(row.getOrDefault("关联项目编号", ""));
                e.setProductId(row.getOrDefault("关联产品ID", ""));
                e.setDepartment(row.getOrDefault("订单所属部门", ""));
                e.setOrganization(row.getOrDefault("所属机构", ""));
                e.setPlanStartDate(row.getOrDefault("计划开始日期", null));
                e.setPlanEndDate(row.getOrDefault("计划结束日期", null));
                e.setOrderCreateDate(row.getOrDefault("订单创建日期", null));
                e.setOrderBudget(parseDec(row.get("订单费用预算")));
                e.setProjectBudgetCost(parseDec(row.get("项目预算口径成本数")));
                e.setBookedCost(parseDec(row.get("订单已入账成本（财务）")));
                e.setUnbookedCost(parseDec(row.get("订单未入账成本")));
                e.setActualCostTotal(parseDec(row.get("实际成本合计")));
                e.setEstimatedManDays(parseDec(row.get("订单预计工作量（人天）")));
                e.setActualAttendanceDays(parseDec(row.get("实际出勤人天a")));
                e.setActualPerformanceDays(parseDec(row.get("实际绩效人天b")));
                e.setExecutor(row.getOrDefault("执行人", ""));
                e.setResponsible(row.getOrDefault("责任人", ""));
                e.setCreateDesc(row.getOrDefault("创建描述", ""));
                e.setSyncDate(now);
                e.setFinanceDate(row.getOrDefault("财务记账日期", ""));
                e.setRawJson(toJson(row));
                e.setSyncStatus("ACTIVE");
                projectOrderCostRepository.save(e);
            } catch (Exception ex) {
                log.warn("映射项目订单成本跳过: {}", ex.getMessage());
            }
        }

        // 2. 标记不在钉钉中的旧记录为 DELETED
        int deleted = 0;
        if (!dingTalkOrderIds.isEmpty()) {
            List<DtProjectOrderCost> allActive = projectOrderCostRepository.findAll().stream()
                    .filter(e -> e.getSyncStatus() == null || "ACTIVE".equals(e.getSyncStatus()))
                    .filter(e -> e.getOrderId() != null && !dingTalkOrderIds.contains(e.getOrderId()))
                    .collect(Collectors.toList());
            for (DtProjectOrderCost e : allActive) {
                e.setSyncStatus("DELETED");
                e.setSyncDate(now);
                projectOrderCostRepository.save(e);
                deleted++;
            }
        }

        log.info("项目订单成本同步完成: 新增={}, 更新={}, 标记删除={}, 总计={}", inserted, updated, deleted, rows.size());
        return inserted + updated;
    }

    // ======================== 目标数据聚合 ========================

    /**
     * 签约合同 + 签约派单 -> targets (targetType=签约收入, category=价值)
     */
    void syncSigningTargets() {
        log.info("开始聚合签约目标数据...");

        // 1. 查询已有签约收入目标，构建 (org+dept) -> Target 的映射
        Map<String, Target> existingMap = targetRepository.findAll().stream()
                .filter(t -> "签约收入".equals(t.getTargetType()) && "价值".equals(t.getCategory()))
                .collect(Collectors.toMap(
                        t -> buildCompositeKey(t.getOrganization(), t.getDepartment()),
                        t -> t,
                        (a, b) -> a));

        // 2. 聚签约合同: department + signing_quarter + signing_risk_level
        List<SigningAggRow> contractAgg = jdbcTemplate.query(
                "SELECT department, signing_quarter, signing_risk_level, " +
                "SUM(COALESCE(signing_amount_wan, 0)) as total_amount " +
                "FROM dt_signing_contracts " +
                "WHERE signing_quarter IS NOT NULL AND signing_quarter != '' " +
                "  AND department IS NOT NULL AND department != '' " +
                "GROUP BY department, signing_quarter, signing_risk_level",
                (rs, rowNum) -> new SigningAggRow(
                        rs.getString("department"),
                        rs.getString("signing_quarter"),
                        rs.getString("signing_risk_level"),
                        rs.getBigDecimal("total_amount")));

        log.info("签约合同聚合: {} 条分组", contractAgg.size());

        // 3. 聚合签约派单: department + signing_quarter (全中风险)
        List<SigningAggRow> orderAgg = jdbcTemplate.query(
                "SELECT department, signing_quarter, " +
                "SUM(COALESCE(order_amount_wan, 0)) as total_amount " +
                "FROM dt_signing_orders " +
                "WHERE signing_quarter IS NOT NULL AND signing_quarter != '' " +
                "  AND department IS NOT NULL AND department != '' " +
                "GROUP BY department, signing_quarter",
                (rs, rowNum) -> new SigningAggRow(
                        rs.getString("department"),
                        rs.getString("signing_quarter"),
                        "中风险",
                        rs.getBigDecimal("total_amount")));

        log.info("签约派单聚合: {} 条分组", orderAgg.size());

        // 合并合同和派单
        List<SigningAggRow> allAgg = new ArrayList<>();
        allAgg.addAll(contractAgg);
        allAgg.addAll(orderAgg);

        List<Target> toSave = new ArrayList<>();
        for (SigningAggRow row : allAgg) {
            String dept = row.department;
            String org = dept; // dt_signing_contracts 没有独立的 org 字段，用 department 替代
            String subCategory = mapRiskLevel(row.riskLevel);
            String compositeKey = buildCompositeKey(org, dept);

            Target target = existingMap.get(compositeKey);
            if (target == null) {
                target = new Target();
                target.setId(generateTargetId(org, dept, DEFAULT_YEAR, "签约收入", "价值", subCategory));
                target.setOrganization(org);
                target.setDepartment(dept);
                target.setTargetType("签约收入");
                target.setCategory("价值");
                target.setSubCategory(subCategory);
                target.setYear(DEFAULT_YEAR);
                target.setAnnualTarget(BigDecimal.ZERO);
                target.setQ1Target(BigDecimal.ZERO);
                target.setQ2Target(BigDecimal.ZERO);
                target.setQ3Target(BigDecimal.ZERO);
                target.setQ4Target(BigDecimal.ZERO);
                target.setQ1Actual(BigDecimal.ZERO);
                target.setQ2Actual(BigDecimal.ZERO);
                target.setQ3Actual(BigDecimal.ZERO);
                target.setQ4Actual(BigDecimal.ZERO);
                existingMap.put(compositeKey, target);
            }

            // 映射季度，填充对应季度目标值
            BigDecimal qVal = row.totalAmount != null ? row.totalAmount : BigDecimal.ZERO;
            switch (mapQuarter(row.quarter)) {
                case 1:
                    // Q1: 如果已有值（来自季度确认表），不覆盖
                    if (target.getQ1Target() == null || target.getQ1Target().compareTo(BigDecimal.ZERO) == 0) {
                        target.setQ1Target(qVal);
                    } else {
                        log.debug("Q1已有数据，跳过签约合同覆盖: org={}, dept={}", org, dept);
                    }
                    break;
                case 2:
                    target.setQ2Target(qVal);
                    break;
                case 3:
                    target.setQ3Target(qVal);
                    break;
                case 4:
                    target.setQ4Target(qVal);
                    break;
                default:
                    log.debug("未知季度: {}", row.quarter);
            }

            // 累加年度目标
            BigDecimal annual = safeAdd(target.getQ1Target(), target.getQ2Target(),
                    target.getQ3Target(), target.getQ4Target());
            target.setAnnualTarget(annual);

            toSave.add(target);
        }

        targetRepository.saveAll(toSave);
        log.info("签约目标聚合完成: {} 条", toSave.size());
    }

    /**
     * 研发成果 -> targets (targetType=研发成果, category=价值, subCategory=研发成果)
     */
    void syncAchievementTargets() {
        log.info("开始聚合研发成果目标数据...");

        // 查询: org_unit + year + quarter + count (实际验收)
        List<AchievementAggRow> actualRows = jdbcTemplate.query(
                "SELECT org_unit, " +
                "CASE " +
                "  WHEN CAST(SUBSTR(actual_accept_date, 6, 2) AS INTEGER) BETWEEN 1 AND 3 THEN '一季度' " +
                "  WHEN CAST(SUBSTR(actual_accept_date, 6, 2) AS INTEGER) BETWEEN 4 AND 6 THEN '二季度' " +
                "  WHEN CAST(SUBSTR(actual_accept_date, 6, 2) AS INTEGER) BETWEEN 7 AND 9 THEN '三季度' " +
                "  WHEN CAST(SUBSTR(actual_accept_date, 6, 2) AS INTEGER) BETWEEN 10 AND 12 THEN '四季度' " +
                "END as quarter, " +
                "COUNT(*) as cnt " +
                "FROM dt_achievements " +
                "WHERE actual_accept_date IS NOT NULL AND actual_accept_date != '' " +
                "  AND org_unit IS NOT NULL AND org_unit != '' " +
                "GROUP BY org_unit, quarter",
                (rs, rowNum) -> new AchievementAggRow(
                        rs.getString("org_unit"),
                        rs.getString("quarter"),
                        rs.getInt("cnt")));

        log.info("研发成果(实际验收)聚合: {} 条分组", actualRows.size());

        // 查询: org_unit + year + quarter + count (计划验收、尚未实际验收)
        List<AchievementAggRow> plannedRows = jdbcTemplate.query(
                "SELECT org_unit, " +
                "CASE " +
                "  WHEN CAST(SUBSTR(planned_accept_date, 6, 2) AS INTEGER) BETWEEN 1 AND 3 THEN '一季度' " +
                "  WHEN CAST(SUBSTR(planned_accept_date, 6, 2) AS INTEGER) BETWEEN 4 AND 6 THEN '二季度' " +
                "  WHEN CAST(SUBSTR(planned_accept_date, 6, 2) AS INTEGER) BETWEEN 7 AND 9 THEN '三季度' " +
                "  WHEN CAST(SUBSTR(planned_accept_date, 6, 2) AS INTEGER) BETWEEN 10 AND 12 THEN '四季度' " +
                "END as quarter, " +
                "COUNT(*) as cnt " +
                "FROM dt_achievements " +
                "WHERE planned_accept_date IS NOT NULL AND planned_accept_date != '' " +
                "  AND (actual_accept_date IS NULL OR actual_accept_date = '') " +
                "  AND org_unit IS NOT NULL AND org_unit != '' " +
                "GROUP BY org_unit, quarter",
                (rs, rowNum) -> new AchievementAggRow(
                        rs.getString("org_unit"),
                        rs.getString("quarter"),
                        rs.getInt("cnt")));

        log.info("研发成果(计划验收)聚合: {} 条分组", plannedRows.size());

        // 合并写入 targets
        Map<String, Target> targetMap = new LinkedHashMap<>();

        for (AchievementAggRow row : actualRows) {
            Target t = getOrCreateAchievementTarget(targetMap, row.orgUnit);
            setQuarterActual(t, mapQuarter(row.quarter), BigDecimal.valueOf(row.cnt));
        }
        for (AchievementAggRow row : plannedRows) {
            Target t = getOrCreateAchievementTarget(targetMap, row.orgUnit);
            setQuarterTarget(t, mapQuarter(row.quarter), BigDecimal.valueOf(row.cnt));
        }

        // 计算年度总和
        for (Target t : targetMap.values()) {
            t.setAnnualTarget(safeAdd(t.getQ1Target(), t.getQ2Target(), t.getQ3Target(), t.getQ4Target()));
        }

        targetRepository.saveAll(targetMap.values());
        log.info("研发成果目标聚合完成: {} 条", targetMap.size());
    }

    /**
     * 损益表(PL) -> targets (targetType=费用, category=费用, subCategory=费用)
     */
    void syncTargetsFromPL(List<Map<String, String>> dataRows) {
        if (dataRows == null || dataRows.isEmpty()) {
            log.warn("损益表数据为空，跳过损益表目标同步");
            return;
        }

        // 1. 分析第一行 keys，找到 科目 字段和月度列
        Map<String, String> firstRow = dataRows.get(0);

        String subjectKey = findKeyContaining(firstRow, "科目");
        if (subjectKey == null) {
            log.warn("未找到科目字段，跳过损益表目标同步。可用字段: {}", firstRow.keySet());
            return;
        }

        // 查找月度预算列: "X月预算" 或 "X 月预算"
        Map<Integer, String> budgetMonthCols = findMonthlyColumns(firstRow, "预算");
        // 查找月度实际列: "X月实际" 或 "X 月实际"
        Map<Integer, String> actualMonthCols = findMonthlyColumns(firstRow, "实际");

        if (budgetMonthCols.isEmpty() && actualMonthCols.isEmpty()) {
            log.warn("未找到月度预算/实际列，跳过损益表目标同步。可用字段: {}", firstRow.keySet());
            return;
        }

        log.info("损益表目标同步: 科目字段={}, 预算月度列={}, 实际月度列={}",
                subjectKey, budgetMonthCols.size(), actualMonthCols.size());

        // 2. 过滤科目="费用"的行，聚合月度数据
        String orgKey = findKeyContaining(firstRow, "经营机构");
        if (orgKey == null) orgKey = findKeyContaining(firstRow, "部门名称");
        if (orgKey == null) orgKey = findKeyContaining(firstRow, "所属部门");
        String deptKey = findKeyContaining(firstRow, "部门名称");
        if (deptKey == null) deptKey = orgKey;
        String yearKey = findKeyContaining(firstRow, "年度");
        if (yearKey == null) yearKey = findKeyContaining(firstRow, "年份");

        Map<String, Target> targetMap = new LinkedHashMap<>();
        int rowCount = 0;

        for (Map<String, String> row : dataRows) {
            String subject = row.getOrDefault(subjectKey, "").trim();
            if (!"费用".equals(subject)) continue;

            String orgVal = row.getOrDefault(orgKey, "").trim();
            String deptVal = row.getOrDefault(deptKey, "").trim();
            if (orgVal.isEmpty()) orgVal = deptVal;
            if (deptVal.isEmpty()) deptVal = orgVal;
            if (orgVal.isEmpty()) continue;
            final String org = orgVal;
            final String dept = deptVal;

            int yearVal = DEFAULT_YEAR;
            if (yearKey != null) {
                String yearStr = row.getOrDefault(yearKey, "").trim();
                if (!yearStr.isEmpty()) {
                    try { yearVal = Integer.parseInt(yearStr); } catch (NumberFormatException ignored) {}
                }
            }
            final int year = yearVal;

            // 聚合月度预算 -> Q1-Q4 target
            BigDecimal q1Budget = sumMonths(row, budgetMonthCols, 1, 3);
            BigDecimal q2Budget = sumMonths(row, budgetMonthCols, 4, 6);
            BigDecimal q3Budget = sumMonths(row, budgetMonthCols, 7, 9);
            BigDecimal q4Budget = sumMonths(row, budgetMonthCols, 10, 12);

            // 聚合月度实际 -> Q1-Q4 actual
            BigDecimal q1Actual = sumMonths(row, actualMonthCols, 1, 3);
            BigDecimal q2Actual = sumMonths(row, actualMonthCols, 4, 6);
            BigDecimal q3Actual = sumMonths(row, actualMonthCols, 7, 9);
            BigDecimal q4Actual = sumMonths(row, actualMonthCols, 10, 12);

            String id = generateTargetId(org, dept, year, "费用", "费用", "费用");
            Target target = targetMap.computeIfAbsent(id, k -> {
                Target t = new Target();
                t.setId(k);
                t.setOrganization(org);
                t.setDepartment(dept);
                t.setTargetType("费用");
                t.setCategory("费用");
                t.setSubCategory("费用");
                t.setYear(year);
                return t;
            });

            target.setQ1Target(q1Budget);
            target.setQ2Target(q2Budget);
            target.setQ3Target(q3Budget);
            target.setQ4Target(q4Budget);
            target.setQ1Actual(q1Actual);
            target.setQ2Actual(q2Actual);
            target.setQ3Actual(q3Actual);
            target.setQ4Actual(q4Actual);
            target.setAnnualTarget(safeAdd(q1Budget, q2Budget, q3Budget, q4Budget));

            rowCount++;
        }

        if (!targetMap.isEmpty()) {
            targetRepository.saveAll(targetMap.values());
        }
        log.info("损益表目标同步完成: {} 行费用数据, {} 条目标", rowCount, targetMap.size());
    }

    /**
     * 重建目标数据: 删除全部 targets，然后从 dt_* 表重新聚合
     */
    public Map<String, Object> rebuildTargets() {
        Map<String, Object> result = new LinkedHashMap<>();
        log.info("重建目标数据: 删除所有现有目标...");
        targetRepository.deleteAll();
        result.put("rebuild", "targets_cleared");

        try {
            syncSigningTargets();
            result.put("signing_targets", "ok");
        } catch (Exception e) {
            log.error("重建签约目标失败: {}", e.getMessage(), e);
            result.put("signing_targets", "error: " + e.getMessage());
        }

        try {
            syncAchievementTargets();
            result.put("achievement_targets", "ok");
        } catch (Exception e) {
            log.error("重建研发成果目标失败: {}", e.getMessage(), e);
            result.put("achievement_targets", "error: " + e.getMessage());
        }

        try {
            List<Map<String, String>> plDataRows = client.listRecordsWithFieldNames("drgfZh4", props.getDataWarehouseBaseId());
            syncTargetsFromPL(plDataRows);
            result.put("pl_targets", "ok");
        } catch (Exception e) {
            log.error("重建损益表目标失败: {}", e.getMessage(), e);
            result.put("pl_targets", "error: " + e.getMessage());
        }

        long count = targetRepository.count();
        result.put("total_targets", count);
        log.info("重建目标完成: {} 条", count);
        return result;
    }

    // ======================== 辅助方法 ========================

    /**
     * 生成确定性的 target ID
     */
    private String generateTargetId(String org, String dept, Integer year,
                                    String targetType, String category, String subCategory) {
        String raw = (org != null ? org : "") + "|" + (dept != null ? dept : "") + "|"
                + year + "|" + targetType + "|" + category + "|" + (subCategory != null ? subCategory : "");
        return UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private String buildCompositeKey(String org, String dept) {
        return (org != null ? org : "") + "||" + (dept != null ? dept : "");
    }

    /**
     * 在 row keys 中查找包含指定关键字的字段名
     */
    private String findKeyContaining(Map<String, String> row, String keyword) {
        for (String key : row.keySet()) {
            if (key.contains(keyword)) return key;
        }
        return null;
    }

    /**
     * 查找月度列: 匹配 "X月keyword" 或 "X 月 keyword" 模式的字段
     */
    private Map<Integer, String> findMonthlyColumns(Map<String, String> row, String suffix) {
        Map<Integer, String> result = new TreeMap<>();
        Pattern p = Pattern.compile("(\\d+)\\s*月\\s*" + Pattern.quote(suffix));
        for (String key : row.keySet()) {
            Matcher m = p.matcher(key);
            if (m.find()) {
                int month = Integer.parseInt(m.group(1));
                if (month >= 1 && month <= 12) {
                    result.put(month, key);
                }
            }
        }
        return result;
    }

    /**
     * 对指定月份范围求和
     */
    private BigDecimal sumMonths(Map<String, String> row, Map<Integer, String> monthCols,
                                  int fromMonth, int toMonth) {
        BigDecimal sum = BigDecimal.ZERO;
        for (int m = fromMonth; m <= toMonth; m++) {
            String colName = monthCols.get(m);
            if (colName != null) {
                sum = sum.add(parseDec(row.get(colName)));
            }
        }
        return sum;
    }

    /**
     * 映射风险等级 -> subCategory
     */
    private String mapRiskLevel(String riskLevel) {
        if (riskLevel == null) return "签约收入(中)";
        switch (riskLevel) {
            case "高风险": return "签约收入(高)";
            case "中风险": return "签约收入(中)";
            default: return "签约收入(中)";
        }
    }

    /**
     * 映射季度名称 -> 数字
     */
    private int mapQuarter(String quarter) {
        if (quarter == null) return 0;
        switch (quarter.trim()) {
            case "一季度": return 1;
            case "二季度": return 2;
            case "三季度": return 3;
            case "四季度": return 4;
            default:
                // 尝试数字解析
                try { return Integer.parseInt(quarter.trim()); } catch (NumberFormatException e) { return 0; }
        }
    }

    /**
     * 安全加法
     */
    private BigDecimal safeAdd(BigDecimal... vals) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal v : vals) {
            if (v != null) sum = sum.add(v);
        }
        return sum;
    }

    private Target getOrCreateAchievementTarget(Map<String, Target> map, String orgUnit) {
        String id = generateTargetId(orgUnit, orgUnit, DEFAULT_YEAR, "研发成果", "价值", "研发成果");
        return map.computeIfAbsent(id, k -> {
            Target t = new Target();
            t.setId(k);
            t.setOrganization(orgUnit);
            t.setDepartment(orgUnit);
            t.setTargetType("研发成果");
            t.setCategory("价值");
            t.setSubCategory("研发成果");
            t.setYear(DEFAULT_YEAR);
            t.setQ1Target(BigDecimal.ZERO);
            t.setQ2Target(BigDecimal.ZERO);
            t.setQ3Target(BigDecimal.ZERO);
            t.setQ4Target(BigDecimal.ZERO);
            t.setQ1Actual(BigDecimal.ZERO);
            t.setQ2Actual(BigDecimal.ZERO);
            t.setQ3Actual(BigDecimal.ZERO);
            t.setQ4Actual(BigDecimal.ZERO);
            t.setAnnualTarget(BigDecimal.ZERO);
            return t;
        });
    }

    private void setQuarterTarget(Target t, int quarter, BigDecimal val) {
        if (val == null) val = BigDecimal.ZERO;
        switch (quarter) {
            case 1: t.setQ1Target(val); break;
            case 2: t.setQ2Target(val); break;
            case 3: t.setQ3Target(val); break;
            case 4: t.setQ4Target(val); break;
        }
    }

    private void setQuarterActual(Target t, int quarter, BigDecimal val) {
        if (val == null) val = BigDecimal.ZERO;
        switch (quarter) {
            case 1: t.setQ1Actual(val); break;
            case 2: t.setQ2Actual(val); break;
            case 3: t.setQ3Actual(val); break;
            case 4: t.setQ4Actual(val); break;
        }
    }

    // ======================== 内部数据类 ========================

    private static class SigningAggRow {
        final String department;
        final String quarter;
        final String riskLevel;
        final BigDecimal totalAmount;
        SigningAggRow(String d, String q, String r, BigDecimal t) {
            this.department = d; this.quarter = q; this.riskLevel = r; this.totalAmount = t;
        }
    }

    private static class AchievementAggRow {
        final String orgUnit;
        final String quarter;
        final int cnt;
        AchievementAggRow(String o, String q, int c) {
            this.orgUnit = o; this.quarter = q; this.cnt = c;
        }
    }

    // ======================== 基础工具方法 ========================

    private BigDecimal parseDec(String val) {
        if (val == null || val.trim().isEmpty()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(val.trim().replace(",", ""));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
