// utils/auth.js - 登录状态管理
const { post, isLoggedIn, saveAuth, clearAuth } = require('./request')

/**
 * 微信确认登录
 * @param {object} wxUser - { nickName, avatarUrl, code }
 * @returns {Promise<object>} { token, userInfo }
 */
function wxLogin(wxUser) {
  // 将微信用户信息发送给后端
  // 后端可通过微信 nickName 作为 username 进行登录/注册
  // code 可用于后端调用微信 jscode2session 获取 openid
  return post('/api/auth/wx-login', {
    nickName: wxUser.nickName,
    avatarUrl: wxUser.avatarUrl,
    code: wxUser.code || '',
  })
}

/**
 * 传统账号密码登录（保留兼容）
 * @param {string} username
 * @param {string} password
 * @returns {Promise<object>} { token, userInfo }
 */
function login(username, password) {
  return post('/api/auth/login', { username, password })
}

/**
 * 注册
 * @param {object} params - { username, password, name, idCard, phone }
 */
function register(params) {
  return post('/api/auth/register', params)
}

/**
 * 登出
 */
function logout() {
  return post('/api/auth/logout').catch(() => {}).finally(() => {
    clearAuth()
    // 清除微信用户缓存
    wx.removeStorageSync('wxUserInfo')
  })
}

/**
 * 获取用户信息
 */
function getUserInfo() {
  const info = wx.getStorageSync('userInfo')
  return info || null
}

/**
 * 获取用户ID
 */
function getUserId() {
  const info = getUserInfo()
  return info ? info.id : null
}

/**
 * 获取微信用户信息（头像、昵称）
 */
function getWxUserInfo() {
  return wx.getStorageSync('wxUserInfo') || null
}

module.exports = {
  wxLogin,
  login,
  register,
  logout,
  isLoggedIn,
  saveAuth,
  clearAuth,
  getUserInfo,
  getUserId,
  getWxUserInfo,
}
