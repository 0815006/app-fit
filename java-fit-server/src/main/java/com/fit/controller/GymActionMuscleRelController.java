package com.fit.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit.common.Result;
import com.fit.entity.GymActionMuscleRel;
import com.fit.service.GymActionMuscleRelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gym-action-muscle-rel")
@RequiredArgsConstructor
public class GymActionMuscleRelController {

    private final GymActionMuscleRelService service;

    @GetMapping
    public Result<Page<GymActionMuscleRel>> queryPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String actionId,
            @RequestParam(required = false) String muscleId) {
        return Result.success(service.queryPage(page, size, actionId, muscleId));
    }

    @GetMapping("/by-action/{actionId}")
    public Result<List<GymActionMuscleRel>> listByActionId(@PathVariable String actionId) {
        return Result.success(service.listByActionId(actionId));
    }

    @GetMapping("/by-muscle-group/{muscleGroup}")
    public Result<List<GymActionMuscleRel>> listByMuscleGroup(@PathVariable String muscleGroup) {
        return Result.success(service.listByMuscleGroup(muscleGroup));
    }

    @PostMapping
    public Result<GymActionMuscleRel> create(@RequestBody GymActionMuscleRel rel) {
        return Result.success(service.save(rel));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        service.delete(id);
        return Result.success();
    }
}
