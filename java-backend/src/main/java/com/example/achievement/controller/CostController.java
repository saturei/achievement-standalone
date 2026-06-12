package com.example.achievement.controller;

import com.example.achievement.entity.DtProjectOrderCost;
import com.example.achievement.repository.DtProjectOrderCostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cost")
@RequiredArgsConstructor
public class CostController {

    private final DtProjectOrderCostRepository costRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 部门月度成本趋势：预算取 dt_department_budgets，实际取 dt_project_order_costs 按月聚合
     */
    @GetMapping("/department-monthly")
    public ResponseEntity<Map<String, Object>> getDepartmentMonthly(
            @RequestParam(required = false) String department) {

        // 部门年度预算（万元）
        String budgetSql = "SELECT COALESCE(budget_total, 0) FROM dt_department_budgets WHERE dept_name = ?";
        BigDecimal annualBudget = BigDecimal.ZERO;
        try {
            if (department != null && !department.isEmpty()) {
                annualBudget = jdbcTemplate.queryForObject(budgetSql, BigDecimal.class, department);
            } else {
                annualBudget = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(budget_total), 0) FROM dt_department_budgets", BigDecimal.class);
            }
        } catch (Exception ignored) {}
        if (annualBudget == null) annualBudget = BigDecimal.ZERO;
        BigDecimal monthlyBudget = annualBudget.divide(new BigDecimal("12"), 2, java.math.RoundingMode.HALF_UP);

        // 月度实际成本（从 dt_project_order_costs 按 financial_date 的月份聚合）
        StringBuilder actualSql = new StringBuilder(
            "SELECT CAST(SUBSTR(COALESCE(finance_date, sync_date), 6, 2) AS INTEGER) as month, " +
            "COALESCE(SUM(actual_cost_total), 0) as total " +
            "FROM dt_project_order_costs " +
            "WHERE (sync_status IS NULL OR sync_status != 'DELETED') " +
            "AND ((finance_date IS NOT NULL AND finance_date != '') OR (sync_date IS NOT NULL AND sync_date != ''))");
        List<Object> params = new ArrayList<>();
        if (department != null && !department.isEmpty()) {
            actualSql.append(" AND department = ?");
            params.add(department);
        }
        actualSql.append(" GROUP BY month ORDER BY month");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(actualSql.toString(), params.toArray());

        // 构建12个月数组
        List<Map<String, Object>> monthly = new ArrayList<>();
        BigDecimal[] actualByMonth = new BigDecimal[12];
        BigDecimal totalActual = BigDecimal.ZERO;
        for (int i = 0; i < 12; i++) actualByMonth[i] = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            Object m = row.get("month");
            int month = m instanceof Number ? ((Number) m).intValue() : Integer.parseInt(m.toString());
            BigDecimal amt = row.get("total") != null ? new BigDecimal(row.get("total").toString()) : BigDecimal.ZERO;
            if (month >= 1 && month <= 12) {
                actualByMonth[month - 1] = amt;
                totalActual = totalActual.add(amt);
            }
        }

        for (int i = 0; i < 12; i++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("month", i + 1);
            item.put("budget", monthlyBudget);
            item.put("actual", actualByMonth[i]);
            monthly.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("monthly", monthly);
        result.put("annualBudget", annualBudget);
        result.put("totalActual", totalActual);

        // 汇总表数据
        String summarySql = "SELECT department, " +
                "COUNT(DISTINCT order_id) as order_count, " +
                "COUNT(DISTINCT project_id) as project_count, " +
                "COALESCE(SUM(order_budget), 0) as total_budget, " +
                "COALESCE(SUM(actual_cost_total), 0) as total_actual_cost, " +
                "COALESCE(SUM(estimated_man_days), 0) as total_estimated_days, " +
                "COALESCE(SUM(actual_attendance_days), 0) as total_attendance_days " +
                "FROM dt_project_order_costs " +
                "WHERE (sync_status IS NULL OR sync_status != 'DELETED') " +
                "AND department IS NOT NULL AND department != ''";
        if (department != null && !department.isEmpty()) summarySql += " AND department = ?";
        summarySql += " GROUP BY department ORDER BY total_actual_cost DESC";
        List<Map<String, Object>> summary;
        if (department != null && !department.isEmpty()) {
            summary = jdbcTemplate.queryForList(summarySql, department);
        } else {
            summary = jdbcTemplate.queryForList(summarySql);
        }
        // 填充年度预算到汇总行
        for (Map<String, Object> s : summary) {
            if (department != null && !department.isEmpty()) {
                s.put("annual_budget", annualBudget);
            } else {
                try {
                    BigDecimal deptBudget = jdbcTemplate.queryForObject(budgetSql, BigDecimal.class, s.get("department").toString());
                    s.put("annual_budget", deptBudget != null ? deptBudget : BigDecimal.ZERO);
                } catch (Exception e) {
                    s.put("annual_budget", BigDecimal.ZERO);
                }
            }
        }
        result.put("summary", summary);

        return ResponseEntity.ok(result);
    }

    /**
     * 产品月度成本趋势：预算和实际均从 dt_project_order_costs 按月聚合
     */
    @GetMapping("/product-monthly")
    public ResponseEntity<Map<String, Object>> getProductMonthly(
            @RequestParam(required = false) String productId) {

        StringBuilder sql = new StringBuilder(
            "SELECT CAST(SUBSTR(COALESCE(finance_date, sync_date), 6, 2) AS INTEGER) as month, " +
            "COALESCE(SUM(order_budget), 0) as budget, " +
            "COALESCE(SUM(actual_cost_total), 0) as actual " +
            "FROM dt_project_order_costs " +
            "WHERE (sync_status IS NULL OR sync_status != 'DELETED') " +
            "AND ((finance_date IS NOT NULL AND finance_date != '') OR (sync_date IS NOT NULL AND sync_date != ''))");
        List<Object> params = new ArrayList<>();
        if (productId != null && !productId.isEmpty()) {
            sql.append(" AND product_id = ?");
            params.add(productId);
        }
        sql.append(" GROUP BY month ORDER BY month");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params.toArray());

        BigDecimal[] budgetByMonth = new BigDecimal[12];
        BigDecimal[] actualByMonth = new BigDecimal[12];
        BigDecimal totalBudget = BigDecimal.ZERO;
        BigDecimal totalActual = BigDecimal.ZERO;
        for (int i = 0; i < 12; i++) { budgetByMonth[i] = BigDecimal.ZERO; actualByMonth[i] = BigDecimal.ZERO; }

        for (Map<String, Object> row : rows) {
            Object m = row.get("month");
            int month = m instanceof Number ? ((Number) m).intValue() : Integer.parseInt(m.toString());
            BigDecimal b = row.get("budget") != null ? new BigDecimal(row.get("budget").toString()) : BigDecimal.ZERO;
            BigDecimal a = row.get("actual") != null ? new BigDecimal(row.get("actual").toString()) : BigDecimal.ZERO;
            if (month >= 1 && month <= 12) {
                budgetByMonth[month - 1] = b;
                actualByMonth[month - 1] = a;
                totalBudget = totalBudget.add(b);
                totalActual = totalActual.add(a);
            }
        }

        List<Map<String, Object>> monthly = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("month", i + 1);
            item.put("budget", budgetByMonth[i]);
            item.put("actual", actualByMonth[i]);
            monthly.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("monthly", monthly);
        result.put("totalBudget", totalBudget);
        result.put("totalActual", totalActual);

        // 汇总表数据
        String summarySql = "SELECT product_id, " +
                "COALESCE(SUM(order_budget), 0) as total_budget, " +
                "COALESCE(SUM(actual_cost_total), 0) as total_actual_cost, " +
                "COALESCE(SUM(estimated_man_days), 0) as total_estimated_days, " +
                "COALESCE(SUM(actual_attendance_days), 0) as total_attendance_days " +
                "FROM dt_project_order_costs " +
                "WHERE (sync_status IS NULL OR sync_status != 'DELETED') " +
                "AND product_id IS NOT NULL AND product_id != ''";
        if (productId != null && !productId.isEmpty()) summarySql += " AND product_id = ?";
        summarySql += " GROUP BY product_id ORDER BY total_actual_cost DESC";

        List<Map<String, Object>> summary;
        if (productId != null && !productId.isEmpty()) {
            summary = jdbcTemplate.queryForList(summarySql, productId);
        } else {
            summary = jdbcTemplate.queryForList(summarySql);
        }
        result.put("summary", summary);

        return ResponseEntity.ok(result);
    }

    /**
     * 部门-机构对照关系
     */
    @GetMapping("/department-org-map")
    public ResponseEntity<List<Map<String, String>>> getDepartmentOrgMap() {
        String sql = "SELECT DISTINCT department, organization FROM dt_project_order_costs " +
                "WHERE (sync_status IS NULL OR sync_status != 'DELETED') " +
                "AND department IS NOT NULL AND department != '' " +
                "AND organization IS NOT NULL AND organization != '' " +
                "ORDER BY department, organization";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        List<Map<String, String>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("department", (String) row.get("department"));
            m.put("organization", (String) row.get("organization"));
            result.add(m);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 产品-项目对照关系
     */
    @GetMapping("/product-project-map")
    public ResponseEntity<List<Map<String, String>>> getProductProjectMap() {
        String sql = "SELECT DISTINCT product_id, project_id FROM dt_project_order_costs " +
                "WHERE (sync_status IS NULL OR sync_status != 'DELETED') " +
                "AND product_id IS NOT NULL AND product_id != '' AND project_id IS NOT NULL AND project_id != '' " +
                "ORDER BY product_id, project_id";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        List<Map<String, String>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("productId", (String) row.get("product_id"));
            m.put("projectId", (String) row.get("project_id"));
            result.add(m);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 订单成本明细列表（支持搜索和筛选，分页）
     */
    @GetMapping("/order-details")
    public ResponseEntity<Map<String, Object>> getOrderDetails(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String projectId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        List<DtProjectOrderCost> all = costRepository.findAll().stream()
                .filter(o -> o.getSyncStatus() == null || !"DELETED".equals(o.getSyncStatus()))
                .collect(Collectors.toList());

        if (keyword != null && !keyword.isEmpty()) {
            String kw = keyword.toLowerCase();
            all = all.stream().filter(o ->
                (o.getOrderId() != null && o.getOrderId().toLowerCase().contains(kw)) ||
                (o.getOrderName() != null && o.getOrderName().toLowerCase().contains(kw))
            ).collect(Collectors.toList());
        }
        if (department != null && !department.isEmpty()) {
            all = all.stream().filter(o -> department.equals(o.getDepartment())).collect(Collectors.toList());
        }
        if (productId != null && !productId.isEmpty()) {
            all = all.stream().filter(o -> productId.equals(o.getProductId())).collect(Collectors.toList());
        }
        if (projectId != null && !projectId.isEmpty()) {
            all = all.stream().filter(o -> projectId.equals(o.getProjectId())).collect(Collectors.toList());
        }

        int total = all.size();
        int from = (page - 1) * pageSize;
        int to = Math.min(from + pageSize, total);
        List<DtProjectOrderCost> pageData = from < total ? all.subList(from, to) : Collections.emptyList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", pageData);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);

        return ResponseEntity.ok(result);
    }
}
