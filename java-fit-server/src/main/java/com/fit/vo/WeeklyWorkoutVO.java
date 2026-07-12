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

    /** 重量(kg)，可为 null */
    private BigDecimal weight;

    /** 次数，可为 null */
    private Integer reps;

    /** 组数，可为 null */
    private Integer setCount;

    /** 1RM估值(kg)，可为 null */
    private BigDecimal rmEstimate;

    /** 是否破个人纪录 */
    private Boolean isPr;

    /** 力竭度系数 */
    private BigDecimal exhaustionScore;

    /** 星期几：1=周一, 7=周日 */
    private int dayOfWeek;
}
