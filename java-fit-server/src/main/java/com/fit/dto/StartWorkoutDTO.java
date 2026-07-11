package com.fit.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StartWorkoutDTO {

    @NotBlank(message = "动作ID不能为空")
    private String actionId;
}
