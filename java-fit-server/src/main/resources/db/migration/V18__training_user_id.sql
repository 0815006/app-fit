-- =====================================================
-- V18: 训练模块操作人由 emp_no(7位工号) 迁移为 user_id(user表主键)
-- training_plan / training_session / body_metric
-- 直接抛弃存量数据，不做数据迁移
-- =====================================================

-- 1. training_plan: emp_no -> user_id
ALTER TABLE training_plan
    DROP KEY idx_emp_no,
    CHANGE COLUMN emp_no user_id VARCHAR(32) NOT NULL COMMENT '所属用户ID(关联user表主键)',
    ADD KEY idx_user_id (user_id);

-- 2. training_session: emp_no -> user_id
ALTER TABLE training_session
    DROP KEY idx_emp_no,
    CHANGE COLUMN emp_no user_id VARCHAR(32) NOT NULL COMMENT '所属用户ID(关联user表主键)',
    ADD KEY idx_user_id (user_id);

-- 3. body_metric: emp_no -> user_id
ALTER TABLE body_metric
    DROP INDEX uk_emp_date,
    DROP KEY idx_metric_date,
    CHANGE COLUMN emp_no user_id VARCHAR(32) NOT NULL COMMENT '所属用户ID(关联user表主键)',
    ADD UNIQUE KEY uk_user_date (user_id, metric_date),
    ADD KEY idx_metric_date (metric_date);
