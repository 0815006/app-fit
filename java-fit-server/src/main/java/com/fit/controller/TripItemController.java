package com.fit.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit.common.Result;
import com.fit.entity.TripItem;
import com.fit.service.TripItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/trip-item")
@RequiredArgsConstructor
public class TripItemController {

    private final TripItemService service;

    /** 分页查询物品 */
    @GetMapping
    public Result<Page<TripItem>> queryPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category) {
        return Result.success(service.queryPage(page, size, name, category));
    }

    /** 获取所有物品(用于下拉选择) */
    @GetMapping("/all")
    public Result<List<TripItem>> listAll() {
        return Result.success(service.listAll());
    }

    /** 根据ID获取物品 */
    @GetMapping("/{id}")
    public Result<TripItem> getById(@PathVariable String id) {
        TripItem item = service.getById(id);
        if (item == null) {
            return Result.error("物品不存在");
        }
        return Result.success(item);
    }

    /** 创建物品 */
    @PostMapping
    public Result<TripItem> create(@RequestBody TripItem item) {
        TripItem saved = service.save(item);
        return Result.success(saved);
    }

    /** 更新物品 */
    @PutMapping("/{id}")
    public Result<TripItem> update(@PathVariable String id, @RequestBody TripItem item) {
        item.setId(id);
        TripItem updated = service.update(item);
        return Result.success(updated);
    }

    /** 删除物品 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        service.delete(id);
        return Result.success();
    }
}
