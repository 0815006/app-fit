package com.fit.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 本周训练记录摘要 VO
 */
@Data
@Builder
public class WeeklyWorkoutVO {

    /** 动作名称 */
    private String actionName;

    /** 肌群大类编码 */
    private String muscleGroup;

    /** 肌群中文名 */
    private String muscleGroupName;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 力竭度系数 */
    private BigDecimal exhaustionScore;

    /** 星期几：1=周一, 7=周日 */
    private int dayOfWeek;
}
