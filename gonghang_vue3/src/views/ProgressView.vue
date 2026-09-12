<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAppointmentStore } from '../stores/appointment'
import { useRouter } from 'vue-router'

const appt = useAppointmentStore()
const router = useRouter()

// 选中的预约 voucherNum
const selectedVoucher = ref<string>('')
// 批量删除选中的 voucherNum 集合
const selectedApptSet = ref<Set<string>>(new Set())
// 叫号提醒开关
const notifyOn = ref(false)
// 评分（1-5）
const reviewRating = ref(0)
const hoverRating = ref(0)
const reviewSubmitted = ref(false)

// 凭证详情弹窗
const voucherModalOpen = ref(false)
const voucherRefreshing = ref(false)
// 刷新按钮的 loading 态
const refreshing = ref(false)

// 轻提示（沿用 NavigationView 的既有写法，不另造一套通知样式）
const toastMsg = ref('')
const toastVisible = ref(false)
let toastTimer: ReturnType<typeof setTimeout> | null = null
function showToast(msg: string) {
  toastMsg.value = msg
  toastVisible.value = true
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toastVisible.value = false }, 2500)
}

// 5 步定义：1=取号 2=排队 3=叫号 4=办理中 5=完成
const steps = [
  { name: '取号', icon: 'fa-solid fa-ticket' },
  { name: '排队中', icon: 'fa-solid fa-bell' },
  { name: '叫号', icon: 'fa-solid fa-bullhorn' },
  { name: '办理中', icon: 'fa-solid fa-pen' },
  { name: '完成', icon: 'fa-solid fa-flag-checkered' },
]

// 当前选中的预约对象
const currentAppt = computed(() => {
  if (!selectedVoucher.value) return appt.appointments[0] || null
  return appt.appointments.find(a => a.voucherNum === selectedVoucher.value) || null
})

// 当前步骤（基于 store.step）
const currentStep = computed(() => currentAppt.value?.step ?? 1)

// 进度百分比
const progressPct = computed(() => {
  const s = currentStep.value
  if (s >= 5) return 100
  return Math.round((s / 5) * 100)
})

// 状态文案
const statusText = computed(() => {
  if (!currentAppt.value) return ''
  const s = currentStep.value
  if (currentAppt.value.status === 'expired') return '已失效'
  if (currentAppt.value.status === 'canceled') return '已取消'
  if (currentAppt.value.status === 'virtual') return '虚拟号未激活'
  if (s === 1) return '已取号'
  if (s === 2) return '排队中'
  if (s === 3) return '已叫号'
  if (s === 4) return '办理中'
  if (s === 5) return '已完成'
  return ''
})

const statusSubText = computed(() => {
  if (!currentAppt.value) return ''
  if (currentAppt.value.status === 'expired') return '号码自动失效，信誉分已扣除'
  if (currentAppt.value.status === 'canceled') return '预约已取消，如需办理请重新取号'
  if (currentAppt.value.status === 'virtual') return '请到店扫码激活后享有优先叫号'
  const s = currentStep.value
  if (s === 2) return `前方还有 ${currentAppt.value.aheadCount ?? 0} 人`
  if (s === 3) return `请前往 ${currentAppt.value.window ?? '待分配'}`
  if (s === 4) return `${currentAppt.value.window ?? ''} 办理中`
  if (s === 5) return '感谢您的耐心'
  return '已生成排队号'
})

// 是否显示待激活面板
const showVirtualPanel = computed(() =>
  currentAppt.value?.status === 'virtual'
)

// 是否显示已失效面板
const showExpiredPanel = computed(() =>
  currentAppt.value?.status === 'expired'
)

// 是否显示手动推进按钮
const showStepBar = computed(() =>
  currentAppt.value &&
  currentAppt.value.status === 'active' &&
  currentStep.value < 5
)

// 是否显示评价区
const showReview = computed(() =>
  currentAppt.value &&
  currentAppt.value.status === 'completed' &&
  currentStep.value === 5
)

// 推进按钮文案
const stepBtnText = computed(() => {
  const s = currentStep.value
  if (s === 2) return '模拟叫号'
  if (s === 3) return '开始办理'
  if (s === 4) return '办理完成'
  return '下一步'
})

const stepBtnIcon = computed(() => {
  const s = currentStep.value
  if (s === 2) return 'fa-solid fa-bullhorn'
  if (s === 3) return 'fa-solid fa-play'
  if (s === 4) return 'fa-solid fa-check'
  return 'fa-solid fa-forward'
})

// 选中预约
function selectAppt(voucherNum: string) {
  selectedVoucher.value = voucherNum
  reviewSubmitted.value = false
  reviewRating.value = 0
}

// 切换批量选中
function toggleApptSelect(voucherNum: string) {
  if (selectedApptSet.value.has(voucherNum)) {
    selectedApptSet.value.delete(voucherNum)
  } else {
    selectedApptSet.value.add(voucherNum)
  }
}

/**
 * 校正选中项：当前选中的预约已不在列表里时，回落到第一条。
 * fetchMy() 是整体替换列表，若不校正，currentAppt 会变成 null，
 * 模板上的 v-if="currentAppt" 会让整个进度面板消失，看起来像页面坏了。
 */
function syncSelectedVoucher() {
  if (selectedVoucher.value && !appt.appointments.find(a => a.voucherNum === selectedVoucher.value)) {
    selectedVoucher.value = appt.appointments[0]?.voucherNum || ''
  }
}

// 批量删除选中的预约
async function deleteSelectedAppts() {
  if (selectedApptSet.value.size === 0) {
    alert('请先选择要删除的预约')
    return
  }
  if (!confirm(`确定删除选中的 ${selectedApptSet.value.size} 条预约吗？此操作不可撤销。`)) return
  const targets = [...selectedApptSet.value]
  // allSettled 而非 all：一条失败不该中断其余，且失败数要如实报出来
  const results = await Promise.allSettled(targets.map(v => appt.cancel(v)))
  const failed = results.filter(r => r.status === 'rejected').length
  showToast(failed ? `删除失败 ${failed} 条` : `已删除 ${targets.length} 条`)
  selectedApptSet.value = new Set()
  syncSelectedVoucher()
}

// 刷新列表
async function refreshAll() {
  refreshing.value = true
  try {
    await appt.fetchMy()
  } catch (e: any) {
    showToast(e?.message || '刷新失败')
  } finally {
    refreshing.value = false
  }
  syncSelectedVoucher()
}

// 模拟激活（虚拟号 -> 预约号）
async function activateFromProgress() {
  if (!currentAppt.value) return
  try {
    await appt.updateStatus(currentAppt.value.voucherNum, 'active')
  } catch (e: any) {
    showToast(e?.message || '激活失败')
  }
}

// 推进一步
async function stepForward() {
  if (!currentAppt.value) return
  try {
    await appt.stepForward(currentAppt.value.voucherNum)
  } catch (e: any) {
    showToast(e?.message || '推进失败')
  }
}

// 打开凭证详情
function openVoucherModal() {
  voucherModalOpen.value = true
}

// 弹窗内刷新单条（走 GET /api/appointments/{id}）
async function refreshVoucher() {
  if (!currentAppt.value?.id) return
  voucherRefreshing.value = true
  try {
    await appt.refreshOne(currentAppt.value.id)
  } catch (e: any) {
    showToast(e?.message || '刷新凭证失败')
  } finally {
    voucherRefreshing.value = false
  }
}

// 切换提醒开关
function toggleNotify() {
  notifyOn.value = !notifyOn.value
}

// 提交评分
function submitReview() {
  if (!currentAppt.value || reviewRating.value === 0) return
  appt.setRating(currentAppt.value.voucherNum, reviewRating.value)
  appt.addScore('完成评价')
  reviewSubmitted.value = true
}

// 跳转到预约取号
function goBooking() {
  router.push('/navigation')
}

// 状态标签样式
function getStatusBadge(status: string) {
  if (status === 'virtual') return { text: '虚拟号', cls: 'bg-slate-200 text-slate-600' }
  if (status === 'active') return { text: '激活号', cls: 'bg-rose-100 text-rose-700' }
  if (status === 'completed') return { text: '已完成', cls: 'bg-emerald-100 text-emerald-700' }
  if (status === 'canceled') return { text: '已取消', cls: 'bg-slate-100 text-slate-400' }
  return { text: '已失效', cls: 'bg-gray-100 text-gray-400' }
}

// 凭证号显示
function voucherDisplay(a: { voucherNum: string; status: string }) {
  if (a.status === 'active') return 'A' + a.voucherNum.slice(-3)
  if (a.status === 'completed') return 'C' + a.voucherNum.slice(-3)
  return 'V' + a.voucherNum.slice(-3)
}

onMounted(() => {
  // 默认选第一个
  if (appt.appointments.length > 0 && !selectedVoucher.value) {
    selectedVoucher.value = appt.appointments[0]!.voucherNum
  }
})
</script>

<template>
  <div class="p-6 space-y-4">
    <!-- 顶部标题栏 -->
    <div class="bg-white/50 backdrop-blur-2xl p-4 rounded-2xl border border-slate-200/60 flex items-center justify-between">
      <div class="flex items-center space-x-3">
        <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-500 flex items-center justify-center text-white">
          <i class="fa-solid fa-gauge-high text-lg"></i>
        </div>
        <div>
          <h3 class="text-sm font-black text-slate-800">办理进度实时追踪</h3>
          <p class="text-xs text-slate-500 font-medium">多预约管理 · 全程状态可视 · 叫号不等待</p>
        </div>
      </div>
      <button @click="refreshAll" :disabled="refreshing"
        class="flex items-center gap-1.5 text-xs font-bold text-slate-600 hover:text-blue-600 bg-slate-100 hover:bg-blue-50 px-3 py-1.5 rounded-lg border border-slate-200 transition-all cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed">
        <i class="fa-solid fa-rotate text-sm" :class="refreshing ? 'animate-spin' : ''"></i>
        <span>{{ refreshing ? '刷新中' : '刷新' }}</span>
      </button>
    </div>

    <!-- 预约列表（横向卡片选择器） -->
    <div v-if="appt.appointments.length > 0" class="bg-white/50 backdrop-blur-2xl p-3 rounded-2xl border border-slate-200/60">
      <div class="flex items-center justify-between mb-2">
        <div class="flex items-center gap-2">
          <span class="text-[10px] font-black text-slate-400 uppercase tracking-wider">我的预约（点击查看进度）</span>
          <span v-if="selectedApptSet.size > 0" class="text-[10px] font-bold text-indigo-500">已选 {{ selectedApptSet.size }} 条</span>
        </div>
        <button @click="deleteSelectedAppts"
          class="bg-white border border-red-200 text-[10px] font-bold px-2.5 py-1 rounded-lg hover:bg-red-50 transition-all cursor-pointer flex items-center gap-1 text-red-600">
          <i class="fa-solid fa-trash-can"></i>
          <span>批量删除</span>
        </button>
      </div>
      <div class="flex gap-2 overflow-x-auto pb-1">
        <div v-for="a in appt.appointments" :key="a.voucherNum"
          @click="selectAppt(a.voucherNum)"
          :class="[
            'shrink-0 w-44 p-3 rounded-xl border cursor-pointer transition-all relative',
            currentAppt?.voucherNum === a.voucherNum
              ? 'border-indigo-500 ring-2 ring-indigo-500/20 bg-indigo-50'
              : 'border-slate-200 bg-white hover:border-indigo-300'
          ]">
          <input type="checkbox" :checked="selectedApptSet.has(a.voucherNum)"
            @click.stop="toggleApptSelect(a.voucherNum)"
            class="absolute top-2 right-2 w-3.5 h-3.5 rounded border-slate-300 text-red-600 focus:ring-red-500 cursor-pointer">
          <div class="flex items-center justify-between mb-1 pr-5">
            <div class="text-xs font-black text-slate-900 truncate">{{ a.branchName }}</div>
          </div>
          <div class="flex items-center gap-1 mb-1">
            <span :class="['px-1.5 py-0.5 rounded text-[9px] font-bold', getStatusBadge(a.status).cls]">
              {{ getStatusBadge(a.status).text }}
            </span>
          </div>
          <div class="text-[10px] text-slate-500">{{ a.date }} {{ a.timeSlot }}</div>
          <div class="text-[10px] font-bold text-slate-400 mt-1">
            <i class="fa-solid fa-ticket mr-0.5"></i>{{ voucherDisplay(a) }}
          </div>
        </div>
      </div>
    </div>

    <!-- 无预约空状态 -->
    <div v-if="appt.appointments.length === 0"
      class="bg-white/50 backdrop-blur-2xl p-8 rounded-2xl border border-slate-200/60 text-center">
      <i class="fa-solid fa-gauge-high text-4xl text-slate-300 mb-3"></i>
      <p class="text-sm font-black text-slate-500">暂无预约记录</p>
      <p class="text-xs text-slate-400 mt-1 font-medium">预约取号后此处可追踪办理进度</p>
      <button @click="goBooking"
        class="mt-4 bg-gradient-to-r from-blue-600 to-cyan-500 hover:from-blue-700 hover:to-cyan-600 text-white text-xs font-bold px-5 py-2 rounded-xl shadow-md transition-all cursor-pointer">
        <i class="fa-solid fa-arrow-left mr-1.5"></i> 去预约取号
      </button>
    </div>

    <!-- 有预约时的进度面板 -->
    <template v-if="currentAppt">
      <!-- 当前状态大卡片 -->
      <div class="bg-gradient-to-r from-indigo-500 via-purple-500 to-pink-500 p-5 rounded-2xl shadow-lg transition-all">
        <div class="flex items-center justify-between text-white">
          <div>
            <div class="text-[10px] font-bold text-white/70 uppercase tracking-wider">当前状态</div>
            <div class="text-2xl font-black mt-0.5">{{ statusText }}</div>
            <div class="text-xs font-bold text-white/80 mt-1">{{ statusSubText }}</div>
          </div>
          <div class="text-right">
            <div class="text-[10px] font-bold text-white/70 uppercase tracking-wider">凭证号</div>
            <div class="text-2xl font-black tracking-wider">{{ voucherDisplay(currentAppt) }}</div>
            <div class="text-xs font-bold text-white/80 mt-1">
              {{ currentAppt.window && currentStep >= 3 ? currentAppt.window : '待分配窗口' }}
            </div>
            <button @click="openVoucherModal"
              class="mt-2 bg-white/20 hover:bg-white/30 border border-white/40 text-white text-[10px] font-bold px-2.5 py-1 rounded-lg transition-all cursor-pointer">
              <i class="fa-solid fa-receipt mr-1"></i>凭证详情
            </button>
          </div>
        </div>
      </div>

      <!-- 待激活状态专属面板 -->
      <div v-if="showVirtualPanel"
        class="bg-amber-50 rounded-2xl p-4 border border-amber-200">
        <div class="flex items-center gap-3">
          <div class="w-14 h-14 bg-white rounded-lg border-2 border-amber-300 p-1.5 shrink-0 flex items-center justify-center">
            <i class="fa-solid fa-qrcode text-2xl text-amber-500"></i>
          </div>
          <div class="flex-1">
            <p class="text-xs font-black text-amber-700">虚拟号未激活 · 银行暂不叫号</p>
            <p class="text-[10px] font-bold text-amber-600 mt-1">请到店后在排队机扫描二维码激活，转为红色预约号享优先叫号</p>
          </div>
          <button @click="activateFromProgress"
            class="bg-gradient-to-r from-rose-500 to-orange-500 hover:from-rose-600 hover:to-orange-600 text-white text-xs font-bold px-3 py-2 rounded-xl shadow-md transition-all cursor-pointer shrink-0">
            <i class="fa-solid fa-bolt mr-1"></i>模拟激活
          </button>
        </div>
      </div>

      <!-- 已失效状态专属面板 -->
      <div v-if="showExpiredPanel"
        class="bg-rose-50 rounded-2xl p-4 border border-rose-200">
        <div class="flex items-center gap-2">
          <i class="fa-solid fa-circle-xmark text-rose-500 text-lg"></i>
          <div>
            <p class="text-xs font-black text-rose-700">该预约已失效</p>
            <p class="text-[10px] font-bold text-rose-500 mt-0.5">未在预约时段结束前到店激活，号码自动失效，信誉分已扣除</p>
          </div>
        </div>
      </div>

      <!-- 进度时间轴 -->
      <div class="bg-white/50 backdrop-blur-2xl p-5 rounded-2xl border border-slate-200/60">
        <div class="flex items-center justify-between mb-1">
          <span class="text-xs font-black text-slate-700">办理进度</span>
          <span class="text-xs font-bold text-indigo-600">{{ progressPct }}%</span>
        </div>
        <div class="relative">
          <!-- 进度条背景 -->
          <div class="h-2 bg-slate-200 rounded-full overflow-hidden">
            <div class="h-full bg-gradient-to-r from-indigo-500 to-purple-500 rounded-full transition-all duration-500"
              :style="{ width: progressPct + '%' }"></div>
          </div>
          <!-- 步骤点 -->
          <div class="flex justify-between mt-3">
            <div v-for="(step, idx) in steps" :key="step.name"
              class="flex flex-col items-center">
              <div :class="[
                'w-7 h-7 rounded-full flex items-center justify-center text-[10px] font-black transition-all',
                idx + 1 < currentStep ? 'bg-emerald-500 text-white' :
                idx + 1 === currentStep ? 'bg-indigo-500 text-white animate-pulse' :
                'bg-slate-200 text-slate-400'
              ]">
                <i :class="idx + 1 < currentStep ? 'fa-solid fa-check' : step.icon"></i>
              </div>
              <span :class="[
                'text-[9px] font-bold mt-1',
                idx + 1 <= currentStep ? 'text-indigo-600' : 'text-slate-400'
              ]">{{ step.name }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 手动推进按钮（演示用，仅激活后显示） -->
      <div v-if="showStepBar"
        class="bg-white/50 backdrop-blur-2xl p-3 rounded-2xl border border-indigo-200/60 flex items-center justify-between">
        <div class="flex items-center gap-2">
          <i class="fa-solid fa-circle-info text-indigo-500"></i>
          <span class="text-[11px] font-bold text-slate-600">演示模式：点击右侧按钮手动推进办理进度</span>
        </div>
        <button @click="stepForward"
          class="bg-gradient-to-r from-indigo-500 to-purple-500 hover:from-indigo-600 hover:to-purple-600 text-white text-xs font-bold px-4 py-2 rounded-xl shadow-md transition-all cursor-pointer whitespace-nowrap">
          <i :class="[stepBtnIcon, 'mr-1.5']"></i>{{ stepBtnText }}
        </button>
      </div>

      <!-- 叫号提醒开关 + 窗口导航 -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div class="bg-white/50 backdrop-blur-2xl p-4 rounded-2xl border border-slate-200/60">
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-2">
              <i class="fa-solid fa-bell text-amber-500"></i>
              <span class="text-xs font-black text-slate-700">叫号提醒</span>
            </div>
            <button @click="toggleNotify"
              :class="[
                'relative w-10 h-5 rounded-full transition-all cursor-pointer',
                notifyOn ? 'bg-indigo-500' : 'bg-slate-200'
              ]">
              <span :class="[
                'absolute top-0.5 w-4 h-4 bg-white rounded-full shadow transition-all',
                notifyOn ? 'left-[22px]' : 'left-0.5'
              ]"></span>
            </button>
          </div>
          <p class="text-[10px] font-bold text-slate-400 mt-2">前方剩2人时推送提醒，可暂时离开</p>
        </div>
        <div class="bg-white/50 backdrop-blur-2xl p-4 rounded-2xl border border-slate-200/60">
          <div class="flex items-center gap-2">
            <i class="fa-solid fa-signs-post text-emerald-500"></i>
            <span class="text-xs font-black text-slate-700">窗口导航</span>
          </div>
          <p class="text-[10px] font-bold text-slate-400 mt-2">
            {{ currentAppt.window && currentStep >= 3
              ? `请前往 ${currentAppt.window}（大堂右侧）`
              : '叫号后显示具体窗口位置' }}
          </p>
        </div>
      </div>

      <!-- 预约详情 -->
      <div class="bg-white/50 backdrop-blur-2xl p-4 rounded-2xl border border-slate-200/60">
        <div class="flex items-center gap-2 mb-3">
          <i class="fa-solid fa-circle-info text-blue-500"></i>
          <span class="text-xs font-black text-slate-700">预约详情</span>
        </div>
        <div class="grid grid-cols-2 gap-2 text-[11px] font-bold text-slate-600">
          <div class="flex justify-between bg-slate-50/60 px-3 py-2 rounded-lg">
            <span class="text-slate-400">网点</span>
            <span class="text-slate-800 truncate ml-2">{{ currentAppt.branchName }}</span>
          </div>
          <div class="flex justify-between bg-slate-50/60 px-3 py-2 rounded-lg">
            <span class="text-slate-400">日期</span>
            <span class="text-slate-800">{{ currentAppt.date }}</span>
          </div>
          <div class="flex justify-between bg-slate-50/60 px-3 py-2 rounded-lg">
            <span class="text-slate-400">时段</span>
            <span class="text-blue-600">{{ currentAppt.timeSlot }}</span>
          </div>
          <div class="flex justify-between bg-slate-50/60 px-3 py-2 rounded-lg">
            <span class="text-slate-400">业务类型</span>
            <span class="text-slate-800">{{ currentAppt.businessType }}</span>
          </div>
          <div class="flex justify-between bg-slate-50/60 px-3 py-2 rounded-lg">
            <span class="text-slate-400">前方等待</span>
            <span class="text-rose-600">{{ currentAppt.aheadCount ?? 0 }} 人</span>
          </div>
          <div class="flex justify-between bg-slate-50/60 px-3 py-2 rounded-lg">
            <span class="text-slate-400">分配窗口</span>
            <span class="text-slate-800">{{ currentAppt.window || '待分配' }}</span>
          </div>
        </div>
      </div>

      <!-- 办理完成后的评价区 -->
      <div v-if="showReview"
        class="bg-white/50 backdrop-blur-2xl p-4 rounded-2xl border border-slate-200/60">
        <div v-if="!reviewSubmitted" class="text-center">
          <i class="fa-solid fa-circle-check text-3xl text-emerald-500 mb-2"></i>
          <p class="text-sm font-black text-slate-700">办理完成</p>
          <p class="text-xs text-slate-400 font-bold mt-1">请为本次服务评分</p>
          <div class="flex justify-center gap-2 mt-3">
            <button v-for="n in 5" :key="n"
              @click="reviewRating = n"
              @mouseenter="hoverRating = n"
              @mouseleave="hoverRating = 0"
              :class="[
                'text-2xl cursor-pointer transition-all',
                (hoverRating || reviewRating) >= n ? 'text-amber-400' : 'text-slate-300 hover:text-amber-400'
              ]">
              <i class="fa-solid fa-star"></i>
            </button>
          </div>
          <button @click="submitReview"
            class="mt-3 bg-gradient-to-r from-emerald-500 to-cyan-500 hover:from-emerald-600 hover:to-cyan-600 text-white text-xs font-bold px-5 py-2 rounded-xl shadow-md transition-all cursor-pointer">
            <i class="fa-solid fa-check mr-1"></i> 提交评价并完成
          </button>
        </div>
        <div v-else class="text-center">
          <i class="fa-solid fa-heart text-3xl text-rose-400 mb-2"></i>
          <p class="text-sm font-black text-slate-700">感谢评价！信誉分 +2</p>
          <p class="text-xs text-slate-400 font-bold mt-1">您的评分：{{ reviewRating }} 星</p>
        </div>
      </div>
    </template>

    <!-- ================= 凭证详情弹窗 ================= -->
    <!-- 放在 v-if="currentAppt" 之外：刷新时 currentAppt 可能短暂为 null，
         放在里面会被连带销毁，弹窗会自己闪掉 -->
    <Teleport to="body">
      <div v-if="voucherModalOpen && currentAppt"
        class="fixed inset-0 bg-black/50 backdrop-blur-sm z-[9999] flex items-center justify-center p-4"
        @click.self="voucherModalOpen = false">
        <div class="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden">
          <!-- 头部 -->
          <div class="bg-gradient-to-r from-indigo-500 via-purple-500 to-pink-500 px-5 py-4 text-white">
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-2">
                <i class="fa-solid fa-receipt text-lg"></i>
                <span class="text-sm font-black">预约凭证详情</span>
              </div>
              <button @click="voucherModalOpen = false"
                class="text-white/80 hover:text-white transition-all cursor-pointer">
                <i class="fa-solid fa-xmark text-lg"></i>
              </button>
            </div>
            <div class="mt-3 text-center">
              <div class="text-[10px] font-bold text-white/70 uppercase tracking-wider">凭证号</div>
              <div class="text-xl font-black tracking-wider break-all">{{ currentAppt.voucherNum || '—' }}</div>
            </div>
          </div>

          <!-- 明细 -->
          <div class="p-5 space-y-2">
            <div class="flex justify-between bg-slate-50/60 px-3 py-2 rounded-lg">
              <span class="text-xs font-bold text-slate-500">排队号</span>
              <span class="text-xs font-black text-slate-800">{{ currentAppt.queueNumber || '—' }}</span>
            </div>
            <div class="flex justify-between bg-slate-50/60 px-3 py-2 rounded-lg">
              <span class="text-xs font-bold text-slate-500">办理网点</span>
              <span class="text-xs font-black text-slate-800">{{ currentAppt.branchName || '—' }}</span>
            </div>
            <div class="flex justify-between bg-slate-50/60 px-3 py-2 rounded-lg">
              <span class="text-xs font-bold text-slate-500">业务类型</span>
              <span class="text-xs font-black text-slate-800">{{ currentAppt.businessType || '—' }}</span>
            </div>
            <div class="flex justify-between bg-slate-50/60 px-3 py-2 rounded-lg">
              <span class="text-xs font-bold text-slate-500">预约日期</span>
              <span class="text-xs font-black text-slate-800">{{ currentAppt.date || '—' }}</span>
            </div>
            <div class="flex justify-between bg-slate-50/60 px-3 py-2 rounded-lg">
              <span class="text-xs font-bold text-slate-500">预约时段</span>
              <span class="text-xs font-black text-slate-800">{{ currentAppt.timeSlot || '—' }}</span>
            </div>
            <div class="flex justify-between items-center bg-slate-50/60 px-3 py-2 rounded-lg">
              <span class="text-xs font-bold text-slate-500">当前状态</span>
              <span :class="['px-2 py-0.5 rounded text-[10px] font-bold', getStatusBadge(currentAppt.status).cls]">
                {{ getStatusBadge(currentAppt.status).text }}
              </span>
            </div>
            <div class="flex justify-between bg-slate-50/60 px-3 py-2 rounded-lg">
              <span class="text-xs font-bold text-slate-500">取号时间</span>
              <span class="text-xs font-black text-slate-800">{{ currentAppt.createdAt || '—' }}</span>
            </div>
            <div class="flex justify-between bg-slate-50/60 px-3 py-2 rounded-lg">
              <span class="text-xs font-bold text-slate-500">预约ID</span>
              <span class="text-xs font-black text-slate-800">{{ currentAppt.id ?? '—' }}</span>
            </div>

            <div class="flex gap-2 pt-2">
              <button @click="refreshVoucher" :disabled="voucherRefreshing"
                class="flex-1 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold py-2 rounded-xl transition-all cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed">
                <i class="fa-solid fa-rotate mr-1" :class="voucherRefreshing ? 'animate-spin' : ''"></i>
                {{ voucherRefreshing ? '刷新中' : '刷新' }}
              </button>
              <button @click="voucherModalOpen = false"
                class="flex-1 bg-gradient-to-r from-indigo-500 to-purple-500 hover:from-indigo-600 hover:to-purple-600 text-white text-xs font-bold py-2 rounded-xl transition-all cursor-pointer">
                关闭
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 轻提示 -->
    <Teleport to="body">
      <div v-if="toastVisible"
        class="fixed bottom-8 left-1/2 -translate-x-1/2 z-[10000] bg-slate-800 text-white text-xs font-bold px-4 py-2.5 rounded-xl shadow-lg">
        {{ toastMsg }}
      </div>
    </Teleport>
  </div>
</template>
