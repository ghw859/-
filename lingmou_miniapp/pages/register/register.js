// pages/register/register.js
const auth = require('../../utils/auth')

Page({
  data: {
    name: '',
    idCard: '',
    phone: '',
    password: '',
    confirmPassword: '',
    loading: false,
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [field]: e.detail.value })
  },

  validate() {
    const { name, idCard, phone, password, confirmPassword } = this.data

    if (!name.trim()) return '请输入姓名'
    if (!/^\d{17}[\dXx]$/.test(idCard)) return '身份证号格式不正确（18位）'
    if (!/^1\d{10}$/.test(phone)) return '手机号格式不正确（11位）'
    if (password.length < 6 || password.length > 20) return '密码长度需6-20位'
    if (password !== confirmPassword) return '两次密码不一致'

    return null
  },

  async handleRegister() {
    const err = this.validate()
    if (err) {
      wx.showToast({ title: err, icon: 'none' })
      return
    }

    const { name, idCard, phone, password } = this.data
    this.setData({ loading: true })

    try {
      // 注册时用手机号作为用户名（与前端一致）
      await auth.register({
        username: phone,
        password,
        realName: name,
        idCard,
        phone,
      })
      wx.showToast({ title: '注册成功', icon: 'success' })

      // 自动登录
      try {
        const data = await auth.login(phone, password)
        if (data && data.token) {
          auth.saveAuth(data.token, data.userInfo || { username: phone })
          setTimeout(() => {
            wx.switchTab({ url: '/pages/overview/overview' })
          }, 800)
        } else {
          setTimeout(() => {
            wx.redirectTo({ url: '/pages/login/login' })
          }, 1000)
        }
      } catch (loginErr) {
        setTimeout(() => {
          wx.redirectTo({ url: '/pages/login/login' })
        }, 1000)
      }
    } catch (e) {
      wx.showToast({ title: e.message || '注册失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  goBack() {
    wx.navigateBack()
  },
})
