// pages/progress/progress.js
const { get, put, del } = require('../../utils/request')
const auth = require('../../utils/auth')

Page({
  data: {
    elderlyMode: false,
    loading: false,
    refreshing: false,
    appointments: [],
    selectedVoucher: '',
    // 当前选中的预约（WXML 不能调用函数，必须用 data 字段承载）
    currentAppt: null,
    // 5步进度
    steps: [
      { name: '取号' },
      { name: '排队中' },
      { name: '叫号' },
      { name: '办理中' },
      { name: '完成' },
    ],
    // 进度条宽度百分比
    progressPercent: 0,
    notifyOn: false,
    reviewRating: 0,
    reviewSubmitted: false,
  },

  onLoad() {
    const elderlyMode = wx.getStorageSync('elderlyMode') || false
    this.setData({ elderlyMode })
  },

  onShow() {
    if (!auth.isLoggedIn()) {
      wx.redirectTo({ url: '/pages/login/login' })
      return
    }
    this.loadAppointments()
  },

  async loadAppointments() {
    this.setData({ loading: true })
    try {
      const data = await get('/api/appointments/my')
      const list = Array.isArray(data) ? data : (data && data.list) || []
      // 状态映射
      const mapped = list.map(a => {
        const step = a.step || this.calcStep(a.status)
        return {
          ...a,
          statusLower: (a.status || '').toLowerCase(),
          step: step,
          voucherDisplay: this.voucherDisplay(a),
          statusText: this.statusText(a.status),
        }
      })

      this.setData({ appointments: mapped })

      // 默认选中第一条；已选中的若还存在则保持
      const exists = mapped.some(a => a.voucherNum === this.data.selectedVoucher)
      const selected = exists
        ? mapped.find(a => a.voucherNum === this.data.selectedVoucher)
        : mapped[0] || null

      this.setData({
        selectedVoucher: selected ? selected.voucherNum : '',
        currentAppt: selected,
        progressPercent: selected ? ((selected.step - 1) / 4) * 100 : 0,
      })
    } catch (e) {
      // 降级空列表
    } finally {
      this.setData({ loading: false })
    }
  },

  calcStep(status) {
    const s = (status || '').toLowerCase()
    if (s === 'virtual') return 1
    if (s === 'active') return 2
    if (s === 'called') return 3
    if (s === 'processing') return 4
    if (s === 'completed') return 5
    return 1
  },

  statusText(status) {
    const s = (status || '').toLowerCase()
    if (s === 'virtual') return '虚拟号'
    if (s === 'active') return '激活号'
    if (s === 'called') return '已叫号'
    if (s === 'processing') return '办理中'
    if (s === 'completed') return '已完成'
    if (s === 'canceled') return '已取消'
    if (s === 'expired') return '已失效'
    return '进行中'
  },

  voucherDisplay(a) {
    const s = (a.status || '').toLowerCase()
    const num = (a.voucherNum || '').slice(-3)
    if (s === 'active') return 'A' + num
    if (s === 'completed') return 'C' + num
    return 'V' + num
  },

  selectAppt(e) {
    const voucherNum = e.currentTarget.dataset.voucher
    const appt = this.data.appointments.find(a => a.voucherNum === voucherNum) || null
    this.setData({
      selectedVoucher: voucherNum,
      currentAppt: appt,
      progressPercent: appt ? ((appt.step - 1) / 4) * 100 : 0,
      reviewSubmitted: false,
      reviewRating: 0,
    })
  },

  // 激活虚拟号
  async activateFromProgress() {
    const appt = this.data.currentAppt
    if (!appt) return
    try {
      await put('/api/appointments/' + appt.id + '/progress', { action: 'activate' })
      wx.showToast({ title: '已激活', icon: 'success' })
      this.loadAppointments()
    } catch (e) {
      wx.showToast({ title: e.message || '激活失败', icon: 'none' })
    }
  },

  // 推进一步
  async stepForward() {
    const appt = this.data.currentAppt
    if (!appt) return
    try {
      await put('/api/appointments/' + appt.id + '/progress', { action: 'step' })
      wx.showToast({ title: '已推进', icon: 'success' })
      this.loadAppointments()
    } catch (e) {
      wx.showToast({ title: e.message || '推进失败', icon: 'none' })
    }
  },

  // 取消预约
  async cancelAppt() {
    const appt = this.data.currentAppt
    if (!appt) return

    wx.showModal({
      title: '取消预约',
      content: '确定取消此预约吗？取消将扣除信誉分5分',
      success: async (res) => {
        if (res.confirm) {
          try {
            await del('/api/appointments/' + appt.id)
            wx.showToast({ title: '已取消', icon: 'success' })
            this.loadAppointments()
          } catch (e) {
            wx.showToast({ title: e.message || '取消失败', icon: 'none' })
          }
        }
      },
    })
  },

  // 刷新
  async refresh() {
    this.setData({ refreshing: true })
    await this.loadAppointments()
    this.setData({ refreshing: false })
  },

  // 刷新单条
  async refreshOne() {
    const appt = this.data.currentAppt
    if (!appt || !appt.id) return
    try {
      await get('/api/appointments/' + appt.id)
      this.loadAppointments()
      wx.showToast({ title: '已刷新', icon: 'success' })
    } catch (e) {
      wx.showToast({ title: e.message || '刷新失败', icon: 'none' })
    }
  },

  toggleNotify() {
    this.setData({ notifyOn: !this.data.notifyOn })
  },

  // 评分
  setRating(e) {
    const rating = Number(e.currentTarget.dataset.rating)
    this.setData({ reviewRating: rating })
  },

  submitReview() {
    if (this.data.reviewRating === 0) {
      wx.showToast({ title: '请选择评分', icon: 'none' })
      return
    }
    this.setData({ reviewSubmitted: true })
    wx.showToast({ title: '感谢评价', icon: 'success' })
  },

  goToBooking() {
    wx.switchTab({ url: '/pages/navigation/navigation' })
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.loadAppointments().finally(() => {
      wx.stopPullDownRefresh()
    })
  },
})
