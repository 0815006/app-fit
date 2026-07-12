-- V29__extend_gym_workout_record.sql
-- 扩展训练记录表：支持记录重量、次数、组数，用于健身榜单计算
ALTER TABLE gym_workout_record
    ADD COLUMN weight      DECIMAL(6,2)  DEFAULT NULL COMMENT '本次动作使用重量(kg)，NULL表示无负重（自重/有氧/拉伸等）',
    ADD COLUMN reps        INT           DEFAULT NULL COMMENT '本次动作训练次数，NULL表示未填写或不适用',
    ADD COLUMN set_count   INT           DEFAULT NULL COMMENT '本次训练组数，NULL表示未填写或不适用',
    ADD COLUMN rm_estimate DECIMAL(7,2)  DEFAULT NULL COMMENT '自动计算的1RM估值(kg)，仅当weight+reps均非NULL时计算',
    ADD COLUMN is_pr       TINYINT       DEFAULT 0 COMMENT '是否破个人纪录 0=否 1=是';
