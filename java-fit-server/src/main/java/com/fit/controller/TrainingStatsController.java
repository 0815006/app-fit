package com.fit.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fit.common.Result;
import com.fit.service.TrainingStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/training-stats")
@RequiredArgsConstructor
public class TrainingStatsController {

    private final TrainingStatsService statsService;

    /** 从 Sa-Token 获取当前登录用户的 userId */
    private String currentUserId() {
        return String.valueOf(StpUtil.getLoginIdAsLong());
    }

    /**
     * 计算1RM
     */
    @GetMapping("/1rm")
    public Result<BigDecimal> calculate1RM(@RequestParam BigDecimal weight, @RequestParam Integer reps) {
        return Result.success(statsService.calculateOneRepMax(weight, reps));
    }

    /**
     * 获取指定动作的历史最佳1RM
     */
    @GetMapping("/best-1rm/{actionId}")
    public Result<BigDecimal> getBest1RM(@PathVariable String actionId) {
        return Result.success(statsService.getBestOneRepMax(currentUserId(), actionId));
    }

    /**
     * 获取指定动作的最近一次训练数据
     */
    @GetMapping("/last-session/{actionId}")
    public Result<Map<String, Object>> getLastSession(@PathVariable String actionId) {
        return Result.success(statsService.getLastSessionDetail(currentUserId(), actionId));
    }

    /**
     * 获取肌群疲劳度
     */
    @GetMapping("/fatigue")
    public Result<Map<String, Integer>> getMuscleFatigue() {
        return Result.success(statsService.calculateMuscleFatigue(currentUserId()));
    }

    /**
     * 获取训练总容量趋势
     */
    @GetMapping("/volume-trend")
    public Result<List<Map<String, Object>>> getVolumeTrend(
            @RequestParam(defaultValue = "week") String groupBy,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : null;
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : null;
        return Result.success(statsService.getVolumeTrend(currentUserId(), groupBy, start, end));
    }

    /**
     * 获取贡献墙数据
     */
    @GetMapping("/contribution")
    public Result<List<Map<String, Object>>> getContributionWall(
            @RequestParam(required = false) Integer year) {
        int y = year != null ? year : LocalDate.now().getYear();
        return Result.success(statsService.getContributionWall(currentUserId(), y));
    }

    /**
     * 坚持榜
     */
    @GetMapping("/ranking/consistency")
    public Result<List<Map<String, Object>>> getConsistencyRanking(
            @RequestParam(defaultValue = "30") int days) {
        return Result.success(statsService.getConsistencyRanking(days));
    }

    /**
     * 进步榜
     */
    @GetMapping("/ranking/progress")
    public Result<List<Map<String, Object>>> getProgressRanking(
            @RequestParam(defaultValue = "30") int days) {
        return Result.success(statsService.getProgressRanking(days));
    }

    /**
     * 平台期检测
     */
    @GetMapping("/plateau")
    public Result<List<Map<String, Object>>> detectPlateau(
            @RequestParam(defaultValue = "6") int weeks) {
        return Result.success(statsService.detectPlateau(currentUserId(), weeks));
    }

    /**
     * 检查是否PR
     */
    @GetMapping("/check-pr")
    public Result<Boolean> checkPr(@RequestParam String actionId,
                                   @RequestParam BigDecimal weight,
                                   @RequestParam Integer reps) {
        return Result.success(statsService.checkIsPr(currentUserId(), actionId, weight, reps));
    }

    /**
     * 力量对比（传入对方 userId）
     */
    @GetMapping("/compare")
    public Result<Map<String, Object>> compareStrength(@RequestParam String userId2) {
        return Result.success(statsService.getStrengthComparison(currentUserId(), userId2));
    }
}
