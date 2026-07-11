-- ============================================================
-- Flyway V20: login_record 新增 user_id 字段，适配 user 表
-- 用于记录真实的 user 表用户登录记录
-- ============================================================
ALTER TABLE login_record
    ADD COLUMN user_id VARCHAR(32) NULL COMMENT 'user表主键ID' AFTER id,
    ADD INDEX idx_user_id (user_id),
    ADD INDEX idx_user_id_login_type (user_id, login_type);
