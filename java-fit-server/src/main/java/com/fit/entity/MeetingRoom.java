package com.fit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("meeting_room")
public class MeetingRoom {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 会议室名称（如"会议室 A (301)"） */
    private String name;

    /** 具体楼层位置 */
    private String location;

    /** 座位数 */
    private Integer capacity;

    /** 配套硬件标签 JSON 数组 */
    private String facilities;

    /** 实景照片 URL */
    private String photoUrl;

    /** 是否上架：1-上架，0-下架 */
    private Integer isActive;

    /** 排序权重 */
    private Integer sortOrder;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
