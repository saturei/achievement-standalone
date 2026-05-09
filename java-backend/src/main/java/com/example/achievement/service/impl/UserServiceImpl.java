package com.example.achievement.service.impl;

import com.example.achievement.entity.UserConfig;
import com.example.achievement.repository.UserConfigRepository;
import com.example.achievement.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {

    private final UserConfigRepository userConfigRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UserConfig> getAllUsers() {
        return userConfigRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserConfig> getEnabledUsers() {
        return userConfigRepository.findByEnabled(1);
    }

    @Override
    public UserConfig createUser(UserConfig user) {
        user.setId(UUID.randomUUID().toString());
        return userConfigRepository.save(user);
    }

    @Override
    public UserConfig updateUser(String id, UserConfig user) {
        UserConfig existing = userConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + id));
        existing.setUsername(user.getUsername());
        existing.setDisplayName(user.getDisplayName());
        existing.setRole(user.getRole());
        existing.setDepartment(user.getDepartment());
        existing.setOrganization(user.getOrganization());
        existing.setEnabled(user.getEnabled());
        return userConfigRepository.save(existing);
    }

    @Override
    public void deleteUser(String id) {
        userConfigRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public UserConfig getByUsername(String username) {
        return userConfigRepository.findByUsername(username).orElse(null);
    }
}
