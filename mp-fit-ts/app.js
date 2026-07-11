var request = require('./utils/request.js')

App({
  onLaunch() {
    this.silentLogin()
  },

  /**
   * 静默登录（返回 Promise，支持外部 await）
   * wx.login → POST /api/auth/wx-login → 存储 satoken + userInfo
   */
  silentLogin() {
    var that = this
    // 如果正在登录中，返回同一个 Promise（防止并发重复登录）
    if (that._loginPromise) return that._loginPromise

    that._loginPromise = new Promise(function (resolve, reject) {
      wx.login({
        success(res) {
          if (!res.code) {
            console.error('wx.login 失败：未获取到 code')
            that._loginPromise = null
            reject(new Error('wx.login 失败'))
            return
          }
          console.log('wx.login 成功, code:', res.code)

          // 直接用 wx.request 避免循环依赖（request.js 里 401 会调用 silentLogin）
          var satoken = wx.getStorageSync('satoken') || ''
          wx.request({
            url: request._BASE_URL + '/auth/wx-login',
            method: 'POST',
            data: { code: res.code },
            header: {
              'Content-Type': 'application/json',
              'satoken': satoken
            },
            success: function (wxRes) {
              var result = wxRes.data
              if (result && result.data) {
                var data = result.data
                wx.setStorageSync('satoken', data.token)
                wx.setStorageSync('isNewUser', data.isNewUser)
                wx.setStorageSync('userInfo', data.userInfo)
                console.log('静默登录成功, isNewUser:', data.isNewUser)

                if (data.isNewUser) {
                  wx.setStorageSync('showProfileModal', true)
                }

                var pages = getCurrentPages()
                for (var i = 0; i < pages.length; i++) {
                  var page = pages[i]
                  if (typeof page.onLoginReady === 'function') {
                    page.onLoginReady()
                  }
                }
                that._loginPromise = null
                resolve()
              } else {
                that._loginPromise = null
                reject(new Error('登录响应异常'))
              }
            },
            fail: function (err) {
              console.error('静默登录请求失败:', err)
              that._loginPromise = null
              reject(err)
            }
          })
        },
        fail(err) {
          console.error('wx.login 调用失败:', err)
          that._loginPromise = null
          reject(err)
        }
      })
    })
    return that._loginPromise
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
