/**
 * 封装的 wx.request 拦截器
 * - 自动注入 satoken 请求头（Sa-Token 认证）
 * - 自动注入 X-Emp-No 请求头（兼容）
 * - 基础路径指向后端 /api（由 config.js 统一管理）
 * - 返回 Promise，自动解析 Result 结构
 * - 401 时自动重新登录并重试原请求（最多 1 次）
 */

var config = require('./config.js')
var BASE_URL = config.BASE_URL
var TOKEN_KEY = 'satoken'

function request(options, _retried) {
  return new Promise(function (resolve, reject) {
    var satoken = wx.getStorageSync(TOKEN_KEY) || ''
    var empNo = wx.getStorageSync('empNo') || '0000000'

    wx.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data,
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
