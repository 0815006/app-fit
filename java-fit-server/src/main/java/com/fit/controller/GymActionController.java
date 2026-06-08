package com.fit.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit.common.Result;
import com.fit.entity.GymAction;
import com.fit.service.GymActionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gym-action")
@RequiredArgsConstructor
public class GymActionController {

    private final GymActionService service;

    @GetMapping
    public Result<Page<GymAction>> queryPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String movementPattern,
            @RequestParam(required = false) Integer difficultyLevel) {
        return Result.success(service.queryPage(page, size, name, actionType, movementPattern, difficultyLevel));
    }

    @GetMapping("/all")
    public Result<List<GymAction>> listAll() {
        return Result.success(service.listAll());
    }

    @GetMapping("/{id}")
    public Result<GymAction> getById(@PathVariable String id) {
        GymAction a = service.getById(id);
        return a != null ? Result.success(a) : Result.error("动作不存在");
    }

    @PostMapping
    public Result<GymAction> create(@RequestBody GymAction action) {
        return Result.success(service.save(action));
    }

    @PutMapping("/{id}")
    public Result<GymAction> update(@PathVariable String id, @RequestBody GymAction action) {
        action.setId(id);
        return Result.success(service.update(action));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        service.delete(id);
        return Result.success();
    }
}
