-- V19: user 表增加员工姓名字段，用于会议预定等场景展示员工姓名
ALTER TABLE `user`
    ADD COLUMN `emp_name` VARCHAR(50) NULL COMMENT '员工姓名' AFTER `emp_no`;
