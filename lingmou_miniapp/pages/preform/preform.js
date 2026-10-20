// pages/preform/preform.js
const { post } = require('../../utils/request')
const auth = require('../../utils/auth')

const BIZ_TYPES = [
  { value: 'cash_reserve', label: '大额预约取现单' },
  { value: 'open_card', label: '新开卡/开户申请单' },
  { value: 'corp_transfer', label: '对公跨行转账汇款' },
  { value: 'cash_deposit', label: '对公现金缴款' },
  { value: 'fx_exchange', label: '外币兑换' },
]

Page({
  data: {
    elderlyMode: false,
    currentBizType: '',
    bizTypes: BIZ_TYPES,
    // 源文本
    srcText: '',
    statusText: '静态就绪，等待输入触发或直接人工填写',
    // 基础身份信息
    baseInfo: { userName: '', idCard: '', phone: '' },
    // 各业务字段
    cashReserve: { amount: '', currency: 'CNY', date: '', time: '', branch: '', purpose: '', fundSource: '', denomination: '100' },
    openCard: { cardType: 'debit_standard', accountType: 'type1', initialDeposit: '', onlineBanking: 'yes', mobileBanking: 'yes', deliveryMethod: 'branch', emergencyContact: '', address: '' },
    corpTransfer: { payerName: '', payerAccount: '', recvName: '', recvCard: '', recvBank: '', amount: '', urgent: 'no', purpose: '' },
    cashDeposit: { payerName: '', payerAccount: '', amount: '', depositMethod: 'cash', purpose: '', branch: '' },
    fxExchange: { fxCurrency: 'usd', fxDirection: 'buy', fxAmount: '', amount: '', purpose: '', time: '', branch: '' },
    // 提交loading
    submitting: false,
    // AI解析面板
    aiPanelOpen: true,
    // 选择器选项
    cardTypeOptions: ['debit_standard', 'debit_gold', 'credit'],
    accountTypeOptions: ['type1', 'type2'],
    yesNoOptions: ['yes', 'no'],
    deliveryOptions: ['branch', 'express'],
    currencyOptions: ['CNY', 'USD', 'EUR', 'HKD', 'JPY'],
    depositMethodOptions: ['cash', 'transfer'],
    fxCurrencyOptions: ['usd', 'eur', 'hkd', 'jpy', 'gbp'],
    fxDirectionOptions: ['buy', 'sell'],
    timeSlotOptions: ['09:00-09:30', '09:30-10:00', '10:00-10:30', '10:30-11:00', '14:00-14:30', '14:30-15:00', '15:30-16:00', '16:00-16:30'],
    // 今天日期（picker最小值）
    todayDate: '',
    // 各picker的选中索引
    cashDateIndex: 0,
    cashTimeIndex: 0,
    openCardTypeIndex: 0,
    openAccountTypeIndex: 0,
    openOnlineBankingIndex: 0,
    openMobileBankingIndex: 0,
    openDeliveryIndex: 0,
    corpUrgentIndex: 1,
    depositMethodIndex: 0,
    fxCurrencyIndex: 0,
    fxDirectionIndex: 0,
    fxTimeIndex: 0,
  },

  onLoad() {
    const elderlyMode = wx.getStorageSync('elderlyMode') || false
    const now = new Date()
    const todayDate = now.getFullYear() + '-' +
      String(now.getMonth() + 1).padStart(2, '0') + '-' +
      String(now.getDate()).padStart(2, '0')
    this.setData({ elderlyMode, todayDate })
  },

  onShow() {
    if (!auth.isLoggedIn()) {
      wx.redirectTo({ url: '/pages/login/login' })
      return
    }
  },

  // 选择业务类型
  selectBizType(e) {
    const value = e.currentTarget.dataset.value
    this.setData({ currentBizType: value })
  },

  // ============ Picker 联动处理 ============
  // 大额取现
  onCashDateChange(e) {
    this.setData({
      cashDateIndex: parseInt(e.detail.value),
      'cashReserve.date': e.detail.value,
    })
  },
  onCashTimeChange(e) {
    const idx = parseInt(e.detail.value)
    this.setData({
      cashTimeIndex: idx,
      'cashReserve.time': this.data.timeSlotOptions[idx],
    })
  },
  onCashCurrencyChange(e) {
    this.setData({ 'cashReserve.currency': e.detail.value })
  },

  // 开户办卡
  onCardTypeChange(e) {
    const idx = parseInt(e.detail.value)
    this.setData({
      openCardTypeIndex: idx,
      'openCard.cardType': this.data.cardTypeOptions[idx],
    })
  },
  onAccountTypeChange(e) {
    const idx = parseInt(e.detail.value)
    this.setData({
      openAccountTypeIndex: idx,
      'openCard.accountType': this.data.accountTypeOptions[idx],
    })
  },
  onOnlineBankingChange(e) {
    this.setData({ 'openCard.onlineBanking': e.detail.value })
  },
  onMobileBankingChange(e) {
    this.setData({ 'openCard.mobileBanking': e.detail.value })
  },
  onDeliveryChange(e) {
    this.setData({ 'openCard.deliveryMethod': e.detail.value })
  },

  // 对公转账
  onUrgentChange(e) {
    this.setData({ 'corpTransfer.urgent': e.detail.value })
  },

  // 现金缴款
  onDepositMethodChange(e) {
    this.setData({ 'cashDeposit.depositMethod': e.detail.value })
  },

  // 外币兑换
  onFxCurrencyChange(e) {
    this.setData({ 'fxExchange.fxCurrency': e.detail.value })
  },
  onFxDirectionChange(e) {
    this.setData({ 'fxExchange.fxDirection': e.detail.value })
  },
  onFxTimeChange(e) {
    const idx = parseInt(e.detail.value)
    this.setData({
      fxTimeIndex: idx,
      'fxExchange.time': this.data.timeSlotOptions[idx],
    })
  },

  // 源文本输入
  onSrcTextInput(e) {
    this.setData({ srcText: e.detail.value })
  },

  // AI文本解析
  async parseText() {
    const text = this.data.srcText.trim()
    if (!text) {
      wx.showToast({ title: '请先粘贴文本', icon: 'none' })
      return
    }

    this.setData({ statusText: 'AI 正在解析...' })

    try {
      const app = getApp()
      const aiBaseUrl = (app && app.globalData.aiBaseUrl) || 'http://localhost:18092'
      const token = wx.getStorageSync('token')

      const res = await new Promise((resolve, reject) => {
        wx.request({
          url: aiBaseUrl + '/api/ai/parse-preform',
          method: 'POST',
          data: { rawText: text },
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
        const aiResult = res.data.data
        // 后端返回 { businessType, fields:{idCard,phone,amount,name}, confidence }
        // 需拍平为 applyParsedData 期望的 { userName, idCard, phone, amount, businessType }
        // 业务类型后端为大写下划线（CASH_RESERVE），小程序用小写下划线（cash_reserve）
        const BIZ_MAP = {
          CASH_RESERVE: 'cash_reserve',
          OPEN_ACCOUNT: 'open_card',
          LARGE_TRANSFER: 'corp_transfer',
          DEPOSIT: 'cash_deposit',
        }
        const fields = aiResult.fields || {}
        const result = {
          userName: fields.name || '',
          idCard: fields.idCard || '',
          phone: fields.phone || '',
          amount: fields.amount != null ? String(fields.amount) : '',
          businessType: BIZ_MAP[aiResult.businessType] || (aiResult.businessType || '').toLowerCase(),
        }
        this.applyParsedData(result)
        this.setData({
          statusText: 'AI 解析完成，已自动填充',
          currentBizType: result.businessType || this.data.currentBizType,
          aiPanelOpen: true,
        })
      } else {
        // 本地规则解析
        this.localParse(text)
      }
    } catch (e) {
      // 本地降级解析
      this.localParse(text)
    }
  },

  // 本地文本解析（与 preform_parser.py 规则一致）
  localParse(text) {
    const result = { userName: '', idCard: '', phone: '', amount: '', businessType: '' }

    // 姓名
    const nameMatch = text.match(/(?:姓名|名字|客户名)\s*[:：]?\s*([\u4e00-\u9fa5]{2,4})/)
    if (nameMatch) result.userName = nameMatch[1]

    // 身份证
    const idMatch = text.match(/(\d{17}[\dXx])/)
    if (idMatch) result.idCard = idMatch[1]

    // 手机号
    const phoneMatch = text.match(/(1\d{10})/)
    if (phoneMatch) result.phone = phoneMatch[1]

    // 金额（支持中文数字）
    const amountMatch = text.match(/(?:金额|取现|存款|转账|汇款|兑换)\s*[:：]?\s*(\d+(?:\.\d+)?)\s*(?:万|元|万元)?/)
    if (amountMatch) {
      let amount = parseFloat(amountMatch[1])
      if (text.includes('万') && amount < 10000) amount *= 10000
      result.amount = String(amount)
    }

    // 业务类型推断
    if (/取现|现金|取钱/.test(text)) result.businessType = 'cash_reserve'
    else if (/开户|办卡|新卡/.test(text)) result.businessType = 'open_card'
    else if (/转账|汇款|跨行|对公/.test(text)) result.businessType = 'corp_transfer'
    else if (/缴款|存款|缴纳/.test(text)) result.businessType = 'cash_deposit'
    else if (/外汇|兑换|外币|美元|欧元|日元/.test(text)) result.businessType = 'fx_exchange'

    this.applyParsedData(result)
    this.setData({
      statusText: '本地规则解析完成，已自动填充',
      currentBizType: result.businessType || this.data.currentBizType,
      aiPanelOpen: true,
    })
  },

  // 应用解析数据
  applyParsedData(data) {
    const updates = {}
    if (data.userName) updates['baseInfo.userName'] = data.userName
    if (data.idCard) updates['baseInfo.idCard'] = data.idCard
    if (data.phone) updates['baseInfo.phone'] = data.phone

    // 金额填入目标业务类型：解析结果里的业务类型优先于旧的选中项
    if (data.amount) {
      const biz = data.businessType || this.data.currentBizType
      if (biz === 'cash_reserve') updates['cashReserve.amount'] = data.amount
      else if (biz === 'corp_transfer') updates['corpTransfer.amount'] = data.amount
      else if (biz === 'cash_deposit') updates['cashDeposit.amount'] = data.amount
      else if (biz === 'fx_exchange') updates['fxExchange.fxAmount'] = data.amount
      else if (biz === 'open_card') updates['openCard.initialDeposit'] = data.amount
    }

    this.setData(updates)
  },

  // 通用输入处理
  onInput(e) {
    const path = e.currentTarget.dataset.path
    this.setData({ [path]: e.detail.value })
  },

  // 切换AI面板
  toggleAIPanel() {
    this.setData({ aiPanelOpen: !this.data.aiPanelOpen })
  },

  // 提交预填单
  async submitForm() {
    const { currentBizType, baseInfo } = this.data
    if (!currentBizType) {
      wx.showToast({ title: '请选择业务类型', icon: 'none' })
      return
    }
    if (!baseInfo.userName || !baseInfo.idCard || !baseInfo.phone) {
      wx.showToast({ title: '请填写基本信息', icon: 'none' })
      return
    }

    // 收集当前业务字段
    const bizFields = this.data[currentBizType === 'cash_reserve' ? 'cashReserve' :
      currentBizType === 'open_card' ? 'openCard' :
      currentBizType === 'corp_transfer' ? 'corpTransfer' :
      currentBizType === 'cash_deposit' ? 'cashDeposit' : 'fxExchange']

    const materials = {
      userName: baseInfo.userName,
      idCard: baseInfo.idCard,
      phone: baseInfo.phone,
      ...bizFields,
    }

    this.setData({ submitting: true })

    try {
      // 预填单落库（与网页端 PreFormView 一致），失败则停在当前页保留现场
      const matchedBiz = BIZ_TYPES.filter(b => b.value === currentBizType)[0]
      const bizTypeName = matchedBiz ? matchedBiz.label : currentBizType
      // rawText 后端 @NotBlank，手动录入无文本时给兜底，否则吃 10001 参数错误
      const rawText = this.data.srcText.trim() || ('[手动录入] ' + bizTypeName)

      const created = await post('/api/preforms', {
        businessType: currentBizType,
        rawText: rawText,
        parsedJson: JSON.stringify({
          bizType: currentBizType,
          baseInfo: { ...baseInfo },
          fields: { ...bizFields },
        }),
      })

      // 保存到本地，传递给直通码页面（带上后端返回的 preFormId）
      const pendingCtx = {
        preFormId: created && created.id ? created.id : undefined,
        businessType: currentBizType,
        bizTypeName: bizTypeName,
        materials: materials,
      }
      wx.setStorageSync('lingmou_pending_qrcode', pendingCtx)

      wx.showToast({ title: '预填单已保存', icon: 'success' })
      setTimeout(() => {
        wx.switchTab({ url: '/pages/qrcode/qrcode' })
      }, 800)
    } catch (e) {
      wx.showToast({ title: e.message || '提交失败', icon: 'none' })
    } finally {
      this.setData({ submitting: false })
    }
  },

  // 重置表单
  resetForm() {
    this.setData({
      srcText: '',
      currentBizType: '',
      baseInfo: { userName: '', idCard: '', phone: '' },
      cashReserve: { amount: '', currency: 'CNY', date: '', time: '', branch: '', purpose: '', fundSource: '', denomination: '100' },
      openCard: { cardType: 'debit_standard', accountType: 'type1', initialDeposit: '', onlineBanking: 'yes', mobileBanking: 'yes', deliveryMethod: 'branch', emergencyContact: '', address: '' },
      corpTransfer: { payerName: '', payerAccount: '', recvName: '', recvCard: '', recvBank: '', amount: '', urgent: 'no', purpose: '' },
      cashDeposit: { payerName: '', payerAccount: '', amount: '', depositMethod: 'cash', purpose: '', branch: '' },
      fxExchange: { fxCurrency: 'usd', fxDirection: 'buy', fxAmount: '', amount: '', purpose: '', time: '', branch: '' },
      statusText: '已重置，等待输入',
    })
  },
})
