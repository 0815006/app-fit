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

    // 收藏相关
    favSet: {},
    favLoading: false,
    expandedDishId: null,   // 当前展开延申区域的菜品ID

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

  pageLifetimes: {
    show: function () {
      // 每次页面显示时刷新收藏状态
      this.loadFavorites()
    },
  },

  methods: {
    // ── 加载收藏状态 ──
    loadFavorites: function () {
      var that = this
      var records = that.data.todayMenuRecords
      if (!records || records.length === 0) return

      var dishNames = []
      for (var i = 0; i < records.length; i++) {
        if (records[i].dishName && dishNames.indexOf(records[i].dishName) < 0) {
          dishNames.push(records[i].dishName)
        }
      }
      if (dishNames.length === 0) return

      api.get('/favorite-dish/check', { dishNames: dishNames.join(',') })
        .then(function (res) {
          var favList = res.data || []
          var favSet = {}
          for (var i = 0; i < favList.length; i++) {
            favSet[favList[i]] = true
          }
          that.setData({ favSet: favSet })
        })
        .catch(function () {
          // 静默失败
        })
    },

    // ── 点击菜品卡片展开/收起延申区域 ──
    handleDishTap: function (e) {
      var id = e.currentTarget.dataset.id
      // 如果点的是同一个菜品，则收起；否则展开新菜品
      if (this.data.expandedDishId === id) {
        this.setData({ expandedDishId: null })
      } else {
        this.setData({ expandedDishId: id })
      }
    },

    // ── 点击星标切换收藏 ──
    handleToggleFavorite: function (e) {
      var that = this
      var dishName = e.currentTarget.dataset.dish
      if (!dishName) return

      if (that.data.favLoading) return
      that.setData({ favLoading: true })

      api.post('/favorite-dish/toggle', { dishName: dishName })
        .then(function (res) {
          var data = res.data
          var favSet = that.data.favSet
          if (data.favorited) {
            favSet[dishName] = true
            // 首次收藏时调用 wx.requestSubscribeMessage 收集订阅授权
            try {
              wx.requestSubscribeMessage({
                tmplIds: ['KABRC3CxbGsD2TZQNjPWcWEl17kU1q0rNipugHkMUmA'],
                success: function () {
                  // 用户授权成功，次数已在后端 toggle 中 +1
                },
                fail: function () {
                  // 用户拒绝授权，不影响收藏
                },
              })
            } catch (err) {
              // 忽略错误
            }
          } else {
            delete favSet[dishName]
          }
          that.setData({ favSet: favSet, favLoading: false })
        })
        .catch(function () {
          that.setData({ favLoading: false })
          wx.showToast({ title: '操作失败', icon: 'none' })
        })
    },

    loadTodayMenu: function () {
      var that = this
      that.setData({ todayLoading: true, todayShowSkeleton: true, expandedDishId: null })

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
          // 菜单加载完成后加载收藏状态
          that.loadFavorites()
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
