<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAppointmentStore } from '../stores/appointment'
import { useBranchStore } from '../stores/branch'

const router = useRouter()
const appt = useAppointmentStore()
const branchStore = useBranchStore()

// 最新预约
const latestAppt = computed(() => appt.appointments[appt.appointments.length - 1] || null)

// 二维码凭证流水号（从最新预约生成）
const qrSn = computed(() => latestAppt.value?.voucherNum || 'ICBC-2026-XXXX')

// 倒计时 30:00
const countdown = ref(30 * 60)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const countdownText = computed(() => {
  const m = Math.floor(countdown.value / 60)
  const s = countdown.value % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

onMounted(() => {
  countdownTimer = setInterval(() => {
    if (countdown.value > 0) countdown.value--
  }, 1000)
})

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})

function goPreForm() {
  router.push('/pre-form')
}

function downloadQRCode() {
  const sn = qrSn.value
  // 顶部提示条
  const tip = document.createElement('div')
  tip.className = 'fixed top-4 left-1/2 -translate-x-1/2 bg-emerald-500 text-white text-xs font-bold px-4 py-2 rounded-xl shadow-lg z-50 flex items-center gap-1.5'
  tip.innerHTML = `<i class="fa-solid fa-circle-check"></i> 凭证流水号 ${sn} 已复制到剪贴板，可截图保存二维码`
  document.body.appendChild(tip)
  // 尝试复制流水号
  try { navigator.clipboard.writeText(sn) } catch (e) { /* ignore */ }
  setTimeout(() => { tip.remove() }, 3000)
}

function goToNavigation() {
  router.push('/navigation')
}

function handleAppointment(branchName: string) {
  router.push({ path: '/navigation', query: { branch: branchName } })
}

// AI 智能推荐网点：按畅通优先排序，取前 4
const recommendedBranches = computed(() => {
  const order = { free: 0, moderate: 1, busy: 2 }
  return [...branchStore.branches]
    .sort((a, b) => order[a.status] - order[b.status])
    .slice(0, 4)
})

function getStatusBadge(status: 'free' | 'moderate' | 'busy') {
  if (status === 'free') return { text: '空闲', cls: 'bg-emerald-100 text-emerald-600', dotCls: 'bg-emerald-500' }
  if (status === 'moderate') return { text: '适中', cls: 'bg-amber-100 text-amber-600', dotCls: 'bg-amber-500' }
  return { text: '较繁忙', cls: 'bg-rose-100 text-rose-600', dotCls: 'bg-rose-500' }
}
</script>

<template>
  <div class="p-6 space-y-4">
    <!-- 顶部标题栏 -->
    <div class="bg-white/50 backdrop-blur-2xl p-4 rounded-2xl border border-slate-200/60 flex items-center justify-between">
      <div class="flex items-center space-x-3">
        <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-600 to-cyan-500 flex items-center justify-center text-white">
          <i class="fa-solid fa-qrcode text-lg"></i>
        </div>
        <div>
          <h3 class="text-sm font-black text-slate-800">业务直通码与智能网点推荐</h3>
          <p class="text-xs text-slate-500 font-medium">专属二维码凭证 · AI智能推荐最优网点</p>
        </div>
      </div>
    </div>

    <!-- 二维码展示区域 -->
    <div class="bg-white/50 backdrop-blur-2xl p-6 rounded-2xl border border-slate-200/60">
      <div class="text-center">
        <span class="text-xs font-bold text-slate-500 uppercase tracking-wider block mb-4">专属业务直通码</span>

        <div class="w-full max-w-xs bg-white rounded-xl p-4 border-2 border-cyan-400 shadow-md mx-auto relative">
          <!-- 环绕光晕动效 -->
          <div class="absolute -inset-1 bg-gradient-to-r from-blue-400 via-cyan-400 to-emerald-400 rounded-xl opacity-30 blur-md animate-pulse"></div>

          <div class="relative p-2 bg-white border border-slate-200 rounded-lg shadow-inner group overflow-hidden">
            <div class="w-full aspect-square flex flex-wrap items-center justify-center text-slate-800 opacity-95 transition-transform group-hover:scale-105 duration-300 relative">
              <div class="grid grid-cols-3 gap-2 w-full h-full p-1">
                <div class="border-[3px] border-slate-800 aspect-square rounded-sm"></div>
                <div class="flex flex-col justify-between p-0.5"><span class="block bg-slate-800 h-1 w-full"></span><span class="block bg-slate-800 h-1 w-1/2"></span></div>
                <div class="border-[3px] border-slate-800 aspect-square rounded-sm ml-auto"></div>
                <div class="col-span-2 space-y-1"><span class="block bg-slate-800 h-1 w-full"></span><span class="block bg-slate-800 h-1 w-3/4"></span></div>
                <div class="bg-slate-800 aspect-square m-auto"></div>
                <div class="border-[3px] border-slate-800 aspect-square rounded-sm mt-auto"></div>
                <div class="col-span-2 flex flex-col justify-end gap-1 mt-auto"><span class="block bg-slate-800 h-1 w-full"></span><span class="block bg-slate-800 h-1 w-1/2"></span></div>
              </div>
            </div>
            <div class="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 bg-blue-600 text-white text-[9px] font-black px-1.5 py-0.5 rounded shadow border border-white scale-90 z-10">
              直通
            </div>
          </div>

          <div class="mt-2.5 space-y-0.5">
            <div class="text-xs font-mono font-bold text-slate-700">
              凭证流水号：<span class="text-blue-600">{{ qrSn }}</span>
            </div>
            <p class="text-[10px] text-slate-400 font-medium max-w-[220px] leading-tight mx-auto">
              到店后出示此码，柜面扫码将<span class="text-emerald-600 font-bold">免填单</span>快速提取全部录入及合规要素。
            </p>
            <!-- 有效期倒计时 -->
            <div class="text-[10px] font-bold text-emerald-600 flex items-center justify-center gap-1 pt-1">
              <i class="fa-solid fa-shield-halved"></i>
              <span>凭证有效：<span>{{ countdownText }}</span></span>
            </div>
          </div>
        </div>

        <div class="mt-4 flex items-center justify-center gap-2 flex-wrap">
          <button @click="goPreForm"
            class="bg-gradient-to-r from-blue-600 to-cyan-500 hover:from-blue-700 hover:to-cyan-600 text-white text-sm font-bold px-5 py-2 rounded-xl shadow-md transition-all cursor-pointer">
            <i class="fa-solid fa-file-pen mr-1.5"></i> 去填写预填单
          </button>
          <button @click="downloadQRCode"
            class="bg-white border border-slate-200 hover:border-blue-300 text-slate-700 hover:text-blue-600 text-sm font-bold px-4 py-2 rounded-xl shadow-sm transition-all cursor-pointer">
            <i class="fa-solid fa-download mr-1.5"></i> 保存二维码
          </button>
        </div>
      </div>
    </div>

    <!-- 最新预约信息卡 -->
    <div v-if="latestAppt" class="bg-white/50 backdrop-blur-2xl p-4 rounded-2xl border border-slate-200/60">
      <div class="flex items-center justify-between mb-3">
        <div class="flex items-center space-x-2">
          <i class="fa-solid fa-ticket text-emerald-600"></i>
          <h3 class="text-sm font-black text-slate-800">最新预约凭证</h3>
        </div>
        <span class="text-[10px] font-bold bg-emerald-100 text-emerald-700 px-2 py-0.5 rounded-full border border-emerald-200 flex items-center gap-1">
          <span class="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>已锁定
        </span>
      </div>
      <div class="space-y-2">
        <div class="grid grid-cols-2 gap-3 text-xs">
          <div class="bg-slate-50 p-3 rounded-xl">
            <div class="text-slate-400 font-bold mb-0.5">网点</div>
            <div class="font-black text-slate-800">{{ latestAppt.branchName }}</div>
          </div>
          <div class="bg-slate-50 p-3 rounded-xl">
            <div class="text-slate-400 font-bold mb-0.5">凭证号</div>
            <div class="font-black text-cyan-600 tracking-wider">{{ latestAppt.voucherNum }}</div>
          </div>
          <div class="bg-slate-50 p-3 rounded-xl">
            <div class="text-slate-400 font-bold mb-0.5">日期</div>
            <div class="font-black text-slate-800">{{ latestAppt.date }}</div>
          </div>
          <div class="bg-slate-50 p-3 rounded-xl">
            <div class="text-slate-400 font-bold mb-0.5">时段</div>
            <div class="font-black text-slate-800">{{ latestAppt.timeSlot }}</div>
          </div>
        </div>
        <div class="text-[10px] text-slate-500 font-bold pt-1 flex items-center gap-1">
          <i class="fa-solid fa-circle-info text-cyan-500"></i>
          <span>请到店后扫码激活虚拟号转为红色预约号，享优先叫号</span>
        </div>
      </div>
    </div>

    <!-- 智能网点推荐区域 -->
    <div class="bg-white/50 backdrop-blur-2xl p-4 rounded-2xl border border-slate-200/60">
      <div class="flex items-center justify-between mb-4">
        <div class="flex items-center space-x-2">
          <i class="fa-solid fa-map-pin text-cyan-600"></i>
          <h3 class="text-sm font-black text-slate-800">AI智能推荐网点</h3>
        </div>
        <span class="text-xs bg-cyan-100 text-cyan-700 px-2 py-0.5 rounded-full font-bold flex items-center gap-1">
          <span class="w-1.5 h-1.5 rounded-full bg-cyan-500 animate-pulse"></span>实时推荐
        </span>
      </div>

      <!-- 网点列表 -->
      <div class="space-y-3">
        <div v-for="b in recommendedBranches" :key="b.id"
          class="bg-white rounded-xl p-4 border border-slate-100 hover:border-cyan-200 hover:shadow-md transition-all">
          <div class="flex items-start justify-between">
            <div>
              <div class="flex items-center space-x-2">
                <h4 class="text-sm font-black text-slate-800">{{ b.name }}</h4>
                <span :class="['px-2 py-0.5 rounded-full text-[10px] font-bold flex items-center gap-1', getStatusBadge(b.status).cls]">
                  <span :class="['w-1.5 h-1.5 rounded-full animate-pulse', getStatusBadge(b.status).dotCls]"></span>{{ getStatusBadge(b.status).text }}
                </span>
              </div>
              <p class="text-xs text-slate-500 mt-1">{{ b.address }}</p>
              <div class="flex items-center space-x-3 mt-2 text-[11px]">
                <span class="text-slate-500"><i class="fa-solid fa-clock mr-1"></i> {{ b.hours }}</span>
                <span class="text-slate-500"><i class="fa-solid fa-user mr-1"></i> 当前排队: {{ b.flow }}人</span>
              </div>
            </div>
            <button @click="handleAppointment(b.name)"
              class="bg-gradient-to-r from-blue-600 to-cyan-500 hover:from-blue-700 hover:to-cyan-600 text-white text-xs font-bold px-4 py-2 rounded-xl shadow-md transition-all cursor-pointer">
              <i class="fa-solid fa-calendar-check mr-1"></i> 去预约
            </button>
          </div>
        </div>
      </div>

      <div class="mt-3 text-center">
        <button @click="goToNavigation"
          class="text-xs font-bold text-cyan-600 hover:text-cyan-700 cursor-pointer">
          查看全部网点 <i class="fa-solid fa-angle-right text-[10px]"></i>
        </button>
      </div>
    </div>
  </div>
</template>
