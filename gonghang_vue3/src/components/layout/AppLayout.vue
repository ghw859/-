<script setup lang="ts">
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'
import AINavButton from '../ai/AINavButton.vue'
import { useElderlyStore } from '../../stores/elderly'
import { useAppointmentStore } from '../../stores/appointment'
import { onMounted, ref, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'

const elderly = useElderlyStore()
const appointment = useAppointmentStore()
const route = useRoute()
const mainRef = ref<HTMLElement | null>(null)

onMounted(() => {
  elderly.init()
  // 加载后端预约数据 + 信用分
  appointment.fetchMy().catch(() => {})
  appointment.fetchCredit().catch(() => {})
})

// 路由切换时滚动到顶部
watch(() => route.path, () => {
  nextTick(() => {
    if (mainRef.value) mainRef.value.scrollTop = 0
  })
})
</script>

<template>
  <!-- 炫彩极光微幅流动背景舱 -->
  <div class="fixed inset-0 pointer-events-none overflow-hidden z-0">
    <div class="aurora-bg absolute top-[-10%] left-[-10%] w-[50vw] h-[50vw] rounded-full bg-gradient-to-tr from-cyan-300 to-blue-400"></div>
    <div class="aurora-bg absolute bottom-[-10%] right-[-5%] w-[45vw] h-[45vw] rounded-full bg-gradient-to-br from-emerald-200 to-cyan-400" style="animation-delay: -4s;"></div>
    <div class="aurora-bg absolute top-[40%] left-[30%] w-[35vw] h-[35vw] rounded-full bg-gradient-to-r from-indigo-200 to-purple-300" style="animation-delay: -8s;"></div>
  </div>

  <div class="flex h-screen overflow-hidden relative z-10 p-4 gap-4 icbc-grid" :class="{ 'elderly-mode': elderly.isElderly }">
    <Sidebar />
    <div class="flex-1 flex flex-col overflow-hidden">
      <Header />
      <main ref="mainRef" class="flex-1 overflow-y-auto">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
    <AINavButton v-if="!elderly.isElderly" />
  </div>
</template>

<style>
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>
