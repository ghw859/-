// pages/history/history.js
const { get } = require('../../utils/request')
const auth = require('../../utils/auth')

const BIZ_TYPES = [
  { value: 'all', label: '全部类型' },
  { value: 'cash_reserve', label: '大额取现预约' },
  { value: 'open_card', label: '办卡开户' },
  { value: 'corp_transfer', label: '对公跨行转账汇款' },
  { value: 'cash_deposit', label: '对公现金缴款' },
  { value: 'fx_exchange', label: '外币兑换' },
]

Page({
  data: {
    elderlyMode: false,
    loading: true,
    // 区块链校验
    chainIntact: null,
    chainLoading: true,
    // 审计日志
    auditLogs: [],
    // 筛选后的列表（WXML 渲染此字段）
    filteredLogs: [],
    // 筛选
    filterBizType: 'all',
    filterStatus: 'all',
    filterTime: 'all',
    bizTypes: BIZ_TYPES,
    timeOptions: [
      { value: 'all', label: '全部时间' },
      { value: 'today', label: '今天' },
      { value: 'week', label: '近一周' },
      { value: 'month', label: '近一个月' },
    ],
    // 详情弹窗
    detailVisible: false,
    selectedLog: null,
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
    this.loadAll()
  },

  async loadAll() {
    this.setData({ loading: true, chainLoading: true })
    await Promise.all([
      this.fetchAuditLogs(),
      this.verifyChain(),
    ]).catch(() => {})
    this.setData({ loading: false, chainLoading: false })
  },

  // 拉取后端区块链存证
  async fetchAuditLogs() {
    try {
      const data = await get('/api/audit/my')
      if (Array.isArray(data)) {
        this.setData({ auditLogs: data })
        this.applyFilters()
      }
    } catch (e) {
      // 降级空列表
    }
  },

  // 链上校验
  async verifyChain() {
    try {
      const data = await get('/api/audit/my/verify')
      if (data && typeof data.intact !== 'undefined') {
        this.setData({ chainIntact: data.intact })
      }
    } catch (e) {
      this.setData({ chainIntact: null })
    }
  },

  // 筛选
  onFilterBizType(e) {
    this.setData({ filterBizType: e.currentTarget.dataset.value })
    this.applyFilters()
  },

  onFilterStatus(e) {
    this.setData({ filterStatus: e.currentTarget.dataset.value })
    this.applyFilters()
  },

  onFilterTime(e) {
    this.setData({ filterTime: e.currentTarget.dataset.value })
    this.applyFilters()
  },

  // 统一的筛选执行入口
  applyFilters() {
    const { auditLogs, filterBizType, filterStatus, filterTime } = this.data
    const filtered = (auditLogs || []).filter(log => {
      // 业务类型
      if (filterBizType !== 'all') {
        const type = log.bizType || log.businessType || ''
        if (type !== filterBizType) return false
      }
      // 状态
      if (filterStatus !== 'all' && log.status !== filterStatus) {
        return false
      }
      // 时间
      if (!this.inTimeRange(log.timestamp, filterTime)) {
        return false
      }
      return true
    })
    this.setData({ filteredLogs: filtered })
  },

  // 时间判断
  inTimeRange(timestamp, range) {
    if (range === 'all' || !timestamp) return true
    const logDate = new Date(String(timestamp).replace(/\//g, '-').replace(' ', 'T'))
    if (isNaN(logDate.getTime())) return true
    const now = Date.now()
    const diff = now - logDate.getTime()
    const oneDay = 24 * 60 * 60 * 1000
    if (range === 'today') return diff >= 0 && diff < oneDay
    if (range === 'week') return diff >= 0 && diff < 7 * oneDay
    if (range === 'month') return diff >= 0 && diff < 30 * oneDay
    return true
  },

  // 查看详情
  viewDetail(e) {
    const log = e.currentTarget.dataset.log
    this.setData({ selectedLog: log, detailVisible: true })
  },

  closeDetail() {
    this.setData({ detailVisible: false, selectedLog: null })
  },

  stopBubble() {},

  // 导出单条凭证
  exportSingle() {
    const log = this.data.selectedLog
    if (!log) return

    const content = 'ICBC · 灵枢 - 预约凭证\n\n' +
      '==================================================\n' +
      '流水号: ' + (log.sn || '') + '\n' +
      '业务类型: ' + (log.bizTypeName || log.bizType || '') + '\n' +
      '状态: ' + (log.status || '') + '\n' +
      '提交时间: ' + (log.timestamp || '') + '\n' +
      '客户姓名: ' + (log.userName || '未填写') + '\n' +
      '身份证号: ' + (log.idCardMasked || '未填写') + '\n' +
      '手机号码: ' + (log.phoneMasked || '未填写') + '\n' +
      '存证凭证号: ' + (log.hash || '') + '\n' +
      '==================================================\n' +
      '此凭证已通过金融级加密技术存证，确保数据安全防篡改。'

    const fileName = (log.sn || 'audit') + '.txt'
    const fs = wx.getFileSystemManager()
    const filePath = wx.env.USER_DATA_PATH + '/' + fileName

    fs.writeFile({
      filePath,
      data: content,
      encoding: 'utf8',
      success() {
        wx.openDocument({
          filePath,
          showMenu: true,
          fail() {
            wx.showToast({ title: '文件已生成', icon: 'success' })
          },
        })
      },
      fail() {
        wx.showToast({ title: '导出失败', icon: 'none' })
      },
    })
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.loadAll().finally(() => {
      wx.stopPullDownRefresh()
    })
  },
})
