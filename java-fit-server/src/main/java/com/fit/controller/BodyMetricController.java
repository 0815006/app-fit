package com.fit.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit.common.EmpContext;
import com.fit.common.Result;
import com.fit.entity.BodyMetric;
import com.fit.service.BodyMetricService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/body-metric")
@RequiredArgsConstructor
public class BodyMetricController {

    private final BodyMetricService service;

    @GetMapping
    public Result<Page<BodyMetric>> queryPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        String empNo = EmpContext.getEmpNo();
        return Result.success(service.queryPage(page, size, empNo, startDate, endDate));
    }

    @GetMapping("/list")
    public Result<List<BodyMetric>> listByDateRange(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        String empNo = EmpContext.getEmpNo();
        return Result.success(service.listByEmpNo(empNo, startDate, endDate));
    }

    @GetMapping("/{id}")
    public Result<BodyMetric> getById(@PathVariable String id) {
        BodyMetric metric = service.getById(id);
        return metric != null ? Result.success(metric) : Result.error("记录不存在");
    }

    @PostMapping
    public Result<BodyMetric> create(@RequestBody BodyMetric metric) {
        metric.setEmpNo(EmpContext.getEmpNo());
        return Result.success(service.save(metric));
    }

    @PutMapping("/{id}")
    public Result<BodyMetric> update(@PathVariable String id, @RequestBody BodyMetric metric) {
        metric.setId(id);
        return Result.success(service.update(metric));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        service.delete(id);
        return Result.success();
    }
}
