package com.example.achievement.entity;

import javax.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "achievement_status_records")
@Data
public class AchievementStatusRecord {

    @Id
    @Column(length = 50)
    private String id;

    @Column(name = "achievement_id", nullable = false, length = 50)
    private String achievementId;

    @Column(name = "achievement_name", nullable = false, length = 200)
    private String achievementName;

    @Column(name = "from_status", nullable = false, length = 50)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 50)
    private String toStatus;

    @Column(name = "change_type", nullable = false, length = 50)
    private String changeType;

    @Column(name = "change_reason", columnDefinition = "TEXT")
    private String changeReason;

    @Column(length = 100)
    private String operator;

    @Column(name = "change_time")
    private LocalDateTime changeTime;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (changeTime == null) {
            changeTime = LocalDateTime.now();
        }
    }
}
