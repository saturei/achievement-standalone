package com.example.achievement.service;

import com.example.achievement.entity.UserConfig;

import java.util.List;

public interface UserService {

    List<UserConfig> getAllUsers();

    List<UserConfig> getEnabledUsers();

    UserConfig createUser(UserConfig user);

    UserConfig updateUser(String id, UserConfig user);

    void deleteUser(String id);

    UserConfig getByUsername(String username);
}
