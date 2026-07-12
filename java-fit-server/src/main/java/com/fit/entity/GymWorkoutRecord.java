package com.fit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("gym_workout_record")
public class GymWorkoutRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 用户ID（关联 user.id） */
    private String userId;

    /** 动作ID（关联 gym_action.id） */
    private String actionId;

    /** 冗余：肌群大类编码（CHEST/BACK/SHOULDER/ARM/LEG/GLUTE/CORE 等） */
    private String muscleGroup;

    /** 训练开始时间 */
    private LocalDateTime startTime;

    /** 训练结束时间（结束时/修正时写入） */
    private LocalDateTime endTime;

    /** 力竭度系数（0.50 ~ 1.20） */
    private BigDecimal exhaustionScore;

    /** 状态：0=训练中, 1=正常结束, 2=超时修正结束 */
    private Integer status;

    /** 本次动作使用重量(kg)，NULL表示无负重或不适用 */
    private BigDecimal weight;

    /** 本次动作训练次数，NULL表示未填写或不适用 */
    private Integer reps;

    /** 本次训练组数，NULL表示未填写或不适用 */
    private Integer setCount;

    /** 自动计算的1RM估值(kg)，仅当weight+reps均非NULL时计算 */
    private BigDecimal rmEstimate;

    /** 是否破个人纪录：0=否, 1=是 */
    private Integer isPr;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
