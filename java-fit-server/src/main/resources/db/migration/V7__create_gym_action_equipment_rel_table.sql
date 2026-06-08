-- Flyway migration V7: Create gym_action_equipment_rel table
-- Action-equipment relationship table.

CREATE TABLE gym_action_equipment_rel (
    id           VARCHAR(32) NOT NULL COMMENT '主键ID',
    action_id    VARCHAR(32) NOT NULL COMMENT '动作ID',
    equipment_id VARCHAR(32) NOT NULL COMMENT '器械ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_action_equipment (action_id, equipment_id),
    KEY idx_action (action_id),
    KEY idx_equipment (equipment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动作器械关联';
