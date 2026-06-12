package com.example.achievement.controller;

import com.example.achievement.entity.UserConfig;
import com.example.achievement.repository.UserConfigRepository;
import com.example.achievement.service.DingTalkLoginService;
import com.example.achievement.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final DingTalkLoginService dingTalkLoginService;
    private final UserConfigRepository userConfigRepository;

    /**
     * DingTalk H5 micro-app login.
     * Frontend calls dd.getAuthCode() and sends the auth code here.
     */
    @PostMapping("/dingtalk/login")
    public ResponseEntity<Map<String, Object>> dingTalkLogin(@RequestBody Map<String, String> body) {
        String authCode = body.get("authCode");
        if (authCode == null || authCode.trim().isEmpty()) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "authCode不能为空");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            Map<String, Object> result = dingTalkLoginService.login(authCode);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("钉钉登录失败", e);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "登录失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    /**
     * Get current authenticated user info.
     * The JWT filter sets UserContext with the userId.
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        String userId = UserContext.get();
        if (userId == null) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "未登录");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        Optional<UserConfig> userOpt = userConfigRepository.findByUserId(userId);
        if (userOpt.isEmpty()) {
            // Fallback: try by username
            userOpt = userConfigRepository.findByUsername(userId);
        }

        if (userOpt.isPresent()) {
            UserConfig user = userOpt.get();
            Map<String, Object> userMap = new LinkedHashMap<>();
            userMap.put("username", user.getUsername());
            userMap.put("displayName", user.getDisplayName());
            userMap.put("role", user.getRole());
            userMap.put("avatar", user.getAvatar());
            userMap.put("userId", user.getUserId());
            userMap.put("department", user.getDepartment());
            userMap.put("organization", user.getOrganization());
            return ResponseEntity.ok(userMap);
        }

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("error", "用户不存在");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
