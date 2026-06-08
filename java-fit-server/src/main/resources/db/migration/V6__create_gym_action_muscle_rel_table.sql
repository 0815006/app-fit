-- Flyway migration V6: Create gym_action_muscle_rel table
-- Action-muscle relationship table.

CREATE TABLE gym_action_muscle_rel (
    id         VARCHAR(32) NOT NULL COMMENT '主键ID',
    action_id  VARCHAR(32) NOT NULL COMMENT '动作ID',
    muscle_id  VARCHAR(32) NOT NULL COMMENT '肌群ID',
    is_primary TINYINT(1)  DEFAULT 0 COMMENT '是否为主肌群（1:主目标肌群, 0:协同/次要肌群）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_action_muscle (action_id, muscle_id),
    KEY idx_action (action_id),
    KEY idx_muscle (muscle_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动作肌群关联';
