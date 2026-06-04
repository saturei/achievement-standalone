package com.example.achievement.entity;

import javax.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "dt_revenue_details")
@Data
public class DtRevenueDetail {

    @Id
    @Column(length = 50)
    private String id;

    @Column(name = "order_id", length = 100)
    private String orderId;

    @Column(name = "contract_id", length = 100)
    private String contractId;

    @Column(name = "contract_name", length = 200)
    private String contractName;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Column(name = "revenue_amount_wan", precision = 15, scale = 2)
    private BigDecimal revenueAmountWan;

    @Column(name = "confirmed_revenue", precision = 15, scale = 2)
    private BigDecimal confirmedRevenue;

    @Column(name = "cost_carryover", precision = 15, scale = 2)
    private BigDecimal costCarryover;

    @Column(name = "delivery_margin", precision = 15, scale = 2)
    private BigDecimal deliveryMargin;

    @Column(name = "recognition_month", length = 50)
    private String recognitionMonth;

    @Column(name = "profit_loss_subject", length = 100)
    private String profitLossSubject;

    @Column(name = "recognition_type", length = 50)
    private String recognitionType;

    @Column(name = "accounting_type", length = 50)
    private String accountingType;

    @Column(length = 100)
    private String operator;

    @Column(name = "org_unit", length = 100)
    private String orgUnit;

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
