-- ============================================================
-- Flyway V21: gym_muscle 追加 base_recovery_hours 字段
-- 用于健身打卡模块的肌肉恢复时间计算
-- ============================================================
ALTER TABLE gym_muscle
    ADD COLUMN base_recovery_hours INT NOT NULL DEFAULT 48
    COMMENT '基础恢复小时数（大肌群72/48，小肌群24）';

-- 按肌群大类批量更新
UPDATE gym_muscle SET base_recovery_hours = 48 WHERE muscle_group = 'CHEST';
UPDATE gym_muscle SET base_recovery_hours = 48 WHERE muscle_group = 'BACK';
UPDATE gym_muscle SET base_recovery_hours = 24 WHERE muscle_group = 'SHOULDER';
UPDATE gym_muscle SET base_recovery_hours = 24 WHERE muscle_group = 'ARM';
UPDATE gym_muscle SET base_recovery_hours = 72 WHERE muscle_group = 'LEG';
UPDATE gym_muscle SET base_recovery_hours = 48 WHERE muscle_group = 'GLUTE';
UPDATE gym_muscle SET base_recovery_hours = 24 WHERE muscle_group = 'CORE';
UPDATE gym_muscle SET base_recovery_hours = 48 WHERE muscle_group = 'FULL_BODY';
