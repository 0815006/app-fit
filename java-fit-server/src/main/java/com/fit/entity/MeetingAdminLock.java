package com.fit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("meeting_admin_lock")
public class MeetingAdminLock {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 关联会议室ID */
    private String roomId;

    /** 征用日期 */
    private LocalDate lockDate;

    /** 起始时段编号 */
    private Integer startSlot;

    /** 结束时段编号（不包含） */
    private Integer endSlot;

    /** 征用原因 */
    private String reason;

    /** 发起部门 */
    private String deptName;

    /** 操作行政人员工号 */
    private String operatorEmpNo;

    /** 创建时间 */
    private LocalDateTime createTime;
}
