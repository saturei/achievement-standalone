package com.example.achievement.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "actual_data")
@Data
public class ActualData {

    @Id
    @Column(length = 50)
    private String id;

    @Column(name = "target_id", length = 50)
    private String targetId;

    @Column(length = 100)
    private String organization;

    @Column(name = "data_type", length = 50)
    private String dataType;

    @Column
    private Integer year;

    @Column
    private Integer month;

    @Column(name = "actual_value", precision = 15, scale = 2)
    private BigDecimal actualValue;

    @Column(columnDefinition = "TEXT")
    private String remark;

    @Column(name = "created_by", length = 100)
    private String createdBy;

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
