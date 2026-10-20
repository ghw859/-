<script setup lang="ts">
import { useElderlyStore } from '../../stores/elderly'

const elderly = useElderlyStore()

// 移动端底部标签：与 Sidebar 保持同一组功能，仅用短标签适配窄屏
const menuItems = [
  { path: '/overview', title: '总览', icon: 'fa-solid fa-chart-pie', complex: true },
  { path: '/pre-form', title: '预填单', icon: 'fa-regular fa-file-lines', complex: false },
  { path: '/navigation', title: '预约', icon: 'fa-solid fa-map-location-dot', complex: false },
  { path: '/progress', title: '进度', icon: 'fa-solid fa-gauge-high', complex: false },
  { path: '/history', title: '历史', icon: 'fa-solid fa-clock-rotate-left', complex: true },
  { path: '/ai-assistant', title: 'AI经理', icon: 'fa-solid fa-robot', complex: false },
]
</script>

<template>
  <!-- 移动端底部导航栏（md 及以上隐藏，由 Sidebar 接管） -->
  <nav class="bottom-nav md:hidden fixed inset-x-0 bottom-0 z-50 bg-white/85 backdrop-blur-2xl border-t border-slate-200/70 shadow-[0_-4px_24px_rgba(15,23,42,0.08)]">
    <div class="flex items-stretch justify-around px-1 pt-1">
      <router-link
        v-for="item in menuItems"
        :key="item.path"
        :to="item.path"
        custom
        v-slot="{ navigate, isActive }"
      >
        <a
          v-show="!(elderly.isElderly && item.complex)"
          @click="navigate"
          :class="[
            'bottom-nav-item relative flex-1 flex flex-col items-center justify-center gap-0.5 py-1.5 rounded-xl transition-all cursor-pointer select-none',
            isActive ? 'text-blue-600' : 'text-slate-400 active:text-slate-600'
          ]"
        >
          <span
            v-if="isActive"
            class="absolute top-0 w-6 h-0.5 rounded-full bg-gradient-to-r from-blue-600 via-cyan-500 to-emerald-500"
          ></span>
          <i :class="[item.icon, 'text-[17px] leading-none']"></i>
          <span :class="['text-[10px] leading-tight', isActive ? 'font-black' : 'font-bold']">
            {{ item.title }}
          </span>
        </a>
      </router-link>
    </div>
  </nav>
</template>

<style scoped>
.bottom-nav {
  padding-bottom: env(safe-area-inset-bottom);
}
</style>
