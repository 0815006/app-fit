package com.fit.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit.common.Result;
import com.fit.entity.GymActionEquipmentRel;
import com.fit.service.GymActionEquipmentRelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gym-action-equipment-rel")
@RequiredArgsConstructor
public class GymActionEquipmentRelController {

    private final GymActionEquipmentRelService service;

    @GetMapping
    public Result<Page<GymActionEquipmentRel>> queryPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String actionId,
            @RequestParam(required = false) String equipmentId) {
        return Result.success(service.queryPage(page, size, actionId, equipmentId));
    }

    @GetMapping("/by-action/{actionId}")
    public Result<List<GymActionEquipmentRel>> listByActionId(@PathVariable String actionId) {
        return Result.success(service.listByActionId(actionId));
    }

    @PostMapping
    public Result<GymActionEquipmentRel> create(@RequestBody GymActionEquipmentRel rel) {
        return Result.success(service.save(rel));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        service.delete(id);
        return Result.success();
    }
}
