package com.fit.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fit.common.EmpContext;
import com.fit.common.Result;
import com.fit.dto.BoardResponse;
import com.fit.entity.MeetingAdminLock;
import com.fit.entity.MeetingBooking;
import com.fit.entity.MeetingRoom;
import com.fit.entity.User;
import com.fit.service.MeetingAdminLockService;
import com.fit.service.MeetingBookingService;
import com.fit.service.MeetingRoomService;
import com.fit.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/meeting-booking")
@RequiredArgsConstructor
public class MeetingBookingController {

    private final MeetingBookingService bookingService;
    private final MeetingRoomService meetingRoomService;
    private final MeetingAdminLockService adminLockService;
    private final UserService userService;

    /** 创建预定（从 User 实体读取 empNo / empName，强制校验用户已维护身份信息） */
    @PostMapping
    public Result<List<MeetingBooking>> create(@RequestBody CreateBookingRequest req) {
        long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        String empNo = user.getEmpNo();
        String empName = user.getEmpName();

        // 校验员工号是否已维护（不得为 null 或默认的 "0000000"）
        if (empNo == null || "0000000".equals(empNo) || empNo.isBlank()) {
            return Result.error(400, "请先在个人中心维护您的7位工号和员工姓名后再预约会议室");
        }
        // 校验员工姓名是否已维护
        if (empName == null || empName.isBlank()) {
            return Result.error(400, "请先在个人中心维护您的员工姓名后再预约会议室");
        }

        MeetingBooking booking = new MeetingBooking();
        booking.setRoomId(req.roomId());
        booking.setBookingDate(req.bookingDate());
        booking.setStartSlot(req.startSlot());
        booking.setEndSlot(req.endSlot());
        booking.setEmpNo(empNo);
        booking.setEmpName(empName);
        booking.setMeetingTitle(req.meetingTitle() != null ? req.meetingTitle() : "");
        booking.setAttendees(req.attendees() != null ? req.attendees() : "");

        List<MeetingBooking> result = bookingService.createBooking(booking, req.weeklyWeeks());
        return Result.success(result);
    }

    /** 修改预定信息 */
    @PutMapping("/{id}")
    public Result<MeetingBooking> updateInfo(@PathVariable String id, @RequestBody UpdateBookingRequest req) {
        MeetingBooking updated = bookingService.updateInfo(id, req.meetingTitle(), req.attendees());
        return Result.success(updated);
    }

    /** 取消单次预定 */
    @DeleteMapping("/{id}")
    public Result<Void> cancel(@PathVariable String id) {
        bookingService.cancelBooking(id);
        return Result.success();
    }

    /** 一键取消后续所有周期约 */
    @DeleteMapping("/group/{groupId}")
    public Result<Integer> cancelGroupFuture(@PathVariable String groupId,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate) {
        int count = bookingService.cancelGroupFuture(groupId, fromDate);
        return Result.success(count);
    }

    /** 查询指定会议室某日所有预定 */
    @GetMapping("/room/{roomId}")
    public Result<List<MeetingBooking>> getRoomBookings(@PathVariable String roomId,
                                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(bookingService.getRoomBookings(roomId, date));
    }

    /** 全局看板：某日所有会议室合并数据 */
    @GetMapping("/board")
    public Result<BoardResponse> getBoard(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<MeetingRoom> rooms = meetingRoomService.lambdaQuery()
                .eq(MeetingRoom::getIsActive, 1)
                .orderByAsc(MeetingRoom::getSortOrder)
                .list();

        List<BoardResponse.RoomBoard> roomBoards = new ArrayList<>();

        for (MeetingRoom room : rooms) {
            List<MeetingBooking> bookings = bookingService.getRoomBookings(room.getId(), date);
            List<MeetingAdminLock> locks = adminLockService.getRoomLocks(room.getId(), date);

            // Build slot → booking/lock map
            Map<Integer, MeetingBooking> slotBookingMap = new HashMap<>();
            for (MeetingBooking b : bookings) {
                for (int s = b.getStartSlot(); s < b.getEndSlot(); s++) {
                    slotBookingMap.put(s, b);
                }
            }

            Map<Integer, MeetingAdminLock> slotLockMap = new HashMap<>();
            for (MeetingAdminLock l : locks) {
                for (int s = l.getStartSlot(); s < l.getEndSlot(); s++) {
                    slotLockMap.put(s, l);
                }
            }

            String currentEmpNo = EmpContext.getEmpNo();
            List<BoardResponse.SlotInfo> slots = new ArrayList<>();
            for (int s = 0; s < 20; s++) {
                String timeLabel = slotToTimeLabel(s);
                MeetingAdminLock lock = slotLockMap.get(s);
                if (lock != null) {
                    slots.add(new BoardResponse.SlotInfo(s, timeLabel, "ADMIN_LOCK", null,
                            new BoardResponse.LockInfo(lock.getId(), lock.getReason(), lock.getDeptName())));
                } else {
                    MeetingBooking booking = slotBookingMap.get(s);
                    if (booking != null) {
                        boolean isMine = currentEmpNo.equals(booking.getEmpNo());
                        slots.add(new BoardResponse.SlotInfo(s, timeLabel, isMine ? "MY_BOOKING" : "BOOKED",
                                new BoardResponse.BookingInfo(booking.getId(), booking.getEmpNo(), booking.getEmpName(),
                                        booking.getMeetingTitle(), booking.getAttendees(),
                                        booking.getIsWeeklyFix() == 1, booking.getGroupId()),
                                null));
                    } else {
                        slots.add(new BoardResponse.SlotInfo(s, timeLabel, "FREE", null, null));
                    }
                }
            }

            List<String> facilities = parseFacilities(room.getFacilities());
            roomBoards.add(new BoardResponse.RoomBoard(room.getId(), room.getName(), room.getCapacity(),
                    facilities, room.getPhotoUrl() != null ? room.getPhotoUrl() : "", slots));
        }

        return Result.success(new BoardResponse(roomBoards));
    }

    /** 0=08:00, 1=08:30, ..., 19=17:30 */
    private static String slotToTimeLabel(int slot) {
        int hour = 8 + slot / 2;
        int minute = (slot % 2) * 30;
        return String.format("%02d:%02d", hour, minute);
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static List<String> parseFacilities(String facilitiesJson) {
        if (facilitiesJson == null || facilitiesJson.isBlank() || "[]".equals(facilitiesJson)) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(facilitiesJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse facilities JSON: {}", facilitiesJson, e);
            return List.of();
        }
    }

    // ── Request Records ──

    public record CreateBookingRequest(
            String roomId,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bookingDate,
            int startSlot,
            int endSlot,
            String meetingTitle,
            String attendees,
            Integer weeklyWeeks) {
    }

    public record UpdateBookingRequest(
            String meetingTitle,
            String attendees) {
    }
}
