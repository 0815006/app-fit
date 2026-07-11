package com.fit.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MakeupWorkoutDTO {

    @NotBlank(message = "动作ID不能为空")
    private String actionId;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "力竭度不能为空")
    @DecimalMin(value = "0.50", message = "力竭度最低为0.50")
    @DecimalMax(value = "1.20", message = "力竭度最高为1.20")
    private BigDecimal exhaustionScore;
}
