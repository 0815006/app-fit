package com.fit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("training_session_detail")
public class TrainingSessionDetail {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String sessionId;
    private String planDetailId;
    private String actionId;
    private Integer sortNo;
    private Integer setNo;
    private BigDecimal targetWeight;
    private Integer targetReps;
    private BigDecimal actualWeight;
    private Integer actualReps;
    private Integer isCompleted;
    private Integer isPr;
    private String notes;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
