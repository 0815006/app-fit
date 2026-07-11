package com.fit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("gym_muscle")
public class GymMuscle {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String muscleCode;
    private String muscleName;
    private String muscleGroup;
    private Integer sortNo;

    /** 基础恢复小时数（大肌群72/48，小肌群24） */
    private Integer baseRecoveryHours;
}
