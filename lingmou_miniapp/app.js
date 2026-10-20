// app.js - 灵枢智慧银行小程序入口
App({
  globalData: {
    // API基础地址（Docker 部署：后端 18091，AI 18092；本机直跑时改回 8080/8000）
    baseUrl: 'http://localhost:18091',
    aiBaseUrl: 'http://localhost:18092',
    // 用户登录状态
    token: '',
    userInfo: null,
    // 是否老年模式
    elderlyMode: false,
  },

  onLaunch() {
    // 从本地存储恢复登录状态
    const token = wx.getStorageSync('token')
    const userInfo = wx.getStorageSync('userInfo')
    if (token) {
      this.globalData.token = token
    }
    if (userInfo) {
      this.globalData.userInfo = userInfo
    }
  },
})
