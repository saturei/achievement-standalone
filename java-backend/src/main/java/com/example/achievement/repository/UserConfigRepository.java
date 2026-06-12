package com.example.achievement.repository;

import com.example.achievement.entity.UserConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserConfigRepository extends JpaRepository<UserConfig, String> {

    Optional<UserConfig> findByUsername(String username);

    Optional<UserConfig> findByUserId(String userId);

    List<UserConfig> findByEnabled(Integer enabled);
}
