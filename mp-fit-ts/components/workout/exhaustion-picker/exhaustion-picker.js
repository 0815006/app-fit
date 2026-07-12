/**
 * 力竭度评价底部弹窗组件
 * 参照 Web端 ExhaustionDialog：滑块 50~120，标记浅练/适中/力竭/超负荷
 */
Component({
  properties: {
    /** 控制显示/隐藏 */
    visible: {
      type: Boolean,
      value: false
    }
  },

  data: {
    score: 100,
    scoreDisplay: '1.00'
  },

  observers: {
    'visible': function (visible) {
      if (visible) {
        this.setData({ score: 100, scoreDisplay: '1.00' })
      }
    }
  },

  methods: {
    /** 滑块值变化 */
    handleSliderChange: function (e) {
      var val = e.detail.value
      this.setData({
        score: val,
        scoreDisplay: (val / 100).toFixed(2)
      })
    },

    /** 确认提交 */
    handleConfirm: function () {
      this.triggerEvent('submit', { score: this.data.score / 100 })
    },

    /** 关闭取消 */
    handleClose: function () {
      this.triggerEvent('close')
    },

    /** 阻止冒泡 */
    noop: function () {}
  }
})
