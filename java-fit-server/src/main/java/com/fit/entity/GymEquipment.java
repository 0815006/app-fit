package com.fit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("gym_equipment")
public class GymEquipment {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 器械编码（如：BARBELL） */
    private String equipmentCode;

    /** 器械名称（如：杠铃） */
    private String equipmentName;

    /** 器械大类类型 */
    private String equipmentType;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
