<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useBranchStore, serviceTagText, type Branch } from '../stores/branch'
import { useAppointmentStore, type Appointment } from '../stores/appointment'
import { useRouter } from 'vue-router'

const branchStore = useBranchStore()
const appt = useAppointmentStore()
const router = useRouter()

// 筛选条件
const filterStatus = ref<'all' | Branch['status']>('all')
const filterDistance = ref<'all' | 'near' | 'mid' | 'far'>('all')
const filterService = ref('all')
const filterSort = ref<'recommend' | 'distance' | 'wait' | 'flow'>('recommend')

const serviceOptions = [
  { value: 'all', label: '全部' },
  { value: '大额现金', label: '大额现金' },
  { value: '外汇', label: '外汇办理' },
  { value: '自助发卡', label: '智能自助发卡' },
  { value: 'VTM', label: 'VTM 远程柜员' },
  { value: 'VIP', label: 'VIP 理财' },
  { value: '对公', label: '对公业务' },
  { value: '无障碍', label: '无障碍通道' },
]

// 状态统计
const statusCounts = computed(() => {
  const free = branchStore.branches.filter(b => b.status === 'free').length
  const moderate = branchStore.branches.filter(b => b.status === 'moderate').length
  const busy = branchStore.branches.filter(b => b.status === 'busy').length
  return { free, moderate, busy }
})

// 距离格式化
function formatDistance(m: number): string {
  if (m < 1000) return `${m}m`
  return `${(m / 1000).toFixed(1)}km`
}

// 状态标签样式
function getStatusBadge(status: Branch['status']) {
  if (status === 'free') return { text: '畅通', cls: 'bg-emerald-100 text-emerald-700 border-emerald-200', dotCls: 'bg-emerald-500' }
  if (status === 'moderate') return { text: '客流适中', cls: 'bg-amber-100 text-amber-700 border-amber-200', dotCls: 'bg-amber-500' }
  return { text: '客流繁忙', cls: 'bg-rose-100 text-rose-600 border-rose-200', dotCls: 'bg-rose-500' }
}

// 趋势图：面积填充 + 折线 + 末端圆点，颜色按趋势方向
function buildTrendAreaPath(trend: number[]): string {
  const max = Math.max(...trend)
  const min = Math.min(...trend)
  const range = max - min || 1
  const W = 100, H = 24, pad = 2
  const pts = trend.map((v, i) => {
    const x = pad + (i / (trend.length - 1)) * (W - pad * 2)
    const y = H - pad - ((v - min) / range) * (H - pad * 2)
    return [x.toFixed(2), y.toFixed(2)] as [string, string]
  })
  const linePath = 'M ' + pts.map(p => p.join(',')).join(' L ')
  const areaPath = linePath + ' L ' + (W - pad) + ',' + H + ' L ' + pad + ',' + H + ' Z'
  return areaPath
}

function buildTrendLinePath(trend: number[]): string {
  const max = Math.max(...trend)
  const min = Math.min(...trend)
  const range = max - min || 1
  const W = 100, H = 24, pad = 2
  const pts = trend.map((v, i) => {
    const x = pad + (i / (trend.length - 1)) * (W - pad * 2)
    const y = H - pad - ((v - min) / range) * (H - pad * 2)
    return [x.toFixed(2), y.toFixed(2)] as [string, string]
  })
  return 'M ' + pts.map(p => p.join(',')).join(' L ')
}

function buildTrendEndDot(trend: number[]): { cx: string; cy: string } {
  const max = Math.max(...trend)
  const min = Math.min(...trend)
  const range = max - min || 1
  const W = 100, H = 24, pad = 2
  const i = trend.length - 1
  const x = pad + (i / (trend.length - 1)) * (W - pad * 2)
  const y = H - pad - ((trend[i]! - min) / range) * (H - pad * 2)
  return { cx: x.toFixed(2), cy: y.toFixed(2) }
}

// 趋势方向颜色：末值>前值=红，末值<前值=绿，持平=蓝
function getTrendColor(trend: number[]): string {
  const last = trend[trend.length - 1]!
  const prev = trend[trend.length - 2]!
  return last > prev ? '#f43f5e' : last < prev ? '#10b981' : '#3b82f6'
}

// 等待时间颜色（趋势图右侧文字）
function getWaitColor(status: Branch['status']) {
  if (status === 'free') return 'text-emerald-600'
  return 'text-amber-600'
}

// ====== 详情弹窗专用：大尺寸趋势图（280×60 视口） ======
function buildTrendAreaPathLarge(trend: number[]): string {
  const max = Math.max(...trend)
  const min = Math.min(...trend)
  const range = max - min || 1
  const W = 280, H = 60, pad = 6
  const pts = trend.map((v, i) => {
    const x = pad + (i / (trend.length - 1)) * (W - pad * 2)
    const y = H - pad - ((v - min) / range) * (H - pad * 2)
    return [x.toFixed(2), y.toFixed(2)] as [string, string]
  })
  const linePath = 'M ' + pts.map(p => p.join(',')).join(' L ')
  return linePath + ' L ' + (W - pad) + ',' + H + ' L ' + pad + ',' + H + ' Z'
}

function buildTrendLinePathLarge(trend: number[]): string {
  const max = Math.max(...trend)
  const min = Math.min(...trend)
  const range = max - min || 1
  const W = 280, H = 60, pad = 6
  const pts = trend.map((v, i) => {
    const x = pad + (i / (trend.length - 1)) * (W - pad * 2)
    const y = H - pad - ((v - min) / range) * (H - pad * 2)
    return [x.toFixed(2), y.toFixed(2)] as [string, string]
  })
  return 'M ' + pts.map(p => p.join(',')).join(' L ')
}

function buildTrendEndDotLarge(trend: number[]): { cx: string; cy: string } {
  const max = Math.max(...trend)
  const min = Math.min(...trend)
  const range = max - min || 1
  const W = 280, H = 60, pad = 6
  const i = trend.length - 1
  const x = pad + (i / (trend.length - 1)) * (W - pad * 2)
  const y = H - pad - ((trend[i]! - min) / range) * (H - pad * 2)
  return { cx: x.toFixed(2), cy: y.toFixed(2) }
}

// ====== 详情弹窗专用：AI客流预测热力图（8 小时柱状图） ======
interface HeatmapBar {
  hour: string
  val: number
  barHeight: number
  bgClass: string
  isBest: boolean
}

function getBranchHeatmap(b: Branch): HeatmapBar[] {
  const hours = ['9:00', '10:00', '11:00', '12:00', '13:00', '14:00', '15:00', '16:00']
  let basePattern: number[]
  if (b.status === 'busy') basePattern = [85, 95, 80, 45, 55, 70, 60, 40]
  else if (b.status === 'moderate') basePattern = [55, 65, 50, 25, 35, 45, 40, 30]
  else basePattern = [30, 35, 28, 15, 20, 25, 22, 18]

  let seed = 0
  for (let i = 0; i < b.name.length; i++) seed += b.name.charCodeAt(i)
  const flows = basePattern.map((v, idx) => {
    const delta = ((seed + idx * 7) % 11) - 5
    return Math.max(10, Math.min(100, v + delta))
  })

  let minIdx = 0, minVal = flows[0]!
  for (let j = 1; j < flows.length; j++) {
    if (flows[j]! < minVal) { minVal = flows[j]!; minIdx = j }
  }

  return flows.map((v, k) => {
    let bgClass: string
    if (v < 35) bgClass = 'bg-emerald-400'
    else if (v < 60) bgClass = 'bg-amber-400'
    else bgClass = 'bg-rose-400'
    return {
      hour: hours[k]!,
      val: v,
      barHeight: Math.round(v * 0.5),
      bgClass,
      isBest: k === minIdx,
    }
  })
}

function getBranchHeatmapBest(b: Branch): { bestHour: string; bestEnd: string; minVal: number } {
  const bars = getBranchHeatmap(b)
  const best = bars.find(x => x.isBest) || bars[0]!
  const bestHourNum = parseInt(best.hour)
  return {
    bestHour: best.hour,
    bestEnd: (bestHourNum + 1) + ':00',
    minVal: best.val,
  }
}

// AI业务基础办理时长（复刻 AI_BIZ_BASE_TIME）
const AI_BIZ_BASE_TIME: Record<string, number> = {
  '大额现金': 18, '外汇': 20, 'VIP': 8, '对公': 25,
  '自助发卡': 5, 'VTM': 3, '无障碍': 12, '理财咨询': 15,
  '开户': 15, '转账': 5, '取现': 10, '销户': 12,
}

// AI预测办理时长（复刻 predictServiceTime）
function predictServiceTime(b: Branch): { time: number; confidence: number } {
  const primaryService = b.services[0] || '通用'
  const baseTime = AI_BIZ_BASE_TIME[primaryService] ?? 12
  const multiplier = b.status === 'busy' ? 1.3 : b.status === 'moderate' ? 1.0 : 0.7
  const predictedTime = Math.round(baseTime * multiplier)
  // 置信度（基于网点名的伪随机，保证一致）
  let seed = 0
  for (let i = 0; i < b.name.length; i++) seed += b.name.charCodeAt(i)
  const confidence = 88 + (seed % 10)
  return { time: predictedTime, confidence }
}

// 数字滚动动画
function animateCountUp(elId: string, target: number, duration = 900) {
  const el = document.getElementById(elId)
  if (!el) return
  const target_el = el
  const startTime = performance.now()
  function step(ts: number) {
    const progress = Math.min((ts - startTime) / duration, 1)
    const eased = 1 - Math.pow(1 - progress, 3)
    target_el.innerText = String(Math.round(target * eased))
    if (progress < 1) requestAnimationFrame(step)
    else target_el.innerText = String(target)
  }
  requestAnimationFrame(step)
}

function runCountUp() {
  nextTick(() => {
    filteredBranches.value.forEach((b, i) => {
      animateCountUp(`flow-${i}`, b.flow)
      animateCountUp(`reserve-${i}`, b.reserve)
    })
  })
}

// 筛选 + 排序后的网点
const filteredBranches = computed<Branch[]>(() => {
  let list = [...branchStore.branches]

  // 客流筛选
  if (filterStatus.value !== 'all') {
    list = list.filter(b => b.status === filterStatus.value)
  }

  // 距离筛选
  if (filterDistance.value === 'near') list = list.filter(b => b.distance <= 1000)
  else if (filterDistance.value === 'mid') list = list.filter(b => b.distance > 1000 && b.distance <= 3000)
  else if (filterDistance.value === 'far') list = list.filter(b => b.distance > 3000)

  // 业务筛选
  if (filterService.value !== 'all') {
    list = list.filter(b => b.services.includes(filterService.value))
  }

  // 排序
  if (filterSort.value === 'distance') {
    list.sort((a, b) => a.distance - b.distance)
  } else if (filterSort.value === 'wait') {
    list.sort((a, b) => a.wait - b.wait)
  } else if (filterSort.value === 'flow') {
    list.sort((a, b) => a.flow - b.flow)
  } else {
    // 智能推荐：综合评分（越低越好）= statusWeight*1000 + distance/10 + wait*2，业务匹配减500
    const statusWeight: Record<string, number> = { free: 0, moderate: 1, busy: 2 }
    const svcVal = filterService.value
    list.sort((a, b) => {
      const scoreA = statusWeight[a.status]! * 1000 + a.distance / 10 + a.wait * 2 - (svcVal !== 'all' && a.services.includes(svcVal) ? 500 : 0)
      const scoreB = statusWeight[b.status]! * 1000 + b.distance / 10 + b.wait * 2 - (svcVal !== 'all' && b.services.includes(svcVal) ? 500 : 0)
      return scoreA - scoreB
    })
  }

  return list
})

// AI 智能推荐（复刻 updateAIRecommendation）
const aiRecommendText = computed(() => {
  if (filteredBranches.value.length === 0) return ''
  const top = filteredBranches.value[0]!
  const statusText = top.status === 'free' ? '畅通' : top.status === 'moderate' ? '客流适中' : '客流繁忙'
  const distText = top.distance >= 1000 ? (top.distance / 1000).toFixed(1) + 'km' : top.distance + 'm'
  const reason = '综合' + (filterService.value !== 'all' ? '业务匹配、' : '') + '客流状态、距离与等待时长多维度评估'
  return `推荐前往 ${top.name}（${statusText}，距您 ${distText}，预计等待 ~${top.wait} 分钟）。${reason}，已为您置顶显示。`
})

onMounted(() => { runCountUp(); checkExpired() })
watch(filteredBranches, runCountUp)

function resetFilter() {
  filterStatus.value = 'all'
  filterDistance.value = 'all'
  filterService.value = 'all'
  filterSort.value = 'recommend'
}

// 详情 modal
const detailBranch = ref<Branch | null>(null)
function openBranchDetail(b: Branch) {
  detailBranch.value = b
}

// 导航（高德地图）
function navigateToBranch(address: string) {
  const url = `https://uri.amap.com/search?keyword=${encodeURIComponent(address)}&src=ICBC_LingMou&callnative=1`
  window.open(url, '_blank')
}

// 我的预约 modal
const showMyAppointments = ref(false)
// 预约详情 modal
const detailAppt = ref<Appointment | null>(null)
// 批量删除选中的 voucherNum 集合
const selectedApptSet = ref<Set<string>>(new Set())

// 查看预约详情
function viewApptDetail(voucherNum: string) {
  const a = appt.appointments.find(x => x.voucherNum === voucherNum)
  if (a) detailAppt.value = a
}

// 切换批量选中
function toggleApptSelect(voucherNum: string) {
  if (selectedApptSet.value.has(voucherNum)) {
    selectedApptSet.value.delete(voucherNum)
  } else {
    selectedApptSet.value.add(voucherNum)
  }
}

// 批量删除选中的预约
function deleteSelectedAppts() {
  if (selectedApptSet.value.size === 0) {
    alert('请先选择要删除的预约')
    return
  }
  const count = selectedApptSet.value.size
  if (!confirm(`确定删除选中的 ${count} 条预约吗？此操作不可撤销。`)) return
  Promise.all([...selectedApptSet.value].map(v => appt.cancel(v)))
    .then(() => showToast(`已删除 ${count} 条预约`))
    .catch((e: any) => showToast(e.message || '删除失败'))
  selectedApptSet.value = new Set()
}

// Toast通知
const toastMsg = ref('')
const toastVisible = ref(false)
function showToast(msg: string) {
  toastMsg.value = msg
  toastVisible.value = true
  setTimeout(() => { toastVisible.value = false }, 2500)
}

// 从我的预约列表中激活预约
async function activateFromList(voucherNum: string) {
  const appointment = appt.appointments.find(a => a.voucherNum === voucherNum)
  if (!appointment || appointment.status !== 'virtual') return
  try {
    await appt.updateStatus(appointment.voucherNum, 'active')
    showToast('激活成功！' + voucherNum + ' 已转为红色预约号')
  } catch (e: any) {
    showToast(e.message || '激活失败')
  }
}

// 从我的预约列表中取消预约
async function cancelFromList(voucherNum: string) {
  if (!confirm('确定取消此预约吗？')) return
  try {
    await appt.cancel(voucherNum)
    showToast('预约已取消')
  } catch (e: any) {
    showToast(e.message || '取消失败')
  }
}

// 过期检测（兜底，后端定时任务已自动处理）
function checkExpired() {
  const now = new Date()
  appt.appointments.forEach(a => {
    if (a.status === 'virtual') {
      try {
        // ISO格式: 2026-09-12
        const apptDate = new Date(a.date + 'T00:00:00')
        const slotParts = a.timeSlot.split('-')
        if (slotParts.length === 2) {
          const endTime = slotParts[1].split(':')
          apptDate.setHours(parseInt(endTime[0]), parseInt(endTime[1]), 0, 0)
          if (now > apptDate) {
            a.status = 'expired'
          }
        }
      } catch (e) { /* ignore parse errors */ }
    }
  })
}

// ====== 预约面板（复刻 view-appointment） ======
const bookingBranch = ref<Branch | null>(null)
const selectedDateOffset = ref(0)
const selectedBizType = ref('cash')
const selectedSlotIdx = ref(-1)
const slotData = ref<{ start: string; end: string; capacity: number; isPast: boolean }[]>([])
const queuePreview = ref<{ pos: number; estStart: string } | null>(null)

// 成功弹窗
const successModal = ref(false)
const voucherNum = ref('')
const voucherActivated = ref(false)

// 日期标签
const dateLabels = ['今天', '明天', '后天', '大后天']
const dates = computed(() => {
  const arr: { offset: number; label: string; dateStr: string }[] = []
  for (let i = 0; i < 4; i++) {
    const d = new Date()
    d.setDate(d.getDate() + i)
    arr.push({
      offset: i,
      label: dateLabels[i],
      dateStr: `${d.getMonth() + 1}月${d.getDate()}日`,
    })
  }
  return arr
})

// 当前日期标签
const currentDateLabel = computed(() => {
  const d = new Date()
  d.setDate(d.getDate() + selectedDateOffset.value)
  const mm = d.getMonth() + 1
  const dd = d.getDate()
  const weekday = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()]
  const dayLabel = dateLabels[selectedDateOffset.value]
  return `${dayLabel} (${mm}月${dd}日 ${weekday}) · 实时放号`
})

// 业务类型选项
const bizOptions = [
  { value: 'cash', label: '大额现金存取 / 预约提现' },
  { value: 'card', label: '个人开户 / 换卡 / 凭证重置' },
  { value: 'forex', label: '跨境汇款 / 外币兑换' },
  { value: 'wealth', label: '理财咨询 / 私人银行专属对账' },
]

// AI预测（预约面板专用）
const apptAIPredict = computed(() => {
  if (!bookingBranch.value) return { time: 0, confidence: 0, detail: '' }
  const bizMap: Record<string, string> = { cash: '大额现金', card: '开户', forex: '外汇', wealth: '理财咨询' }
  const bizLabel: Record<string, string> = { cash: '大额现金存取', card: '个人开户', forex: '跨境汇款', wealth: '理财咨询' }
  const bizKey = bizMap[selectedBizType.value] || '通用'
  const bizText = bizLabel[selectedBizType.value] || bizKey
  const p = predictServiceTime({
    ...bookingBranch.value,
    services: [bizKey],
  })
  const loadText = bookingBranch.value.status === 'busy' ? '繁忙负载' : bookingBranch.value.status === 'moderate' ? '适中负载' : '畅通负载'
  return { time: p.time, confidence: p.confidence, detail: `${bizText} · ${loadText}` }
})

// 基础时段模板
const baseSlots = [
  { start: '09:00', end: '09:30' }, { start: '09:30', end: '10:00' },
  { start: '10:00', end: '10:30' }, { start: '10:30', end: '11:00' },
  { start: '11:00', end: '11:30' },
  { start: '14:00', end: '14:30' }, { start: '14:30', end: '15:00' },
  { start: '15:00', end: '15:30' }, { start: '15:30', end: '16:00' },
  { start: '16:00', end: '16:30' },
]

// 渲染时段（复刻 renderTimeSlots）
function renderTimeSlots() {
  if (!bookingBranch.value) return
  const status = bookingBranch.value.status
  const baseCapacity = status === 'free' ? 10 : status === 'moderate' ? 8 : 5
  const offsetBonus = selectedDateOffset.value * 3
  const bizMultiplier: Record<string, number> = { cash: 0.7, card: 1.0, forex: 0.85, wealth: 1.2 }
  const bizMult = bizMultiplier[selectedBizType.value] || 1.0

  const branchName = bookingBranch.value.name
  const data: { start: string; end: string; capacity: number; isPast: boolean }[] = []
  let totalAvail = 0

  baseSlots.forEach((slot, idx) => {
    const seed = branchName.length + idx * 7 + selectedDateOffset.value * 13 + slot.start.charCodeAt(0)
    const pseudo = (Math.sin(seed) * 10000) % 1
    const ratio = 0.3 + Math.abs(pseudo) * 0.7
    let isPast = false
    if (selectedDateOffset.value === 0) {
      const now = new Date()
      const slotHour = parseInt(slot.start.split(':')[0])
      const slotMin = parseInt(slot.start.split(':')[1])
      if (slotHour < now.getHours() || (slotHour === now.getHours() && slotMin <= now.getMinutes())) {
        isPast = true
      }
    }
    const capacity = isPast ? 0 : Math.min(10, Math.max(0, Math.round((baseCapacity + offsetBonus) * bizMult * ratio)))
    if (!isPast) totalAvail += capacity
    data.push({ start: slot.start, end: slot.end, capacity, isPast })
  })

  slotData.value = data
  selectedSlotIdx.value = -1
  queuePreview.value = null

  // AI推荐：余号最多且未过时段
  let bestIdx = -1, bestCap = 0
  data.forEach((s, i) => {
    if (!s.isPast && s.capacity > bestCap) { bestCap = s.capacity; bestIdx = i }
  })
  if (bestIdx >= 0) selectSlot(bestIdx)

  return totalAvail
}

// 时段汇总
const slotSummary = computed(() => {
  const total = slotData.value.length
  const avail = slotData.value.reduce((sum, s) => !s.isPast ? sum + s.capacity : sum, 0)
  return `共 ${total} 个时段 · 余号 ${avail}`
})

// 选中时段
function selectSlot(idx: number) {
  const s = slotData.value[idx]
  if (!s || s.isPast || s.capacity === 0) return
  selectedSlotIdx.value = idx

  // 排队预览
  if (!bookingBranch.value) return
  const seed = bookingBranch.value.name.length + idx * 11 + selectedDateOffset.value * 7
  const pseudo = Math.abs((Math.sin(seed) * 10000) % 1)
  const alreadyReserved = Math.round(s.capacity * pseudo * 0.6)
  const queuePos = alreadyReserved + 1
  const totalWait = (queuePos - 1) * 5
  const startHour = parseInt(s.start.split(':')[0])
  const startMin = parseInt(s.start.split(':')[1])
  const estMin = startHour * 60 + startMin + totalWait
  const estHour = Math.floor(estMin / 60)
  const estMinPart = estMin % 60
  const estTime = String(estHour).padStart(2, '0') + ':' + String(estMinPart).padStart(2, '0')
  queuePreview.value = { pos: queuePos, estStart: estTime }
}

// 当前选中的时段文本
const selectedSlotText = computed(() => {
  if (selectedSlotIdx.value < 0) return '未选择'
  const s = slotData.value[selectedSlotIdx.value]
  return s ? `${s.start} - ${s.end}` : '未选择'
})

// 打开预约面板
function handleAppointment(b: Branch) {
  bookingBranch.value = b
  selectedDateOffset.value = 0
  selectedBizType.value = 'cash'
  successModal.value = false
  voucherActivated.value = false
  renderTimeSlots()
}

// 返回网点列表
function backToBranchList() {
  bookingBranch.value = null
}

// 切换日期
function selectDate(offset: number) {
  selectedDateOffset.value = offset
  renderTimeSlots()
}

// 切换业务类型
function onBizTypeChange() {
  renderTimeSlots()
}

// 前端网点ID → 后端网点ID 映射
const BRANCH_ID_MAP: Record<string, number> = { b1: 1, b2: 2, b3: 3, b4: 4, b5: 1, b6: 2 }

// 确认预约
async function confirmBooking() {
  if (selectedSlotIdx.value < 0) return
  if (!bookingBranch.value) return

  const branchId = BRANCH_ID_MAP[bookingBranch.value.id] || 1
  const d = new Date()
  d.setDate(d.getDate() + selectedDateOffset.value)
  const dateStr = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  const slot = slotData.value[selectedSlotIdx.value]
  const slotText = `${slot.start}-${slot.end}`
  const bizText = bizOptions.find(o => o.value === selectedBizType.value)?.label || '-'

  try {
    const newAppt = await appt.book({
      branchId,
      branchName: bookingBranch.value.name,
      date: dateStr,
      timeSlot: slotText,
      businessType: bizText,
    })
    voucherNum.value = newAppt.voucherNum || ('V' + Math.floor(100 + Math.random() * 900))
    voucherActivated.value = false
    successModal.value = true
  } catch (e: any) {
    showToast(e.message || '预约失败，请稍后重试')
  }
}

// 激活预约
function activateAppointment() {
  if (!voucherNum.value) return
  voucherNum.value = 'A' + voucherNum.value.substring(1)
  voucherActivated.value = true
}

// 关闭成功弹窗
function closeSuccessModal() {
  successModal.value = false
  bookingBranch.value = null
}

// 成功弹窗中显示的到店日期
const successDateText = computed(() => {
  const d = new Date()
  d.setDate(d.getDate() + selectedDateOffset.value)
  return `${d.getMonth() + 1}月${d.getDate()}日`
})

// 时段卡片样式（复刻 renderTimeSlots / selectTimeSlot）
function getSlotClass(s: { capacity: number; isPast: boolean }, idx: number): string {
  const base = 'slot-btn p-3 rounded-2xl border transition-all text-left group relative '
  if (s.isPast) return base + 'border-slate-200/40 bg-slate-50/50 opacity-50 cursor-not-allowed'
  if (s.capacity === 0) return base + 'border-slate-200/40 bg-slate-50/50 cursor-not-allowed'
  if (idx === selectedSlotIdx.value) return base + 'border-blue-500 bg-blue-50/80 shadow-xs ring-2 ring-blue-500/20 hover:bg-blue-50 cursor-pointer'
  if (s.capacity <= 3) return base + 'border-slate-200/80 bg-white/60 hover:border-amber-400 hover:bg-amber-50/50 cursor-pointer'
  return base + 'border-slate-200/80 bg-white/60 hover:border-blue-500 hover:bg-blue-50/50 cursor-pointer'
}

function getSlotTimeClass(s: { capacity: number; isPast: boolean }, idx: number): string {
  const base = 'text-xs font-black '
  if (s.isPast) return base + 'text-slate-400 line-through'
  if (s.capacity === 0) return base + 'text-slate-400'
  if (idx === selectedSlotIdx.value) return base + 'text-blue-600'
  return base + 'text-slate-800'
}
</script>

<template>
  <div>
  <div class="p-6 space-y-4" v-if="!bookingBranch">
    <!-- 顶栏说明与状态指示 -->
    <div class="bg-white/50 backdrop-blur-2xl p-4 rounded-2xl border border-slate-200/60 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-3">
      <div class="flex items-center space-x-3">
        <div class="w-9 h-9 rounded-xl bg-emerald-500/10 flex items-center justify-center text-emerald-600 shrink-0">
          <i class="fa-solid fa-building-columns text-base"></i>
        </div>
        <div>
          <h3 class="text-sm font-black text-slate-800">全域智慧网点实时客流与调度舱</h3>
          <p class="text-xs text-slate-500 font-medium">实时同步周边网点拥挤度 · 智能分流与排队预测</p>
        </div>
      </div>
      <div class="flex items-center space-x-2 text-[11px] font-bold text-slate-600 flex-wrap">
        <span class="inline-flex items-center gap-1.5 bg-emerald-50 text-emerald-600 px-2.5 py-1 rounded-lg border border-emerald-200/60">
          <span class="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span> 畅通 ({{ statusCounts.free }})
        </span>
        <span class="inline-flex items-center gap-1.5 bg-amber-50 text-amber-600 px-2.5 py-1 rounded-lg border border-amber-200/60">
          <span class="w-2 h-2 rounded-full bg-amber-500"></span> 适中 ({{ statusCounts.moderate }})
        </span>
        <span class="inline-flex items-center gap-1.5 bg-rose-50 text-rose-600 px-2.5 py-1 rounded-lg border border-rose-200/60">
          <span class="w-2 h-2 rounded-full bg-rose-500"></span> 繁忙 ({{ statusCounts.busy }})
        </span>
        <button @click="checkExpired(); selectedApptSet = new Set(); showMyAppointments = true"
          class="flex items-center gap-1.5 text-slate-600 hover:text-blue-600 bg-slate-100 hover:bg-blue-50 px-2.5 py-1 rounded-lg border border-slate-200 transition-all cursor-pointer">
          <i class="fa-solid fa-clipboard-list text-xs"></i>
          <span>我的预约</span>
          <span v-if="appt.activeAppointments.length > 0"
            class="bg-rose-500 text-white text-[9px] font-black px-1.5 py-0.5 rounded-full min-w-[16px] text-center">
            {{ appt.activeAppointments.length }}
          </span>
        </button>
      </div>
    </div>

    <!-- 智能筛选栏 -->
    <div class="bg-white/60 backdrop-blur-2xl p-3 rounded-2xl border border-slate-200/60 shadow-sm">
      <div class="flex flex-wrap items-center gap-3">
        <span class="text-[11px] font-black text-slate-500 uppercase tracking-wider flex items-center gap-1">
          <i class="fa-solid fa-filter text-blue-500"></i> 智能筛选：
        </span>

        <div class="flex items-center gap-1.5">
          <span class="text-[10px] font-bold text-slate-400">客流</span>
          <select v-model="filterStatus"
            class="bg-white border border-slate-200 rounded-lg text-[11px] font-bold px-2 py-1 focus:outline-none cursor-pointer">
            <option value="all">全部</option>
            <option value="free">畅通</option>
            <option value="moderate">适中</option>
            <option value="busy">繁忙</option>
          </select>
        </div>

        <div class="flex items-center gap-1.5">
          <span class="text-[10px] font-bold text-slate-400">距离</span>
          <select v-model="filterDistance"
            class="bg-white border border-slate-200 rounded-lg text-[11px] font-bold px-2 py-1 focus:outline-none cursor-pointer">
            <option value="all">全部</option>
            <option value="near">1km 内</option>
            <option value="mid">1-3km</option>
            <option value="far">3km 以上</option>
          </select>
        </div>

        <div class="flex items-center gap-1.5">
          <span class="text-[10px] font-bold text-slate-400">业务</span>
          <select v-model="filterService"
            class="bg-white border border-slate-200 rounded-lg text-[11px] font-bold px-2 py-1 focus:outline-none cursor-pointer">
            <option v-for="opt in serviceOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>
        </div>

        <div class="flex items-center gap-1.5">
          <span class="text-[10px] font-bold text-slate-400">排序</span>
          <select v-model="filterSort"
            class="bg-white border border-slate-200 rounded-lg text-[11px] font-bold px-2 py-1 focus:outline-none cursor-pointer">
            <option value="recommend">智能推荐</option>
            <option value="distance">距离最近</option>
            <option value="wait">等待最短</option>
            <option value="flow">客流最少</option>
          </select>
        </div>

        <button @click="resetFilter"
          class="text-[10px] font-bold text-slate-500 hover:text-blue-600 bg-slate-100 hover:bg-blue-50 px-2 py-1 rounded-lg border border-slate-200 transition-all cursor-pointer flex items-center gap-1">
          <i class="fa-solid fa-rotate-left"></i> 重置
        </button>

        <span class="ml-auto text-[10px] font-bold text-slate-500">
          共 {{ filteredBranches.length }} 个网点
        </span>
      </div>
    </div>

    <!-- AI 智能推荐置顶条 -->
    <div v-if="aiRecommendText"
      class="bg-gradient-to-r from-blue-50 via-cyan-50 to-emerald-50 p-4 rounded-2xl border border-cyan-200/60 shadow-sm">
      <div class="flex items-start gap-3">
        <div class="w-9 h-9 rounded-xl bg-gradient-to-br from-blue-600 to-cyan-500 flex items-center justify-center text-white shrink-0 shadow-md">
          <i class="fa-solid fa-wand-magic-sparkles text-sm"></i>
        </div>
        <div class="flex-1 min-w-0">
          <div class="flex items-center gap-2 mb-1">
            <span class="text-[11px] font-black text-slate-800">灵枢 AI 智能推荐</span>
            <span class="text-[9px] font-bold text-cyan-700 bg-cyan-100 px-1.5 py-0.5 rounded">置顶</span>
          </div>
          <p class="text-[11px] font-medium text-slate-600 leading-relaxed" v-html="aiRecommendText.replace(/推荐前往 (\S+)（/, '推荐前往 <b class=&quot;text-slate-800&quot;>$1</b>（')"></p>
        </div>
      </div>
    </div>

    <!-- 网点格栅 -->
    <div v-if="filteredBranches.length > 0" class="grid grid-cols-1 md:grid-cols-2 gap-4">
      <div v-for="(b, idx) in filteredBranches" :key="b.id"
        class="branch-card bg-white/60 backdrop-blur-2xl p-5 rounded-3xl border border-slate-200/80 shadow-sm flex flex-col justify-between space-y-4 tech-card relative overflow-hidden transition-all hover:shadow-md hover:border-cyan-300/50">

        <!-- 收藏按钮 -->
        <button @click="branchStore.toggleFavorite(b.id)"
          :class="[
            'fav-btn absolute top-3 right-14 z-10 w-7 h-7 rounded-full bg-white/80 backdrop-blur flex items-center justify-center transition-all cursor-pointer',
            b.favorite ? 'text-amber-400' : 'text-slate-400 hover:text-rose-500'
          ]">
          <i :class="b.favorite ? 'fa-solid fa-star text-xs' : 'fa-regular fa-star text-xs'"></i>
        </button>

        <!-- 名称与状态 -->
        <div class="flex justify-between items-start">
          <div class="space-y-1">
            <div class="flex items-center gap-2">
              <h4 class="text-sm font-black text-slate-800">{{ b.name }}</h4>
              <span :class="['text-[9px] font-bold px-2 py-0.5 rounded-full border flex items-center gap-1', getStatusBadge(b.status).cls]">
                <span :class="['w-1.5 h-1.5 rounded-full animate-pulse', getStatusBadge(b.status).dotCls]"></span>{{ getStatusBadge(b.status).text }}
              </span>
            </div>
            <p class="text-[11px] text-slate-500 font-medium flex items-center gap-1">
              <i class="fa-solid fa-location-dot text-slate-400"></i> {{ b.address }} (距您 {{ formatDistance(b.distance) }})
            </p>
          </div>
          <div :class="['w-9 h-9 rounded-xl flex items-center justify-center shrink-0', b.iconBg, b.iconColor]">
            <i :class="[b.icon, 'text-sm']"></i>
          </div>
        </div>

        <!-- 核心指标 -->
        <div class="grid grid-cols-2 gap-3 bg-slate-50/80 p-3 rounded-2xl border border-slate-100">
          <div class="space-y-0.5">
            <span class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">实时在店人数</span>
            <div class="flex items-baseline space-x-1">
              <span :id="`flow-${idx}`" class="text-xl font-black text-slate-800">0</span>
              <span class="text-[10px] font-bold text-slate-500">人</span>
            </div>
          </div>
          <div class="space-y-0.5 border-l border-slate-200/60 pl-3">
            <span class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">线上预约人数</span>
            <div class="flex items-baseline space-x-1">
              <span :id="`reserve-${idx}`" class="text-xl font-black text-cyan-600">0</span>
              <span class="text-[10px] font-bold text-slate-500">人</span>
            </div>
          </div>
        </div>

        <!-- 等待时间趋势迷你图 -->
        <div class="flex items-center justify-between gap-2 bg-slate-50/80 p-2 rounded-xl border border-slate-100">
          <span class="text-[10px] font-bold text-slate-500 flex items-center gap-1 shrink-0">
            <i class="fa-solid fa-chart-line text-blue-500"></i>等待趋势
          </span>
          <svg class="trend-svg flex-1 h-6" viewBox="0 0 100 24" preserveAspectRatio="none">
            <path :d="buildTrendAreaPath(b.trend)" :fill="getTrendColor(b.trend)" fill-opacity="0.12" />
            <path :d="buildTrendLinePath(b.trend)" fill="none" :stroke="getTrendColor(b.trend)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
            <circle :cx="buildTrendEndDot(b.trend).cx" :cy="buildTrendEndDot(b.trend).cy" r="1.8" :fill="getTrendColor(b.trend)" />
          </svg>
          <span :class="['text-[10px] font-black shrink-0', getWaitColor(b.status)]">
            {{ b.wait < 3 ? '< 3' : '~' + b.wait }} 分钟
          </span>
        </div>

        <!-- AI预测办理行 -->
        <div class="ai-predict-row flex items-center justify-between gap-2 bg-gradient-to-r from-indigo-50 to-purple-50 p-2 rounded-xl border border-indigo-100">
          <span class="text-[10px] font-bold text-indigo-600 flex items-center gap-1 shrink-0">
            <i class="fa-solid fa-brain text-indigo-500"></i>AI预测办理
          </span>
          <span class="text-[10px] font-black text-purple-600">~{{ predictServiceTime(b).time }} 分钟</span>
          <span class="text-[9px] font-bold text-indigo-400 flex items-center gap-0.5 shrink-0">
            <i class="fa-solid fa-shield-halved text-[8px]"></i>置信{{ predictServiceTime(b).confidence }}%
          </span>
        </div>

        <!-- 网点辅助详情 -->
        <div class="space-y-2">
          <div class="flex justify-between text-[11px] font-bold text-slate-600">
            <span class="flex items-center gap-1"><i class="fa-solid fa-door-open text-blue-500"></i> 开放窗口: {{ b.window }} 个</span>
            <span class="flex items-center gap-1">
              <i :class="b.status === 'free' ? 'fa-solid fa-clock text-emerald-500' : 'fa-solid fa-clock text-amber-500'"></i>
              预计等待: {{ b.wait < 3 ? '< 3' : '~' + b.wait }} 分钟
            </span>
          </div>
          <div class="flex flex-wrap gap-1">
            <span v-for="s in b.services" :key="s"
              class="text-[9px] font-bold bg-slate-100 text-slate-600 px-2 py-0.5 rounded">
              {{ serviceTagText[s] || s }}
            </span>
          </div>
        </div>

        <!-- 右下角预约操作区 -->
        <div class="flex justify-between items-center pt-2 border-t border-slate-200/50 gap-2">
          <div class="flex items-center gap-1">
            <button @click="openBranchDetail(b)"
              class="text-[10px] font-bold text-slate-500 hover:text-blue-600 bg-slate-100 hover:bg-blue-50 px-2 py-1.5 rounded-lg border border-slate-200 transition-all cursor-pointer flex items-center gap-1">
              <i class="fa-solid fa-circle-info"></i> 详情
            </button>
            <button @click="navigateToBranch(b.address)"
              class="text-[10px] font-bold text-slate-500 hover:text-emerald-600 bg-slate-100 hover:bg-emerald-50 px-2 py-1.5 rounded-lg border border-slate-200 transition-all cursor-pointer flex items-center gap-1">
              <i class="fa-solid fa-route"></i> 导航
            </button>
          </div>
          <button @click="handleAppointment(b)"
            class="bg-gradient-to-r from-blue-600 to-cyan-500 hover:from-blue-700 hover:to-cyan-600 text-white text-xs font-bold px-4 py-2 rounded-xl shadow-md transition-all cursor-pointer flex items-center gap-1.5">
            <i class="fa-solid fa-ticket text-xs"></i>
            <span>预约取号</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 筛选无结果空状态 -->
    <div v-else class="bg-white/50 p-8 rounded-2xl border border-dashed border-slate-300 text-center">
      <i class="fa-solid fa-magnifying-glass text-3xl text-slate-300 mb-2"></i>
      <p class="text-sm font-bold text-slate-500">没有符合筛选条件的网点</p>
      <button @click="resetFilter" class="mt-3 text-xs font-bold text-blue-600 hover:text-blue-700 cursor-pointer">
        <i class="fa-solid fa-rotate-left"></i> 重置筛选条件
      </button>
    </div>

    <!-- 网点详情 Modal -->
    <div v-if="detailBranch" class="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4" @click.self="detailBranch = null">
      <div class="bg-white rounded-3xl shadow-2xl w-full max-w-md max-h-[85vh] border border-slate-200/80 overflow-hidden flex flex-col animate-fade-in">
        <div class="bg-gradient-to-r from-blue-600 via-cyan-500 to-emerald-500 p-4 shrink-0">
          <div class="flex items-center justify-between">
            <div class="flex items-center space-x-2 min-w-0">
              <div class="w-8 h-8 rounded-lg bg-white/20 flex items-center justify-center text-white shrink-0">
                <i class="fa-solid fa-building-columns text-sm"></i>
              </div>
              <h3 class="text-sm font-black text-white truncate">{{ detailBranch.name }}</h3>
            </div>
            <button @click="detailBranch = null"
              class="w-8 h-8 rounded-lg bg-white/20 flex items-center justify-center text-white hover:bg-white/30 transition-all cursor-pointer shrink-0">
              <i class="fa-solid fa-xmark text-sm"></i>
            </button>
          </div>
        </div>
        <div class="p-5 space-y-4 overflow-y-auto flex-1">
          <!-- 状态与距离 -->
          <div class="flex items-center gap-2">
            <span :class="['text-[10px] font-bold px-2 py-0.5 rounded-full border flex items-center gap-1', getStatusBadge(detailBranch.status).cls]">
              <span :class="['w-1.5 h-1.5 rounded-full animate-pulse', getStatusBadge(detailBranch.status).dotCls]"></span>{{ getStatusBadge(detailBranch.status).text }}
            </span>
            <span class="text-[10px] font-bold text-slate-500 flex items-center gap-1">
              <i class="fa-solid fa-location-dot text-slate-400"></i> 距您 {{ formatDistance(detailBranch.distance) }}
            </span>
          </div>

          <!-- 核心指标 4 宫格 -->
          <div class="grid grid-cols-2 gap-2">
            <div class="bg-slate-50 rounded-xl p-3 border border-slate-100">
              <div class="text-[10px] font-bold text-slate-400">实时在店</div>
              <div class="text-lg font-black text-slate-800">{{ detailBranch.flow }} 人</div>
            </div>
            <div class="bg-slate-50 rounded-xl p-3 border border-slate-100">
              <div class="text-[10px] font-bold text-slate-400">线上预约</div>
              <div class="text-lg font-black text-cyan-600">{{ detailBranch.reserve }} 人</div>
            </div>
            <div class="bg-slate-50 rounded-xl p-3 border border-slate-100">
              <div class="text-[10px] font-bold text-slate-400">开放窗口</div>
              <div class="text-lg font-black text-blue-600">{{ detailBranch.window }}</div>
            </div>
            <div class="bg-slate-50 rounded-xl p-3 border border-slate-100">
              <div class="text-[10px] font-bold text-slate-400">预计等待</div>
              <div class="text-lg font-black text-amber-600">~{{ detailBranch.wait }} 分钟</div>
            </div>
          </div>

          <!-- 近 6 时段等待趋势大图 -->
          <div class="bg-slate-50 rounded-2xl p-3 border border-slate-100">
            <div class="flex items-center justify-between mb-2">
              <span class="text-[10px] font-black text-slate-500 uppercase tracking-wider">近 6 时段等待趋势</span>
              <span class="text-[10px] font-bold text-slate-400">单位：分钟</span>
            </div>
            <svg viewBox="0 0 280 60" class="w-full h-14" preserveAspectRatio="none">
              <path :d="buildTrendAreaPathLarge(detailBranch.trend)" :fill="getTrendColor(detailBranch.trend)" fill-opacity="0.12" />
              <path :d="buildTrendLinePathLarge(detailBranch.trend)" fill="none" :stroke="getTrendColor(detailBranch.trend)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
              <circle :cx="buildTrendEndDotLarge(detailBranch.trend).cx" :cy="buildTrendEndDotLarge(detailBranch.trend).cy" r="3" :fill="getTrendColor(detailBranch.trend)" />
            </svg>
            <div class="flex justify-between mt-1 text-[9px] font-bold text-slate-400">
              <span v-for="(v, i) in detailBranch.trend" :key="i">{{ v }}</span>
            </div>
          </div>

          <!-- AI客流预测热力图 -->
          <div class="bg-gradient-to-br from-indigo-50 to-purple-50 rounded-2xl p-3 border border-indigo-100">
            <div class="flex items-center justify-between mb-2">
              <span class="text-[10px] font-black text-indigo-600 flex items-center gap-1">
                <i class="fa-solid fa-brain text-indigo-500"></i>AI客流预测热力图
              </span>
              <span class="text-[9px] font-bold text-indigo-400">基于历史30天数据</span>
            </div>
            <div class="flex items-end gap-1 bg-white/50 rounded-xl p-2">
              <div v-for="(h, i) in getBranchHeatmap(detailBranch)" :key="i" class="flex flex-col items-center gap-1 flex-1">
                <span :class="['text-[8px] font-black', h.isBest ? 'text-emerald-600' : 'text-slate-400']">{{ h.val }}</span>
                <div class="w-full flex items-end justify-center" style="height:52px">
                  <div :class="['w-3.5 rounded-t-md transition-all', h.bgClass, h.isBest ? 'ring-2 ring-emerald-500 ring-offset-1' : '']" :style="{ height: h.barHeight + 'px' }"></div>
                </div>
                <span :class="['text-[8px] font-bold', h.isBest ? 'text-emerald-600' : 'text-slate-400']">{{ h.hour }}</span>
              </div>
            </div>
            <div class="flex items-center justify-center gap-3 mt-2 text-[8px] font-bold text-slate-400">
              <span class="flex items-center gap-0.5"><span class="w-2 h-2 rounded-sm bg-emerald-400"></span>畅通</span>
              <span class="flex items-center gap-0.5"><span class="w-2 h-2 rounded-sm bg-amber-400"></span>适中</span>
              <span class="flex items-center gap-0.5"><span class="w-2 h-2 rounded-sm bg-rose-400"></span>繁忙</span>
            </div>
            <div class="mt-2 bg-emerald-50 rounded-lg p-2 border border-emerald-200 flex items-center gap-2">
              <i class="fa-solid fa-lightbulb text-emerald-500 text-xs"></i>
              <span class="text-[10px] font-bold text-emerald-700">AI推荐：{{ getBranchHeatmapBest(detailBranch).bestHour }}-{{ getBranchHeatmapBest(detailBranch).bestEnd }} 客流最少（预计{{ getBranchHeatmapBest(detailBranch).minVal }}%负载），建议此时段到店</span>
            </div>
          </div>

          <!-- 地址、电话、营业时间 -->
          <div class="space-y-2">
            <div class="flex items-center gap-2 text-[11px] font-bold text-slate-600">
              <i class="fa-solid fa-location-dot text-slate-400 w-4 text-center"></i><span class="text-slate-500 font-medium">{{ detailBranch.address }}</span>
            </div>
            <div class="flex items-center gap-2 text-[11px] font-bold text-slate-600">
              <i class="fa-solid fa-phone text-slate-400 w-4 text-center"></i><span class="text-slate-500 font-medium">{{ detailBranch.phone }}</span>
            </div>
            <div class="flex items-center gap-2 text-[11px] font-bold text-slate-600">
              <i class="fa-solid fa-clock text-slate-400 w-4 text-center"></i><span class="text-slate-500 font-medium">营业时间：{{ detailBranch.hours }}</span>
            </div>
          </div>

          <!-- 可办理业务 -->
          <div>
            <div class="text-[10px] font-black text-slate-500 uppercase tracking-wider mb-2">可办理业务</div>
            <div class="flex flex-wrap gap-1">
              <span v-for="s in detailBranch.services" :key="s"
                class="text-[10px] font-bold bg-slate-100 text-slate-600 px-2 py-1 rounded">
                {{ serviceTagText[s] || s }}
              </span>
            </div>
          </div>
        </div>
        <!-- 底部操作栏：关闭 + 导航 + 预约 -->
        <div class="border-t border-slate-200/60 p-3 shrink-0 bg-white flex gap-2">
          <button @click="detailBranch = null"
            class="flex-1 px-4 py-2.5 text-xs font-bold text-slate-600 bg-slate-100 hover:bg-slate-200 rounded-xl transition-all cursor-pointer">
            关闭
          </button>
          <button @click="navigateToBranch(detailBranch.address)"
            class="flex-1 px-4 py-2.5 text-xs font-bold text-white bg-gradient-to-r from-emerald-500 to-cyan-500 hover:from-emerald-600 hover:to-cyan-600 rounded-xl transition-all cursor-pointer flex items-center justify-center gap-1.5">
            <i class="fa-solid fa-route"></i> 一键导航
          </button>
          <button @click="handleAppointment(detailBranch); detailBranch = null"
            class="flex-1 px-4 py-2.5 text-xs font-bold text-white bg-gradient-to-r from-blue-600 to-cyan-500 hover:from-blue-700 hover:to-cyan-600 rounded-xl transition-all cursor-pointer flex items-center justify-center gap-1.5">
            <i class="fa-solid fa-ticket"></i> 预约取号
          </button>
        </div>
      </div>
    </div>

    <!-- 我的预约 Modal -->
    <div v-if="showMyAppointments" class="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4" @click.self="showMyAppointments = false">
      <div class="bg-white rounded-3xl w-full max-w-md max-h-[85vh] shadow-2xl border border-slate-200/80 overflow-hidden flex flex-col animate-fade-in">
        <div class="bg-gradient-to-r from-blue-600 via-cyan-500 to-emerald-500 p-4 shrink-0">
          <div class="flex items-center justify-between">
            <div class="flex items-center space-x-2">
              <div class="w-8 h-8 rounded-lg bg-white/20 flex items-center justify-center text-white shrink-0">
                <i class="fa-solid fa-clipboard-list text-sm"></i>
              </div>
              <span class="text-sm font-black text-white">我的预约记录</span>
            </div>
            <button @click="showMyAppointments = false"
              class="w-8 h-8 rounded-lg bg-white/20 flex items-center justify-center text-white hover:bg-white/30 transition-all cursor-pointer">
              <i class="fa-solid fa-xmark text-sm"></i>
            </button>
          </div>
        </div>
        <div class="p-4 overflow-y-auto flex-1 space-y-3">
          <div v-if="appt.appointments.length === 0" class="text-center py-8">
            <i class="fa-solid fa-clipboard text-3xl text-slate-300 mb-2"></i>
            <p class="text-sm font-bold text-slate-400">暂无预约记录</p>
            <p class="text-[10px] text-slate-400 mt-1">预约成功后此处可查看凭证与详情</p>
          </div>
          <div v-if="appt.appointments.length > 0" class="flex items-center justify-between">
            <span class="text-[10px] font-black text-slate-500">我的预约列表</span>
            <button @click="deleteSelectedAppts"
              class="bg-white border border-red-200 text-[10px] font-bold px-2.5 py-1 rounded-lg hover:bg-red-50 transition-all cursor-pointer flex items-center gap-1 text-red-600">
              <i class="fa-solid fa-trash-can"></i>
              <span>批量删除</span>
              <span v-if="selectedApptSet.size > 0" class="bg-red-100 text-red-600 px-1.5 rounded-full">{{ selectedApptSet.size }}</span>
            </button>
          </div>
          <div v-for="a in appt.appointments" :key="a.voucherNum"
            @click="viewApptDetail(a.voucherNum)"
            :class="['bg-slate-50 rounded-2xl p-3 border border-slate-100 space-y-2 relative cursor-pointer hover:border-blue-300 hover:bg-blue-50/30 transition-all', a.status === 'expired' ? 'opacity-60' : '']">
            <!-- 复选框 -->
            <input type="checkbox" :checked="selectedApptSet.has(a.voucherNum)"
              @click.stop="toggleApptSelect(a.voucherNum)"
              class="absolute top-3 right-3 w-4 h-4 rounded border-slate-300 text-red-600 focus:ring-red-500 cursor-pointer">
            <!-- 凭证号 + 状态徽章 + 操作按钮 -->
            <div class="flex items-center justify-between pr-8">
              <div class="flex items-center gap-2">
                <span :class="[
                  'text-base font-black tracking-wider',
                  a.status === 'active' ? 'text-rose-500' :
                  a.status === 'expired' ? 'text-slate-400 line-through' :
                  a.status === 'completed' ? 'text-slate-400 line-through' : 'text-slate-400'
                ]">{{ a.voucherNum }}</span>
                <span v-if="a.status === 'active'"
                  class="text-[9px] font-bold bg-rose-100 text-rose-600 px-2 py-0.5 rounded-full border border-rose-200 flex items-center gap-1">
                  <span class="w-1.5 h-1.5 rounded-full bg-rose-500 animate-pulse"></span>已激活
                </span>
                <span v-else-if="a.status === 'expired'"
                  class="text-[9px] font-bold bg-slate-100 text-slate-500 px-2 py-0.5 rounded-full border border-slate-200">已失效</span>
                <span v-else-if="a.status === 'completed'"
                  class="text-[9px] font-bold bg-slate-100 text-slate-500 px-2 py-0.5 rounded-full border border-slate-200">已完成</span>
                <span v-else
                  class="text-[9px] font-bold bg-slate-100 text-slate-500 px-2 py-0.5 rounded-full border border-slate-200 flex items-center gap-1">
                  <i class="fa-solid fa-clock text-[8px]"></i>待激活
                </span>
              </div>
              <div class="flex items-center gap-1">
                <button v-if="a.status === 'virtual'" @click="activateFromList(a.voucherNum)"
                  class="text-[10px] font-bold text-emerald-600 hover:text-emerald-700 bg-emerald-50 hover:bg-emerald-100 px-2 py-1 rounded-lg border border-emerald-200 transition-all cursor-pointer">
                  <i class="fa-solid fa-qrcode mr-0.5"></i>激活
                </button>
                <button v-if="a.status === 'virtual' || a.status === 'active'" @click="cancelFromList(a.voucherNum)"
                  class="text-[10px] font-bold text-rose-500 hover:text-rose-700 bg-rose-50 hover:bg-rose-100 px-2 py-1 rounded-lg border border-rose-200 transition-all cursor-pointer">
                  <i class="fa-solid fa-xmark" :class="a.status === 'active' ? 'mr-0.5' : ''"></i>{{ a.status === 'active' ? '取消' : '' }}
                </button>
              </div>
            </div>
            <!-- 详细信息 -->
            <div class="space-y-1 text-[11px] font-bold text-slate-600">
              <div class="flex items-center gap-1.5">
                <i class="fa-solid fa-building-columns text-slate-400 w-3.5 text-center"></i>
                <span class="text-slate-800">{{ a.branchName }}</span>
              </div>
              <div class="flex items-center gap-1.5">
                <i class="fa-solid fa-calendar text-slate-400 w-3.5 text-center"></i>
                <span>{{ a.date }}</span> · <i class="fa-solid fa-clock text-slate-400"></i>
                <span class="text-blue-600">{{ a.timeSlot }}</span>
              </div>
              <div class="flex items-center gap-1.5">
                <i class="fa-solid fa-list-check text-slate-400 w-3.5 text-center"></i>
                <span class="text-slate-500">{{ a.businessType || '-' }}</span>
              </div>
              <div class="flex items-center gap-1.5">
                <i class="fa-solid fa-users text-slate-400 w-3.5 text-center"></i>
                <span>排队位置：第 {{ a.aheadCount || '-' }} 位</span>
              </div>
            </div>
            <!-- 迷你激活二维码（仅待激活状态显示） -->
            <div v-if="a.status === 'virtual'" class="flex items-center gap-2 bg-white rounded-xl p-2 border border-slate-200">
              <div class="w-12 h-12 bg-white border border-slate-300 p-1 shrink-0">
                <div class="grid grid-cols-3 gap-0.5 w-full h-full">
                  <div class="border border-slate-700 aspect-square rounded-sm"></div>
                  <div class="flex flex-col justify-between p-0.5"><span class="block bg-slate-700 h-0.5 w-full"></span><span class="block bg-slate-700 h-0.5 w-1/2"></span></div>
                  <div class="border border-slate-700 aspect-square rounded-sm ml-auto"></div>
                  <div class="col-span-2 space-y-0.5"><span class="block bg-slate-700 h-0.5 w-full"></span><span class="block bg-slate-700 h-0.5 w-2/3"></span></div>
                  <div class="bg-slate-700 aspect-square m-auto"></div>
                  <div class="border border-slate-700 aspect-square rounded-sm mt-auto"></div>
                  <div class="col-span-2 flex flex-col justify-end gap-0.5 mt-auto"><span class="block bg-slate-700 h-0.5 w-full"></span><span class="block bg-slate-700 h-0.5 w-1/2"></span></div>
                </div>
              </div>
              <div class="text-[10px] font-bold text-slate-500 leading-tight">到店扫描此码激活<br><span class="text-slate-400">转为红色预约号享优先叫号</span></div>
            </div>
          </div>
        </div>
        <div class="border-t border-slate-200/60 p-3 shrink-0 bg-white">
          <button @click="showMyAppointments = false"
            class="w-full px-4 py-2.5 text-xs font-bold text-slate-600 bg-slate-100 hover:bg-slate-200 rounded-xl transition-all cursor-pointer">
            关闭
          </button>
        </div>
      </div>
    </div>

    <!-- 预约详情 Modal -->
    <div v-if="detailAppt" class="fixed inset-0 z-[55] bg-black/50 backdrop-blur-sm flex items-center justify-center p-4" @click.self="detailAppt = null">
      <div class="bg-white rounded-3xl w-full max-w-sm shadow-2xl border border-slate-200/80 overflow-hidden flex flex-col max-h-[85vh] animate-fade-in">
        <!-- 头部 -->
        <div class="bg-gradient-to-r from-blue-600 via-cyan-500 to-emerald-500 p-4 shrink-0">
          <div class="flex items-center justify-between">
            <div class="flex items-center space-x-2">
              <div class="w-8 h-8 rounded-lg bg-white/20 flex items-center justify-center text-white shrink-0">
                <i class="fa-solid fa-qrcode text-sm"></i>
              </div>
              <span class="text-sm font-black text-white">预约凭证详情</span>
            </div>
            <button @click="detailAppt = null"
              class="w-8 h-8 rounded-lg bg-white/20 flex items-center justify-center text-white hover:bg-white/30 transition-all cursor-pointer">
              <i class="fa-solid fa-xmark text-sm"></i>
            </button>
          </div>
        </div>

        <!-- 内容 -->
        <div class="p-5 overflow-y-auto flex-1">
          <!-- 二维码 -->
          <div class="text-center mb-4">
            <span class="text-[10px] font-bold text-slate-500 uppercase tracking-wider block mb-2">预约凭证二维码</span>
            <div class="inline-block bg-white rounded-xl p-3 border-2 border-cyan-400 shadow-md">
              <div class="relative p-1.5 bg-white border border-slate-200 rounded-lg shadow-inner">
                <div class="w-28 h-28 flex flex-wrap items-center justify-center text-slate-800 opacity-90">
                  <div class="grid grid-cols-3 gap-1.5 w-full h-full p-0.5">
                    <div class="border-[3px] border-slate-800 aspect-square rounded-sm"></div>
                    <div class="flex flex-col justify-between p-0.5"><span class="block bg-slate-800 h-0.5 w-full"></span><span class="block bg-slate-800 h-0.5 w-1/2"></span></div>
                    <div class="border-[3px] border-slate-800 aspect-square rounded-sm ml-auto"></div>
                    <div class="col-span-2 space-y-0.5"><span class="block bg-slate-800 h-0.5 w-full"></span><span class="block bg-slate-800 h-0.5 w-3/4"></span></div>
                    <div class="bg-slate-800 aspect-square m-auto"></div>
                    <div class="border-[3px] border-slate-800 aspect-square rounded-sm mt-auto"></div>
                    <div class="col-span-2 flex flex-col justify-end gap-0.5 mt-auto"><span class="block bg-slate-800 h-0.5 w-full"></span><span class="block bg-slate-800 h-0.5 w-1/2"></span></div>
                  </div>
                </div>
                <div class="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 bg-blue-600 text-white text-[7px] font-black px-1 rounded shadow border border-white scale-90">直通</div>
              </div>
              <div class="mt-1.5 text-[10px] font-mono font-bold text-slate-700">
                <span class="text-slate-400">SN:</span> <span class="text-blue-600">{{ detailAppt.voucherNum }}</span>
              </div>
            </div>
            <p class="text-[9px] text-slate-400 mt-1.5">到店出示此码，柜面扫码快速办理</p>
          </div>

          <div class="border-t border-slate-200/60 my-2"></div>

          <!-- 凭证号 + 状态 -->
          <div class="flex items-center justify-between mb-4">
            <div>
              <h3 class="text-sm font-black text-slate-800">预约详情</h3>
              <p class="text-xs text-slate-500">凭证号: {{ detailAppt.voucherNum }}</p>
            </div>
            <span class="text-[9px] font-bold px-2 py-1 rounded"
              :class="detailAppt.status === 'active' ? 'bg-rose-100 text-rose-600' :
                detailAppt.status === 'expired' ? 'bg-slate-100 text-slate-500' :
                detailAppt.status === 'completed' ? 'bg-emerald-100 text-emerald-600' :
                'bg-slate-100 text-slate-500'">
              {{ detailAppt.status === 'active' ? '已激活' :
                detailAppt.status === 'expired' ? '已失效' :
                detailAppt.status === 'completed' ? '已完成' : '待激活' }}
            </span>
          </div>

          <!-- 详细信息 -->
          <div class="grid grid-cols-2 gap-3 text-xs">
            <div><span class="text-slate-400">网点:</span> <span class="font-bold text-slate-800">{{ detailAppt.branchName }}</span></div>
            <div><span class="text-slate-400">日期:</span> <span class="font-bold text-slate-800">{{ detailAppt.date }}</span></div>
            <div><span class="text-slate-400">时段:</span> <span class="font-bold text-blue-600">{{ detailAppt.timeSlot }}</span></div>
            <div><span class="text-slate-400">业务类型:</span> <span class="font-bold text-slate-800">{{ detailAppt.businessType || '-' }}</span></div>
            <div><span class="text-slate-400">排队位置:</span> <span class="font-bold text-rose-600">第 {{ detailAppt.aheadCount ?? '-' }} 位</span></div>
            <div><span class="text-slate-400">分配窗口:</span> <span class="font-bold text-slate-800">{{ detailAppt.window || '待分配' }}</span></div>
          </div>
        </div>

        <!-- 底部操作 -->
        <div class="border-t border-slate-200/60 p-4 shrink-0 bg-white flex items-center justify-end space-x-2">
          <button @click="detailAppt = null"
            class="bg-white border border-slate-200 text-xs font-bold px-4 py-2 rounded-xl hover:bg-slate-50 transition-all cursor-pointer text-slate-700">
            关闭
          </button>
          <button v-if="detailAppt.status === 'virtual'" @click="activateFromList(detailAppt.voucherNum); detailAppt = null"
            class="bg-emerald-600 text-white text-xs font-bold px-4 py-2 rounded-xl hover:bg-emerald-700 transition-all cursor-pointer flex items-center gap-1.5">
            <i class="fa-solid fa-bolt"></i> 激活
          </button>
          <button v-if="detailAppt.status === 'virtual' || detailAppt.status === 'active'"
            @click="cancelFromList(detailAppt.voucherNum); detailAppt = null"
            class="bg-rose-600 text-white text-xs font-bold px-4 py-2 rounded-xl hover:bg-rose-700 transition-all cursor-pointer flex items-center gap-1.5">
            <i class="fa-solid fa-xmark"></i> 取消预约
          </button>
        </div>
      </div>
    </div>

    <!-- Toast 通知 -->
    <div v-if="toastVisible"
      class="fixed top-4 left-1/2 -translate-x-1/2 bg-emerald-500 text-white text-xs font-bold px-4 py-2 rounded-xl shadow-lg z-[60] flex items-center gap-1.5 animate-fade-in">
      <i class="fa-solid fa-circle-check"></i> {{ toastMsg }}
    </div>
  </div>

  <!-- ================= 预约面板视图（复刻 view-appointment） ================= -->
  <div v-else class="p-6 space-y-4">
    <!-- 顶部导航返回控制栏 -->
    <div class="bg-white/45 backdrop-blur-2xl p-4 rounded-2xl border border-slate-200/80 shadow-sm flex items-center justify-between">
      <button @click="backToBranchList"
        class="flex items-center space-x-2 text-xs font-bold text-slate-700 hover:text-blue-600 transition-all cursor-pointer">
        <i class="fa-solid fa-arrow-left text-sm"></i>
        <span>返回网点列表</span>
      </button>
      <div class="flex items-center gap-3">
        <div class="text-xs font-black text-slate-800 flex items-center gap-1.5">
          <i class="fa-solid fa-calendar-check text-blue-600"></i>
          <span>智能预约取号面板</span>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- 左侧：网点实况 + 守时须知 -->
      <div class="lg:col-span-1 space-y-4">
        <!-- 当前选择网点实况舱 -->
        <div class="bg-white/45 backdrop-blur-2xl p-5 rounded-3xl border border-slate-200/80 shadow-sm space-y-4 tech-card">
          <div class="space-y-1">
            <span class="text-[10px] font-bold text-blue-600 uppercase tracking-wider bg-blue-50 px-2 py-0.5 rounded border border-blue-100">目标网点</span>
            <h3 class="text-base font-black text-slate-800 pt-1">{{ bookingBranch?.name }}</h3>
            <p class="text-[11px] font-medium text-slate-500">{{ bookingBranch?.address }}</p>
          </div>

          <div class="w-full h-px bg-slate-200/60"></div>

          <!-- 当前网点实时数据镜像 -->
          <div class="space-y-2.5">
            <span class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">网点实时大盘态势</span>
            <div class="grid grid-cols-2 gap-2 text-center">
              <div class="bg-slate-900/5 backdrop-blur-md p-2.5 rounded-2xl border border-slate-200/50">
                <span class="text-[10px] font-bold text-slate-500 block">当前在店人数</span>
                <span class="text-lg font-black text-slate-800">{{ bookingBranch?.flow }} 人</span>
              </div>
              <div class="bg-slate-900/5 backdrop-blur-md p-2.5 rounded-2xl border border-slate-200/50">
                <span class="text-[10px] font-bold text-slate-500 block">预约排队人数</span>
                <span class="text-lg font-black text-cyan-600">{{ bookingBranch?.reserve }} 人</span>
              </div>
            </div>
            <div class="flex justify-between items-center text-[11px] font-bold text-slate-600 bg-slate-50/60 p-2.5 rounded-xl border border-slate-200/40">
              <span><i class="fa-solid fa-door-open text-blue-500 mr-1"></i> 开放窗口: <strong>{{ bookingBranch?.window }} 个</strong></span>
              <span><i class="fa-solid fa-clock text-amber-500 mr-1"></i> 预计等待: <strong>~{{ bookingBranch?.wait }}分钟</strong></span>
            </div>
          </div>
        </div>

        <!-- 守时履约须知 -->
        <div class="bg-amber-500/10 backdrop-blur-2xl p-5 rounded-3xl border border-amber-500/30 shadow-sm space-y-3 relative overflow-hidden">
          <div class="flex items-center space-x-2 text-amber-800">
            <i class="fa-solid fa-triangle-exclamation text-base animate-bounce"></i>
            <h4 class="text-xs font-black uppercase tracking-wider">预约守时履约须知</h4>
          </div>
          <p class="text-xs text-amber-900/90 leading-relaxed font-semibold">
            根据ICBC · "灵枢"智慧分流规则，预约成功后系统将为您保留优先叫号特权。
          </p>
          <blockquote class="bg-amber-50/80 p-2.5 rounded-xl border border-amber-200/80 text-[11px] font-bold text-rose-600 leading-snug">
            ⚠️ 重点提醒：若您超过预约时段 <span class="underline underline-offset-2 decoration-rose-500 font-black">迟到 3 分钟及以上</span>，系统将自动取消优先特权，该预约号将直接降级转换为现场【普通排队号】，需重新顺延等待！
          </blockquote>
          <p class="text-[10px] font-bold text-amber-700">请您合理规划出行路线，务必准时到店核销。保持良好履约习惯将提升您的工银信用度。</p>
        </div>
      </div>

      <!-- 右侧：时间段选择与确认 -->
      <div class="lg:col-span-2 bg-white/45 backdrop-blur-2xl p-6 rounded-3xl border border-slate-200/80 shadow-sm flex flex-col justify-between space-y-6 tech-card">
        <div class="space-y-4">
          <div>
            <h3 class="text-sm font-black text-slate-800">请选择您计划到店的快捷预约时段</h3>
            <p class="text-xs text-slate-400 font-bold">{{ currentDateLabel }}</p>
          </div>

          <!-- 日期选择器 -->
          <div class="space-y-1.5">
            <label class="text-[10px] font-bold text-slate-500 uppercase tracking-wider block">选择到店日期</label>
            <div class="grid grid-cols-4 gap-2">
              <button v-for="d in dates" :key="d.offset" @click="selectDate(d.offset)"
                :class="[
                  'appt-date-btn p-2 rounded-xl border transition-all text-center cursor-pointer group',
                  selectedDateOffset === d.offset
                    ? 'border-blue-500 bg-blue-50/80 ring-2 ring-blue-500/20'
                    : 'border-slate-200/80 bg-white/60 hover:border-blue-400'
                ]">
                <div :class="['text-[10px] font-black', selectedDateOffset === d.offset ? 'text-blue-600' : 'text-slate-700 group-hover:text-blue-600']">{{ d.label }}</div>
                <div class="text-[9px] font-bold text-slate-400">{{ d.dateStr }}</div>
              </button>
            </div>
          </div>

          <!-- 办理业务类型 -->
          <div class="space-y-1">
            <label class="text-[10px] font-bold text-slate-500 uppercase tracking-wider block">拟办理业务事项</label>
            <select v-model="selectedBizType" @change="onBizTypeChange"
              class="w-full bg-white/80 border border-slate-200 text-xs font-bold rounded-xl p-3 focus:outline-none focus:border-blue-500 transition-all text-slate-800 cursor-pointer">
              <option v-for="opt in bizOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
            </select>
          </div>

          <!-- AI办理时长预测 -->
          <div class="bg-gradient-to-r from-indigo-50 to-purple-50 p-3 rounded-xl border border-indigo-100 flex items-center justify-between">
            <div class="flex items-center gap-2">
              <div class="w-7 h-7 rounded-lg bg-gradient-to-br from-indigo-500 to-purple-500 flex items-center justify-center text-white">
                <i class="fa-solid fa-brain text-xs"></i>
              </div>
              <div>
                <div class="text-[10px] font-black text-indigo-700">AI办理时长预测</div>
                <div class="text-[9px] font-bold text-indigo-400">{{ apptAIPredict.detail || '基于历史数据+网点负载' }}</div>
              </div>
            </div>
            <div class="text-right">
              <div class="text-lg font-black text-purple-600">{{ apptAIPredict.time ? '~' + apptAIPredict.time + ' 分钟' : '--' }}</div>
              <div class="text-[9px] font-bold text-indigo-400">{{ apptAIPredict.confidence ? '置信 ' + apptAIPredict.confidence + '%' : '置信 --%' }}</div>
            </div>
          </div>

          <!-- 时间段网格 -->
          <div class="space-y-2">
            <div class="flex items-center justify-between">
              <label class="text-[10px] font-bold text-slate-500 uppercase tracking-wider block">可预约号源时段 (点击选择)</label>
              <span class="text-[10px] font-bold text-slate-400">{{ slotSummary }}</span>
            </div>
            <div class="grid grid-cols-2 sm:grid-cols-3 gap-3">
              <button v-for="(s, i) in slotData" :key="i" @click="selectSlot(i)" :class="getSlotClass(s, i)">
                <div :class="getSlotTimeClass(s, i)">{{ s.start }} - {{ s.end }}</div>
                <span v-if="s.isPast" class="text-[9px] font-bold text-slate-400 bg-slate-100 px-1.5 py-0.5 rounded">已过</span>
                <span v-else-if="s.capacity === 0" class="text-[9px] font-bold text-rose-500 bg-rose-50 px-1.5 py-0.5 rounded border border-rose-100">已满</span>
                <span v-else-if="i === selectedSlotIdx" class="text-[9px] font-bold text-blue-600 bg-blue-100/80 px-1.5 py-0.5 rounded border border-blue-200">推荐 · 余{{ s.capacity }}</span>
                <span v-else-if="s.capacity <= 3" class="text-[9px] font-bold text-amber-600 bg-amber-50 px-1.5 py-0.5 rounded border border-amber-100">号源紧张 · 余{{ s.capacity }}</span>
                <span v-else class="text-[9px] font-bold text-emerald-600 bg-emerald-50 px-1.5 py-0.5 rounded border border-emerald-100">余号充沛 · 余{{ s.capacity }}</span>
              </button>
            </div>
          </div>

          <!-- 排队预览 -->
          <div v-if="queuePreview" class="bg-gradient-to-r from-blue-50 to-cyan-50 p-3 rounded-2xl border border-cyan-200/60">
            <div class="flex items-center gap-2 mb-1.5">
              <i class="fa-solid fa-wand-magic-sparkles text-cyan-600 text-xs"></i>
              <span class="text-[10px] font-black text-slate-700">灵枢排队预测</span>
            </div>
            <div class="grid grid-cols-2 gap-2 text-center">
              <div>
                <div class="text-[10px] font-bold text-slate-500">预计排队位置</div>
                <div class="text-sm font-black text-blue-600">第 {{ queuePreview.pos }} 位</div>
              </div>
              <div class="border-l border-slate-200/60">
                <div class="text-[10px] font-bold text-slate-500">预计办理开始</div>
                <div class="text-sm font-black text-emerald-600">{{ queuePreview.estStart }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 确认提交栏 -->
        <div class="border-t border-slate-200/60 pt-4 space-y-3">
          <div class="flex items-center justify-between text-xs font-bold text-slate-600">
            <span>当前选择时段: <span class="text-blue-600 font-black">{{ selectedSlotText }}</span></span>
            <span class="text-[10px] text-slate-400">核销凭证自动下发至系统账舱</span>
          </div>
          <button @click="confirmBooking"
            :disabled="selectedSlotIdx < 0"
            class="w-full bg-gradient-to-r from-blue-600 via-cyan-500 to-emerald-500 text-white font-bold py-3 rounded-xl text-sm shadow-md hover:opacity-95 cursor-pointer transition-all flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed">
            <i class="fa-solid fa-circle-check"></i>
            <span>确认提交预约并锁定优先号源</span>
          </button>
        </div>
      </div>
    </div>
  </div>

  <!-- ================= 预约成功凭证弹窗 ================= -->
  <div v-if="successModal" class="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4" @click.self="closeSuccessModal">
    <div class="bg-white rounded-3xl w-full max-w-sm shadow-2xl border border-slate-200/80 overflow-hidden flex flex-col">
      <div class="bg-gradient-to-r from-emerald-500 via-cyan-500 to-blue-600 p-5 shrink-0 text-center">
        <div class="w-14 h-14 rounded-full bg-white/20 flex items-center justify-center text-white mx-auto mb-2">
          <i class="fa-solid fa-circle-check text-3xl"></i>
        </div>
        <h3 class="text-sm font-black text-white">预约成功 · 虚拟号已生成</h3>
        <p class="text-[10px] text-white/80 font-bold mt-0.5">请到店扫码激活后享有优先叫号</p>
      </div>
      <div class="p-5 space-y-3 overflow-y-auto flex-1">
        <!-- 虚拟号 + 二维码 -->
        <div class="flex items-center gap-3 bg-slate-50 rounded-2xl p-3 border border-slate-100">
          <div class="shrink-0">
            <div class="text-[9px] font-bold text-slate-400 uppercase tracking-wider text-center mb-1">激活码</div>
            <div class="w-28 h-28 bg-white rounded-lg border-2 border-slate-300 p-2 relative">
              <div class="grid grid-cols-3 gap-1 w-full h-full">
                <div class="border-2 border-slate-700 aspect-square rounded-sm"></div>
                <div class="flex flex-col justify-between p-0.5"><span class="block bg-slate-700 h-0.5 w-full"></span><span class="block bg-slate-700 h-0.5 w-1/2"></span></div>
                <div class="border-2 border-slate-700 aspect-square rounded-sm ml-auto"></div>
                <div class="col-span-2 space-y-0.5"><span class="block bg-slate-700 h-0.5 w-full"></span><span class="block bg-slate-700 h-0.5 w-2/3"></span></div>
                <div class="bg-slate-700 aspect-square m-auto"></div>
                <div class="border-2 border-slate-700 aspect-square rounded-sm mt-auto"></div>
                <div class="col-span-2 flex flex-col justify-end gap-0.5 mt-auto"><span class="block bg-slate-700 h-0.5 w-full"></span><span class="block bg-slate-700 h-0.5 w-1/2"></span></div>
              </div>
              <div class="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 bg-slate-500 text-white text-[7px] font-black px-1 py-0.5 rounded border border-white scale-90">激活</div>
            </div>
          </div>
          <div class="flex-1 text-center">
            <div class="text-[9px] font-bold text-slate-400 uppercase tracking-wider">虚拟排队号</div>
            <div :class="['text-xl font-black tracking-wider', voucherActivated ? 'text-rose-600' : 'text-slate-400']">{{ voucherNum }}</div>
            <div class="text-[9px] font-bold text-slate-400 mt-0.5">{{ voucherActivated ? '已激活 · 享有优先叫号' : '未激活 · 银行暂不叫号' }}</div>
          </div>
        </div>

        <div class="space-y-2 text-[11px] font-bold text-slate-600">
          <div class="flex justify-between"><span class="text-slate-400">目标网点</span><span class="text-slate-800">{{ bookingBranch?.name }}</span></div>
          <div class="flex justify-between"><span class="text-slate-400">到店日期</span><span class="text-slate-800">{{ successDateText }}</span></div>
          <div class="flex justify-between"><span class="text-slate-400">预约时段</span><span class="text-blue-600">{{ selectedSlotText }}</span></div>
          <div class="flex justify-between"><span class="text-slate-400">排队位置</span><span class="text-slate-800">{{ queuePreview?.pos || '-' }}</span></div>
        </div>

        <!-- 激活说明 -->
        <div class="bg-blue-50 rounded-xl p-3 border border-blue-200/60 text-[10px] font-bold text-blue-700 leading-relaxed space-y-1">
          <div class="flex items-start gap-1.5">
            <i class="fa-solid fa-circle-info text-blue-500 mt-0.5"></i>
            <span><b>激活流程：</b>到店后在排队机/大堂PAD扫描左侧二维码 → 虚拟号变为红色预约号 → 享有优先叫号</span>
          </div>
          <div class="flex items-start gap-1.5">
            <i class="fa-solid fa-triangle-exclamation text-amber-500 mt-0.5"></i>
            <span class="text-amber-700">未在预约时段结束前到店激活，号码自动失效，扣除信誉分10分</span>
          </div>
        </div>

        <!-- 模拟到店激活按钮 -->
        <button v-if="!voucherActivated" @click="activateAppointment"
          class="w-full bg-gradient-to-r from-rose-500 to-orange-500 hover:from-rose-600 hover:to-orange-600 text-white text-xs font-bold py-2.5 rounded-xl shadow-md transition-all cursor-pointer flex items-center justify-center gap-1.5">
          <i class="fa-solid fa-qrcode"></i> 模拟到店扫码激活
        </button>
        <div v-else class="w-full bg-emerald-50 border border-emerald-200 text-emerald-700 text-xs font-bold py-2.5 rounded-xl flex items-center justify-center gap-1.5">
          <i class="fa-solid fa-circle-check"></i> 已激活 · 优先叫号已生效
        </div>
      </div>
      <div class="border-t border-slate-200/60 p-3 shrink-0 bg-white">
        <button @click="closeSuccessModal"
          class="w-full px-4 py-2.5 text-xs font-bold text-white bg-gradient-to-r from-blue-600 to-cyan-500 hover:from-blue-700 hover:to-cyan-600 rounded-xl transition-all cursor-pointer flex items-center justify-center gap-1.5">
          <i class="fa-solid fa-check"></i> 我知道了，返回网点看板
        </button>
      </div>
    </div>
  </div>
  </div>
</template>
