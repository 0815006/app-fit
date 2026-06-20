package com.fit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("body_metric")
public class BodyMetric {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String empNo;
    private LocalDate metricDate;
    private BigDecimal weight;
    private BigDecimal bodyFat;
    private BigDecimal bmi;
    private BigDecimal muscleMass;
    private String notes;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
