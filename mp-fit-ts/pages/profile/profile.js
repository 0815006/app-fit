var request = require('../../utils/request.js')
var config = require('../../utils/config.js')

Component({
  // 页面生命周期（Component 作为页面时，必须在顶层，不在 methods 中）
  pageLifetimes: {},
  onLoad: function () {
    // 先不加载数据，等待 privacy-popup 组件的 agree/refuse 事件
  },

  onShow: function () {
    if (this.data.privacyPassed) {
      this.loadUser()
    }
  },

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

  methods: {
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

    /**
     * 将服务端相对路径转为可访问的完整 URL
     */
    _resolveAvatarUrl: function (url) {
      if (!url) return ''
      if (url.indexOf('/uploads/') === 0) {
        // /uploads/avatar/xxx.png -> http://localhost:8091/uploads/avatar/xxx.png
        var base = config.BASE_URL.replace(/\/api\/?$/, '')
        return base + url
      }
      return url
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
            avatarUrl: that._resolveAvatarUrl(user.avatarUrl || ''),
            nickname: user.nickname || '',
            empNo: empNo,
            empName: empName,
            _avatarError: false,
            loading: false
          })
        })
        .catch(function () {
          wx.showToast({ title: '获取用户信息失败', icon: 'none' })
          that.setData({ loading: false })
        })
    },

    /**
     * 选择头像（wx.chooseMedia，兼容 Windows 模拟器和真机）
     */
    onChooseAvatar: function () {
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
          console.warn('[profile] 选择头像取消或失败:', err)
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

      // 内容安全检测：先检测昵称文本
      request.post('/security/check-text', { content: nickname.trim() })
        .then(function (checkRes) {
          var pass = checkRes.data && checkRes.data.pass
          if (!pass) {
            that.setData({ saving: false })
            wx.showToast({ title: '内容含违规信息，请修改后重试', icon: 'none' })
            return
          }
          // 检测通过，继续提交
          that._doSaveProfile()
        })
        .catch(function (err) {
          that.setData({ saving: false })
          wx.showToast({ title: err.message || '安全检测失败', icon: 'none' })
        })
    },

    /**
     * 执行实际保存（安全检测通过后）
     */
    _doSaveProfile: function () {
      var that = this
      var nickname = this.data.nickname

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
            wx.showToast({ title: '保存成功', icon: 'success' })
            that.setData({ saving: false })
            // 刷新数据
            that.loadUser()
          })
          .catch(function (err) {
            that.setData({ saving: false })
            wx.showToast({ title: err.message || '保存失败', icon: 'none' })
          })
      }

      var avatarUrl = this.data.avatarUrl
      // 如果头像变更了且不是已上传到服务器的 URL，需要先上传
      var originalAvatar = this.data.user ? (this.data.user.avatarUrl || '') : ''
      var isServerUrl = avatarUrl && avatarUrl.indexOf('/uploads/') === 0
      if (avatarUrl && avatarUrl !== originalAvatar && !isServerUrl) {
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
    }
  }
})
