package com.example.achievement.repository;

import com.example.achievement.entity.AchievementVersionRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AchievementVersionRecordRepository extends JpaRepository<AchievementVersionRecord, String> {

    List<AchievementVersionRecord> findByAchievementIdOrderByChangeTimeDesc(String achievementId);

    Page<AchievementVersionRecord> findByAchievementId(String achievementId, Pageable pageable);

    Page<AchievementVersionRecord> findByAchievementName(String achievementName, Pageable pageable);

    @Query("SELECT v FROM AchievementVersionRecord v WHERE " +
           "(:achievementName IS NULL OR v.achievementName LIKE %:achievementName%) AND " +
           "(:startTime IS NULL OR v.changeTime >= :startTime) AND " +
           "(:endTime IS NULL OR v.changeTime <= :endTime)")
    Page<AchievementVersionRecord> findByConditions(
            @Param("achievementName") String achievementName,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);
}
