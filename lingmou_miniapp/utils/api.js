// utils/api.js - 全部API接口封装，与前端Vue3保持一致
const { get, post, put, del } = require('./request')

// ============ 认证接口 ============
module.exports.login = (username, password) => post('/api/auth/login', { username, password })
module.exports.wxLogin = (wxUser) => post('/api/auth/wx-login', wxUser)
module.exports.register = (params) => post('/api/auth/register', params)
module.exports.logout = () => post('/api/auth/logout')

// ============ 预约接口 ============
module.exports.fetchMyAppointments = () => get('/api/appointments/my')
module.exports.getAppointment = (id) => get('/api/appointments/' + id)
module.exports.createAppointment = (data) => post('/api/appointments', data)
module.exports.cancelAppointment = (id) => del('/api/appointments/' + id)
module.exports.updateProgress = (id, data) => put('/api/appointments/' + id + '/progress', data)

// ============ 信誉分接口 ============
module.exports.getCredit = () => get('/api/credit')

// ============ 网点接口 ============
module.exports.fetchBranches = () => get('/api/branches')

// ============ 总览接口 ============
module.exports.getOverview = () => get('/api/overview')

// ============ 二维码/直通码接口 ============
module.exports.generateQRCode = (data) => post('/api/qrcode/generate', data)
module.exports.bindAppointment = (voucherNum, appointmentId) =>
  put('/api/qrcode/' + voucherNum + '/bindAppointment?appointmentId=' + appointmentId)
module.exports.getQRCodeImage = (voucherNum) => get('/api/qrcode/' + voucherNum + '/image')

// ============ 区块链存证接口 ============
module.exports.getAuditLogs = () => get('/api/audit/my')
module.exports.verifyChain = () => get('/api/audit/my/verify')

// ============ AI接口（走Python服务） ============
module.exports.aiChat = (sessionId, message) =>
  post('/api/ai/chat', { sessionId, message }, 'ai')
module.exports.aiHeatmap = (date) =>
  get('/api/ai/heatmap?date=' + (date || 'today'), 'ai')
module.exports.aiDiagnosis = (userId) =>
  get('/api/ai/diagnosis?userId=' + userId, 'ai')
module.exports.aiParsePreform = (text) =>
  post('/api/ai/parse-preform', { rawText: text }, 'ai')
