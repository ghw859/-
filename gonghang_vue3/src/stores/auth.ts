import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'

export interface UserInfo {
  id: number
  username: string
  realName: string
  phone: string
  role: string
  customerLevel: string
  creditScore: number
  elderlyMode: number
}

// userInfo 需要跨刷新持久化：AI 诊断等接口要带 userId，仅存 username 无法在刷新后恢复
function loadStoredUserInfo(): UserInfo | null {
  try {
    const raw = localStorage.getItem('lingmou_user_info')
    return raw ? (JSON.parse(raw) as UserInfo) : null
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const username = ref(localStorage.getItem('lingmou_username') || '')
  const isLoggedIn = ref(localStorage.getItem('lingmou_logged_in') === 'true')
  const userInfo = ref<UserInfo | null>(loadStoredUserInfo())

  async function login(user: string, pwd: string) {
    if (!user || !pwd) throw new Error('请输入账号和密码')
    const data = await request.post('/api/auth/login', { username: user, password: pwd })
    localStorage.setItem('lingmou_token', data.token)
    localStorage.setItem('lingmou_logged_in', 'true')
    localStorage.setItem('lingmou_username', data.userInfo.username)
    localStorage.setItem('lingmou_user_info', JSON.stringify(data.userInfo))
    isLoggedIn.value = true
    username.value = data.userInfo.username
    userInfo.value = data.userInfo
  }

  async function register(payload: { phone: string; password: string; realName: string; idCard: string }) {
    // 后端要求 username 唯一，用手机号作为用户名
    const data = await request.post('/api/auth/register', {
      username: payload.phone,
      password: payload.password,
      realName: payload.realName,
      phone: payload.phone,
      idCard: payload.idCard,
    })
    localStorage.setItem('lingmou_token', data.token)
    localStorage.setItem('lingmou_logged_in', 'true')
    localStorage.setItem('lingmou_username', data.userInfo.username)
    localStorage.setItem('lingmou_user_info', JSON.stringify(data.userInfo))
    isLoggedIn.value = true
    username.value = data.userInfo.username
    userInfo.value = data.userInfo
  }

  async function logout() {
    try {
      await request.post('/api/auth/logout')
    } catch {
      // 即使后端登出失败也要清本地状态
    }
    localStorage.removeItem('lingmou_token')
    localStorage.removeItem('lingmou_logged_in')
    localStorage.removeItem('lingmou_username')
    localStorage.removeItem('lingmou_user_info')
    isLoggedIn.value = false
    username.value = ''
    userInfo.value = null
  }

  return { username, isLoggedIn, userInfo, login, register, logout }
})
