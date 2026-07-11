var request = require('./utils/request.js')

App({
  onLaunch() {
    this.silentLogin()
  },

  /**
   * 静默登录
   * wx.login → POST /api/auth/wx-login → 存储 satoken + userInfo
   */
  silentLogin() {
    var that = this
    // 防止重复调用：如果正在登录中则跳过
    if (that._loginPending) return
    that._loginPending = true

    wx.login({
      success(res) {
        if (!res.code) {
          console.error('wx.login 失败：未获取到 code')
          that._loginPending = false
          return
        }
        console.log('wx.login 成功, code:', res.code)

        request.post('/auth/wx-login', { code: res.code })
          .then(function (result) {
            var data = result.data
            wx.setStorageSync('satoken', data.token)
            wx.setStorageSync('isNewUser', data.isNewUser)
            wx.setStorageSync('userInfo', data.userInfo)
            console.log('静默登录成功, isNewUser:', data.isNewUser)

            // 新用户标记资料完善弹窗
            if (data.isNewUser) {
              wx.setStorageSync('showProfileModal', true)
            }

            // 通知所有已加载的页面：登录已完成
            var pages = getCurrentPages()
            for (var i = 0; i < pages.length; i++) {
              var page = pages[i]
              if (typeof page.onLoginReady === 'function') {
                page.onLoginReady()
              }
            }
          })
          .catch(function (err) {
            console.error('静默登录失败:', err)
          })
          .finally(function () {
            that._loginPending = false
          })
      },
      fail(err) {
        console.error('wx.login 调用失败:', err)
        that._loginPending = false
      }
    })
  },

  /**
   * 检查用户状态
   * 新用户 → 弹出资料完善弹窗；老用户 → 直接执行 callback
   * @param {Function} callback 校验通过后执行的回调
   */
  checkUserStatus(callback) {
    var isNewUser = wx.getStorageSync('isNewUser')
    if (isNewUser) {
      // 触发全局事件，让页面展示资料完善弹窗
      wx.setStorageSync('showProfileModal', true)
      // 页面监听 storage 变化来展示弹窗
    } else if (typeof callback === 'function') {
      callback()
    }
  },

  globalData: {
    userInfo: null
  }
})
