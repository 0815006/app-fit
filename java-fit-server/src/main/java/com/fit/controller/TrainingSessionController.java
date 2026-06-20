package com.fit.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit.common.EmpContext;
import com.fit.common.Result;
import com.fit.entity.TrainingSession;
import com.fit.entity.TrainingSessionDetail;
import com.fit.service.TrainingSessionService;
import com.fit.service.TrainingSessionDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/training-session")
@RequiredArgsConstructor
public class TrainingSessionController {

    private final TrainingSessionService sessionService;
    private final TrainingSessionDetailService detailService;

    @GetMapping
    public Result<Page<TrainingSession>> queryPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        String empNo = EmpContext.getEmpNo();
        return Result.success(sessionService.queryPage(page, size, empNo, startDate, endDate));
    }

    @GetMapping("/{id}")
    public Result<TrainingSession> getById(@PathVariable String id) {
        TrainingSession session = sessionService.getById(id);
        return session != null ? Result.success(session) : Result.error("训练记录不存在");
    }

    @GetMapping("/{id}/details")
    public Result<List<TrainingSessionDetail>> getDetails(@PathVariable String id) {
        return Result.success(detailService.listBySessionId(id));
    }

    @PostMapping
    public Result<TrainingSession> create(@RequestBody TrainingSession session) {
        session.setEmpNo(EmpContext.getEmpNo());
        return Result.success(sessionService.save(session));
    }

    @PutMapping("/{id}")
    public Result<TrainingSession> update(@PathVariable String id, @RequestBody TrainingSession session) {
        session.setId(id);
        return Result.success(sessionService.update(session));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        detailService.deleteBySessionId(id);
        sessionService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/details")
    public Result<TrainingSessionDetail> addDetail(@PathVariable String id, @RequestBody TrainingSessionDetail detail) {
        detail.setSessionId(id);
        return Result.success(detailService.save(detail));
    }

    @PutMapping("/details/{detailId}")
    public Result<TrainingSessionDetail> updateDetail(@PathVariable String detailId, @RequestBody TrainingSessionDetail detail) {
        detail.setId(detailId);
        return Result.success(detailService.update(detail));
    }

    @DeleteMapping("/details/{detailId}")
    public Result<Void> deleteDetail(@PathVariable String detailId) {
        detailService.delete(detailId);
        return Result.success();
    }
}
