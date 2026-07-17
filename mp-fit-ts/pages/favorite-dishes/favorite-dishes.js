var api = require('../../utils/request')

var TEMPLATE_ID = 'KABRC3CxbGsD2TZQNjPWcWEl17kU1q0rNipugHkMUmA'

Page({
  data: {
    // 推送次数
    remainingCount: 0,
    pushEnabled: true,
    quotaLoading: true,

    // 收藏列表
    favoriteList: [],
    listLoading: true,

    // 取消收藏确认中的 dishName
    deletingDish: null,
  },

  onLoad: function () {
    this.loadAll()
  },

  onShow: function () {
    this.loadAll()
  },

  onPullDownRefresh: function () {
    this.loadAll().then(function () {
      wx.stopPullDownRefresh()
    })
  },

  loadAll: function () {
    var that = this
    return Promise.all([that.loadQuota(), that.loadFavorites()])
  },

  // ── 加载推送次数 ──
  loadQuota: function () {
    var that = this
    that.setData({ quotaLoading: true })

    return api.get('/subscribe-quota', { templateId: TEMPLATE_ID })
      .then(function (res) {
        var data = res.data
        that.setData({
          remainingCount: data.remainingCount || 0,
          pushEnabled: data.pushEnabled !== false,
          quotaLoading: false,
        })
      })
      .catch(function () {
        that.setData({ quotaLoading: false })
      })
  },

  // ── 加载收藏列表 ──
  loadFavorites: function () {
    var that = this
    that.setData({ listLoading: true })

    return api.get('/favorite-dish/list')
      .then(function (res) {
        that.setData({
          favoriteList: res.data || [],
          listLoading: false,
        })
      })
      .catch(function () {
        that.setData({ listLoading: false })
      })
  },

  // ── 切换推送开关 ──
  handleTogglePush: function (e) {
    var that = this
    var enabled = e.detail.value
    api.post('/subscribe-quota/toggle-push', {
      templateId: TEMPLATE_ID,
      pushEnabled: enabled,
    })
      .then(function () {
        that.setData({ pushEnabled: enabled })
        if (!enabled) {
          wx.showToast({ title: '推送已关闭', icon: 'none' })
        } else {
          wx.showToast({ title: '推送已开启', icon: 'none' })
        }
      })
      .catch(function () {
        // 恢复原状态
        that.setData({ pushEnabled: !enabled })
        wx.showToast({ title: '操作失败', icon: 'none' })
      })
  },

  // ── 取消收藏 ──
  handleCancelFav: function (e) {
    var that = this
    var dishName = e.currentTarget.dataset.dish

    wx.showModal({
      title: '取消收藏',
      content: '确定要取消收藏「' + dishName + '」吗？',
      confirmText: '确定',
      cancelText: '取消',
      confirmColor: '#f56c6c',
      success: function (modalRes) {
        if (modalRes.confirm) {
          that._doCancelFav(dishName)
        }
      },
    })
  },

  _doCancelFav: function (dishName) {
    var that = this
    wx.showLoading({ title: '取消中...' })

    api.del('/favorite-dish/' + encodeURIComponent(dishName))
      .then(function () {
        wx.hideLoading()
        wx.showToast({ title: '已取消收藏', icon: 'success' })
        // 刷新列表
        that.loadAll()
      })
      .catch(function () {
        wx.hideLoading()
        wx.showToast({ title: '操作失败', icon: 'none' })
      })
  },

  // ── 浏览菜品攒次数 ──
  handleBrowseDishes: function () {
    var that = this
    wx.showLoading({ title: '攒次数中...' })

    api.post('/subscribe-quota/browse', {
      templateId: TEMPLATE_ID,
    })
      .then(function (res) {
        wx.hideLoading()
        var newCount = res.data.remainingCount
        that.setData({ remainingCount: newCount })
        wx.showToast({ title: '推送次数 +1', icon: 'success' })

        // 攒次数成功后立即弹微信授权弹窗
        try {
          wx.requestSubscribeMessage({
            tmplIds: [TEMPLATE_ID],
            success: function () {
              // 用户授权成功
            },
            fail: function () {
              // 用户拒绝授权，不影响已攒的次数
            },
          })
        } catch (err) {
          // 忽略错误
        }
      })
      .catch(function () {
        wx.hideLoading()
        // request.js 已自动显示错误 toast，这里只关闭 loading
      })
  },

  // ── 重新授权接收推送（不消耗次数） ──
  handleReAuth: function () {
    try {
      wx.requestSubscribeMessage({
        tmplIds: [TEMPLATE_ID],
        success: function () {
          wx.showToast({ title: '授权成功', icon: 'success' })
        },
        fail: function () {
          wx.showToast({ title: '授权失败，请稍后再试', icon: 'none' })
        },
      })
    } catch (err) {
      wx.showToast({ title: '当前版本不支持', icon: 'none' })
    }
  },

  // ── 跳转今日菜单 ──
  goToMenu: function () {
    wx.navigateTo({ url: '/pages/menu/menu' })
  },
})
