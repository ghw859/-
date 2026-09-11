<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useAppointmentStore } from '../stores/appointment'

const appt = useAppointmentStore()

// 真实数据：已办结预约数（后端 /api/appointments/my）
const completedCount = computed(() => appt.appointments.filter(a => a.status === 'completed').length)

// 4 个核心指标卡片（完全复刻原文案+图标+颜色）
const stats = computed(() => [
  {
    label: '工银账户总资产',
    value: '4,825,638',
    prefix: '￥',
    suffix: '',
    icon: 'fa-solid fa-wallet',
    iconBg: 'bg-blue-500/10',
    iconColor: 'text-blue-600',
    tipIcon: 'fa-solid fa-arrow-trend-up',
    tipColor: 'text-emerald-600',
    tipText: '较上月环比攀升 4.2%',
  },
  {
    label: '本月网点预约办结',
    value: String(completedCount.value),
    prefix: '',
    suffix: '单',
    icon: 'fa-solid fa-calendar-check',
    iconBg: 'bg-cyan-500/10',
    iconColor: 'text-cyan-600',
    tipIcon: 'fa-solid fa-clock text-cyan-500',
    tipColor: 'text-slate-500',
    tipText: '自主选择空闲网点，平均节省 22 分钟',
  },
  {
    label: 'AI 预填单自动解析',
    value: '9',
    prefix: '',
    suffix: '份',
    icon: 'fa-solid fa-wand-magic-sparkles',
    iconBg: 'bg-indigo-500/10',
    iconColor: 'text-indigo-600',
    tipIcon: 'fa-solid fa-file-lines text-indigo-500',
    tipColor: 'text-slate-500',
    tipText: '线上上传材料自动填表，柜台免手录',
  },
  {
    label: '预约信誉分',
    value: String(appt.creditScore),
    prefix: '',
    suffix: '/ 100',
    icon: 'fa-solid fa-shield-halved',
    iconBg: 'bg-emerald-500/10',
    iconColor: 'text-emerald-600',
    valueColor: 'text-emerald-600',
    tipIcon: 'fa-solid fa-circle-check',
    tipColor: 'text-emerald-600',
    tipText: '信誉优质 · 按时到店激活享优先叫号',
  },
])

// AI 客流预测对比热力图（完全复刻的网点名称+状态）
const branches: { name: string; status: 'busy' | 'moderate' | 'free' }[] = [
  { name: '北京分行营业部', status: 'busy' },
  { name: '长安街智慧示范支行', status: 'moderate' },
  { name: '金融街私人银行旗舰支行', status: 'free' },
  { name: '中关村科技创新特色支行', status: 'moderate' },
  { name: '望京SOHO社区支行', status: 'free' },
  { name: '国贸CBD中心支行', status: 'busy' },
]
const hours = ['9:00', '10:00', '11:00', '12:00', '13:00', '14:00', '15:00', '16:00']

interface HeatCell { val: number; bg: string; textClass: string; }
interface HeatRow { branch: string; shortName: string; cells: HeatCell[] }

const heatmapData = ref<HeatRow[]>([])
const heatmapTip = ref('')

function valToClass(val: number): { bg: string; textClass: string } {
  if (val < 35) return { bg: 'bg-emerald-400', textClass: 'text-white' }
  if (val < 60) return { bg: 'bg-amber-400', textClass: 'text-white' }
  return { bg: 'bg-rose-400', textClass: 'text-white' }
}

// 完全复刻的 renderOverviewHeatmap 算法
function buildHeatmap() {
  const basePatterns: Record<string, number[]> = {
    busy: [85, 95, 80, 45, 55, 70, 60, 40],
    moderate: [55, 65, 50, 25, 35, 45, 40, 30],
    free: [30, 35, 28, 15, 20, 25, 22, 18],
  }

  let globalMin = { val: 999, branch: '', hour: '' }
  const rows: HeatRow[] = []

  for (let i = 0; i < branches.length; i++) {
    const b = branches[i]!
    const basePattern = basePatterns[b.status]!
    let seed = 0
    for (let c = 0; c < b.name.length; c++) seed += b.name.charCodeAt(c)

    const shortName = b.name.length > 8 ? b.name.substring(0, 7) + '…' : b.name
    const cells: HeatCell[] = []

    for (let k = 0; k < basePattern.length; k++) {
      const delta = ((seed + k * 7) % 11) - 5
      const val = Math.max(10, Math.min(100, basePattern[k]! + delta))
      const cls = valToClass(val)
      // 追踪全局最小值
      if (val < globalMin.val) { globalMin.val = val; globalMin.branch = b.name; globalMin.hour = hours[k]! }
      cells.push({ val, bg: cls.bg, textClass: cls.textClass })
    }

    rows.push({ branch: b.name, shortName, cells })
  }

  heatmapData.value = rows

  // AI推荐（完全复刻的拼接逻辑）
  const shortBranch = globalMin.branch.length > 10 ? globalMin.branch.substring(0, 9) + '…' : globalMin.branch
  const bestEnd = (parseInt(globalMin.hour, 10) + 1) + ':00'
  heatmapTip.value = 'AI推荐：' + shortBranch + ' ' + globalMin.hour + '-' + bestEnd + ' 全网客流最低（仅' + globalMin.val + '%负载），建议此时段到店办理'
}

// 折线图切换（复刻 toggleLineChart）
type ChartRange = 'half' | 'full'
const chartRange = ref<ChartRange>('full')

const linePathClip = computed(() =>
  chartRange.value === 'half'
    ? 'polygon(0% 70%, 20% 50%, 40% 75%, 60% 30%, 80% 45%, 100% 10%, 100% 100%, 0% 100%)'
    : 'polygon(0% 85%, 10% 60%, 20% 70%, 30% 40%, 40% 55%, 50% 25%, 60% 50%, 70% 35%, 80% 40%, 90% 15%, 100% 5%, 100% 100%, 0% 100%)'
)
const axisMax = computed(() => (chartRange.value === 'half' ? '500k' : '1,000k'))
const xLabels = computed(() =>
  chartRange.value === 'half'
    ? [
        { text: '02月', peak: false },
        { text: '03月', peak: false },
        { text: '04月', peak: false },
        { text: '05月', peak: false },
        { text: '06月', peak: false },
        { text: '￥482.5K', sub: '07月', peak: true },
      ]
    : [
        { text: '上半年简况', peak: false },
        { text: '07月', peak: false },
        { text: '08月', peak: false },
        { text: '09月', peak: false },
        { text: '10月', peak: false },
        { text: '11月', peak: false },
        { text: '￥945.0K', sub: '12月', peak: true },
      ]
)

function toggleLineChart(range: ChartRange) {
  chartRange.value = range
}

// AI 实时诊断打字机效果（复刻 DOMContentLoaded 逻辑）
const aiDiagnosis = ref('数据初始化中，AI 核心引擎正在解算您的全维账单资产态势...')
const fullDiagnosisText =
  '经 ICBC-AI 业务调度与风控双模型扫描，本月您通过平台自主选择网点预约办理柜面业务 14 笔，依托线上资料预填 + 网点智能分流，单笔业务平均办理时长压缩 41%；系统同时识别 2 笔夜间陌生大额转账并弹窗拦截，账户综合安全系数 98 分。闲置活期资金可配置工行低风险定期产品稳健增值。'
let typeTimer: ReturnType<typeof setInterval> | null = null

function startTyping() {
  let idx = 0
  aiDiagnosis.value = ''
  typeTimer = setInterval(() => {
    if (idx < fullDiagnosisText.length) {
      aiDiagnosis.value += fullDiagnosisText.charAt(idx)
      idx++
    } else if (typeTimer) {
      clearInterval(typeTimer)
      typeTimer = null
    }
  }, 20)
}

onMounted(() => {
  buildHeatmap()
  startTyping()
})

onUnmounted(() => {
  if (typeTimer) clearInterval(typeTimer)
})

// 最近三笔交易记录（静态数据）
const recentTransactions = [
  {
    icon: 'fa-solid fa-bolt-lightning',
    iconBg: 'bg-cyan-50',
    iconColor: 'text-cyan-600',
    title: '国家电网智能代扣缴费 (2026年度)',
    sn: 'ICBC992019481 · 18:32:10',
    amount: '- ￥324.50',
    amountColor: 'text-rose-500',
    tag: '生活缴费',
    tagBg: 'bg-slate-100',
    tagColor: 'text-slate-500',
    tagBorder: '',
  },
  {
    icon: 'fa-solid fa-arrow-trend-up',
    iconBg: 'bg-emerald-50',
    iconColor: 'text-emerald-600',
    title: '工银瑞信核心价值股票基金 - 红利再投',
    sn: 'ICBC882049104 · 15:11:02',
    amount: '+ ￥1,250.00',
    amountColor: 'text-emerald-600',
    tag: '基金理财',
    tagBg: 'bg-emerald-50',
    tagColor: 'text-emerald-600',
    tagBorder: 'border-emerald-100',
  },
  {
    icon: 'fa-solid fa-plane',
    iconBg: 'bg-blue-50',
    iconColor: 'text-blue-600',
    title: '工银环球旅行卡境外亚马逊消费 (B2C)',
    sn: 'ICBC771239556 · 10:45:19',
    amount: '- ￥2,410.88',
    amountColor: 'text-rose-500',
    tag: '跨境消费',
    tagBg: 'bg-blue-50',
    tagColor: 'text-blue-600',
    tagBorder: 'border-blue-100',
  },
]
</script>

<template>
  <div class="p-6 space-y-6">
    <!-- 顶部核心资产态势数据舱 -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      <div v-for="stat in stats" :key="stat.label"
        class="bg-white/45 backdrop-blur-2xl p-5 rounded-2xl border border-slate-200/80 shadow-sm flex flex-col justify-between tech-card">
        <div class="flex justify-between items-start">
          <div class="space-y-1">
            <span class="text-[10px] font-bold text-slate-500 uppercase tracking-wider">{{ stat.label }}</span>
            <div class="flex items-baseline space-x-1">
              <span v-if="stat.prefix" class="text-xs font-black text-slate-900">{{ stat.prefix }}</span>
              <span :class="['text-xl font-black tracking-tight', stat.valueColor || 'text-slate-900']">{{ stat.value }}</span>
              <span v-if="stat.suffix" class="text-xs font-black text-slate-900">{{ stat.suffix }}</span>
            </div>
          </div>
          <div :class="['w-8 h-8 rounded-xl flex items-center justify-center', stat.iconBg, stat.iconColor]">
            <i :class="[stat.icon, 'text-xs']"></i>
          </div>
        </div>
        <div :class="['text-[10px] font-bold flex items-center gap-1 mt-3', stat.tipColor]">
          <i :class="stat.tipIcon"></i>
          <span>{{ stat.tipText }}</span>
        </div>
      </div>
    </div>

    <!-- AI 诊断卡片（放在热力图之上） -->
    <div class="bg-gradient-to-r from-blue-600/5 via-cyan-500/5 to-emerald-500/5 border border-cyan-500/20 rounded-2xl p-4 flex items-start space-x-4 relative overflow-hidden">
      <div class="absolute top-0 right-0 p-1 bg-cyan-500/10 text-cyan-600 font-mono text-[8px] font-bold rounded-bl-lg tracking-wider">
        AI DIAGNOSIS
      </div>
      <div class="w-9 h-9 rounded-xl bg-cyan-500/10 flex items-center justify-center text-cyan-600 shrink-0 mt-0.5">
        <i class="fa-solid fa-wand-magic-sparkles animate-pulse"></i>
      </div>
      <div class="space-y-1">
        <h4 class="text-xs font-black text-cyan-900">灵枢财富健康度诊断（AI引擎实时分析生成）</h4>
        <p class="text-xs text-slate-600 leading-relaxed font-semibold">{{ aiDiagnosis }}</p>
      </div>
    </div>

    <!-- AI客流预测对比热力图 -->
    <div class="bg-white/50 backdrop-blur-2xl p-5 rounded-3xl border border-slate-200/60 shadow-sm">
      <div class="flex items-center justify-between mb-4">
        <div class="flex items-center gap-2.5">
          <div class="w-9 h-9 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-500 flex items-center justify-center text-white">
            <i class="fa-solid fa-brain text-base"></i>
          </div>
          <div>
            <h3 class="text-sm font-black text-slate-800">AI客流预测对比热力图</h3>
            <p class="text-[10px] font-bold text-slate-400">6网点 × 8时段 · 基于历史30天数据预测</p>
          </div>
        </div>
        <div class="flex items-center gap-2 text-[9px] font-bold text-slate-400">
          <span class="flex items-center gap-0.5"><span class="w-2.5 h-2.5 rounded bg-emerald-400"></span>畅通</span>
          <span class="flex items-center gap-0.5"><span class="w-2.5 h-2.5 rounded bg-amber-400"></span>适中</span>
          <span class="flex items-center gap-0.5"><span class="w-2.5 h-2.5 rounded bg-rose-400"></span>繁忙</span>
        </div>
      </div>
      <div class="overflow-x-auto">
        <table class="w-full text-center border-collapse min-w-[600px]">
          <thead>
            <tr>
              <th class="text-[10px] font-black text-slate-500 text-left pl-2 pb-2">网点</th>
              <th v-for="h in hours" :key="h" class="text-[9px] font-bold text-slate-400 pb-2 px-1">{{ h }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in heatmapData" :key="row.branch">
              <td class="text-[10px] font-bold text-slate-700 text-left pl-2 py-1 whitespace-nowrap">{{ row.shortName }}</td>
              <td v-for="(cell, i) in row.cells" :key="i" class="py-0.5 px-0.5">
                <div :class="[cell.bg, cell.textClass, 'rounded-md py-1.5 text-[9px] font-black']">{{ cell.val }}</div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="mt-3 bg-emerald-50 rounded-xl p-3 border border-emerald-200 flex items-center gap-2">
        <i class="fa-solid fa-lightbulb text-emerald-500"></i>
        <span class="text-xs font-bold text-emerald-700">{{ heatmapTip }}</span>
      </div>
    </div>

    <!-- 中上区：饼图 + 折线图 -->
    <div class="grid grid-cols-1 lg:grid-cols-5 gap-6">
      <!-- 1. 消费分类饼图卡片 -->
      <div class="lg:col-span-2 bg-white/45 backdrop-blur-2xl p-6 rounded-3xl border border-slate-200/80 shadow-sm flex flex-col justify-between min-h-[300px] tech-card">
        <div class="flex items-center justify-between">
          <span class="text-xs font-bold text-slate-700 uppercase tracking-wider flex items-center gap-1.5">
            <i class="fa-solid fa-pie-chart text-cyan-500"></i> 消费分类占比 (AI实时归类)
          </span>
          <span class="text-[10px] font-bold text-cyan-700 bg-cyan-100/70 px-2 py-0.5 rounded-full border border-cyan-300">本月数据</span>
        </div>

        <div class="flex items-center justify-around my-4">
          <div class="relative w-32 h-32 rounded-full p-[10px] flex items-center justify-center shadow-md hover-scale duration-300 bg-white"
            style="background: conic-gradient(#06b6d4 0% 45%, #3b82f6 45% 75%, #10b981 75% 90%, #cbd5e1 90% 100%);">
            <div class="w-full h-full bg-white/90 backdrop-blur-md rounded-full flex flex-col items-center justify-center">
              <span class="text-base font-black text-slate-900">￥14.2K</span>
              <span class="text-[9px] text-slate-500 font-extrabold">总支出</span>
            </div>
          </div>

          <div class="space-y-2 text-xs font-bold text-slate-600">
            <div class="flex items-center space-x-2"><span class="w-2.5 h-2.5 rounded-full bg-cyan-500"></span><span>生活缴费 45%</span></div>
            <div class="flex items-center space-x-2"><span class="w-2.5 h-2.5 rounded-full bg-blue-500"></span><span>基金理财 30%</span></div>
            <div class="flex items-center space-x-2"><span class="w-2.5 h-2.5 rounded-full bg-emerald-500"></span><span>跨境消费 15%</span></div>
            <div class="flex items-center space-x-2"><span class="w-2.5 h-2.5 rounded-full bg-slate-300"></span><span>其他支出 10%</span></div>
          </div>
        </div>
      </div>

      <!-- 2. 月度趋势折线图卡片 -->
      <div class="lg:col-span-3 bg-white/45 backdrop-blur-2xl p-6 rounded-3xl border border-slate-200/80 shadow-sm flex flex-col justify-between min-h-[300px] tech-card">
        <div class="flex items-center justify-between">
          <span class="text-xs font-bold text-slate-700 uppercase tracking-wider flex items-center gap-1.5">
            <i class="fa-solid fa-chart-line text-blue-500"></i> 资产总值与净值月度演进趋势
          </span>
          <div class="flex space-x-1 bg-slate-200/80 p-0.5 rounded-lg text-[10px] font-bold text-slate-600">
            <button @click="toggleLineChart('half')"
              :class="chartRange === 'half' ? 'bg-white text-slate-900 px-2 py-0.5 rounded-md shadow-xs cursor-pointer transition-all' : 'px-2 py-0.5 cursor-pointer transition-all hover:text-slate-900'">
              近半年
            </button>
            <button @click="toggleLineChart('full')"
              :class="chartRange === 'full' ? 'bg-white text-slate-900 px-2 py-0.5 rounded-md shadow-xs cursor-pointer transition-all' : 'px-2 py-0.5 cursor-pointer transition-all hover:text-slate-900'">
              全年
            </button>
          </div>
        </div>

        <div class="h-44 w-full relative overflow-hidden mt-3">
          <div class="h-36 w-[calc(100%-12px)] flex items-end justify-between absolute bottom-6 left-10 border-l-2 border-b-2 border-slate-400">
            <div class="absolute -left-8 inset-y-0 flex flex-col justify-between text-[10px] font-mono font-bold text-slate-600 py-1 pointer-events-none">
              <div>{{ axisMax }}</div>
              <div>500k</div>
              <div>0</div>
            </div>

            <div class="absolute inset-0 flex flex-col justify-between pointer-events-none opacity-40">
              <div class="border-b border-dashed border-slate-400 w-full pt-1"></div>
              <div class="border-b border-dashed border-slate-400 w-full pt-16"></div>
            </div>

            <div class="w-full h-28 absolute bottom-0 left-0 overflow-hidden opacity-80 hover-scale origin-bottom transition-transform duration-300">
              <div class="w-full h-full chart-line-gradient" :style="{ clipPath: linePathClip }"></div>
            </div>

            <div class="absolute -bottom-6 inset-x-0 flex justify-between text-[10px] font-mono font-bold text-slate-600 px-1">
              <template v-for="(lbl, i) in xLabels" :key="i">
                <span v-if="!lbl.peak">{{ lbl.text }}</span>
                <span v-else class="text-cyan-700 font-black flex flex-col items-center -mt-3">
                  <span>{{ lbl.text }}</span>
                  <span class="text-[9px] font-bold text-slate-600">{{ lbl.sub }}</span>
                </span>
              </template>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 中下区第二图表网格：雷达图 + 堆叠柱状图 -->
    <div class="grid grid-cols-1 lg:grid-cols-5 gap-6">
      <!-- 3. AI 智能风控多模态雷达图 -->
      <div class="lg:col-span-2 bg-white/45 backdrop-blur-2xl p-6 rounded-3xl border border-slate-200/80 shadow-sm flex flex-col justify-between min-h-[350px] tech-card">
        <div class="flex items-center justify-between">
          <span class="text-xs font-bold text-slate-700 uppercase tracking-wider flex items-center gap-1.5">
            <i class="fa-solid fa-shield-halved text-emerald-500"></i> AI多维账户风控与资产健康度
          </span>
          <span class="text-xs font-mono text-emerald-600 bg-emerald-50 border border-emerald-200 px-2.5 py-0.5 rounded font-bold">安全系数: 98</span>
        </div>

        <div class="flex items-center justify-center my-6 relative h-48 hover-scale duration-300 overflow-hidden">
          <div class="absolute w-44 h-44 rounded-full border border-dashed border-slate-400/80 flex items-center justify-center bg-slate-950/5">
            <div class="w-28 h-28 rounded-full border border-dashed border-slate-300 flex items-center justify-center">
              <div class="w-14 h-14 rounded-full border border-dotted border-slate-300"></div>
            </div>
          </div>
          <div class="absolute w-48 h-[1.5px] bg-slate-400"></div>
          <div class="absolute h-48 w-[1.5px] bg-slate-400"></div>
          <svg class="w-48 h-48 drop-shadow-[0_0_12px_rgba(16,185,129,0.5)] relative z-10" viewBox="0 0 100 100">
            <polygon points="50,15 85,30 75,75 50,85 25,75 15,30" fill="rgba(16, 185, 129, 0.28)" stroke="#10b981" stroke-width="2" />
            <circle cx="50" cy="15" r="3" fill="#06b6d4" />
            <circle cx="85" cy="30" r="3" fill="#3b82f6" />
            <circle cx="75" cy="75" r="3" fill="#10b981" />
            <circle cx="50" cy="85" r="3" fill="#6366f1" />
            <circle cx="25" cy="75" r="3" fill="#f59e0b" />
            <circle cx="15" cy="30" r="3" fill="#8b5cf6" />
          </svg>
          <span class="absolute top-0 text-[10px] font-black text-slate-700 bg-white/80 px-1.5 py-0.5 rounded shadow-xs z-20">账户交易安全</span>
          <span class="absolute right-0 top-1/4 text-[10px] font-black text-slate-700 bg-white/80 px-1.5 py-0.5 rounded shadow-xs z-20">电信反诈健康度</span>
          <span class="absolute right-0 bottom-1/4 text-[10px] font-black text-slate-700 bg-white/80 px-1.5 py-0.5 rounded shadow-xs z-20">资金流动性</span>
          <span class="absolute bottom-0 text-[10px] font-black text-slate-700 bg-white/80 px-1.5 py-0.5 rounded shadow-xs z-20">跨境交易合规</span>
          <span class="absolute left-0 bottom-1/4 text-[10px] font-black text-slate-700 bg-white/80 px-1.5 py-0.5 rounded shadow-xs z-20">反洗钱风险等级</span>
          <span class="absolute left-0 top-1/4 text-[10px] font-black text-slate-700 bg-white/80 px-1.5 py-0.5 rounded shadow-xs z-20">预约业务授权合规度</span>
        </div>
      </div>

      <!-- 4. 智能组合投资产出堆叠柱状图 -->
      <div class="lg:col-span-3 bg-white/45 backdrop-blur-2xl p-6 rounded-3xl border border-slate-200/80 shadow-sm flex flex-col justify-between min-h-[350px] tech-card">
        <div class="flex items-center justify-between">
          <span class="text-xs font-bold text-slate-700 uppercase tracking-wider flex items-center gap-1.5">
            <i class="fa-solid fa-chart-column text-indigo-500"></i> 近四季度 自选网点预约业务分类办结量
          </span>
          <span class="text-[10px] font-bold text-slate-500">单位: 笔</span>
        </div>

        <div class="h-44 w-full flex items-end justify-between relative px-2 pt-6 pl-10 border-l-2 border-b-2 border-slate-400 mt-4">
          <div class="absolute left-2 inset-y-0 flex flex-col justify-between text-[10px] font-mono font-bold text-slate-600 py-3 pointer-events-none">
            <div>150</div>
            <div>75</div>
            <div>0</div>
          </div>
          <div class="w-full flex justify-around items-end h-36 pb-1 hover-scale origin-bottom duration-300">
            <div class="w-8 flex flex-col justify-end h-full space-y-0.5">
              <div class="bg-indigo-500 h-[20%] rounded-t-sm" title="对公材料、贷款面签预约"></div>
              <div class="bg-blue-500 h-[40%]" title="理财签约、风险测评"></div>
              <div class="bg-cyan-500 h-[25%]" title="个人开户 / 挂失业务"></div>
            </div>
            <div class="w-8 flex flex-col justify-end h-full space-y-0.5">
              <div class="bg-indigo-500 h-[30%] rounded-t-sm"></div>
              <div class="bg-blue-500 h-[25%]"></div>
              <div class="bg-cyan-500 h-[35%]"></div>
            </div>
            <div class="w-8 flex flex-col justify-end h-full space-y-0.5">
              <div class="bg-indigo-500 h-[25%] rounded-t-sm"></div>
              <div class="bg-blue-500 h-[45%]"></div>
              <div class="bg-cyan-500 h-[20%]"></div>
            </div>
            <div class="w-8 flex flex-col justify-end h-full space-y-0.5">
              <div class="bg-indigo-500 h-[40%] rounded-t-sm"></div>
              <div class="bg-blue-500 h-[35%]"></div>
              <div class="bg-cyan-500 h-[15%]"></div>
            </div>
          </div>
          <div class="absolute -bottom-6 inset-x-0 left-10 flex justify-around text-[10px] font-mono font-bold text-slate-600">
            <span>一季度</span><span>二季度</span><span>三季度</span><span>四季度</span>
          </div>
        </div>
        <div class="flex justify-center space-x-4 text-[10px] font-bold text-slate-500 mt-4">
          <div class="flex items-center gap-1"><span class="w-2 h-2 bg-cyan-500 rounded-xs"></span>个人开户 / 挂失业务</div>
          <div class="flex items-center gap-1"><span class="w-2 h-2 bg-blue-500 rounded-xs"></span>理财签约、风险测评</div>
          <div class="flex items-center gap-1"><span class="w-2 h-2 bg-indigo-500 rounded-xs"></span>对公材料、贷款面签预约</div>
        </div>
      </div>
    </div>

    <!-- 底半区：最近三笔交易记录 -->
    <div class="bg-white/50 backdrop-blur-2xl p-6 rounded-3xl border border-slate-200/60 shadow-sm space-y-4 tech-card">
      <div class="flex items-center justify-between">
        <span class="text-xs font-bold text-slate-700 uppercase tracking-wider flex items-center gap-1.5">
          <i class="fa-solid fa-list-check text-emerald-500"></i> 实时核心多模态流水（最近三笔交易）
        </span>
        <a href="#" class="text-xs text-cyan-600 hover:text-cyan-700 font-bold flex items-center space-x-1">
          <span>穿透历史账单</span> <i class="fa-solid fa-angle-right text-[10px]"></i>
        </a>
      </div>

      <div class="space-y-2.5">
        <div v-for="tx in recentTransactions" :key="tx.sn"
          class="flex items-center justify-between p-3.5 bg-white/70 rounded-xl border border-slate-100 hover:border-cyan-500/20 transition-all">
          <div class="flex items-center space-x-3.5">
            <div :class="['w-9 h-9 rounded-lg flex items-center justify-center', tx.iconBg, tx.iconColor]">
              <i :class="[tx.icon, 'text-sm']"></i>
            </div>
            <div>
              <h5 class="text-xs font-bold text-slate-800">{{ tx.title }}</h5>
              <p class="text-[10px] font-mono font-bold text-slate-500 mt-0.5">流水号: {{ tx.sn }}</p>
            </div>
          </div>
          <div class="text-right">
            <span :class="['text-sm font-black', tx.amountColor]">{{ tx.amount }}</span>
            <p :class="['text-[9px] font-bold px-1.5 py-0.5 rounded mt-0.5 inline-block', tx.tagBg, tx.tagColor, tx.tagBorder ? 'border ' + tx.tagBorder : '']">
              {{ tx.tag }}
            </p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
