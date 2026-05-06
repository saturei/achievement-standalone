package com.example.achievement.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class ActualDataRequest {

    private String targetId;

    @NotBlank(message = "组织不能为空")
    private String organization;

    @NotBlank(message = "数据类型不能为空")
    private String dataType;

    @NotNull(message = "年份不能为空")
    private Integer year;

    @NotNull(message = "月份不能为空")
    private Integer month;

    @NotNull(message = "实际值不能为空")
    private BigDecimal actualValue;

    private String remark;

    private String createdBy;
}
