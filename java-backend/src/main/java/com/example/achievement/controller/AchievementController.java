package com.example.achievement.controller;

import com.example.achievement.dto.request.*;
import com.example.achievement.dto.response.AchievementListResponse;
import com.example.achievement.dto.response.AchievementResponse;
import com.example.achievement.dto.response.AchievementStatisticsResponse;
import com.example.achievement.dto.response.StatusRecordResponse;
import com.example.achievement.dto.response.VersionRecordResponse;
import com.example.achievement.service.AchievementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/achievements")
@Tag(name = "成果管理", description = "成果管理相关接口")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService achievementService;

    @GetMapping
    @Operation(summary = "获取成果列表")
    public ResponseEntity<AchievementListResponse> getAchievements(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String achievementForm,
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean includeDeleted,
            @RequestParam(required = false) String plannedAcceptanceMonth,
            @RequestParam(required = false) String organizationName) {
        return ResponseEntity.ok(achievementService.getAchievements(
                page, pageSize, status, achievementForm, productId, keyword, includeDeleted, plannedAcceptanceMonth, organizationName));
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取成果统计")
    public ResponseEntity<AchievementStatisticsResponse> getStatistics() {
        return ResponseEntity.ok(achievementService.getStatistics());
    }

    @GetMapping("/{achievementId}")
    @Operation(summary = "获取成果详情")
    public ResponseEntity<AchievementResponse> getAchievement(
            @PathVariable String achievementId) {
        return ResponseEntity.ok(achievementService.getAchievement(achievementId));
    }

    @PostMapping("/pre-register")
    @Operation(summary = "成果预注册")
    public ResponseEntity<AchievementResponse> preRegister(
            @Valid @RequestBody AchievementPreRegisterRequest request) {
        return ResponseEntity.ok(achievementService.preRegister(request));
    }

    @PostMapping("/{achievementId}/register")
    @Operation(summary = "成果注册")
    public ResponseEntity<AchievementResponse> register(
            @PathVariable String achievementId,
            @RequestBody AchievementRegisterRequest request) {
        return ResponseEntity.ok(achievementService.register(achievementId, request));
    }

    @PostMapping("/{achievementId}/record")
    @Operation(summary = "成果登记")
    public ResponseEntity<AchievementResponse> record(
            @PathVariable String achievementId,
            @RequestBody AchievementRecordRequest request) {
        return ResponseEntity.ok(achievementService.record(achievementId, request));
    }

    @PostMapping("/{achievementId}/change")
    @Operation(summary = "成果变更")
    public ResponseEntity<AchievementResponse> change(
            @PathVariable String achievementId,
            @Valid @RequestBody AchievementChangeRequest request) {
        return ResponseEntity.ok(achievementService.change(achievementId, request));
    }

    @PutMapping("/{achievementId}/offline")
    @Operation(summary = "成果下架")
    public ResponseEntity<AchievementResponse> offline(
            @PathVariable String achievementId,
            @RequestBody AchievementOfflineRequest request) {
        return ResponseEntity.ok(achievementService.offline(achievementId, request));
    }

    @PutMapping("/{achievementId}/online")
    @Operation(summary = "成果上架")
    public ResponseEntity<AchievementResponse> online(
            @PathVariable String achievementId,
            @RequestBody AchievementOnlineRequest request) {
        return ResponseEntity.ok(achievementService.online(achievementId, request));
    }

    @PutMapping("/{achievementId}/delete")
    @Operation(summary = "成果删除")
    public ResponseEntity<AchievementResponse> delete(
            @PathVariable String achievementId) {
        return ResponseEntity.ok(achievementService.delete(achievementId));
    }

    @GetMapping("/{achievementId}/history")
    @Operation(summary = "获取成果变更历史")
    public ResponseEntity<List<VersionRecordResponse>> getVersionRecords(
            @PathVariable String achievementId) {
        return ResponseEntity.ok(achievementService.getVersionRecords(achievementId));
    }

    @GetMapping("/organizations")
    @Operation(summary = "获取所有机构列表")
    public ResponseEntity<List<String>> getAllOrganizations() {
        return ResponseEntity.ok(achievementService.getAllOrganizations());
    }
}
