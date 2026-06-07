package com.fit.service.impl;

import com.fit.entity.CanteenMenuRecord;
import com.fit.mapper.CanteenMenuRecordMapper;
import com.fit.service.CanteenMenuRecordService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CanteenMenuRecordServiceImpl implements CanteenMenuRecordService {

    private final CanteenMenuRecordMapper mapper;

    private static final String[] WEEKDAYS = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    /**
     * Date formats that may appear in Excel cells.
     * Some cells are stored as strings "2026.06.08", others as numeric dates that POI
     * converts to ISO "2026-06-08".
     */
    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/M/d"),
    };

    /** Set of meal types that Phase-2 canteen supports (no dinner / supper). */
    private static final Set<String> PHASE2_MEAL_TYPES = Set.of("早餐", "午餐");

    @Override
    public String uploadExcel(MultipartFile file) {
        String batchNo = UUID.randomUUID().toString().replace("-", "");
        List<CanteenMenuRecord> allRecords = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            for (int si = 0; si < workbook.getNumberOfSheets(); si++) {
                Sheet sheet = workbook.getSheetAt(si);
                String sheetName = sheet.getSheetName().trim();
                String canteenZone = detectZone(sheetName);
                if (canteenZone == null) {
                    log.warn("Skipping unrecognized sheet: {}", sheetName);
                    continue;
                }

                int step = "一期".equals(canteenZone) ? 3 : 4;
                List<CanteenMenuRecord> sheetRecords = parseSheet(sheet, canteenZone, step, batchNo);
                allRecords.addAll(sheetRecords);
                log.info("Sheet [{}] zone={} → {} records", sheetName, canteenZone, sheetRecords.size());
            }
        } catch (Exception e) {
            log.error("Excel parse error", e);
            throw new RuntimeException("Excel 解析失败: " + e.getMessage(), e);
        }

        // Batch insert
        for (CanteenMenuRecord record : allRecords) {
            record.setImportBatchNo(batchNo);
            mapper.insert(record);
        }

        log.info("Imported {} canteen menu records, batchNo={}", allRecords.size(), batchNo);
        return batchNo;
    }

    @Override
    public Page<CanteenMenuRecord> queryRecords(int page, int size, String canteenZone, LocalDate menuDate, String mealType) {
        LambdaQueryWrapper<CanteenMenuRecord> qw = new LambdaQueryWrapper<>();
        if (isNotBlank(canteenZone)) {
            qw.eq(CanteenMenuRecord::getCanteenZone, canteenZone);
        }
        if (menuDate != null) {
            qw.eq(CanteenMenuRecord::getMenuDate, menuDate);
        }
        if (isNotBlank(mealType)) {
            qw.eq(CanteenMenuRecord::getMealType, mealType);
        }
        qw.orderByAsc(CanteenMenuRecord::getMenuDate)
          .orderByAsc(CanteenMenuRecord::getCanteenZone)
          .orderByAsc(CanteenMenuRecord::getMealType)
          .orderByAsc(CanteenMenuRecord::getCategoryName);
        return mapper.selectPage(new Page<>(page, size), qw);
    }

    @Override
    public List<String> getBatches() {
        LambdaQueryWrapper<CanteenMenuRecord> qw = new LambdaQueryWrapper<>();
        qw.select(CanteenMenuRecord::getImportBatchNo, CanteenMenuRecord::getCreateTime)
          .groupBy(CanteenMenuRecord::getImportBatchNo, CanteenMenuRecord::getCreateTime)
          .orderByDesc(CanteenMenuRecord::getCreateTime);
        return mapper.selectList(qw).stream()
                .map(CanteenMenuRecord::getImportBatchNo)
                .distinct()
                .toList();
    }

    @Override
    public void deleteByBatch(String batchNo) {
        LambdaQueryWrapper<CanteenMenuRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(CanteenMenuRecord::getImportBatchNo, batchNo);
        mapper.delete(qw);
        log.info("Deleted batch: {}", batchNo);
    }

    // ──────────────── private helpers ────────────────

    private static boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String detectZone(String sheetName) {
        if (sheetName.contains("一期") || sheetName.contains("1期")) return "一期";
        if (sheetName.contains("二期") || sheetName.contains("2期")) return "二期";
        // Also try detecting from the first row of the sheet (fallback)
        return null;
    }

    private List<CanteenMenuRecord> parseSheet(Sheet sheet, String canteenZone, int step, String batchNo) {
        List<CanteenMenuRecord> records = new ArrayList<>();
        int totalRows = sheet.getLastRowNum();

        String currentMealType = null;
        List<LocalDate> currentDates = null;
        boolean isPhase2 = "二期".equals(canteenZone);

        for (int r = 0; r <= totalRows; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            String firstCell = getCellString(sheet, row, 0);

            // Detect meal section header — strip spaces because Excel cells
            // often have spaces between characters ("一 期 每 周 早 餐 菜 单")
            String compact = firstCell.replaceAll("\\s+", "");
            if (compact.contains("餐") && compact.contains("单") && compact.contains("周")) {
                String mealType = extractMealType(compact);

                // Phase 2 only has breakfast & lunch; skip dinner / supper
                if (isPhase2 && !PHASE2_MEAL_TYPES.contains(mealType)) {
                    log.debug("Skipping {} meal section for 二期 at row {}", mealType, r);
                    currentMealType = null;
                    currentDates = null;
                    continue;
                }

                currentMealType = mealType;

                // Date row is immediately after the meal header row (r + 1)
                if (r + 1 <= totalRows) {
                    Row dateRow = sheet.getRow(r + 1);
                    if (dateRow != null) {
                        currentDates = parseDateRow(sheet, dateRow, step);
                        log.debug("Meal [{}] at row {} → {} dates: {}", mealType, r, currentDates.size(), currentDates);
                    }
                }

                // Skip header row + date row + sub-header row (3 rows total)
                // r += 2 sets r to header+2 (sub-header), then for-loop ++ makes it header+3 (first data row)
                r += 2;
                continue;
            }

            // Skip if we're not in a valid meal section
            if (currentMealType == null || currentDates == null || currentDates.isEmpty()) continue;

            // Skip completely empty rows
            if (isBlank(firstCell) && isRowEmpty(row, currentDates.size() * step + 1)) continue;

            // Category name from col 0 (may be merged, handled by getCellString)
            String categoryName = cleanCategory(firstCell);

            // Parse each day's data
            for (int d = 0; d < currentDates.size(); d++) {
                int col = 1 + d * step;

                String dishRaw = getCellString(sheet, row, col);
                if (isBlank(dishRaw)) continue;
                // Skip sub-header cells and anything that looks like a date
                if (isHeaderOrDate(dishRaw)) continue;

                CanteenMenuRecord record = new CanteenMenuRecord();
                record.setId(UUID.randomUUID().toString().replace("-", ""));
                record.setCanteenZone(canteenZone);
                record.setMenuDate(currentDates.get(d));
                record.setWeekDay(WEEKDAYS[currentDates.get(d).getDayOfWeek().getValue() - 1]);
                record.setMealType(currentMealType);
                record.setCategoryName(categoryName);
                record.setImportBatchNo(batchNo);

                // Clean dish name and detect spicy
                if (dishRaw.contains("/辣")) {
                    record.setDishName(dishRaw.replace("/辣", "").trim());
                    record.setIsSpicy(1);
                } else {
                    record.setDishName(dishRaw.trim());
                    record.setIsSpicy(0);
                }

                if ("一期".equals(canteenZone)) {
                    record.setUnit("份");
                    record.setPrice(parsePrice(getCellString(sheet, row, col + 1)));
                    record.setEnergyKcal(parseEnergy(getCellString(sheet, row, col + 2)));
                } else {
                    // 二期: col+1 = unit, col+2 = price, col+3 = energy
                    String unit = getCellString(sheet, row, col + 1);
                    record.setUnit(isBlank(unit) ? "份" : unit.trim());
                    record.setPrice(parsePrice(getCellString(sheet, row, col + 2)));
                    record.setEnergyKcal(parseEnergy(getCellString(sheet, row, col + 3)));
                }

                records.add(record);
            }
        }

        return records;
    }

    /**
     * Extract meal type from header text like "一 期 每 周 早 餐 菜 单" or "每 周 午 餐 菜 单"
     */
    private String extractMealType(String header) {
        if (header.contains("早餐")) return "早餐";
        if (header.contains("午餐")) return "午餐";
        if (header.contains("晚餐")) return "晚餐";
        if (header.contains("夜宵")) return "夜宵";
        return "未知";
    }

    /**
     * Parse date row to extract all dates.
     * Each date is at column 1 + d * step.
     * Supports multiple date formats because Excel may store dates as strings
     * ("2026.06.08") or as numeric date cells (POI → "2026-06-08").
     */
    private List<LocalDate> parseDateRow(Sheet sheet, Row dateRow, int step) {
        List<LocalDate> dates = new ArrayList<>();
        short maxCol = dateRow.getLastCellNum();
        if (maxCol < 0) maxCol = 50;

        for (int c = 1; c < maxCol; c += step) {
            String raw = getCellString(sheet, dateRow, c);
            if (isBlank(raw)) break;

            LocalDate parsed = tryParseDate(raw.trim());
            if (parsed != null) {
                dates.add(parsed);
            } else {
                // Not a valid date in any format — stop, we've reached the end of dates
                break;
            }
        }
        return dates;
    }

    /**
     * Try to parse a date string with all known formats.
     */
    private LocalDate tryParseDate(String raw) {
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(raw, fmt);
            } catch (Exception ignored) {
                // try next format
            }
        }
        return null;
    }

    /**
     * Returns true if the cell value is a sub-header label or looks like a date
     * (prevents date values from leaking into dish_name).
     */
    private boolean isHeaderOrDate(String value) {
        if ("品名".equals(value) || "品种".equals(value)) return true;
        // Skip anything that parses as a date
        if (tryParseDate(value) != null) return true;
        // Also skip sub-header entries that might appear in merged date columns
        if (value.startsWith("售价") || value.startsWith("价格") || value.contains("能量")) return true;
        return false;
    }

    /**
     * Get the string value of a cell, handling merged cells.
     */
    private String getCellString(Sheet sheet, Row row, int colIndex) {
        if (row == null) return "";
        Cell cell = row.getCell(colIndex);
        if (cell != null) {
            String val = cellToString(cell);
            if (isNotBlank(val)) return val;
        }
        // Check merged regions
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress region = sheet.getMergedRegion(i);
            if (region.isInRange(row.getRowNum(), colIndex)) {
                Row firstRow = sheet.getRow(region.getFirstRow());
                if (firstRow != null) {
                    Cell firstCell = firstRow.getCell(region.getFirstColumn());
                    if (firstCell != null) {
                        return cellToString(firstCell);
                    }
                }
            }
        }
        return "";
    }

    private String cellToString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double v = cell.getNumericCellValue();
                if (v == Math.floor(v) && !Double.isInfinite(v)) {
                    yield String.valueOf((long) v);
                }
                yield String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private String cleanCategory(String raw) {
        if (isBlank(raw)) return "";
        return raw.replaceAll("<br>", "").replaceAll("\n", "").trim();
    }

    private BigDecimal parsePrice(String raw) {
        if (isBlank(raw)) return BigDecimal.ZERO;
        if ("免费".equals(raw.trim())) return BigDecimal.ZERO;
        try {
            String clean = raw.replaceAll("[￥,元, ]", "").trim();
            return new BigDecimal(clean);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private Integer parseEnergy(String raw) {
        if (isBlank(raw)) return 0;
        try {
            String clean = raw.replaceAll("[≈, ,kcal,Kcal,KCAL]", "");
            if (clean.contains("/")) {
                clean = clean.split("/")[0];
            }
            if (isBlank(clean)) return 0;
            return Integer.parseInt(clean.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean isRowEmpty(Row row, int maxCol) {
        for (int c = 0; c < maxCol; c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }
}
