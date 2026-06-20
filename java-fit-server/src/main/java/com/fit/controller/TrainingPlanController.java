package com.fit.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit.common.EmpContext;
import com.fit.common.Result;
import com.fit.entity.TrainingPlan;
import com.fit.entity.TrainingPlanDetail;
import com.fit.service.TrainingPlanService;
import com.fit.service.TrainingPlanDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/training-plan")
@RequiredArgsConstructor
public class TrainingPlanController {

    private final TrainingPlanService planService;
    private final TrainingPlanDetailService detailService;

    @GetMapping
    public Result<Page<TrainingPlan>> queryPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String planName,
            @RequestParam(required = false) String muscleGroup) {
        String empNo = EmpContext.getEmpNo();
        return Result.success(planService.queryPage(page, size, empNo, planName, muscleGroup));
    }

    @GetMapping("/all")
    public Result<List<TrainingPlan>> listAll() {
        String empNo = EmpContext.getEmpNo();
        return Result.success(planService.listByEmpNo(empNo));
    }

    @GetMapping("/{id}")
    public Result<TrainingPlan> getById(@PathVariable String id) {
        TrainingPlan plan = planService.getById(id);
        return plan != null ? Result.success(plan) : Result.error("训练计划不存在");
    }

    @GetMapping("/{id}/details")
    public Result<List<TrainingPlanDetail>> getDetails(@PathVariable String id) {
        return Result.success(detailService.listByPlanId(id));
    }

    @PostMapping
    public Result<TrainingPlan> create(@RequestBody TrainingPlan plan) {
        plan.setEmpNo(EmpContext.getEmpNo());
        return Result.success(planService.save(plan));
    }

    @PutMapping("/{id}")
    public Result<TrainingPlan> update(@PathVariable String id, @RequestBody TrainingPlan plan) {
        plan.setId(id);
        return Result.success(planService.update(plan));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        detailService.deleteByPlanId(id);
        planService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/details")
    public Result<TrainingPlanDetail> addDetail(@PathVariable String id, @RequestBody TrainingPlanDetail detail) {
        detail.setPlanId(id);
        return Result.success(detailService.save(detail));
    }

    @PutMapping("/details/{detailId}")
    public Result<TrainingPlanDetail> updateDetail(@PathVariable String detailId, @RequestBody TrainingPlanDetail detail) {
        detail.setId(detailId);
        return Result.success(detailService.update(detail));
    }

    @DeleteMapping("/details/{detailId}")
    public Result<Void> deleteDetail(@PathVariable String detailId) {
        detailService.delete(detailId);
        return Result.success();
    }
}
