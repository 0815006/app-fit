package com.fit.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardVO {

    /** 已恢复肌群的中文名列表，如["胸部","背部"]，前端拼「今天适合训练的部位：胸部、背部」 */
    private List<String> readyMuscleNames;

    /** 所有肌群的状态列表 */
    private List<MuscleGroupStatusVO> muscleGroups;

    /** 超时记录（若有 status=0 且超2h 的） */
    private TimeoutRecordVO timeoutRecord;
}
