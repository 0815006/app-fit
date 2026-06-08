package com.fit.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit.common.Result;
import com.fit.entity.GymEquipment;
import com.fit.service.GymEquipmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/gym-equipment")
@RequiredArgsConstructor
public class GymEquipmentController {

    private final GymEquipmentService service;

    /** Paginated query with optional filters */
    @GetMapping
    public Result<Page<GymEquipment>> queryPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String equipmentName,
            @RequestParam(required = false) String equipmentType) {
        return Result.success(service.queryPage(page, size, equipmentName, equipmentType));
    }

    /** List all (for dropdowns) */
    @GetMapping("/all")
    public Result<List<GymEquipment>> listAll() {
        return Result.success(service.listAll());
    }

    /** Get by id */
    @GetMapping("/{id}")
    public Result<GymEquipment> getById(@PathVariable String id) {
        GymEquipment eq = service.getById(id);
        if (eq == null) {
            return Result.error("器械不存在");
        }
        return Result.success(eq);
    }

    /** Create */
    @PostMapping
    public Result<GymEquipment> create(@RequestBody GymEquipment equipment) {
        GymEquipment saved = service.save(equipment);
        return Result.success(saved);
    }

    /** Update */
    @PutMapping("/{id}")
    public Result<GymEquipment> update(@PathVariable String id, @RequestBody GymEquipment equipment) {
        equipment.setId(id);
        GymEquipment updated = service.update(equipment);
        return Result.success(updated);
    }

    /** Delete */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        service.delete(id);
        return Result.success();
    }
}
