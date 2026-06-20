package com.fit.controller;

import com.fit.common.Result;
import com.fit.entity.TripTag;
import com.fit.service.TripTagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/trip-tag")
@RequiredArgsConstructor
public class TripTagController {

    private final TripTagService service;

    /** 获取所有标签 */
    @GetMapping("/all")
    public Result<List<TripTag>> listAll() {
        return Result.success(service.listAll());
    }

    /** 根据类型获取标签 */
    @GetMapping("/type/{type}")
    public Result<List<TripTag>> listByType(@PathVariable String type) {
        return Result.success(service.listByType(type));
    }

    /** 根据ID获取标签 */
    @GetMapping("/{id}")
    public Result<TripTag> getById(@PathVariable String id) {
        TripTag tag = service.getById(id);
        if (tag == null) {
            return Result.error("标签不存在");
        }
        return Result.success(tag);
    }

    /** 创建标签 */
    @PostMapping
    public Result<TripTag> create(@RequestBody TripTag tag) {
        TripTag saved = service.save(tag);
        return Result.success(saved);
    }

    /** 更新标签 */
    @PutMapping("/{id}")
    public Result<TripTag> update(@PathVariable String id, @RequestBody TripTag tag) {
        tag.setId(id);
        TripTag updated = service.update(tag);
        return Result.success(updated);
    }

    /** 删除标签 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        service.delete(id);
        return Result.success();
    }
}
