// pages/qrcode/qrcode.js
const { post } = require('../../utils/request')
const auth = require('../../utils/auth')

Page({
  data: {
    elderlyMode: false,
    genState: 'idle', // idle | loading | success | rejected | error
    passCode: null,
    // 二维码绝对地址（小程序 <image> 不支持相对路径）
    qrImageUrl: '',
    qrImageError: false,
    pendingCtx: null,
    missingLabels: [],
    errorMsg: '',
    countdown: 30 * 60,
    countdownText: '30:00',
    countdownTimer: null,
    // 推荐网点
    recommendedBranches: [],
  },

  onLoad() {
    const elderlyMode = wx.getStorageSync('elderlyMode') || false
    this.setData({ elderlyMode })

    // 恢复上次签发的直通码
    const lastRaw = wx.getStorageSync('lingmou_last_qrcode')
    if (lastRaw) {
      try {
        const last = typeof lastRaw === 'string' ? JSON.parse(lastRaw) : lastRaw
        this.setData({
          passCode: last,
          genState: 'success',
          qrImageUrl: this.buildQrUrl(last),
        })
        this.restartCountdown()
      } catch (e) {}
    }

    // 消费预填单交接的上下文
    const pendingRaw = wx.getStorageSync('lingmou_pending_qrcode')
    if (pendingRaw) {
      wx.removeStorageSync('lingmou_pending_qrcode')
      const pending = typeof pendingRaw === 'string' ? JSON.parse(pendingRaw) : pendingRaw
      this.setData({ pendingCtx: pending })
      this.generateCode()
    }

    // 加载推荐网点
    this.loadRecommendedBranches()
  },

  onShow() {
    if (!auth.isLoggedIn()) {
      wx.redirectTo({ url: '/pages/login/login' })
      return
    }
  },

  onUnload() {
    if (this.data.countdownTimer) {
      clearInterval(this.data.countdownTimer)
    }
  },

  loadRecommendedBranches() {
    const { BRANCHES } = require('../../utils/constants')
    const order = { free: 0, moderate: 1, busy: 2 }
    const sorted = [...BRANCHES].sort((a, b) => order[a.status] - order[b.status]).slice(0, 4)
    this.setData({ recommendedBranches: sorted })
  },

  // 拼接二维码绝对地址
  buildQrUrl(passCode) {
    if (!passCode) return ''
    const app = getApp()
    const baseUrl = (app && app.globalData.baseUrl) || 'http://localhost:18091'
    const path = passCode.qrcodeImageUrl || ('/api/qrcode/' + passCode.voucherNum + '/image')
    if (/^https?:\/\//.test(path)) return path
    return baseUrl + path
  },

  // 二维码加载失败兜底
  onQrError() {
    this.setData({ qrImageError: true })
  },

  restartCountdown() {
    if (this.data.countdownTimer) {
      clearInterval(this.data.countdownTimer)
    }
    this.setData({ countdown: 30 * 60 })
    const timer = setInterval(() => {
      const cd = this.data.countdown - 1
      if (cd <= 0) {
        this.setData({ countdown: 0, countdownText: '00:00' })
        clearInterval(timer)
      } else {
        const m = Math.floor(cd / 60)
        const s = cd % 60
        this.setData({
          countdown: cd,
          countdownText: String(m).padStart(2, '0') + ':' + String(s).padStart(2, '0'),
        })
      }
    }, 1000)
    this.setData({ countdownTimer: timer })
  },

  // 生成直通码：POST /api/qrcode/generate
  async generateCode() {
    const ctx = this.data.pendingCtx
    if (!ctx) return

    this.setData({ genState: 'loading', errorMsg: '', missingLabels: [] })

    try {
      const data = await post('/api/qrcode/generate', {
        businessType: ctx.businessType,
        materials: ctx.materials || {},
        preFormId: ctx.preFormId,
      })

      if (data && data.passed) {
        const passCode = {
          voucherNum: data.voucherNum,
          sn: data.sn,
          bizTypeName: data.bizTypeName || ctx.bizTypeName || '',
          qrcodeImageUrl: data.qrcodeImageUrl || ('/api/qrcode/' + data.voucherNum + '/image'),
          generatedAt: data.generatedAt || new Date().toISOString(),
          auditSynced: data.auditSynced !== false,
        }
        this.setData({
          passCode,
          genState: 'success',
          pendingCtx: null,
          qrImageUrl: this.buildQrUrl(passCode),
          qrImageError: false,
        })
        wx.setStorageSync('lingmou_last_qrcode', passCode)
        this.restartCountdown()
      } else {
        // 预检拦截
        const missing = data.missingLabels || data.missing || ['必要材料']
        this.setData({ genState: 'rejected', missingLabels: missing })
      }
    } catch (e) {
      this.setData({
        genState: 'error',
        errorMsg: e.message || '直通码签发失败，请稍后重试',
      })
    }
  },

  // 保存二维码图片
  async downloadQRCode() {
    const passCode = this.data.passCode
    if (!passCode) {
      this.copySnFallback()
      return
    }

    try {
      const app = getApp()
      const baseUrl = (app && app.globalData.baseUrl) || 'http://localhost:18091'
      const imageUrl = baseUrl + passCode.qrcodeImageUrl

      const downloadRes = await new Promise((resolve, reject) => {
        wx.downloadFile({
          url: imageUrl,
          success(r) { resolve(r) },
          fail(e) { reject(e) },
        })
      })

      if (downloadRes.statusCode === 200) {
        wx.saveImageToPhotosAlbum({
          filePath: downloadRes.tempFilePath,
          success() {
            wx.showToast({ title: '已保存到相册', icon: 'success' })
          },
          fail() {
            // 无权限则预览
            wx.previewImage({
              urls: [downloadRes.tempFilePath],
              current: downloadRes.tempFilePath,
            })
          },
        })
      } else {
        this.copySnFallback()
      }
    } catch (e) {
      this.copySnFallback()
    }
  },

  copySnFallback() {
    const sn = this.data.passCode ? this.data.passCode.sn : ''
    if (sn) {
      wx.setClipboardData({
        data: sn,
        success() {
          wx.showToast({ title: '凭证号已复制', icon: 'success' })
        },
      })
    }
  },

  // 返回预填单
  goPreForm() {
    wx.switchTab({ url: '/pages/preform/preform' })
  },

  // 去预约排队
  goToNavigation() {
    wx.switchTab({ url: '/pages/navigation/navigation' })
  },

  // 重新签发
  retryGenerate() {
    this.setData({ genState: 'idle', passCode: null, missingLabels: [], errorMsg: '' })
    wx.removeStorageSync('lingmou_last_qrcode')
    this.goPreForm()
  },

  // 去进度追踪
  goToProgress() {
    wx.switchTab({ url: '/pages/progress/progress' })
  },
})
