-- V36: push_message_history emp_no → user_id 迁移
-- 历史日志表同步迁移，user_id 允许 NULL（旧数据可能已无对应用户）

-- 1. 新增 user_id 字段（允许 NULL，历史日志不强制）
ALTER TABLE push_message_history
    ADD COLUMN user_id VARCHAR(32) NULL COMMENT '用户ID (user表主键)' AFTER id;

-- 2. 通过 emp_no 关联 user 表回填 user_id
UPDATE push_message_history h
    INNER JOIN user u ON h.emp_no = u.emp_no
SET h.user_id = u.id;

-- 3. 删除旧的 emp_no 索引和字段（保留无法回填的历史记录）
ALTER TABLE push_message_history
    DROP INDEX idx_emp_no;

ALTER TABLE push_message_history
    DROP COLUMN emp_no;

-- 4. 创建 user_id 索引
ALTER TABLE push_message_history
    ADD INDEX idx_user_id (user_id);
