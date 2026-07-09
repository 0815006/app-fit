-- Flyway migration V17: Add unique constraint for admin lock (数据库层最后防线)
-- 注意：meeting_booking 因支持多 slot 范围和软删除，无法用简单 UNIQUE 索引防冲突，
-- 冲突检测由应用层 MeetingBookingServiceImpl.createBooking() 负责。

-- 征用表：同一房间、同一日期、同一起始时段唯一
ALTER TABLE meeting_admin_lock
  ADD UNIQUE INDEX uq_lock_room_date_startslot (room_id, lock_date, start_slot);
