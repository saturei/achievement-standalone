package com.example.achievement.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class StatusRecordResponse {
    private String id;
    private String achievementId;
    private String achievementName;
    private String oldStatus;
    private String newStatus;
    private String changeReason;
    private String changedBy;
    private LocalDateTime changeTime;
}
