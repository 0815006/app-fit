var request = require('../../utils/request.js')

Component({
  properties: {
    /** 外部控制是否展示 */
    visible: {
      type: Boolean,
      value: false
    },
    /** 是否在 attached 时自动检查 wx.getPrivacySetting */
    autoCheck: {
      type: Boolean,
      value: true
    }
  },

  data: {
    _needAuth: false  // 是否需要弹窗授权
  },

  lifetimes: {
    attached: function () {
      if (!this.properties.autoCheck) {
        return
      }
      var that = this
      // 延迟到下一帧，确保父组件事件绑定已完成
      wx.nextTick(function () {
        that._checkPrivacySetting()
      })
    }
  },

  observers: {
    'visible': function (newVal) {
      // 每次 visible 变为 true 时重新检查
      if (newVal && this.properties.autoCheck) {
        var that = this
        // 延迟到下一帧，确保父组件事件绑定已完成
        wx.nextTick(function () {
          that._checkPrivacySetting()
        })
      }
    }
  },

  methods: {
    /**
     * 检查隐私授权状态
     */
    _checkPrivacySetting: function () {
      var that = this

      // 基础库 2.32.3 以下不支持
      if (!wx.getPrivacySetting) {
        // 低版本直接视为已同意
        that.triggerEvent('agree')
        return
      }

      wx.getPrivacySetting({
        success: function (res) {
          if (res.needAuthorization) {
            // 需要授权，展示弹窗
            that.setData({ _needAuth: true })
          } else {
            // 已授权，直接通过
            that.setData({ _needAuth: false })
            that.triggerEvent('agree')
          }
        },
        fail: function () {
          // 检查失败也直接通过（降级处理）
          that.triggerEvent('agree')
        }
      })
    },

    /**
     * 用户同意隐私协议（由 open-type="agreePrivacyAuthorization" 触发）
     */
    handleAgree: function () {
      var that = this

      // 调后端记录同意时间（容错：失败不影响流程）
      request.post('/user/agree-privacy')
        .then(function () {
          console.log('[privacy-popup] 隐私同意记录成功')
        })
        .catch(function (err) {
          console.warn('[privacy-popup] 隐私同意记录失败（不阻塞）:', err)
        })

      that.setData({ _needAuth: false })
      that.triggerEvent('agree')
    },

    /**
     * 用户拒绝
     */
    handleRefuse: function () {
      this.setData({ _needAuth: false })
      this.triggerEvent('refuse')
    },

    /**
     * 跳转隐私政策全文页面
     */
    goToPrivacy: function () {
      wx.navigateTo({ url: '/pages/privacy/privacy' })
    }
  }
})
