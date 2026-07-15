-- V35: user_subscribe_quota emp_no → user_id 迁移
-- 将可变 emp_no 替换为不可变 user_id (user表主键)，解决用户修改工号后配额数据断裂问题

-- 1. 新增 user_id 字段（先可空，回填后再设 NOT NULL）
ALTER TABLE user_subscribe_quota
    ADD COLUMN user_id VARCHAR(32) NULL COMMENT '用户ID (user表主键)' AFTER id;

-- 2. 通过 emp_no 关联 user 表回填 user_id
UPDATE user_subscribe_quota q
    INNER JOIN user u ON q.emp_no = u.emp_no
SET q.user_id = u.id;

-- 3. 删除无法匹配到 user 的孤儿数据
DELETE FROM user_subscribe_quota WHERE user_id IS NULL;

-- 4. user_id 设为 NOT NULL
ALTER TABLE user_subscribe_quota
    MODIFY COLUMN user_id VARCHAR(32) NOT NULL COMMENT '用户ID (user表主键)';

-- 5. 删除旧的 emp_no 唯一键和索引
ALTER TABLE user_subscribe_quota
    DROP INDEX uk_emp_template;

ALTER TABLE user_subscribe_quota
    DROP INDEX idx_emp_no;

-- 6. 删除 emp_no 字段
ALTER TABLE user_subscribe_quota
    DROP COLUMN emp_no;

-- 7. 创建新的 user_id 唯一键和索引
ALTER TABLE user_subscribe_quota
    ADD UNIQUE KEY uk_user_template (user_id, template_id);

ALTER TABLE user_subscribe_quota
    ADD INDEX idx_user_id (user_id);
