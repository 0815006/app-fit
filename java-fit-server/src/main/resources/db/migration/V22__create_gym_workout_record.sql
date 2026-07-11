-- ============================================================
-- Flyway V22: 健身打卡训练记录表 gym_workout_record
-- 对应 PRD 一期核心业务表
-- ============================================================
CREATE TABLE gym_workout_record (
    id               VARCHAR(32)   NOT NULL COMMENT '主键ID（雪花ID）',
    user_id          VARCHAR(32)   NOT NULL COMMENT '用户ID（关联 user.id）',
    action_id        VARCHAR(32)   NOT NULL COMMENT '动作ID（关联 gym_action.id）',
    muscle_group     VARCHAR(50)   NOT NULL COMMENT '冗余：肌群大类编码（CHEST/BACK/SHOULDER/ARM/LEG/GLUTE/CORE 等）',
    start_time       DATETIME      NOT NULL COMMENT '训练开始时间',
    end_time         DATETIME      DEFAULT NULL COMMENT '训练结束时间（结束时/修正时写入）',
    exhaustion_score DECIMAL(3,2)  DEFAULT NULL COMMENT '力竭度系数（0.50 ~ 1.20）',
    status           TINYINT       NOT NULL DEFAULT 0 COMMENT '状态：0=训练中, 1=正常结束, 2=超时修正结束',
    create_time      DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_action_id (action_id),
    KEY idx_muscle_group (muscle_group),
    KEY idx_status (status),
    KEY idx_start_time (start_time),
    KEY idx_user_muscle_time (user_id, muscle_group, start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健身打卡训练记录';
