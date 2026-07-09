package com.fit.dto;

import java.util.List;

/**
 * 看板响应 DTO：某日所有会议室的预约与征用数据
 */
public record BoardResponse(List<RoomBoard> rooms) {

    public record RoomBoard(
            String roomId,
            String roomName,
            int capacity,
            List<String> facilities,
            String photoUrl,
            List<SlotInfo> slots) {
    }

    public record SlotInfo(
            int slot,
            String timeLabel,
            String type,          // FREE / BOOKED / ADMIN_LOCK
            BookingInfo booking,
            LockInfo lock) {
    }

    public record BookingInfo(
            String bookingId,
            String empNo,
            String empName,
            String meetingTitle,
            String attendees,
            boolean isWeeklyFix,
            String groupId) {
    }

    public record LockInfo(
            String lockId,
            String reason,
            String deptName) {
    }
}
