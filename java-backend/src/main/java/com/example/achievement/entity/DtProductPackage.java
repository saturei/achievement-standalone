package com.example.achievement.entity;

import javax.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "dt_product_packages")
@Data
public class DtProductPackage {

    @Id
    @Column(length = 50)
    private String id;

    @Column(name = "package_id", length = 100)
    private String packageId;

    @Column(name = "linked_product_id", length = 100)
    private String linkedProductId;

    @Column(name = "package_name", length = 200)
    private String packageName;

    @Column(length = 50)
    private String status;

    @Column(name = "standard_price_wan", precision = 15, scale = 2)
    private BigDecimal standardPriceWan;

    @Column(name = "standard_margin_rate", precision = 5, scale = 2)
    private BigDecimal standardMarginRate;

    @Column(name = "impl_cycle", length = 50)
    private String implCycle;

    @Column(name = "impl_scale", length = 100)
    private String implScale;

    @Column(name = "responsible_person", length = 100)
    private String responsiblePerson;

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
