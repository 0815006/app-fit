var api = require('../../../utils/request')

Component({
  properties: {
    /** 控制显示/隐藏 */
    visible: {
      type: Boolean,
      value: false
    },
    /** 限定大肌群编码（空=全部肌群） */
    muscleGroup: {
      type: String,
      value: ''
    },
    /** 置顶高亮的二级肌肉编码 */
    highlightMuscleCode: {
      type: String,
      value: ''
    },
    /** 高亮肌肉中文名，用于标题展示 */
    highlightMuscleName: {
      type: String,
      value: ''
    }
  },

  data: {
    /** 高亮推荐动作 */
    highlightedActions: [],
    /** 其他动作 */
    normalActions: [],
    /** 动作ID → 主肌群名称 map */
    actionMuscleGroupMap: {},
    /** 动作ID → 器械类型列表 map */
    actionEquipmentTypeMap: {},
    /** 加载中 */
    loading: false,
    /** 加载完 */
    ready: false
  },

  observers: {
    'visible': function (visible) {
      if (visible && !this.data.ready) {
        this.loadActions()
      }
    }
  },

  methods: {
    /** 加载动作数据 */
    loadActions: function () {
      var that = this
      that.setData({ loading: true })

      var pActions = api.get('/gym-action/all')
      var pMuscles = api.get('/gym-muscle/all')
      var pMuscleRels = api.get('/gym-action-muscle-rel', { page: 1, size: 500 })
      var pEquipment = api.get('/gym-equipment', { page: 1, size: 500 })
      var pEquipmentRels = api.get('/gym-action-equipment-rel', { page: 1, size: 500 })

      Promise.all([pActions, pMuscles, pMuscleRels, pEquipment, pEquipmentRels])
        .then(function (results) {
          var actions = results[0].data || []
          var muscles = results[1].data || []
          var muscleRels = (results[2].data && results[2].data.records) || []
          var equipment = (results[3].data && results[3].data.records) || []
          var equipmentRels = (results[4].data && results[4].data.records) || []

          // muscleId → muscleGroup
          var muscleGroupMap = {}
          var muscleIdToCode = {}
          for (var i = 0; i < muscles.length; i++) {
            muscleGroupMap[muscles[i].id] = muscles[i].muscleGroup
            muscleIdToCode[muscles[i].id] = muscles[i].muscleCode || muscles[i].id
          }

          // equipmentId → equipmentType
          var equipmentTypeMap = {}
          for (var i = 0; i < equipment.length; i++) {
            equipmentTypeMap[equipment[i].id] = equipment[i].equipmentType
          }

          // actionId → primary muscleGroup
          var actionMuscleGroupMap = {}
          var primaryByAction = {}
          for (var i = 0; i < muscleRels.length; i++) {
            var rel = muscleRels[i]
            if (rel.isPrimary === 1) {
              if (!primaryByAction[rel.actionId]) {
                primaryByAction[rel.actionId] = rel.muscleId
                actionMuscleGroupMap[rel.actionId] = muscleGroupMap[rel.muscleId] || ''
              }
            }
          }

          // actionId → [equipmentTypes]
          var actionEquipmentTypeMap = {}
          for (var i = 0; i < equipmentRels.length; i++) {
            var er = equipmentRels[i]
            if (!actionEquipmentTypeMap[er.actionId]) {
              actionEquipmentTypeMap[er.actionId] = []
            }
            var etype = equipmentTypeMap[er.equipmentId]
            if (etype && actionEquipmentTypeMap[er.actionId].indexOf(etype) === -1) {
              actionEquipmentTypeMap[er.actionId].push(etype)
            }
          }

          // 过滤 & 分离高亮/普通动作
          var result = that._splitAndFilter(
            actions,
            that.properties.muscleGroup,
            that.properties.highlightMuscleCode,
            actionMuscleGroupMap,
            muscleIdToCode,
            muscleRels
          )

          that.setData({
            highlightedActions: result.highlighted,
            normalActions: result.normal,
            actionMuscleGroupMap: actionMuscleGroupMap,
            actionEquipmentTypeMap: actionEquipmentTypeMap,
            loading: false,
            ready: true
          })
        })
        .catch(function (err) {
          console.error('加载动作失败:', err)
          wx.showToast({ title: '加载动作失败', icon: 'none' })
          that.setData({ loading: false })
        })
    },

    /** 过滤 + 分离高亮/普通动作 */
    _splitAndFilter: function (actions, muscleGroup, highlightCode, muscleGroupMap, muscleIdToCode, muscleRels) {
      var filtered = []

      for (var i = 0; i < actions.length; i++) {
        var action = actions[i]
        if (muscleGroup) {
          var ag = muscleGroupMap[action.id] || ''
          if (ag !== muscleGroup) continue
        }
        filtered.push(action)
      }

      var highlighted = []
      var normal = []

      if (highlightCode) {
        var highlightActionIds = {}
        for (var j = 0; j < muscleRels.length; j++) {
          var rel = muscleRels[j]
          var mCode = muscleIdToCode[rel.muscleId] || ''
          if (mCode === highlightCode) {
            highlightActionIds[rel.actionId] = true
          }
        }

        for (var k = 0; k < filtered.length; k++) {
          if (highlightActionIds[filtered[k].id]) {
            highlighted.push(filtered[k])
          } else {
            normal.push(filtered[k])
          }
        }
      } else {
        // 无高亮时，全部归入普通
        normal = filtered
      }

      return { highlighted: highlighted, normal: normal }
    },

    /** 选择动作 */
    handleSelect: function (e) {
      var actionId = e.currentTarget.dataset.id
      var actionName = e.currentTarget.dataset.name
      var actionType = e.currentTarget.dataset.actionType || ''
      this.triggerEvent('select', { actionId: actionId, actionName: actionName, actionType: actionType })
    },

    /** 关闭 */
    handleClose: function () {
      this.triggerEvent('close')
    },

    /** 阻止冒泡 */
    noop: function () {}
  }
})
