-- Flyway migration V1: Create login_record table
-- Records every user login event, distinguishing between WEB and MINI_PROGRAM clients.

CREATE TABLE login_record (
    id          VARCHAR(32)  NOT NULL COMMENT '主键ID (雪花ID)',
    emp_no      VARCHAR(7)   NOT NULL COMMENT '员工工号 (7位)',
    login_type  VARCHAR(20)  NOT NULL COMMENT '登录类型: WEB-网页端, MINI_PROGRAM-小程序端',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_emp_no (emp_no),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户登录记录表';
