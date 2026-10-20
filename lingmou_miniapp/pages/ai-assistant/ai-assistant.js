// pages/ai-assistant/ai-assistant.js
const { getAIReply, getThoughtSteps } = require('../../utils/constants')
const auth = require('../../utils/auth')

Page({
  data: {
    elderlyMode: false,
    messages: [],
    inputMsg: '',
    scrollIntoView: '',
    // 快捷指令
    quickCommands: [
      { label: '附近网点推荐', cmd: '附近哪个网点人少？' },
      { label: '业务材料咨询', cmd: '我想办外汇需要带什么材料？' },
      { label: '排队等待查询', cmd: '现在排队大概要等多久？' },
    ],
    // 发送中
    sending: false,
  },

  onLoad() {
    const elderlyMode = wx.getStorageSync('elderlyMode') || false
    this.setData({ elderlyMode })

    // 初始化欢迎消息
    this.setData({
      messages: [{
        id: 'welcome',
        role: 'ai',
        content: '您好！我是"灵枢"AI数字人大堂经理，很高兴为您服务。我可以为您推荐最合适的网点、查询实时排队情况、指导业务办理流程和所需材料。请问今天想办理什么业务？',
        showQuickCmds: true,
      }],
    })
  },

  onShow() {
    if (!auth.isLoggedIn()) {
      wx.redirectTo({ url: '/pages/login/login' })
      return
    }
    // 模块级 AI 导航：携带预设问题自动提问
    const app = getApp()
    const preset = app && app.globalData ? app.globalData.aiPresetQuestion : ''
    if (preset) {
      app.globalData.aiPresetQuestion = ''
      this.setData({ inputMsg: preset })
      this.sendMessage()
    }
  },

  onInput(e) {
    this.setData({ inputMsg: e.detail.value })
  },

  // 使用快捷指令
  useQuick(e) {
    const cmd = e.currentTarget.dataset.cmd
    this.setData({ inputMsg: cmd })
    this.sendMessage()
  },

  // 获取 sessionId
  getSessionId() {
    let sid = wx.getStorageSync('lingmou_ai_session')
    if (!sid) {
      sid = 'mini_' + Date.now() + '_' + Math.random().toString(36).slice(2, 8)
      wx.setStorageSync('lingmou_ai_session', sid)
    }
    return sid
  },

  // 调用 AI 接口
  async fetchReply(query) {
    try {
      const app = getApp()
      const aiBaseUrl = (app && app.globalData.aiBaseUrl) || 'http://localhost:18092'
      const token = wx.getStorageSync('token')

      const res = await new Promise((resolve, reject) => {
        wx.request({
          url: aiBaseUrl + '/api/ai/chat',
          method: 'POST',
          data: {
            sessionId: this.getSessionId(),
            message: query,
          },
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
        const reply = res.data.data.reply || res.data.data
        if (typeof reply === 'string' && reply.length > 0) {
          return reply
        }
      }
      return getAIReply(query)
    } catch (e) {
      return getAIReply(query)
    }
  },

  // 发送消息
  async sendMessage() {
    const query = this.data.inputMsg.trim()
    if (!query || this.data.sending) return

    this.setData({ sending: true })

    // 添加用户消息
    const userMsgId = 'u-' + Date.now()
    const aiMsgId = 'a-' + (Date.now() + 1)
    const cotSteps = getThoughtSteps(query)

    const newMessages = [
      ...this.data.messages,
      { id: userMsgId, role: 'user', content: query },
      { id: aiMsgId, role: 'ai', content: '', cotSteps: [], hasCoT: true, typing: true },
    ]
    this.setData({ messages: newMessages, inputMsg: '' })
    this.scrollToBottom()

    // 定位消息下标，后续全部用路径级 setData，保证视图一定刷新
    const msgIndex = newMessages.length - 1
    const base = 'messages[' + msgIndex + ']'

    // 逐步展示思维链
    let stepIdx = 0
    const renderNextStep = () => {
      if (stepIdx < cotSteps.length) {
        const nextSteps = (this.data.messages[msgIndex].cotSteps || []).concat([cotSteps[stepIdx]])
        this.setData({ [base + '.cotSteps']: nextSteps })
        stepIdx++
        this.scrollToBottom()
        setTimeout(renderNextStep, 450)
      } else {
        // 思维链完成，开始打字
        setTimeout(async () => {
          this.setData({ [base + '.hasCoT']: false })

          const reply = await this.fetchReply(query)
          let charIdx = 0
          const typingTimer = setInterval(() => {
            if (charIdx < reply.length) {
              charIdx++
              this.setData({ [base + '.content']: reply.substring(0, charIdx) })
              this.scrollToBottom()
            } else {
              clearInterval(typingTimer)
              this.setData({ [base + '.typing']: false, sending: false })
              this.scrollToBottom()
            }
          }, 25)
        }, 300)
      }
    }
    setTimeout(renderNextStep, 700)
  },

  scrollToBottom() {
    setTimeout(() => {
      this.setData({ scrollIntoView: 'msg-bottom' })
    }, 100)
  },

  // 清空对话
  clearChat() {
    wx.showModal({
      title: '清空对话',
      content: '确定要清空所有对话记录吗？',
      success: (res) => {
        if (res.confirm) {
          this.setData({
            messages: [{
              id: 'welcome',
              role: 'ai',
              content: '您好！我是"灵枢"AI数字人大堂经理，很高兴为您服务。我可以为您推荐最合适的网点、查询实时排队情况、指导业务办理流程和所需材料。请问今天想办理什么业务？',
              showQuickCmds: true,
            }],
          })
        }
      },
    })
  },
})
