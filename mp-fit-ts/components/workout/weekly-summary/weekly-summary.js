var api = require('../../../utils/request')

var WEEKDAY_NAMES = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

Component({
  data: {
    /** 本周训练记录列表 */
    records: [],
    /** 加载中 */
    loading: false,
    /** 按日期分组后的数据 */
    groupedRecords: [],
    /** 本周统计 */
    totalDays: 0,
    totalActions: 0
  },

  lifetimes: {
    attached: function () {
      this.loadWeeklySummary()
    }
  },

  methods: {
    /** 加载本周训练摘要 */
    loadWeeklySummary: function () {
      var that = this
      that.setData({ loading: true })

      api.get('/gym-workout/weekly-summary')
        .then(function (res) {
          var records = res.data || []
          var grouped = that._groupByDay(records)
          var totalDays = grouped.length
          var totalActions = records.length

          that.setData({
            records: records,
            groupedRecords: grouped,
            totalDays: totalDays,
            totalActions: totalActions
          })
        })
        .catch(function () {
          that.setData({ records: [], groupedRecords: [], totalDays: 0, totalActions: 0 })
        })
        .finally(function () {
          that.setData({ loading: false })
        })
    },

    /** 按 dayOfWeek 分组 */
    _groupByDay: function (records) {
      var dayMap = {}
      for (var i = 0; i < records.length; i++) {
        var rec = records[i]
        var day = rec.dayOfWeek != null ? rec.dayOfWeek : 1
        if (!dayMap[day]) {
          dayMap[day] = []
        }
        dayMap[day].push(rec)
      }

      var result = []
      for (var d = 1; d <= 7; d++) {
        if (dayMap[d]) {
          result.push({
            dayOfWeek: d,
            label: WEEKDAY_NAMES[d - 1] || ('周' + d),
            items: dayMap[d]
          })
        }
      }
      return result
    },

    /** 格式化重量 × 次数 */
    formatWeightReps: function (item) {
      var parts = []
      if (item.weight != null && item.weight > 0) {
        parts.push(item.weight + 'kg')
      }
      if (item.reps != null && item.reps > 0) {
        parts.push('×' + item.reps)
      }
      if (item.setCount != null && item.setCount > 0) {
        parts.push(item.setCount + '组')
      }
      return parts.join(' ')
    },

    /** 获取PR标签 */
    getPrLabel: function (isPr) {
      return isPr ? '🏆PR' : ''
    },

    /** 获取力竭度标签 */
    getExhaustionLabel: function (score) {
      if (!score || score <= 0) return ''
      if (score >= 1.1) return '💥'
      if (score >= 0.9) return '🔥'
      return ''
    }
  }
})
