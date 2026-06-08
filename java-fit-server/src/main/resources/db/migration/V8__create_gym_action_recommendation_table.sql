-- Flyway migration V8: Create gym_action_recommendation table
-- Action training recommendation for AI coach.

CREATE TABLE gym_action_recommendation (
    id                 VARCHAR(32)  NOT NULL COMMENT '主键ID',
    action_id          VARCHAR(32)  NOT NULL COMMENT '动作ID',
    training_goal      VARCHAR(30)  NOT NULL COMMENT '训练目标（HYPERTROPHY:增肌, FAT_LOSS:减脂, STRENGTH:力量, ENDURANCE:耐力）',
    min_sets           TINYINT      DEFAULT 3 COMMENT '推荐最小组数',
    max_sets           TINYINT      DEFAULT 5 COMMENT '推荐最大组数',
    min_reps           TINYINT      DEFAULT 8 COMMENT '推荐每组最小次数',
    max_reps           TINYINT      DEFAULT 12 COMMENT '推荐每组最大次数',
    recommend_rest_time INT         DEFAULT 60 COMMENT '推荐组间休息时间（单位：秒）',
    intensity_tips     VARCHAR(255) DEFAULT NULL COMMENT '强度建议提示（如：建议使用 75%-85% 1RM 重量）',
    create_time        DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time        DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_action_goal (action_id, training_goal),
    KEY idx_action (action_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动作训练建议与AI教练推荐表';
