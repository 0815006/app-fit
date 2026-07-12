package com.fit.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fit.common.Result;
import com.fit.dto.CorrectTimeoutDTO;
import com.fit.dto.EndWorkoutDTO;
import com.fit.dto.MakeupWorkoutDTO;
import com.fit.dto.StartWorkoutDTO;
import com.fit.service.GymWorkoutRecordService;
import com.fit.vo.DashboardVO;
import com.fit.vo.TimeoutRecordVO;
import com.fit.vo.WeeklyWorkoutVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/gym-workout")
@RequiredArgsConstructor
public class GymWorkoutController {

    private final GymWorkoutRecordService workoutService;

    /** 从 Sa-Token 获取当前登录用户的 userId */
    private String currentUserId() {
        return String.valueOf(StpUtil.getLoginIdAsLong());
    }

    /**
     * 开始训练
     * POST /api/gym-workout/start
     */
    @PostMapping("/start")
    public Result<String> startWorkout(@Valid @RequestBody StartWorkoutDTO dto) {
        String recordId = workoutService.startWorkout(currentUserId(), dto.getActionId());
        return Result.success(recordId);
    }

    /**
     * 结束训练
     * PUT /api/gym-workout/{id}/end
     */
    @PutMapping("/{id}/end")
    public Result<Void> endWorkout(@PathVariable String id, @Valid @RequestBody EndWorkoutDTO dto) {
        workoutService.endWorkout(id, dto.getWeight(), dto.getReps(), dto.getSetCount(), dto.getExhaustionScore());
        return Result.success();
    }

    /**
     * 超时修正
     * PUT /api/gym-workout/{id}/correct
     */
    @PutMapping("/{id}/correct")
    public Result<Void> correctTimeout(@PathVariable String id, @Valid @RequestBody CorrectTimeoutDTO dto) {
        workoutService.correctTimeout(id, dto.getActualMinutes(), dto.getExhaustionScore());
        return Result.success();
    }

    /**
     * 获取首页看板
     * GET /api/gym-workout/dashboard
     */
    @GetMapping("/dashboard")
    public Result<DashboardVO> getDashboard() {
        return Result.success(workoutService.getDashboard(currentUserId()));
    }

    /**
     * 自由补打卡
     * POST /api/gym-workout/makeup
     */
    @PostMapping("/makeup")
    public Result<Void> makeupWorkout(@Valid @RequestBody MakeupWorkoutDTO dto) {
        workoutService.makeupWorkout(currentUserId(), dto.getActionId(), dto.getStartTime(),
                dto.getWeight(), dto.getReps(), dto.getSetCount(), dto.getExhaustionScore());
        return Result.success();
    }

    /**
     * 超时检查
     * GET /api/gym-workout/timeout-check
     */
    @GetMapping("/timeout-check")
    public Result<TimeoutRecordVO> checkTimeout() {
        return Result.success(workoutService.checkTimeout(currentUserId()));
    }

    /**
     * 本周训练摘要
     * GET /api/gym-workout/weekly-summary
     */
    /**
     * 获取当前用户半年内打卡日期列表（YYYY-MM-DD，去重）
     * GET /api/gym-workout/checkin-dates
     */
    @GetMapping("/checkin-dates")
    public Result<java.util.List<String>> getCheckinDates() {
        return Result.success(workoutService.getCheckinDates(currentUserId()));
    }

    /**
     * 本周训练摘要
     * GET /api/gym-workout/weekly-summary
     */
    @GetMapping("/weekly-summary")
    public Result<List<WeeklyWorkoutVO>> getWeeklySummary() {
        return Result.success(workoutService.getWeeklySummary(currentUserId()));
    }
}
