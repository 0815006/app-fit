package com.fit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("canteen_menu_record")
public class CanteenMenuRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 食堂区域：一期、二期 */
    private String canteenZone;

    /** 具体日期 */
    private LocalDate menuDate;

    /** 星期几 */
    private String weekDay;

    /** 餐次类型：早餐、午餐、晚餐、夜宵 */
    private String mealType;

    /** 菜品细分类别 */
    private String categoryName;

    /** 菜品名称 */
    private String dishName;

    /** 单位（份、碗、只等） */
    private String unit;

    /** 价格 */
    private BigDecimal price;

    /** 能量(kcal) */
    private Integer energyKcal;

    /** 是否辣：0-否，1-是 */
    private Integer isSpicy;

    /** 导入批次号 */
    private String importBatchNo;

    /** 创建时间 */
    private LocalDateTime createTime;
}
