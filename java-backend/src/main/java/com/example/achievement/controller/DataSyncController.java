package com.example.achievement.controller;

import com.example.achievement.service.DingTalkDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/data")
@RequiredArgsConstructor
public class DataSyncController {

    private final DingTalkDataService dingTalkDataService;

    @PostMapping("/sync-all")
    public ResponseEntity<Map<String, Object>> syncAll() {
        try {
            Map<String, Object> result = dingTalkDataService.syncAll();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/sync-status")
    public ResponseEntity<Map<String, Object>> syncStatus() {
        Map<String, Object> status = dingTalkDataService.getSyncStatus();
        return ResponseEntity.ok(status);
    }
}
