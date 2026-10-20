// pages/overview/overview.js
const { get } = require('../../utils/request')
const { getAIReply } = require('../../utils/constants')
const auth = require('../../utils/auth')

Page({
  data: {
    elderlyMode: false,
    loading: false,
    // 统计卡片
    stats: {
      totalPreForms: 0,
      totalAppointments: 0,
      completedCount: 0,
      creditScore: 100,
    },
    // 热力图
    heatmap: [],
    heatmapLoading: true,
    timeLabels: ['09:00', '09:30', '10:00', '10:30', '11:00', '11:30', '12:00', '12:30'],
    branchLabels: ['北京分行', '中关村', '国贸CBD', '三里屯', '金融街', '望京SOHO'],
    // AI诊断
    diagnosisText: '',
    diagnosisLoading: true,
    typingIndex: 0,
    typingTimer: null,
    // 饼图数据
    pieData: [
      { label: '已完成', value: 0, color: '#10b981' },
      { label: '进行中', value: 0, color: '#3b82f6' },
      { label: '已取消', value: 0, color: '#94a3b8' },
    ],
    // 折线图数据
    lineData: [],
    // 雷达图数据
    radarData: [
      { label: '办理效率', value: 85 },
      { label: '服务态度', value: 90 },
      { label: '等待时间', value: 75 },
      { label: '业务覆盖', value: 92 },
      { label: '安全等级', value: 95 },
    ],
    // 柱状图数据
    barData: [
      { label: '取现', value: 0, color: '#f59e0b' },
      { label: '开户', value: 0, color: '#10b981' },
      { label: '转账', value: 0, color: '#6366f1' },
      { label: '缴款', value: 0, color: '#f97316' },
      { label: '外汇', value: 0, color: '#a855f7' },
    ],
    // 最近交易
    recentTransactions: [],
  },

  onLoad() {
    const elderlyMode = wx.getStorageSync('elderlyMode') || false
    this.setData({ elderlyMode })
  },

  onShow() {
    // 检查登录状态
    if (!auth.isLoggedIn()) {
      wx.redirectTo({ url: '/pages/login/login' })
      return
    }
    this.loadAll()
  },

  onUnload() {
    if (this.data.typingTimer) {
      clearTimeout(this.data.typingTimer)
    }
  },

  async loadAll() {
    this.setData({ loading: true })
    await Promise.all([
      this.loadOverviewCards(),
      this.loadHeatmap(),
      this.loadDiagnosis(),
      this.loadAppointments(),
    ]).catch(() => {})
    this.setData({ loading: false })
  },

  // 统计卡片 + 总览数据
  async loadOverviewCards() {
    try {
      const data = await get('/api/overview')
      if (data) {
        this.setData({
          'stats.totalPreForms': data.totalPreForms || 0,
          'stats.totalAppointments': data.totalAppointments || 0,
          'stats.completedCount': data.completedCount || 0,
          'stats.creditScore': data.creditScore || 100,
        })
      }
    } catch (e) {
      // 降级：使用默认值
    }

    // 同时拉取信誉分
    try {
      const credit = await get('/api/credit')
      if (credit && credit.score) {
        this.setData({ 'stats.creditScore': credit.score })
      }
    } catch (e) {}
  },

  // 热力图
  async loadHeatmap() {
    this.setData({ heatmapLoading: true })
    try {
      const app = getApp()
      const aiBaseUrl = (app && app.globalData.aiBaseUrl) || 'http://localhost:18092'
      const token = auth.isLoggedIn() ? wx.getStorageSync('token') : ''
      const res = await new Promise((resolve, reject) => {
        wx.request({
          url: aiBaseUrl + '/api/ai/heatmap?date=today',
          method: 'GET',
          header: {
            'Content-Type': 'application/json',
            ...(token ? { Authorization: 'Bearer ' + token } : {}),
          },
          timeout: 10000,
          success(r) { resolve(r) },
          fail(e) { reject(e) },
        })
      })

      if (res.statusCode === 200 && res.data && res.data.code === 0 && res.data.data) {
        // 后端返回 { rows: [{branch, cells:[{val,bg,textClass}]}] }，小程序渲染用二维数值网格
        const rows = res.data.data.rows
        if (Array.isArray(rows) && rows.length > 0 && Array.isArray(rows[0].cells)) {
          const grid = rows.map(r => r.cells.map(c => c.val))
          this.setData({ heatmap: grid, heatmapLoading: false })
          return
        }
      }
      throw new Error('fallback')
    } catch (e) {
      // 本地降级
      const { buildHeatmap } = require('../../utils/constants')
      this.setData({ heatmap: buildHeatmap(), heatmapLoading: false })
    }
  },

  // AI诊断
  async loadDiagnosis() {
    this.setData({ diagnosisLoading: true, diagnosisText: '' })

    // 获取userId
    const userInfo = wx.getStorageSync('userInfo')
    const userId = userInfo ? userInfo.id : 1

    try {
      const app = getApp()
      const aiBaseUrl = (app && app.globalData.aiBaseUrl) || 'http://localhost:18092'
      const token = wx.getStorageSync('token')
      const res = await new Promise((resolve, reject) => {
        wx.request({
          url: aiBaseUrl + '/api/ai/diagnosis?userId=' + userId,
          method: 'GET',
          header: {
            'Content-Type': 'application/json',
            ...(token ? { Authorization: 'Bearer ' + token } : {}),
          },
          timeout: 10000,
          success(r) { resolve(r) },
          fail(e) { reject(e) },
        })
      })

      if (res.statusCode === 200 && res.data && res.data.code === 0 && res.data.data) {
        const text = res.data.data.text || res.data.data.diagnosis || JSON.stringify(res.data.data)
        this.startTyping(text)
        return
      }
      throw new Error('fallback')
    } catch (e) {
      // 本地降级
      const fallback = '【AI智能诊断】根据您的业务画像分析：本月办理效率优于85%用户，信誉分保持优秀水平。建议优先选择金融街或望京SOHO网点（畅通状态），预计等待时间3-5分钟。近期外汇汇率波动较大，如有外汇需求建议提前预约。'
      this.startTyping(fallback)
    }
  },

  // 打字机效果
  startTyping(text) {
    this.setData({ diagnosisLoading: false, diagnosisText: '', typingIndex: 0 })
    let idx = 0
    const timer = setInterval(() => {
      if (idx < text.length) {
        this.setData({
          diagnosisText: text.substring(0, idx + 1),
          typingIndex: idx,
        })
        idx++
      } else {
        clearInterval(timer)
      }
    }, 40)
    this.setData({ typingTimer: timer })
  },

  // 预约列表
  async loadAppointments() {
    try {
      const data = await get('/api/appointments/my')
      const list = Array.isArray(data) ? data : (data && data.list) || []

      // 统计饼图
      let completed = 0, active = 0, canceled = 0
      const bizCount = { cash_reserve: 0, open_card: 0, corp_transfer: 0, cash_deposit: 0, fx_exchange: 0 }
      const recent = []

      list.forEach(a => {
        const status = (a.status || '').toLowerCase()
        if (status === 'completed') completed++
        else if (status === 'canceled' || status === 'expired') canceled++
        else if (status !== 'virtual') active++

        const bizType = (a.businessType || '').toLowerCase()
        if (bizCount[bizType] !== undefined) bizCount[bizType]++

        if (recent.length < 5) {
          recent.push({
            id: a.id,
            voucherNum: a.voucherNum || '',
            businessType: a.businessType || '',
            status: status,
            createdAt: a.createdAt || a.appointmentTime || '',
          })
        }
      })

      this.setData({
        'pieData[0].value': completed,
        'pieData[1].value': active,
        'pieData[2].value': canceled,
        'barData[0].value': bizCount.cash_reserve,
        'barData[1].value': bizCount.open_card,
        'barData[2].value': bizCount.corp_transfer,
        'barData[3].value': bizCount.cash_deposit,
        'barData[4].value': bizCount.fx_exchange,
        recentTransactions: recent,
      })
    } catch (e) {
      // 降级使用空数据
    }
  },

  // 热力图颜色
  getHeatColor(val) {
    if (val >= 80) return '#dc2626'
    if (val >= 60) return '#f97316'
    if (val >= 40) return '#f59e0b'
    if (val >= 20) return '#84cc16'
    return '#22c55e'
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.loadAll().finally(() => {
      wx.stopPullDownRefresh()
    })
  },

  // 跳转到预约排队
  goToNavigation() {
    wx.switchTab({ url: '/pages/navigation/navigation' })
  },

  // 跳转到预填单
  goToPreForm() {
    wx.switchTab({ url: '/pages/preform/preform' })
  },

  // 跳转到AI助手
  goToAI() {
    wx.switchTab({ url: '/pages/ai-assistant/ai-assistant' })
  },

  // 跳转到进度追踪
  goToProgress() {
    wx.switchTab({ url: '/pages/progress/progress' })
  },
})
