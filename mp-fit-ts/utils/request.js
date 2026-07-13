/**
 * 封装的 wx.request 拦截器
 * - 自动注入 satoken 请求头（Sa-Token 认证）
 * - 自动注入 X-Emp-No 请求头（兼容）
 * - 基础路径指向后端 /api（由 config.js 统一管理）
 * - 返回 Promise，自动解析 Result 结构
 * - 401 时自动重新登录并重试原请求（最多 1 次）
 * - 写操作（POST/PUT/DELETE）时若 isNewUser 则先触发资料完善弹窗
 */

var config = require('./config.js')
var BASE_URL = config.BASE_URL
var TOKEN_KEY = 'satoken'

/**
 * 无需完善资料即可调用的写接口白名单
 */
var PROFILE_SKIP_URLS = [
  '/security/check-text',
  '/user/update-profile',
  '/user/agree-privacy',
  '/upload/avatar'
]

function request(options, _retried) {
  var method = (options.method || 'GET').toUpperCase()
  var isWrite = method === 'POST' || method === 'PUT' || method === 'DELETE'

  // 写操作且非白名单路径：检查是否需要完善资料
  if (isWrite && !_retried) {
    var skipProfile = false
    for (var i = 0; i < PROFILE_SKIP_URLS.length; i++) {
      if (options.url.indexOf(PROFILE_SKIP_URLS[i]) === 0) {
        skipProfile = true
        break
      }
    }

    if (!skipProfile) {
      var isNewUser = wx.getStorageSync('isNewUser')
      if (isNewUser) {
        // 挂起请求，等待资料完善完成后再重试
        var app = getApp()
        if (app && app.requestProfile) {
          return app.requestProfile().then(function () {
            // 资料完善成功，重试原请求（标记已重试，不再检查 isNewUser）
            return request(options, true)
          })
        }
      }
    }
  }

  return new Promise(function (resolve, reject) {
    var satoken = wx.getStorageSync(TOKEN_KEY) || ''
    var empNo = wx.getStorageSync('empNo') || '0000000'

    wx.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data,
      timeout: 15000,  // 15 秒超时，避免长时间挂起
      header: Object.assign(
        {
          'Content-Type': 'application/json',
          'satoken': satoken,
          'X-Emp-No': empNo
        },
        options.header || {}
      ),
      success: function (res) {
        var result = res.data
        if (result.code === 401 && !_retried) {
          // Token 过期，清除旧 token
          wx.removeStorageSync(TOKEN_KEY)
          wx.removeStorageSync('isNewUser')

          var app = getApp()
          if (app && app.silentLogin) {
            // 等待重新登录完成后，自动重试原请求
            app.silentLogin().then(function () {
              request(options, true).then(resolve).catch(reject)
            }).catch(function (err) {
              console.error('重新登录失败:', err)
              wx.showToast({
                title: '登录失败，请稍后重试',
                icon: 'none',
              })
              reject(new Error('登录失败'))
            })
          } else {
            wx.showToast({
              title: '登录已过期，请重新打开小程序',
              icon: 'none',
            })
            reject(new Error('登录已过期'))
          }
          return
        }
        if (result.code === 200) {
          resolve(result)
        } else {
          wx.showToast({
            title: result.message || '请求失败',
            icon: 'none',
          })
          reject(new Error(result.message || '请求失败'))
        }
      },
      fail: function (err) {
        wx.showToast({
          title: '网络异常，请稍后重试',
          icon: 'none',
        })
        reject(err)
      },
    })
  })
}

function post(url, data) {
  return request({ url: url, method: 'POST', data: data })
}

function get(url, data) {
  return request({ url: url, method: 'GET', data: data })
}

function put(url, data) {
  return request({ url: url, method: 'PUT', data: data })
}

function del(url, data) {
  return request({ url: url, method: 'DELETE', data: data })
}

module.exports = {
  request: request,
  post: post,
  get: get,
  put: put,
  del: del,
  _BASE_URL: BASE_URL
}
