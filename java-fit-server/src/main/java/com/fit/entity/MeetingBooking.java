package com.fit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("meeting_booking")
public class MeetingBooking {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 关联会议室ID */
    private String roomId;

    /** 预定日期 */
    private LocalDate bookingDate;

    /** 起始时段编号（0=08:00, 1=08:30, ...） */
    private Integer startSlot;

    /** 结束时段编号（不包含） */
    private Integer endSlot;

    /** 预定人工号 */
    private String empNo;

    /** 预定人姓名（冗余） */
    private String empName;

    /** 会议名称（选填） */
    private String meetingTitle;

    /** 参会人（选填） */
    private String attendees;

    /** 是否固定周期约：0-否，1-是 */
    private Integer isWeeklyFix;

    /** 周期约组ID（同一批生成共享） */
    private String groupId;

    /** 状态：1-正常，0-已取消 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
