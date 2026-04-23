package com.example.achievement.entity;

import com.example.achievement.enums.AchievementStatus;
import javax.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "achievements")
@Data
public class Achievement {

    @Id
    @Column(length = 200)
    private String id;

    @Column(name = "product_id", nullable = false, length = 50)
    private String productId;

    @Column(name = "product_name", length = 200)
    private String productName;

    @Column(name = "organization_id", length = 50)
    private String organizationId;

    @Column(name = "organization_name", length = 200)
    private String organizationName;

    @Column(name = "department_id", length = 50)
    private String departmentId;

    @Column(name = "department_name", length = 200)
    private String departmentName;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 50)
    private String version;

    @Column(name = "change_version", length = 50)
    private String changeVersion;

    @Column(name = "product_external_version", length = 50)
    private String productExternalVersion;

    @Column(name = "has_baseline", length = 20)
    private String hasBaseline = "无基线";

    @Column(name = "requirement_proposer", length = 100)
    private String requirementProposer;

    @Column(name = "achievement_form", length = 100)
    private String achievementForm;

    @Column(name = "sale_type", length = 100)
    private String saleType;

    @Column(name = "function_list_file", length = 500)
    private String functionListFile;

    @Column(name = "package_ids", columnDefinition = "TEXT")
    @org.hibernate.annotations.Type(type = "text")
    private String packageIds;

    @Column(name = "application_scenario", columnDefinition = "TEXT")
    private String applicationScenario;

    @Column(name = "module_id", length = 50)
    private String moduleId;

    @Column(name = "module_name", length = 200)
    private String moduleName;

    @Column(length = 50)
    private String type;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String owner;

    @Column(name = "achievement_target", columnDefinition = "TEXT")
    private String achievementTarget;

    @Column(name = "planned_acceptance_date")
    private LocalDate plannedAcceptanceDate;

    @Column(name = "acceptance_method", columnDefinition = "TEXT")
    private String acceptanceMethod;

    @Column(length = 200)
    private String acceptor;

    @Column(name = "related_project_id", length = 50)
    private String relatedProjectId;

    @Column(name = "related_project_name", length = 200)
    private String relatedProjectName;

    @Column(name = "related_order_id", length = 50)
    private String relatedOrderId;

    @Column(name = "related_order_name", length = 200)
    private String relatedOrderName;

    @Column(name = "acceptance_requirements", columnDefinition = "TEXT")
    private String acceptanceRequirements;

    @Column(name = "acceptance_organization", length = 200)
    private String acceptanceOrganization;

    @Column(name = "change_reason", columnDefinition = "TEXT")
    private String changeReason;

    @Column(columnDefinition = "TEXT")
    private String deliverables;

    @Column(name = "code_repository_url", length = 500)
    private String codeRepositoryUrl;

    @Column(name = "demo_url", length = 500)
    private String demoUrl;

    @Column(name = "actual_acceptance_date")
    private LocalDate actualAcceptanceDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private AchievementStatus status = AchievementStatus.PRE_REGISTER;

    @Column(name = "estimated_acceptance_month", length = 20)
    private String estimatedAcceptanceMonth;

    @Column(name = "pre_register_time")
    private LocalDateTime preRegisterTime;

    @Column(name = "register_time")
    private LocalDateTime registerTime;

    @Column(name = "record_time")
    private LocalDateTime recordTime;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "risk_tags", columnDefinition = "TEXT")
    @org.hibernate.annotations.Type(type = "text")
    private String riskTags;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (preRegisterTime == null) {
            preRegisterTime = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
