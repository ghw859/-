// pages/appt-history/appt-history.js - 历史预约查询（与网页版 /history 模块一致）
const { fetchAppointmentHistory } = require('../../utils/api')
const auth = require('../../utils/auth')

const STATUS_TABS = [
  { value: 'all', label: '全部' },
  { value: 'ACTIVE', label: '待办理' },
  { value: 'COMPLETED', label: '已办结' },
  { value: 'CANCELED', label: '已取消' },
]

Page({
  data: {
    elderlyMode: false,
    tabs: STATUS_TABS,
    filter: 'all',
    list: [],
    total: 0,
    pageNum: 1,
    pageSize: 10,
    loading: false,
    finished: false,
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
    this.reload()
  },

  onFilter(e) {
    const value = e.currentTarget.dataset.value
    if (value === this.data.filter) return
    this.setData({ filter: value })
    this.reload()
  },

  reload() {
    this.setData({ list: [], pageNum: 1, finished: false })
    this.loadPage()
  },

  async loadPage() {
    if (this.data.loading || this.data.finished) return
    this.setData({ loading: true })
    try {
      const params = { pageNum: this.data.pageNum, pageSize: this.data.pageSize }
      if (this.data.filter !== 'all') params.status = this.data.filter
      const data = await fetchAppointmentHistory(params)
      const records = (data && data.records) || []
      const mapped = records.map(a => ({
        id: a.id,
        branchName: a.branchName || '',
        businessType: a.businessType || '网点业务',
        appointmentDate: a.appointmentDate || '',
        timeSlot: a.timeSlot || '',
        status: (a.status || '').toUpperCase(),
        statusText: this.statusText(a.status),
        statusClass: this.statusClass(a.status),
      }))
      const list = this.data.list.concat(mapped)
      const total = (data && data.total) || list.length
      this.setData({
        list,
        total,
        pageNum: this.data.pageNum + 1,
        finished: list.length >= total || records.length === 0,
      })
    } catch (e) {
      // 静默失败，展示空态
      this.setData({ finished: true })
    } finally {
      this.setData({ loading: false })
    }
  },

  statusText(status) {
    switch ((status || '').toUpperCase()) {
      case 'COMPLETED': return '已办结'
      case 'ACTIVE': return '待办理'
      case 'CALLED': return '叫号中'
      case 'PROCESSING': return '办理中'
      case 'CANCELED': return '已取消'
      case 'EXPIRED': return '已过期'
      default: return '已预约'
    }
  },

  statusClass(status) {
    switch ((status || '').toUpperCase()) {
      case 'COMPLETED': return 'badge-completed'
      case 'CANCELED':
      case 'EXPIRED': return 'badge-canceled'
      default: return 'badge-active'
    }
  },

  onReachBottom() {
    this.loadPage()
  },

  onPullDownRefresh() {
    this.setData({ list: [], pageNum: 1, finished: false })
    this.loadPage()
    wx.stopPullDownRefresh()
  },
})
