package com.fit.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit.common.Result;
import com.fit.entity.GymActionRecommendation;
import com.fit.service.GymActionRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gym-action-recommendation")
@RequiredArgsConstructor
public class GymActionRecommendationController {

    private final GymActionRecommendationService service;

    @GetMapping
    public Result<Page<GymActionRecommendation>> queryPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String actionId,
            @RequestParam(required = false) String trainingGoal) {
        return Result.success(service.queryPage(page, size, actionId, trainingGoal));
    }

    @GetMapping("/by-action/{actionId}")
    public Result<List<GymActionRecommendation>> listByActionId(@PathVariable String actionId) {
        return Result.success(service.listByActionId(actionId));
    }

    @GetMapping("/{id}")
    public Result<GymActionRecommendation> getById(@PathVariable String id) {
        GymActionRecommendation rec = service.getById(id);
        return rec != null ? Result.success(rec) : Result.error("推荐记录不存在");
    }

    @PostMapping
    public Result<GymActionRecommendation> create(@RequestBody GymActionRecommendation rec) {
        return Result.success(service.save(rec));
    }

    @PutMapping("/{id}")
    public Result<GymActionRecommendation> update(@PathVariable String id, @RequestBody GymActionRecommendation rec) {
        rec.setId(id);
        return Result.success(service.update(rec));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        service.delete(id);
        return Result.success();
    }
}
