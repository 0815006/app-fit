package com.fit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("training_plan")
public class TrainingPlan {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 所属用户ID（关联user表主键） */
    private String userId;
    private String planName;
    private String description;
    private String muscleGroup;
    private Integer sortNo;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
