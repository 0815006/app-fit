package com.fit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fit.entity.MeetingRoom;
import com.fit.mapper.MeetingRoomMapper;
import com.fit.service.MeetingRoomService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MeetingRoomServiceImpl extends ServiceImpl<MeetingRoomMapper, MeetingRoom> implements MeetingRoomService {

    @Override
    public int toggleActive(String id) {
        MeetingRoom room = getById(id);
        if (room == null) {
            throw new RuntimeException("会议室不存在");
        }
        int newStatus = room.getIsActive() == 1 ? 0 : 1;
        room.setIsActive(newStatus);
        updateById(room);
        log.info("MeetingRoom {} toggle active -> {}", id, newStatus);
        return newStatus;
    }
}
