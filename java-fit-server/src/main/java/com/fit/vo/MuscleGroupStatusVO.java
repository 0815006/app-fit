package com.fit.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MuscleGroupStatusVO {

    /** 肌群大类编码（如 CHEST） */
    private String muscleGroup;

    /** 肌群大类中文名（如 胸部） */
    private String muscleGroupName;

    /** 本周打卡动作种类数 */
    private int weeklyCount;

    /** 恢复状态：READY / RECOVERING */
    private String status;

    /** 恢复中时的剩余秒数，READY 时为 0 */
    private long remainingSeconds;
}
