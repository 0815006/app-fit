package com.fit.service;

import com.fit.vo.DashboardVO;
import com.fit.vo.TimeoutRecordVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface GymWorkoutRecordService {

    /**
     * 开始训练：根据 actionId 自动推导 muscleGroup，创建 status=0 的记录
     * @param userId 当前用户ID
     * @param actionId 动作ID
     * @return 训练记录ID
     */
    String startWorkout(String userId, String actionId);

    /**
     * 结束训练：写入 end_time=now + 力竭度，status→1
     */
    void endWorkout(String recordId, BigDecimal exhaustionScore);

    /**
     * 超时修正：end_time = start_time + actualMinutes，status→2
     */
    void correctTimeout(String recordId, int actualMinutes, BigDecimal exhaustionScore);

    /**
     * 核心看板：返回所有肌群的恢复状态 + 本周打卡次数 + 超时记录
     */
    DashboardVO getDashboard(String userId);

    /**
     * 自由补打卡：end_time = startTime（瞬间打卡），status=1
     */
    void makeupWorkout(String userId, String actionId, LocalDateTime startTime, BigDecimal exhaustionScore);

    /**
     * 检查超时：status=0 且 now - start_time > 2小时 的记录
     */
    TimeoutRecordVO checkTimeout(String userId);
}
