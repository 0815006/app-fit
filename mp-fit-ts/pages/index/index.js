var api = require('../../utils/request')

var CHINESE_WEEKDAYS = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']

function getTodayStr() {
  var d = new Date()
  var y = d.getFullYear()
  var m = String(d.getMonth() + 1)
  if (m.length < 2) m = '0' + m
  var day = String(d.getDate())
  if (day.length < 2) day = '0' + day
  return y + '-' + m + '-' + day
}

function getDefaultMealType() {
  var h = new Date().getHours()
  if (h < 10) return '早餐'
  if (h < 14) return '午餐'
  if (h < 19) return '晚餐'
  return '夜宵'
}

function getDayOfWeekStr(dateStr) {
  if (!dateStr) return ''
  var d = new Date(dateStr.replace(/-/g, '/'))
  return CHINESE_WEEKDAYS[d.getDay()]
}

function dateAddDays(dateStr, days) {
  var d = new Date(dateStr.replace(/-/g, '/'))
  d.setDate(d.getDate() + days)
  var y = d.getFullYear()
  var m = String(d.getMonth() + 1)
  if (m.length < 2) m = '0' + m
  var day = String(d.getDate())
  if (day.length < 2) day = '0' + day
  return y + '-' + m + '-' + day
}

function makeDateDisplay(dateStr) {
  if (!dateStr) return ''
  var wd = getDayOfWeekStr(dateStr)
  return dateStr + ' ' + wd
}

Page({
  data: {
    empNo: '0000000',
    loginCount: null,
    loading: false,
    activeTab: 'stats',
    tabBars: [
      { key: 'stats', label: '登录统计', icon: '📊' },
      { key: 'menu', label: '今日菜单', icon: '🍽️' },
      { key: 'fitness', label: '健身动作', icon: '🏋️' },
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

    // scroll-view height for content above tab-bar
    scrollHeight: 500,

    // ── Today's Menu ──
    todayZone: '一期',
    todayDate: getTodayStr(),
    todayDateDisplay: makeDateDisplay(getTodayStr()),
    todayMealType: getDefaultMealType(),
    todayLoading: false,
    todayMenuRecords: [],
    todayGrouped: [],
    todayShowSkeleton: false,
    isTodaySelected: true,

    todayMealTypes: [
      { label: '早餐', icon: '🍳', value: '早餐', key: 'breakfast' },
      { label: '午餐', icon: '🍛', value: '午餐', key: 'lunch' },
      { label: '晚餐', icon: '🍲', value: '晚餐', key: 'dinner' },
      { label: '夜宵', icon: '🌙', value: '夜宵', key: 'supper' },
    ],

    // ── Fitness Actions ──
    fitnessActions: [],
    fitnessFilteredActions: [],
    fitnessAllMuscles: [],
    fitnessAllEquipment: [],
    // actionId → primary muscle group (from the first primary muscle)
    fitnessActionMuscleGroupMap: {},
    // actionId → [equipmentTypes]
    fitnessActionEquipmentTypeMap: {},
    // Unique values for filter UI
    fitnessMuscleGroups: [],
    fitnessEquipmentTypes: [],
    fitnessMovementPatterns: [],
    fitnessDifficultyLevels: [
      { label: '初学者', value: 1 },
      { label: '中级', value: 2 },
      { label: '进阶', value: 3 },
    ],
    // Active filters (empty string means "全部")
    fitnessActiveMuscleGroup: '',
    fitnessActiveEquipmentType: '',
    fitnessActiveMovementPattern: '',
    fitnessActiveDifficultyLevel: 0,
    fitnessLoading: false,
    fitnessDataReady: false,
  },

  onLoad: function () {
    var empNo = wx.getStorageSync('empNo') || '0000000'
    var sys = wx.getSystemInfoSync()
    var rpx = sys.windowWidth / 750
    var tabBarPx = Math.round(100 * rpx)
    var scrollHeight = sys.windowHeight - tabBarPx
    this.setData({ empNo: empNo, scrollHeight: scrollHeight })
    this.loadLoginData()
  },

  onShow: function () {
    this.loadLoginData()
  },

  // ── Tab switching ──
  switchTab: function (e) {
    var key = e.currentTarget.dataset.key
    this.setData({ activeTab: key })
    if (key === 'stats') {
      this.loadLoginData()
    } else if (key === 'menu') {
      this.loadTodayMenu()
    } else if (key === 'fitness') {
      if (!this.data.fitnessDataReady) {
        this.loadFitnessData()
      }
    }
  },

  // ── Login stats ──
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

  // ═══════════════════════════════════════════════
  // ── Today's Menu Logic ──
  // ═══════════════════════════════════════════════

  loadTodayMenu: function () {
    var that = this
    that.setData({ todayLoading: true, todayShowSkeleton: true })

    api
      .get('/canteen-menu/records', {
        page: 1,
        size: 500,
        canteenZone: that.data.todayZone,
        menuDate: that.data.todayDate,
        mealType: that.data.todayMealType,
      })
      .then(function (res) {
        var records = res.data.records || []
        var grouped = that._groupRecords(records)
        that.setData({
          todayMenuRecords: records,
          todayGrouped: grouped,
        })
      })
      .catch(function () {
        that.setData({ todayMenuRecords: [], todayGrouped: [] })
      })
      .finally(function () {
        that.setData({ todayLoading: false })
        setTimeout(function () {
          that.setData({ todayShowSkeleton: false })
        }, 200)
      })
  },

  _groupRecords: function (records) {
    var map = {}
    for (var i = 0; i < records.length; i++) {
      var cat = records[i].categoryName
      if (!map[cat]) map[cat] = []
      map[cat].push(records[i])
    }
    var result = []
    for (var key in map) {
      if (map.hasOwnProperty(key)) {
        result.push({ name: key, items: map[key] })
      }
    }
    return result
  },

  // ── Tier 1: Zone ──
  handleTodayZoneChange: function (e) {
    var zone = e.currentTarget.dataset.zone
    if (zone === this.data.todayZone) return
    this.setData({ todayZone: zone })
    this.loadTodayMenu()
  },

  // ── Tier 2: Date ──
  handleTodayDateChange: function (e) {
    var val = e.detail.value // YYYY-MM-DD from picker
    if (val === this.data.todayDate) return
    var todayStr = getTodayStr()
    this.setData({
      todayDate: val,
      todayDateDisplay: makeDateDisplay(val),
      isTodaySelected: val === todayStr,
    })
    this.loadTodayMenu()
  },

  goToPrevDay: function () {
    var newDate = dateAddDays(this.data.todayDate, -1)
    var todayStr = getTodayStr()
    this.setData({
      todayDate: newDate,
      todayDateDisplay: makeDateDisplay(newDate),
      isTodaySelected: newDate === todayStr,
    })
    this.loadTodayMenu()
  },

  goToNextDay: function () {
    var newDate = dateAddDays(this.data.todayDate, 1)
    var todayStr = getTodayStr()
    this.setData({
      todayDate: newDate,
      todayDateDisplay: makeDateDisplay(newDate),
      isTodaySelected: newDate === todayStr,
    })
    this.loadTodayMenu()
  },

  goToToday: function () {
    var todayStr = getTodayStr()
    if (this.data.todayDate === todayStr) return
    this.setData({
      todayDate: todayStr,
      todayDateDisplay: makeDateDisplay(todayStr),
      isTodaySelected: true,
    })
    this.loadTodayMenu()
  },

  // ── Tier 3: Meal Type ──
  handleTodayMealChange: function (e) {
    var type = e.currentTarget.dataset.type
    if (type === this.data.todayMealType) return
    this.setData({ todayMealType: type })
    this.loadTodayMenu()
  },

  // ═══════════════════════════════════════════════
  // ── Fitness Actions Logic ──
  // ═══════════════════════════════════════════════

  loadFitnessData: function () {
    var that = this
    that.setData({ fitnessLoading: true })

    // Fetch all required data in parallel
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

        // Build muscleId → muscleGroup map
        var muscleGroupMap = {}
        for (var i = 0; i < muscles.length; i++) {
          muscleGroupMap[muscles[i].id] = muscles[i].muscleGroup
        }

        // Build muscleId → muscleName map
        var muscleNameMap = {}
        for (var i = 0; i < muscles.length; i++) {
          muscleNameMap[muscles[i].id] = muscles[i].muscleName
        }

        // Build equipmentId → equipmentType map
        var equipmentTypeMap = {}
        for (var i = 0; i < equipment.length; i++) {
          equipmentTypeMap[equipment[i].id] = equipment[i].equipmentType
        }

        // Translate English muscle group names to Chinese
        var MG_TRANSLATIONS = {
          'arm': '手臂',
          'arms': '手臂',
          'back': '背部',
          'chest': '胸部',
          'shoulder': '肩部',
          'shoulders': '肩部',
          'leg': '腿部',
          'legs': '腿部',
          'core': '核心',
          'abs': '核心',
          'glute': '臀部',
          'glutes': '臀部',
          'hip': '臀部',
          'full body': '全身',
          'fullbody': '全身',
          'cardio': '有氧',
        }

        function translateMuscleGroup(name) {
          var lower = (name || '').toLowerCase().trim()
          return MG_TRANSLATIONS[lower] || name
        }

        // Build actionId → primary muscleGroup map (translated to Chinese)
        var actionMuscleGroupMap = {}
        var primaryMuscleByAction = {}
        for (var i = 0; i < muscleRels.length; i++) {
          var rel = muscleRels[i]
          if (rel.isPrimary === 1) {
            if (!primaryMuscleByAction[rel.actionId]) {
              primaryMuscleByAction[rel.actionId] = rel.muscleId
              actionMuscleGroupMap[rel.actionId] = translateMuscleGroup(muscleGroupMap[rel.muscleId] || '其他')
            }
          }
        }

        // Build actionId → [equipmentTypes] map
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

        // Extract unique muscle groups (sorted) – use translated names
        var muscleGroupSet = {}
        for (var key in muscleGroupMap) {
          if (muscleGroupMap.hasOwnProperty(key)) {
            muscleGroupSet[translateMuscleGroup(muscleGroupMap[key])] = true
          }
        }
        var muscleGroups = Object.keys(muscleGroupSet)
        // Put common groups first
        var priorityGroups = ['胸部', '背部', '肩部', '手臂', '腿部', '核心', '臀部']
        muscleGroups.sort(function (a, b) {
          var ia = priorityGroups.indexOf(a)
          var ib = priorityGroups.indexOf(b)
          if (ia !== -1 && ib !== -1) return ia - ib
          if (ia !== -1) return -1
          if (ib !== -1) return 1
          return a.localeCompare(b)
        })
        muscleGroups.unshift('全部')

        // Extract unique equipment types (with "全部" prepended)
        var equipmentTypeSet = {}
        for (var i = 0; i < equipment.length; i++) {
          if (equipment[i].equipmentType) {
            equipmentTypeSet[equipment[i].equipmentType] = true
          }
        }
        var equipmentTypesRaw = Object.keys(equipmentTypeSet)
        var equipmentTypes = ['全部'].concat(equipmentTypesRaw)

        // Extract unique movement patterns (with "全部" prepended)
        var movementPatternSet = {}
        for (var i = 0; i < actions.length; i++) {
          if (actions[i].movementPattern) {
            movementPatternSet[actions[i].movementPattern] = true
          }
        }
        var movementPatternsRaw = Object.keys(movementPatternSet)
        var movementPatterns = ['全部'].concat(movementPatternsRaw)

        that.setData({
          fitnessActions: actions,
          fitnessAllMuscles: muscles,
          fitnessAllEquipment: equipment,
          fitnessActionMuscleGroupMap: actionMuscleGroupMap,
          fitnessActionEquipmentTypeMap: actionEquipmentTypeMap,
          fitnessMuscleGroups: muscleGroups,
          fitnessEquipmentTypes: equipmentTypes,
          fitnessMovementPatterns: movementPatterns,
          fitnessDataReady: true,
          fitnessActiveMuscleGroup: '全部',
          fitnessActiveEquipmentType: '',
          fitnessActiveMovementPattern: '',
          fitnessActiveDifficultyLevel: 0,
        })

        that._applyFitnessFilters()
      })
      .catch(function (err) {
        console.error('Failed to load fitness data:', err)
        wx.showToast({ title: '加载健身数据失败', icon: 'none' })
      })
      .finally(function () {
        that.setData({ fitnessLoading: false })
      })
  },

  // ── Filter handlers ──

  handleFitnessMuscleGroupChange: function (e) {
    var group = e.currentTarget.dataset.group
    if (group === this.data.fitnessActiveMuscleGroup) return
    this.setData({ fitnessActiveMuscleGroup: group })
    this._applyFitnessFilters()
  },

  handleFitnessEquipmentTypeChange: function (e) {
    var idx = Number(e.detail.value)
    var types = this.data.fitnessEquipmentTypes
    var type = idx === 0 ? '' : types[idx] || ''
    if (type === this.data.fitnessActiveEquipmentType) return
    this.setData({ fitnessActiveEquipmentType: type })
    this._applyFitnessFilters()
  },

  handleFitnessMovementPatternChange: function (e) {
    var idx = Number(e.detail.value)
    var patterns = this.data.fitnessMovementPatterns
    var pattern = idx === 0 ? '' : patterns[idx] || ''
    if (pattern === this.data.fitnessActiveMovementPattern) return
    this.setData({ fitnessActiveMovementPattern: pattern })
    this._applyFitnessFilters()
  },

  handleFitnessDifficultyChange: function (e) {
    var level = Number(e.currentTarget.dataset.level)
    if (level === this.data.fitnessActiveDifficultyLevel) {
      // Toggle off
      level = 0
    }
    this.setData({ fitnessActiveDifficultyLevel: level })
    this._applyFitnessFilters()
  },

  handleFitnessClearFilters: function () {
    this.setData({
      fitnessActiveMuscleGroup: '全部',
      fitnessActiveEquipmentType: '',
      fitnessActiveMovementPattern: '',
      fitnessActiveDifficultyLevel: 0,
    })
    this._applyFitnessFilters()
  },

  _applyFitnessFilters: function () {
    var that = this
    var actions = that.data.fitnessActions
    var activeGroup = that.data.fitnessActiveMuscleGroup
    var activeEquipmentType = that.data.fitnessActiveEquipmentType
    var activeMovementPattern = that.data.fitnessActiveMovementPattern
    var activeDifficultyLevel = that.data.fitnessActiveDifficultyLevel
    var muscleGroupMap = that.data.fitnessActionMuscleGroupMap
    var equipmentTypeMap = that.data.fitnessActionEquipmentTypeMap

    var filtered = []
    for (var i = 0; i < actions.length; i++) {
      var action = actions[i]

      // Filter by muscle group
      if (activeGroup && activeGroup !== '全部') {
        var ag = muscleGroupMap[action.id] || '其他'
        if (ag !== activeGroup) continue
      }

      // Filter by equipment type
      if (activeEquipmentType) {
        var etypes = equipmentTypeMap[action.id] || []
        if (etypes.indexOf(activeEquipmentType) === -1) continue
      }

      // Filter by movement pattern
      if (activeMovementPattern) {
        if (action.movementPattern !== activeMovementPattern) continue
      }

      // Filter by difficulty level
      if (activeDifficultyLevel > 0) {
        if (action.difficultyLevel !== activeDifficultyLevel) continue
      }

      filtered.push(action)
    }

    that.setData({ fitnessFilteredActions: filtered })
  },

  // ── Navigate to action detail ──
  navigateToActionDetail: function (e) {
    var actionId = e.currentTarget.dataset.id
    wx.navigateTo({
      url: '/pages/action-detail/action-detail?actionId=' + actionId,
    })
  },

  // ── Share ──
  onShareAppMessage: function () {
    return {
      title: 'Fit 健身打卡',
      path: '/pages/index/index',
    }
  },
})