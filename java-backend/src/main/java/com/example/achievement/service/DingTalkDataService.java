package com.example.achievement.service;

import com.example.achievement.entity.*;
import com.example.achievement.repository.*;
import com.example.achievement.config.DingTalkProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

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
    private final DingTalkClient client;
    private final DingTalkProperties props;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> MANAGED_SHEET_IDS = new LinkedHashSet<>(Arrays.asList(
            "O0DHU7u", "LfxeQF7", "BRNEkOg", "IjclEN7", "SXS7oOg", "Jwe8QNe", "drgfZh4"
    ));

    public Map<String, Object> syncAll() {
        Map<String, Object> result = new LinkedHashMap<>();
        log.info("开始同步所有钉钉数据表... (API调用计数: {})", client.getCallCount());

        if (client.isNearLimit()) {
            int remaining = client.remainingCalls();
            result.put("warning", String.format("API调用次数接近上限(剩余%d次), 请减少同步频率", remaining));
            log.warn("钉钉API接近上限，剩余 {} 次", remaining);
        }

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

                int count = syncSheetData(sheetId, dataRows);
                result.put(sheetId + "_count", count);
                result.put(sheetId + "_status", "ok");
            } catch (Exception e) {
                log.error("同步表格 {} 失败: {}", sheetId, e.getMessage(), e);
                result.put(sheetId + "_count", 0);
                result.put(sheetId + "_status", "error: " + e.getMessage());
            }
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
        switch (sheetId) {
            case "O0DHU7u": return syncSigningContracts(dataRows);
            case "LfxeQF7": return syncSigningOrders(dataRows);
            case "BRNEkOg": return syncRevenueDetails(dataRows);
            case "IjclEN7": return syncAchievements(dataRows);
            case "SXS7oOg": return syncProducts(dataRows);
            case "Jwe8QNe": return syncProductPackages(dataRows);
            case "drgfZh4": return syncDepartmentBudgets(dataRows);
            default:
                log.warn("未知表格ID: {}", sheetId);
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
