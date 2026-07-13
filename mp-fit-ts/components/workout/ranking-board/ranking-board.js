var api = require('../../../utils/request')
var config = require('../../../utils/config')

Component({
  data: {
    /** 当前激活的榜单Tab */
    activeTab: 'consistency',
    /** 坚持榜子模式：cumulative 累计 / streak 连续 */
    consistencyMode: 'cumulative',
    /** 统计周期天数 */
    days: 30,
    /** 榜单数据 */
    rankingData: [],
    /** 加载中 */
    loading: false,

    /** Tab 列表 */
    tabs: [
      { key: 'consistency', label: '坚持榜', icon: '📅', needLift: false },
      { key: 'volume', label: '容量榜', icon: '💪', needLift: true },
      { key: 'peak1rm', label: '1RM巅峰', icon: '🏆', needLift: true },
      { key: 'progress', label: '进步榜', icon: '📈', needLift: true }
    ],

    /** 当前选中的动作（后3榜用） */
    selectedLift: 'bench',

    /** 动作选项 */
    liftOptions: [
      { key: 'bench', label: '卧推' },
      { key: 'squat', label: '深蹲' },
      { key: 'deadlift', label: '硬拉' },
      { key: 'total', label: '三大项' }
    ]
  },

  lifetimes: {
    attached: function () {
      this.loadRanking()
    }
  },

  methods: {
    /** 切换 Tab */
    handleTabChange: function (e) {
      var tab = e.currentTarget.dataset.tab
      if (tab === this.data.activeTab) return
      this.setData({ activeTab: tab })
      this.loadRanking()
    },

    /** 切换坚持榜子模式（累计 / 连续） */
    handleConsistencyModeChange: function (e) {
      var mode = e.currentTarget.dataset.mode
      if (mode === this.data.consistencyMode) return
      this.setData({ consistencyMode: mode })
      this.loadRanking()
    },

    /** 切换动作（后3榜） */
    handleLiftChange: function (e) {
      var lift = e.currentTarget.dataset.lift
      if (lift === this.data.selectedLift) return
      this.setData({ selectedLift: lift })
      this.loadRanking()
    },

    /** 加载榜单数据 */
    loadRanking: function () {
      var that = this
      that.setData({ loading: true })

      var url = ''
      var lift = that.data.selectedLift
      switch (that.data.activeTab) {
        case 'consistency':
          url = '/training-stats/ranking/consistency-v2?days=' + that.data.days + '&mode=' + that.data.consistencyMode
          break
        case 'volume':
          url = '/training-stats/ranking/max-single-volume?days=' + that.data.days + '&lift=' + lift
          break
        case 'peak1rm':
          url = '/training-stats/ranking/peak-1rm?days=' + that.data.days + '&lift=' + lift
          break
        case 'progress':
          url = '/training-stats/ranking/progress-v2?days=' + that.data.days + '&lift=' + lift
          break
      }

      api.get(url)
        .then(function (res) {
          var data = res.data || []
          that._resolveAvatarUrls(data)
          that.setData({ rankingData: data })
        })
        .catch(function () {
          that.setData({ rankingData: [] })
        })
        .finally(function () {
          that.setData({ loading: false })
        })
    },

    /** 将相对路径头像 URL 解析为完整 URL */
    _resolveAvatarUrls: function (list) {
      if (!list || list.length === 0) return
      var base = config.BASE_URL.replace(/\/api\/?$/, '')
      for (var i = 0; i < list.length; i++) {
        var item = list[i]
        if (item.avatarUrl && item.avatarUrl.indexOf('/uploads/') === 0) {
          item.avatarUrl = base + item.avatarUrl
        }
      }
    },

    /** 获取排名勋章 */
    getMedal: function (index) {
      if (index === 0) return '🥇'
      if (index === 1) return '🥈'
      if (index === 2) return '🥉'
      return String(index + 1)
    },

    /** 获取排名行样式 */
    getRankClass: function (index) {
      if (index < 3) return 'rank-top'
      return ''
    },

    /** 格式化辅助值 */
    formatAuxValue: function (item) {
      if (item.auxiliaryValue == null || item.auxiliaryValue === undefined) return ''
      return String(item.auxiliaryValue)
    },

    /** 趋势图标 */
    getTrendIcon: function (trend) {
      if (trend === 'up') return '📈'
      if (trend === 'down') return '📉'
      if (trend === 'new') return '🆕'
      return ''
    }
  }
})
