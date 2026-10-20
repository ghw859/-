// pages/login/login.js
const auth = require('../../utils/auth')
const { clearAuth } = require('../../utils/request')

Page({
  data: {
    elderlyMode: false,
    loading: false,
    // 微信头像/昵称（通过官方 chooseAvatar + nickname 组件获取）
    avatarUrl: '',
    nickName: '',
    // 是否展开资料完善区
    profileOpen: false,
    // 登录态检查完成标记，未完成时禁用按钮，避免重复点击
    ready: false,
  },

  onLoad() {
    clearAuth()
    const elderlyMode = wx.getStorageSync('elderlyMode') || false
    const cached = wx.getStorageSync('wxUserInfo') || {}
    this.setData({
      elderlyMode,
      avatarUrl: cached.avatarUrl || '',
      nickName: cached.nickName || '',
    })
    this.setData({ ready: true })
  },

  toggleElderlyMode() {
    const newVal = !this.data.elderlyMode
    this.setData({ elderlyMode: newVal })
    wx.setStorageSync('elderlyMode', newVal)
    const app = getApp()
    if (app && app.globalData) {
      app.globalData.elderlyMode = newVal
    }
  },

  // 选择微信头像（open-type="chooseAvatar" 官方推荐方式）
  onChooseAvatar(e) {
    const avatarUrl = e.detail.avatarUrl
    this.setData({ avatarUrl })
  },

  // 昵称输入（type="nickname" 会唤起微信昵称填写面板）
  onNickNameInput(e) {
    this.setData({ nickName: e.detail.value })
  },

  toggleProfile() {
    this.setData({ profileOpen: !this.data.profileOpen })
  },

  // 设备稳定标识：wx.login 的 code 每次都会变，不能当用户唯一标识，
  // 因此首次登录生成一个持久化 deviceId，保证同一设备多次登录是同一账号。
  getDeviceId() {
    let deviceId = wx.getStorageSync('lingmou_device_id')
    if (!deviceId) {
      deviceId = 'dev_' + Date.now() + '_' + Math.random().toString(36).slice(2, 10)
      wx.setStorageSync('lingmou_device_id', deviceId)
    }
    return deviceId
  },

  // wx.login 获取临时登录凭证
  getWxLoginCode() {
    return new Promise((resolve) => {
      wx.login({
        success: (res) => resolve(res.code || ''),
        fail: () => resolve(''),
      })
    })
  },

  // 微信一键登录
  async handleWxLogin() {
    if (this.data.loading || !this.data.ready) return
    this.setData({ loading: true })

    try {
      // 1. 获取 wx.login 临时凭证（静默，无需用户额外授权）
      const code = await this.getWxLoginCode()

      // 2. 设备稳定标识
      const deviceId = this.getDeviceId()

      // 3. 用户已选择的头像昵称（可选）
      const nickName = this.data.nickName || '微信用户'
      const avatarUrl = this.data.avatarUrl || ''

      // 4. 调用后端微信登录（首次自动创建账号）
      const loginData = await auth.wxLogin({ nickName, avatarUrl, code, deviceId })

      if (loginData && loginData.token) {
        const userInfo = loginData.userInfo || { username: nickName }
        auth.saveAuth(loginData.token, userInfo)
        wx.setStorageSync('wxUserInfo', { nickName, avatarUrl })

        wx.showToast({ title: '登录成功', icon: 'success' })
        setTimeout(() => {
          wx.switchTab({ url: '/pages/overview/overview' })
        }, 700)
      } else {
        wx.showToast({ title: '登录返回异常', icon: 'none' })
      }
    } catch (e) {
      const msg = e && e.message ? e.message : '登录失败'
      // 后端未启动时给出可执行的提示，而不是笼统的"网络失败"
      if (msg.indexOf('网络') >= 0 || msg.indexOf('connect') >= 0) {
        wx.showModal({
          title: '无法连接后端服务',
          content: '请确认后端已启动（Docker 部署默认 18091 端口），或检查 app.js 中的 baseUrl 配置。',
          showCancel: false,
        })
      } else {
        wx.showToast({ title: msg, icon: 'none' })
      }
    } finally {
      this.setData({ loading: false })
    }
  },

  // 跳转账号注册（保留，供已有账号体系使用）
  goToRegister() {
    wx.navigateTo({ url: '/pages/register/register' })
  },
})
