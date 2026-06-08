package com.fit.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit.common.Result;
import com.fit.entity.GymMuscle;
import com.fit.service.GymMuscleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gym-muscle")
@RequiredArgsConstructor
public class GymMuscleController {

    private final GymMuscleService service;

    @GetMapping
    public Result<Page<GymMuscle>> queryPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String muscleName,
            @RequestParam(required = false) String muscleGroup) {
        return Result.success(service.queryPage(page, size, muscleName, muscleGroup));
    }

    @GetMapping("/all")
    public Result<List<GymMuscle>> listAll() {
        return Result.success(service.listAll());
    }

    @GetMapping("/{id}")
    public Result<GymMuscle> getById(@PathVariable String id) {
        GymMuscle m = service.getById(id);
        return m != null ? Result.success(m) : Result.error("肌群不存在");
    }

    @PostMapping
    public Result<GymMuscle> create(@RequestBody GymMuscle muscle) {
        return Result.success(service.save(muscle));
    }

    @PutMapping("/{id}")
    public Result<GymMuscle> update(@PathVariable String id, @RequestBody GymMuscle muscle) {
        muscle.setId(id);
        return Result.success(service.update(muscle));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        service.delete(id);
        return Result.success();
    }
}
