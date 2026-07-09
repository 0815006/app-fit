-- Flyway migration V15: Create meeting_booking table
-- 会议室预定记录表

CREATE TABLE meeting_booking (
    id              VARCHAR(32)    NOT NULL COMMENT '主键雪花ID',
    room_id         VARCHAR(32)    NOT NULL COMMENT '关联会议室ID',
    booking_date    DATE           NOT NULL COMMENT '预定日期',
    start_slot      INT            NOT NULL COMMENT '起始时段编号（0=08:00,19=17:30，共20个半小时槽）',
    end_slot        INT            NOT NULL COMMENT '结束时段编号（不包含，即[start_slot, end_slot)）',
    emp_no          VARCHAR(7)     NOT NULL COMMENT '预定人工号',
    emp_name        VARCHAR(50)    NOT NULL DEFAULT '' COMMENT '预定人姓名（冗余）',
    meeting_title   VARCHAR(200)   DEFAULT '' COMMENT '会议名称（选填）',
    attendees       VARCHAR(500)   DEFAULT '' COMMENT '参会人（选填）',
    is_weekly_fix   TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '是否固定周期约：0-否，1-是',
    group_id        VARCHAR(32)    DEFAULT '' COMMENT '周期约组ID（同一批生成共享）',
    status          TINYINT(1)     NOT NULL DEFAULT 1 COMMENT '状态：1-正常，0-已取消',
    create_time     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_room_date (room_id, booking_date),
    INDEX idx_group_id (group_id),
    INDEX idx_emp_date (emp_no, booking_date),
    INDEX idx_booking_date (booking_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议室预定记录表';
