var api = require('../../utils/request')

Page({
  data: {
    action: null,
    equipments: [],
    muscles: { primary: [], secondary: [] },
    images: [],
    videoUrl: '',
    loading: true,
    error: false,
    errorMsg: '',
  },

  onLoad: function (options) {
    var actionId = options.actionId
    if (!actionId) {
      this.setData({ loading: false, error: true, errorMsg: '缺少动作ID' })
      return
    }
    this.loadActionDetail(actionId)
  },

  loadActionDetail: function (actionId) {
    var that = this

    // Fetch action, muscle-rels, equipment-rels, muscles, equipment in parallel
    var pAction = api.get('/gym-action/' + actionId)
    var pMuscleRel = api.get('/gym-action-muscle-rel', { page: 1, size: 100, actionId: actionId })
    var pEquipRel = api.get('/gym-action-equipment-rel', { page: 1, size: 100, actionId: actionId })
    var pMuscles = api.get('/gym-muscle/all')
    var pEquipment = api.get('/gym-equipment', { page: 1, size: 500 })

    Promise.all([pAction, pMuscleRel, pEquipRel, pMuscles, pEquipment])
      .then(function (results) {
        var action = results[0].data
        if (!action) {
          that.setData({ loading: false, error: true, errorMsg: '动作不存在' })
          return
        }

        var muscleRels = (results[1].data && results[1].data.records) || []
        var equipmentRels = (results[2].data && results[2].data.records) || []
        var muscles = results[3].data || []
        var equipment = (results[4].data && results[4].data.records) || []

        // Build muscle name maps
        var muscleNameMap = {}
        for (var i = 0; i < muscles.length; i++) {
          muscleNameMap[muscles[i].id] = muscles[i].muscleName
        }
        var muscleGroupMap = {}
        for (var i = 0; i < muscles.length; i++) {
          muscleGroupMap[muscles[i].id] = muscles[i].muscleGroup
        }

        // Build equipment maps
        var equipNameMap = {}
        var equipTypeMap = {}
        for (var i = 0; i < equipment.length; i++) {
          equipNameMap[equipment[i].id] = equipment[i].equipmentName
          equipTypeMap[equipment[i].id] = equipment[i].equipmentType || ''
        }

        // Classify muscles into primary / secondary
        var primaryMuscles = []
        var secondaryMuscles = []
        for (var i = 0; i < muscleRels.length; i++) {
          var mr = muscleRels[i]
          var name = muscleNameMap[mr.muscleId] || '未知肌肉'
          var group = muscleGroupMap[mr.muscleId] || '其他'
          var item = { name: name, group: group }
          if (mr.isPrimary === 1) {
            primaryMuscles.push(item)
          } else {
            secondaryMuscles.push(item)
          }
        }

        // Build equipment list
        var equipments = []
        for (var i = 0; i < equipmentRels.length; i++) {
          var er = equipmentRels[i]
          equipments.push({
            name: equipNameMap[er.equipmentId] || '未知器械',
            typeDesc: equipTypeMap[er.equipmentId] || '其他',
          })
        }

        // Parse images
        var images = []
        if (action.imageUrls) {
          images = action.imageUrls.split(',').filter(function (url) {
            return url && url.trim().length > 0
          })
        }

        that.setData({
          action: action,
          equipments: equipments,
          muscles: { primary: primaryMuscles, secondary: secondaryMuscles },
          images: images,
          videoUrl: action.videoUrl || '',
          loading: false,
          error: false,
        })

        // Update navigation bar title
        wx.setNavigationBarTitle({
          title: action.name || '动作详情',
        })
      })
      .catch(function (err) {
        console.error('Failed to load action detail:', err)
        that.setData({
          loading: false,
          error: true,
          errorMsg: '加载失败，请稍后重试',
        })
      })
  },

  // Preview image
  previewImage: function (e) {
    var url = e.currentTarget.dataset.url
    if (!url) return
    wx.previewImage({
      urls: this.data.images,
      current: url,
    })
  },

  // Share
  onShareAppMessage: function () {
    var action = this.data.action
    return {
      title: action ? '健身动作：' + action.name : '健身动作详情',
      path: '/pages/action-detail/action-detail?actionId=' + (action ? action.id : ''),
    }
  },
})