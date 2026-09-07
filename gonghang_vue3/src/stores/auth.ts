import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const username = ref(localStorage.getItem('lingmou_username') || '诸葛灵枢')
  const isLoggedIn = ref(localStorage.getItem('lingmou_logged_in') === 'true')

  function login(user: string, pwd: string) {
    if (!user || !pwd) throw new Error('请输入账号和密码')
    isLoggedIn.value = true
    username.value = user
    localStorage.setItem('lingmou_logged_in', 'true')
    localStorage.setItem('lingmou_username', user)
  }

  function logout() {
    isLoggedIn.value = false
    username.value = ''
    localStorage.removeItem('lingmou_logged_in')
    localStorage.removeItem('lingmou_username')
  }

  return { username, isLoggedIn, login, logout }
})
