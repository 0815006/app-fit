package com.fit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fit.entity.MeetingAdminLock;

import java.time.LocalDate;
import java.util.List;

public interface MeetingAdminLockService extends IService<MeetingAdminLock> {

    /** 创建强制征用 */
    MeetingAdminLock createLock(MeetingAdminLock lock);

    /** 释放征用（删除记录，使原预定恢复） */
    void releaseLock(String id);

    /** 查询指定会议室某日的所有征用记录 */
    List<MeetingAdminLock> getRoomLocks(String roomId, LocalDate date);
}
