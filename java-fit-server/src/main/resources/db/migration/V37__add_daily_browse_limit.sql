ALTER TABLE user_subscribe_quota
    ADD COLUMN last_browse_date DATE NULL COMMENT '最近一次浏览攒次数日期',
    ADD COLUMN today_browse_count INT NOT NULL DEFAULT 0 COMMENT '今日已浏览攒次数';
