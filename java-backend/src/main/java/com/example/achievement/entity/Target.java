package com.example.achievement.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "targets")
@Data
public class Target {

    @Id
    @Column(length = 50)
    private String id;

    @Column(length = 100)
    private String department;

    @Column(length = 100)
    private String organization;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "sub_category", length = 50)
    private String subCategory;

    @Column(name = "target_type", length = 50)
    private String targetType;

    @Column
    private Integer year;

    @Column(name = "annual_target", precision = 15, scale = 2)
    private BigDecimal annualTarget;

    @Column(name = "q1_target", precision = 15, scale = 2)
    private BigDecimal q1Target;

    @Column(name = "q2_target", precision = 15, scale = 2)
    private BigDecimal q2Target;

    @Column(name = "q3_target", precision = 15, scale = 2)
    private BigDecimal q3Target;

    @Column(name = "q4_target", precision = 15, scale = 2)
    private BigDecimal q4Target;

    @Column(name = "q1_actual", precision = 15, scale = 2)
    private BigDecimal q1Actual;

    @Column(name = "q2_actual", precision = 15, scale = 2)
    private BigDecimal q2Actual;

    @Column(name = "q3_actual", precision = 15, scale = 2)
    private BigDecimal q3Actual;

    @Column(name = "q4_actual", precision = 15, scale = 2)
    private BigDecimal q4Actual;

    @Column(length = 100)
    private String owner;

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
