package com.example.achievement.entity;

import javax.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "achievement_version_records")
@Data
public class AchievementVersionRecord {

    @Id
    @Column(length = 50)
    private String id;

    @Column(name = "achievement_name", nullable = false, length = 100)
    private String achievementName;

    @Column(name = "achievement_id", nullable = false, length = 50)
    private String achievementId;

    @Column(name = "product_external_version", nullable = false, length = 20)
    private String productExternalVersion;

    @Column(name = "from_version", nullable = false, length = 20)
    private String fromVersion;

    @Column(name = "to_version", nullable = false, length = 20)
    private String toVersion;

    @Column(name = "changed_fields", nullable = false, columnDefinition = "TEXT")
    @org.hibernate.annotations.Type(type = "text")
    private String changedFields;

    @Column(name = "change_description", columnDefinition = "TEXT")
    private String changeDescription;

    @Column(name = "risk_tags", columnDefinition = "TEXT")
    @org.hibernate.annotations.Type(type = "text")
    private String riskTags;

    @Column(length = 50)
    private String operator;

    @Column(name = "change_time", nullable = false)
    private LocalDateTime changeTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (changeTime == null) {
            changeTime = LocalDateTime.now();
        }
    }
}
