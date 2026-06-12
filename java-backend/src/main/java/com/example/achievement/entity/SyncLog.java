package com.example.achievement.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sync_log")
public class SyncLog {

    @Id
    private String id;

    /** 钉钉 sheet ID */
    @Column(nullable = false)
    private String sheetId;

    /** 钉钉 sheet 名称 */
    @Column(nullable = false)
    private String sheetName;

    /** 目标数据库表名 */
    @Column(nullable = false)
    private String tableName;

    /** 操作类型: SYNC, ROLLBACK */
    @Column(nullable = false)
    private String operation;

    /** 同步前记录数 */
    private Integer recordCountBefore;

    /** 同步后记录数 */
    private Integer recordCountAfter;

    /** 状态: SUCCESS, FAILED, ROLLED_BACK */
    @Column(nullable = false)
    private String status;

    /** 错误信息 */
    @Column(length = 2000)
    private String errorMessage;

    /** 同步前数据快照（JSON，用于回滚恢复） */
    @Column(columnDefinition = "TEXT")
    private String snapshot;

    /** 操作人 */
    private String createdBy;

    /** 操作时间 */
    @Column(nullable = false)
    private LocalDateTime createdAt;
}
