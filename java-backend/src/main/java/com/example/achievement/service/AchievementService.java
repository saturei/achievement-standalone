package com.example.achievement.service;

import com.example.achievement.dto.request.*;
import com.example.achievement.dto.response.AchievementListResponse;
import com.example.achievement.dto.response.AchievementResponse;
import com.example.achievement.dto.response.AchievementStatisticsResponse;
import com.example.achievement.dto.response.StatusRecordResponse;
import com.example.achievement.dto.response.VersionRecordResponse;

import java.util.List;

public interface AchievementService {

    AchievementListResponse getAchievements(int page, int pageSize, String status, String achievementForm, 
                                           String productId, String keyword, Boolean includeDeleted,
                                           String plannedAcceptanceMonth, String organizationName);

    AchievementStatisticsResponse getStatistics();

    AchievementResponse getAchievement(String achievementId);

    AchievementResponse preRegister(AchievementPreRegisterRequest request);

    AchievementResponse register(String achievementId, AchievementRegisterRequest request);

    AchievementResponse record(String achievementId, AchievementRecordRequest request);

    AchievementResponse change(String achievementId, AchievementChangeRequest request);

    AchievementResponse offline(String achievementId, AchievementOfflineRequest request);

    AchievementResponse online(String achievementId, AchievementOnlineRequest request);

    AchievementResponse delete(String achievementId);

    List<StatusRecordResponse> getStatusRecords(String achievementId);
    
    List<VersionRecordResponse> getVersionRecords(String achievementId);

    List<String> getAllOrganizations();
}
