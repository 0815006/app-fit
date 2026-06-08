/**
 * 封装的 wx.request 拦截器
 * - 自动注入 X-Emp-No 请求头
 * - 基础路径指向后端 /api
 * - 返回 Promise，自动解析 Result 结构
 */

// var BASE_URL = 'http://localhost:8082/api'
// 切换为线上生产环境域名，专走 443 端口
const BASE_URL = 'https://realapex.site/api';

function request(options) {
  return new Promise(function (resolve, reject) {
    var empNo = wx.getStorageSync('empNo') || '0000000'

    wx.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data,
      header: Object.assign(
        { 'Content-Type': 'application/json', 'X-Emp-No': empNo },
        options.header || {}
      ),
      success: function (res) {
        var result = res.data
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

module.exports = { request: request, post: post, get: get }
