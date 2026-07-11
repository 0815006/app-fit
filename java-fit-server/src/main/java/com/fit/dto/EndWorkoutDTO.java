package com.fit.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EndWorkoutDTO {

    @NotNull(message = "力竭度不能为空")
    @DecimalMin(value = "0.50", message = "力竭度最低为0.50")
    @DecimalMax(value = "1.20", message = "力竭度最高为1.20")
    private BigDecimal exhaustionScore;
}
