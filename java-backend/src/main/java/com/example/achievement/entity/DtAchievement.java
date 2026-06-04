package com.example.achievement.entity;

import javax.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "dt_achievements")
@Data
public class DtAchievement {

    @Id
    @Column(length = 50)
    private String id;

    @Column(name = "achievement_name", length = 200)
    private String achievementName;

    @Column(name = "linked_product", length = 200)
    private String linkedProduct;

    @Column(name = "linked_packages", length = 500)
    private String linkedPackages;

    @Column(name = "linked_order_id", length = 100)
    private String linkedOrderId;

    @Column(name = "linked_order", length = 200)
    private String linkedOrder;

    @Column(name = "linked_project", length = 200)
    private String linkedProject;

    @Column(name = "achievement_form", length = 100)
    private String achievementForm;

    @Column(name = "achievement_version", length = 100)
    private String achievementVersion;

    @Column(name = "planned_accept_date", length = 50)
    private String plannedAcceptDate;

    @Column(name = "actual_accept_date", length = 50)
    private String actualAcceptDate;

    @Column(name = "accept_method", length = 100)
    private String acceptMethod;

    @Column(length = 500)
    private String acceptors;

    @Column(length = 100)
    private String department;

    @Column(name = "org_unit", length = 100)
    private String orgUnit;

    @Column(name = "has_baseline", length = 50)
    private String hasBaseline;

    @Column(name = "app_scenario", length = 200)
    private String appScenario;

    @Column(length = 100)
    private String requester;

    @Column(name = "goal_description", columnDefinition = "TEXT")
    private String goalDescription;

    @Column(name = "raw_json", columnDefinition = "TEXT")
    private String rawJson;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
