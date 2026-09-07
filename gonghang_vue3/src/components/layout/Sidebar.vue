<script setup lang="ts">
import { useElderlyStore } from '../../stores/elderly'

const elderly = useElderlyStore()

const menuItems = [
  { path: '/overview', title: '数据总览看板', icon: 'fa-solid fa-chart-pie', complex: true },
  { path: '/pre-form', title: '智能预填单解析', icon: 'fa-regular fa-file-lines', complex: false },
  { path: '/navigation', title: '智能预约排队', icon: 'fa-solid fa-map-location-dot', complex: false },
  { path: '/progress', title: '办理进度追踪', icon: 'fa-solid fa-gauge-high', complex: false },
  { path: '/history', title: '历史预约查询', icon: 'fa-solid fa-clock-rotate-left', complex: true },
  { path: '/ai-assistant', title: 'AI数字人大堂经理', icon: 'fa-solid fa-robot', complex: false },
]
</script>

<template>
  <aside class="w-72 bg-white/45 backdrop-blur-2xl rounded-3xl border border-slate-200/80 shadow-sm flex flex-col justify-between p-5 shrink-0 transition-all duration-300">
    <div class="space-y-6">
      <!-- 品牌徽标区 -->
      <div class="flex items-center space-x-3 px-2">
        <div class="w-10 h-10 rounded-2xl bg-gradient-to-br from-blue-600 via-cyan-500 to-emerald-500 flex items-center justify-center shadow-lg shadow-cyan-500/20">
          <i class="fa-solid fa-eye text-white text-lg"></i>
        </div>
        <div>
          <h1 class="text-sm font-black tracking-wider text-slate-900 flex items-center gap-1">
            ICBC · <span class="text-transparent bg-clip-text bg-gradient-to-r from-blue-600 to-cyan-500">灵枢</span>
          </h1>
          <p class="text-[9px] font-mono font-bold text-slate-400 tracking-widest uppercase">Smart Banking System</p>
        </div>
      </div>

      <div class="w-full h-px bg-slate-200/60"></div>

      <!-- 导航链路组 -->
      <nav class="space-y-1.5">
        <router-link
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          custom
          v-slot="{ navigate, isActive }"
        >
          <button
            v-show="!(elderly.isElderly && item.complex)"
            @click="navigate"
            :class="[
              'nav-btn w-full flex items-center space-x-3 px-4 py-3 rounded-xl text-sm transition-all cursor-pointer',
              isActive
                ? 'font-bold bg-gradient-to-r from-blue-600 via-cyan-500 to-emerald-500 text-white shadow-md shadow-cyan-500/10'
                : 'font-semibold text-slate-600 hover:bg-slate-200/40 hover:text-slate-800'
            ]"
          >
            <i
              :class="[item.icon, 'text-sm']"
              :style="item.title === 'AI数字人大堂经理' ? 'color: #0891b2; animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;' : ''"
            ></i>
            <span :class="item.title === 'AI数字人大堂经理' && !isActive ? 'font-bold text-slate-800' : ''">
              {{ item.title }}
            </span>
          </button>
        </router-link>
      </nav>
    </div>

    <!-- 底部安全签章与系统版本 -->
    <div class="space-y-3">
      <div class="bg-slate-950/5 p-3 rounded-2xl border border-slate-200/40 text-[10px] font-medium text-slate-500 space-y-1">
        <div class="flex justify-between">
          <span>核心引擎:</span>
          <span class="font-mono font-bold text-slate-700">ICBC-AI v2.6</span>
        </div>
        <div class="flex justify-between">
          <span>安全保护:</span>
          <span class="text-emerald-600 font-bold">
            <i class="fa-solid fa-shield-halved"></i> 量子加密
          </span>
        </div>
      </div>
      <p class="text-[9px] text-center font-bold text-slate-400">中国工商银行 © 2026 智慧升级版</p>
    </div>
  </aside>
</template>
