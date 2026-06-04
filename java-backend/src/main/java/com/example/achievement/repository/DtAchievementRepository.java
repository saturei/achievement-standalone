package com.example.achievement.repository;

import com.example.achievement.entity.DtAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DtAchievementRepository extends JpaRepository<DtAchievement, String> {
}
