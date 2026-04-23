package com.example.achievement.dto.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class ChangeRequest {
    @NotBlank(message = "成果ID不能为空")
    private String achievementId;
    
    private String changeReason;
    
    private String changedBy;
}
