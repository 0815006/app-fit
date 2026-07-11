package com.fit.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CorrectTimeoutDTO {

    @NotNull(message = "实际训练分钟数不能为空")
    @Min(value = 1, message = "实际训练分钟数至少为1")
    private int actualMinutes;

    @NotNull(message = "力竭度不能为空")
    @DecimalMin(value = "0.50", message = "力竭度最低为0.50")
    @DecimalMax(value = "1.20", message = "力竭度最高为1.20")
    private BigDecimal exhaustionScore;
}
