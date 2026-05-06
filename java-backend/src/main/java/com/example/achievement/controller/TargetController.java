package com.example.achievement.controller;

import com.example.achievement.dto.request.ActualDataRequest;
import com.example.achievement.dto.request.CreateTargetRequest;
import com.example.achievement.dto.response.MonthlyDistributionResponse;
import com.example.achievement.dto.response.TargetStatisticsResponse;
import com.example.achievement.service.TargetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/targets")
@Tag(name = "目标统计管理", description = "目标统计相关接口")
@RequiredArgsConstructor
public class TargetController {

    private final TargetService targetService;

    @GetMapping("/statistics")
    @Operation(summary = "获取目标统计数据")
    public ResponseEntity<TargetStatisticsResponse> getStatistics(
            @RequestParam(defaultValue = "organization") String dimension,
            @RequestParam Integer year,
            @RequestParam(required = false) String product,
            @RequestParam(required = false) String organization,
            @RequestParam(required = false) String owner,
            @RequestParam(required = false) String subCategory) {
        System.out.println("========== CONTROLLER DEBUG: getStatistics called with year=" + year);
        TargetStatisticsResponse response = targetService.getStatistics(dimension, year, product, organization, owner, subCategory);
        System.out.println("========== CONTROLLER DEBUG: Returning " + response.getStatistics().size() + " records");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/test-count")
    @Operation(summary = "测试数据库记录数")
    public ResponseEntity<String> testCount() {
        return ResponseEntity.ok(targetService.testDatabaseCount());
    }

    @GetMapping("/distribution")
    @Operation(summary = "获取月度成果分布")
    public ResponseEntity<MonthlyDistributionResponse> getMonthlyDistribution(
            @RequestParam Integer year) {
        return ResponseEntity.ok(targetService.getMonthlyDistribution(year));
    }

    @PostMapping("/actual")
    @Operation(summary = "保存实际数据")
    public ResponseEntity<Boolean> saveActualData(
            @Valid @RequestBody ActualDataRequest request) {
        return ResponseEntity.ok(targetService.saveActualData(request));
    }

    @PostMapping("/import")
    @Operation(summary = "导入目标配置数据")
    public ResponseEntity<String> importTargets(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(targetService.importTargets(file));
    }

    @GetMapping("/products")
    @Operation(summary = "获取所有产品列表")
    public ResponseEntity<java.util.List<String>> getAllProducts() {
        return ResponseEntity.ok(targetService.getAllProducts());
    }

    @GetMapping("/organizations")
    @Operation(summary = "获取所有机构列表")
    public ResponseEntity<java.util.List<String>> getAllOrganizations() {
        return ResponseEntity.ok(targetService.getAllOrganizations());
    }

    @GetMapping("/owners")
    @Operation(summary = "获取所有负责人列表")
    public ResponseEntity<java.util.List<String>> getAllOwners() {
        return ResponseEntity.ok(targetService.getAllOwners());
    }

    @PostMapping
    @Operation(summary = "创建新目标")
    public ResponseEntity<Boolean> createTarget(@Valid @RequestBody CreateTargetRequest request) {
        return ResponseEntity.ok(targetService.createTarget(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新目标")
    public ResponseEntity<Boolean> updateTarget(@PathVariable String id, @Valid @RequestBody CreateTargetRequest request) {
        boolean result = targetService.updateTarget(id, request);
        if (!result) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(true);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除目标")
    public ResponseEntity<Boolean> deleteTarget(@PathVariable String id) {
        boolean result = targetService.deleteTarget(id);
        if (!result) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(true);
    }

    @GetMapping("/quarterly-summary")
    @Operation(summary = "获取季度汇总数据")
    public ResponseEntity<java.util.Map<String, java.util.List<com.example.achievement.dto.response.TargetStatisticsResponse.QuarterlyData>>>
            getQuarterlySummary(@RequestParam Integer year, @RequestParam(required = false) String organization) {
        return ResponseEntity.ok(targetService.getQuarterlySummary(year, organization));
    }
}
