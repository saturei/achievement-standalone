package com.example.achievement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TargetStatisticsResponse {

    private String dimension;
    private Integer year;
    private List<DimensionStatistics> statistics;
    private Summary summary;
    private Integer rdActual;
    private Integer rdPlanned;
    private Integer rdTarget;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DimensionStatistics {
        private String id;
        private String name;
        private String departmentName;
        private String productName;
        private String organizationName;
        private String category;
        private String subCategory;
        private String targetType;
        private String owner;
        private BigDecimal target;
        private BigDecimal actual;
        private BigDecimal completionRate;
        private List<QuarterlyData> quarterlyData;
        private BigDecimal signingTarget;
        private BigDecimal signingActual;
        private BigDecimal confirmationTarget;
        private BigDecimal confirmationActual;
        private Integer rdTarget;
        private Integer rdActual;
        private Integer rdPlanned;
        private BigDecimal budgetTarget;
        private BigDecimal budgetActual;
        private BigDecimal annualTarget;
        private BigDecimal actualValue;
        private BigDecimal q1Target;
        private BigDecimal q2Target;
        private BigDecimal q3Target;
        private BigDecimal q4Target;
        private BigDecimal q1Actual;
        private BigDecimal q2Actual;
        private BigDecimal q3Actual;
        private BigDecimal q4Actual;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuarterlyData {
        private String quarter;
        private BigDecimal target;
        private BigDecimal actual;
        private BigDecimal completionRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private BigDecimal totalTarget;
        private BigDecimal totalActual;
        private BigDecimal averageCompletionRate;
        private int totalDimensions;
        private BigDecimal signingTarget;
        private BigDecimal signingActual;
        private BigDecimal signingRate;
        private BigDecimal confirmationTarget;
        private BigDecimal confirmationActual;
        private BigDecimal confirmationRate;
        private BigDecimal budgetTarget;
        private BigDecimal budgetActual;
        private BigDecimal budgetRate;
    }
}
