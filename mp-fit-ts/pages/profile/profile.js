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
        // /uploads/avatar/xxx.png -> https://realapex.site/uploads/avatar/xxx.png
        var base = config.BASE_URL.replace(/\/api\/?$/, '')
        var fullUrl = base + url
        console.log('[profile] _resolveAvatarUrl:', url, '->', fullUrl)
        return fullUrl
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
            nickname: user.nickname || '',
            empNo: empNo,
            empName: empName,
            loading: false
          })

          // 头像通过 downloadFile 下载到本地临时文件后显示
          // 这样能明确诊断 downloadFile 域名白名单问题
          that._loadAndDisplayAvatar(user.avatarUrl)
        })
        .catch(function () {
          wx.showToast({ title: '获取用户信息失败', icon: 'none' })
          that.setData({ loading: false })
        })
    },

    /**
     * 下载头像到本地临时文件再显示
     * 微信小程序 <image> 加载网络图片底层走 downloadFile，
     * 若域名未加入 downloadFile 白名单则静默失败。
     * 直接用 wx.downloadFile 可以拿到明确的失败信息。
     */
    _loadAndDisplayAvatar: function (avatarUrl) {
      var that = this
      if (!avatarUrl) {
        that.setData({ avatarUrl: '', _avatarError: false })
        return
      }

      // 如果当前 avatarUrl 已经是临时文件路径（刚选完还没保存），保持不变
      if (avatarUrl.indexOf('wxfile://') === 0 || avatarUrl.indexOf('http://tmp/') === 0) {
        that.setData({ avatarUrl: avatarUrl, _avatarError: false })
        return
      }

      var fullUrl = that._resolveAvatarUrl(avatarUrl)
      console.log('[profile] 下载头像:', fullUrl)

      wx.downloadFile({
        url: fullUrl,
        success: function (res) {
          if (res.statusCode === 200) {
            console.log('[profile] 头像下载成功, tempFilePath:', res.tempFilePath)
            that.setData({ avatarUrl: res.tempFilePath, _avatarError: false })
          } else {
            console.error('[profile] 头像下载 HTTP', res.statusCode, ', 降级使用远程 URL')
            that.setData({ avatarUrl: fullUrl, _avatarError: false })
          }
        },
        fail: function (err) {
          console.error('[profile] 头像下载失败（请检查 downloadFile 域名白名单是否包含', config.BASE_URL, ')', err)
          // 降级：直接使用远程 URL（开发者工具勾选"不校验域名"时可用）
          that.setData({ avatarUrl: fullUrl, _avatarError: false })
        }
      })
    },

    /**
     * 使用微信头像（button open-type="chooseAvatar" 回调）
     */
    onWechatAvatar: function (e) {
      var avatarUrl = e.detail.avatarUrl
      if (avatarUrl) {
        this.setData({ avatarUrl: avatarUrl, _avatarError: false })
      }
    },

    /**
     * 头像加载失败时降级显示占位符
     */
    onAvatarError: function () {
      console.warn('[profile] 头像图片加载失败，当前 avatarUrl:', this.data.avatarUrl)
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
