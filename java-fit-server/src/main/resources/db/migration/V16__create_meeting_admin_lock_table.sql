-- Flyway migration V16: Create meeting_admin_lock table
-- 行政征用覆盖表（不删除原预定，仅覆盖显示）

CREATE TABLE meeting_admin_lock (
    id              VARCHAR(32)    NOT NULL COMMENT '主键雪花ID',
    room_id         VARCHAR(32)    NOT NULL COMMENT '关联会议室ID',
    lock_date       DATE           NOT NULL COMMENT '征用日期',
    start_slot      INT            NOT NULL COMMENT '起始时段编号',
    end_slot        INT            NOT NULL COMMENT '结束时段编号（不包含）',
    reason          VARCHAR(500)   NOT NULL DEFAULT '' COMMENT '征用原因',
    dept_name       VARCHAR(100)   NOT NULL DEFAULT '' COMMENT '发起部门',
    operator_emp_no VARCHAR(7)     NOT NULL COMMENT '操作行政人员工号',
    create_time     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_lock_room_date (room_id, lock_date),
    INDEX idx_lock_date (lock_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行政征用覆盖表';
