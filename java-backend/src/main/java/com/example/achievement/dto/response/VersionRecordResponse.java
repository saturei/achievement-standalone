package com.example.achievement.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VersionRecordResponse {
    private String id;
    private String achievementId;
    private String achievementName;
    private String fromVersion;
    private String toVersion;
    private String changedFields;
    private String changeDescription;
    private String changedBy;
    private LocalDateTime changeTime;
}
