package com.example.achievement.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final JdbcTemplate jdbcTemplate;

    // ===== KPI 卡片 =====

    @GetMapping("/kpi")
    public ResponseEntity<Map<String, Object>> getKpis() {
        Map<String, Object> kpis = new LinkedHashMap<>();

        // 签约总额(万元)
        kpis.put("signingContractTotal", queryBigDecimal(
                "SELECT COALESCE(SUM(signing_amount_wan),0) FROM dt_signing_contracts"));
        // 派单总额(万元)
        kpis.put("signingOrderTotal", queryBigDecimal(
                "SELECT COALESCE(SUM(order_amount_wan),0) FROM dt_signing_orders"));
        // 确权收入(万元)
        kpis.put("revenueTotal", queryBigDecimal(
                "SELECT COALESCE(SUM(revenue_amount_wan),0) FROM dt_revenue_details"));
        // 交付毛利(万元)
        kpis.put("deliveryMargin", queryBigDecimal(
                "SELECT COALESCE(SUM(delivery_margin),0) FROM dt_revenue_details"));
        // 成果验收数
        kpis.put("acceptedAchievements", queryLong(
                "SELECT COUNT(*) FROM dt_achievements WHERE actual_accept_date IS NOT NULL"));
        // 成果总数
        kpis.put("totalAchievements", queryLong(
                "SELECT COUNT(*) FROM dt_achievements"));
        // 成本合计(万元)
        kpis.put("costTotal", queryBigDecimal(
                "SELECT COALESCE(SUM(actual_cost_total),0) FROM dt_department_budgets"));

        return ResponseEntity.ok(kpis);
    }

    // ===== 部门签约排名 =====

    @GetMapping("/department-signing")
    public ResponseEntity<List<Map<String, Object>>> departmentSigning() {
        String sql = "SELECT department, SUM(amount) as total_amount FROM (" +
                "SELECT department, COALESCE(SUM(signing_amount_wan),0) as amount " +
                "FROM dt_signing_contracts WHERE department IS NOT NULL GROUP BY department " +
                "UNION ALL " +
                "SELECT department, COALESCE(SUM(order_amount_wan),0) as amount " +
                "FROM dt_signing_orders WHERE department IS NOT NULL GROUP BY department" +
                ") t GROUP BY department ORDER BY total_amount DESC";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
        return ResponseEntity.ok(result);
    }

    // ===== 月度收入趋势 =====

    @GetMapping("/revenue-monthly")
    public ResponseEntity<List<Map<String, Object>>> revenueMonthly() {
        String sql = "SELECT recognition_month, COALESCE(SUM(revenue_amount_wan),0) as amount, " +
                "COUNT(*) as count FROM dt_revenue_details " +
                "WHERE recognition_month IS NOT NULL " +
                "GROUP BY recognition_month ORDER BY recognition_month";
        return ResponseEntity.ok(jdbcTemplate.queryForList(sql));
    }

    // ===== 成果状态分布 =====

    @GetMapping("/achievement-status")
    public ResponseEntity<Map<String, Object>> achievementStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("byForm", jdbcTemplate.queryForList(
                "SELECT achievement_form, COUNT(*) as count FROM dt_achievements GROUP BY achievement_form"));
        status.put("byOrg", jdbcTemplate.queryForList(
                "SELECT org_unit, COUNT(*) as total, " +
                "SUM(CASE WHEN actual_accept_date IS NOT NULL THEN 1 ELSE 0 END) as accepted " +
                "FROM dt_achievements WHERE org_unit IS NOT NULL GROUP BY org_unit"));
        return ResponseEntity.ok(status);
    }

    // ===== 签约风险结构 =====

    @GetMapping("/signing-risk")
    public ResponseEntity<List<Map<String, Object>>> signingRisk() {
        String sql = "SELECT signing_risk_level, COALESCE(SUM(signing_amount_wan),0) as amount, " +
                "COUNT(*) as count FROM dt_signing_contracts " +
                "WHERE signing_risk_level IS NOT NULL GROUP BY signing_risk_level";
        return ResponseEntity.ok(jdbcTemplate.queryForList(sql));
    }

    // ===== 部门预算 vs 实际 =====

    @GetMapping("/department-budget")
    public ResponseEntity<List<Map<String, Object>>> departmentBudget() {
        String sql = "SELECT dept_name, dept_head, " +
                "COALESCE(budget_total,0) as budget, COALESCE(actual_cost_total,0) as actual, " +
                "(COALESCE(actual_cost_total,0) - COALESCE(budget_total,0)) as diff, " +
                "CASE WHEN COALESCE(budget_total,0) > 0 " +
                "THEN ROUND(COALESCE(actual_cost_total,0)*100.0/budget_total,1) ELSE 0 END as execute_rate " +
                "FROM dt_department_budgets ORDER BY actual_cost_total DESC";
        return ResponseEntity.ok(jdbcTemplate.queryForList(sql));
    }

    // ===== 产品概览 =====

    @GetMapping("/products")
    public ResponseEntity<List<Map<String, Object>>> products() {
        String sql = "SELECT product_id, product_name, product_department, " +
                "COALESCE(annual_budget_wan,0) as budget, COALESCE(cost_accumulated,0) as cost " +
                "FROM dt_products";
        return ResponseEntity.ok(jdbcTemplate.queryForList(sql));
    }

    // ===== 列表数据端点（供前端表格追踪器使用） =====

    @GetMapping("/table/signing-contracts")
    public ResponseEntity<List<Map<String, Object>>> signingContracts(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String quarter) {
        StringBuilder sql = new StringBuilder("SELECT * FROM dt_signing_contracts WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (department != null && !department.isEmpty()) {
            sql.append(" AND department = ?");
            params.add(department);
        }
        if (riskLevel != null && !riskLevel.isEmpty()) {
            sql.append(" AND signing_risk_level = ?");
            params.add(riskLevel);
        }
        if (quarter != null && !quarter.isEmpty()) {
            sql.append(" AND signing_quarter = ?");
            params.add(quarter);
        }
        sql.append(" ORDER BY signing_amount_wan DESC");
        return ResponseEntity.ok(jdbcTemplate.queryForList(sql.toString(), params.toArray()));
    }

    @GetMapping("/table/signing-orders")
    public ResponseEntity<List<Map<String, Object>>> signingOrders(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String quarter) {
        StringBuilder sql = new StringBuilder("SELECT * FROM dt_signing_orders WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (department != null && !department.isEmpty()) {
            sql.append(" AND department = ?");
            params.add(department);
        }
        if (quarter != null && !quarter.isEmpty()) {
            sql.append(" AND signing_quarter = ?");
            params.add(quarter);
        }
        sql.append(" ORDER BY order_amount_wan DESC");
        return ResponseEntity.ok(jdbcTemplate.queryForList(sql.toString(), params.toArray()));
    }

    @GetMapping("/table/revenue-details")
    public ResponseEntity<List<Map<String, Object>>> revenueDetails(
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String orgUnit) {
        StringBuilder sql = new StringBuilder("SELECT * FROM dt_revenue_details WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (month != null && !month.isEmpty()) {
            sql.append(" AND recognition_month = ?");
            params.add(month);
        }
        if (orgUnit != null && !orgUnit.isEmpty()) {
            sql.append(" AND org_unit = ?");
            params.add(orgUnit);
        }
        sql.append(" ORDER BY revenue_amount_wan DESC");
        return ResponseEntity.ok(jdbcTemplate.queryForList(sql.toString(), params.toArray()));
    }

    @GetMapping("/table/achievements")
    public ResponseEntity<List<Map<String, Object>>> achievements(
            @RequestParam(required = false) String orgUnit,
            @RequestParam(required = false) String status) {
        StringBuilder sql = new StringBuilder("SELECT * FROM dt_achievements WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (orgUnit != null && !orgUnit.isEmpty()) {
            sql.append(" AND org_unit = ?");
            params.add(orgUnit);
        }
        if ("accepted".equals(status)) {
            sql.append(" AND actual_accept_date IS NOT NULL");
        } else if ("planned".equals(status)) {
            sql.append(" AND planned_accept_date IS NOT NULL AND actual_accept_date IS NULL");
        }
        sql.append(" ORDER BY achievement_name");
        return ResponseEntity.ok(jdbcTemplate.queryForList(sql.toString(), params.toArray()));
    }

    // ===== 筛选选项 =====

    @GetMapping("/filter-options")
    public ResponseEntity<Map<String, Object>> filterOptions() {
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("departments", jdbcTemplate.queryForList(
                "SELECT DISTINCT department FROM dt_signing_contracts WHERE department IS NOT NULL " +
                "UNION SELECT DISTINCT department FROM dt_signing_orders WHERE department IS NOT NULL"));
        options.put("riskLevels", jdbcTemplate.queryForList(
                "SELECT DISTINCT signing_risk_level FROM dt_signing_contracts WHERE signing_risk_level IS NOT NULL"));
        options.put("quarters", jdbcTemplate.queryForList(
                "SELECT DISTINCT signing_quarter FROM dt_signing_contracts WHERE signing_quarter IS NOT NULL"));
        options.put("orgUnits", jdbcTemplate.queryForList(
                "SELECT DISTINCT org_unit FROM dt_achievements WHERE org_unit IS NOT NULL"));
        options.put("recognitionMonths", jdbcTemplate.queryForList(
                "SELECT DISTINCT recognition_month FROM dt_revenue_details " +
                "WHERE recognition_month IS NOT NULL ORDER BY recognition_month"));
        return ResponseEntity.ok(options);
    }

    // ===== 工具方法 =====

    private BigDecimal queryBigDecimal(String sql) {
        try {
            Object result = jdbcTemplate.queryForObject(sql, BigDecimal.class);
            return result != null ? (BigDecimal) result : BigDecimal.ZERO;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private long queryLong(String sql) {
        try {
            Long result = jdbcTemplate.queryForObject(sql, Long.class);
            return result != null ? result : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }
}
