package com.fit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("training_plan_detail")
public class TrainingPlanDetail {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String planId;
    private String actionId;
    private Integer sortNo;
    private Integer targetSets;
    private Integer targetReps;
    private BigDecimal targetWeight;
    private Integer restSeconds;
    private String notes;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
