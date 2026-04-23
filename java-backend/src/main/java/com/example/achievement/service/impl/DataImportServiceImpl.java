package com.example.achievement.service.impl;

import com.example.achievement.dto.response.ImportResult;
import com.example.achievement.entity.Achievement;
import com.example.achievement.entity.AchievementStatusRecord;
import com.example.achievement.entity.Product;
import com.example.achievement.enums.AchievementStatus;
import com.example.achievement.repository.AchievementRepository;
import com.example.achievement.repository.AchievementStatusRecordRepository;
import com.example.achievement.repository.ProductRepository;
import com.example.achievement.service.DataImportService;
import com.example.achievement.util.DataConverterUtil;
import com.example.achievement.util.ExcelReaderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 数据导入服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataImportServiceImpl implements DataImportService {

    private final AchievementRepository achievementRepository;
    private final ProductRepository productRepository;
    private final AchievementStatusRecordRepository statusRecordRepository;

    @Override
    @Transactional
    public ImportResult importFromExcel(String filePath) {
        log.info("开始从文件导入数据: {}", filePath);
        ImportResult result = ImportResult.builder().build();

        try {
            List<Map<String, String>> dataList = ExcelReaderUtil.readExcel(filePath);
            return processDataList(dataList, result, false);
        } catch (IOException e) {
            log.error("读取Excel文件失败: {}", filePath, e);
            result.addError("读取Excel文件失败: " + e.getMessage());
            return result;
        }
    }

    @Override
    @Transactional
    public ImportResult importFromExcel(InputStream inputStream) {
        log.info("开始从输入流导入数据");
        ImportResult result = ImportResult.builder().build();

        try {
            List<Map<String, String>> dataList = ExcelReaderUtil.readExcel(inputStream, "产品成果明细登记薄");
            return processDataList(dataList, result, false);
        } catch (IOException e) {
            log.error("读取Excel数据失败", e);
            result.addError("读取Excel数据失败: " + e.getMessage());
            return result;
        }
    }

    @Override
    @Transactional
    public ImportResult reinitializeFromExcel(InputStream inputStream) {
        log.info("开始清空数据库并重新导入数据");
        ImportResult result = ImportResult.builder().build();

        try {
            // 清空数据库
            clearAllData();
            log.info("数据库清空完成");

            // 导入新数据
            List<Map<String, String>> dataList = ExcelReaderUtil.readExcel(inputStream, "产品成果明细登记薄");
            return processDataList(dataList, result, true);
        } catch (IOException e) {
            log.error("读取Excel数据失败", e);
            result.addError("读取Excel数据失败: " + e.getMessage());
            return result;
        } catch (Exception e) {
            log.error("重新初始化数据失败", e);
            result.addError("重新初始化数据失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 清空数据库
     */
    private void clearAllData() {
        log.info("开始清空数据库...");
        
        // 删除所有状态变更记录
        long statusRecordCount = statusRecordRepository.count();
        statusRecordRepository.deleteAll();
        log.info("已删除 {} 条状态变更记录", statusRecordCount);
        
        // 删除所有成果记录
        long achievementCount = achievementRepository.count();
        achievementRepository.deleteAll();
        log.info("已删除 {} 条成果记录", achievementCount);
        
        // 删除所有产品记录
        long productCount = productRepository.count();
        productRepository.deleteAll();
        log.info("已删除 {} 条产品记录", productCount);
    }

    /**
     * 处理数据列表
     * 
     * @param dataList 数据列表
     * @param result 导入结果
     * @param isReinitialize 是否为重新初始化模式
     * @return 导入结果
     */
    private ImportResult processDataList(List<Map<String, String>> dataList, ImportResult result, boolean isReinitialize) {
        if (dataList == null || dataList.isEmpty()) {
            result.addWarning("Excel文件中没有数据");
            return result;
        }

        result.setTotalRecords(dataList.size());
        log.info("共读取到 {} 条数据", dataList.size());

        for (int i = 0; i < dataList.size(); i++) {
            try {
                Map<String, String> rowData = dataList.get(i);
                int rowNum = i + 2; // Excel行号（从第2行开始，第1行是表头）

                // 验证必填字段
                String validationError = validateRowData(rowData, rowNum);
                if (validationError != null) {
                    result.addError(validationError);
                    result.setFailedCount(result.getFailedCount() + 1);
                    continue;
                }

                // 转换为Achievement实体
                Achievement achievement = convertToAchievement(rowData);

                // 检查是否已存在（非重新初始化模式下）
                if (!isReinitialize) {
                    Optional<Achievement> existingOpt = achievementRepository.findById(achievement.getId());
                    if (existingOpt.isPresent()) {
                        result.addWarning(String.format("第%d行: 成果ID [%s] 已存在，跳过导入", rowNum, achievement.getId()));
                        result.setSkippedCount(result.getSkippedCount() + 1);
                        continue;
                    }
                }

                // 创建或获取产品
                createOrGetProduct(rowData, achievement);

                // 保存成果
                achievementRepository.save(achievement);

                // 创建状态变更记录
                createStatusRecords(achievement);

                result.addImportedId(achievement.getId());
                result.setSuccessCount(result.getSuccessCount() + 1);

                log.debug("第{}行导入成功: {}", rowNum, achievement.getId());

            } catch (Exception e) {
                log.error("处理第{}行数据时发生错误", i + 2, e);
                result.addError(String.format("第%d行处理失败: %s", i + 2, e.getMessage()));
                result.setFailedCount(result.getFailedCount() + 1);
            }
        }

        log.info("数据导入完成: 总数={}, 成功={}, 失败={}, 跳过={}",
                result.getTotalRecords(), result.getSuccessCount(),
                result.getFailedCount(), result.getSkippedCount());

        return result;
    }

    /**
     * 验证行数据
     */
    private String validateRowData(Map<String, String> rowData, int rowNum) {
        // 成果名称必填
        String name = rowData.get("成果名称");
        if (name == null || name.trim().isEmpty()) {
            return String.format("第%d行: 成果名称不能为空", rowNum);
        }

        // 成果版本必填
        String version = rowData.get("成果版本");
        if (version == null || version.trim().isEmpty()) {
            return String.format("第%d行: 成果版本不能为空", rowNum);
        }

        // 关联产品必填
        String product = rowData.get("关联产品");
        if (product == null || product.trim().isEmpty()) {
            return String.format("第%d行: 关联产品不能为空", rowNum);
        }

        return null;
    }

    /**
     * 将Excel行数据转换为Achievement实体
     */
    private Achievement convertToAchievement(Map<String, String> rowData) {
        Achievement achievement = new Achievement();

        // 生成ID: 成果名称_成果版本
        String name = DataConverterUtil.safeString(rowData.get("成果名称"));
        String version = DataConverterUtil.safeString(rowData.get("成果版本"));
        String id = generateId(name, version);
        achievement.setId(id);
        achievement.setName(name);
        achievement.setVersion(version);

        // 基础数据
        achievement.setAchievementForm(DataConverterUtil.safeString(rowData.get("成果形态")));
        achievement.setOrganizationName(DataConverterUtil.safeString(rowData.get("所属机构")));
        achievement.setDepartmentName(DataConverterUtil.safeString(rowData.get("部门")));
        achievement.setHasBaseline(DataConverterUtil.safeString(rowData.get("是否有基线")));
        achievement.setApplicationScenario(DataConverterUtil.safeString(rowData.get("应用场景")));
        achievement.setRequirementProposer(DataConverterUtil.safeString(rowData.get("成果需求提出人")));

        // 处理关联产品
        String product = DataConverterUtil.safeString(rowData.get("关联产品"));
        String productCode = DataConverterUtil.safeString(rowData.get("产品编码"));
        if (product != null) {
            // 使用产品编码作为ID，如果没有产品编码则使用产品名称生成
            String productId = (productCode != null && !productCode.trim().isEmpty()) 
                    ? productCode.trim() 
                    : generateProductId(product);
            achievement.setProductId(productId);
            achievement.setProductName(product);
        }

        // 处理关联套餐（转换为JSON）
        String packageIds = DataConverterUtil.safeString(rowData.get("关联套餐"));
        if (packageIds != null) {
            achievement.setPackageIds(DataConverterUtil.convertPackageIdsToJson(packageIds));
        }

        // 计划数据
        achievement.setAchievementTarget(DataConverterUtil.safeString(rowData.get("成果目标描述")));
        achievement.setAcceptor(DataConverterUtil.safeString(rowData.get("验收人（可多人）")));
        achievement.setAcceptanceMethod(DataConverterUtil.safeString(rowData.get("成果验收方式")));
        achievement.setPlannedAcceptanceDate(DataConverterUtil.parseDate(rowData.get("计划验收日期")));
        achievement.setRelatedProjectName(DataConverterUtil.safeString(rowData.get("关联项目")));
        achievement.setRelatedOrderName(DataConverterUtil.safeString(rowData.get("关联订单")));
        achievement.setRelatedOrderId(DataConverterUtil.safeString(rowData.get("关联订单编号")));
        achievement.setEstimatedAcceptanceMonth(DataConverterUtil.safeString(rowData.get("预估验收年月")));

        // 实际数据
        achievement.setActualAcceptanceDate(DataConverterUtil.parseDate(rowData.get("实际验收日期")));
        achievement.setDemoUrl(DataConverterUtil.safeString(rowData.get("DEMO地址")));
        achievement.setDeliverables(DataConverterUtil.safeString(rowData.get("成果验收提交物")));
        achievement.setCodeRepositoryUrl(DataConverterUtil.safeString(rowData.get("代码仓库/在线文档地址")));

        // 系统数据 - 解析状态
        String statusStr = DataConverterUtil.safeString(rowData.get("成果状态"));
        AchievementStatus status = DataConverterUtil.parseStatus(statusStr);
        achievement.setStatus(status != null ? status : AchievementStatus.RECORDED);

        achievement.setSaleType(DataConverterUtil.safeString(rowData.get("成果可售类型")));
        achievement.setChangeReason(DataConverterUtil.safeString(rowData.get("异常变更原因")));
        achievement.setCreatedBy(DataConverterUtil.safeString(rowData.get("创建人")));
        achievement.setUpdatedBy(DataConverterUtil.safeString(rowData.get("更新人")));

        // 处理更新时间
        String updatedAtStr = DataConverterUtil.safeString(rowData.get("最后更新时间"));
        if (updatedAtStr != null) {
            try {
                achievement.setUpdatedAt(DataConverterUtil.parseDate(updatedAtStr).atStartOfDay());
            } catch (Exception e) {
                log.debug("解析更新时间失败: {}", updatedAtStr);
            }
        }

        // 设置默认值
        if (achievement.getHasBaseline() == null) {
            achievement.setHasBaseline("无基线");
        }
        if (achievement.getStatus() == null) {
            achievement.setStatus(AchievementStatus.RECORDED);
        }
        if (achievement.getPackageIds() == null) {
            achievement.setPackageIds("[]");
        }

        // 根据状态设置时间字段
        setStatusTimeFields(achievement);

        return achievement;
    }

    /**
     * 根据状态设置时间字段
     */
    private void setStatusTimeFields(Achievement achievement) {
        LocalDateTime now = LocalDateTime.now();
        AchievementStatus status = achievement.getStatus();

        if (status == null) {
            return;
        }

        switch (status) {
            case PRE_REGISTER:
                // 预注册状态：只设置 pre_register_time
                if (achievement.getPreRegisterTime() == null) {
                    achievement.setPreRegisterTime(now);
                }
                break;
            case REGISTER:
                // 注册状态：设置 pre_register_time 和 register_time
                if (achievement.getPreRegisterTime() == null) {
                    achievement.setPreRegisterTime(now);
                }
                if (achievement.getRegisterTime() == null) {
                    achievement.setRegisterTime(now);
                }
                break;
            case RECORDED:
                // 登记状态：设置 pre_register_time、register_time 和 record_time
                if (achievement.getPreRegisterTime() == null) {
                    achievement.setPreRegisterTime(now);
                }
                if (achievement.getRegisterTime() == null) {
                    achievement.setRegisterTime(now);
                }
                if (achievement.getRecordTime() == null) {
                    achievement.setRecordTime(now);
                }
                break;
            default:
                // 其他状态默认设置预注册时间
                if (achievement.getPreRegisterTime() == null) {
                    achievement.setPreRegisterTime(now);
                }
                break;
        }
    }

    /**
     * 创建或获取产品
     */
    private void createOrGetProduct(Map<String, String> rowData, Achievement achievement) {
        String productName = DataConverterUtil.safeString(rowData.get("关联产品"));
        String productCode = DataConverterUtil.safeString(rowData.get("产品编码"));
        
        if (productName == null || productName.trim().isEmpty()) {
            return;
        }

        // 使用产品编码作为ID，如果没有产品编码则使用产品名称生成
        String productId = (productCode != null && !productCode.trim().isEmpty()) 
                ? productCode.trim() 
                : generateProductId(productName);

        // 检查产品是否已存在
        Optional<Product> existingProduct = productRepository.findById(productId);
        if (existingProduct.isPresent()) {
            log.debug("产品已存在: {}", productId);
            return;
        }

        // 创建新产品
        Product product = new Product();
        product.setId(productId);
        product.setProductName(productName);
        product.setProductCode(productCode != null ? productCode : productId);
        product.setOrganizationName(DataConverterUtil.safeString(rowData.get("所属机构")));
        product.setCreatedBy(DataConverterUtil.safeString(rowData.get("创建人")));

        productRepository.save(product);
        log.info("创建新产品: {} - {}", productId, productName);
    }

    /**
     * 创建状态变更记录
     */
    private void createStatusRecords(Achievement achievement) {
        AchievementStatus status = achievement.getStatus();
        if (status == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        String operator = achievement.getCreatedBy() != null ? achievement.getCreatedBy() : "system";

        switch (status) {
            case PRE_REGISTER:
                // 预注册状态：不创建状态变更记录
                log.debug("预注册状态，不创建状态变更记录: {}", achievement.getId());
                break;
            case REGISTER:
                // 注册状态：创建 pre_register -> register 的记录
                createStatusRecord(achievement, "PRE_REGISTER", "REGISTER", now, operator, "导入数据创建");
                break;
            case RECORDED:
                // 登记状态：创建 pre_register -> register -> recorded 的记录
                createStatusRecord(achievement, "PRE_REGISTER", "REGISTER", 
                        achievement.getRegisterTime() != null ? achievement.getRegisterTime() : now, 
                        operator, "导入数据创建");
                createStatusRecord(achievement, "REGISTER", "RECORDED", 
                        achievement.getRecordTime() != null ? achievement.getRecordTime() : now, 
                        operator, "导入数据创建");
                break;
            default:
                break;
        }
    }

    /**
     * 创建单个状态变更记录
     */
    private void createStatusRecord(Achievement achievement, String fromStatus, String toStatus, 
                                    LocalDateTime changeTime, String operator, String reason) {
        AchievementStatusRecord record = new AchievementStatusRecord();
        record.setId(UUID.randomUUID().toString());
        record.setAchievementId(achievement.getId());
        record.setAchievementName(achievement.getName());
        record.setFromStatus(fromStatus);
        record.setToStatus(toStatus);
        record.setChangeType("STATUS_CHANGE");
        record.setChangeReason(reason);
        record.setOperator(operator);
        record.setChangeTime(changeTime);

        statusRecordRepository.save(record);
        log.debug("创建状态变更记录: {} -> {} for {}", fromStatus, toStatus, achievement.getId());
    }

    /**
     * 生成成果ID
     * 规则: 成果名称_成果版本
     */
    private String generateId(String name, String version) {
        if (name == null || version == null) {
            return null;
        }
        return name.trim() + "_" + version.trim();
    }

    /**
     * 生成产品ID
     * 如果产品不存在，使用产品名称作为ID
     */
    private String generateProductId(String productName) {
        if (productName == null || productName.trim().isEmpty()) {
            return "UNKNOWN";
        }
        // 使用产品名称作为ID（去除空格和特殊字符）
        return productName.trim().replaceAll("[\\s\\-_/]", "_");
    }
}
