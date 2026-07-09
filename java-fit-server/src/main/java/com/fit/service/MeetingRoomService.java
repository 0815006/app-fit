package com.fit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fit.entity.MeetingRoom;

public interface MeetingRoomService extends IService<MeetingRoom> {

    /** 上架/下架切换，返回最新状态 */
    int toggleActive(String id);
}
