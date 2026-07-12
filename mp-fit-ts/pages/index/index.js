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

function getDefaultMealType() {
  var h = new Date().getHours()
  if (h < 10) return '早餐'
  if (h < 14) return '午餐'
  if (h < 19) return '晚餐'
  return '夜宵'
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

Page({
  data: {
    showProfileModal: false,
    myLoginCount: null,
    totalLoginCount: null,
    statsLoading: false,
    techCollapsed: true,
    currentUser: null,
    activeTab: 'fitness',

    // 坚持榜双卡片数据
    streakData: [],
    cumulativeData: [],

    tabBars: [
      { key: 'fitness', label: '健身打卡', icon: '🏋️' },
      { key: 'menu', label: '今日菜单', icon: '🍽️' },
      { key: 'meeting', label: '会议预定', icon: '📅' },
      { key: 'tech', label: '个人信息', icon: '👤' },
    ],
    techStack: [
      { category: '后端框架', name: 'Spring Boot', version: '3.4+' },
      { category: '后端语言', name: 'Java', version: '21' },
      { category: 'ORM', name: 'MyBatis Plus', version: '3.5.11' },
      { category: '数据库', name: 'MySQL', version: '8.4 LTS' },
      { category: '数据库迁移', name: 'Flyway', version: 'Latest' },
      { category: 'Web框架', name: 'Vue', version: '3.5+' },
      { category: '构建工具', name: 'Vite', version: '6.x' },
      { category: 'Web语言', name: 'TypeScript', version: '5.7+' },
      { category: 'Web UI库', name: 'Element Plus', version: '2.9+' },
      { category: '小程序框架', name: '微信原生', version: 'Latest' },
      { category: '小程序语言', name: 'JavaScript', version: 'ES6+' },
      { category: '小程序 UI库', name: 'TDesign Miniprogram', version: 'Latest' },
    ],

    // scroll-view height for content above tab-bar
    scrollHeight: 500,

    // ── Today's Menu ──
    todayZone: '一期',
    todayDate: getTodayStr(),
    todayDateDisplay: makeDateDisplay(getTodayStr()),
    todayMealType: getDefaultMealType(),
    todayLoading: false,
    todayMenuRecords: [],
    todayGrouped: [],
    todayShowSkeleton: false,
    isTodaySelected: true,

    todayMealTypes: [
      { label: '早餐', icon: '🍳', value: '早餐', key: 'breakfast' },
      { label: '午餐', icon: '🍛', value: '午餐', key: 'lunch' },
      { label: '晚餐', icon: '🍲', value: '晚餐', key: 'dinner' },
      { label: '夜宵', icon: '🌙', value: '夜宵', key: 'supper' },
    ],

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

  onLoad: function () {
    var windowInfo = wx.getWindowInfo()
    var rpx = windowInfo.windowWidth / 750
    var tabBarPx = Math.round(100 * rpx)
    var scrollHeight = windowInfo.windowHeight - tabBarPx
    this.setData({ scrollHeight: scrollHeight })

    // 检查是否需要弹出资料完善弹窗（基于上次已存储的标记）
    this._checkProfileModal()
  },

  /**
   * 供 app.js 在静默登录成功后回调
   */
  onLoginReady: function () {
    this._loginReady = true
    this.loadMiniProgramStats()
    // 登录完成后重新检查资料完善弹窗（新用户标记在登录时才写入）
    this._checkProfileModal()
    // 默认Tab为健身打卡，加载坚持榜数据
    this.loadRankings()
  },

  onShow: function () {
    // 仅当静默登录已完成后才加载统计数据
    if (this._loginReady) {
      this.loadMiniProgramStats()
    }
    // 每次回到前台也检查一次资料完善弹窗
    this._checkProfileModal()
  },

  // ── Tab switching ──
  switchTab: function (e) {
    var key = e.currentTarget.dataset.key
    this.setData({ activeTab: key })
    if (key === 'meeting') {
      if (!this.data.meetingDataReady) {
        this.loadMeetingData()
      }
    } else if (key === 'menu') {
      this.loadTodayMenu()
    } else if (key === 'fitness') {
      this.loadRankings()
    }
  },

  // ── 加载坚持榜双卡片 ──
  loadRankings: function () {
    var that = this
    // 连续打卡 Top 5
    api.get('/training-stats/ranking/consistency-v2', { days: 30, mode: 'streak' }).then(function (res) {
      var list = res.data || []
      that.setData({ streakData: list.slice(0, 5) })
    }).catch(function () {
      that.setData({ streakData: [] })
    })
    // 累计打卡 Top 5
    api.get('/training-stats/ranking/consistency-v2', { days: 30, mode: 'cumulative' }).then(function (res) {
      var list = res.data || []
      that.setData({ cumulativeData: list.slice(0, 5) })
    }).catch(function () {
      that.setData({ cumulativeData: [] })
    })
  },

  // ── Tech Stack Collapse ──
  toggleTechCollapse: function () {
    this.setData({ techCollapsed: !this.data.techCollapsed })
  },

// ── 跳转个人信息页 ──
goToProfile: function () {
  wx.navigateTo({
    url: '/pages/profile/profile',
  })
},


  // ── Mini Program Login Stats + Current User ──
  loadMiniProgramStats: function () {
    var that = this
    that.setData({ statsLoading: true })

    var pStats = api.get('/login-record/mini-program-stats')
    var pUser  = api.get('/user/current')

    Promise.all([pStats, pUser])
      .then(function (results) {
        var statsData = results[0].data
        var userData  = results[1].data
        that.setData({
          myLoginCount: statsData.myCount,
          totalLoginCount: statsData.totalCount,
          currentUser: userData,
        })
      })
      .catch(function () {
        that.setData({ myLoginCount: null, totalLoginCount: null, currentUser: null })
      })
      .finally(function () {
        that.setData({ statsLoading: false })
      })
  },

  // ═══════════════════════════════════════════════
  // ── Today's Menu Logic ──
  // ═══════════════════════════════════════════════

  loadTodayMenu: function () {
    var that = this
    that.setData({ todayLoading: true, todayShowSkeleton: true })

    api
      .get('/canteen-menu/records', {
        page: 1,
        size: 500,
        canteenZone: that.data.todayZone,
        menuDate: that.data.todayDate,
        mealType: that.data.todayMealType,
      })
      .then(function (res) {
        var records = res.data.records || []
        var grouped = that._groupRecords(records)
        that.setData({
          todayMenuRecords: records,
          todayGrouped: grouped,
        })
      })
      .catch(function () {
        that.setData({ todayMenuRecords: [], todayGrouped: [] })
      })
      .finally(function () {
        that.setData({ todayLoading: false })
        setTimeout(function () {
          that.setData({ todayShowSkeleton: false })
        }, 200)
      })
  },

  _groupRecords: function (records) {
    var map = {}
    for (var i = 0; i < records.length; i++) {
      var cat = records[i].categoryName
      if (!map[cat]) map[cat] = []
      map[cat].push(records[i])
    }
    var result = []
    for (var key in map) {
      if (map.hasOwnProperty(key)) {
        result.push({ name: key, items: map[key] })
      }
    }
    return result
  },

  // ── Tier 1: Zone ──
  handleTodayZoneChange: function (e) {
    var zone = e.currentTarget.dataset.zone
    if (zone === this.data.todayZone) return
    this.setData({ todayZone: zone })
    this.loadTodayMenu()
  },

  // ── Tier 2: Date ──
  handleTodayDateChange: function (e) {
    var val = e.detail.value // YYYY-MM-DD from picker
    if (val === this.data.todayDate) return
    var todayStr = getTodayStr()
    this.setData({
      todayDate: val,
      todayDateDisplay: makeDateDisplay(val),
      isTodaySelected: val === todayStr,
    })
    this.loadTodayMenu()
  },

  goToPrevDay: function () {
    var newDate = dateAddDays(this.data.todayDate, -1)
    var todayStr = getTodayStr()
    this.setData({
      todayDate: newDate,
      todayDateDisplay: makeDateDisplay(newDate),
      isTodaySelected: newDate === todayStr,
    })
    this.loadTodayMenu()
  },

  goToNextDay: function () {
    var newDate = dateAddDays(this.data.todayDate, 1)
    var todayStr = getTodayStr()
    this.setData({
      todayDate: newDate,
      todayDateDisplay: makeDateDisplay(newDate),
      isTodaySelected: newDate === todayStr,
    })
    this.loadTodayMenu()
  },

  goToToday: function () {
    var todayStr = getTodayStr()
    if (this.data.todayDate === todayStr) return
    this.setData({
      todayDate: todayStr,
      todayDateDisplay: makeDateDisplay(todayStr),
      isTodaySelected: true,
    })
    this.loadTodayMenu()
  },

  // ── Tier 3: Meal Type ──
  handleTodayMealChange: function (e) {
    var type = e.currentTarget.dataset.type
    if (type === this.data.todayMealType) return
    this.setData({ todayMealType: type })
    this.loadTodayMenu()
  },

  // ═══════════════════════════════════════════════
  // ── 健身打卡入口导航 ──
  // ═══════════════════════════════════════════════

  navigateToRanking: function () {
    wx.navigateTo({ url: '/pages/workout/ranking/ranking' })
  },

  navigateToCheckin: function () {
    wx.navigateTo({ url: '/pages/workout/checkin/checkin' })
  },

  navigateToMakeup: function () {
    wx.navigateTo({ url: '/pages/workout/makeup/makeup' })
  },

  navigateToWeekly: function () {
    wx.navigateTo({ url: '/pages/workout/weekly/weekly' })
  },

  navigateToCalendar: function () {
    wx.navigateTo({ url: '/pages/workout/calendar/calendar' })
  },

  // ── noop (阻冒泡) ──
  noop: function () {},

  // ── 资料完善弹窗 ──
  _checkProfileModal: function () {
    if (wx.getStorageSync('showProfileModal')) {
      this.setData({ showProfileModal: true })
    }
  },

  onProfileClose: function () {
    this.setData({ showProfileModal: false })
  },

  onProfileSuccess: function () {
    this.setData({ showProfileModal: false })
    // 刷新小程序统计数据
    this.loadMiniProgramStats()
  },

  // ── Share ──
  onShareAppMessage: function () {
    return {
      title: 'Fit 健身打卡',
      path: '/pages/index/index',
    }
  },

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
      // Layout: left=hour label, right=two half-hour cells side by side (no time in cells)
      // Insert divider between morning (8:00-11:30) and afternoon (13:00-17:30)
      if (roomBoard && roomBoard.slots) {
        var displayRows = []
        var allSlots = roomBoard.slots
        var idx = 0
        while (idx < allSlots.length) {
          // Build time range label from first slot (e.g. "08:00" → "08:00-09:00")
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

  _isMeetingSlotPast: function (slotIndex) {
    if (!this.data.meetingIsToday) return false
    var now = new Date()
    var endHour = 8 + Math.floor((slotIndex + 1) / 2)
    var endMinute = ((slotIndex + 1) % 2) * 30
    var slotEnd = new Date(now.getFullYear(), now.getMonth(), now.getDate(), endHour, endMinute)
    return now > slotEnd
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

  // ── 预约前检查用户信息是否完善 ──
  _checkProfileBeforeBooking: function (onPassed) {
    var that = this
    // 调用后端 /user/current 获取最新用户信息
    api.get('/user/current').then(function (res) {
      var user = res.data
      var empNo = user.empNo || ''
      var empName = user.empName || ''
      // 检查工号是否已维护（不得为空或默认的 "0000000"）
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
      // 检查员工姓名是否已维护
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
      // 检查通过，执行回调
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
})