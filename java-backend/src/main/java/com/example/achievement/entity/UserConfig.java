package com.example.achievement.entity;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_configs")
@Data
public class UserConfig {

    @Id
    @Column(length = 50)
    private String id;

    @Column(length = 50, nullable = false, unique = true)
    private String username;

    @Column(name = "user_id", length = 256)
    private String userId;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(length = 512)
    private String avatar;

    @Column(length = 20, nullable = false)
    private String role = "USER";

    @Column(length = 100)
    private String department;

    @Column(length = 100)
    private String organization;

    @Column(nullable = false)
    private Integer enabled = 1;

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
