package com.example.achievement.entity;

import javax.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 产品实体类
 */
@Entity
@Table(name = "products")
@Data
public class Product {

    @Id
    @Column(length = 50)
    private String id;

    @Column(name = "product_name", length = 200)
    private String productName;

    @Column(name = "product_code", length = 50)
    private String productCode;

    @Column(name = "organization_id", length = 50)
    private String organizationId;

    @Column(name = "organization_name", length = 200)
    private String organizationName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

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
