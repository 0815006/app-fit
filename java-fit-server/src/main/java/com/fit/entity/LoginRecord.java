package com.fit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("login_record")
public class LoginRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 员工工号 (7 digits)
     */
    private String empNo;

    /**
     * 登录类型: WEB / MINI_PROGRAM
     */
    private String loginType;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
