package com.fit.controller;

import com.fit.common.Result;
import com.fit.entity.MeetingRoom;
import com.fit.service.MeetingRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/meeting-room")
@RequiredArgsConstructor
public class MeetingRoomController {

    private final MeetingRoomService meetingRoomService;

    /** 查询所有会议室（含下架） */
    @GetMapping
    public Result<List<MeetingRoom>> listAll() {
        return Result.success(meetingRoomService.list());
    }

    /** 查询上架会议室（小程序用） */
    @GetMapping("/active")
    public Result<List<MeetingRoom>> listActive() {
        List<MeetingRoom> list = meetingRoomService.lambdaQuery()
                .eq(MeetingRoom::getIsActive, 1)
                .orderByAsc(MeetingRoom::getSortOrder)
                .list();
        return Result.success(list);
    }

    /** 新增会议室 */
    @PostMapping
    public Result<MeetingRoom> create(@RequestBody MeetingRoom room) {
        meetingRoomService.save(room);
        return Result.success(room);
    }

    /** 编辑会议室 */
    @PutMapping("/{id}")
    public Result<MeetingRoom> update(@PathVariable String id, @RequestBody MeetingRoom room) {
        room.setId(id);
        meetingRoomService.updateById(room);
        return Result.success(meetingRoomService.getById(id));
    }

    /** 删除会议室 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        meetingRoomService.removeById(id);
        return Result.success();
    }

    /** 上架/下架切换 */
    @PutMapping("/{id}/toggle")
    public Result<Integer> toggleActive(@PathVariable String id) {
        int newStatus = meetingRoomService.toggleActive(id);
        return Result.success(newStatus);
    }
}
