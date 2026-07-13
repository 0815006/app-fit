var request = require('../../utils/request.js')
var config = require('../../utils/config.js')

Component({
  properties: {
    visible: {
      type: Boolean,
      value: false
    }
  },

  data: {
    avatarUrl: '',
    nickname: '',
    privacyAgreed: false,   // 是否已同意隐私协议
    _avatarError: false     // 头像加载失败降级
  },

  methods: {
    /**
     * 关闭弹窗
     */
    onClose() {
      this.setData({ avatarUrl: '', nickname: '', privacyAgreed: false })
      // 通知 app 层资料完善取消，拒绝所有挂起的请求
      var app = getApp()
      if (app && app.onProfileCancel) {
        app.onProfileCancel()
      }
      this.triggerEvent('close')
    },

    /**
     * 选择头像（wx.chooseMedia，兼容 Windows 模拟器和真机）
     */
    onChooseAvatar() {
      var that = this
      wx.chooseMedia({
        count: 1,
        mediaType: ['image'],
        sourceType: ['album', 'camera'],
        success: function (res) {
          var tempFilePath = res.tempFiles[0].tempFilePath
          if (tempFilePath) {
            that.setData({ avatarUrl: tempFilePath, _avatarError: false })
          }
        },
        fail: function (err) {
          console.warn('[profile-completion] 选择头像取消或失败:', err)
        }
      })
    },

    /**
     * 头像加载失败时降级显示占位符
     */
    onAvatarError: function () {
      this.setData({ _avatarError: true })
    },

    /**
     * 昵称输入
     */
    onNameInput(e) {
      this.setData({ nickname: e.detail.value })
    },

    /**
     * 隐私协议同意回调（由 open-type="agreePrivacyAuthorization" 触发）
     */
    handlePrivacyAgree: function () {
      var that = this

      // 调后端记录同意时间（容错：失败不影响流程）
      request.post('/user/agree-privacy')
        .then(function () {
          console.log('[profile-completion] 隐私同意记录成功')
        })
        .catch(function (err) {
          console.warn('[profile-completion] 隐私同意记录失败（不阻塞）:', err)
        })

      that.setData({ privacyAgreed: true })
    },

    /**
     * 跳转隐私政策全文页面
     */
    goToPrivacy: function () {
      wx.navigateTo({ url: '/pages/privacy/privacy' })
    },

    /**
     * 提交资料
     */
    submitProfile() {
      var that = this
      var nickname = this.data.nickname

      if (!this.data.privacyAgreed) {
        wx.showToast({ title: '请先阅读并同意隐私保护指引', icon: 'none' })
        return
      }

      if (!nickname || nickname.trim() === '') {
        wx.showToast({ title: '请输入名字', icon: 'none' })
        return
      }

      // 内容安全检测：先检测昵称文本
      wx.showLoading({ title: '安全检测中...' })
      request.post('/security/check-text', { content: nickname.trim() })
        .then(function (checkRes) {
          wx.hideLoading()
          var pass = checkRes.data && checkRes.data.pass
          if (!pass) {
            wx.showToast({ title: '内容含违规信息，请修改后重试', icon: 'none' })
            return
          }
          // 检测通过，继续提交
          that._doSubmit()
        })
        .catch(function (err) {
          wx.hideLoading()
          wx.showToast({ title: err.message || '安全检测失败', icon: 'none' })
        })
    },

    /**
     * 执行实际提交（安全检测通过后）
     */
    _doSubmit: function () {
      var that = this
      var nickname = this.data.nickname

      wx.showLoading({ title: '保存中...' })

      // 先上传头像（如果有选），再更新资料
      var doUpdate = function (avatarUrl) {
        request.post('/user/update-profile', {
          nickname: nickname.trim(),
          avatarUrl: avatarUrl || ''
        }).then(function () {
          wx.hideLoading()
          wx.showToast({ title: '资料完善成功', icon: 'success' })
          // 通知 app 层资料完善完成，重试所有挂起的请求
          var app = getApp()
          if (app && app.onProfileDone) {
            app.onProfileDone()
          }
          that.setData({ avatarUrl: '', nickname: '', privacyAgreed: false })
          that.triggerEvent('success')
        }).catch(function (err) {
          wx.hideLoading()
          wx.showToast({ title: err.message || '保存失败', icon: 'none' })
        })
      }

      var avatarUrl = this.data.avatarUrl
      if (avatarUrl) {
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
              // 上传失败，仍提交资料（无头像）
              doUpdate('')
            }
          },
          fail: function () {
            // 上传失败，仍提交资料（无头像）
            doUpdate('')
          }
        })
      } else {
        doUpdate('')
      }
    }
  }
})
