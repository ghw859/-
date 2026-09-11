import axios from 'axios'
import router from '@/router'

/**
 * Axios 实例
 * - 自动注入 JWT Token
 * - 401 自动跳转登录页
 * - 统一错误处理
 */
const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

// 请求拦截器：注入 Token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('lingmou_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

// 响应拦截器：统一处理错误
api.interceptors.response.use(
  (response) => {
    // 后端统一返回 { code, msg, data }
    const { code, msg, data } = response.data
    if (code === 0) {
      return data
    }
    return Promise.reject(new Error(msg || '请求失败'))
  },
  (error) => {
    if (error.response?.status === 401) {
      // Token 过期或无效，清除本地状态并跳转登录
      localStorage.removeItem('lingmou_token')
      localStorage.removeItem('lingmou_logged_in')
      localStorage.removeItem('lingmou_username')
      router.push('/login')
      return Promise.reject(new Error('登录已过期，请重新登录'))
    }
    const msg = error.response?.data?.msg || error.message || '网络异常'
    return Promise.reject(new Error(msg))
  },
)

export default api
