<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useElderlyStore } from '../../stores/elderly'
import { useRouter } from 'vue-router'

const elderly = useElderlyStore()
const router = useRouter()
const showMenu = ref(false)
const nowTime = ref('')

let timeTimer: ReturnType<typeof setInterval> | null = null

function pad(n: number) { return n < 10 ? '0' + n : String(n) }
function updateTime() {
  const d = new Date()
  nowTime.value = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function toggleMenu() {
  showMenu.value = !showMenu.value
}

function handleLogout() {
  showMenu.value = false
  router.push('/login')
}

function toggleElderly() {
  elderly.toggle()
  if (elderly.isElderly) {
    router.push('/navigation')
  }
}

onMounted(() => {
  updateTime()
  timeTimer = setInterval(updateTime, 1000)
})

onUnmounted(() => {
  if (timeTimer) clearInterval(timeTimer)
})
</script>

<template>
  <header class="h-16 w-full border border-slate-200/80 bg-white/30 backdrop-blur-xl px-8 flex items-center justify-between shrink-0 rounded-2xl mt-2 shadow-xs relative z-50">
    <div>
      <h1 class="text-base font-black text-slate-800">{{ $route.meta.title || '数据总览看板' }}</h1>
    </div>

    <div class="flex items-center space-x-6">
      <!-- 量子加密链路状态 + 实时时间 -->
      <div class="hidden sm:flex items-center space-x-3 bg-slate-900/5 border border-cyan-500/10 rounded-full py-1.5 px-4 text-[11px] font-mono">
        <div class="flex items-center space-x-1.5">
          <span class="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
          <span class="text-slate-500">量子加密链路:</span>
          <span class="text-cyan-600 font-bold">ACTIVE</span>
        </div>
        <span class="text-slate-300">|</span>
        <div class="text-slate-600 font-medium">{{ nowTime }}</div>
      </div>

      <!-- 老年模式切换按钮（黄色渐变） -->
      <button
        @click="toggleElderly"
        :class="[
          'flex items-center gap-[6px] px-[14px] py-[6px] rounded-[20px] text-xs font-bold border transition-all cursor-pointer whitespace-nowrap',
          elderly.isElderly
            ? 'bg-gradient-to-br from-amber-500 to-amber-600 text-white border-amber-600 shadow-[0_4px_16px_rgba(245,158,11,0.4)]'
            : 'bg-gradient-to-br from-amber-100 to-yellow-100 text-amber-700 border-yellow-200 hover:from-amber-100 hover:to-amber-300 hover:scale-105 hover:shadow-[0_4px_12px_rgba(251,191,36,0.3)]'
        ]"
        title="切换老年模式"
      >
        <i class="fa-solid fa-user-friends text-base"></i>
        <span>{{ elderly.isElderly ? '标准模式' : '老年模式' }}</span>
      </button>

      <!-- 用户头像 + 诸葛灵枢 -->
      <div class="relative">
        <button @click="toggleMenu"
          class="flex items-center space-x-3 border-l border-slate-200 pl-6 cursor-pointer hover:bg-slate-100/50 rounded-xl py-1 pr-2 transition-all">
          <div class="flex flex-col text-right">
            <span class="text-xs font-bold text-slate-700">诸葛灵枢</span>
            <span class="text-[9px] text-emerald-600 font-bold bg-emerald-50 px-1.5 py-0.5 rounded-md mt-0.5 border border-emerald-200/50">工银贵宾客户</span>
          </div>
          <div class="w-9 h-9 rounded-xl bg-gradient-to-br from-slate-200 to-slate-300 border border-white shadow-sm flex items-center justify-center text-slate-600 overflow-hidden">
            <i class="fa-solid fa-user text-base text-slate-500"></i>
          </div>
        </button>

        <div v-if="showMenu"
          class="hidden absolute right-0 top-full mt-2 w-48 bg-white rounded-xl shadow-lg border border-slate-200 py-2 z-[100]"
          :class="{ '!block': showMenu }">
          <div class="px-4 py-2 border-b border-slate-100">
            <p class="text-[10px] text-slate-400 font-bold">当前登录账号</p>
            <p class="text-xs font-bold text-slate-700 truncate">诸葛灵枢</p>
          </div>
          <button @click="handleLogout"
            class="w-full flex items-center space-x-2 px-4 py-2.5 text-xs font-bold text-rose-600 hover:bg-rose-50 transition-colors cursor-pointer">
            <i class="fa-solid fa-right-from-bracket"></i>
            <span>退出登录</span>
          </button>
        </div>
      </div>
    </div>
  </header>
</template>
