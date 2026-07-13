var api = require('../../utils/request')

var CHINESE_WEEKDAYS = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']

function getTodayStr() {
  var d = new Date()
  var y = d.getFullYear()
  var m = String(d.getMonth() + 1)
  if (m.length < 2) m = '0' + m
  var day = String(d.getDate())
  if (day.length < 2) day = '0' + day
  return y + '-' + m + '-' + day
}

function getDayOfWeekStr(dateStr) {
  if (!dateStr) return ''
  var d = new Date(dateStr.replace(/-/g, '/'))
  return CHINESE_WEEKDAYS[d.getDay()]
}

function dateAddDays(dateStr, days) {
  var d = new Date(dateStr.replace(/-/g, '/'))
  d.setDate(d.getDate() + days)
  var y = d.getFullYear()
  var m = String(d.getMonth() + 1)
  if (m.length < 2) m = '0' + m
  var day = String(d.getDate())
  if (day.length < 2) day = '0' + day
  return y + '-' + m + '-' + day
}

function makeDateDisplay(dateStr) {
  if (!dateStr) return ''
  var wd = getDayOfWeekStr(dateStr)
  return dateStr + ' ' + wd
}

Component({
  data: {
    // ── Meeting Booking ──
    meetingDataReady: false,
    meetingLoading: false,
    meetingDate: getTodayStr(),
    meetingDateDisplay: makeDateDisplay(getTodayStr()),
    meetingIsToday: true,
    meetingRooms: [],
    meetingActiveRoomIndex: 0,
    meetingActiveRoomId: '',
    meetingBoardData: null,
    meetingViewMode: 'name',  // 'name' = 预定人视图, 'title' = 会议名称视图
    meetingViewLabel: '预定人视图',
    meetingShowBookingModal: false,
    meetingBookingMode: '',  // 'create'|'edit-mine'|'view-others'|'view-lock'
    meetingSelectedSlot: null,
    meetingSelectedSlotEndLabel: '',
    meetingFormTitle: '',
    meetingFormAttendees: '',
    meetingFormWeeklyWeeks: 1,
    meetingFormWeeklyEnabled: false,
    meetingSaving: false,
  },

  lifetimes: {
    attached: function () {
      this.loadMeetingData()
    },
  },

  methods: {
    // ═══════════════════════════════════════════════
    // ── Meeting Booking Logic ──
    // ═══════════════════════════════════════════════

    loadMeetingData: function () {
      var that = this
      that.setData({ meetingLoading: true })

      // Load active rooms first, then board
      api.get('/meeting-room/active').then(function (res) {
        var rooms = res.data || []
        that.setData({
          meetingRooms: rooms,
          meetingActiveRoomId: rooms.length > 0 ? rooms[0].id : '',
          meetingActiveRoomIndex: 0,
        })
        return that._loadMeetingBoard()
      }).catch(function () {
        that.setData({ meetingLoading: false })
      })
    },

    _loadMeetingBoard: function () {
      var that = this
      var roomId = that.data.meetingActiveRoomId
      if (!roomId) {
        that.setData({ meetingLoading: false, meetingDataReady: true })
        return
      }

      api.get('/meeting-booking/board', { date: that.data.meetingDate }).then(function (res) {
        var boardData = res.data
        // Find the active room's board data
        var roomBoard = null
        if (boardData && boardData.rooms) {
          for (var i = 0; i < boardData.rooms.length; i++) {
            if (boardData.rooms[i].roomId === roomId) {
              roomBoard = boardData.rooms[i]
              break
            }
          }
        }
        // Pre-calculate isPast for each slot
        if (roomBoard && roomBoard.slots && that.data.meetingIsToday) {
          var now = new Date()
          for (var s = 0; s < roomBoard.slots.length; s++) {
            var slotIndex = roomBoard.slots[s].slot
            var endHour = 8 + Math.floor((slotIndex + 1) / 2)
            var endMinute = ((slotIndex + 1) % 2) * 30
            var slotEnd = new Date(now.getFullYear(), now.getMonth(), now.getDate(), endHour, endMinute)
            roomBoard.slots[s].isPast = now > slotEnd
          }
        } else if (roomBoard && roomBoard.slots) {
          for (var s2 = 0; s2 < roomBoard.slots.length; s2++) {
            roomBoard.slots[s2].isPast = false
          }
        }
        // Mark non-work-time slots: 0=8:00-8:30, 9=12:30-13:00, 10=13:00-13:30, 19=17:30-18:00
        if (roomBoard && roomBoard.slots) {
          var nonWorkSlots = [0, 9, 10, 19]
          for (var n = 0; n < roomBoard.slots.length; n++) {
            roomBoard.slots[n].isNonWorkTime = nonWorkSlots.indexOf(roomBoard.slots[n].slot) !== -1
          }
        }
        // Build displayRows: pair every two slots into one row
        if (roomBoard && roomBoard.slots) {
          var displayRows = []
          var allSlots = roomBoard.slots
          var idx = 0
          while (idx < allSlots.length) {
            var firstTime = allSlots[idx].timeLabel
            var endHour = (parseInt(firstTime, 10) + 1) % 24
            var hourLabel = firstTime + '-' + (endHour < 10 ? '0' : '') + endHour + ':00'
            // Insert afternoon divider before 13:00
            if (firstTime === '13:00') {
              displayRows.push({ key: 'divider', type: 'divider' })
            }
            if (idx + 1 < allSlots.length) {
              displayRows.push({ key: 'd' + idx, hourLabel: hourLabel, slots: [allSlots[idx], allSlots[idx + 1]] })
              idx += 2
            } else {
              displayRows.push({ key: 'd' + idx, hourLabel: hourLabel, slots: [allSlots[idx], null] })
              idx++
            }
          }
          roomBoard.displayRows = displayRows
        }
        that.setData({
          meetingBoardData: roomBoard,
          meetingLoading: false,
          meetingDataReady: true,
        })
      }).catch(function () {
        that.setData({ meetingLoading: false, meetingDataReady: true })
      })
    },

    // ── Date navigation ──
    handleMeetingPrevDay: function () {
      var newDate = dateAddDays(this.data.meetingDate, -1)
      var todayStr = getTodayStr()
      this.setData({
        meetingDate: newDate,
        meetingDateDisplay: makeDateDisplay(newDate),
        meetingIsToday: newDate === todayStr,
        meetingLoading: true,
      })
      this._loadMeetingBoard()
    },

    handleMeetingNextDay: function () {
      var newDate = dateAddDays(this.data.meetingDate, 1)
      var todayStr = getTodayStr()
      this.setData({
        meetingDate: newDate,
        meetingDateDisplay: makeDateDisplay(newDate),
        meetingIsToday: newDate === todayStr,
        meetingLoading: true,
      })
      this._loadMeetingBoard()
    },

    handleMeetingDateChange: function (e) {
      var val = e.detail.value
      if (val === this.data.meetingDate) return
      var todayStr = getTodayStr()
      this.setData({
        meetingDate: val,
        meetingDateDisplay: makeDateDisplay(val),
        meetingIsToday: val === todayStr,
        meetingLoading: true,
      })
      this._loadMeetingBoard()
    },

    goToMeetingToday: function () {
      var todayStr = getTodayStr()
      if (this.data.meetingDate === todayStr) return
      this.setData({
        meetingDate: todayStr,
        meetingDateDisplay: makeDateDisplay(todayStr),
        meetingIsToday: true,
        meetingLoading: true,
      })
      this._loadMeetingBoard()
    },

    // ── Room switch ──
    handleMeetingRoomChange: function (e) {
      var index = parseInt(e.currentTarget.dataset.index)
      var room = this.data.meetingRooms[index]
      if (!room || room.id === this.data.meetingActiveRoomId) return
      this.setData({
        meetingActiveRoomIndex: index,
        meetingActiveRoomId: room.id,
        meetingLoading: true,
      })
      this._loadMeetingBoard()
    },

    // ── View mode toggle ──
    handleMeetingViewToggle: function () {
      if (this.data.meetingViewMode === 'name') {
        this.setData({ meetingViewMode: 'title', meetingViewLabel: '会议名称视图' })
      } else {
        this.setData({ meetingViewMode: 'name', meetingViewLabel: '预定人视图' })
      }
    },

    // ── Slot click ──
    handleMeetingSlotClick: function (e) {
      var slotIndex = parseInt(e.currentTarget.dataset.slot)
      var board = this.data.meetingBoardData
      if (!board || !board.slots) return

      var slot = board.slots[slotIndex]
      if (!slot) return

      // Check if past
      if (slot.isPast) {
        wx.showToast({ title: '无法预约过去的时间', icon: 'none' })
        return
      }

      // Calculate end time label
      var endLabel = this._slotToTimeLabel(slotIndex + 1)

      this.setData({
        meetingSelectedSlot: slot,
        meetingSelectedSlotEndLabel: endLabel,
        meetingFormTitle: slot.booking ? slot.booking.meetingTitle || '' : '',
        meetingFormAttendees: slot.booking ? slot.booking.attendees || '' : '',
        meetingFormWeeklyWeeks: 1,
        meetingFormWeeklyEnabled: false,
      })

      if (slot.type === 'FREE') {
        // 预约前检查用户信息是否已完善
        var that = this
        this._checkProfileBeforeBooking(function () {
          that.setData({ meetingBookingMode: 'create', meetingShowBookingModal: true })
        })
      } else if (slot.type === 'MY_BOOKING') {
        this.setData({ meetingBookingMode: 'edit-mine', meetingShowBookingModal: true })
      } else if (slot.type === 'BOOKED') {
        this.setData({ meetingBookingMode: 'view-others', meetingShowBookingModal: true })
      } else if (slot.type === 'ADMIN_LOCK') {
        this.setData({ meetingBookingMode: 'view-lock', meetingShowBookingModal: true })
      }
    },

    // ── Booking modal ──
    closeMeetingModal: function () {
      this.setData({ meetingShowBookingModal: false })
    },

    // Create booking
    handleMeetingCreate: function () {
      var that = this
      var slot = that.data.meetingSelectedSlot
      if (!slot) return

      that.setData({ meetingSaving: true })
      api.post('/meeting-booking', {
        roomId: that.data.meetingActiveRoomId,
        bookingDate: that.data.meetingDate,
        startSlot: slot.slot,
        endSlot: slot.slot + 1,
        meetingTitle: that.data.meetingFormTitle,
        attendees: that.data.meetingFormAttendees,
        weeklyWeeks: that.data.meetingFormWeeklyEnabled ? (that.data.meetingFormWeeklyWeeks || 1) : undefined,
      }).then(function () {
        wx.showToast({ title: '预定成功', icon: 'success' })
        that.setData({ meetingShowBookingModal: false, meetingSaving: false })
        that._loadMeetingBoard()
      }).catch(function () {
        that.setData({ meetingSaving: false })
      })
    },

    // Update booking
    handleMeetingUpdate: function () {
      var that = this
      var slot = that.data.meetingSelectedSlot
      if (!slot || !slot.booking) return

      that.setData({ meetingSaving: true })
      api.put('/meeting-booking/' + slot.booking.bookingId, {
        meetingTitle: that.data.meetingFormTitle,
        attendees: that.data.meetingFormAttendees,
      }).then(function () {
        wx.showToast({ title: '已保存', icon: 'success' })
        that.setData({ meetingShowBookingModal: false })
        that._loadMeetingBoard()
      }).catch(function () {
        that.setData({ meetingSaving: false })
      })
    },

    // Cancel single booking
    handleMeetingCancel: function () {
      var that = this
      var slot = that.data.meetingSelectedSlot
      if (!slot || !slot.booking) return

      wx.showModal({
        title: '取消预定',
        content: '确定要取消此预定吗？',
        success: function (res) {
          if (res.confirm) {
            that.setData({ meetingSaving: true })
            api.del('/meeting-booking/' + slot.booking.bookingId).then(function () {
              wx.showToast({ title: '已取消', icon: 'success' })
              that.setData({ meetingShowBookingModal: false })
              that._loadMeetingBoard()
            }).catch(function () {
              that.setData({ meetingSaving: false })
            })
          }
        },
      })
    },

    // Cancel group future
    handleMeetingCancelGroup: function () {
      var that = this
      var slot = that.data.meetingSelectedSlot
      if (!slot || !slot.booking) return

      wx.showModal({
        title: '一键取消后续',
        content: '将取消后续所有周期约，已过去的保留不动。确定吗？',
        success: function (res) {
          if (res.confirm) {
            that.setData({ meetingSaving: true })
            api.del('/meeting-booking/group/' + slot.booking.groupId, { fromDate: that.data.meetingDate }).then(function (result) {
              var count = (result.data) || 0
              wx.showToast({ title: '已取消 ' + count + ' 场', icon: 'success' })
              that.setData({ meetingShowBookingModal: false })
              that._loadMeetingBoard()
            }).catch(function () {
              that.setData({ meetingSaving: false })
            })
          }
        },
      })
    },

    // Form inputs
    onMeetingTitleInput: function (e) {
      this.setData({ meetingFormTitle: e.detail.value })
    },
    onMeetingAttendeesInput: function (e) {
      this.setData({ meetingFormAttendees: e.detail.value })
    },
    onMeetingWeeklyToggle: function () {
      this.setData({ meetingFormWeeklyEnabled: !this.data.meetingFormWeeklyEnabled })
    },
    onMeetingWeeklyInput: function (e) {
      var val = parseInt(e.detail.value) || 1
      if (val < 1) val = 1
      if (val > 8) val = 8
      this.setData({ meetingFormWeeklyWeeks: val })
    },

    // ── noop (阻冒泡) ──
    noop: function () {},

    // ── 预约前检查用户信息是否完善 ──
    _checkProfileBeforeBooking: function (onPassed) {
      var that = this
      api.get('/user/current').then(function (res) {
        var user = res.data
        var empNo = user.empNo || ''
        var empName = user.empName || ''
        if (!empNo || empNo === '0000000') {
          wx.showModal({
            title: '请完善个人信息',
            content: '预约会议室前需要维护您的7位工号和员工姓名，请前往个人中心完善资料。',
            confirmText: '去完善',
            cancelText: '取消',
            success: function (modalRes) {
              if (modalRes.confirm) {
                wx.navigateTo({ url: '/pages/profile/profile' })
              }
            }
          })
          return
        }
        if (!empName || empName.trim() === '') {
          wx.showModal({
            title: '请完善个人信息',
            content: '预约会议室前需要维护您的员工姓名，请前往个人中心完善资料。',
            confirmText: '去完善',
            cancelText: '取消',
            success: function (modalRes) {
              if (modalRes.confirm) {
                wx.navigateTo({ url: '/pages/profile/profile' })
              }
            }
          })
          return
        }
        onPassed()
      }).catch(function () {
        wx.showToast({ title: '获取用户信息失败，请稍后再试', icon: 'none' })
      })
    },

    // Helper: get meeting time label for slot
    _slotToTimeLabel: function (s) {
      var hour = 8 + Math.floor(s / 2)
      var minute = (s % 2) * 30
      return (hour < 10 ? '0' + hour : '' + hour) + ':' + (minute < 10 ? '0' + minute : '' + minute)
    },
  },
})
