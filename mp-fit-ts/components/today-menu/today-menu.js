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

Component({
  data: {
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

  lifetimes: {
    attached: function () {
      this.loadTodayMenu()
    },
  },

  methods: {
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
      var val = e.detail.value
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
  },
})
