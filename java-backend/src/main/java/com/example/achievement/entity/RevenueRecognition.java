package com.example.achievement.entity;

import javax.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "revenue_recognitions")
@Data
public class RevenueRecognition {

    @Id
    @Column(length = 50)
    private String id;

    @Column(name = "order_id", length = 100)
    private String orderId;

    @Column(name = "contract_id", length = 100)
    private String contractId;

    @Column(name = "contract_name", length = 200)
    private String contractName;

    @Column(name = "recognition_amount", precision = 15, scale = 2)
    private BigDecimal recognitionAmount;

    @Column(name = "revenue_amount", precision = 15, scale = 2)
    private BigDecimal revenueAmount;

    @Column(name = "recognition_month", length = 10)
    private String recognitionMonth;

    @Column(name = "recognition_risk_level", length = 50)
    private String recognitionRiskLevel;

    @Column(name = "sub_category", length = 50)
    private String subCategory;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Column(length = 100)
    private String operator;

    @Column(columnDefinition = "TEXT")
    private String remark;

    @Column(length = 100, nullable = false)
    private String organization;

    @Column
    private Integer quarter;

    @Column
    private Integer year;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        calcYearQuarter();
        calcSubCategory();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calcYearQuarter();
        calcSubCategory();
    }

    private void calcYearQuarter() {
        if (recognitionMonth != null && recognitionMonth.length() >= 7) {
            try {
                this.year = Integer.parseInt(recognitionMonth.substring(0, 4));
                int month = Integer.parseInt(recognitionMonth.substring(5, 7));
                this.quarter = (month - 1) / 3 + 1;
            } catch (Exception e) {}
        }
    }

    private void calcSubCategory() {
        if (recognitionRiskLevel == null) {
            this.subCategory = null;
        } else if (recognitionRiskLevel.contains("高")) {
            this.subCategory = "确权收入（高）";
        } else if (recognitionRiskLevel.contains("中")) {
            this.subCategory = "确权收入（中）";
        } else {
            this.subCategory = null;
        }
    }
}
