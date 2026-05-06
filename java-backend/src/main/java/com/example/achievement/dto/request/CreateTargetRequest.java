package com.example.achievement.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class CreateTargetRequest {

    @NotBlank(message = "部门不能为空")
    private String department;

    @NotBlank(message = "机构不能为空")
    private String organization;

    @NotBlank(message = "目标类型不能为空")
    private String targetType;

    private String category;
    private String subCategory;

    @NotNull(message = "年份不能为空")
    private Integer year;

    private BigDecimal annualTarget;

    private BigDecimal q1Target;
    private BigDecimal q2Target;
    private BigDecimal q3Target;
    private BigDecimal q4Target;

    private BigDecimal q1Actual;
    private BigDecimal q2Actual;
    private BigDecimal q3Actual;
    private BigDecimal q4Actual;

    private String owner;
}
