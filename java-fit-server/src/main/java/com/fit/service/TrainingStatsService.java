package com.fit.service;

import com.fit.vo.RankingItemVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 训练统计服务接口
 */
public interface TrainingStatsService {

    /**
     * 计算1RM（最大单次重量）
     * 公式: 1RM = w × (1 + r/30)
     */
    BigDecimal calculateOneRepMax(BigDecimal weight, Integer reps);

    /**
     * 获取指定动作的历史最佳1RM
     */
    BigDecimal getBestOneRepMax(String userId, String actionId);

    /**
     * 获取指定动作的最近一次训练数据
     */
    Map<String, Object> getLastSessionDetail(String userId, String actionId);

    /**
     * 计算各肌群疲劳度
     * @return Map<肌群, 距上次训练小时数>
     */
    Map<String, Integer> calculateMuscleFatigue(String userId);

    /**
     * 获取训练总容量趋势（按周/月）
     * @param groupBy "week" 或 "month"
     */
    List<Map<String, Object>> getVolumeTrend(String userId, String groupBy, LocalDate startDate, LocalDate endDate);

    /**
     * 获取训练贡献墙数据（类似GitHub贡献图）
     */
    List<Map<String, Object>> getContributionWall(String userId, int year);

    /**
     * 获取坚持榜数据（旧版，基于 training_session）
     * @param days 统计天数（默认30天）
     */
    List<Map<String, Object>> getConsistencyRanking(int days);

    /**
     * 获取进步榜数据（旧版，基于 training_session_detail）
     * @param days 统计天数（默认30天）
     */
    List<Map<String, Object>> getProgressRanking(int days);

    // ═══════════════════════════════════════════════
    // V2 榜单（基于 gym_workout_record，返回 RankingItemVO）
    // ═══════════════════════════════════════════════

    /**
     * 坚持榜 V2：周期内累计打卡天数排名（基于 gym_workout_record）
     * @param days 统计天数
     */
    List<RankingItemVO> getConsistencyRankingV2(int days);

    /**
     * 坚持榜-连续打卡：周期内最长连续打卡天数排名（基于 gym_workout_record）
     * @param days 统计天数
     */
    List<RankingItemVO> getConsistencyStreakRanking(int days);

    /**
     * 容量榜：周期内训练总容量（weight × reps × setCount）排名
     * @param days 统计天数
     */
    List<RankingItemVO> getVolumeRanking(int days);

    /**
     * 容量榜：单次最大容量排行（深蹲/卧推/硬拉/三大项之和）
     * @param days 统计天数
     * @param liftType 三大项类型：bench / squat / deadlift / all
     */
    List<RankingItemVO> getMaxSingleVolumeRanking(int days, String liftType);

    /**
     * 1RM巅峰榜：单次最大 1RM 排行
     * @param days 统计天数
     * @param lift 三大项类型：bench / squat / deadlift / all
     */
    List<RankingItemVO> getPeak1RMRanking(int days, String lift);

    /**
     * 进步榜 V2：相比上个周期 1RM 增长百分比排名
     * @param days 统计天数
     * @param lift 三大项类型：bench / squat / deadlift / all
     */
    List<RankingItemVO> getProgressRankingV2(int days, String lift);

    /**
     * 检测平台期/停滞预警
     * @return 停滞的动作列表
     */
    List<Map<String, Object>> detectPlateau(String userId, int weeks);

    /**
     * 检查是否破纪录（PR）
     */
    boolean checkIsPr(String userId, String actionId, BigDecimal weight, Integer reps);

    /**
     * 获取力量对比数据（用于PK）
     */
    Map<String, Object> getStrengthComparison(String userId1, String userId2);
}
