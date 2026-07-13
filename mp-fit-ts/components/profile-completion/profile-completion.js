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
    privacyAgreed: false   // 是否已同意隐私协议
  },

  methods: {
    /**
     * 关闭弹窗
     */
    onClose() {
      this.setData({ avatarUrl: '', nickname: '', privacyAgreed: false })
      this.triggerEvent('close')
    },

    /**
     * 选择头像（使用 wx.chooseMedia，不依赖隐私 scope）
     */
    onChooseAvatar() {
      var that = this
      wx.chooseMedia({
        count: 1,
        mediaType: ['image'],
        sourceType: ['album', 'camera'],
        success: function (res) {
          var tempFilePath = res.tempFiles[0].tempFilePath
          that.setData({ avatarUrl: tempFilePath })
        },
        fail: function () {
          // 用户取消选择
        }
      })
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

      wx.showLoading({ title: '保存中...' })

      // 先上传头像（如果有选），再更新资料
      var doUpdate = function (avatarUrl) {
        request.post('/user/update-profile', {
          nickname: nickname.trim(),
          avatarUrl: avatarUrl || ''
        }).then(function () {
          wx.hideLoading()
          wx.showToast({ title: '资料完善成功', icon: 'success' })
          wx.setStorageSync('isNewUser', false)
          wx.removeStorageSync('showProfileModal')
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
