var request = require('./utils/request.js')

App({
  /**
   * 全局资料完善请求管理
   * - _profileCallbacks: 待重试的请求队列 [{ resolve, reject }]
   * - requestProfile(): 返回 Promise，挂起当前操作直到资料完善完成
   * - onProfileDone(): 资料完善成功 → 重试所有挂起的请求
   * - onProfileCancel(): 资料完善取消/关闭 → 拒绝所有挂起的请求
   */
  _profileCallbacks: [],

  /**
   * 请求完善资料（挂起当前操作）
   * @returns {Promise<void>} 资料完成后 resolve，取消后 reject
   */
  requestProfile: function () {
    var app = this
    return new Promise(function (resolve, reject) {
      app._profileCallbacks.push({ resolve: resolve, reject: reject })

      // 通知当前页面展示资料完善弹窗
      var pages = getCurrentPages()
      var currentPage = pages[pages.length - 1]
      if (currentPage && typeof currentPage.showProfileModal === 'function') {
        currentPage.showProfileModal()
      }
    })
  },

  /**
   * 资料完善成功回调
   * 清除 isNewUser 标记，重试所有挂起的请求
   */
  onProfileDone: function () {
    wx.setStorageSync('isNewUser', false)
    var callbacks = this._profileCallbacks.splice(0)
    for (var i = 0; i < callbacks.length; i++) {
      callbacks[i].resolve()
    }
  },

  /**
   * 资料完善取消回调
   * 拒绝所有挂起的请求
   */
  onProfileCancel: function () {
    var callbacks = this._profileCallbacks.splice(0)
    for (var i = 0; i < callbacks.length; i++) {
      callbacks[i].reject(new Error('请先完善个人资料'))
    }
  },

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
            timeout: 15000,
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

                // 不再强制弹出资料完善弹窗，改为按需触发（request.js 拦截写操作时）

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

  globalData: {
    userInfo: null
  }
})
