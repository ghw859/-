<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAppointmentStore } from '../stores/appointment'
import { useBranchStore } from '../stores/branch'
import request from '@/utils/request'

const router = useRouter()
const appt = useAppointmentStore()
const branchStore = useBranchStore()

// 最新预约
const latestAppt = computed(() => appt.appointments[appt.appointments.length - 1] || null)

// ====== T3 业务直通码签发状态 ======
type GenState = 'idle' | 'loading' | 'success' | 'rejected' | 'error'
const genState = ref<GenState>('idle')

interface PassCode {
  voucherNum: string
  sn: string
  bizTypeName: string
  qrcodeImageUrl: string
  generatedAt: string
  /** 存证链写入是否成功；后端未返回时按已同步处理（兼容旧响应） */
  auditSynced?: boolean
}

/** 本次会话已签发的直通码（刷新页面可恢复展示） */
const passCode = ref<PassCode | null>(null)
/** 待签发上下文（PreFormView 交接而来，预检失败可重试） */
const pendingCtx = ref<{ preFormId?: number; businessType: string; bizTypeName?: string; materials: Record<string, any> } | null>(null)
const missingLabels = ref<string[]>([])
const errorMsg = ref('')

const LAST_CODE_KEY = 'lingmou_last_qrcode'
const PENDING_KEY = 'lingmou_pending_qrcode'

// 二维码凭证流水号（优先本次签发的 T 码，其次最新预约 V 号，最后占位）
const qrSn = computed(() =>
  passCode.value?.sn || passCode.value?.voucherNum || latestAppt.value?.voucherNum || 'ICBC-2026-XXXX'
)
const qrImageUrl = computed(() =>
  passCode.value ? `${passCode.value.qrcodeImageUrl}?v=${encodeURIComponent(passCode.value.voucherNum)}` : ''
)

// 倒计时 30:00（签发成功后重置）
const countdown = ref(30 * 60)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const countdownText = computed(() => {
  const m = Math.floor(countdown.value / 60)
  const s = countdown.value % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

function restartCountdown() {
  countdown.value = 30 * 60
}

onMounted(() => {
  countdownTimer = setInterval(() => {
    if (countdown.value > 0) countdown.value--
  }, 1000)

  // 刷新恢复：已签发过的直通码直接展示（后端 /api/qrcode/{num}/image 公开可访问）
  const lastRaw = sessionStorage.getItem(LAST_CODE_KEY)
  if (lastRaw) {
    try { passCode.value = JSON.parse(lastRaw) as PassCode } catch { /* ignore */ }
  }

  // 消费 PreFormView 交接的待签发上下文（仅消费一次，刷新不重复生成）
  const pendingRaw = sessionStorage.getItem(PENDING_KEY)
  if (pendingRaw) {
    sessionStorage.removeItem(PENDING_KEY)
    try {
      pendingCtx.value = JSON.parse(pendingRaw)
      generateCode()
    } catch {
      genState.value = 'error'
      errorMsg.value = '预填单上下文解析失败，请返回重新填写'
    }
  }
})

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})

/**
 * 调 POST /api/qrcode/generate：
 * - code=0 且 passed=true  → T 码签发成功，展示真实二维码
 * - code=0 且 passed=false → 弹窗展示缺失材料（missingLabels）
 * - 60004 等业务错误       → 拦截器已 alert，本页进入 error 态
 */
async function generateCode() {
  const ctx = pendingCtx.value
  if (!ctx) return
  genState.value = 'loading'
  errorMsg.value = ''
  missingLabels.value = []
  try {
    const data = (await request.post('/api/qrcode/generate', {
      businessType: ctx.businessType,
      materials: ctx.materials || {},
      preFormId: ctx.preFormId,
    })) as any

    if (data?.passed) {
      passCode.value = {
        voucherNum: data.voucherNum,
        sn: data.sn,
        bizTypeName: data.bizTypeName || ctx.bizTypeName || '',
        qrcodeImageUrl: data.qrcodeImageUrl || `/api/qrcode/${data.voucherNum}/image`,
        generatedAt: data.generatedAt || new Date().toISOString(),
        auditSynced: data.auditSynced !== false,
      }
      sessionStorage.setItem(LAST_CODE_KEY, JSON.stringify(passCode.value))
      pendingCtx.value = null
      genState.value = 'success'
      restartCountdown()
    } else {
      // 预检拦截：正常业务分支（后端 code=0）
      missingLabels.value = Array.isArray(data?.missingLabels) && data.missingLabels.length > 0
        ? data.missingLabels
        : (Array.isArray(data?.missing) ? data.missing : ['必要材料'])
      genState.value = 'rejected'
    }
  } catch (e: any) {
    genState.value = 'error'
    errorMsg.value = e?.message || '直通码签发失败，请稍后重试'
  }
}

/** 预检失败弹窗：返回补充材料 */
function backToPreForm() {
  // 保留 pendingCtx，回到预填单修改后重新跳转会携带新上下文
  router.push('/pre-form')
}

function goPreForm() {
  router.push('/pre-form')
}

/** 保存二维码：签发后下载后端真实 PNG；未签发时沿用复制流水号兜底 */
async function downloadQRCode() {
  if (!passCode.value) {
    copySnFallback()
    return
  }
  try {
    // 图片接口公开（WebConfig 已排除鉴权），直接走 fetch 取 Blob
    const resp = await fetch(qrImageUrl.value)
    if (!resp.ok) throw new Error('图片下载失败')
    const blob = await resp.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `业务直通码_${passCode.value.voucherNum}.png`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    showTip(`<i class="fa-solid fa-circle-check"></i> 直通码 ${passCode.value.voucherNum} 已保存为 PNG`)
  } catch {
    copySnFallback()
  }
}

function copySnFallback() {
  const sn = qrSn.value
  try { navigator.clipboard.writeText(sn) } catch (e) { /* ignore */ }
  showTip(`<i class="fa-solid fa-circle-check"></i> 凭证流水号 ${sn} 已复制到剪贴板，可截图保存二维码`)
}

function showTip(html: string) {
  const tip = document.createElement('div')
  tip.className = 'fixed top-4 left-1/2 -translate-x-1/2 bg-emerald-500 text-white text-xs font-bold px-4 py-2 rounded-xl shadow-lg z-50 flex items-center gap-1.5'
  tip.innerHTML = html
  document.body.appendChild(tip)
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
      <!-- 签发状态徽章 -->
      <span v-if="genState === 'success' && passCode?.auditSynced === false"
        class="text-[10px] font-bold bg-amber-100 text-amber-700 px-2.5 py-1 rounded-full border border-amber-200 flex items-center gap-1"
        title="直通码已签发，存证上链稍后自动重试">
        <i class="fa-solid fa-hourglass-half"></i>存证同步中
      </span>
      <span v-else-if="genState === 'success'"
        class="text-[10px] font-bold bg-emerald-100 text-emerald-700 px-2.5 py-1 rounded-full border border-emerald-200 flex items-center gap-1">
        <span class="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>AI 预检通过 · 已上链存证
      </span>
      <span v-else-if="genState === 'loading'"
        class="text-[10px] font-bold bg-blue-100 text-blue-700 px-2.5 py-1 rounded-full border border-blue-200 flex items-center gap-1">
        <i class="fa-solid fa-spinner animate-spin"></i>AI 材料预检中
      </span>
    </div>

    <!-- 二维码展示区域 -->
    <div class="bg-white/50 backdrop-blur-2xl p-6 rounded-2xl border border-slate-200/60">
      <div class="text-center">
        <span class="text-xs font-bold text-slate-500 uppercase tracking-wider block mb-4">专属业务直通码</span>

        <div class="w-full max-w-xs bg-white rounded-xl p-4 border-2 border-cyan-400 shadow-md mx-auto relative">
          <!-- 环绕光晕动效 -->
          <div class="absolute -inset-1 bg-gradient-to-r from-blue-400 via-cyan-400 to-emerald-400 rounded-xl opacity-30 blur-md animate-pulse"></div>

          <div class="relative p-2 bg-white border border-slate-200 rounded-lg shadow-inner group overflow-hidden">
            <!-- 签发成功：后端 ZXing 真实二维码 PNG -->
            <img v-if="passCode"
              :src="qrImageUrl"
              alt="业务直通码"
              class="w-full aspect-square object-contain transition-transform group-hover:scale-105 duration-300" />

            <!-- 未签发：保留原装饰码 + 状态遮罩 -->
            <div v-else class="w-full aspect-square relative">
              <div class="w-full h-full flex flex-wrap items-center justify-center text-slate-800 opacity-40">
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

              <!-- 预检中遮罩 -->
              <div v-if="genState === 'loading'"
                class="absolute inset-0 bg-white/85 backdrop-blur-sm flex flex-col items-center justify-center gap-2">
                <i class="fa-solid fa-spinner animate-spin text-2xl text-blue-600"></i>
                <span class="text-xs font-black text-slate-700">AI 材料预检中…</span>
                <span class="text-[10px] font-bold text-slate-400">预检通过后自动签发直通码</span>
              </div>

              <!-- 错误态遮罩 -->
              <div v-else-if="genState === 'error'"
                class="absolute inset-0 bg-white/90 backdrop-blur-sm flex flex-col items-center justify-center gap-2 px-4">
                <i class="fa-solid fa-triangle-exclamation text-2xl text-rose-500"></i>
                <span class="text-xs font-black text-slate-700 text-center">{{ errorMsg }}</span>
                <button @click="generateCode"
                  class="mt-1 bg-blue-600 hover:bg-blue-700 text-white text-[11px] font-bold px-3 py-1.5 rounded-lg cursor-pointer">
                  <i class="fa-solid fa-rotate mr-1"></i>重新签发
                </button>
              </div>

              <!-- 空态遮罩 -->
              <div v-else-if="genState === 'idle'"
                class="absolute inset-0 bg-white/70 backdrop-blur-[1px] flex flex-col items-center justify-center gap-1.5 px-4">
                <i class="fa-solid fa-file-pen text-xl text-cyan-500"></i>
                <span class="text-xs font-black text-slate-700">暂无直通码</span>
                <span class="text-[10px] font-bold text-slate-400 text-center">先填写智能预填单，AI 预检通过后在此签发</span>
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
            <div v-if="passCode" class="text-[10px] font-mono font-bold text-slate-400 break-all">
              直通码：<span class="text-cyan-600">{{ passCode.voucherNum }}</span>
            </div>
            <div v-if="passCode?.bizTypeName" class="text-[10px] font-bold text-slate-500">
              业务类型：{{ passCode.bizTypeName }}
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
          <span>预约 V 凭证与直通码 T 已互通，请到店后扫码激活享优先叫号</span>
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

    <!-- ================= AI 材料预检拦截弹窗 ================= -->
    <Teleport to="body">
      <div v-if="genState === 'rejected'"
        class="fixed inset-0 bg-black/50 backdrop-blur-sm z-[9999] flex items-center justify-center p-4"
        @click.self="genState = 'idle'">
        <div class="bg-white rounded-3xl w-full max-w-sm shadow-2xl border border-slate-200/80 overflow-hidden flex flex-col">
          <div class="bg-gradient-to-r from-amber-500 via-orange-500 to-rose-500 p-4 shrink-0">
            <div class="flex items-center justify-between">
              <div class="flex items-center space-x-2">
                <div class="w-8 h-8 rounded-lg bg-white/20 flex items-center justify-center text-white shrink-0">
                  <i class="fa-solid fa-triangle-exclamation text-sm"></i>
                </div>
                <div>
                  <span class="text-sm font-black text-white block">AI 材料预检未通过</span>
                  <span class="text-[10px] text-white/85 font-bold">业务直通码已拦截，请补齐材料后再办理</span>
                </div>
              </div>
            </div>
          </div>
          <div class="p-5 space-y-3 overflow-y-auto flex-1">
            <div class="bg-amber-50 rounded-xl p-3 border border-amber-200/60 flex items-start gap-2">
              <i class="fa-solid fa-circle-exclamation text-amber-500 text-sm mt-0.5"></i>
              <p class="text-[11px] font-bold text-amber-700 leading-relaxed">
                检测到以下必需材料缺失或缺件将无法办理，需补齐后重新预检
              </p>
            </div>
            <div class="space-y-2">
              <div v-for="(m, i) in missingLabels" :key="i"
                class="flex items-center gap-2.5 bg-rose-50/60 rounded-xl p-2.5 border border-rose-100">
                <div class="w-8 h-8 rounded-lg bg-white flex items-center justify-center text-rose-500 border border-rose-200 shrink-0">
                  <i class="fa-solid fa-file-circle-xmark text-xs"></i>
                </div>
                <span class="text-xs font-bold text-slate-700 flex-1">{{ m }}</span>
                <span class="text-[8px] font-black bg-rose-100 text-rose-600 px-1.5 py-0.5 rounded-full border border-rose-200">缺失</span>
              </div>
            </div>
          </div>
          <div class="border-t border-slate-200/60 p-3 shrink-0 bg-white space-y-2">
            <button @click="generateCode"
              class="w-full px-4 py-2.5 text-xs font-bold text-white bg-gradient-to-r from-blue-600 to-cyan-500 hover:from-blue-700 hover:to-cyan-600 rounded-xl transition-all cursor-pointer flex items-center justify-center gap-1.5">
              <i class="fa-solid fa-rotate"></i> 重新预检
            </button>
            <button @click="backToPreForm"
              class="w-full px-4 py-2 text-[11px] font-bold text-slate-500 hover:text-slate-700 transition-all cursor-pointer">
              返回补充材料
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
