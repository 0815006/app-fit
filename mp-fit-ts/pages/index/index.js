var api = require('../../utils/request')

Page({
  data: {
    empNo: '0000000',
    loginCount: null,
    loading: false,
    activeTab: 'stats',
    tabBars: [
      { key: 'stats', label: '登录统计', icon: '📊' },
      { key: 'tech', label: '技术选型', icon: '🛠️' },
    ],
    techStack: [
      { category: '后端框架', name: 'Spring Boot', version: '3.4+' },
      { category: '后端语言', name: 'Java', version: '21' },
      { category: 'ORM', name: 'MyBatis Plus', version: '3.5.11' },
      { category: '数据库', name: 'MySQL', version: '8.4 LTS' },
      { category: '数据库迁移', name: 'Flyway', version: 'Latest' },
      { category: 'Web框架', name: 'Vue', version: '3.5+' },
      { category: '构建工具', name: 'Vite', version: '6.x' },
      { category: 'Web语言', name: 'TypeScript', version: '5.7+' },
      { category: 'Web UI库', name: 'Element Plus', version: '2.9+' },
      { category: '小程序框架', name: '微信原生', version: 'Latest' },
      { category: '小程序语言', name: 'JavaScript', version: 'ES6+' },
      { category: '小程序 UI库', name: 'TDesign Miniprogram', version: 'Latest' },
    ],
  },

  onLoad: function () {
    var empNo = wx.getStorageSync('empNo') || '0000000'
    this.setData({ empNo: empNo })
    this.loadLoginData()
  },

  onShow: function () {
    this.loadLoginData()
  },

  switchTab: function (e) {
    var key = e.currentTarget.dataset.key
    this.setData({ activeTab: key })
    if (key === 'stats') {
      this.loadLoginData()
    }
  },

  loadLoginData: function () {
    var that = this
    var empNo = wx.getStorageSync('empNo') || '0000000'
    that.setData({ loading: true, empNo: empNo })

    api
      .post('/login-record', { loginType: 'MINI_PROGRAM' })
      .then(function () {
        return api.get('/login-record/count/' + empNo)
      })
      .then(function (result) {
        that.setData({ loginCount: result.data })
      })
      .catch(function () {
        that.setData({ loginCount: null })
      })
      .finally(function () {
        that.setData({ loading: false })
      })
  },
})
