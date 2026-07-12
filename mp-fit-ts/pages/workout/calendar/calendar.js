var api = require('../../../utils/request')

/** 日历起始：2026年1月 */
var START_YEAR = 2026
var START_MONTH = 0

Page({
  data: {
    loading: true,
    loadingMore: false,
    monthBlocks: [],    // [{ year, month, label, days: [{ day, date, checked }] }]
    checkedDates: {},   // { "2026-07-12": true }
    totalDays: 0,       // 总打卡天数
    canvasWidth: 375,
    canvasHeight: 800
  },

  /** 当前已展示的最早月份游标 */
  earliestYear: 0,
  earliestMonth: 0,

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

        var blocks = that.buildInitialMonths(checkedMap)
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
      })
      .catch(function () {
        var blocks = that.buildInitialMonths({})
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
   * 构建初始日历块：从 2026年1月 到 当前月份
   */
  buildInitialMonths: function (checkedMap) {
    var that = this
    var now = new Date()
    var currentYear = now.getFullYear()
    var currentMonth = now.getMonth() // 0-based

    // 月数 = 从2026-01到当前月的跨度 + 1
    var totalMonths = (currentYear - START_YEAR) * 12 + (currentMonth - START_MONTH) + 1

    var blocks = []
    var monthLabels = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']

    for (var i = totalMonths - 1; i >= 0; i--) {
      blocks.push(that.makeMonthBlock(currentYear, currentMonth - i, checkedMap, monthLabels))
    }

    // 记录最早月份游标（即2026-01）
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

    // 已到起始边界（不再向前加载）
    if (that.earliestYear <= START_YEAR && that.earliestMonth <= START_MONTH) return

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
      if (y < START_YEAR || (y === START_YEAR && m < START_MONTH)) break
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

  /** 点击分享浮动按钮：Canvas 绘制 → 保存图片 → 分享 */
  handleShare: function () {
    var that = this

    wx.showLoading({ title: '生成中...' })

    // 取最近 6 个月用于分享卡片
    var allBlocks = that.data.monthBlocks
    var shareMonths = allBlocks.slice(-6)

    // 等数据就绪后再绘制
    setTimeout(function () {
      that.drawShareImage(shareMonths)
    }, 200)
  },

  drawShareImage: function (shareMonths) {
    var that = this
    var dpr = wx.getWindowInfo().pixelRatio || 2
    var w = 375

    // 计算实际需要的像素参数
    var colGap = 10
    var padH = 12                         // 左右外边距
    var blockW = (w - padH * 2 - colGap) / 2
    var cellSize = Math.floor((blockW - 20) / 7)
    blockW = cellSize * 7 + 20            // 整像素对齐
    var x0 = padH
    var x1 = padH + blockW + colGap
    var rowGap = 12
    var headerH = 80                      // 标题+统计区域高度
    var footerH = 40                      // 底部水印高度
    var rows = Math.ceil(shareMonths.length / 2)

    // 预计算每行 block 高度
    var rowHeights = []
    for (var ri = 0; ri < rows; ri++) {
      var b1 = shareMonths[ri * 2]
      var b2 = shareMonths[ri * 2 + 1]
      var h1 = b1 ? (24 + 16 + cellSize * Math.ceil(b1.days.length / 7) + 12) : 0
      var h2 = b2 ? (24 + 16 + cellSize * Math.ceil(b2.days.length / 7) + 12) : 0
      rowHeights.push(Math.max(h1, h2))
    }
    var h = headerH + footerH
    for (var j = 0; j < rowHeights.length; j++) {
      h += rowHeights[j] + (j > 0 ? rowGap : 0)
    }

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

        // 背景：白色
        ctx.fillStyle = '#ffffff'
        ctx.fillRect(0, 0, w, h)

        // 标题
        ctx.fillStyle = '#303133'
        ctx.font = 'bold 18px sans-serif'
        ctx.textAlign = 'center'
        ctx.fillText('🏋️ 我的健身打卡记录', w / 2, 34)

        // 统计
        ctx.fillStyle = '#909399'
        ctx.font = '12px sans-serif'
        ctx.fillText(that.data.todayStr + '  累计打卡 ' + that.data.totalDays + ' 天', w / 2, 58)

        // 月份日历块
        var y = headerH
        for (var i = 0; i < shareMonths.length; i++) {
          var col = i % 2
          var bx = col === 0 ? x0 : x1
          if (col === 0 && i > 0) y += rowHeights[Math.floor(i / 2) - 1] + rowGap

          var block = shareMonths[i]
          var blockH = 24 + 16 + cellSize * Math.ceil(block.days.length / 7) + 12

          // 白色卡片背景
          ctx.fillStyle = '#fafafa'
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
          ctx.fillStyle = '#c0c4cc'
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

            if (cell.day) {
              ctx.fillStyle = cell.checked ? '#ffffff' : '#c0c4cc'
              ctx.font = '8px sans-serif'
              ctx.textAlign = 'center'
              ctx.textBaseline = 'middle'
              ctx.fillText('' + cell.day, cx, cy)
            }
          }
        }

        // 底部水印
        ctx.fillStyle = '#c0c4cc'
        ctx.font = '10px sans-serif'
        ctx.textAlign = 'center'
        ctx.fillText('Fit 个人健身 · 口袋健身', w / 2, h - 14)

        // 导出图片
        wx.canvasToTempFilePath({
          canvas: canvas,
          success: function (imgRes) {
            wx.hideLoading()
            wx.saveImageToPhotosAlbum({
              filePath: imgRes.tempFilePath,
              success: function () {
                wx.showToast({ title: '已保存到相册', icon: 'success' })
              },
              fail: function (e) {
                if (e.errMsg.indexOf('auth deny') >= 0) {
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
