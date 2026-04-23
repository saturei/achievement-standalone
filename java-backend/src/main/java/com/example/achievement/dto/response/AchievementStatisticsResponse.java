package com.example.achievement.dto.response;

import lombok.Data;

import java.util.Map;

@Data
public class AchievementStatisticsResponse {

    private long totalCount;
    private long preRegisterCount;
    private long registerCount;
    private long recordCount;
    private long offlineCount;
    private Map<String, Long> byStatus;
    private Map<String, Long> byType;
    private Map<String, Long> byProduct;
    private long checkoutCount;
}
