package com.example.achievement.entity;

import javax.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "dt_project_order_costs")
@Data
public class DtProjectOrderCost {

    @Id
    @Column(length = 50)
    private String id;

    /** 订单编号 */
    @Column(name = "order_id", length = 100)
    private String orderId;

    /** 订单名称 */
    @Column(name = "order_name", length = 200)
    private String orderName;

    /** 所属订单编号 */
    @Column(name = "parent_order_id", length = 100)
    private String parentOrderId;

    /** 订单类别 */
    @Column(name = "order_category", length = 50)
    private String orderCategory;

    /** 订单状态 */
    @Column(name = "order_status", length = 50)
    private String orderStatus;

    /** 关联项目编号 */
    @Column(name = "project_id", length = 100)
    private String projectId;

    /** 关联产品ID */
    @Column(name = "product_id", length = 100)
    private String productId;

    /** 订单所属部门 */
    @Column(length = 100)
    private String department;

    /** 所属机构 */
    @Column(length = 100)
    private String organization;

    /** 计划开始日期 */
    @Column(name = "plan_start_date", length = 50)
    private String planStartDate;

    /** 计划结束日期 */
    @Column(name = "plan_end_date", length = 50)
    private String planEndDate;

    /** 订单创建日期 */
    @Column(name = "order_create_date", length = 50)
    private String orderCreateDate;

    /** 订单费用预算（万元） */
    @Column(name = "order_budget", precision = 15, scale = 2)
    private BigDecimal orderBudget;

    /** 项目预算口径成本数 */
    @Column(name = "project_budget_cost", precision = 15, scale = 2)
    private BigDecimal projectBudgetCost;

    /** 订单已入账成本（财务） */
    @Column(name = "booked_cost", precision = 15, scale = 2)
    private BigDecimal bookedCost;

    /** 订单未入账成本 */
    @Column(name = "unbooked_cost", precision = 15, scale = 2)
    private BigDecimal unbookedCost;

    /** 实际成本合计 */
    @Column(name = "actual_cost_total", precision = 15, scale = 2)
    private BigDecimal actualCostTotal;

    /** 订单预计工作量（人天） */
    @Column(name = "estimated_man_days", precision = 10, scale = 1)
    private BigDecimal estimatedManDays;

    /** 实际出勤人天 */
    @Column(name = "actual_attendance_days", precision = 10, scale = 1)
    private BigDecimal actualAttendanceDays;

    /** 实际绩效人天 */
    @Column(name = "actual_performance_days", precision = 10, scale = 1)
    private BigDecimal actualPerformanceDays;

    /** 执行人 */
    @Column(length = 100)
    private String executor;

    /** 责任人 */
    @Column(length = 100)
    private String responsible;

    /** 创建描述 */
    @Column(name = "create_desc", length = 500)
    private String createDesc;

    /** 同步日期 */
    @Column(name = "sync_date", length = 50)
    private String syncDate;

    /** 同步状态: ACTIVE / DELETED */
    @Column(name = "sync_status", length = 20)
    private String syncStatus = "ACTIVE";

    /** 财务记账日期 */
    @Column(name = "finance_date", length = 50)
    private String financeDate;

    /** 原始JSON */
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
