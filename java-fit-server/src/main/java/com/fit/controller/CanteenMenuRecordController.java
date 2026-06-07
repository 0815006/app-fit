package com.fit.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit.common.Result;
import com.fit.entity.CanteenMenuRecord;
import com.fit.service.CanteenMenuRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/canteen-menu")
@RequiredArgsConstructor
public class CanteenMenuRecordController {

    private final CanteenMenuRecordService service;

    /**
     * Upload an Excel file, parse and import canteen menu records.
     */
    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件为空");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || (!originalName.endsWith(".xlsx") && !originalName.endsWith(".xls"))) {
            return Result.error("仅支持 .xlsx 或 .xls 格式的 Excel 文件");
        }
        try {
            String batchNo = service.uploadExcel(file);
            return Result.success(Map.of("batchNo", batchNo));
        } catch (Exception e) {
            log.error("Upload failed", e);
            return Result.error("导入失败: " + e.getMessage());
        }
    }

    /**
     * Query menu records with optional filters.
     */
    @GetMapping("/records")
    public Result<Page<CanteenMenuRecord>> queryRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String canteenZone,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate menuDate,
            @RequestParam(required = false) String mealType) {
        Page<CanteenMenuRecord> result = service.queryRecords(page, size, canteenZone, menuDate, mealType);
        return Result.success(result);
    }

    /**
     * Get all distinct batch numbers.
     */
    @GetMapping("/batches")
    public Result<List<String>> getBatches() {
        return Result.success(service.getBatches());
    }

    /**
     * Delete a batch and all its records.
     */
    @DeleteMapping("/batches/{batchNo}")
    public Result<Void> deleteBatch(@PathVariable String batchNo) {
        service.deleteByBatch(batchNo);
        return Result.success();
    }
}
