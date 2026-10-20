// utils/request.js - 封装 wx.request，与前端 axios 拦截器保持一致
// 统一响应格式：{ code: 0 success, msg, data }
// code !== 0 时抛出错误，401/token失效时跳转登录页

const AUTH_FAIL_CODES = [10002, 11008]

/**
 * 获取基础URL
 * @param {'backend'|'ai'} type
 */
function getBaseUrl(type) {
  const app = getApp()
  if (!app || !app.globalData) return 'http://localhost:18091'
  if (type === 'ai') return app.globalData.aiBaseUrl
  return app.globalData.baseUrl
}

/**
 * 获取token
 */
function getToken() {
  const app = getApp()
  if (app && app.globalData && app.globalData.token) {
    return app.globalData.token
  }
  return wx.getStorageSync('token') || ''
}

/**
 * 统一请求方法
 * @param {string} url - 接口路径，如 /api/auth/login
 * @param {object} options - { method, data, baseUrlType: 'backend'|'ai' }
 * @returns {Promise<any>} - 成功返回 data 字段
 */
function request(url, options = {}) {
  const { method = 'GET', data = {}, baseUrlType = 'backend' } = options
  const baseUrl = getBaseUrl(baseUrlType)
  const token = getToken()

  return new Promise((resolve, reject) => {
    wx.request({
      url: baseUrl + url,
      method: method,
      data: data,
      header: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: 'Bearer ' + token } : {}),
      },
      timeout: 15000,
      success(res) {
        const body = res.data

        // HTTP 非 2xx
        if (res.statusCode < 200 || res.statusCode >= 300) {
          // 401 未授权
          if (res.statusCode === 401) {
            clearAuthAndRedirect()
            reject(new Error('登录已过期，请重新登录'))
            return
          }
          reject(new Error(body?.msg || '网络请求失败 (' + res.statusCode + ')'))
          return
        }

        // 统一响应格式 { code, msg, data }
        if (body && typeof body.code !== 'undefined') {
          if (body.code === 0) {
            resolve(body.data)
          } else {
            // 业务错误
            if (AUTH_FAIL_CODES.includes(body.code)) {
              clearAuthAndRedirect()
            }
            reject(new Error(body.msg || '请求失败 (' + body.code + ')'))
          }
        } else {
          // 非标准格式，直接返回
          resolve(body)
        }
      },
      fail(err) {
        reject(new Error('网络连接失败，请检查网络'))
      },
    })
  })
}

/**
 * GET 请求
 */
function get(url, baseUrlType) {
  return request(url, { method: 'GET', baseUrlType })
}

/**
 * POST 请求
 */
function post(url, data, baseUrlType) {
  return request(url, { method: 'POST', data, baseUrlType })
}

/**
 * PUT 请求
 */
function put(url, data, baseUrlType) {
  return request(url, { method: 'PUT', data, baseUrlType })
}

/**
 * DELETE 请求
 */
function del(url, baseUrlType) {
  return request(url, { method: 'DELETE', baseUrlType })
}

/**
 * 清除登录状态并跳转到登录页
 */
function clearAuthAndRedirect() {
  const app = getApp()
  if (app && app.globalData) {
    app.globalData.token = ''
    app.globalData.userInfo = null
  }
  wx.removeStorageSync('token')
  wx.removeStorageSync('userInfo')
  // 避免重复跳转
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  if (currentPage && currentPage.route !== 'pages/login/login') {
    wx.redirectTo({ url: '/pages/login/login' })
  }
}

/**
 * 检查是否已登录
 */
function isLoggedIn() {
  return !!getToken()
}

/**
 * 保存登录状态
 */
function saveAuth(token, userInfo) {
  const app = getApp()
  if (app && app.globalData) {
    app.globalData.token = token
    app.globalData.userInfo = userInfo
  }
  wx.setStorageSync('token', token)
  wx.setStorageSync('userInfo', userInfo)
}

/**
 * 清除登录状态
 */
function clearAuth() {
  const app = getApp()
  if (app && app.globalData) {
    app.globalData.token = ''
    app.globalData.userInfo = null
  }
  wx.removeStorageSync('token')
  wx.removeStorageSync('userInfo')
}

module.exports = {
  request,
  get,
  post,
  put,
  del,
  isLoggedIn,
  saveAuth,
  clearAuth,
  AUTH_FAIL_CODES,
}
