var api = require('../../../utils/request')

/** 肌群图标 */
var GROUP_ICONS = {
  CHEST: '💪', BACK: '🦾', SHOULDER: '🏋️', ARM: '💪',
  LEG: '🦵', GLUTE: '🍑', CORE: '🧘', CARDIO: '🏃', FULL_BODY: '🏃'
}

/** 肌群左侧色条 */
var GROUP_COLORS = {
  CHEST: '#f56c6c', BACK: '#e6a23c', SHOULDER: '#409eff',
  ARM: '#67c23a', LEG: '#b37feb', GLUTE: '#ff85c0',
  CORE: '#36cfc9', CARDIO: '#f39c12', FULL_BODY: '#909399'
}

/** 自重动作关键词 */
var BODYWEIGHT_KEYWORDS = [
  '引体向上', '双杠臂屈伸', '平板支撑', '悬垂举腿', '仰卧抬腿', '基础卷腹',
  '俯卧撑', '仰卧起坐', '平板', '臀桥'
]

Component({
  data: {
    /** Web 端 MuscleDashboard 数据 */
    readyMuscleNames: [],
    readyMuscleNamesText: '',
    muscleGroups: [],
    timeoutRecord: null,
    loading: false,

    /** 当前正在训练的肌群 */
    activeWorkoutMuscleGroup: '',
    activeWorkoutMuscleName: '',
    activeWorkoutActionId: '',
    activeWorkoutActionName: '',
    activeWorkoutActionType: '',
    workoutStartTime: 0,
    timerDisplay: '00:00:00',
    _timerInterval: 0,
    currentRecordId: '',

    /** 训练数据输入 */
    inputMode: 'weighted',
    weight: null,
    reps: null,
    setCount: null,

    /** 1RM / 总容量 估算 */
    estimated1RM: '',
    totalVolume: '',

    /** 训练参考信息 */
    refMuscles: [],
    refEquipments: [],
    refRecommendations: [],
    refLoading: false,
    refCollapsed: false,

    /** 训练目标中文映射 */
    goalLabelMap: { HYPERTROPHY: '增肌', STRENGTH: '力量', FAT_LOSS: '减脂', ENDURANCE: '耐力' },

    /** Action Picker 控制 */
    pickerVisible: false,
    pickerMuscleGroup: '',
    pickerHighlightCode: '',
    pickerHighlightName: '',

    /** 力竭度弹窗 */
    exhaustionVisible: false,
    /** 暂存训练数据（结束计时时暂存，等力竭度确认后一起提交） */
    pendingWeight: null,
    pendingReps: null,
    pendingSetCount: null
  },

  lifetimes: {
    attached: function () {
      this.loadDashboard()
    },
    detached: function () {
      this.stopTimer()
    }
  },

  methods: {
    /** 加载首页看板 GET /api/gym-workout/dashboard */
    loadDashboard: function () {
      var that = this
      that.setData({ loading: true })

      api.get('/gym-workout/dashboard')
        .then(function (res) {
          var data = res.data || {}
          var groups = data.muscleGroups || []

          for (var i = 0; i < groups.length; i++) {
            var g = groups[i]
            g.remainingLabel = that._formatRemaining(g.remainingSeconds)
            g.groupIcon = that._groupIcon(g.muscleGroup)
            g.groupColor = that._groupColor(g.muscleGroup)
          }

          var names = data.readyMuscleNames || []
          that.setData({
            readyMuscleNames: names,
            readyMuscleNamesText: names.join('、'),
            muscleGroups: groups,
            timeoutRecord: data.timeoutRecord || null
          })
        })
        .catch(function () {
          that.setData({ muscleGroups: [], readyMuscleNames: [], timeoutRecord: null })
          wx.showToast({ title: '加载肌群数据失败', icon: 'none' })
        })
        .finally(function () {
          that.setData({ loading: false })
        })
    },

    /** 格式化恢复剩余时间 */
    _formatRemaining: function (seconds) {
      if (seconds <= 0) return ''
      var h = Math.floor(seconds / 3600)
      if (h >= 24) {
        var d = Math.floor(h / 24)
        return '⏳ ' + d + '天'
      }
      if (h > 0) return '⏳ ' + h + 'h'
      var m = Math.floor((seconds % 3600) / 60)
      return '⏳ ' + m + 'm'
    },

    _groupIcon: function (code) { return GROUP_ICONS[code] || '🏋️' },
    _groupColor: function (code) { return GROUP_COLORS[code] || '#dcdfe6' },

    /** 判断输入模式 */
    _determineInputMode: function (actionName, actionType) {
      if (actionType && (actionType === 'CARDIO' || actionType === 'STRETCH' || actionType === 'MOBILITY')) return 'cardio'
      if (actionType === 'COMPOUND') return 'weighted'
      if (BODYWEIGHT_KEYWORDS.some(function (k) { return actionName.indexOf(k) >= 0 })) return 'bodyweight'
      return 'weighted'
    },

    /** 点击子肌群：开始训练 */
    handleSubMuscleClick: function (e) {
      var muscleGroup = e.currentTarget.dataset.muscleGroup
      var muscleCode = e.currentTarget.dataset.muscleCode
      var muscleName = e.currentTarget.dataset.muscleName

      if (this.data.activeWorkoutMuscleGroup) {
        wx.showToast({ title: '请先结束当前训练', icon: 'none' })
        return
      }

      this.setData({
        pickerVisible: true,
        pickerMuscleGroup: muscleGroup,
        pickerHighlightCode: muscleCode || '',
        pickerHighlightName: muscleName || ''
      })
    },

    /** 点击大肌群标题区域 */
    handleGroupHeaderTap: function (e) {
      var muscleGroup = e.currentTarget.dataset.muscleGroup
      var groupName = e.currentTarget.dataset.groupName

      if (this.data.activeWorkoutMuscleGroup) {
        wx.showToast({ title: '请先结束当前训练', icon: 'none' })
        return
      }

      this.setData({
        pickerVisible: true,
        pickerMuscleGroup: muscleGroup,
        pickerHighlightCode: muscleGroup,
        pickerHighlightName: groupName
      })
    },

    /** Action Picker 选择动作后 */
    handleActionSelect: function (e) {
      var actionId = e.detail.actionId
      var actionName = e.detail.actionName
      var actionType = e.detail.actionType || ''
      var inputMode = this._determineInputMode(actionName, actionType)

      this.setData({ pickerVisible: false })
      this.startWorkout(actionId, actionName, actionType, inputMode)
    },

    /** 开始训练计时 */
    startWorkout: function (actionId, actionName, actionType, inputMode) {
      var that = this
      var beginTime = Date.now()

      that.setData({
        activeWorkoutMuscleGroup: that.data.pickerMuscleGroup,
        activeWorkoutMuscleName: that.data.pickerMuscleName,
        activeWorkoutActionId: actionId,
        activeWorkoutActionName: actionName,
        activeWorkoutActionType: actionType,
        inputMode: inputMode,
        workoutStartTime: beginTime,
        timerDisplay: '00:00:00',
        weight: null,
        reps: null,
        setCount: null,
        estimated1RM: '',
        totalVolume: '',
        refMuscles: [],
        refEquipments: [],
        refRecommendations: [],
        refCollapsed: false
      })

      api.post('/gym-workout/start', { actionId: actionId })
        .then(function (res) {
          that.setData({ currentRecordId: res.data || '' })
        })
        .catch(function (err) {
          console.error('启动训练记录失败:', err)
        })

      that._timerInterval = setInterval(function () {
        if (!that.data.workoutStartTime) return
        var elapsed = Date.now() - that.data.workoutStartTime
        var totalSec = Math.floor(elapsed / 1000)
        var hh = Math.floor(totalSec / 3600)
        var mm = Math.floor((totalSec % 3600) / 60)
        var ss = totalSec % 60
        that.setData({
          timerDisplay: that._pad(hh) + ':' + that._pad(mm) + ':' + that._pad(ss)
        })
      }, 500)

      // 加载参考信息
      that._loadRefInfo(actionId)
    },

    /** 加载参考信息：肌群、器械、训练建议 */
    _loadRefInfo: function (actionId) {
      var that = this
      if (!actionId) return
      that.setData({ refLoading: true })

      var p1 = api.get('/gym-action-muscle-rel/by-action/' + actionId).catch(function () { return null })
      var p2 = api.get('/gym-action-equipment-rel/by-action/' + actionId).catch(function () { return null })
      var p3 = api.get('/gym-action-recommendation/by-action/' + actionId).catch(function () { return null })

      Promise.all([p1, p2, p3])
        .then(function (results) {
          var recs = (results[2] && results[2].data) || []
          // 预计算建议展示文本
          for (var i = 0; i < recs.length; i++) {
            recs[i]._setDisplay = that._recSetDisplay(recs[i])
            recs[i]._repDisplay = that._recRepDisplay(recs[i])
            recs[i]._restDisplay = that._recRestDisplay(recs[i])
          }

          that.setData({
            refMuscles: (results[0] && results[0].data) || [],
            refEquipments: (results[1] && results[1].data) || [],
            refRecommendations: recs
          })
        })
        .catch(function () {})
        .finally(function () {
          that.setData({ refLoading: false })
        })
    },

    /** 组数展示 */
    _recSetDisplay: function (rec) {
      if (rec.minSets != null && rec.maxSets != null) {
        return rec.minSets === rec.maxSets ? rec.maxSets + ' 组' : rec.minSets + '~' + rec.maxSets + ' 组'
      }
      return ''
    },

    /** 次数展示 */
    _recRepDisplay: function (rec) {
      if (rec.minReps != null && rec.maxReps != null && rec.minReps > 0) {
        return '× ' + (rec.minReps === rec.maxReps ? rec.maxReps + ' 次' : rec.minReps + '~' + rec.maxReps + ' 次')
      }
      return ''
    },

    /** 组间休息展示 */
    _recRestDisplay: function (rec) {
      if (rec.recommendRestTime != null && rec.recommendRestTime > 0) {
        return '组休 ' + this._formatRest(rec.recommendRestTime)
      }
      return ''
    },

    _formatRest: function (s) {
      if (s >= 60) {
        var m = Math.floor(s / 60)
        return s % 60 === 0 ? m + 'min' : m + 'min' + (s % 60) + 's'
      }
      return s + 's'
    },

    /** 切换参考信息折叠 */
    handleToggleRefInfo: function () {
      this.setData({ refCollapsed: !this.data.refCollapsed })
    },

    /** 训练数据输入变更 */
    handleWeightInput: function (e) {
      var val = parseFloat(e.detail.value)
      this.setData({ weight: isNaN(val) ? null : val })
      this._recalc()
    },
    handleRepsInput: function (e) {
      var val = parseInt(e.detail.value, 10)
      this.setData({ reps: isNaN(val) ? null : val })
      this._recalc()
    },
    handleSetCountInput: function (e) {
      var val = parseInt(e.detail.value, 10)
      this.setData({ setCount: isNaN(val) ? null : val })
      this._recalc()
    },

    /** 实时计算 1RM 和总容量 */
    _recalc: function () {
      var weight = this.data.weight
      var reps = this.data.reps
      var setCount = this.data.setCount
      var rm1 = ''
      var vol = ''

      if (weight != null && reps != null && weight > 0 && reps > 0) {
        rm1 = (weight * (1 + reps / 30)).toFixed(1)
      }
      if (weight != null && reps != null && setCount != null && weight > 0 && reps > 0 && setCount > 0) {
        vol = (weight * reps * setCount).toFixed(0)
      }

      this.setData({ estimated1RM: rm1, totalVolume: vol })
    },

    /** 结束训练 - 先暂存数据，弹出力竭度评价 */
    handleEndWorkout: function () {
      var that = this
      if (!that.data.activeWorkoutMuscleGroup) return

      wx.showModal({
        title: '结束训练',
        content: '确认结束本次训练？已训练: ' + that.data.timerDisplay,
        success: function (modalRes) {
          if (!modalRes.confirm) return
          that.stopTimer()

          // 暂存训练数据，弹出力竭度
          that.setData({
            pendingWeight: that.data.weight,
            pendingReps: that.data.reps,
            pendingSetCount: that.data.setCount,
            exhaustionVisible: true
          })
        }
      })
    },

    /** 力竭度评价提交 */
    handleExhaustionSubmit: function (e) {
      var that = this
      var score = e.detail.score
      var recordId = that.data.currentRecordId

      that.setData({ exhaustionVisible: false })

      if (recordId) {
        api.put('/gym-workout/' + recordId + '/end', {
          weight: that.data.pendingWeight != null ? that.data.pendingWeight : null,
          reps: that.data.pendingReps != null ? that.data.pendingReps : null,
          setCount: that.data.pendingSetCount != null ? that.data.pendingSetCount : null,
          exhaustionScore: score
        }).then(function () {
          wx.showToast({ title: '训练记录已保存', icon: 'success' })
        }).catch(function (err) {
          console.error('保存训练记录失败:', err)
          wx.showToast({ title: '记录保存失败，请稍后补卡', icon: 'none' })
        })
      } else {
        wx.showToast({ title: '训练完成！（未关联后端记录）', icon: 'none' })
      }

      that.setData({
        activeWorkoutMuscleGroup: '',
        activeWorkoutMuscleName: '',
        activeWorkoutActionId: '',
        activeWorkoutActionName: '',
        activeWorkoutActionType: '',
        workoutStartTime: 0,
        timerDisplay: '00:00:00',
        currentRecordId: '',
        weight: null,
        reps: null,
        setCount: null,
        estimated1RM: '',
        totalVolume: '',
        pendingWeight: null,
        pendingReps: null,
        pendingSetCount: null
      })

      that.loadDashboard()
    },

    /** 力竭度弹窗关闭 */
    handleExhaustionClose: function () {
      // 用户取消力竭度评价，视为放弃本次结束，恢复训练数据
      this.setData({ exhaustionVisible: false })
      // 但计时已停，所以依然重置
      var that = this
      that.setData({
        activeWorkoutMuscleGroup: '',
        activeWorkoutMuscleName: '',
        activeWorkoutActionId: '',
        activeWorkoutActionName: '',
        activeWorkoutActionType: '',
        workoutStartTime: 0,
        timerDisplay: '00:00:00',
        currentRecordId: '',
        weight: null,
        reps: null,
        setCount: null,
        estimated1RM: '',
        totalVolume: '',
        pendingWeight: null,
        pendingReps: null,
        pendingSetCount: null
      })
      wx.showToast({ title: '本次训练未保存', icon: 'none' })
      that.loadDashboard()
    },

    /** 取消训练 */
    handleCancelWorkout: function () {
      var that = this
      wx.showModal({
        title: '取消训练',
        content: '放弃本次训练计时？',
        success: function (modalRes) {
          if (!modalRes.confirm) return
          that.stopTimer()
          that.setData({
            activeWorkoutMuscleGroup: '',
            activeWorkoutMuscleName: '',
            activeWorkoutActionId: '',
            activeWorkoutActionName: '',
            activeWorkoutActionType: '',
            workoutStartTime: 0,
            timerDisplay: '00:00:00',
            currentRecordId: '',
            weight: null,
            reps: null,
            setCount: null,
            estimated1RM: '',
            totalVolume: ''
          })
        }
      })
    },

    stopTimer: function () {
      if (this.data._timerInterval) {
        clearInterval(this.data._timerInterval)
        this.data._timerInterval = 0
      }
    },

    _pad: function (n) {
      return n < 10 ? '0' + n : '' + n
    },

    handlePickerClose: function () {
      this.setData({ pickerVisible: false })
    }
  }
})
