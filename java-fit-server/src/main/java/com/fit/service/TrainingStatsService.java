package com.fit.service;

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
     * 获取坚持榜数据
     * @param days 统计天数（默认30天）
     */
    List<Map<String, Object>> getConsistencyRanking(int days);

    /**
     * 获取进步榜数据（基于1RM增长率）
     * @param days 统计天数（默认30天）
     */
    List<Map<String, Object>> getProgressRanking(int days);

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
