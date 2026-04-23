package com.example.achievement.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class AchievementListResponse {

    private long total;
    private int page;
    private int pageSize;
    private List<AchievementResponse> items;
}
