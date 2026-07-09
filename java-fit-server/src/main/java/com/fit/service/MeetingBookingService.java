package com.fit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fit.entity.MeetingBooking;

import java.time.LocalDate;
import java.util.List;

public interface MeetingBookingService extends IService<MeetingBooking> {

    /** 创建预定（含周期约拆分） */
    List<MeetingBooking> createBooking(MeetingBooking booking, Integer weeklyWeeks);

    /** 查询指定会议室某日的所有有效预定 */
    List<MeetingBooking> getRoomBookings(String roomId, LocalDate date);

    /** 查询当前用户所有有效预定 */
    List<MeetingBooking> getMyBookings(String empNo);

    /** 修改会议信息（名称/参会人） */
    MeetingBooking updateInfo(String id, String meetingTitle, String attendees);

    /** 取消单次预定 */
    void cancelBooking(String id);

    /** 一键取消后续所有周期约（group_id + 当前时间之后的） */
    int cancelGroupFuture(String groupId, LocalDate fromDate);
}
