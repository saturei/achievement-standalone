package com.example.achievement.dto.request;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AchievementRegisterRequest {

    private LocalDateTime registerTime;
    private String registerNotes;
    private String version;
    private String achievementTarget;
    private LocalDate plannedAcceptanceDate;
    private String estimatedAcceptanceMonth;
    private String acceptanceMethod;
    private String acceptor;
    private String acceptanceOrganization;
    private String acceptanceRequirements;
    private String functionListFile;
    private String packageIds;
}
