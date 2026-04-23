package com.example.achievement.dto.response;

import com.example.achievement.enums.AchievementStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AchievementResponse {

    private String id;
    private String name;
    private String version;
    private String changeVersion;
    private String productId;
    private String productName;
    private String moduleId;
    private String moduleName;
    private String type;
    private String description;
    private AchievementStatus status;
    private String owner;
    private LocalDateTime preRegisterTime;
    private LocalDateTime registerTime;
    private LocalDateTime recordTime;
    private String relatedOrderId;
    private String relatedProjectId;
    private String acceptanceRequirements;
    private String organizationName;
    private String departmentName;
    private String productExternalVersion;
    private String hasBaseline;
    private String requirementProposer;
    private String achievementForm;
    private String saleType;
    private String applicationScenario;
    private String achievementTarget;
    private String functionListFile;
    private LocalDate plannedAcceptanceDate;
    private String acceptanceMethod;
    private String acceptor;
    private String relatedProjectName;
    private String relatedOrderName;
    private String acceptanceOrganization;
    private String changeReason;
    private String deliverables;
    private String codeRepositoryUrl;
    private String demoUrl;
    private LocalDate actualAcceptanceDate;
    private String estimatedAcceptanceMonth;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> riskTags;
    private List<String> packageIds;
}
