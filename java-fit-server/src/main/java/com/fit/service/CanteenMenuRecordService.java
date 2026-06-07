package com.fit.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit.entity.CanteenMenuRecord;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface CanteenMenuRecordService {

    /**
     * Upload and parse an Excel file, save all records to database.
     *
     * @param file the uploaded Excel file
     * @return the import batch number
     */
    String uploadExcel(MultipartFile file);

    /**
     * Query menu records with optional filters, paginated.
     *
     * @param page        page number
     * @param size        page size
     * @param canteenZone canteen zone filter (optional)
     * @param menuDate    menu date filter (optional)
     * @param mealType    meal type filter (optional)
     * @return paginated result
     */
    Page<CanteenMenuRecord> queryRecords(int page, int size, String canteenZone, LocalDate menuDate, String mealType);

    /**
     * Get all distinct import batch numbers.
     */
    List<String> getBatches();

    /**
     * Delete all records of a given batch.
     */
    void deleteByBatch(String batchNo);
}
