// pages/overview/overview.js
const { get } = require('../../utils/request')
const { getAIReply } = require('../../utils/constants')
const charts = require('../../utils/charts')
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
    // 资产趋势图范围（half=近半年 full=全年）
    chartRange: 'full',
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

  onReady() {
    this.drawAllCharts()
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

  // 预约列表（最近记录）
  async loadAppointments() {
    try {
      const data = await get('/api/appointments/my')
      const list = Array.isArray(data) ? data : (data && data.list) || []

      const recent = []
      list.forEach(a => {
        if (recent.length < 5) {
          recent.push({
            id: a.id,
            voucherNum: a.voucherNum || '',
            businessType: a.businessType || '',
            status: (a.status || '').toLowerCase(),
            createdAt: a.createdAt || a.appointmentTime || '',
          })
        }
      })

      this.setData({ recentTransactions: recent })
    } catch (e) {
      // 降级使用空数据
    }
  },

  // ============ 数据总览四图（canvas 2d） ============
  drawAllCharts() {
    // 1. 消费分类占比环形图
    charts.initCanvas(this, '#donutCanvas').then(({ ctx, width, height }) => {
      charts.drawDonut(ctx, width, height, {
        data: [
          { name: '生活缴费', value: 45, color: '#06b6d4' },
          { name: '基金理财', value: 30, color: '#3b82f6' },
          { name: '跨境消费', value: 15, color: '#10b981' },
          { name: '其他支出', value: 10, color: '#cbd5e1' },
        ],
        totalText: '￥14.2K',
        subText: '总支出',
      })
    }).catch(() => {})

    // 2. 资产趋势面积图
    this.drawAreaChart()

    // 3. AI多维账户风控雷达图
    charts.initCanvas(this, '#radarCanvas').then(({ ctx, width, height }) => {
      charts.drawRadarChart(ctx, width, height, {
        indicators: ['账户交易安全', '电信反诈健康度', '资金流动性', '跨境交易合规', '反洗钱风险等级', '预约授权合规'],
        values: [98, 96, 88, 92, 90, 95],
        max: 100,
      })
    }).catch(() => {})

    // 4. 近四季度预约业务分类办结量堆叠柱状图
    charts.initCanvas(this, '#barCanvas').then(({ ctx, width, height }) => {
      charts.drawStackedBar(ctx, width, height, {
        labels: ['一季度', '二季度', '三季度', '四季度'],
        yMax: 150,
        series: [
          { name: '个人开户/挂失', color: '#06b6d4', data: [38, 52, 30, 22] },
          { name: '理财签约/测评', color: '#3b82f6', data: [60, 38, 68, 52] },
          { name: '对公/贷款面签', color: '#6366f1', data: [30, 45, 38, 60] },
        ],
      })
    }).catch(() => {})
  },

  drawAreaChart() {
    const AREA_DATA = {
      half: {
        months: ['02月', '03月', '04月', '05月', '06月', '07月'],
        total: [318, 352, 336, 405, 446, 482.5],
        net: [296, 328, 310, 378, 418, 452],
      },
      full: {
        months: ['01月', '02月', '03月', '04月', '05月', '06月', '07月', '08月', '09月', '10月', '11月', '12月'],
        total: [268, 289, 318, 305, 348, 382, 421, 463, 528, 612, 738, 945],
        net: [252, 270, 296, 286, 324, 356, 392, 430, 488, 566, 684, 886],
      },
    }
    const d = AREA_DATA[this.data.chartRange]
    charts.initCanvas(this, '#areaCanvas').then(({ ctx, width, height }) => {
      charts.drawArea(ctx, width, height, d)
    }).catch(() => {})
  },

  // 近半年/全年切换
  toggleAreaRange(e) {
    const range = e.currentTarget.dataset.range
    if (!range || range === this.data.chartRange) return
    this.setData({ chartRange: range })
    this.drawAreaChart()
  },

  // ============ 历史预约查询（独立模块，跳转专属页面） ============
  goHistory() {
    wx.navigateTo({ url: '/pages/appt-history/appt-history' })
  },

  // ============ 模块级 AI 导航 ============
  goAINav(e) {
    const q = (e.currentTarget.dataset.q || '').trim()
    const app = getApp()
    if (app && app.globalData) {
      app.globalData.aiPresetQuestion = q
    }
    wx.switchTab({ url: '/pages/ai-assistant/ai-assistant' })
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
