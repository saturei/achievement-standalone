package com.example.achievement.entity;

import javax.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "contract_signings")
@Data
public class ContractSigning {

    @Id
    @Column(length = 50)
    private String id;

    @Column(name = "contract_id", length = 100)
    private String contractId;

    @Column(name = "contract_name", length = 200)
    private String contractName;

    @Column(name = "order_id", length = 100)
    private String orderId;

    @Column(name = "lead_id", length = 100)
    private String leadId;

    @Column(name = "package_id", length = 100)
    private String packageId;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "accounting_type", length = 50)
    private String accountingType;

    @Column(name = "signing_risk_level", length = 50)
    private String signingRiskLevel;

    @Column(name = "recognition_risk_level", length = 50)
    private String recognitionRiskLevel;

    @Column(name = "sub_category", length = 50)
    private String subCategory;

    @Column(length = 100)
    private String operator;

    @Column(columnDefinition = "TEXT")
    private String remark;

    @Column(length = 100)
    private String region;

    @Column(name = "product_id", length = 100)
    private String productId;

    @Column(length = 100, nullable = false)
    private String organization;

    @Column(name = "sign_month", length = 10)
    private String signMonth;

    @Column
    private Integer quarter;

    @Column(name = "\"year\"")
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
        if (signMonth != null && signMonth.length() >= 7) {
            try {
                this.year = Integer.parseInt(signMonth.substring(0, 4));
                int month = Integer.parseInt(signMonth.substring(5, 7));
                this.quarter = (month - 1) / 3 + 1;
            } catch (Exception e) {}
        }
    }

    private void calcSubCategory() {
        if (signingRiskLevel == null) {
            this.subCategory = null;
        } else if (signingRiskLevel.contains("高")) {
            this.subCategory = "签约收入（高）";
        } else if (signingRiskLevel.contains("中")) {
            this.subCategory = "签约收入（中）";
        } else {
            this.subCategory = null;
        }
    }
}
