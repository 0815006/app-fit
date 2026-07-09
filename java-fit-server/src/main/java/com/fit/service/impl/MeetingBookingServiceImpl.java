package com.fit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fit.entity.MeetingBooking;
import com.fit.mapper.MeetingBookingMapper;
import com.fit.service.MeetingBookingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class MeetingBookingServiceImpl extends ServiceImpl<MeetingBookingMapper, MeetingBooking> implements MeetingBookingService {

    private static final int MAX_SLOT = 20; // 08:00 - 17:30, 20 个半小时槽

    @Override
    @Transactional
    public List<MeetingBooking> createBooking(MeetingBooking booking, Integer weeklyWeeks) {
        // 参数校验
        validateBookingSlots(booking.getStartSlot(), booking.getEndSlot());

        List<MeetingBooking> result = new ArrayList<>();
        int weeks = (weeklyWeeks != null && weeklyWeeks > 0) ? Math.min(weeklyWeeks, 8) : 1;
        String groupId = weeks > 1 ? UUID.randomUUID().toString().replace("-", "") : "";

        for (int w = 0; w < weeks; w++) {
            LocalDate targetDate = booking.getBookingDate().plusWeeks(w);

            // 并发冲突检测：检查目标日期 [startSlot, endSlot) 是否已有有效预定
            List<MeetingBooking> existing = getRoomBookings(booking.getRoomId(), targetDate);
            for (MeetingBooking ex : existing) {
                if (slotsOverlap(booking.getStartSlot(), booking.getEndSlot(), ex.getStartSlot(), ex.getEndSlot())) {
                    throw new RuntimeException("时段 [" + booking.getStartSlot() + "-" + booking.getEndSlot()
                            + ") 与已有预定冲突（预定人：" + ex.getEmpName() + "）");
                }
            }

            MeetingBooking b = new MeetingBooking();
            b.setRoomId(booking.getRoomId());
            b.setBookingDate(targetDate);
            b.setStartSlot(booking.getStartSlot());
            b.setEndSlot(booking.getEndSlot());
            b.setEmpNo(booking.getEmpNo());
            b.setEmpName(booking.getEmpName());
            b.setMeetingTitle(booking.getMeetingTitle());
            b.setAttendees(booking.getAttendees());
            b.setIsWeeklyFix(weeks > 1 ? 1 : 0);
            b.setGroupId(groupId);
            b.setStatus(1);
            save(b);
            result.add(b);
        }

        log.info("Created {} bookings, groupId={}, empNo={}", result.size(), groupId, booking.getEmpNo());
        return result;
    }

    @Override
    public List<MeetingBooking> getRoomBookings(String roomId, LocalDate date) {
        LambdaQueryWrapper<MeetingBooking> qw = new LambdaQueryWrapper<>();
        qw.eq(MeetingBooking::getRoomId, roomId)
          .eq(MeetingBooking::getBookingDate, date)
          .eq(MeetingBooking::getStatus, 1)
          .orderByAsc(MeetingBooking::getStartSlot);
        return list(qw);
    }

    @Override
    public List<MeetingBooking> getMyBookings(String empNo) {
        LambdaQueryWrapper<MeetingBooking> qw = new LambdaQueryWrapper<>();
        qw.eq(MeetingBooking::getEmpNo, empNo)
          .eq(MeetingBooking::getStatus, 1)
          .orderByAsc(MeetingBooking::getBookingDate)
          .orderByAsc(MeetingBooking::getStartSlot);
        return list(qw);
    }

    @Override
    public MeetingBooking updateInfo(String id, String meetingTitle, String attendees) {
        MeetingBooking b = getById(id);
        if (b == null) {
            throw new RuntimeException("预定记录不存在");
        }
        b.setMeetingTitle(meetingTitle != null ? meetingTitle : b.getMeetingTitle());
        b.setAttendees(attendees != null ? attendees : b.getAttendees());
        updateById(b);
        return b;
    }

    @Override
    public void cancelBooking(String id) {
        MeetingBooking b = getById(id);
        if (b == null) {
            throw new RuntimeException("预定记录不存在");
        }
        b.setStatus(0);
        updateById(b);
        log.info("Cancelled booking id={}", id);
    }

    @Override
    public int cancelGroupFuture(String groupId, LocalDate fromDate) {
        LambdaQueryWrapper<MeetingBooking> qw = new LambdaQueryWrapper<>();
        qw.eq(MeetingBooking::getGroupId, groupId)
          .eq(MeetingBooking::getStatus, 1)
          .ge(MeetingBooking::getBookingDate, fromDate);

        List<MeetingBooking> list = list(qw);
        for (MeetingBooking b : list) {
            b.setStatus(0);
            updateById(b);
        }
        log.info("Cancelled {} future bookings for groupId={}, fromDate={}", list.size(), groupId, fromDate);
        return list.size();
    }

    // ── helpers ──

    /** 校验 slot 范围合法性 */
    static void validateBookingSlots(Integer startSlot, Integer endSlot) {
        if (startSlot == null || endSlot == null) {
            throw new RuntimeException("时段参数不能为空");
        }
        if (startSlot < 0 || startSlot >= MAX_SLOT) {
            throw new RuntimeException("起始时段无效，范围 [0, " + (MAX_SLOT - 1) + "]");
        }
        if (endSlot <= startSlot || endSlot > MAX_SLOT) {
            throw new RuntimeException("结束时段必须大于起始且 ≤ " + MAX_SLOT);
        }
    }

    /** 两个开区间 [a1, a2) 与 [b1, b2) 是否重叠 */
    static boolean slotsOverlap(int a1, int a2, int b1, int b2) {
        return a1 < b2 && b1 < a2;
    }
}
