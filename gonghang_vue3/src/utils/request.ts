import axios from 'axios'
import router from '@/router'

/**
 * 统一请求层（接线层）
 * - baseURL 留空，走 Vite Proxy（/api/ai→8000，/api→8080）
 * - 请求拦截器：localStorage 取 token，注入 Authorization: Bearer xxx
 * - 响应拦截器：统一解包 { code, msg, data }
 *   - code === 0 → 返回 data
 *   - 非 0 → 弹 msg 并 reject
 *   - 401 → 清登录态跳 /login
 */
const request = axios.create({
  timeout: 10000,
})

// 后端登录失效相关业务码：10002=未登录或登录已过期，11008=Token无效
const AUTH_FAIL_CODES = new Set([10002, 11008])

/** 清登录态并跳转登录页（已在登录页时不重复跳转） */
function redirectToLogin(message: string) {
  localStorage.removeItem('lingmou_token')
  localStorage.removeItem('lingmou_logged_in')
  localStorage.removeItem('lingmou_username')
  if (router.currentRoute.value.path !== '/login') {
    router.push('/login')
  }
  return Promise.reject(new Error(message))
}

// 请求拦截器：注入 Token
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('lingmou_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

// 响应拦截器：统一解包 + 错误处理
request.interceptors.response.use(
  (response) => {
    const { code, msg, data } = response.data
    if (code === 0) {
      return data
    }
    // 登录态失效（后端以 HTTP 200 + 业务码返回）：清登录态跳登录页
    if (AUTH_FAIL_CODES.has(code)) {
      return redirectToLogin(msg || '登录已过期，请重新登录')
    }
    // 非 0：弹错误信息并 reject
    if (msg) alert(msg)
    return Promise.reject(new Error(msg || '请求失败'))
  },
  (error) => {
    if (error.response?.status === 401) {
      return redirectToLogin('登录已过期，请重新登录')
    }
    // HTTP 200 体外的业务码失效（兜底）
    const bizCode = error.response?.data?.code
    if (AUTH_FAIL_CODES.has(bizCode)) {
      return redirectToLogin(error.response?.data?.msg || '登录已过期，请重新登录')
    }
    const msg = error.response?.data?.msg || error.message || '网络异常'
    return Promise.reject(new Error(msg))
  },
)

// 类型声明：拦截器已解包，方法直接返回 data 的类型
declare module 'axios' {
  export interface AxiosInstance {
    get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
    post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T>
    put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T>
    delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
  }
}

export default request
