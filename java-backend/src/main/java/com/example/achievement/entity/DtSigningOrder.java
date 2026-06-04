package com.example.achievement.entity;

import javax.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "dt_signing_orders")
@Data
public class DtSigningOrder {

    @Id
    @Column(length = 50)
    private String id;

    @Column(name = "order_id", length = 100)
    private String orderId;

    @Column(name = "contract_id", length = 100)
    private String contractId;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Column(name = "order_amount_wan", precision = 15, scale = 2)
    private BigDecimal orderAmountWan;

    @Column(name = "planned_recognition_amount", precision = 15, scale = 2)
    private BigDecimal plannedRecognitionAmount;

    @Column(name = "accounting_type", length = 50)
    private String accountingType;

    @Column(name = "contract_status", length = 50)
    private String contractStatus;

    @Column(length = 100)
    private String operator;

    @Column(length = 100)
    private String department;

    @Column(name = "signing_quarter", length = 50)
    private String signingQuarter;

    @Column(name = "linked_package", length = 200)
    private String linkedPackage;

    @Column(name = "order_create_date", length = 50)
    private String orderCreateDate;

    @Column(columnDefinition = "TEXT")
    private String remark;

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
