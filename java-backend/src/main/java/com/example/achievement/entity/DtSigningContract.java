package com.example.achievement.entity;

import javax.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "dt_signing_contracts")
@Data
public class DtSigningContract {

    @Id
    @Column(length = 50)
    private String id;

    @Column(name = "contract_id", length = 100)
    private String contractId;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Column(name = "contract_name", length = 200)
    private String contractName;

    @Column(name = "signing_amount_wan", precision = 15, scale = 2)
    private BigDecimal signingAmountWan;

    @Column(name = "accounting_type", length = 50)
    private String accountingType;

    @Column(name = "signing_risk_level", length = 50)
    private String signingRiskLevel;

    @Column(name = "recognition_risk_level", length = 50)
    private String recognitionRiskLevel;

    @Column(length = 100)
    private String operator;

    @Column(length = 100)
    private String department;

    @Column(name = "signing_quarter", length = 50)
    private String signingQuarter;

    @Column(name = "linked_package", length = 200)
    private String linkedPackage;

    @Column(name = "linked_opportunity", length = 200)
    private String linkedOpportunity;

    @Column(name = "signing_date", length = 50)
    private String signingDate;

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
