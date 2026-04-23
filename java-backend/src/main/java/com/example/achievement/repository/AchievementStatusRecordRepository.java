package com.example.achievement.repository;

import com.example.achievement.entity.AchievementStatusRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AchievementStatusRecordRepository extends JpaRepository<AchievementStatusRecord, String> {

    List<AchievementStatusRecord> findByAchievementIdOrderByChangeTimeDesc(String achievementId);

    Page<AchievementStatusRecord> findByAchievementId(String achievementId, Pageable pageable);
}
