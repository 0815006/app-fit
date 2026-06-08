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
    empNo: '0000000',
    loginCount: null,
    loading: false,
    activeTab: 'stats',
    tabBars: [
      { key: 'stats', label: '登录统计', icon: '📊' },
      { key: 'menu', label: '今日菜单', icon: '🍽️' },
      { key: 'tech', label: '技术选型', icon: '🛠️' },
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
  },

  onLoad: function () {
    var empNo = wx.getStorageSync('empNo') || '0000000'
    // calculate scroll-view height: window height minus custom tab bar (~100rpx)
    var sys = wx.getSystemInfoSync()
    var rpx = sys.windowWidth / 750
    var tabBarPx = Math.round(100 * rpx)
    var scrollHeight = sys.windowHeight - tabBarPx
    this.setData({ empNo: empNo, scrollHeight: scrollHeight })
    this.loadLoginData()
  },

  onShow: function () {
    this.loadLoginData()
  },

  // ── Tab switching ──
  switchTab: function (e) {
    var key = e.currentTarget.dataset.key
    this.setData({ activeTab: key })
    if (key === 'stats') {
      this.loadLoginData()
    } else if (key === 'menu') {
      this.loadTodayMenu()
    }
  },

  // ── Login stats ──
  loadLoginData: function () {
    var that = this
    var empNo = wx.getStorageSync('empNo') || '0000000'
    that.setData({ loading: true, empNo: empNo })

    api
      .post('/login-record', { loginType: 'MINI_PROGRAM' })
      .then(function () {
        return api.get('/login-record/count/' + empNo)
      })
      .then(function (result) {
        that.setData({ loginCount: result.data })
      })
      .catch(function () {
        that.setData({ loginCount: null })
      })
      .finally(function () {
        that.setData({ loading: false })
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
})
