package com.example.achievement.service.impl;

import com.example.achievement.dto.request.*;
import com.example.achievement.dto.response.AchievementListResponse;
import com.example.achievement.dto.response.AchievementResponse;
import com.example.achievement.dto.response.AchievementStatisticsResponse;
import com.example.achievement.dto.response.StatusRecordResponse;
import com.example.achievement.dto.response.VersionRecordResponse;
import com.example.achievement.entity.Achievement;
import com.example.achievement.entity.AchievementStatusRecord;
import com.example.achievement.entity.AchievementVersionRecord;
import com.example.achievement.enums.AchievementStatus;
import com.example.achievement.exception.AchievementNotFoundException;
import com.example.achievement.exception.InvalidStatusTransitionException;
import com.example.achievement.repository.AchievementRepository;
import com.example.achievement.repository.AchievementStatusRecordRepository;
import com.example.achievement.repository.AchievementVersionRecordRepository;
import com.example.achievement.service.AchievementService;
import com.example.achievement.util.VersionGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AchievementServiceImpl implements AchievementService {

    private final AchievementRepository achievementRepository;
    private final AchievementStatusRecordRepository statusRecordRepository;
    private final AchievementVersionRecordRepository versionRecordRepository;
    private final VersionGenerator versionGenerator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(readOnly = true)
    public AchievementListResponse getAchievements(int page, int pageSize, String keyword, String departmentName,
                                                   String organizationNames, String status,
                                                   String productId, Boolean includeDeleted) {
        int offset = (page - 1) * pageSize;
        
        String statusStr = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                statusStr = AchievementStatus.fromCode(status).name();
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status code: {}", status);
            }
        }
        
        int includeDeletedFlag;
        if ("DELETED".equals(statusStr)) {
            includeDeletedFlag = 1;
        } else if (includeDeleted != null && includeDeleted) {
            includeDeletedFlag = 1;
        } else {
            includeDeletedFlag = 0;
        }
        
        String keywordParam = (keyword != null && !keyword.trim().isEmpty()) ? keyword : null;
        String departmentNameParam = (departmentName != null && !departmentName.trim().isEmpty()) ? departmentName : null;
        String organizationNamesParam = (organizationNames != null && !organizationNames.trim().isEmpty()) ? organizationNames : null;
        String productIdParam = (productId != null && !productId.trim().isEmpty()) ? productId : null;
        
        List<Achievement> achievements = achievementRepository.findByConditionsNative(
                keywordParam, departmentNameParam, organizationNamesParam, statusStr,
                productIdParam, includeDeletedFlag, pageSize, offset);

        long total = achievementRepository.countByConditionsNative(
                keywordParam, departmentNameParam, organizationNamesParam, statusStr,
                productIdParam, includeDeletedFlag);

        List<AchievementResponse> items = achievements.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        AchievementListResponse response = new AchievementListResponse();
        response.setTotal(total);
        response.setPage(page);
        response.setPageSize(pageSize);
        response.setItems(items);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public AchievementStatisticsResponse getStatistics() {
        AchievementStatisticsResponse response = new AchievementStatisticsResponse();
        
        response.setTotalCount(achievementRepository.countAllExcludingDeleted());
        response.setPreRegisterCount(achievementRepository.countByStatus("PRE_REGISTER"));
        response.setRegisterCount(achievementRepository.countByStatus("REGISTER"));
        response.setRecordCount(achievementRepository.countByStatus("RECORDED"));
        response.setOfflineCount(achievementRepository.countByStatus("OFFLINE"));

        Map<String, Long> byStatus = new HashMap<>();
        byStatus.put("pre_register", response.getPreRegisterCount());
        byStatus.put("register", response.getRegisterCount());
        byStatus.put("recorded", response.getRecordCount());
        byStatus.put("offline", response.getOfflineCount());
        response.setByStatus(byStatus);

        Map<String, Long> byType = new HashMap<>();
        List<Achievement> allAchievements = achievementRepository.findAll();
        allAchievements.stream()
                .filter(a -> a.getStatus() != AchievementStatus.DELETED)
                .filter(a -> a.getType() != null)
                .collect(Collectors.groupingBy(Achievement::getType, Collectors.counting()))
                .forEach(byType::put);
        response.setByType(byType);

        Map<String, Long> byProduct = new HashMap<>();
        allAchievements.stream()
                .filter(a -> a.getStatus() != AchievementStatus.DELETED)
                .filter(a -> a.getProductId() != null)
                .collect(Collectors.groupingBy(Achievement::getProductId, Collectors.counting()))
                .forEach(byProduct::put);
        response.setByProduct(byProduct);

        response.setCheckoutCount(0L);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public AchievementResponse getAchievement(String achievementId) {
        Achievement achievement = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new AchievementNotFoundException("成果不存在: " + achievementId));
        return convertToResponse(achievement);
    }

    @Override
    public AchievementResponse preRegister(AchievementPreRegisterRequest request) {
        Achievement achievement = new Achievement();
        BeanUtils.copyProperties(request, achievement, "packageIds", "riskTags");
        
        if (request.getPackageIds() != null && !request.getPackageIds().isEmpty()) {
            try {
                achievement.setPackageIds(objectMapper.writeValueAsString(request.getPackageIds()));
            } catch (JsonProcessingException e) {
                log.error("序列化packageIds失败", e);
            }
        }
        
        if (request.getRiskTags() != null && !request.getRiskTags().isEmpty()) {
            try {
                achievement.setRiskTags(objectMapper.writeValueAsString(request.getRiskTags()));
            } catch (JsonProcessingException e) {
                log.error("序列化riskTags失败", e);
            }
        }
        
        String version = request.getVersion() != null ? request.getVersion() : 
                versionGenerator.generateManagementVersion(request.getName(), request.getProductExternalVersion());
        achievement.setVersion(version);
        
        String id = request.getName() + "_" + version;
        achievement.setId(id);
        
        achievement.setStatus(AchievementStatus.PRE_REGISTER);
        achievement.setPreRegisterTime(LocalDateTime.now());
        achievement.setCreatedBy("system");
        achievement.setUpdatedBy("system");

        Achievement savedAchievement = achievementRepository.save(achievement);
        
        createStatusRecord(savedAchievement, null, AchievementStatus.PRE_REGISTER, "预注册", "创建成果预注册");
        
        List<String> changedFields = new ArrayList<>();
        changedFields.add("status");
        createVersionRecord(savedAchievement, "", version, changedFields, "预注册: 创建成果", null);

        log.info("成果预注册成功: {}", savedAchievement.getId());
        return convertToResponse(savedAchievement);
    }

    @Override
    public AchievementResponse register(String achievementId, AchievementRegisterRequest request) {
        Achievement achievement = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new AchievementNotFoundException("成果不存在: " + achievementId));

        if (achievement.getStatus() != AchievementStatus.PRE_REGISTER) {
            throw new InvalidStatusTransitionException("只有预注册状态的成果才能注册");
        }

        AchievementStatus fromStatus = achievement.getStatus();
        String fromVersion = achievement.getVersion();
        
        achievement.setStatus(AchievementStatus.REGISTER);
        achievement.setRegisterTime(request.getRegisterTime() != null ? request.getRegisterTime() : LocalDateTime.now());
        
        if (request.getVersion() != null && !request.getVersion().isEmpty()) {
            achievement.setVersion(request.getVersion());
        }
        if (request.getAchievementTarget() != null && !request.getAchievementTarget().isEmpty()) {
            achievement.setAchievementTarget(request.getAchievementTarget());
        }
        if (request.getPlannedAcceptanceDate() != null) {
            achievement.setPlannedAcceptanceDate(request.getPlannedAcceptanceDate());
        }
        if (request.getEstimatedAcceptanceMonth() != null && !request.getEstimatedAcceptanceMonth().isEmpty()) {
            achievement.setEstimatedAcceptanceMonth(request.getEstimatedAcceptanceMonth());
        }
        if (request.getAcceptanceMethod() != null && !request.getAcceptanceMethod().isEmpty()) {
            achievement.setAcceptanceMethod(request.getAcceptanceMethod());
        }
        if (request.getAcceptor() != null && !request.getAcceptor().isEmpty()) {
            achievement.setAcceptor(request.getAcceptor());
        }
        if (request.getAcceptanceOrganization() != null && !request.getAcceptanceOrganization().isEmpty()) {
            achievement.setAcceptanceOrganization(request.getAcceptanceOrganization());
        }
        if (request.getAcceptanceRequirements() != null && !request.getAcceptanceRequirements().isEmpty()) {
            achievement.setAcceptanceRequirements(request.getAcceptanceRequirements());
        }
        if (request.getFunctionListFile() != null && !request.getFunctionListFile().isEmpty()) {
            achievement.setFunctionListFile(request.getFunctionListFile());
        }
        if (request.getPackageIds() != null && !request.getPackageIds().isEmpty()) {
            achievement.setPackageIds(request.getPackageIds());
        }
        achievement.setUpdatedBy("system");

        Achievement savedAchievement = achievementRepository.save(achievement);
        
        createStatusRecord(savedAchievement, fromStatus, AchievementStatus.REGISTER, 
                "注册", request.getRegisterNotes() != null ? request.getRegisterNotes() : "成果注册");
        
        List<String> changedFields = new ArrayList<>();
        changedFields.add("status");
        createVersionRecord(savedAchievement, fromVersion, savedAchievement.getVersion(), 
                changedFields, "注册: " + (request.getRegisterNotes() != null ? request.getRegisterNotes() : "成果注册"), null);

        log.info("成果注册成功: {}", savedAchievement.getId());
        return convertToResponse(savedAchievement);
    }

    @Override
    public AchievementResponse record(String achievementId, AchievementRecordRequest request) {
        Achievement achievement = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new AchievementNotFoundException("成果不存在: " + achievementId));

        if (achievement.getStatus() != AchievementStatus.REGISTER) {
            throw new InvalidStatusTransitionException("只有注册状态的成果才能登记");
        }

        AchievementStatus fromStatus = achievement.getStatus();
        String fromVersion = achievement.getVersion();
        
        achievement.setStatus(AchievementStatus.RECORDED);
        achievement.setRecordTime(request.getRecordTime() != null ? request.getRecordTime() : LocalDateTime.now());
        
        if (request.getVersion() != null && !request.getVersion().isEmpty()) {
            achievement.setVersion(request.getVersion());
        }
        if (request.getActualAcceptanceDate() != null) {
            achievement.setActualAcceptanceDate(request.getActualAcceptanceDate());
        }
        if (request.getDemoUrl() != null && !request.getDemoUrl().isEmpty()) {
            achievement.setDemoUrl(request.getDemoUrl());
        }
        if (request.getCodeRepositoryUrl() != null && !request.getCodeRepositoryUrl().isEmpty()) {
            achievement.setCodeRepositoryUrl(request.getCodeRepositoryUrl());
        }
        if (request.getDeliverables() != null && !request.getDeliverables().isEmpty()) {
            achievement.setDeliverables(request.getDeliverables());
        }
        achievement.setUpdatedBy("system");

        Achievement savedAchievement = achievementRepository.save(achievement);
        
        createStatusRecord(savedAchievement, fromStatus, AchievementStatus.RECORDED, 
                "登记", request.getRecordNotes() != null ? request.getRecordNotes() : "成果登记");
        
        List<String> changedFields = new ArrayList<>();
        changedFields.add("status");
        createVersionRecord(savedAchievement, fromVersion, savedAchievement.getVersion(), 
                changedFields, "登记: " + (request.getRecordNotes() != null ? request.getRecordNotes() : "成果登记"), null);

        log.info("成果登记成功: {}", savedAchievement.getId());
        return convertToResponse(savedAchievement);
    }

    @Override
    public AchievementResponse change(String achievementId, AchievementChangeRequest request) {
        Achievement achievement = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new AchievementNotFoundException("成果不存在: " + achievementId));

        if (achievement.getStatus() == AchievementStatus.DELETED) {
            throw new InvalidStatusTransitionException("已删除的成果不能变更");
        }

        String fromVersion = achievement.getVersion();
        List<String> changedFields = new ArrayList<>();

        if (request.getDescription() != null && !request.getDescription().equals(achievement.getDescription())) {
            achievement.setDescription(request.getDescription());
            changedFields.add("description");
        }
        if (request.getOwner() != null && !request.getOwner().equals(achievement.getOwner())) {
            achievement.setOwner(request.getOwner());
            changedFields.add("owner");
        }
        if (request.getRelatedProjectId() != null && !request.getRelatedProjectId().equals(achievement.getRelatedProjectId())) {
            achievement.setRelatedProjectId(request.getRelatedProjectId());
            achievement.setRelatedProjectName(request.getRelatedProjectName());
            changedFields.add("relatedProjectId");
        }
        if (request.getRelatedOrderId() != null && !request.getRelatedOrderId().equals(achievement.getRelatedOrderId())) {
            achievement.setRelatedOrderId(request.getRelatedOrderId());
            achievement.setRelatedOrderName(request.getRelatedOrderName());
            changedFields.add("relatedOrderId");
        }
        if (request.getAcceptanceRequirements() != null && !request.getAcceptanceRequirements().equals(achievement.getAcceptanceRequirements())) {
            achievement.setAcceptanceRequirements(request.getAcceptanceRequirements());
            changedFields.add("acceptanceRequirements");
        }
        if (request.getRegisterTime() != null) {
            achievement.setRegisterTime(request.getRegisterTime());
            changedFields.add("registerTime");
        }
        if (request.getRiskTags() != null) {
            try {
                achievement.setRiskTags(objectMapper.writeValueAsString(request.getRiskTags()));
            } catch (JsonProcessingException e) {
                log.error("序列化riskTags失败", e);
            }
            changedFields.add("riskTags");
        }
        if (request.getProductExternalVersion() != null) {
            achievement.setProductExternalVersion(request.getProductExternalVersion());
            changedFields.add("productExternalVersion");
        }
        if (request.getModuleId() != null && !request.getModuleId().isEmpty() && !request.getModuleId().equals(achievement.getModuleId())) {
            achievement.setModuleId(request.getModuleId());
            achievement.setModuleName(request.getModuleName());
            changedFields.add("moduleId");
        }
        if (request.getAchievementTarget() != null && !request.getAchievementTarget().isEmpty() && !request.getAchievementTarget().equals(achievement.getAchievementTarget())) {
            achievement.setAchievementTarget(request.getAchievementTarget());
            changedFields.add("achievementTarget");
        }
        if (request.getPlannedAcceptanceDate() != null && !request.getPlannedAcceptanceDate().equals(achievement.getPlannedAcceptanceDate())) {
            achievement.setPlannedAcceptanceDate(request.getPlannedAcceptanceDate());
            changedFields.add("plannedAcceptanceDate");
        }
        if (request.getEstimatedAcceptanceMonth() != null && !request.getEstimatedAcceptanceMonth().isEmpty() && !request.getEstimatedAcceptanceMonth().equals(achievement.getEstimatedAcceptanceMonth())) {
            achievement.setEstimatedAcceptanceMonth(request.getEstimatedAcceptanceMonth());
            changedFields.add("estimatedAcceptanceMonth");
        }
        if (request.getAcceptanceMethod() != null && !request.getAcceptanceMethod().isEmpty() && !request.getAcceptanceMethod().equals(achievement.getAcceptanceMethod())) {
            achievement.setAcceptanceMethod(request.getAcceptanceMethod());
            changedFields.add("acceptanceMethod");
        }
        if (request.getAcceptor() != null && !request.getAcceptor().isEmpty() && !request.getAcceptor().equals(achievement.getAcceptor())) {
            achievement.setAcceptor(request.getAcceptor());
            changedFields.add("acceptor");
        }
        if (request.getAcceptanceOrganization() != null && !request.getAcceptanceOrganization().isEmpty() && !request.getAcceptanceOrganization().equals(achievement.getAcceptanceOrganization())) {
            achievement.setAcceptanceOrganization(request.getAcceptanceOrganization());
            changedFields.add("acceptanceOrganization");
        }
        if (request.getFunctionListFile() != null && !request.getFunctionListFile().isEmpty() && !request.getFunctionListFile().equals(achievement.getFunctionListFile())) {
            achievement.setFunctionListFile(request.getFunctionListFile());
            changedFields.add("functionListFile");
        }
        if (request.getPackageIds() != null && !request.getPackageIds().isEmpty() && !request.getPackageIds().equals(achievement.getPackageIds())) {
            achievement.setPackageIds(request.getPackageIds());
            changedFields.add("packageIds");
        }

        if (!changedFields.isEmpty()) {
            String currentChangeVersion = achievement.getChangeVersion() != null ? achievement.getChangeVersion() : "V1.0.0";
            String newChangeVersion = versionGenerator.incrementVersion(currentChangeVersion);
            achievement.setChangeVersion(newChangeVersion);
            
            if (request.getVersion() != null && !request.getVersion().isEmpty()) {
                achievement.setVersion(request.getVersion());
                changedFields.add("version");
            }
            achievement.setUpdatedBy("system");

            Achievement savedAchievement = achievementRepository.save(achievement);
            
            createVersionRecord(savedAchievement, currentChangeVersion, newChangeVersion, changedFields, 
                    request.getChangeDescription(), request.getRiskTags());

            log.info("成果变更成功: {}, 新变更版本: {}", savedAchievement.getId(), newChangeVersion);
            return convertToResponse(savedAchievement);
        }

        return convertToResponse(achievement);
    }

    @Override
    public AchievementResponse offline(String achievementId, AchievementOfflineRequest request) {
        Achievement achievement = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new AchievementNotFoundException("成果不存在: " + achievementId));

        if (achievement.getStatus() != AchievementStatus.RECORDED) {
            throw new InvalidStatusTransitionException("只有登记状态的成果才能下架");
        }

        AchievementStatus fromStatus = achievement.getStatus();
        String fromVersion = achievement.getChangeVersion();
        
        achievement.setStatus(AchievementStatus.OFFLINE);
        achievement.setChangeReason(request.getReason());
        achievement.setUpdatedBy("system");

        Achievement savedAchievement = achievementRepository.save(achievement);
        
        createStatusRecord(savedAchievement, fromStatus, AchievementStatus.OFFLINE, 
                "下架", request.getReason() != null ? request.getReason() : "成果下架");
        
        List<String> changedFields = new ArrayList<>();
        changedFields.add("status");
        createVersionRecord(savedAchievement, fromVersion != null ? fromVersion : "", fromVersion != null ? fromVersion : "",
                changedFields, "下架操作: " + (request.getReason() != null ? request.getReason() : "成果下架"), null);

        log.info("成果下架成功: {}", savedAchievement.getId());
        return convertToResponse(savedAchievement);
    }

    @Override
    public AchievementResponse online(String achievementId, AchievementOnlineRequest request) {
        Achievement achievement = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new AchievementNotFoundException("成果不存在: " + achievementId));

        if (achievement.getStatus() != AchievementStatus.OFFLINE) {
            throw new InvalidStatusTransitionException("只有下架状态的成果才能上架");
        }

        AchievementStatus fromStatus = achievement.getStatus();
        String fromVersion = achievement.getChangeVersion();
        
        achievement.setStatus(AchievementStatus.RECORDED);
        achievement.setChangeReason(request.getReason());
        achievement.setUpdatedBy("system");

        Achievement savedAchievement = achievementRepository.save(achievement);
        
        createStatusRecord(savedAchievement, fromStatus, AchievementStatus.RECORDED, 
                "上架", request.getReason() != null ? request.getReason() : "成果上架");
        
        List<String> changedFields = new ArrayList<>();
        changedFields.add("status");
        createVersionRecord(savedAchievement, fromVersion != null ? fromVersion : "", fromVersion != null ? fromVersion : "",
                changedFields, "上架操作: " + (request.getReason() != null ? request.getReason() : "成果上架"), null);

        log.info("成果上架成功: {}", savedAchievement.getId());
        return convertToResponse(savedAchievement);
    }

    @Override
    public AchievementResponse delete(String achievementId) {
        Achievement achievement = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new AchievementNotFoundException("成果不存在: " + achievementId));

        if (achievement.getStatus() != AchievementStatus.PRE_REGISTER) {
            throw new InvalidStatusTransitionException("只有预注册状态的成果才能删除");
        }

        AchievementStatus fromStatus = achievement.getStatus();
        achievement.setStatus(AchievementStatus.DELETED);
        achievement.setUpdatedBy("system");

        Achievement savedAchievement = achievementRepository.save(achievement);
        
        createStatusRecord(savedAchievement, fromStatus, AchievementStatus.DELETED, 
                "删除", "成果删除");

        log.info("成果删除成功: {}", savedAchievement.getId());
        return convertToResponse(savedAchievement);
    }

    private void createStatusRecord(Achievement achievement, AchievementStatus fromStatus, 
                                   AchievementStatus toStatus, String changeType, String reason) {
        AchievementStatusRecord record = new AchievementStatusRecord();
        record.setId(UUID.randomUUID().toString());
        record.setAchievementId(achievement.getId());
        record.setAchievementName(achievement.getName());
        record.setFromStatus(fromStatus != null ? fromStatus.getCode() : "null");
        record.setToStatus(toStatus.getCode());
        record.setChangeType(changeType);
        record.setChangeReason(reason);
        record.setOperator("system");
        record.setChangeTime(LocalDateTime.now());
        
        statusRecordRepository.save(record);
    }

    private void createVersionRecord(Achievement achievement, String fromVersion, String toVersion,
                                    List<String> changedFields, String description, List<String> riskTags) {
        AchievementVersionRecord record = new AchievementVersionRecord();
        record.setId(UUID.randomUUID().toString());
        record.setAchievementName(achievement.getName());
        record.setAchievementId(achievement.getId());
        record.setProductExternalVersion(achievement.getProductExternalVersion() != null ? achievement.getProductExternalVersion() : "");
        record.setFromVersion(fromVersion != null ? fromVersion : "");
        record.setToVersion(toVersion != null ? toVersion : "");
        
        try {
            record.setChangedFields(objectMapper.writeValueAsString(changedFields));
            if (riskTags != null) {
                record.setRiskTags(objectMapper.writeValueAsString(riskTags));
            }
        } catch (JsonProcessingException e) {
            log.error("序列化字段失败", e);
        }
        
        record.setChangeDescription(description);
        record.setOperator("system");
        record.setChangeTime(LocalDateTime.now());
        
        versionRecordRepository.save(record);
    }

    private AchievementResponse convertToResponse(Achievement achievement) {
        AchievementResponse response = new AchievementResponse();
        BeanUtils.copyProperties(achievement, response);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatusRecordResponse> getStatusRecords(String achievementId) {
        List<AchievementStatusRecord> records = statusRecordRepository.findByAchievementIdOrderByChangeTimeDesc(achievementId);
        return records.stream().map(this::convertToStatusRecordResponse).collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VersionRecordResponse> getVersionRecords(String achievementId) {
        List<AchievementVersionRecord> records = versionRecordRepository.findByAchievementIdOrderByChangeTimeDesc(achievementId);
        return records.stream().map(this::convertToVersionRecordResponse).collect(java.util.stream.Collectors.toList());
    }

    private StatusRecordResponse convertToStatusRecordResponse(AchievementStatusRecord record) {
        StatusRecordResponse response = new StatusRecordResponse();
        response.setId(record.getId());
        response.setAchievementId(record.getAchievementId());
        response.setAchievementName(record.getAchievementName());
        response.setOldStatus(record.getFromStatus());
        response.setNewStatus(record.getToStatus());
        response.setChangeReason(record.getChangeReason());
        response.setChangedBy(record.getOperator());
        response.setChangeTime(record.getChangeTime());
        return response;
    }

    private VersionRecordResponse convertToVersionRecordResponse(AchievementVersionRecord record) {
        VersionRecordResponse response = new VersionRecordResponse();
        response.setId(record.getId());
        response.setAchievementId(record.getAchievementId());
        response.setAchievementName(record.getAchievementName());
        response.setFromVersion(record.getFromVersion());
        response.setToVersion(record.getToVersion());
        response.setChangedFields(record.getChangedFields());
        response.setChangeDescription(record.getChangeDescription());
        response.setChangedBy(record.getOperator());
        response.setChangeTime(record.getChangeTime());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAllOrganizations() {
        return achievementRepository.findDistinctOrganizationNames();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAllDepartments() {
        return achievementRepository.findDistinctDepartmentNames();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAllOwners() {
        return achievementRepository.findDistinctOwners();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAllTypes() {
        return achievementRepository.findDistinctTypes();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<String>> getFilteredOptions(String keyword, String departmentName,
                                                         String organizationNames, String status, String productId) {
        String statusStr = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                statusStr = AchievementStatus.fromCode(status).name();
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status code: {}", status);
            }
        }

        String keywordParam = (keyword != null && !keyword.trim().isEmpty()) ? keyword : null;
        String departmentNameParam = (departmentName != null && !departmentName.trim().isEmpty()) ? departmentName : null;
        String organizationNamesParam = (organizationNames != null && !organizationNames.trim().isEmpty()) ? organizationNames : null;
        String productIdParam = (productId != null && !productId.trim().isEmpty()) ? productId : null;

        List<String> departments = achievementRepository.findDistinctDepartmentNamesFiltered(
                keywordParam, organizationNamesParam, statusStr, productIdParam);
        List<String> organizations = achievementRepository.findDistinctOrganizationNamesFiltered(
                keywordParam, departmentNameParam, statusStr, productIdParam);

        Map<String, List<String>> result = new HashMap<>();
        result.put("departments", departments);
        result.put("organizations", organizations);
        return result;
    }
}
