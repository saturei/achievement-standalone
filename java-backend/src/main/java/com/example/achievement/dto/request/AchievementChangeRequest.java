package com.example.achievement.dto.request;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AchievementChangeRequest {

    private String changeDescription;
    private String version;
    private String description;
    private String owner;
    private String relatedProjectId;
    private String relatedProjectName;
    private String relatedOrderId;
    private String relatedOrderName;
    private String acceptanceRequirements;
    private LocalDateTime registerTime;
    private List<String> riskTags;
    private String productExternalVersion;
    private String moduleId;
    private String moduleName;
    
    private String achievementTarget;
    private LocalDate plannedAcceptanceDate;
    private String estimatedAcceptanceMonth;
    private String acceptanceMethod;
    private String acceptor;
    private String acceptanceOrganization;
    private String functionListFile;
    private String packageIds;
}
