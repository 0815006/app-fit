package com.fit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("training_session")
public class TrainingSession {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String empNo;
    private String planId;
    private LocalDate sessionDate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal totalVolume;
    private Integer totalDuration;
    private Integer feeling;
    private String notes;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
