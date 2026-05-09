package com.example.achievement.service.impl;

import com.example.achievement.dto.request.ActualDataRequest;
import com.example.achievement.dto.request.CreateTargetRequest;
import com.example.achievement.dto.response.MonthlyDistributionResponse;
import com.example.achievement.dto.response.TargetStatisticsResponse;
import com.example.achievement.entity.*;
import com.example.achievement.enums.AchievementStatus;
import com.example.achievement.repository.*;
import com.example.achievement.service.TargetService;
import com.example.achievement.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TargetServiceImpl implements TargetService {

    private final TargetRepository targetRepository;
    private final ActualDataRepository actualDataRepository;
    private final AchievementRepository achievementRepository;
    private final ContractSigningRepository signingRepository;
    private final RevenueRecognitionRepository recognitionRepository;
    private final UserConfigRepository userConfigRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(readOnly = true)
    public TargetStatisticsResponse getStatistics(String dimension, Integer year, String product, String organization, String owner, String subCategory) {
        System.out.println("========== DEBUG: Parameters - dimension=" + dimension + ", year=" + year + ", product=" + product + ", organization=" + organization + ", owner=" + owner + ", subCategory=" + subCategory);
        
        // 直接使用JdbcTemplate查询
        String sql = "SELECT * FROM targets WHERE year = ?";
        List<Target> targets = jdbcTemplate.query(sql, new Object[]{year}, (rs, rowNum) -> {
            Target target = new Target();
            target.setId(rs.getString("id"));
            target.setDepartment(rs.getString("department"));
            target.setOrganization(rs.getString("organization"));
            target.setCategory(rs.getString("category"));
            target.setSubCategory(rs.getString("sub_category"));
            target.setTargetType(rs.getString("target_type"));
            target.setYear(rs.getInt("year"));
            target.setAnnualTarget(rs.getBigDecimal("annual_target"));
            target.setQ1Target(rs.getBigDecimal("q1_target"));
            target.setQ2Target(rs.getBigDecimal("q2_target"));
            target.setQ3Target(rs.getBigDecimal("q3_target"));
            target.setQ4Target(rs.getBigDecimal("q4_target"));
            target.setQ1Actual(rs.getBigDecimal("q1_actual"));
            target.setQ2Actual(rs.getBigDecimal("q2_actual"));
            target.setQ3Actual(rs.getBigDecimal("q3_actual"));
            target.setQ4Actual(rs.getBigDecimal("q4_actual"));
            target.setOwner(rs.getString("owner"));
            return target;
        });
        
        System.out.println("========== DEBUG: Found " + targets.size() + " targets for year " + year + " using JdbcTemplate");
        log.info("Found {} targets for year {}", targets.size(), year);
        
        if (!targets.isEmpty()) {
            if (product != null && !product.isEmpty()) {
                targets = targets.stream()
                        .filter(t -> product.equals(t.getDepartment()))
                        .collect(Collectors.toList());
                log.info("After product filter: {} targets", targets.size());
            }
            if (organization != null && !organization.isEmpty()) {
                Set<String> orgSet = new HashSet<>(Arrays.asList(organization.split(",")));
                targets = targets.stream()
                        .filter(t -> orgSet.contains(t.getOrganization()))
                        .collect(Collectors.toList());
                log.info("After organization filter: {} targets", targets.size());
            }
            if (owner != null && !owner.isEmpty()) {
                targets = targets.stream()
                        .filter(t -> owner.equals(t.getOwner()))
                        .collect(Collectors.toList());
                log.info("After owner filter: {} targets", targets.size());
            }
            if (subCategory != null && !subCategory.isEmpty()) {
                Set<String> subSet = new HashSet<>(Arrays.asList(subCategory.split(",")));
                targets = targets.stream()
                        .filter(t -> subSet.contains(t.getSubCategory()))
                        .collect(Collectors.toList());
                log.info("After subCategory filter: {} targets", targets.size());
            }
        }

        // 权限过滤：根据当前用户过滤可见数据
        String currentUser = UserContext.get();
        if (currentUser != null && !currentUser.isEmpty() && !"admin".equals(currentUser)) {
            UserConfig userConfig = userConfigRepository.findByUsername(currentUser).orElse(null);
            if (userConfig != null) {
                if ("DEPT_LEADER".equals(userConfig.getRole())) {
                    String dept = userConfig.getDepartment();
                    targets = targets.stream()
                            .filter(t -> dept != null && dept.equals(t.getDepartment()))
                            .collect(Collectors.toList());
                } else if ("ORG_LEADER".equals(userConfig.getRole())) {
                    String org = userConfig.getOrganization();
                    targets = targets.stream()
                            .filter(t -> org != null && org.equals(t.getOrganization()))
                            .collect(Collectors.toList());
                } else if ("USER".equals(userConfig.getRole())) {
                    targets = targets.stream()
                            .filter(t -> currentUser.equals(t.getOwner()))
                            .collect(Collectors.toList());
                }
            }
        }

        // 预加载明细聚合数据
        Map<String, BigDecimal> signingMap = new HashMap<>();
        Map<String, BigDecimal> recognitionMap = new HashMap<>();
        Map<String, BigDecimal> revenueMap = new HashMap<>();
        try {
            List<Object[]> signingSums = signingRepository.sumAmountByOrgYearQuarter();
            for (Object[] row : signingSums) {
                String key = row[0] + "_" + row[1] + "_" + row[2] + "_" + row[3];
                signingMap.put(key, row[4] != null ? new BigDecimal(row[4].toString()) : BigDecimal.ZERO);
            }
            List<Object[]> recSums = recognitionRepository.sumAmountsByOrgYearQuarter();
            for (Object[] row : recSums) {
                String key = row[0] + "_" + row[1] + "_" + row[2] + "_" + row[3];
                recognitionMap.put(key, row[4] != null ? new BigDecimal(row[4].toString()) : BigDecimal.ZERO);
                revenueMap.put(key, row[5] != null ? new BigDecimal(row[5].toString()) : BigDecimal.ZERO);
            }
        } catch (Exception e) {
            System.out.println("========== DEBUG 加载明细聚合数据失败: " + e.getMessage());
            e.printStackTrace();
            log.warn("加载明细聚合数据失败: {}", e.getMessage());
        }
        
        if (targets.isEmpty()) {
            return TargetStatisticsResponse.builder()
                    .dimension(dimension)
                    .year(year)
                    .statistics(new ArrayList<>())
                    .summary(TargetStatisticsResponse.Summary.builder()
                            .totalTarget(BigDecimal.ZERO)
                            .totalActual(BigDecimal.ZERO)
                            .averageCompletionRate(BigDecimal.ZERO)
                            .totalDimensions(0)
                            .build())
                    .build();
        }

        List<TargetStatisticsResponse.DimensionStatistics> statisticsList = new ArrayList<>();
        
        System.out.println("========== DEBUG: Starting to process " + targets.size() + " targets");
        System.out.println("========== DEBUG: Targets list size: " + targets.size());
        BigDecimal totalTarget = BigDecimal.ZERO;
        BigDecimal totalActual = BigDecimal.ZERO;
        BigDecimal totalSigningTarget = BigDecimal.ZERO;
        BigDecimal totalSigningActual = BigDecimal.ZERO;
        BigDecimal totalConfirmationTarget = BigDecimal.ZERO;
        BigDecimal totalConfirmationActual = BigDecimal.ZERO;
        BigDecimal totalBudgetTarget = BigDecimal.ZERO;
        BigDecimal totalBudgetActual = BigDecimal.ZERO;
        
        int count = 0;
        for (Target target : targets) {
            count++;
            System.out.println("========== DEBUG: Processing target " + count + "/" + targets.size() + ": " + target.getSubCategory() + " - " + target.getOrganization());
            String departmentName = target.getDepartment() != null ? target.getDepartment() : "";
            String organizationName = target.getOrganization() != null ? target.getOrganization() : "";
            String category = target.getCategory() != null ? target.getCategory() : "";
            String subCategoryName = target.getSubCategory() != null ? target.getSubCategory() : "";
            String ownerName = target.getOwner() != null ? target.getOwner() : "";
            String targetType = target.getTargetType() != null ? target.getTargetType() : "";
            
            BigDecimal signingTarget = BigDecimal.ZERO;
            BigDecimal signingActual = BigDecimal.ZERO;
            BigDecimal confirmationTarget = BigDecimal.ZERO;
            BigDecimal confirmationActual = BigDecimal.ZERO;
            Integer rdTarget = 0;
            BigDecimal budgetTarget = BigDecimal.ZERO;
            BigDecimal budgetActual = BigDecimal.ZERO;
            Integer rdActual = 0;
            Integer rdPlanned = 0;
            
            if (subCategoryName.contains("签约")) {
                signingTarget = target.getAnnualTarget() != null ? target.getAnnualTarget() : BigDecimal.ZERO;
                // 优先从签约明细聚合取实际值（按机构+年度+季度+细分目标匹配）
                BigDecimal detailSigning = BigDecimal.ZERO;
                for (int q = 1; q <= 4; q++) {
                    String key = organizationName + "_" + year + "_" + q + "_" + subCategoryName;
                    BigDecimal val = signingMap.get(key);
                    if (val != null) detailSigning = detailSigning.add(val);
                }
                if (detailSigning.compareTo(BigDecimal.ZERO) > 0) {
                    signingActual = detailSigning;
                } else {
                    signingActual = q1a(target).add(q2a(target)).add(q3a(target)).add(q4a(target));
                }
            } else if (subCategoryName.contains("确权")) {
                confirmationTarget = target.getAnnualTarget() != null ? target.getAnnualTarget() : BigDecimal.ZERO;
                BigDecimal detailRev = BigDecimal.ZERO;
                for (int q = 1; q <= 4; q++) {
                    String key = organizationName + "_" + year + "_" + q + "_" + subCategoryName;
                    BigDecimal val = revenueMap.get(key);
                    if (val != null) detailRev = detailRev.add(val);
                }
                if (detailRev.compareTo(BigDecimal.ZERO) > 0) {
                    confirmationActual = detailRev;
                } else {
                    confirmationActual = q1a(target).add(q2a(target)).add(q3a(target)).add(q4a(target));
                }
            } else if ("研发成果".equals(subCategoryName)) {
                rdActual = calculateRdActualForOrganization(organizationName, year);
                rdPlanned = calculateRdPlannedForOrganization(organizationName, year);
                rdTarget = rdActual + rdPlanned;
            } else if ("费用".equals(subCategoryName)) {
                budgetTarget = target.getAnnualTarget() != null ? target.getAnnualTarget() : BigDecimal.ZERO;
                budgetActual = q1a(target).add(q2a(target)).add(q3a(target)).add(q4a(target));
            }
            
            BigDecimal dimensionTarget = signingTarget.add(confirmationTarget).add(budgetTarget);
            BigDecimal dimensionActual = signingActual.add(confirmationActual).add(budgetActual);

            if ("研发成果".equals(subCategoryName)) {
                dimensionTarget = dimensionTarget.add(new BigDecimal(rdTarget));
                dimensionActual = dimensionActual.add(new BigDecimal(rdActual));
            }

            BigDecimal completionRate = BigDecimal.ZERO;
            if (dimensionTarget.compareTo(BigDecimal.ZERO) > 0) {
                completionRate = dimensionActual.divide(dimensionTarget, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
            }
            
            List<TargetStatisticsResponse.QuarterlyData> quarterlyData = calculateQuarterlyDataForTarget(target, year);
            
            statisticsList.add(TargetStatisticsResponse.DimensionStatistics.builder()
                    .id(target.getId())
                    .name(subCategory)
                    .departmentName(departmentName)
                    .productName("")
                    .organizationName(organizationName)
                    .category(category)
                    .subCategory(subCategoryName)
                    .targetType(targetType)
                    .owner(ownerName)
                    .target(dimensionTarget)
                    .actual(dimensionActual)
                    .completionRate(completionRate)
                    .quarterlyData(quarterlyData)
                    .signingTarget(signingTarget)
                    .signingActual(signingActual)
                    .confirmationTarget(confirmationTarget)
                    .confirmationActual(confirmationActual)
                    .rdTarget(rdTarget)
                    .rdActual(rdActual)
                    .rdPlanned(rdPlanned)
                    .budgetTarget(budgetTarget)
                    .budgetActual(budgetActual)
                    .annualTarget("研发成果".equals(subCategoryName)
                            ? new BigDecimal(rdTarget)
                            : (target.getAnnualTarget() != null ? target.getAnnualTarget() : BigDecimal.ZERO))
                    .actualValue("研发成果".equals(subCategoryName)
                            ? new BigDecimal(rdActual)
                            : signingActual.add(confirmationActual).add(budgetActual))
                    .q1Target(target.getQ1Target() != null ? target.getQ1Target() : BigDecimal.ZERO)
                    .q2Target(target.getQ2Target() != null ? target.getQ2Target() : BigDecimal.ZERO)
                    .q3Target(target.getQ3Target() != null ? target.getQ3Target() : BigDecimal.ZERO)
                    .q4Target(target.getQ4Target() != null ? target.getQ4Target() : BigDecimal.ZERO)
                    .q1Actual(detailQuarterActual(organizationName, year, 1, signingMap, recognitionMap, revenueMap, subCategoryName, target))
                    .q2Actual(detailQuarterActual(organizationName, year, 2, signingMap, recognitionMap, revenueMap, subCategoryName, target))
                    .q3Actual(detailQuarterActual(organizationName, year, 3, signingMap, recognitionMap, revenueMap, subCategoryName, target))
                    .q4Actual(detailQuarterActual(organizationName, year, 4, signingMap, recognitionMap, revenueMap, subCategoryName, target))
                    .build());
            
            totalTarget = totalTarget.add(dimensionTarget);
            totalActual = totalActual.add(dimensionActual);
            totalSigningTarget = totalSigningTarget.add(signingTarget);
            totalSigningActual = totalSigningActual.add(signingActual);
            totalConfirmationTarget = totalConfirmationTarget.add(confirmationTarget);
            totalConfirmationActual = totalConfirmationActual.add(confirmationActual);
            totalBudgetTarget = totalBudgetTarget.add(budgetTarget);
            totalBudgetActual = totalBudgetActual.add(budgetActual);
        }
        
        BigDecimal averageCompletionRate = BigDecimal.ZERO;
        if (totalTarget.compareTo(BigDecimal.ZERO) > 0) {
            averageCompletionRate = totalActual.divide(totalTarget, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }
        
        BigDecimal budgetRate = BigDecimal.ZERO;
        if (totalBudgetTarget.compareTo(BigDecimal.ZERO) > 0) {
            budgetRate = totalBudgetActual.divide(totalBudgetTarget, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        BigDecimal signingRate = BigDecimal.ZERO;
        if (totalSigningTarget.compareTo(BigDecimal.ZERO) > 0) {
            signingRate = totalSigningActual.divide(totalSigningTarget, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        BigDecimal confirmationRate = BigDecimal.ZERO;
        if (totalConfirmationTarget.compareTo(BigDecimal.ZERO) > 0) {
            confirmationRate = totalConfirmationActual.divide(totalConfirmationTarget, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        TargetStatisticsResponse.Summary summary = TargetStatisticsResponse.Summary.builder()
                .totalTarget(totalTarget)
                .totalActual(totalActual)
                .averageCompletionRate(averageCompletionRate)
                .totalDimensions(statisticsList.size())
                .signingTarget(totalSigningTarget)
                .signingActual(totalSigningActual)
                .signingRate(signingRate)
                .confirmationTarget(totalConfirmationTarget)
                .confirmationActual(totalConfirmationActual)
                .confirmationRate(confirmationRate)
                .budgetTarget(totalBudgetTarget)
                .budgetActual(totalBudgetActual)
                .budgetRate(budgetRate)
                .build();
        
        Integer totalRdActual = calculateTotalRdActual(year);
        Integer totalRdPlanned = calculateTotalRdPlanned();

        return TargetStatisticsResponse.builder()
                .dimension(dimension)
                .year(year)
                .statistics(statisticsList)
                .summary(summary)
                .rdActual(totalRdActual)
                .rdPlanned(totalRdPlanned)
                .rdTarget(totalRdActual + totalRdPlanned)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlyDistributionResponse getMonthlyDistribution(Integer year) {
        List<MonthlyDistributionResponse.MonthlyData> monthlyDataList = new ArrayList<>();
        int totalCount = 0;
        int maxCount = Integer.MIN_VALUE;
        int minCount = Integer.MAX_VALUE;
        int maxMonth = 1;
        int minMonth = 1;
        
        for (int month = 1; month <= 12; month++) {
            final int currentMonth = month;
            
            List<Achievement> allAchievements = achievementRepository.findAll();
            
            List<Achievement> preRegisterAchievements = allAchievements.stream()
                    .filter(a -> a.getStatus() == AchievementStatus.PRE_REGISTER)
                    .filter(a -> a.getPlannedAcceptanceDate() != null)
                    .filter(a -> a.getPlannedAcceptanceDate().getYear() == year)
                    .filter(a -> a.getPlannedAcceptanceDate().getMonthValue() == currentMonth)
                    .collect(Collectors.toList());
            
            List<Achievement> registerAchievements = allAchievements.stream()
                    .filter(a -> a.getStatus() == AchievementStatus.REGISTER)
                    .filter(a -> a.getPlannedAcceptanceDate() != null)
                    .filter(a -> a.getPlannedAcceptanceDate().getYear() == year)
                    .filter(a -> a.getPlannedAcceptanceDate().getMonthValue() == currentMonth)
                    .collect(Collectors.toList());
            
            List<Achievement> recordedAchievements = allAchievements.stream()
                    .filter(a -> a.getStatus() == AchievementStatus.RECORDED)
                    .filter(a -> a.getActualAcceptanceDate() != null)
                    .filter(a -> a.getActualAcceptanceDate().getYear() == year)
                    .filter(a -> a.getActualAcceptanceDate().getMonthValue() == currentMonth)
                    .collect(Collectors.toList());
            
            List<Achievement> allMonthAchievements = new ArrayList<>();
            allMonthAchievements.addAll(preRegisterAchievements);
            allMonthAchievements.addAll(registerAchievements);
            allMonthAchievements.addAll(recordedAchievements);
            
            int count = allMonthAchievements.size();
            totalCount += count;
            
            if (count > maxCount) {
                maxCount = count;
                maxMonth = month;
            }
            if (count < minCount) {
                minCount = count;
                minMonth = month;
            }
            
            Map<String, Long> orgCounts = allMonthAchievements.stream()
                    .collect(Collectors.groupingBy(
                            a -> a.getOrganizationName() != null ? a.getOrganizationName() : "未知组织",
                            Collectors.counting()
                    ));
            
            List<MonthlyDistributionResponse.OrganizationData> orgDataList = orgCounts.entrySet().stream()
                    .map(e -> MonthlyDistributionResponse.OrganizationData.builder()
                            .organization(e.getKey())
                            .count(e.getValue().intValue())
                            .build())
                    .sorted(Comparator.comparing(MonthlyDistributionResponse.OrganizationData::getCount).reversed())
                    .collect(Collectors.toList());
            
            List<MonthlyDistributionResponse.StatusData> statusDataList = new ArrayList<>();
            if (!preRegisterAchievements.isEmpty()) {
                statusDataList.add(MonthlyDistributionResponse.StatusData.builder()
                        .status("PRE_REGISTER")
                        .count(preRegisterAchievements.size())
                        .build());
            }
            if (!registerAchievements.isEmpty()) {
                statusDataList.add(MonthlyDistributionResponse.StatusData.builder()
                        .status("REGISTER")
                        .count(registerAchievements.size())
                        .build());
            }
            if (!recordedAchievements.isEmpty()) {
                statusDataList.add(MonthlyDistributionResponse.StatusData.builder()
                        .status("RECORDED")
                        .count(recordedAchievements.size())
                        .build());
            }
            
            monthlyDataList.add(MonthlyDistributionResponse.MonthlyData.builder()
                    .month(month)
                    .count(count)
                    .byOrganization(orgDataList)
                    .byStatus(statusDataList)
                    .build());
        }
        
        double averagePerMonth = totalCount / 12.0;
        
        MonthlyDistributionResponse.TotalSummary summary = MonthlyDistributionResponse.TotalSummary.builder()
                .totalCount(totalCount)
                .averagePerMonth(averagePerMonth)
                .maxMonth(maxMonth)
                .maxCount(maxCount)
                .minMonth(minMonth)
                .minCount(minCount)
                .build();
        
        return MonthlyDistributionResponse.builder()
                .year(year)
                .monthlyData(monthlyDataList)
                .summary(summary)
                .build();
    }

    @Override
    public boolean saveActualData(ActualDataRequest request) {
        ActualData actualData = new ActualData();
        actualData.setId(UUID.randomUUID().toString());
        actualData.setTargetId(request.getTargetId());
        actualData.setOrganization(request.getOrganization());
        actualData.setDataType(request.getDataType());
        actualData.setYear(request.getYear());
        actualData.setMonth(request.getMonth());
        actualData.setActualValue(request.getActualValue());
        actualData.setRemark(request.getRemark());
        actualData.setCreatedBy(request.getCreatedBy());
        
        actualDataRepository.save(actualData);
        return true;
    }

    @Override
    public String importTargets(MultipartFile file) {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int successCount = 0;
            int errorCount = 0;
            
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                try {
                    Target target = new Target();
                    target.setId(UUID.randomUUID().toString());
                    target.setDepartment(getCellValueAsString(row.getCell(0)));
                    target.setOrganization(getCellValueAsString(row.getCell(1)));
                    target.setCategory(getCellValueAsString(row.getCell(2)));
                    target.setSubCategory(getCellValueAsString(row.getCell(3)));
                    target.setTargetType(getCellValueAsString(row.getCell(4)));
                    target.setYear(getCellValueAsInteger(row.getCell(5)));
                    target.setAnnualTarget(getCellValueAsBigDecimal(row.getCell(6)));
                    target.setQ1Target(getCellValueAsBigDecimal(row.getCell(7)));
                    target.setQ2Target(getCellValueAsBigDecimal(row.getCell(8)));
                    target.setQ3Target(getCellValueAsBigDecimal(row.getCell(9)));
                    target.setQ4Target(getCellValueAsBigDecimal(row.getCell(10)));
                    target.setOwner(getCellValueAsString(row.getCell(11)));
                    
                    targetRepository.save(target);
                    successCount++;
                } catch (Exception e) {
                    log.error("Error importing target at row {}: {}", i + 1, e.getMessage());
                    errorCount++;
                }
            }
            
            return String.format("导入完成: 成功 %d 条, 失败 %d 条", successCount, errorCount);
        } catch (IOException e) {
            log.error("Error reading Excel file", e);
            return "导入失败: " + e.getMessage();
        }
    }

    private Map<String, List<Target>> groupByDimension(List<Target> targets, String dimension) {
        switch (dimension.toLowerCase()) {
            case "organization":
                return targets.stream()
                        .collect(Collectors.groupingBy(
                                t -> t.getOrganization() != null ? t.getOrganization() : "未知组织",
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));
            case "owner":
                return targets.stream()
                        .collect(Collectors.groupingBy(
                                t -> t.getOwner() != null ? t.getOwner() : "未知负责人",
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));
            case "product":
            default:
                return targets.stream()
                        .collect(Collectors.groupingBy(
                                t -> {
                                    String dept = t.getDepartment() != null ? t.getDepartment() : "未知部门";
                                    String org = t.getOrganization() != null ? t.getOrganization() : "未知机构";
                                    return dept + "|" + org;
                                },
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));
        }
    }

    private BigDecimal calculateActualFromAchievements(String dimensionName, String dimension, Integer year) {
        List<Achievement> achievements = achievementRepository.findAll().stream()
                .filter(a -> a.getStatus() == AchievementStatus.RECORDED)
                .filter(a -> a.getRecordTime() != null && a.getRecordTime().getYear() == year)
                .filter(a -> {
                    switch (dimension.toLowerCase()) {
                        case "organization":
                            return dimensionName.equals(a.getOrganizationName() != null ? a.getOrganizationName() : "未知组织");
                        case "owner":
                            return dimensionName.equals(a.getOwner() != null ? a.getOwner() : "未知负责人");
                        case "product":
                        default:
                            String dept = a.getDepartmentName() != null ? a.getDepartmentName() : "未知部门";
                            String org = a.getOrganizationName() != null ? a.getOrganizationName() : "未知机构";
                            String productKey = dept + "|" + org;
                            return dimensionName.equals(productKey);
                    }
                })
                .collect(Collectors.toList());
        
        return new BigDecimal(achievements.size());
    }

    private Integer calculateRdActual(String dimensionName, String dimension, Integer year) {
        List<ActualData> actualDataList = actualDataRepository.findAll().stream()
                .filter(a -> "rd".equals(a.getDataType()))
                .filter(a -> a.getYear().equals(year))
                .filter(a -> dimensionName.equals(a.getOrganization()))
                .collect(Collectors.toList());
        
        if (!actualDataList.isEmpty()) {
            return actualDataList.stream()
                    .mapToInt(a -> a.getActualValue() != null ? a.getActualValue().intValue() : 0)
                    .sum();
        }
        
        return calculateRdActualFromAchievements(dimensionName, dimension, year);
    }

    private Integer calculateRdActualFromAchievements(String dimensionName, String dimension, Integer year) {
        List<Achievement> achievements = achievementRepository.findAll().stream()
                .filter(a -> {
                    if (a.getStatus() == AchievementStatus.RECORDED) {
                        return a.getRecordTime() != null && a.getRecordTime().getYear() == year;
                    } else if (a.getStatus() == AchievementStatus.REGISTER) {
                        return a.getRegisterTime() != null && a.getRegisterTime().getYear() == year;
                    }
                    return false;
                })
                .filter(a -> {
                    switch (dimension.toLowerCase()) {
                        case "organization":
                            return dimensionName.equals(a.getOrganizationName() != null ? a.getOrganizationName() : "未知组织");
                        case "owner":
                            return dimensionName.equals(a.getOwner() != null ? a.getOwner() : "未知负责人");
                        case "product":
                        default:
                            String dept = a.getDepartmentName() != null ? a.getDepartmentName() : "未知部门";
                            String org = a.getOrganizationName() != null ? a.getOrganizationName() : "未知机构";
                            String productKey = dept + "|" + org;
                            return dimensionName.equals(productKey);
                    }
                })
                .collect(Collectors.toList());
        
        return achievements.size();
    }

    private Integer calculateRdActualForOrganization(String organizationName, Integer year) {
        List<Achievement> achievements = achievementRepository.findAll().stream()
                .filter(a -> a.getActualAcceptanceDate() != null)
                .filter(a -> a.getStatus() == AchievementStatus.RECORDED)
                .filter(a -> a.getActualAcceptanceDate().getYear() == year)
                .filter(a -> organizationName.equals(a.getOrganizationName() != null ? a.getOrganizationName() : "未知组织"))
                .collect(Collectors.toList());

        return achievements.size();
    }

    private Integer calculateRdPlannedForOrganization(String organizationName, Integer year) {
        List<Achievement> achievements = achievementRepository.findAll().stream()
                .filter(a -> a.getActualAcceptanceDate() == null)
                .filter(a -> organizationName.equals(a.getOrganizationName() != null ? a.getOrganizationName() : "未知组织"))
                .collect(Collectors.toList());

        return achievements.size();
    }

    private Integer calculateTotalRdActual(Integer year) {
        List<Achievement> achievements = achievementRepository.findAll().stream()
                .filter(a -> a.getActualAcceptanceDate() != null)
                .filter(a -> a.getActualAcceptanceDate().getYear() == year)
                .filter(a -> a.getStatus() == AchievementStatus.RECORDED)
                .collect(Collectors.toList());
        return achievements.size();
    }

    private Integer calculateTotalRdPlanned() {
        List<Achievement> achievements = achievementRepository.findAll().stream()
                .filter(a -> a.getActualAcceptanceDate() == null)
                .collect(Collectors.toList());
        return achievements.size();
    }

    private List<TargetStatisticsResponse.QuarterlyData> calculateQuarterlyDataForTarget(Target target, Integer year) {
        List<TargetStatisticsResponse.QuarterlyData> quarterlyData = new ArrayList<>();
        
        String[] quarterNames = {"Q1", "Q2", "Q3", "Q4"};
        BigDecimal[] quarterTargets = {
                target.getQ1Target() != null ? target.getQ1Target() : BigDecimal.ZERO,
                target.getQ2Target() != null ? target.getQ2Target() : BigDecimal.ZERO,
                target.getQ3Target() != null ? target.getQ3Target() : BigDecimal.ZERO,
                target.getQ4Target() != null ? target.getQ4Target() : BigDecimal.ZERO
        };
        
        int[][] quarters = {{1, 3}, {4, 6}, {7, 9}, {10, 12}};
        
        for (int i = 0; i < 4; i++) {
            final int startMonth = quarters[i][0];
            final int endMonth = quarters[i][1];
            
            String subCategoryName2 = target.getSubCategory() != null ? target.getSubCategory() : "";
            long quarterActual = 0;
            
            if ("研发成果".equals(subCategoryName2)) {
                String organizationName = target.getOrganization() != null ? target.getOrganization() : "";
                quarterActual = achievementRepository.findAll().stream()
                        .filter(a -> a.getActualAcceptanceDate() != null)
                        .filter(a -> a.getStatus() == AchievementStatus.RECORDED)
                        .filter(a -> a.getActualAcceptanceDate().getYear() == year)
                        .filter(a -> a.getActualAcceptanceDate().getMonthValue() >= startMonth
                                && a.getActualAcceptanceDate().getMonthValue() <= endMonth)
                        .filter(a -> organizationName.equals(a.getOrganizationName() != null ? a.getOrganizationName() : "未知组织"))
                        .count();
            }
            
            BigDecimal actual = new BigDecimal(quarterActual);
            BigDecimal completionRate = BigDecimal.ZERO;
            if (quarterTargets[i].compareTo(BigDecimal.ZERO) > 0) {
                completionRate = actual.divide(quarterTargets[i], 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
            }
            
            quarterlyData.add(TargetStatisticsResponse.QuarterlyData.builder()
                    .quarter(quarterNames[i])
                    .target(quarterTargets[i])
                    .actual(actual)
                    .completionRate(completionRate)
                    .build());
        }
        
        return quarterlyData;
    }

    private List<TargetStatisticsResponse.QuarterlyData> calculateQuarterlyData(
            List<Target> targets, String dimensionName, String dimension, Integer year) {
        List<TargetStatisticsResponse.QuarterlyData> quarterlyData = new ArrayList<>();
        
        int[][] quarters = {{1, 3}, {4, 6}, {7, 9}, {10, 12}};
        String[] quarterNames = {"Q1", "Q2", "Q3", "Q4"};
        BigDecimal[] quarterTargets = {
                targets.stream().map(t -> t.getQ1Target() != null ? t.getQ1Target() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                targets.stream().map(t -> t.getQ2Target() != null ? t.getQ2Target() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                targets.stream().map(t -> t.getQ3Target() != null ? t.getQ3Target() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                targets.stream().map(t -> t.getQ4Target() != null ? t.getQ4Target() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        };
        
        for (int i = 0; i < 4; i++) {
            final int startMonth = quarters[i][0];
            final int endMonth = quarters[i][1];
            
            long quarterActual = achievementRepository.findAll().stream()
                    .filter(a -> a.getStatus() == AchievementStatus.RECORDED)
                    .filter(a -> a.getRecordTime() != null && a.getRecordTime().getYear() == year)
                    .filter(a -> a.getRecordTime().getMonthValue() >= startMonth 
                            && a.getRecordTime().getMonthValue() <= endMonth)
                    .filter(a -> {
                        switch (dimension.toLowerCase()) {
                            case "organization":
                                return dimensionName.equals(a.getOrganizationName() != null ? a.getOrganizationName() : "未知组织");
                            case "owner":
                                return dimensionName.equals(a.getOwner() != null ? a.getOwner() : "未知负责人");
                            case "product":
                            default:
                                String dept = a.getDepartmentName() != null ? a.getDepartmentName() : "未知部门";
                                String org = a.getOrganizationName() != null ? a.getOrganizationName() : "未知机构";
                                String productKey = dept + "|" + org;
                                return dimensionName.equals(productKey);
                        }
                    })
                    .count();
            
            BigDecimal actual = new BigDecimal(quarterActual);
            BigDecimal completionRate = BigDecimal.ZERO;
            if (quarterTargets[i].compareTo(BigDecimal.ZERO) > 0) {
                completionRate = actual.divide(quarterTargets[i], 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
            }
            
            quarterlyData.add(TargetStatisticsResponse.QuarterlyData.builder()
                    .quarter(quarterNames[i])
                    .target(quarterTargets[i])
                    .actual(actual)
                    .completionRate(completionRate)
                    .build());
        }
        
        return quarterlyData;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }

    private Integer getCellValueAsInteger(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        }
        return null;
    }

    private BigDecimal getCellValueAsBigDecimal(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        return null;
    }

    @Override
    public List<String> getAllProducts() {
        return targetRepository.findAll().stream()
                .map(Target::getDepartment)
                .filter(dept -> dept != null && !dept.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getAllOrganizations() {
        return targetRepository.findAll().stream()
                .map(Target::getOrganization)
                .filter(org -> org != null && !org.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getAllOwners() {
        return targetRepository.findAll().stream()
                .map(Target::getOwner)
                .filter(owner -> owner != null && !owner.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean createTarget(CreateTargetRequest request) {
        Target target = new Target();
        target.setId(UUID.randomUUID().toString());
        target.setDepartment(request.getDepartment());
        target.setOrganization(request.getOrganization());
        target.setCategory(request.getCategory());
        target.setSubCategory(request.getSubCategory());
        target.setTargetType(request.getTargetType());
        target.setYear(request.getYear());

        BigDecimal q1 = request.getQ1Target() != null ? request.getQ1Target() : BigDecimal.ZERO;
        BigDecimal q2 = request.getQ2Target() != null ? request.getQ2Target() : BigDecimal.ZERO;
        BigDecimal q3 = request.getQ3Target() != null ? request.getQ3Target() : BigDecimal.ZERO;
        BigDecimal q4 = request.getQ4Target() != null ? request.getQ4Target() : BigDecimal.ZERO;
        target.setAnnualTarget(q1.add(q2).add(q3).add(q4));
        target.setQ1Target(request.getQ1Target());
        target.setQ2Target(request.getQ2Target());
        target.setQ3Target(request.getQ3Target());
        target.setQ4Target(request.getQ4Target());
        target.setQ1Actual(request.getQ1Actual());
        target.setQ2Actual(request.getQ2Actual());
        target.setQ3Actual(request.getQ3Actual());
        target.setQ4Actual(request.getQ4Actual());
        target.setOwner(request.getOwner());

        targetRepository.save(target);
        return true;
    }

    @Override
    @Transactional
    public boolean updateTarget(String id, CreateTargetRequest request) {
        Target target = targetRepository.findById(id).orElse(null);
        if (target == null) {
            return false;
        }
        target.setDepartment(request.getDepartment());
        target.setOrganization(request.getOrganization());
        target.setCategory(request.getCategory());
        target.setSubCategory(request.getSubCategory());
        target.setTargetType(request.getTargetType());
        target.setYear(request.getYear());

        BigDecimal q1 = request.getQ1Target() != null ? request.getQ1Target() : BigDecimal.ZERO;
        BigDecimal q2 = request.getQ2Target() != null ? request.getQ2Target() : BigDecimal.ZERO;
        BigDecimal q3 = request.getQ3Target() != null ? request.getQ3Target() : BigDecimal.ZERO;
        BigDecimal q4 = request.getQ4Target() != null ? request.getQ4Target() : BigDecimal.ZERO;
        target.setAnnualTarget(q1.add(q2).add(q3).add(q4));
        target.setQ1Target(request.getQ1Target());
        target.setQ2Target(request.getQ2Target());
        target.setQ3Target(request.getQ3Target());
        target.setQ4Target(request.getQ4Target());
        target.setQ1Actual(request.getQ1Actual());
        target.setQ2Actual(request.getQ2Actual());
        target.setQ3Actual(request.getQ3Actual());
        target.setQ4Actual(request.getQ4Actual());
        target.setOwner(request.getOwner());

        targetRepository.save(target);
        return true;
    }

    @Override
    @Transactional
    public boolean deleteTarget(String id) {
        if (!targetRepository.existsById(id)) {
            return false;
        }
        targetRepository.deleteById(id);
        return true;
    }

    @Override
    public Map<String, List<TargetStatisticsResponse.QuarterlyData>> getQuarterlySummary(Integer year, String organization) {
        Map<String, List<TargetStatisticsResponse.QuarterlyData>> result = new java.util.LinkedHashMap<>();

        List<Target> targets = jdbcTemplate.query(
                "SELECT * FROM targets WHERE year = ?",
                new Object[]{year},
                (rs, rowNum) -> {
                    Target t = new Target();
                    t.setId(rs.getString("id"));
                    t.setOrganization(rs.getString("organization"));
                    t.setSubCategory(rs.getString("sub_category"));
                    t.setQ1Target(rs.getBigDecimal("q1_target"));
                    t.setQ2Target(rs.getBigDecimal("q2_target"));
                    t.setQ3Target(rs.getBigDecimal("q3_target"));
                    t.setQ4Target(rs.getBigDecimal("q4_target"));
                    t.setQ1Actual(rs.getBigDecimal("q1_actual"));
                    t.setQ2Actual(rs.getBigDecimal("q2_actual"));
                    t.setQ3Actual(rs.getBigDecimal("q3_actual"));
                    t.setQ4Actual(rs.getBigDecimal("q4_actual"));
                    return t;
                });

        if (organization != null && !organization.isEmpty()) {
            Set<String> orgSet = new HashSet<>(Arrays.asList(organization.split(",")));
            targets = targets.stream()
                    .filter(t -> orgSet.contains(t.getOrganization()))
                    .collect(Collectors.toList());
        }

        result.put("signing", aggregateQuarterlyData(targets, "签约"));
        result.put("confirmation", aggregateQuarterlyData(targets, "确权"));
        result.put("budget", aggregateBudgetData(targets));

        return result;
    }

    private List<TargetStatisticsResponse.QuarterlyData> aggregateQuarterlyData(List<Target> targets, String keyword) {
        List<Target> filtered = targets.stream()
                .filter(t -> t.getSubCategory() != null && t.getSubCategory().contains(keyword))
                .collect(Collectors.toList());

        String[] quarterNames = {"Q1", "Q2", "Q3", "Q4"};
        List<TargetStatisticsResponse.QuarterlyData> result = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            BigDecimal sumTarget = BigDecimal.ZERO;
            BigDecimal sumActual = BigDecimal.ZERO;
            for (Target t : filtered) {
                BigDecimal[] targets_q = {t.getQ1Target(), t.getQ2Target(), t.getQ3Target(), t.getQ4Target()};
                BigDecimal[] actuals = {t.getQ1Actual(), t.getQ2Actual(), t.getQ3Actual(), t.getQ4Actual()};
                sumTarget = sumTarget.add(targets_q[i] != null ? targets_q[i] : BigDecimal.ZERO);
                sumActual = sumActual.add(actuals[i] != null ? actuals[i] : BigDecimal.ZERO);
            }
            BigDecimal rate = BigDecimal.ZERO;
            if (sumTarget.compareTo(BigDecimal.ZERO) > 0) {
                rate = sumActual.divide(sumTarget, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
            }
            result.add(TargetStatisticsResponse.QuarterlyData.builder()
                    .quarter(quarterNames[i])
                    .target(sumTarget)
                    .actual(sumActual)
                    .completionRate(rate)
                    .build());
        }
        return result;
    }

    private List<TargetStatisticsResponse.QuarterlyData> aggregateBudgetData(List<Target> targets) {
        return aggregateQuarterlyData(targets, "费用");
    }

    @Override
    public String testDatabaseCount() {
        long jpaCount = targetRepository.count();
        Integer sqlCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM targets WHERE year=2026", Integer.class);
        Integer totalCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM targets", Integer.class);
        return String.format("JPA count: %d, SQL count for 2026: %d, Total SQL count: %d", jpaCount, sqlCount, totalCount);
    }

    private static BigDecimal q1a(Target t) { return t.getQ1Actual() != null ? t.getQ1Actual() : BigDecimal.ZERO; }
    private static BigDecimal q2a(Target t) { return t.getQ2Actual() != null ? t.getQ2Actual() : BigDecimal.ZERO; }
    private static BigDecimal q3a(Target t) { return t.getQ3Actual() != null ? t.getQ3Actual() : BigDecimal.ZERO; }
    private static BigDecimal q4a(Target t) { return t.getQ4Actual() != null ? t.getQ4Actual() : BigDecimal.ZERO; }

    private BigDecimal detailQuarterActual(String org, int year, int quarter,
            Map<String, BigDecimal> signingMap, Map<String, BigDecimal> recognitionMap,
            Map<String, BigDecimal> revenueMap, String subCategory, Target target) {
        String key = org + "_" + year + "_" + quarter + "_" + subCategory;
        BigDecimal fallback = qActual(target, quarter);
        if (subCategory.contains("签约")) {
            BigDecimal val = signingMap.get(key);
            return val != null ? val : fallback;
        }
        if (subCategory.contains("确权")) {
            BigDecimal val = revenueMap.get(key);
            return val != null ? val : fallback;
        }
        if ("费用".equals(subCategory)) {
            return fallback;
        }
        return fallback;
    }

    private static BigDecimal qActual(Target t, int quarter) {
        BigDecimal val = null;
        switch (quarter) {
            case 1: val = t.getQ1Actual(); break;
            case 2: val = t.getQ2Actual(); break;
            case 3: val = t.getQ3Actual(); break;
            case 4: val = t.getQ4Actual(); break;
        }
        return val != null ? val : BigDecimal.ZERO;
    }
}
