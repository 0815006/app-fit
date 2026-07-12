Page({
  data: {},

  /**
   * 分享给好友 — 根据当前榜单Tab动态生成标题
   */
  onShareAppMessage: function () {
    var board = this.selectComponent('#rankingBoard')
    var title = '🏋️ 健身榜单'
    if (board) {
      var tab = board.data.activeTab || 'consistency'
      var mode = board.data.consistencyMode || 'cumulative'
      var liftMap = { bench: '卧推', squat: '深蹲', deadlift: '硬拉', total: '三大项' }
      var lift = board.data.selectedLift || 'bench'
      var tabLabelMap = { consistency: '坚持榜', volume: '容量榜', peak1rm: '1RM巅峰榜', progress: '进步榜' }
      var tabLabel = tabLabelMap[tab] || '健身榜单'
      if (tab === 'consistency') {
        title = '📅 健身' + tabLabel + ' · ' + (mode === 'streak' ? '连续打卡' : '累计打卡')
      } else {
        title = '💪 健身' + tabLabel + ' · ' + (liftMap[lift] || lift)
      }
    }
    return {
      title: title,
      path: '/pages/workout/ranking/ranking'
    }
  },

  /**
   * 分享到朋友圈
   */
  onShareTimeline: function () {
    return {
      title: '🏋️ 健身榜单 · 坚持榜 · 容量榜 · 1RM巅峰榜 · 进步榜'
    }
  }
})
