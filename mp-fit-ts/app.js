App({
  onLaunch() {
    var empNo = wx.getStorageSync('empNo')
    if (!empNo) {
      wx.setStorageSync('empNo', '0000000')
    }
    console.log('Fit 小程序启动, empNo:', wx.getStorageSync('empNo'))
  },
})
