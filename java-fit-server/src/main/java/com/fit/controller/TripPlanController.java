package com.fit.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit.common.EmpContext;
import com.fit.common.Result;
import com.fit.entity.TripPlan;
import com.fit.entity.TripPlanDetail;
import com.fit.entity.TripPlanSource;
import com.fit.entity.TripTag;
import com.fit.entity.TripTagItem;
import com.fit.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/trip-plan")
@RequiredArgsConstructor
public class TripPlanController {

    private final TripPlanService planService;
    private final TripPlanDetailService planDetailService;
    private final TripPlanSourceService planSourceService;
    private final TripTagItemService tagItemService;
    private final TripItemService tripItemService;
    private final TripTagService tripTagService;

    /** 分页查询计划 */
    @GetMapping
    public Result<Page<TripPlan>> queryPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String status) {
        String userId = EmpContext.getEmpNo();
        return Result.success(planService.queryPage(page, size, userId, status));
    }

    /** 获取用户所有计划 */
    @GetMapping("/my")
    public Result<List<TripPlan>> listMyPlans() {
        String userId = EmpContext.getEmpNo();
        return Result.success(planService.listByUserId(userId));
    }

    /** 根据ID获取计划 */
    @GetMapping("/{id}")
    public Result<TripPlan> getById(@PathVariable String id) {
        TripPlan plan = planService.getById(id);
        if (plan == null) {
            return Result.error("计划不存在");
        }
        return Result.success(plan);
    }

    /** 创建计划 */
    @PostMapping
    public Result<TripPlan> create(@RequestBody TripPlan plan) {
        plan.setUserId(EmpContext.getEmpNo());
        if (plan.getStatus() == null) {
            plan.setStatus("DRAFT");
        }
        TripPlan saved = planService.save(plan);
        return Result.success(saved);
    }

    /** 更新计划 */
    @PutMapping("/{id}")
    public Result<TripPlan> update(@PathVariable String id, @RequestBody TripPlan plan) {
        plan.setId(id);
        TripPlan updated = planService.update(plan);
        return Result.success(updated);
    }

    /** 删除计划 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        // 先删除明细和来源
        planDetailService.deleteByPlanId(id);
        planService.delete(id);
        return Result.success();
    }

    /** 更新计划状态 */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable String id, @RequestParam String status) {
        planService.updateStatus(id, status);
        return Result.success();
    }

    /** 根据标签生成计划明细 */
    @PostMapping("/{id}/generate")
    public Result<List<TripPlanDetail>> generateDetails(
            @PathVariable String id,
            @RequestBody List<String> tagIds) {
        TripPlan plan = planService.getById(id);
        if (plan == null) {
            return Result.error("计划不存在");
        }

        // 获取标签关联的物品
        List<TripTagItem> tagItems = tagItemService.listByTagIds(tagIds);
        if (tagItems.isEmpty()) {
            return Result.success(List.of());
        }

        // 预加载所有标签名称 (用于 sourceContextsJson)
        List<TripTag> allTags = tripTagService.listAll();
        Map<String, String> tagNameMap = new HashMap<>();
        for (TripTag tag : allTags) {
            tagNameMap.put(tag.getId(), tag.getName());
        }

        // 按物品ID去重
        Map<String, List<TripTagItem>> groupedByItem = tagItems.stream()
                .collect(Collectors.groupingBy(TripTagItem::getItemId));

        List<TripPlanDetail> details = new ArrayList<>();

        for (Map.Entry<String, List<TripTagItem>> entry : groupedByItem.entrySet()) {
            String itemId = entry.getKey();
            List<TripTagItem> items = entry.getValue();
            TripTagItem firstItem = items.get(0);

            // 获取物品信息
            var tripItem = tripItemService.getById(itemId);
            if (tripItem == null) continue;

            // 计算数量
            int quantity = calculateQuantity(firstItem.getMultiplierExpr(), plan.getTripDays());

            // 构建血缘快照JSON
            List<Map<String, String>> sourceContexts = new ArrayList<>();
            for (TripTagItem tagItem : items) {
                Map<String, String> ctx = new HashMap<>();
                ctx.put("tagId", tagItem.getTagId());
                ctx.put("tagName", tagNameMap.getOrDefault(tagItem.getTagId(), "未知标签"));
                sourceContexts.add(ctx);
            }
            String sourceJson;
            try {
                sourceJson = new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsString(sourceContexts);
            } catch (Exception e) {
                sourceJson = "[]";
            }

            // 创建明细
            TripPlanDetail detail = new TripPlanDetail();
            detail.setPlanId(id);
            detail.setItemId(itemId);
            detail.setItemName(tripItem.getName());
            detail.setContainer(tripItem.getDefaultContainer());
            detail.setImportanceLevel(tripItem.getImportanceLevel());
            detail.setTargetQuantity(quantity);
            detail.setIsChecked(0);
            detail.setExcludeFlag(0);
            detail.setSourceContextsJson(sourceJson);
            detail.setVersionNo(1L);

            planDetailService.save(detail);
            details.add(detail);

            // 创建来源记录
            for (TripTagItem tagItem : items) {
                TripPlanSource source = new TripPlanSource();
                source.setPlanDetailId(detail.getId());
                source.setTagId(tagItem.getTagId());
                planSourceService.save(source);
            }
        }

        return Result.success(details);
    }

    /** 获取计划明细列表 */
    @GetMapping("/{id}/details")
    public Result<List<TripPlanDetail>> getDetails(@PathVariable String id) {
        return Result.success(planDetailService.listByPlanId(id));
    }

    /** 更新明细装箱状态 */
    @PutMapping("/detail/{id}/check")
    public Result<Void> updateChecked(@PathVariable String id, @RequestParam Integer isChecked) {
        planDetailService.updateChecked(id, isChecked);
        return Result.success();
    }

    /** 更新明细排除标志 */
    @PutMapping("/detail/{id}/exclude")
    public Result<Void> updateExcludeFlag(@PathVariable String id, @RequestParam Integer excludeFlag) {
        planDetailService.updateExcludeFlag(id, excludeFlag);
        return Result.success();
    }

    /** 更新明细数量 */
    @PutMapping("/detail/{id}/quantity")
    public Result<Void> updateQuantity(@PathVariable String id, @RequestParam Integer targetQuantity) {
        planDetailService.updateQuantity(id, targetQuantity);
        return Result.success();
    }

    /** 手动添加物品到计划 */
    @PostMapping("/{id}/detail")
    public Result<TripPlanDetail> addDetail(@PathVariable String id, @RequestBody TripPlanDetail detail) {
        detail.setPlanId(id);
        TripPlanDetail saved = planDetailService.save(detail);
        return Result.success(saved);
    }

    /** 计算数量 */
    private int calculateQuantity(String expr, int days) {
        if (expr == null || expr.isBlank() || "FIXED".equals(expr)) {
            return 1;
        }
        if ("DAY*1".equals(expr)) {
            return days;
        }
        if ("DAY/2".equals(expr)) {
            return (int) Math.ceil(days / 2.0);
        }
        if ("LIMIT_MAX_4".equals(expr)) {
            return Math.min(days, 4);
        }
        return 1;
    }
}
