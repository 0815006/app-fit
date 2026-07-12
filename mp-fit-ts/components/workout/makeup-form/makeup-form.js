var api = require('../../../utils/request')

Component({
  properties: {},
  data: {
    /** 补卡日期 */
    makeupDate: '',
    /** 动作列表 */
    actions: [],
    /** 加载中 */
    loading: false,
    /** 提交中 */
    submitting: false,
    /** 记录列表（含输入状态） */
    recordList: [
      {
        id: 1,
        actionId: '',
        actionName: '',
        weight: '',
        reps: '',
        setCount: 3,
        exhaustionScore: 100
      }
    ],
    /** 动作选择器索引（当前编辑哪一行） */
    pickerActiveIndex: -1,
    /** 力竭度选择器索引 */
    exhaustionPickerIndex: -1,
    /** 保存成功提示 */
    savedTip: false
  },

  lifetimes: {
    attached: function () {
      var today = new Date()
      var yyyy = today.getFullYear()
      var mm = today.getMonth() + 1
      var dd = today.getDate()
      this.setData({
        makeupDate: yyyy + '-' + this._pad(mm) + '-' + this._pad(dd)
      })
    }
  },

  methods: {
    /** 日期选择 */
    handleDateChange: function (e) {
      this.setData({ makeupDate: e.detail.value })
    },

    /** 点击某个训练项展开动作选择 */
    handleActionPick: function (e) {
      var index = e.currentTarget.dataset.index
      this.setData({ pickerActiveIndex: index })
    },

    /** Action Picker 选择动作后 */
    handleActionSelect: function (e) {
      var actionId = e.detail.actionId
      var actionName = e.detail.actionName
      var idx = this.data.pickerActiveIndex

      if (idx < 0 || idx >= this.data.recordList.length) return

      var list = this.data.recordList
      list[idx].actionId = actionId
      list[idx].actionName = actionName
      this.setData({
        recordList: list,
        pickerActiveIndex: -1
      })
    },

    /** 关闭 Action Picker */
    handlePickerClose: function () {
      this.setData({ pickerActiveIndex: -1 })
    },

    /** 重量输入 */
    handleWeightInput: function (e) {
      var idx = e.currentTarget.dataset.index
      var list = this.data.recordList
      list[idx].weight = e.detail.value
      this.setData({ recordList: list })
    },

    /** 次数输入 */
    handleRepsInput: function (e) {
      var idx = e.currentTarget.dataset.index
      var list = this.data.recordList
      list[idx].reps = e.detail.value
      this.setData({ recordList: list })
    },

    /** 组数输入 */
    handleSetCountInput: function (e) {
      var idx = e.currentTarget.dataset.index
      var list = this.data.recordList
      list[idx].setCount = parseInt(e.detail.value) || 3
      this.setData({ recordList: list })
    },

    /** 展开力竭度选择器 */
    handleExhaustionPick: function (e) {
      var index = e.currentTarget.dataset.index
      this.setData({ exhaustionPickerIndex: index })
    },

    /** 力竭度选择确认 */
    handleExhaustionSubmit: function (e) {
      var idx = this.data.exhaustionPickerIndex
      if (idx < 0 || idx >= this.data.recordList.length) return

      var list = this.data.recordList
      list[idx].exhaustionScore = Math.round(e.detail.score * 100)
      this.setData({
        recordList: list,
        exhaustionPickerIndex: -1
      })
    },

    /** 关闭力竭度选择器 */
    handleExhaustionClose: function () {
      this.setData({ exhaustionPickerIndex: -1 })
    },

    /** 添加一组记录 */
    handleAddRecord: function () {
      var list = this.data.recordList
      var newId = list.length > 0 ? list[list.length - 1].id + 1 : 1
      list.push({
        id: newId,
        actionId: '',
        actionName: '',
        weight: '',
        reps: '',
        setCount: 3,
        exhaustionScore: 100
      })
      this.setData({ recordList: list })
    },

    /** 删除一组记录 */
    handleRemoveRecord: function (e) {
      var idx = e.currentTarget.dataset.index
      var list = this.data.recordList
      if (list.length <= 1) return
      list.splice(idx, 1)
      this.setData({ recordList: list })
    },

    /** 力竭度系数显示文本 */
    _exhaustionLabel: function (score) {
      var s = parseInt(score) || 100
      if (s <= 60) return '浅练'
      if (s <= 85) return '适中'
      if (s <= 105) return '力竭'
      return '超负荷'
    },

    /** 提交补卡 */
    handleSubmit: function () {
      var that = this

      if (!that.data.makeupDate) {
        wx.showToast({ title: '请选择日期', icon: 'none' })
        return
      }

      // 收集有效记录（至少选择了动作）
      var validRecords = []
      for (var i = 0; i < that.data.recordList.length; i++) {
        var r = that.data.recordList[i]
        if (r.actionId) {
          validRecords.push(r)
        }
      }

      if (validRecords.length === 0) {
        wx.showToast({ title: '请至少选择一个动作', icon: 'none' })
        return
      }

      that.setData({ submitting: true })

      // 构造 startTime（所选日期的 12:00）
      var startTime = that.data.makeupDate + 'T12:00:00'

      // 逐条调用后端补卡 API
      function submitOne(index) {
        if (index >= validRecords.length) {
          // 全部完成
          that.setData({
            recordList: [
              { id: 1, actionId: '', actionName: '', weight: '', reps: '', setCount: 3, exhaustionScore: 100 }
            ],
            savedTip: true,
            submitting: false
          })

          setTimeout(function () {
            that.setData({ savedTip: false })
          }, 2000)

          wx.showToast({ title: '补卡成功！', icon: 'success' })
          return
        }

        var r = validRecords[index]
        var weightVal = r.weight && parseFloat(r.weight) > 0 ? parseFloat(r.weight) : undefined
        var repsVal = r.reps && parseInt(r.reps) > 0 ? parseInt(r.reps) : undefined
        var setCountVal = parseInt(r.setCount) || 3

        var payload = {
          actionId: r.actionId,
          startTime: startTime,
          exhaustionScore: (parseInt(r.exhaustionScore) || 100) / 100
        }
        if (weightVal !== undefined) payload.weight = weightVal
        if (repsVal !== undefined) payload.reps = repsVal
        if (setCountVal) payload.setCount = setCountVal

        api.post('/gym-workout/makeup', payload)
          .then(function () {
            submitOne(index + 1)
          })
          .catch(function (err) {
            wx.showToast({ title: '提交失败，请重试', icon: 'none' })
            console.error('补卡失败:', err)
            that.setData({ submitting: false })
          })
      }

      submitOne(0)
    },

    _pad: function (n) {
      return n < 10 ? '0' + n : '' + n
    }
  }
})
