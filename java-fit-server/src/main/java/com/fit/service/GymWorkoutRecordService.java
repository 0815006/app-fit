package com.fit.service;

import com.fit.vo.DashboardVO;
import com.fit.vo.TimeoutRecordVO;
import com.fit.vo.WeeklyWorkoutVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface GymWorkoutRecordService {

    /**
     * 开始训练：根据 actionId 自动推导 muscleGroup，创建 status=0 的记录
     * @param userId 当前用户ID
     * @param actionId 动作ID
     * @return 训练记录ID
     */
    String startWorkout(String userId, String actionId);

    /**
     * 结束训练：写入训练数据 + end_time=now + 力竭度，status→1
     * @param weight 重量(kg)，可为null
     * @param reps 次数，可为null
     * @param setCount 组数，可为null
     */
    void endWorkout(String recordId, BigDecimal weight, Integer reps, Integer setCount, BigDecimal exhaustionScore);

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

    /**
     * 本周训练摘要：返回本周一至周日所有已结束的训练记录，含动作名和肌群中文名
     */
    List<WeeklyWorkoutVO> getWeeklySummary(String userId);
}
