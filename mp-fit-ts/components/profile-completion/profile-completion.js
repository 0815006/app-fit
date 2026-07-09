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
    nickname: ''
  },

  methods: {
    /**
     * 关闭弹窗
     */
    onClose() {
      this.setData({ avatarUrl: '', nickname: '' })
      this.triggerEvent('close')
    },

    /**
     * 微信头像选择回调
     */
    onChooseAvatar(e) {
      var avatarUrl = e.detail.avatarUrl
      this.setData({ avatarUrl: avatarUrl })
    },

    /**
     * 昵称输入
     */
    onNameInput(e) {
      this.setData({ nickname: e.detail.value })
    },

    /**
     * 提交资料
     */
    submitProfile() {
      var that = this
      var nickname = this.data.nickname

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
          that.setData({ avatarUrl: '', nickname: '' })
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
