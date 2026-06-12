package com.example.achievement.service;

import com.example.achievement.dto.request.ActualDataRequest;
import com.example.achievement.dto.request.CreateTargetRequest;
import com.example.achievement.dto.response.MonthlyDistributionResponse;
import com.example.achievement.dto.response.TargetStatisticsResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TargetService {

    /**
     * 获取目标统计数据
     * @param dimension 维度: product/organization/owner
     * @param year 年份
     * @param product 产品筛选（可选）
     * @param organization 机构筛选（可选）
     * @param owner 负责人筛选（可选）
     * @return 目标统计响应
     */
    TargetStatisticsResponse getStatistics(String dimension, Integer year, String product, String organization, String owner, String subCategory);

    /**
     * 获取月度成果分布
     * @param year 年份
     * @param department 部门筛选（可选）
     * @param organization 机构筛选（可选）
     * @return 月度分布响应
     */
    MonthlyDistributionResponse getMonthlyDistribution(Integer year, String department, String organization);

    /**
     * 保存实际数据
     * @param request 实际数据请求
     * @return 是否成功
     */
    boolean saveActualData(ActualDataRequest request);

    /**
     * 导入目标配置数据
     * @param file Excel文件
     * @return 导入结果
     */
    String importTargets(MultipartFile file);

    /**
     * 获取所有产品列表
     * @return 产品列表
     */
    List<String> getAllProducts();

    /**
     * 获取所有部门列表
     * @return 部门列表
     */
    List<String> getAllDepartments();

    /**
     * 获取部门-机构对照关系
     * @return [{"department": "...", "organization": "..."}]
     */
    List<java.util.Map<String, String>> getDepartmentOrganizationMap();

    /**
     * 获取所有机构列表
     * @return 机构列表
     */
    List<String> getAllOrganizations();

    /**
     * 获取所有负责人列表
     * @return 负责人列表
     */
    List<String> getAllOwners();

    /**
     * 创建新目标
     * @param request 创建目标请求
     * @return 是否成功
     */
    boolean createTarget(CreateTargetRequest request);

    /**
     * 更新目标
     * @param id 目标ID
     * @param request 更新目标请求
     * @return 是否成功
     */
    boolean updateTarget(String id, CreateTargetRequest request);

    /**
     * 删除目标
     * @param id 目标ID
     * @return 是否成功
     */
    boolean deleteTarget(String id);
    
    /**
     * 测试数据库记录数
     * @return 测试结果
     */
    String testDatabaseCount();

    /**
     * 获取季度汇总数据（按签约/确权/预算分组）
     * @param year 年份
     * @param department 部门筛选（可选）
     * @param organization 机构筛选（可选）
     * @return 分组季度数据
     */
    java.util.Map<String, java.util.List<com.example.achievement.dto.response.TargetStatisticsResponse.QuarterlyData>>
        getQuarterlySummary(Integer year, String department, String organization);
}
