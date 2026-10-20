// pages/navigation/navigation.js
const { get, post } = require('../../utils/request')
const { BRANCHES } = require('../../utils/constants')
const auth = require('../../utils/auth')

Page({
  data: {
    elderlyMode: false,
    loading: false,
    branches: [],
    // 筛选后的列表
    filteredList: [],
    // 筛选
    filterStatus: 'all', // all | free | moderate | busy
    filterService: 'all', // all | cash_reserve | open_card | ...
    sortBy: 'distance', // distance | waitTime
    sortIndex: 0,
    // 预约弹窗
    bookingVisible: false,
    bookingBranch: null,
    bookingDate: '',
    bookingTime: '',
    bookingBizType: '',
    bookingBizIndex: 0,
    bookingLoading: false,
    bizTypes: [
      { value: 'cash_reserve', label: '大额取现预约' },
      { value: 'open_card', label: '办卡开户' },
      { value: 'corp_transfer', label: '对公跨行转账汇款' },
      { value: 'cash_deposit', label: '对公现金缴款' },
      { value: 'fx_exchange', label: '外币兑换' },
    ],
    timeSlots: ['09:00-09:30', '09:30-10:00', '10:00-10:30', '10:30-11:00', '14:00-14:30', '14:30-15:00', '15:00-15:30', '15:30-16:00'],
    timeSlotIndex: 0,
    // 排序选项
    sortOptions: [
      { value: 'distance', label: '距离最近' },
      { value: 'waitTime', label: '等待最短' },
    ],
    // 今天日期（用于picker最小值）
    todayDate: '',
  },

  onLoad() {
    const elderlyMode = wx.getStorageSync('elderlyMode') || false
    // 生成今天日期字符串
    const now = new Date()
    const todayDate = now.getFullYear() + '-' +
      String(now.getMonth() + 1).padStart(2, '0') + '-' +
      String(now.getDate()).padStart(2, '0')

    this.setData({
      elderlyMode,
      branches: BRANCHES,
      filteredList: this.applyFilter(BRANCHES),
      todayDate,
    })
  },

  onShow() {
    if (!auth.isLoggedIn()) {
      wx.redirectTo({ url: '/pages/login/login' })
      return
    }
    this.loadBranches()
  },

  // 拉取后端网点数据
  async loadBranches() {
    this.setData({ loading: true })
    try {
      const data = await get('/api/branches')
      if (Array.isArray(data) && data.length > 0) {
        // 后端返回的数据映射到本地结构
        const mapped = data.map((b, idx) => {
          const localBranch = BRANCHES[idx] || BRANCHES[0]
          return {
            ...localBranch,
            ...b,
            id: localBranch.id,
            backendId: b.id || localBranch.backendId,
            name: b.name || localBranch.name,
            address: b.address || localBranch.address,
            distance: b.distance || localBranch.distance,
            status: this.mapStatus(b.status || b.busyFactor || localBranch.busyFactor),
            waitTime: b.waitTime || b.estimatedWait || localBranch.waitTime,
            services: b.services || localBranch.services,
          }
        })
        this.setData({
          branches: mapped,
          filteredList: this.applyFilter(mapped),
        })
      } else {
        // 后端返回空，使用本地数据
        this.setData({
          branches: BRANCHES,
          filteredList: this.applyFilter(BRANCHES),
        })
      }
    } catch (e) {
      // 降级使用本地mock数据
      this.setData({
        branches: BRANCHES,
        filteredList: this.applyFilter(BRANCHES),
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  mapStatus(busyFactor) {
    if (typeof busyFactor === 'string') return busyFactor
    if (busyFactor < 0.8) return 'free'
    if (busyFactor < 1.5) return 'moderate'
    return 'busy'
  },

  // 筛选
  onFilterStatus(e) {
    const value = e.currentTarget.dataset.value
    this.setData({
      filterStatus: value,
      filteredList: this.applyFilter(this.data.branches, value, this.data.filterService, this.data.sortBy),
    })
  },

  onFilterService(e) {
    const value = e.currentTarget.dataset.value
    this.setData({
      filterService: value,
      filteredList: this.applyFilter(this.data.branches, this.data.filterStatus, value, this.data.sortBy),
    })
  },

  onSortChange(e) {
    const idx = parseInt(e.detail.value)
    const sortBy = this.data.sortOptions[idx].value
    this.setData({
      sortIndex: idx,
      sortBy: sortBy,
      filteredList: this.applyFilter(this.data.branches, this.data.filterStatus, this.data.filterService, sortBy),
    })
  },

  // 筛选+排序逻辑
  applyFilter(list, status, service, sortBy) {
    status = status || this.data.filterStatus
    service = service || this.data.filterService
    sortBy = sortBy || this.data.sortBy

    let result = [...list]

    // 状态筛选
    if (status !== 'all') {
      result = result.filter(b => b.status === status)
    }
    // 业务筛选
    if (service !== 'all') {
      result = result.filter(b => b.services && b.services.includes(service))
    }
    // 排序
    if (sortBy === 'distance') {
      result.sort((a, b) => a.distance - b.distance)
    } else if (sortBy === 'waitTime') {
      result.sort((a, b) => a.waitTime - b.waitTime)
    }
    return result
  },

  // 打开预约弹窗
  openBooking(e) {
    const branch = e.currentTarget.dataset.branch
    this.setData({
      bookingVisible: true,
      bookingBranch: branch,
      bookingDate: '',
      bookingTime: '',
      bookingBizType: '',
      bookingBizIndex: 0,
      timeSlotIndex: 0,
    })
  },

  closeBooking() {
    this.setData({ bookingVisible: false, bookingBranch: null })
  },

  // 阻止冒泡
  stopBubble() {},

  onDateChange(e) {
    this.setData({ bookingDate: e.detail.value })
  },

  onTimeChange(e) {
    const idx = parseInt(e.detail.value)
    this.setData({
      timeSlotIndex: idx,
      bookingTime: this.data.timeSlots[idx],
    })
  },

  onBizTypeChange(e) {
    const idx = parseInt(e.detail.value)
    this.setData({
      bookingBizIndex: idx,
      bookingBizType: this.data.bizTypes[idx].value,
    })
  },

  // 确认预约
  async confirmBooking() {
    const { bookingBranch, bookingDate, bookingTime, bookingBizType } = this.data

    if (!bookingDate) {
      wx.showToast({ title: '请选择日期', icon: 'none' })
      return
    }
    if (!bookingTime) {
      wx.showToast({ title: '请选择时段', icon: 'none' })
      return
    }
    if (!bookingBizType) {
      wx.showToast({ title: '请选择业务类型', icon: 'none' })
      return
    }

    this.setData({ bookingLoading: true })

    try {
      // POST /api/appointments
      const appointmentData = {
        branchId: bookingBranch.backendId,
        businessType: bookingBizType,
        appointmentDate: bookingDate,
        timeSlot: bookingTime,
      }

      const result = await post('/api/appointments', appointmentData)

      if (result && result.id) {
        wx.showToast({ title: '预约成功', icon: 'success' })
        this.setData({ bookingVisible: false, bookingLoading: false })
        // 跳转到进度追踪
        setTimeout(() => {
          wx.switchTab({ url: '/pages/progress/progress' })
        }, 1000)
      }
    } catch (e) {
      this.setData({ bookingLoading: false })
      wx.showToast({ title: e.message || '预约失败', icon: 'none' })
    }
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.loadBranches().finally(() => {
      wx.stopPullDownRefresh()
    })
  },
})
