var api = require('../../utils/request')
var config = require('../../utils/config')

Page({
  data: {
    showProfileModal: false,
    showProfileBanner: false,   // 信息不完善横幅
    myLoginCount: null,
    totalLoginCount: null,
    statsLoading: false,
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
    // scroll-view height for content above tab-bar
    scrollHeight: 500,
  },

  onLoad: function () {
    var windowInfo = wx.getWindowInfo()
    var rpx = windowInfo.windowWidth / 750
    var tabBarPx = Math.round(100 * rpx)
    var scrollHeight = windowInfo.windowHeight - tabBarPx
    this.setData({ scrollHeight: scrollHeight })
  },

  /**
   * 供 app.js 在静默登录成功后回调
   */
  onLoginReady: function () {
    this._loginReady = true
    this.loadMiniProgramStats()
    // 默认Tab为健身打卡，加载坚持榜数据
    this.loadRankings()
    // 检查是否需要展示信息不完善横幅
    this._checkProfileBanner()
  },

  onShow: function () {
    // 每次回到前台直接加载统计数据（App.onShow 已保证 token 有效）
    this.loadMiniProgramStats()
    // 如果榜单数据为空，补拉
    if (this.data.streakData.length === 0 && this.data.cumulativeData.length === 0) {
      this.loadRankings()
    }
    this._checkProfileBanner()
  },

  // ── Tab switching ──
  switchTab: function (e) {
    var key = e.currentTarget.dataset.key
    if (key === 'menu') {
      // 今日菜单 — 跳转至独立页面
      wx.navigateTo({ url: '/pages/menu/menu' })
      return
    }
    if (key === 'meeting') {
      // 会议预定 — 跳转至独立页面
      wx.navigateTo({ url: '/pages/meeting/meeting' })
      return
    }
    this.setData({ activeTab: key })
    if (key === 'fitness') {
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
        // 解析头像相对路径为完整 URL
        if (userData && userData.avatarUrl) {
          if (userData.avatarUrl.indexOf('/uploads/') === 0) {
            var base = config.BASE_URL.replace(/\/api\/?$/, '')
            userData.avatarUrl = base + userData.avatarUrl
          }
        }
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

  // ── 健身打卡入口导航 ──

  _guardWriteNav: function (url) {
    var isNewUser = wx.getStorageSync('isNewUser')
    if (isNewUser) {
      // 新用户尚未完善资料，拦截导航并弹出完善资料弹窗
      this.showProfileModal()
      return
    }
    wx.navigateTo({ url: url })
  },

  navigateToRanking: function () {
    wx.navigateTo({ url: '/pages/workout/ranking/ranking' })
  },

  navigateToCheckin: function () {
    this._guardWriteNav('/pages/workout/checkin/checkin')
  },

  navigateToMakeup: function () {
    this._guardWriteNav('/pages/workout/makeup/makeup')
  },

  navigateToWeekly: function () {
    this._guardWriteNav('/pages/workout/weekly/weekly')
  },

  navigateToCalendar: function () {
    this._guardWriteNav('/pages/workout/calendar/calendar')
  },

  // ── noop (阻冒泡) ──
  noop: function () {},

  // ── 信息不完善横幅 ──
  _checkProfileBanner: function () {
    var isNewUser = wx.getStorageSync('isNewUser')
    if (isNewUser) {
      this.setData({ showProfileBanner: true })
    } else {
      this.setData({ showProfileBanner: false })
    }
  },

  onTapProfileBanner: function () {
    this.showProfileModal()
  },

  // ── 资料完善弹窗（由 app.requestProfile() 驱动） ──
  showProfileModal: function () {
    // 隐藏横幅，展示弹窗
    this.setData({ showProfileModal: true, showProfileBanner: false })
  },

  onProfileClose: function () {
    this.setData({ showProfileModal: false })
    // 弹窗关闭后重新检查横幅
    this._checkProfileBanner()
  },

  onProfileSuccess: function () {
    this.setData({ showProfileModal: false, showProfileBanner: false })
    // 刷新小程序统计数据
    this.loadMiniProgramStats()
  },

  // ── 跳转隐私保护协议 ──
  goToPrivacy: function () {
    wx.navigateTo({ url: '/pages/privacy/privacy' })
  },

  // ── 跳转收藏菜品页 ──
  goToFavoriteDishes: function () {
    wx.navigateTo({ url: '/pages/favorite-dishes/favorite-dishes' })
  },

  // ── Share ──
  onShareAppMessage: function () {
    return {
      title: 'Fit 健身打卡',
      path: '/pages/index/index',
    }
  },
})
