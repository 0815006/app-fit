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

  _lastLoginSuccess: 0,

  onLaunch() {
    this.silentLogin()
  },

  /**
   * 热启动时检查登录态
   * - 本地无 token → 立即登录
   * - 上次登录超过 60 秒 → 重新登录（防止频繁 wx.login）
   */
  onShow: function () {
    var hasToken = wx.getStorageSync('satoken')
    var now = Date.now()
    if (!hasToken || now - this._lastLoginSuccess > 60000) {
      this.silentLogin()
    }
  },

  /**
   * 静默登录（返回 Promise，带重试机制）
   * wx.login → POST /api/auth/wx-login → 存储 satoken + userInfo
   * @param {number} _attempt 内部重试计数，外部调用无需传参
   */
  silentLogin: function (_attempt) {
    var that = this
    var attempt = _attempt || 0
    var MAX_RETRY = 2

    // 如果正在登录中，返回同一个 Promise（防止并发重复登录）
    if (that._loginPromise) return that._loginPromise

    that._loginPromise = new Promise(function (resolve, reject) {
      wx.login({
        success: function (res) {
          if (!res.code) {
            console.error('wx.login 失败：未获取到 code')
            that._loginPromise = null
            reject(new Error('wx.login 失败'))
            return
          }
          console.log('wx.login 成功, code:', res.code)

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
                that._lastLoginSuccess = Date.now()
                console.log('静默登录成功, isNewUser:', data.isNewUser)

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
              if (attempt < MAX_RETRY) {
                console.log('静默登录重试 ' + (attempt + 1) + '/' + MAX_RETRY)
                setTimeout(function () {
                  resolve(that.silentLogin(attempt + 1))
                }, 2000)
              } else {
                reject(err)
              }
            }
          })
        },
        fail: function (err) {
          console.error('wx.login 调用失败:', err)
          that._loginPromise = null
          if (attempt < MAX_RETRY) {
            console.log('静默登录重试 ' + (attempt + 1) + '/' + MAX_RETRY)
            setTimeout(function () {
              resolve(that.silentLogin(attempt + 1))
            }, 2000)
          } else {
            reject(err)
          }
        }
      })
    })
    return that._loginPromise
  },

  globalData: {
    userInfo: null
  }
})
