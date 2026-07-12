package com.fit.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

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

    /** 该肌群下面的二级肌肉状态列表 */
    @Builder.Default
    private List<SubMuscleStatus> subMuscles = List.of();

    // ============================================================
    // 二级肌肉状态（内嵌 VO）
    // ============================================================

    @Data
    @Builder
    public static class SubMuscleStatus {
        /** 肌肉编码（如 CHEST_MAJOR） */
        private String muscleCode;
        /** 肌肉中文名（如 胸大肌） */
        private String muscleName;
        /** 本周是否有训练记录 */
        private boolean trainedThisWeek;
    }
}
