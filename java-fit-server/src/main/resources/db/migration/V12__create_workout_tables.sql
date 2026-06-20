-- Flyway migration V12: Create training tables for Pocket Gym
-- 口袋健身核心业务表

-- 1. 训练计划表 (Training Plan)
CREATE TABLE training_plan (
    id              VARCHAR(32)   NOT NULL COMMENT '主键ID',
    emp_no          VARCHAR(7)    NOT NULL COMMENT '员工工号（7位）',
    plan_name       VARCHAR(100)  NOT NULL COMMENT '计划名称（如：推日计划、腿日计划）',
    description     VARCHAR(500)  DEFAULT NULL COMMENT '计划描述',
    muscle_group    VARCHAR(30)   DEFAULT NULL COMMENT '主要目标肌群（CHEST/BACK/SHOULDER/ARM/LEG/GLUTE/CORE/FULL_BODY）',
    sort_no         INT           DEFAULT 0 COMMENT '排序号',
    status          TINYINT(1)    DEFAULT 1 COMMENT '状态（1:启用, 0:禁用）',
    create_time     DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_emp_no (emp_no),
    KEY idx_muscle_group (muscle_group),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='训练计划';

-- 2. 训练计划详情表 (Training Plan Detail)
CREATE TABLE training_plan_detail (
    id              VARCHAR(32)   NOT NULL COMMENT '主键ID',
    plan_id         VARCHAR(32)   NOT NULL COMMENT '训练计划ID',
    action_id       VARCHAR(32)   NOT NULL COMMENT '动作ID（关联gym_action）',
    sort_no         INT           DEFAULT 0 COMMENT '动作排序号',
    target_sets     INT           DEFAULT 3 COMMENT '目标组数',
    target_reps     INT           DEFAULT 10 COMMENT '目标次数',
    target_weight   DECIMAL(6,2)  DEFAULT 0 COMMENT '目标重量(kg)',
    rest_seconds    INT           DEFAULT 60 COMMENT '组间休息时间(秒)',
    notes           VARCHAR(200)  DEFAULT NULL COMMENT '备注',
    create_time     DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_plan_id (plan_id),
    KEY idx_action_id (action_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='训练计划详情';

-- 3. 训练记录表 (Training Session)
CREATE TABLE training_session (
    id              VARCHAR(32)   NOT NULL COMMENT '主键ID',
    emp_no          VARCHAR(7)    NOT NULL COMMENT '员工工号（7位）',
    plan_id         VARCHAR(32)   DEFAULT NULL COMMENT '使用的训练计划ID（可为空，表示自由训练）',
    session_date    DATE          NOT NULL COMMENT '训练日期',
    start_time      DATETIME      DEFAULT NULL COMMENT '训练开始时间',
    end_time        DATETIME      DEFAULT NULL COMMENT '训练结束时间',
    total_volume    DECIMAL(10,2) DEFAULT 0 COMMENT '总训练容量(kg)',
    total_duration  INT           DEFAULT 0 COMMENT '训练总时长(秒)',
    feeling         TINYINT       DEFAULT NULL COMMENT '训练感受（1:很差, 2:较差, 3:一般, 4:较好, 5:很好）',
    notes           VARCHAR(500)  DEFAULT NULL COMMENT '训练备注',
    create_time     DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_emp_no (emp_no),
    KEY idx_session_date (session_date),
    KEY idx_plan_id (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='训练记录';

-- 4. 训练记录详情表 (Training Session Detail)
CREATE TABLE training_session_detail (
    id              VARCHAR(32)   NOT NULL COMMENT '主键ID',
    session_id      VARCHAR(32)   NOT NULL COMMENT '训练记录ID',
    plan_detail_id  VARCHAR(32)   DEFAULT NULL COMMENT '计划详情ID（关联training_plan_detail，可为空）',
    action_id       VARCHAR(32)   NOT NULL COMMENT '动作ID',
    sort_no         INT           DEFAULT 0 COMMENT '动作排序号',
    set_no          INT           NOT NULL COMMENT '组号',
    target_weight   DECIMAL(6,2)  DEFAULT 0 COMMENT '目标重量(kg)',
    target_reps     INT           DEFAULT 0 COMMENT '目标次数',
    actual_weight   DECIMAL(6,2)  DEFAULT 0 COMMENT '实际重量(kg)',
    actual_reps     INT           DEFAULT 0 COMMENT '实际次数',
    is_completed    TINYINT(1)    DEFAULT 0 COMMENT '是否完成（1:完成, 0:未完成）',
    is_pr           TINYINT(1)    DEFAULT 0 COMMENT '是否破纪录（1:是, 0:否）',
    notes           VARCHAR(200)  DEFAULT NULL COMMENT '备注',
    create_time     DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_session_id (session_id),
    KEY idx_action_id (action_id),
    KEY idx_set (session_id, action_id, set_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='训练记录详情';

-- 5. 身体指标记录表 (Body Metric)
CREATE TABLE body_metric (
    id              VARCHAR(32)   NOT NULL COMMENT '主键ID',
    emp_no          VARCHAR(7)    NOT NULL COMMENT '员工工号（7位）',
    metric_date     DATE          NOT NULL COMMENT '记录日期',
    weight          DECIMAL(5,2)  DEFAULT NULL COMMENT '体重(kg)',
    body_fat        DECIMAL(4,2)  DEFAULT NULL COMMENT '体脂率(%)',
    bmi             DECIMAL(4,2)  DEFAULT NULL COMMENT 'BMI指数',
    muscle_mass     DECIMAL(5,2)  DEFAULT NULL COMMENT '肌肉量(kg)',
    notes           VARCHAR(200)  DEFAULT NULL COMMENT '备注',
    create_time     DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_emp_date (emp_no, metric_date),
    KEY idx_metric_date (metric_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='身体指标记录';
