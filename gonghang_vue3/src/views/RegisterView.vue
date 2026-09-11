<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useElderlyStore } from '../stores/elderly'

const router = useRouter()
const auth = useAuthStore()
const elderly = useElderlyStore()

const form = ref({
  name: '',
  idCard: '',
  phone: '',
  password: '',
  confirmPassword: ''
})
const errorMsg = ref('')
const successMsg = ref('')

onMounted(() => {
  elderly.init()
})

async function handleRegister() {
  errorMsg.value = ''
  successMsg.value = ''

  if (!form.value.name || !form.value.idCard || !form.value.phone || !form.value.password || !form.value.confirmPassword) {
    errorMsg.value = '请填写所有必填项'
    return
  }
  if (!/^\d{17}[\dXx]$/.test(form.value.idCard)) {
    errorMsg.value = '身份证号格式不正确，请输入18位'
    return
  }
  if (!/^1\d{10}$/.test(form.value.phone)) {
    errorMsg.value = '手机号格式不正确，请输入11位'
    return
  }
  if (form.value.password.length < 6 || form.value.password.length > 20) {
    errorMsg.value = '密码长度需为6-20位'
    return
  }
  if (form.value.password !== form.value.confirmPassword) {
    errorMsg.value = '两次输入的密码不一致'
    return
  }

  try {
    await auth.register({
      phone: form.value.phone,
      password: form.value.password,
      realName: form.value.name,
      idCard: form.value.idCard,
    })
    successMsg.value = '注册成功！即将进入系统...'
    setTimeout(() => {
      router.push('/')
    }, 1500)
  } catch (e: any) {
    errorMsg.value = e.message
  }
}

function backToLogin() {
  router.push('/login')
}

function toggleElderly() {
  elderly.toggle()
}
</script>

<template>
  <div
    class="fixed inset-0 z-[9999] icbc-liquid-bg overflow-hidden"
    :class="{ 'elderly-mode': elderly.isElderly }"
  >
    <!-- 冰晶极光背景 -->
    <div class="absolute inset-0 w-full h-full z-0 opacity-90 pointer-events-none">
      <div class="blob-ice-cyan absolute top-[-10%] left-[-5%] w-[900px] h-[900px] rounded-full bg-cyan-200/40"></div>
      <div class="blob-deep-blue absolute bottom-[-15%] right-[-5%] w-[1000px] h-[1000px] rounded-full bg-blue-200/30"></div>
      <div class="blob-aurora-mint absolute top-[20%] left-[30%] w-[750px] h-[750px] rounded-full bg-emerald-100/30"></div>
    </div>

    <!-- 数字流星 -->
    <div class="absolute inset-0 w-full h-full z-10 overflow-hidden pointer-events-none">
      <div class="meteor-line"
        style="top: 15%; left: 20%; animation: data-meteor 8s infinite ease-in; animation-delay: 1s;"></div>
      <div class="meteor-line"
        style="top: 45%; left: 45%; animation: data-meteor 11s infinite ease-in; animation-delay: 3s;"></div>
      <div class="meteor-line"
        style="top: 70%; left: 10%; animation: data-meteor 7s infinite ease-in; animation-delay: 5s;"></div>
    </div>

    <!-- 大粒子漂移 -->
    <div class="absolute inset-0 w-full h-full z-10 overflow-hidden pointer-events-none">
      <div
        class="absolute top-0 left-0 opacity-0 -translate-x-full w-12 h-12 rounded-full bg-gradient-to-br from-cyan-400 to-blue-500 shadow-[0_0_40px_rgba(6,182,212,0.6)] pt-flow-1"
        style="animation-delay: 0s;"></div>
      <div
        class="absolute top-0 left-0 opacity-0 -translate-x-full w-8 h-8 rounded-full bg-cyan-300 shadow-[0_0_30px_rgba(34,211,238,0.5)] pt-flow-1"
        style="animation-delay: 4.5s; animation-duration: 12s;"></div>
      <div
        class="absolute top-0 left-0 opacity-0 -translate-x-full w-10 h-10 rounded-full bg-emerald-400 shadow-[0_0_35px_rgba(52,211,153,0.4)] pt-flow-1"
        style="animation-delay: 9s; animation-duration: 18s;"></div>
      <div
        class="absolute top-0 left-0 opacity-0 -translate-x-full w-11 h-11 rounded-full bg-gradient-to-br from-blue-400 to-indigo-500 shadow-[0_0_35px_rgba(59,130,246,0.5)] pt-flow-2"
        style="animation-delay: 2s;"></div>
      <div
        class="absolute top-0 left-0 opacity-0 -translate-x-full w-7 h-7 rounded-full bg-cyan-200 shadow-[0_0_25px_rgba(165,243,252,0.6)] pt-flow-2"
        style="animation-delay: 7s; animation-duration: 15s;"></div>
    </div>

    <!-- 灵枢Logo动画区 -->
    <div class="absolute inset-0 w-full h-full z-10 flex items-center pointer-events-none">
      <div class="w-full lg:w-[58%] h-full flex items-center justify-center relative">
        <div class="w-[520px] h-[520px] sm:w-[640px] sm:h-[640px] relative flex items-center justify-center">
          <div class="absolute w-[80%] h-[80%] rounded-full border-2 border-cyan-400/40 radar-wave"></div>
          <div class="absolute w-[80%] h-[80%] rounded-full border border-blue-400/30 radar-wave"
            style="animation-delay: 2s;"></div>

          <!-- 土星环轨道 -->
          <svg class="absolute w-[125%] h-[125%] orbit-fast-cw opacity-90" viewBox="0 0 200 200">
            <ellipse cx="100" cy="100" rx="96" ry="34" stroke="url(#light-grad-cyan)" stroke-width="1.8" fill="none"
              stroke-dasharray="35 15" class="laser-track" transform="rotate(-23 100 100)" />
          </svg>
          <svg class="absolute w-[112%] h-[112%] orbit-slow-ccw opacity-90" viewBox="0 0 200 200">
            <ellipse cx="100" cy="100" rx="84" ry="26" stroke="url(#light-grad-blue)" stroke-width="1.6" fill="none"
              stroke-dasharray="12 12" class="laser-track" style="animation-duration: 0.9s;"
              transform="rotate(14 100 100)" />
          </svg>
          <svg class="absolute w-[100%] h-[100%] orbit-mid-cw opacity-60" viewBox="0 0 200 200">
            <ellipse cx="100" cy="100" rx="72" ry="19" stroke="#059669" stroke-width="1" fill="none"
              stroke-dasharray="50 15 15 20" transform="rotate(-6 100 100)" />
          </svg>

          <!-- SVG渐变定义 -->
          <svg class="w-0 h-0 absolute">
            <defs>
              <linearGradient id="light-grad-cyan" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#06b6d4" />
                <stop offset="100%" stop-color="#3b82f6" />
              </linearGradient>
              <linearGradient id="light-grad-blue" x1="100%" y1="0%" x2="0%" y2="100%">
                <stop offset="0%" stop-color="#2563eb" />
                <stop offset="100%" stop-color="#10b981" />
              </linearGradient>
            </defs>
          </svg>

          <!-- 灵枢眼睛 -->
          <div
            class="w-[66%] h-[66%] rounded-full border border-slate-200/60 p-2.5 relative animate-lingmou-blink flex items-center justify-center bg-white/85 backdrop-blur-md shadow-[0_20px_50px_rgba(148,163,184,0.2),_inset_0_2px_4px_rgba(255,255,255,0.9)]">
            <svg class="absolute inset-0 w-full h-full scale-105" viewBox="0 0 100 100">
              <circle cx="50" cy="50" r="47" stroke="#06b6d4" stroke-width="1" fill="none" stroke-dasharray="35 15 5 10"
                opacity="0.6" />
              <circle cx="50" cy="50" r="44" stroke="#3b82f6" stroke-width="1.5" fill="none" stroke-dasharray="14 35"
                opacity="0.5" />
            </svg>
            <div
              class="w-full h-full rounded-full bg-gradient-to-b from-white via-[#f8fafc] to-slate-50 flex items-center justify-center p-8 relative shadow-[inset_0_0_40px_rgba(6,182,212,0.18),_0_6px_30px_rgba(0,0,0,0.03)] animate-lingmou-scan">
              <div
                class="w-[74%] h-[74%] rounded-full bg-gradient-to-tr from-blue-600 via-slate-100 to-cyan-400 p-[1.5px] shadow-[0_10px_35px_rgba(6,182,212,0.35)] flex items-center justify-center relative overflow-hidden">
                <div class="w-full h-full bg-white rounded-full flex items-center justify-center relative shadow-inner">
                  <div class="absolute w-[88%] h-[0.5px] bg-cyan-500/20"></div>
                  <div class="absolute h-[88%] w-[0.5px] bg-cyan-500/20"></div>
                  <i
                    class="fa-solid fa-user-plus text-3xl text-transparent bg-clip-text bg-gradient-to-r from-cyan-500 via-blue-600 to-emerald-400"></i>
                </div>
                <div
                  class="absolute top-3 left-7 w-16 h-8 bg-gradient-to-b from-white/80 to-transparent rounded-full blur-[0.5px] transform -rotate-20">
                </div>
              </div>
              <span
                class="absolute top-3 text-[7px] font-black font-mono text-blue-600 tracking-widest uppercase opacity-70">ICBC
                AI.Matrix</span>
              <span class="absolute bottom-3 text-[8px] font-bold font-mono text-cyan-500">SYSTEM_PROTECTED</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 注册表单卡片 -->
    <div
      class="absolute inset-0 w-full h-full z-20 flex items-center justify-center lg:justify-end lg:pr-24 xl:pr-44 p-4 pointer-events-none">
      <div
        class="glass-card w-full max-w-[440px] p-8 sm:p-10 rounded-[2.5rem] pointer-events-auto relative bg-white/45 backdrop-blur-3xl overflow-hidden shadow-[0_40px_80px_rgba(15,23,42,0.05),_inset_0_1px_2px_rgba(255,255,255,0.8)] hover:shadow-[0_40px_90px_rgba(15,23,42,0.08)] transition-all duration-500 flex flex-col justify-between space-y-6">
        <svg class="card-glow-border">
          <defs>
            <linearGradient id="card-streamer-gradient" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" class="border-stop-1" />
              <stop offset="100%" class="border-stop-2" />
            </linearGradient>
          </defs>
          <rect x="0.75" y="0.75" width="calc(100% - 1.5px)" height="calc(100% - 1.5px)" />
        </svg>

        <div class="flex items-center space-x-3.5 relative z-10">
          <div
            class="w-11 h-11 rounded-2xl bg-gradient-to-tr from-blue-600 via-cyan-500 to-emerald-400 flex items-center justify-center text-white font-bold shadow-md shadow-cyan-500/20 relative">
            <i class="fa-solid fa-user-plus text-lg text-white"></i>
            <span class="absolute -top-0.5 -right-0.5 w-2 h-2 bg-emerald-400 rounded-full animate-pulse"></span>
          </div>
          <div class="flex flex-col">
            <span class="font-black text-xl tracking-tight text-slate-800">ICBC · 灵枢</span>
            <span class="text-[9px] text-cyan-600 font-extrabold tracking-wider uppercase -mt-0.5">新用户注册开户</span>
          </div>
        </div>

        <div class="space-y-5 relative z-10">
          <div class="space-y-2">
            <div
              class="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-gradient-to-r from-cyan-500/5 to-blue-500/5 border border-cyan-500/20 text-cyan-700 text-[10px] font-black tracking-wide">
              <i class="fa-solid fa-trophy text-cyan-500"></i>
              <span>2026 中国工商银行"工行杯"演示环境</span>
            </div>
            <h2 class="text-2xl font-black tracking-tight text-slate-800">创建您的灵枢账户</h2>
            <p class="text-xs text-slate-500">填写以下信息，开启智能银行办理新体验。</p>
          </div>

          <form class="space-y-[18px]" @submit.prevent="handleRegister">
            <div class="space-y-1.5">
              <label class="text-xs font-bold text-slate-600 tracking-wide">姓名</label>
              <div class="relative">
                <span class="absolute inset-y-0 left-0 pl-4 flex items-center text-slate-400 text-sm">
                  <i class="fa-regular fa-user"></i>
                </span>
                <input v-model="form.name" type="text" required placeholder="请输入您的真实姓名"
                  class="w-full bg-white/60 border border-slate-200/80 focus:border-cyan-500 focus:bg-white/90 rounded-xl pl-11 pr-4 py-2.5 text-sm text-slate-800 outline-none transition-all focus:ring-4 focus:ring-cyan-500/10 backdrop-blur-md placeholder:text-slate-400">
              </div>
            </div>

            <div class="space-y-1.5">
              <label class="text-xs font-bold text-slate-600 tracking-wide">身份证号</label>
              <div class="relative">
                <span class="absolute inset-y-0 left-0 pl-4 flex items-center text-slate-400 text-sm">
                  <i class="fa-regular fa-id-card"></i>
                </span>
                <input v-model="form.idCard" type="text" required placeholder="请输入18位身份证号"
                  class="w-full bg-white/60 border border-slate-200/80 focus:border-cyan-500 focus:bg-white/90 rounded-xl pl-11 pr-4 py-2.5 text-sm text-slate-800 outline-none transition-all focus:ring-4 focus:ring-cyan-500/10 backdrop-blur-md placeholder:text-slate-400">
              </div>
            </div>

            <div class="space-y-1.5">
              <label class="text-xs font-bold text-slate-600 tracking-wide">手机号</label>
              <div class="relative">
                <span class="absolute inset-y-0 left-0 pl-4 flex items-center text-slate-400 text-sm">
                  <i class="fa-solid fa-mobile-screen"></i>
                </span>
                <input v-model="form.phone" type="tel" required placeholder="请输入11位手机号"
                  class="w-full bg-white/60 border border-slate-200/80 focus:border-cyan-500 focus:bg-white/90 rounded-xl pl-11 pr-4 py-2.5 text-sm text-slate-800 outline-none transition-all focus:ring-4 focus:ring-cyan-500/10 backdrop-blur-md placeholder:text-slate-400">
              </div>
            </div>

            <div class="space-y-1.5">
              <label class="text-xs font-bold text-slate-600 tracking-wide">登录密码</label>
              <div class="relative">
                <span class="absolute inset-y-0 left-0 pl-4 flex items-center text-slate-400 text-sm">
                  <i class="fa-solid fa-fingerprint"></i>
                </span>
                <input v-model="form.password" type="password" required placeholder="请设置6-20位登录密码"
                  class="w-full bg-white/60 border border-slate-200/80 focus:border-cyan-500 focus:bg-white/90 rounded-xl pl-11 pr-4 py-2.5 text-sm text-slate-800 outline-none transition-all focus:ring-4 focus:ring-cyan-500/10 backdrop-blur-md placeholder:text-slate-400">
              </div>
            </div>

            <div class="space-y-1.5">
              <label class="text-xs font-bold text-slate-600 tracking-wide">确认密码</label>
              <div class="relative">
                <span class="absolute inset-y-0 left-0 pl-4 flex items-center text-slate-400 text-sm">
                  <i class="fa-solid fa-fingerprint"></i>
                </span>
                <input v-model="form.confirmPassword" type="password" required placeholder="请再次输入密码"
                  class="w-full bg-white/60 border border-slate-200/80 focus:border-cyan-500 focus:bg-white/90 rounded-xl pl-11 pr-4 py-2.5 text-sm text-slate-800 outline-none transition-all focus:ring-4 focus:ring-cyan-500/10 backdrop-blur-md placeholder:text-slate-400">
              </div>
            </div>

            <p v-if="errorMsg" class="text-xs text-red-500 font-bold">{{ errorMsg }}</p>
            <p v-if="successMsg" class="text-xs text-emerald-600 font-bold">{{ successMsg }}</p>

            <button type="submit"
              class="w-full bg-gradient-to-r from-blue-600 via-cyan-500 to-emerald-500 hover:brightness-105 text-white font-bold py-3 px-4 rounded-xl text-sm shadow-md shadow-cyan-500/10 transition-all active:scale-[0.97] cursor-pointer block text-center tracking-wider">
              完成注册
            </button>
          </form>

          <div class="relative flex py-1 items-center text-xs">
            <div class="flex-grow border-t border-slate-200"></div>
            <span class="flex-shrink mx-3 text-slate-400 font-mono text-[9px] uppercase tracking-widest">Trust Domain
              Secured</span>
            <div class="flex-grow border-t border-slate-200"></div>
          </div>

          <button @click="backToLogin"
            class="w-full flex items-center justify-center space-x-2 bg-white/50 hover:bg-white/90 border border-slate-200 py-2.5 rounded-xl text-xs font-semibold transition-all cursor-pointer backdrop-blur-md text-slate-600 shadow-sm">
            <i class="fa-solid fa-arrow-left text-cyan-600"></i>
            <span>已有账号？返回登录</span>
          </button>

          <!-- 老年模式切换 -->
          <div class="flex justify-center pt-1">
            <button @click="toggleElderly" class="elderly-toggle" :class="{ active: elderly.isElderly }" title="切换老年模式">
              <i class="fa-solid fa-user-friends"></i>
              <span>老年模式</span>
            </button>
          </div>
        </div>

        <div class="text-[10px] text-slate-400 font-mono text-center tracking-wide relative z-10">
          &copy; 2026 中国工商银行 - ICBC · 灵枢研发团队
        </div>
      </div>
    </div>
  </div>
</template>
