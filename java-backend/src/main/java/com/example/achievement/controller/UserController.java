package com.example.achievement.controller;

import com.example.achievement.entity.UserConfig;
import com.example.achievement.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getEnabledUsers() {
        List<Map<String, Object>> users = userService.getEnabledUsers().stream()
                .map(u -> {
                    Map<String, Object> map = new java.util.LinkedHashMap<>();
                    map.put("username", u.getUsername());
                    map.put("displayName", u.getDisplayName());
                    map.put("role", u.getRole());
                    map.put("department", u.getDepartment());
                    map.put("organization", u.getOrganization());
                    return map;
                })
                .collect(Collectors.toList());

        Map<String, Object> admin = new java.util.LinkedHashMap<>();
        admin.put("username", "admin");
        admin.put("displayName", "管理员");
        admin.put("role", "ADMIN");
        admin.put("department", null);
        admin.put("organization", null);
        users.add(0, admin);

        return ResponseEntity.ok(users);
    }

    @GetMapping("/config")
    public ResponseEntity<List<UserConfig>> getAllConfigs() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping
    public ResponseEntity<UserConfig> create(@RequestBody UserConfig user) {
        return ResponseEntity.ok(userService.createUser(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserConfig> update(@PathVariable String id, @RequestBody UserConfig user) {
        return ResponseEntity.ok(userService.updateUser(id, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }
}
