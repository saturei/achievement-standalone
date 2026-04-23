package com.example.achievement.dto.request;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AchievementRecordRequest {

    private String version;
    private LocalDate actualAcceptanceDate;
    private String demoUrl;
    private String codeRepositoryUrl;
    private String deliverables;
    private LocalDateTime recordTime;
    private String recordNotes;
}
