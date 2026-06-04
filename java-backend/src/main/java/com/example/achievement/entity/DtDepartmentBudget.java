package com.example.achievement.entity;

import javax.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "dt_department_budgets")
@Data
public class DtDepartmentBudget {

    @Id
    @Column(length = 50)
    private String id;

    @Column(name = "dept_key", length = 100)
    private String deptKey;

    @Column(name = "dept_name", length = 200)
    private String deptName;

    @Column(name = "dept_head", length = 100)
    private String deptHead;

    @Column(name = "\"year\"", length = 10)
    private String year;

    @Column(name = "budget_total", precision = 15, scale = 2)
    private BigDecimal budgetTotal;

    @Column(name = "actual_cost_total", precision = 15, scale = 2)
    private BigDecimal actualCostTotal;

    @Column(name = "budget_labor", precision = 15, scale = 2)
    private BigDecimal budgetLabor;

    @Column(name = "budget_travel", precision = 15, scale = 2)
    private BigDecimal budgetTravel;

    @Column(name = "budget_other", precision = 15, scale = 2)
    private BigDecimal budgetOther;

    @Column(name = "actual_labor", precision = 15, scale = 2)
    private BigDecimal actualLabor;

    @Column(name = "actual_travel", precision = 15, scale = 2)
    private BigDecimal actualTravel;

    @Column(name = "actual_other", precision = 15, scale = 2)
    private BigDecimal actualOther;

    @Column(name = "parent_dept", length = 100)
    private String parentDept;

    @Column(length = 50)
    private String status;

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
