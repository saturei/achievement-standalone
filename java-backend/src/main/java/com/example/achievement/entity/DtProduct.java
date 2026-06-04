package com.example.achievement.entity;

import javax.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "dt_products")
@Data
public class DtProduct {

    @Id
    @Column(length = 50)
    private String id;

    @Column(name = "product_id", length = 100)
    private String productId;

    @Column(name = "product_name", length = 200)
    private String productName;

    @Column(name = "product_department", length = 100)
    private String productDepartment;

    @Column(name = "product_manager", length = 100)
    private String productManager;

    @Column(name = "product_owner", length = 100)
    private String productOwner;

    @Column(name = "annual_budget_wan", precision = 15, scale = 2)
    private BigDecimal annualBudgetWan;

    @Column(name = "linked_projects", columnDefinition = "TEXT")
    private String linkedProjects;

    @Column(name = "cost_accumulated", precision = 15, scale = 2)
    private BigDecimal costAccumulated;

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
