-- ============================================================
-- Flyway V30: user 表增加隐私协议同意时间字段
-- ============================================================
ALTER TABLE `user`
  ADD COLUMN `privacy_agreed_time` DATETIME NULL
  COMMENT '隐私协议同意时间' AFTER `update_time`;
