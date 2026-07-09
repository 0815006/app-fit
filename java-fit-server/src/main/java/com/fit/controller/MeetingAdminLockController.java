package com.fit.controller;

import com.fit.common.EmpContext;
import com.fit.common.Result;
import com.fit.entity.MeetingAdminLock;
import com.fit.service.MeetingAdminLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/meeting-admin-lock")
@RequiredArgsConstructor
public class MeetingAdminLockController {

    private final MeetingAdminLockService adminLockService;

    /** 创建强制征用 */
    @PostMapping
    public Result<MeetingAdminLock> createLock(@RequestBody CreateLockRequest req) {
        MeetingAdminLock lock = new MeetingAdminLock();
        lock.setRoomId(req.roomId());
        lock.setLockDate(req.lockDate());
        lock.setStartSlot(req.startSlot());
        lock.setEndSlot(req.endSlot());
        lock.setReason(req.reason() != null ? req.reason() : "");
        lock.setDeptName(req.deptName() != null ? req.deptName() : "");
        lock.setOperatorEmpNo(EmpContext.getEmpNo());

        MeetingAdminLock created = adminLockService.createLock(lock);
        return Result.success(created);
    }

    /** 释放征用 */
    @DeleteMapping("/{id}")
    public Result<Void> releaseLock(@PathVariable String id) {
        adminLockService.releaseLock(id);
        return Result.success();
    }

    /** 查询指定会议室某日所有征用记录 */
    @GetMapping("/room/{roomId}")
    public Result<List<MeetingAdminLock>> getRoomLocks(@PathVariable String roomId,
                                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(adminLockService.getRoomLocks(roomId, date));
    }

    public record CreateLockRequest(
            String roomId,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate lockDate,
            int startSlot,
            int endSlot,
            String reason,
            String deptName) {
    }
}
