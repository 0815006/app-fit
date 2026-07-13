Page({
  data: {
    showProfileModal: false
  },

  onLoad: function () {},

  // ── 资料完善弹窗（由 app.requestProfile() 驱动） ──
  showProfileModal: function () {
    this.setData({ showProfileModal: true })
  },

  onProfileClose: function () {
    this.setData({ showProfileModal: false })
  },

  onProfileSuccess: function () {
    this.setData({ showProfileModal: false })
  },
})
