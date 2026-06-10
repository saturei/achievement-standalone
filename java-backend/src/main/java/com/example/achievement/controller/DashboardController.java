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

    // 季度→月份范围映射
    private static final Map<String, String> QUARTER_MONTHS = new LinkedHashMap<>();
    static {
        QUARTER_MONTHS.put("一季度", "'01','02','03'");
        QUARTER_MONTHS.put("二季度", "'04','05','06'");
        QUARTER_MONTHS.put("三季度", "'07','08','09'");
        QUARTER_MONTHS.put("四季度", "'10','11','12'");
    }

    // ===== KPI 卡片 =====

    @GetMapping("/kpi")
    public ResponseEntity<Map<String, Object>> getKpis(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String orgUnit,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String quarter) {
        Map<String, Object> kpis = new LinkedHashMap<>();

        kpis.put("signingContractTotal", queryBigDecimal(
                applyFilter("SELECT COALESCE(SUM(signing_amount_wan),0) FROM dt_signing_contracts",
                        null, department, null, year, quarter, "signing_quarter")));
        kpis.put("signingOrderTotal", queryBigDecimal(
                applyFilter("SELECT COALESCE(SUM(order_amount_wan),0) FROM dt_signing_orders",
                        null, department, null, year, quarter, "signing_quarter")));
        kpis.put("revenueTotal", queryBigDecimal(
                applyFilter("SELECT COALESCE(SUM(revenue_amount_wan),0) FROM dt_revenue_details",
                        orgUnit, null, null, year, quarter, "recognition_month")));
        kpis.put("deliveryMargin", queryBigDecimal(
                applyFilter("SELECT COALESCE(SUM(delivery_margin),0) FROM dt_revenue_details",
                        orgUnit, null, null, year, quarter, "recognition_month")));
        kpis.put("acceptedAchievements", queryLong(
                applyFilter("SELECT COUNT(*) FROM dt_achievements WHERE actual_accept_date IS NOT NULL",
                        orgUnit, null, null, year, quarter, "planned_accept_date")));
        kpis.put("totalAchievements", queryLong(
                applyFilter("SELECT COUNT(*) FROM dt_achievements",
                        orgUnit, null, null, year, quarter, "planned_accept_date")));
        kpis.put("costTotal", queryBigDecimal(
                applyFilter("SELECT COALESCE(SUM(actual_cost_total),0) FROM dt_department_budgets",
                        null, department, "dept_name", year, null, "year")));

        return ResponseEntity.ok(kpis);
    }

    // ===== 部门签约排名 =====

    @GetMapping("/department-signing")
    public ResponseEntity<List<Map<String, Object>>> departmentSigning(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String quarter) {
        String contractPart = applyFilter(
                "SELECT department, COALESCE(SUM(signing_amount_wan),0) as amount " +
                "FROM dt_signing_contracts WHERE department IS NOT NULL GROUP BY department",
                null, department, null, year, quarter, "signing_quarter");
        String orderPart = applyFilter(
                "SELECT department, COALESCE(SUM(order_amount_wan),0) as amount " +
                "FROM dt_signing_orders WHERE department IS NOT NULL GROUP BY department",
                null, department, null, year, quarter, "signing_quarter");

        String sql = "SELECT department, SUM(amount) as total_amount FROM (" +
                contractPart + " UNION ALL " + orderPart +
                ") t GROUP BY department ORDER BY total_amount DESC";
        return ResponseEntity.ok(jdbcTemplate.queryForList(sql));
    }

    // ===== 月度收入趋势 =====

    @GetMapping("/revenue-monthly")
    public ResponseEntity<List<Map<String, Object>>> revenueMonthly(
            @RequestParam(required = false) String orgUnit,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String quarter) {
        String sql = applyFilter(
                "SELECT recognition_month, COALESCE(SUM(revenue_amount_wan),0) as amount, " +
                "COUNT(*) as count FROM dt_revenue_details WHERE recognition_month IS NOT NULL",
                orgUnit, null, null, year, quarter, "recognition_month");
        sql += " GROUP BY recognition_month ORDER BY recognition_month";
        return ResponseEntity.ok(jdbcTemplate.queryForList(sql));
    }

    // ===== 成果状态分布 =====

    @GetMapping("/achievement-status")
    public ResponseEntity<Map<String, Object>> achievementStatus(
            @RequestParam(required = false) String orgUnit,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String quarter) {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("byForm", jdbcTemplate.queryForList(
                applyFilter("SELECT achievement_form, COUNT(*) as count FROM dt_achievements",
                        orgUnit, null, null, year, quarter, "planned_accept_date") +
                " GROUP BY achievement_form"));
        status.put("byOrg", jdbcTemplate.queryForList(
                applyFilter("SELECT org_unit, COUNT(*) as total, " +
                "SUM(CASE WHEN actual_accept_date IS NOT NULL THEN 1 ELSE 0 END) as accepted " +
                "FROM dt_achievements WHERE org_unit IS NOT NULL",
                orgUnit, null, null, year, quarter, "planned_accept_date") +
                " GROUP BY org_unit"));
        return ResponseEntity.ok(status);
    }

    // ===== 签约风险结构 =====

    @GetMapping("/signing-risk")
    public ResponseEntity<List<Map<String, Object>>> signingRisk(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String quarter) {
        String sql = applyFilter(
                "SELECT signing_risk_level, COALESCE(SUM(signing_amount_wan),0) as amount, " +
                "COUNT(*) as count FROM dt_signing_contracts WHERE signing_risk_level IS NOT NULL",
                null, department, null, year, quarter, "signing_quarter");
        sql += " GROUP BY signing_risk_level";
        return ResponseEntity.ok(jdbcTemplate.queryForList(sql));
    }

    // ===== 部门预算 vs 实际 =====

    @GetMapping("/department-budget")
    public ResponseEntity<List<Map<String, Object>>> departmentBudget(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String year) {
        String sql = applyFilter(
                "SELECT dept_name, dept_head, " +
                "COALESCE(budget_total,0) as budget, COALESCE(actual_cost_total,0) as actual, " +
                "(COALESCE(actual_cost_total,0) - COALESCE(budget_total,0)) as diff, " +
                "CASE WHEN COALESCE(budget_total,0) > 0 " +
                "THEN ROUND(COALESCE(actual_cost_total,0)*100.0/budget_total,1) ELSE 0 END as execute_rate " +
                "FROM dt_department_budgets",
                null, department, "dept_name", year, null, "year");
        sql += " ORDER BY actual_cost_total DESC";
        return ResponseEntity.ok(jdbcTemplate.queryForList(sql));
    }

    // ===== 产品概览 =====

    @GetMapping("/products")
    public ResponseEntity<List<Map<String, Object>>> products(
            @RequestParam(required = false) String department) {
        String sql = "SELECT product_id, product_name, product_department, " +
                "COALESCE(annual_budget_wan,0) as budget, COALESCE(cost_accumulated,0) as cost " +
                "FROM dt_products";
        if (department != null && !department.isEmpty()) {
            sql += " WHERE product_department = '" + department.replace("'", "''") + "'";
        }
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
                "SELECT DISTINCT signing_quarter FROM dt_signing_contracts " +
                "WHERE signing_quarter IS NOT NULL AND signing_quarter != ''"));
        options.put("orgUnits", jdbcTemplate.queryForList(
                "SELECT DISTINCT org_unit FROM dt_achievements WHERE org_unit IS NOT NULL"));
        options.put("recognitionMonths", jdbcTemplate.queryForList(
                "SELECT DISTINCT recognition_month FROM dt_revenue_details " +
                "WHERE recognition_month IS NOT NULL ORDER BY recognition_month"));
        options.put("years", jdbcTemplate.queryForList(
                "SELECT DISTINCT SUBSTR(recognition_month,1,4) as year FROM dt_revenue_details " +
                "WHERE recognition_month IS NOT NULL " +
                "UNION SELECT DISTINCT CAST(year AS TEXT) FROM dt_department_budgets WHERE year IS NOT NULL " +
                "ORDER BY year DESC"));
        return ResponseEntity.ok(options);
    }

    // ===== 动态 WHERE 构建 =====

    /**
     * 在已有 SQL 后追加筛选条件。自动处理 WHERE 缺失和 GROUP BY 位置。
     */
    private String applyFilter(String baseSql, String orgUnit,
                                String department, String deptColumn,
                                String year, String quarter, String timeColumn) {
        // 分离 GROUP BY 及之后的部分
        String upperSql = baseSql.toUpperCase();
        String prefix = baseSql;
        String suffix = "";
        int gbIdx = upperSql.indexOf("GROUP BY");
        if (gbIdx >= 0) {
            prefix = baseSql.substring(0, gbIdx);
            suffix = " " + baseSql.substring(gbIdx);
        }
        // 若无 WHERE，补 WHERE 1=1（在 GROUP BY 之前）
        if (!prefix.toUpperCase().contains("WHERE")) {
            prefix += " WHERE 1=1";
        }
        // 拼接筛选条件
        StringBuilder cond = new StringBuilder();
        if (orgUnit != null && !orgUnit.isEmpty()) {
            cond.append(" AND org_unit = '").append(orgUnit.replace("'", "''")).append("'");
        }
        if (department != null && !department.isEmpty()) {
            String col = (deptColumn != null) ? deptColumn : "department";
            cond.append(" AND ").append(col).append(" = '")
                    .append(department.replace("'", "''")).append("'");
        }
        if (year != null && !year.isEmpty() && timeColumn != null) {
            cond.append(" AND ").append(timeColumn).append(" LIKE '")
                    .append(year.replace("'", "''")).append("%'");
        }
        if (quarter != null && !quarter.isEmpty() && timeColumn != null) {
            appendQuarterCondition(cond, quarter, timeColumn);
        }
        return prefix + cond.toString() + suffix;
    }

    /**
     * 根据时间列类型追加季度条件。
     */
    private void appendQuarterCondition(StringBuilder cond, String quarter, String timeColumn) {
        String safeQuarter = quarter.replace("'", "''");
        if ("signing_quarter".equals(timeColumn)) {
            cond.append(" AND ").append(timeColumn).append(" = '").append(safeQuarter).append("'");
        } else {
            String months = QUARTER_MONTHS.get(quarter);
            if (months != null) {
                cond.append(" AND CAST(SUBSTR(").append(timeColumn).append(",6,2) AS INTEGER) IN (")
                        .append(months).append(")");
            }
        }
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
