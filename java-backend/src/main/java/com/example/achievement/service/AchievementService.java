package com.example.achievement.service;

import com.example.achievement.dto.request.*;
import com.example.achievement.dto.response.AchievementListResponse;
import com.example.achievement.dto.response.AchievementResponse;
import com.example.achievement.dto.response.AchievementStatisticsResponse;
import com.example.achievement.dto.response.StatusRecordResponse;
import com.example.achievement.dto.response.VersionRecordResponse;

import java.util.List;
import java.util.Map;

public interface AchievementService {

    AchievementListResponse getAchievements(int page, int pageSize, String keyword, String departmentName,
                                           String organizationNames, String status,
                                           String productId, Boolean includeDeleted);

    Map<String, List<String>> getFilteredOptions(String keyword, String departmentName,
                                                  String organizationNames, String status, String productId);

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

    List<String> getAllDepartments();

    List<String> getAllOwners();

    List<String> getAllTypes();
}
