package com.example.achievement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyDistributionResponse {

    private Integer year;
    private List<MonthlyData> monthlyData;
    private TotalSummary summary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyData {
        private Integer month;
        private Integer count;
        private List<OrganizationData> byOrganization;
        private List<StatusData> byStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusData {
        private String status;
        private Integer count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrganizationData {
        private String organization;
        private Integer count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TotalSummary {
        private Integer totalCount;
        private Double averagePerMonth;
        private Integer maxMonth;
        private Integer maxCount;
        private Integer minMonth;
        private Integer minCount;
    }
}
