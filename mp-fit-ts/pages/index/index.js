var api = require('../../utils/request')

Page({
  data: {
    showProfileModal: false,
    myLoginCount: null,
    totalLoginCount: null,
    statsLoading: false,
    techDetailOpen: false,
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

  // ── Tech Stack Collapse ──
  toggleTechDetail: function () {
    this.setData({ techDetailOpen: !this.data.techDetailOpen })
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

  // ── 健身打卡入口导航 ──

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

  // ── 跳转隐私保护协议 ──
  goToPrivacy: function () {
    wx.navigateTo({ url: '/pages/privacy/privacy' })
  },

  // ── Share ──
  onShareAppMessage: function () {
    return {
      title: 'Fit 健身打卡',
      path: '/pages/index/index',
    }
  },
})
