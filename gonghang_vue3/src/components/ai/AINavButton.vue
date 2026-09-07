<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useBranchStore } from '../../stores/branch'

const router = useRouter()
const branchStore = useBranchStore()
const showModal = ref(false)
const question = ref('')

interface NavStep {
  title: string
  desc: string
  time: string
  materials: string[]
}
interface BizNavItem {
  keywords: string[]
  title: string
  icon: string
  color: string
  understanding: string
  steps: NavStep[]
  recommendService: string
}

const AI_BIZ_NAV_DB: Record<string, BizNavItem> = {
  forex: {
    keywords: ['国外', '跨境', '外汇', '外币', '美元', '日元', '欧元', '港币', '汇出', '海外', '留学'],
    title: '跨境汇款', icon: 'fa-globe', color: 'from-blue-500 to-cyan-500',
    understanding: '您需要向境外汇款，涉及购汇和跨境汇款两个步骤',
    steps: [
      { title: '购汇', desc: '将人民币兑换为目标外币', time: '~10分钟', materials: ['身份证', '银行卡'] },
      { title: '跨境汇款', desc: '填写收款人信息并汇出', time: '~20分钟', materials: ['身份证', '收款人姓名及地址', '收款银行SWIFT代码', '汇款用途说明'] }
    ],
    recommendService: '外汇'
  },
  card: {
    keywords: ['开户', '办卡', '新卡', '开账户', '新开', '申请卡', '银行卡', '办新'],
    title: '个人开户办卡', icon: 'fa-credit-card', color: 'from-emerald-500 to-teal-500',
    understanding: '您需要办理新的银行卡开户',
    steps: [
      { title: '填写开户申请', desc: '提交个人信息并选择账户类型', time: '~5分钟', materials: ['身份证', '手机号'] },
      { title: '柜面核验开卡', desc: '柜员核验身份并制卡', time: '~10分钟', materials: ['身份证', '预留手机验证'] }
    ],
    recommendService: '自助发卡'
  },
  wealth: {
    keywords: ['理财', '基金', '投资', '存款产品', '私行', '财富管理'],
    title: '理财咨询', icon: 'fa-chart-pie', color: 'from-amber-500 to-orange-500',
    understanding: '您需要咨询理财产品或私人银行服务',
    steps: [
      { title: '风险评估', desc: '完成风险承受能力评估问卷', time: '~5分钟', materials: ['身份证', '银行卡'] },
      { title: '理财咨询', desc: '理财经理为您推荐合适产品', time: '~15分钟', materials: ['身份证', '资金来源说明'] }
    ],
    recommendService: 'VIP'
  },
  cash: {
    keywords: ['大额', '现金', '取现', '提现', '取钱', '大量'],
    title: '大额现金提取', icon: 'fa-money-bill-wave', color: 'from-rose-500 to-pink-500',
    understanding: '您需要提取大额现金，须提前预约',
    steps: [
      { title: '预约提现', desc: '线上预约提取金额和时间', time: '~2分钟', materials: ['身份证', '银行卡'] },
      { title: '柜面取款', desc: '到网点柜面核验并取款', time: '~15分钟', materials: ['身份证', '银行卡'] }
    ],
    recommendService: '大额现金'
  },
  loss: {
    keywords: ['挂失', '丢了', '丢失', '补卡', '换卡', '坏卡', '找不到卡', '卡丢', '遗失'],
    title: '卡片挂失补办', icon: 'fa-ban', color: 'from-slate-500 to-slate-600',
    understanding: '您的银行卡遗失，需挂失并补办新卡',
    steps: [
      { title: '紧急挂失', desc: '冻结遗失卡片防止资金风险', time: '~3分钟', materials: ['身份证', '手机号'] },
      { title: '补办新卡', desc: '柜面办理新卡并转移账户', time: '~10分钟', materials: ['身份证', '挂失手续费'] }
    ],
    recommendService: '自助发卡'
  },
  password: {
    keywords: ['密码', '忘记', '重置', '忘了', '改密码', '忘记密码'],
    title: '密码重置', icon: 'fa-key', color: 'from-indigo-500 to-purple-500',
    understanding: '您需要重置银行卡密码',
    steps: [
      { title: '身份核验', desc: '柜员核验您的身份信息', time: '~3分钟', materials: ['身份证', '银行卡'] },
      { title: '密码重置', desc: '设置新的银行卡密码', time: '~5分钟', materials: ['身份证', '手机验证码'] }
    ],
    recommendService: 'VTM'
  }
}

const AI_BIZ_BASE_TIME: Record<string, number> = {
  '大额现金': 18, '外汇': 20, 'VIP': 8, '对公': 25,
  '自助发卡': 5, 'VTM': 3, '无障碍': 12, '理财咨询': 15,
  '开户': 15, '转账': 5, '取现': 10, '销户': 12
}

function predictServiceTime(status: 'free' | 'moderate' | 'busy', services: string[], branchName: string): { time: number; confidence: number } {
  const primaryService = services[0] || '通用'
  const baseTime = AI_BIZ_BASE_TIME[primaryService] || 12
  const multiplier = status === 'busy' ? 1.3 : status === 'moderate' ? 1.0 : 0.7
  const predictedTime = Math.round(baseTime * multiplier)
  let seed = 0
  for (let i = 0; i < branchName.length; i++) seed += branchName.charCodeAt(i)
  const confidence = 88 + (seed % 10)
  return { time: predictedTime, confidence }
}

interface NavResult {
  matched: BizNavItem
  branch: { name: string; status: 'free' | 'moderate' | 'busy'; distance: number } | null
  aiTime: number
  totalTime: number
}
const navResult = ref<NavResult | null>(null)
const isLoading = ref(false)
const notRecognized = ref(false)

function quickNav(text: string) {
  question.value = text
  navigate()
}

function navigate() {
  if (!question.value.trim()) return
  isLoading.value = true
  navResult.value = null
  notRecognized.value = false

  setTimeout(() => {
    const text = question.value.toLowerCase()
    let matched: BizNavItem | null = null
    for (const biz of Object.values(AI_BIZ_NAV_DB)) {
      if (biz.keywords.some(k => text.includes(k.toLowerCase()))) { matched = biz; break }
    }
    if (!matched) {
      notRecognized.value = true
      isLoading.value = false
      return
    }
    // 找推荐网点
    let recommended = branchStore.branches.find(b => b.services.includes((matched as BizNavItem).recommendService)) || null
    let aiTime = 12
    if (recommended) {
      const p = predictServiceTime(recommended.status, recommended.services, recommended.name)
      aiTime = p.time
    }
    const totalTime = (matched as BizNavItem).steps.reduce((sum, s) => sum + parseInt(s.time.replace(/[~分钟]/g, ''), 10), 0)
    navResult.value = {
      matched: matched as BizNavItem,
      branch: recommended ? { name: recommended.name, status: recommended.status, distance: recommended.distance } : null,
      aiTime,
      totalTime,
    }
    isLoading.value = false
  }, 700)
}

function goToAppointment() {
  router.push('/navigation')
  showModal.value = false
}

function resetState() {
  navResult.value = null
  isLoading.value = false
  notRecognized.value = false
  question.value = ''
}
</script>

<template>
  <!-- AI导航浮动按钮（保留原来一闪一闪的脉冲效果） -->
  <div class="fixed bottom-6 right-6 z-50">
    <button @click="showModal = true; resetState()"
      class="flex items-center gap-2 bg-gradient-to-r from-indigo-600 to-purple-600 text-white rounded-2xl px-4 py-2 shadow-lg animate-pulse hover:shadow-xl transition-shadow cursor-pointer">
      <i class="fa-solid fa-compass"></i>
      <span class="font-bold text-sm">AI导航</span>
    </button>
  </div>

  <!-- AI智能业务导航弹窗 -->
  <div v-if="showModal" class="fixed inset-0 bg-black/50 backdrop-blur-sm z-[60] flex items-center justify-center p-4"
    @click.self="showModal = false">
    <div class="bg-white rounded-3xl shadow-2xl w-full max-w-lg max-h-[85vh] flex flex-col overflow-hidden animate-fade-in">
      <!-- 头部 -->
      <div class="bg-gradient-to-r from-indigo-500 via-purple-500 to-pink-500 p-4 flex items-center justify-between shrink-0">
        <div class="flex items-center gap-2.5 text-white">
          <div class="w-9 h-9 rounded-xl bg-white/20 flex items-center justify-center">
            <i class="fa-solid fa-compass text-lg"></i>
          </div>
          <div>
            <h3 class="text-sm font-black">AI智能业务导航</h3>
            <p class="text-[10px] font-bold text-white/80">自然语言 → 业务拆解 → 网点推荐</p>
          </div>
        </div>
        <button @click="showModal = false"
          class="text-white/80 hover:text-white text-lg w-8 h-8 flex items-center justify-center rounded-lg hover:bg-white/20 transition-all cursor-pointer">
          <i class="fa-solid fa-xmark"></i>
        </button>
      </div>

      <!-- 输入区 -->
      <div class="p-4 border-b border-slate-100 shrink-0">
        <div class="flex gap-2">
          <input v-model="question" type="text" placeholder="描述您想办理的业务，如：我想给国外汇钱"
            @keyup.enter="navigate"
            class="flex-1 bg-slate-50 border border-slate-200 text-xs font-bold rounded-xl p-3 focus:outline-none focus:border-indigo-400 transition-all text-slate-800">
          <button @click="navigate"
            class="bg-gradient-to-r from-indigo-500 to-purple-500 hover:from-indigo-600 hover:to-purple-600 text-white text-xs font-bold px-4 rounded-xl shadow-md transition-all cursor-pointer flex items-center gap-1">
            <i class="fa-solid fa-bolt"></i>解析
          </button>
        </div>
        <!-- 快捷标签 -->
        <div class="flex flex-wrap gap-1.5 mt-2">
          <button @click="quickNav('我想给国外汇钱')"
            class="text-[10px] font-bold text-indigo-600 bg-indigo-50 hover:bg-indigo-100 px-2.5 py-1 rounded-full border border-indigo-200 transition-all cursor-pointer">跨境汇款</button>
          <button @click="quickNav('我想办一张新银行卡')"
            class="text-[10px] font-bold text-indigo-600 bg-indigo-50 hover:bg-indigo-100 px-2.5 py-1 rounded-full border border-indigo-200 transition-all cursor-pointer">开户办卡</button>
          <button @click="quickNav('我想咨询理财')"
            class="text-[10px] font-bold text-indigo-600 bg-indigo-50 hover:bg-indigo-100 px-2.5 py-1 rounded-full border border-indigo-200 transition-all cursor-pointer">理财咨询</button>
          <button @click="quickNav('我要取大额现金')"
            class="text-[10px] font-bold text-indigo-600 bg-indigo-50 hover:bg-indigo-100 px-2.5 py-1 rounded-full border border-indigo-200 transition-all cursor-pointer">大额取现</button>
          <button @click="quickNav('我银行卡丢了想挂失')"
            class="text-[10px] font-bold text-indigo-600 bg-indigo-50 hover:bg-indigo-100 px-2.5 py-1 rounded-full border border-indigo-200 transition-all cursor-pointer">挂失补卡</button>
          <button @click="quickNav('我忘记密码了')"
            class="text-[10px] font-bold text-indigo-600 bg-indigo-50 hover:bg-indigo-100 px-2.5 py-1 rounded-full border border-indigo-200 transition-all cursor-pointer">密码重置</button>
        </div>
      </div>

      <!-- 结果区（滚动） -->
      <div class="flex-1 overflow-y-auto p-4 space-y-3">
        <!-- 加载中 -->
        <div v-if="isLoading" class="text-center py-8">
          <i class="fa-solid fa-spinner fa-spin text-3xl text-indigo-300 mb-2"></i>
          <p class="text-xs font-bold text-slate-500">AI 正在解析您的业务需求...</p>
          <p class="text-[10px] font-bold text-slate-400 mt-1">请稍候</p>
        </div>

        <!-- 未识别 -->
        <div v-else-if="notRecognized" class="text-center py-6">
          <i class="fa-solid fa-circle-question text-3xl text-slate-300 mb-2"></i>
          <p class="text-xs font-bold text-slate-500">未能识别您的业务需求</p>
          <p class="text-[10px] font-bold text-slate-400 mt-1">请尝试更具体的描述，或点击上方快捷标签</p>
        </div>

        <!-- 空状态 -->
        <div v-else-if="!navResult" class="text-center py-8">
          <i class="fa-solid fa-compass text-4xl text-slate-200 mb-2"></i>
          <p class="text-xs font-bold text-slate-400">输入您想办理的业务</p>
          <p class="text-[10px] font-bold text-slate-300 mt-1">AI将为您拆解流程、推荐网点、列出材料</p>
        </div>

        <!-- 解析结果 -->
        <template v-else-if="navResult">
          <!-- AI理解 -->
          <div class="bg-slate-50 rounded-xl p-3 border border-slate-100">
            <div class="flex items-start gap-2">
              <div :class="['w-7 h-7 rounded-lg bg-gradient-to-br', navResult.matched.color, 'flex items-center justify-center text-white shrink-0']">
                <i :class="['fa-solid', navResult.matched.icon, 'text-xs']"></i>
              </div>
              <div>
                <div class="text-[10px] font-black text-slate-400 uppercase">AI理解</div>
                <div class="text-xs font-bold text-slate-700 mt-0.5">{{ navResult.matched.understanding }}</div>
                <div class="text-[10px] font-bold text-indigo-500 mt-1">分类：{{ navResult.matched.title }} · 预计总时长 ~{{ navResult.totalTime }}分钟</div>
              </div>
            </div>
          </div>

          <!-- 办理流程拆解 -->
          <div class="bg-white rounded-xl p-3 border border-slate-100">
            <div class="text-[10px] font-black text-slate-400 uppercase mb-2">办理流程拆解</div>
            <div class="space-y-0">
              <div v-for="(step, idx) in navResult.matched.steps" :key="idx" class="flex gap-3">
                <div class="flex flex-col items-center shrink-0">
                  <div :class="['w-8 h-8 rounded-full bg-gradient-to-br', navResult.matched.color, 'text-white flex items-center justify-center text-xs font-black']">{{ idx + 1 }}</div>
                  <div v-if="idx < navResult.matched.steps.length - 1" class="w-0.5 flex-1 bg-slate-200 mt-1 min-h-[24px]"></div>
                </div>
                <div class="flex-1 pb-3">
                  <div class="flex items-center gap-2 mb-1">
                    <span class="text-xs font-black text-slate-800">{{ step.title }}</span>
                    <span class="text-[9px] font-bold text-amber-500 bg-amber-50 px-1.5 py-0.5 rounded">{{ step.time }}</span>
                  </div>
                  <p class="text-[10px] font-bold text-slate-500 mb-1.5">{{ step.desc }}</p>
                  <div class="flex flex-wrap gap-1">
                    <span v-for="m in step.materials" :key="m"
                      class="text-[9px] font-bold bg-white text-slate-600 px-1.5 py-0.5 rounded border border-slate-200">
                      <i class="fa-solid fa-check text-emerald-400 text-[7px] mr-0.5"></i>{{ m }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- AI推荐网点 -->
          <div v-if="navResult.branch" class="bg-gradient-to-r from-indigo-50 to-purple-50 rounded-xl p-3 border border-indigo-100">
            <div class="flex items-center justify-between mb-1">
              <span class="text-[10px] font-black text-indigo-600 flex items-center gap-1">
                <i class="fa-solid fa-brain text-indigo-500"></i>AI推荐网点
              </span>
              <span :class="[
                'text-[9px] font-bold flex items-center gap-0.5',
                navResult.branch.status === 'busy' ? 'text-rose-500' :
                navResult.branch.status === 'moderate' ? 'text-amber-500' : 'text-emerald-500'
              ]">
                <span class="w-1.5 h-1.5 rounded-full bg-current"></span>
                {{ navResult.branch.status === 'busy' ? '客流繁忙' : navResult.branch.status === 'moderate' ? '客流适中' : '畅通' }}
              </span>
            </div>
            <div class="text-xs font-black text-slate-800">{{ navResult.branch.name }}</div>
            <div class="flex items-center gap-3 mt-1 text-[10px] font-bold text-slate-500">
              <span><i class="fa-solid fa-location-dot text-slate-400"></i> {{ (navResult.branch.distance / 1000).toFixed(1) }}km</span>
              <span><i class="fa-solid fa-brain text-indigo-400"></i> AI预测 ~{{ navResult.aiTime }}分钟</span>
            </div>
            <button @click="goToAppointment"
              class="mt-2 w-full bg-gradient-to-r from-indigo-500 to-purple-500 hover:from-indigo-600 hover:to-purple-600 text-white text-xs font-bold py-2 rounded-xl shadow-md transition-all cursor-pointer">
              <i class="fa-solid fa-ticket mr-1"></i> 去此网点预约
            </button>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>
