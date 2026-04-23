package com.example.achievement.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class AchievementPreRegisterRequest {

    @NotBlank(message = "成果名称不能为空")
    private String name;

    private String version;

    @NotBlank(message = "产品ID不能为空")
    private String productId;

    private String productName;

    private String productExternalVersion;
    private String organizationId;
    private String organizationName;
    private String departmentId;
    private String departmentName;
    private String hasBaseline = "无基线";

    @NotBlank(message = "需求提出人不能为空")
    private String requirementProposer;

    @NotBlank(message = "成果形态不能为空")
    private String achievementForm;
    private String saleType;
    private String functionListFile;
    private List<String> packageIds;
    private String moduleId;
    private String moduleName;

    private String type;

    private String description;

    @NotBlank(message = "负责人不能为空")
    private String owner;

    private String achievementTarget;
    private LocalDate plannedAcceptanceDate;
    private String acceptanceMethod;
    private String acceptor;
    private String relatedProjectId;
    private String relatedProjectName;
    private String relatedOrderId;
    private String relatedOrderName;
    private String acceptanceRequirements;
    private String acceptanceOrganization;
    private List<String> riskTags;
}
