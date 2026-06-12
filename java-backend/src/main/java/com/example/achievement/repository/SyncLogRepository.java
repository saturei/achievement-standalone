package com.example.achievement.repository;

import com.example.achievement.entity.SyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SyncLogRepository extends JpaRepository<SyncLog, String> {

    List<SyncLog> findAllByOrderByCreatedAtDesc();

    List<SyncLog> findBySheetIdAndStatusOrderByCreatedAtDesc(String sheetId, String status);

    List<SyncLog> findByTableNameAndStatusOrderByCreatedAtDesc(String tableName, String status);
}
