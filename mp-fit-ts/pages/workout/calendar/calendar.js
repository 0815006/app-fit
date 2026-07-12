var api = require('../../../utils/request')

/** 最早追溯到的年月 */
var EPOCH_YEAR = 2024
var EPOCH_MONTH = 1

Page({
  data: {
    loading: true,
    loadingMore: false,
    monthBlocks: [],    // [{ year, month, label, days: [{ day, date, checked }] }]
    checkedDates: {},   // { "2026-07-12": true }
    totalDays: 0,       // 总打卡天数
    scrollTop: 0,       // scroll-view 滚动位置，默认滚到底部
    canvasWidth: 375,
    canvasHeight: 800
  },

  /** 当前已展示的最早月份游标 */
  earliestYear: 0,
  earliestMonth: 0,
  /** 初始加载量 */
  initLoadCount: 12,

  onLoad: function () {
    this.loadCheckinDates()
  },

  loadCheckinDates: function () {
    var that = this
    this.setData({ loading: true })

    api.get('/gym-workout/checkin-dates')
      .then(function (res) {
        var dates = res.data || []
        var checkedMap = {}
        dates.forEach(function (d) {
          checkedMap[d] = true
        })

        var blocks = that.buildInitialMonths(checkedMap, that.initLoadCount)
        var today = that.formatDate(new Date())
        var todayChecked = !!checkedMap[today]

        that.setData({
          loading: false,
          checkedDates: checkedMap,
          monthBlocks: blocks,
          totalDays: dates.length,
          todayStr: today,
          todayChecked: todayChecked
        })

        // 滚动到底部（最近月份），用一个极大值确保滚到底
        setTimeout(function () {
          that.setData({ scrollTop: 99999 })
        }, 300)
      })
      .catch(function () {
        var blocks = that.buildInitialMonths({}, that.initLoadCount)
        that.setData({
          loading: false,
          monthBlocks: blocks,
          totalDays: 0,
          todayStr: that.formatDate(new Date()),
          todayChecked: false
        })
      })
  },

  formatDate: function (d) {
    var y = d.getFullYear()
    var m = (d.getMonth() + 1)
    var dd = d.getDate()
    if (m < 10) m = '0' + m
    if (dd < 10) dd = '0' + dd
    return y + '-' + m + '-' + dd
  },

  /**
   * 构建初始 N 个月的日历块
   */
  buildInitialMonths: function (checkedMap, count) {
    var that = this
    var now = new Date()
    var blocks = []
    var monthLabels = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']

    for (var i = count - 1; i >= 0; i--) {
      blocks.push(that.makeMonthBlock(now.getFullYear(), now.getMonth() - i, checkedMap, monthLabels))
    }

    // 记录最早月份游标
    var oldest = blocks[0]
    that.earliestYear = oldest.year
    that.earliestMonth = oldest.month

    return blocks
  },

  makeMonthBlock: function (baseYear, offsetMonth, checkedMap, monthLabels) {
    var d = new Date(baseYear, offsetMonth, 1)
    var year = d.getFullYear()
    var month = d.getMonth() // 0-based
    var daysInMonth = new Date(year, month + 1, 0).getDate()
    var label = year + '年' + (monthLabels || ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'])[month]
    if (!monthLabels) monthLabels = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']

    var days = []
    var firstDayOfWeek = new Date(year, month, 1).getDay()

    // 填充前置空白格
    for (var p = 0; p < firstDayOfWeek; p++) {
      days.push({ day: '', date: '', checked: false, empty: true })
    }

    // 填充当月每一天
    for (var dd2 = 1; dd2 <= daysInMonth; dd2++) {
      var mm = month + 1
      var mmStr = mm < 10 ? '0' + mm : '' + mm
      var ddStr = dd2 < 10 ? '0' + dd2 : '' + dd2
      var dateStr = year + '-' + mmStr + '-' + ddStr
      days.push({
        day: dd2,
        date: dateStr,
        checked: !!checkedMap[dateStr],
        empty: false
      })
    }

    return {
      year: year,
      month: month,
      label: label,
      days: days
    }
  },

  handleScrollUpper: function () {
    var that = this
    if (this.data.loadingMore) return

    // 已到纪元边界
    if (that.earliestYear <= EPOCH_YEAR && that.earliestMonth <= EPOCH_MONTH) return

    this.setData({ loadingMore: true })

    var monthLabels = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']
    var newBlocks = []
    var y = that.earliestYear
    var m = that.earliestMonth

    for (var i = 0; i < 6; i++) {
      m--
      if (m < 0) {
        m = 11
        y--
      }
      if (y < EPOCH_YEAR || (y === EPOCH_YEAR && m < EPOCH_MONTH)) break
      newBlocks.unshift(that.makeMonthBlock(y, m, that.data.checkedDates, monthLabels))
    }

    if (newBlocks.length > 0) {
      var allBlocks = newBlocks.concat(that.data.monthBlocks)
      that.setData({
        monthBlocks: allBlocks,
        loadingMore: false
      })
      that.earliestYear = newBlocks[0].year
      that.earliestMonth = newBlocks[0].month
    } else {
      that.setData({ loadingMore: false })
    }
  },

  /** 点击分享卡片：Canvas 绘制 → 保存图片 → 分享 */
  handleShare: function () {
    var that = this

    wx.showLoading({ title: '生成中...' })

    // 计算 Canvas 实际尺寸
    var dpr = wx.getWindowInfo().pixelRatio || 2
    var cw = 375
    var ch = 800
    var blocks = that.data.monthBlocks
    var monthCount = blocks.length
    // 一行2月，预估高度
    var rows = Math.ceil(monthCount / 2)
    ch = 160 + rows * 240 + 80
    if (ch < 800) ch = 800

    that.setData({
      canvasWidth: cw,
      canvasHeight: ch
    })

    // 等 setData 生效后再绘制
    setTimeout(function () {
      that.drawShareImage(cw, ch, dpr)
    }, 200)
  },

  drawShareImage: function (w, h, dpr) {
    var that = this
    var query = wx.createSelectorQuery()
    query.select('#shareCanvas')
      .fields({ node: true, size: true })
      .exec(function (res) {
        if (!res || !res[0]) {
          wx.hideLoading()
          wx.showToast({ title: '生成失败，请重试', icon: 'none' })
          return
        }

        var canvas = res[0].node
        var ctx = canvas.getContext('2d')
        canvas.width = w * dpr
        canvas.height = h * dpr
        ctx.scale(dpr, dpr)

        // 背景
        ctx.fillStyle = '#f5f5f5'
        ctx.fillRect(0, 0, w, h)

        // 标题
        ctx.fillStyle = '#303133'
        ctx.font = 'bold 18px sans-serif'
        ctx.textAlign = 'center'
        ctx.fillText('🏋️ 我的健身打卡记录', w / 2, 36)

        // 统计
        ctx.fillStyle = '#606266'
        ctx.font = '13px sans-serif'
        ctx.fillText(that.data.todayStr + '  累计打卡 ' + that.data.totalDays + ' 天', w / 2, 62)

        // 月份日历块
        var blocks = that.data.monthBlocks
        var startY = 90
        var colGap = 12
        var rowGap = 14
        var blockW = (w - 24 - colGap) / 2
        var cellSize = (blockW - 20) / 7
        // 使 cellSize 为整数
        cellSize = Math.floor(cellSize)
        blockW = cellSize * 7 + 20

        var x0 = 12
        var x1 = 12 + blockW + colGap
        var y = startY

        for (var i = 0; i < blocks.length; i++) {
          var col = i % 2
          var bx = col === 0 ? x0 : x1
          if (col === 0 && i > 0) y += blockH + rowGap

          var block = blocks[i]
          var blockH = 24 + 16 + cellSize * Math.ceil(block.days.length / 7) + 12

          // 白色卡片背景
          ctx.fillStyle = '#ffffff'
          ctx.beginPath()
          var r = 8
          ctx.moveTo(bx + r, y)
          ctx.lineTo(bx + blockW - r, y)
          ctx.quadraticCurveTo(bx + blockW, y, bx + blockW, y + r)
          ctx.lineTo(bx + blockW, y + blockH - r)
          ctx.quadraticCurveTo(bx + blockW, y + blockH, bx + blockW - r, y + blockH)
          ctx.lineTo(bx + r, y + blockH)
          ctx.quadraticCurveTo(bx, y + blockH, bx, y + blockH - r)
          ctx.lineTo(bx, y + r)
          ctx.quadraticCurveTo(bx, y, bx + r, y)
          ctx.closePath()
          ctx.fill()

          // 月份标签
          ctx.fillStyle = '#303133'
          ctx.font = 'bold 11px sans-serif'
          ctx.textAlign = 'left'
          ctx.fillText(block.label, bx + 10, y + 18)

          // 星期头
          var weekDays = ['日', '一', '二', '三', '四', '五', '六']
          ctx.fillStyle = '#909399'
          ctx.font = '8px sans-serif'
          ctx.textAlign = 'center'
          for (var wd = 0; wd < 7; wd++) {
            ctx.fillText(weekDays[wd], bx + 10 + cellSize * wd + cellSize / 2, y + 36)
          }

          // 日期圆点
          var dotY = y + 42
          for (var d = 0; d < block.days.length; d++) {
            var cell = block.days[d]
            if (cell.empty) continue
            var cx = bx + 10 + (d % 7) * cellSize + cellSize / 2
            var cy = dotY + Math.floor(d / 7) * cellSize + cellSize / 2
            var radius = cellSize * 0.4

            ctx.beginPath()
            ctx.arc(cx, cy, radius, 0, Math.PI * 2)
            ctx.fillStyle = cell.checked ? '#67c23a' : '#f0f0f0'
            ctx.fill()

            // 日期数字
            if (cell.day) {
              ctx.fillStyle = cell.checked ? '#ffffff' : '#c0c4cc'
              ctx.font = '8px sans-serif'
              ctx.textAlign = 'center'
              ctx.textBaseline = 'middle'
              ctx.fillText('' + cell.day, cx, cy)
            }
          }

          // 保存每个 block 的高度用于下一行计算
          if (col === 0) {
            blockH = 24 + 16 + cellSize * Math.ceil(block.days.length / 7) + 12
          }
        }

        // 底部水印
        ctx.fillStyle = '#c0c4cc'
        ctx.font = '10px sans-serif'
        ctx.textAlign = 'center'
        ctx.fillText('Fit 个人健身 · 口袋健身', w / 2, h - 20)

        // 导出图片
        wx.canvasToTempFilePath({
          canvas: canvas,
          success: function (imgRes) {
            wx.hideLoading()
            // 先保存到相册再分享
            wx.saveImageToPhotosAlbum({
              filePath: imgRes.tempFilePath,
              success: function () {
                wx.showToast({ title: '已保存到相册', icon: 'success' })
              },
              fail: function (err) {
                if (err.errMsg.indexOf('auth deny') >= 0) {
                  wx.showToast({ title: '请允许保存到相册', icon: 'none' })
                }
              }
            })
          },
          fail: function () {
            wx.hideLoading()
            wx.showToast({ title: '生成失败', icon: 'none' })
          }
        }, that)
      })
  }
})
