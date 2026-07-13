var request = require('../../utils/request.js')
var config = require('../../utils/config.js')

Page({
  data: {
    loading: true,
    user: null,
    avatarUrl: '',
    nickname: '',
    empNo: '',
    empName: '',
    saving: false,

    // 隐私弹窗相关
    privacyPassed: false,
    showPrivacyPopup: true
  },

  onLoad: function () {
    // 先不加载数据，等待 privacy-popup 组件的 agree/refuse 事件
  },

  onShow: function () {
    if (this.data.privacyPassed) {
      this.loadUser()
    }
  },

  /**
   * 隐私协议已同意（包括历史已同意无需弹窗的情况）
   */
  onPrivacyAgreed: function () {
    this.setData({ privacyPassed: true, showPrivacyPopup: false })
    this.loadUser()
  },

  /**
   * 用户拒绝了隐私协议
   */
  onPrivacyRefused: function () {
    this.setData({ privacyPassed: false, showPrivacyPopup: false })
    wx.showToast({ title: '需要同意隐私协议才能使用', icon: 'none' })
  },

  /**
   * 重新唤起隐私弹窗
   */
  retryPrivacy: function () {
    this.setData({ showPrivacyPopup: true })
  },

  loadUser: function () {
    var that = this
    that.setData({ loading: true })

    request.get('/user/current')
      .then(function (res) {
        var user = res.data
        // "0000000" 视为未维护，显示为空
        var empNo = (user.empNo && user.empNo !== '0000000') ? user.empNo : ''
        var empName = user.empName || ''
        that.setData({
          user: user,
          avatarUrl: user.avatarUrl || '',
          nickname: user.nickname || '',
          empNo: empNo,
          empName: empName,
          loading: false
        })
      })
      .catch(function () {
        wx.showToast({ title: '获取用户信息失败', icon: 'none' })
        that.setData({ loading: false })
      })
  },

  /**
   * 选择头像（使用 wx.chooseMedia，不依赖隐私 scope）
   */
  onChooseAvatar: function () {
    var that = this
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success: function (res) {
        var tempFilePath = res.tempFiles[0].tempFilePath
        that.setData({ avatarUrl: tempFilePath })
      }
    })
  },

  /**
   * 昵称输入
   */
  onNicknameInput: function (e) {
    this.setData({ nickname: e.detail.value })
  },

  /**
   * 工号输入
   */
  onEmpNoInput: function (e) {
    // 工号输入限定7位数字
    var value = e.detail.value.replace(/[^0-9]/g, '').slice(0, 7)
    this.setData({ empNo: value })
  },

  /**
   * 员工姓名输入
   */
  onEmpNameInput: function (e) {
    this.setData({ empName: e.detail.value })
  },

  /**
   * 保存资料
   */
  saveProfile: function () {
    var that = this
    var nickname = this.data.nickname

    if (!nickname || nickname.trim() === '') {
      wx.showToast({ title: '请输入昵称', icon: 'none' })
      return
    }

    that.setData({ saving: true })
    wx.showLoading({ title: '保存中...' })

    var doUpdate = function (avatarUrl) {
      var payload = {
        nickname: nickname.trim(),
        avatarUrl: avatarUrl || that.data.user.avatarUrl || '',
      }
      // 只在有非空工号时传递
      if (that.data.empNo && that.data.empNo !== '0000000') {
        payload.empNo = that.data.empNo
      }
      // 员工姓名
      if (that.data.empName && that.data.empName.trim() !== '') {
        payload.empName = that.data.empName.trim()
      }

      request.post('/user/update-profile', payload)
        .then(function () {
          wx.hideLoading()
          wx.showToast({ title: '保存成功', icon: 'success' })
          that.setData({ saving: false })
          // 刷新数据
          that.loadUser()
        })
        .catch(function (err) {
          wx.hideLoading()
          that.setData({ saving: false })
          wx.showToast({ title: err.message || '保存失败', icon: 'none' })
        })
    }

    var avatarUrl = this.data.avatarUrl
    // 如果头像变更了（不是后端已有的URL），需要先上传
    var originalAvatar = this.data.user ? (this.data.user.avatarUrl || '') : ''
    if (avatarUrl && avatarUrl !== originalAvatar && avatarUrl.indexOf('http') !== 0) {
      wx.uploadFile({
        url: config.BASE_URL + '/upload/avatar',
        filePath: avatarUrl,
        name: 'file',
        header: {
          'satoken': wx.getStorageSync('satoken') || ''
        },
        success: function (uploadRes) {
          var result = JSON.parse(uploadRes.data)
          if (result.code === 200 && result.data && result.data.url) {
            doUpdate(result.data.url)
          } else {
            doUpdate('')
          }
        },
        fail: function () {
          doUpdate('')
        }
      })
    } else {
      doUpdate(avatarUrl || originalAvatar)
    }
  },

  /**
   * 返回上一页
   */
  goBack: function () {
    wx.navigateBack()
  },

})
