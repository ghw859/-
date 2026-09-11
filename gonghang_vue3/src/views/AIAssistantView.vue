<script setup lang="ts">
import { ref, nextTick, onMounted, watch } from 'vue'
import request from '../utils/request'

interface ChatMsg {
  id: string
  role: 'ai' | 'user'
  content: string
  hasCoT?: boolean
  cotSteps?: string[]
  typing?: boolean
  showQuickCmds?: boolean
}

const messages = ref<ChatMsg[]>([])
const inputMsg = ref('')
const chatContainer = ref<HTMLElement | null>(null)

// 快捷指令（欢迎消息内嵌）
const quickCommands = [
  { label: '附近网点推荐 🏦', cmd: '附近哪个网点人少？' },
  { label: '业务材料咨询 📋', cmd: '我想办外汇需要带什么材料？' },
  { label: '排队等待查询 ⏱️', cmd: '现在排队大概要等多久？' },
]

// 知识库匹配
function getAIReply(query: string): string {
  let responseText = "您好，我是灵枢AI数字人大堂经理。我可以为您推荐网点、查询排队、指导业务办理。请告诉我您想办理什么业务？"

  if (/网点|人少|附近|哪个/.test(query)) {
    responseText = "【AI网点推荐】根据实时客流分析，目前金融街私人银行旗舰支行客流最少（畅通状态），AI预测办理时长仅约6分钟，距您2.1km。其次是望京SOHO社区支行，同样畅通，预计5分钟。建议您优先选择客流畅通的网点，点击左侧'智能预约排队'即可查看详情并预约取号。"
  } else if (/外汇|汇款|跨境|国外/.test(query)) {
    responseText = "【外汇业务指引】跨境汇款需分两步办理：①购汇（人民币兑外币，约10分钟）②跨境汇款（填写收款信息，约20分钟）。所需材料：身份证、银行卡、收款人姓名及地址、收款银行SWIFT代码、汇款用途说明。推荐前往国贸CBD中心支行或北京分行营业部，均支持外汇业务。"
  } else if (/材料|带什么|证件/.test(query)) {
    responseText = "【业务材料清单】不同业务所需材料不同：①开户办卡：身份证+手机号 ②大额取现：身份证+银行卡（须预约）③跨境汇款：身份证+收款人信息+SWIFT代码 ④理财咨询：身份证+银行卡 ⑤挂失补卡：身份证+手机号。建议办理前确认材料齐全，避免白跑一趟。您也可点击右下角'AI导航'按钮，输入业务类型获取详细指引。"
  } else if (/排队|等多久|等待/.test(query)) {
    responseText = "【实时排队查询】目前各网点等待情况：北京分行营业部约25分钟（繁忙）、长安街支行约8分钟（适中）、金融街支行约3分钟（畅通）、中关村支行约12分钟（适中）。建议选择畅通网点预约取号，到场扫码激活后享优先叫号，无需久等。"
  } else if (/预约|取号/.test(query)) {
    responseText = "【预约取号指引】线上预约流程：①点击左侧'智能预约排队'选择网点 ②选择日期和时段（每半小时放10个号） ③选择业务类型 ④确认后生成虚拟号（灰色） ⑤到店扫码激活为预约号（红色）享优先叫号。注意：未按时到店激活将自动失效并扣除信誉分10分。"
  } else if (/开户|办卡|新卡/.test(query)) {
    responseText = "【开户办卡指引】个人开户需携带身份证原件和手机号，办理时长约15分钟。流程：①填写开户申请（5分钟）②柜面核验制卡（10分钟）。推荐前往长安街智慧示范支行，支持自助发卡机快速办理，AI预测仅约4分钟。点击左侧'智能预约排队'即可预约。"
  } else if (/取现|现金|取钱/.test(query)) {
    responseText = "【大额取现指引】大额现金提取须提前预约，办理时长约15分钟。所需材料：身份证+银行卡。流程：①线上预约提现金额和时段 ②到网点柜面核验取款。推荐北京分行营业部，设有大额现金专柜。建议避开上午高峰（10:00-11:00），下午14:00后客流较少。"
  } else if (/挂失|丢了|补卡/.test(query)) {
    responseText = "【挂失补卡指引】银行卡遗失请尽快挂失，办理时长约13分钟。流程：①紧急挂失冻结卡片（3分钟）②补办新卡（10分钟）。所需材料：身份证+手机号+挂失手续费。推荐长安街智慧示范支行，支持自助发卡机快速补办。建议立即预约以免资金风险。"
  } else if (/密码|忘记|重置/.test(query)) {
    responseText = "【密码重置指引】银行卡密码重置需到柜面办理，时长约8分钟。所需材料：身份证+银行卡+手机验证码。流程：①身份核验（3分钟）②密码重置（5分钟）。推荐前往设有VTM自助终端的网点，如长安街智慧示范支行，可快速办理。"
  } else if (/你好|hi|hello|您好/.test(query)) {
    responseText = "您好！我是灵枢AI数字人大堂经理，很高兴为您服务。我可以为您推荐最合适的网点、查询实时排队情况、指导业务办理流程和所需材料。请问您今天想办理什么业务？"
  } else if (/帮助|功能|能做/.test(query)) {
    responseText = "【大堂经理服务清单】我可以为您提供：①智能网点推荐（按客流+距离+业务匹配）②实时排队查询 ③业务办理流程指引 ④所需材料清单 ⑤预约取号指导 ⑥办理进度追踪。您也可点击右下角'AI导航'按钮，输入自然语言快速获取业务指引。"
  } else if (/谢谢|感谢/.test(query)) {
    responseText = "不用客气！很高兴能帮到您。如果您在办理业务过程中有任何疑问，随时可以来找我。祝您办理顺利！"
  } else if (/开户|转账|贷款|挂失|外汇|大额|营业|位置/.test(query)) {
    // 兼容原有的简单关键词
    if (/开户/.test(query)) responseText = "【开户办卡指引】个人开户需携带身份证原件和手机号，办理时长约15分钟。流程：①填写开户申请（5分钟）②柜面核验制卡（10分钟）。推荐前往长安街智慧示范支行，支持自助发卡机快速办理，AI预测仅约4分钟。点击左侧'智能预约排队'即可预约。"
    else if (/转账/.test(query)) responseText = "【对公跨行转账汇款】转账需提供收款人账号、户名和开户行。对公跨行转账还需填写转账用途和加急标识。通过预填单可自动提取信息。所需材料：营业执照原件、法人身份证原件、公章+财务章、对公账户信息。"
    else if (/贷款/.test(query)) responseText = "【贷款咨询】贷款需准备：身份证、收入证明、征信报告、资产证明。不同贷款类型（经营贷/消费贷/房贷）条件不同，建议到网点详细咨询。"
    else if (/挂失/.test(query)) responseText = "【挂失补卡指引】银行卡遗失请尽快挂失，办理时长约13分钟。流程：①紧急挂失冻结卡片（3分钟）②补办新卡（10分钟）。所需材料：身份证+手机号+挂失手续费。"
    else if (/外汇/.test(query)) responseText = "【外汇业务指引】外汇业务需携带身份证，填写外汇用途证明。每人每年限额5万美元等值外汇。建议提前预约。"
    else if (/大额/.test(query)) responseText = "【大额取现指引】大额现金提取须提前预约，办理时长约15分钟。所需材料：身份证+银行卡。流程：①线上预约提现金额和时段 ②到网点柜面核验取款。"
    else if (/营业/.test(query)) responseText = "工行网点营业时间一般为09:00-17:00，部分商圈网点延长至19:00。具体可在网点查询页查看。"
    else if (/位置/.test(query)) responseText = "支持6个网点预约：中关村、建国门、国贸、三里屯、金融街、望京。可在智能预约排队页查看详情。"
  }

  return responseText
}

// 根据 query 生成思维链步骤
function getThoughtSteps(query: string): string[] {
  if (/配置|优化|理财/.test(query)) {
    return [
      "正在穿透隔离级别账户数据安全舱...",
      "正在提取资产全景：储蓄 17.6%、基金 31.6%、理财 50.8%...",
      "正在拉取 2026 年最新一季工银天天盈与权益基金波动率矩阵...",
      "正在计算最优风险对冲系数，生成资产组合再平衡策略..."
    ]
  } else if (/风险|风控|安全/.test(query)) {
    return [
      "正在调取端侧国密安全审计模块...",
      "正在扫描当前连接指纹与异常设备签名...",
      "正在对最近三笔大额流水实施区块链可信哈希二次校验...",
      "确认无封控隐患，开始合成可信风控报告摘要..."
    ]
  }
  return [
    "正在穿透隔离级别账户数据安全舱...",
    "正在唤醒端侧轻量化 NLP 实体识别解析内核...",
    "正在读取多因子资产配置雷达与风控准入模型...",
    "正在拟合决策树，准备输出安全应答..."
  ]
}

// 滚动到底部
function scrollToBottom() {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  })
}

// 使用快捷指令
function useQuick(cmd: string) {
  inputMsg.value = cmd
  sendMessage()
}

// Day8：多轮会话 ID（localStorage 持久化，刷新页面后续接 Redis 上下文）
function getSessionId(): string {
  let sid = localStorage.getItem('lingmou_ai_session')
  if (!sid) {
    sid = 'web_' + Date.now() + '_' + Math.random().toString(36).slice(2, 8)
    localStorage.setItem('lingmou_ai_session', sid)
  }
  return sid
}

// Day8：改调 Python AI 服务 /api/ai/chat（多轮上下文 + 长文知识库）
// 接口失败（服务未启动/网络异常/业务错误）时回退本地 getAIReply()，保证演示不中断
async function fetchReply(query: string): Promise<string> {
  try {
    const data = await request.post<{ reply: string }>('/api/ai/chat', {
      sessionId: getSessionId(),
      message: query,
    })
    if (data && typeof data.reply === 'string' && data.reply.length > 0) {
      return data.reply
    }
    return getAIReply(query)
  } catch {
    return getAIReply(query)
  }
}

// 发送消息
function sendMessage() {
  const query = inputMsg.value.trim()
  if (!query) return

  // 用户消息
  const userMsgId = 'u-' + Date.now()
  messages.value.push({ id: userMsgId, role: 'user', content: query })
  inputMsg.value = ''
  scrollToBottom()

  // AI 回复占位
  const aiMsgId = 'a-' + (Date.now() + 1)
  const cotSteps = getThoughtSteps(query)
  messages.value.push({
    id: aiMsgId,
    role: 'ai',
    content: '',
    hasCoT: true,
    cotSteps: [],
    typing: true,
  })
  scrollToBottom()

  // 逐步展示思维链
  let stepIdx = 0
  const renderNextStep = () => {
    if (stepIdx < cotSteps.length) {
      const msg = messages.value.find(m => m.id === aiMsgId)
      if (msg) msg.cotSteps = [...(msg.cotSteps || []), cotSteps[stepIdx]!]
      stepIdx++
      scrollToBottom()
      setTimeout(renderNextStep, 450)
    } else {
      // 思维链完成，开始打字输出
      setTimeout(async () => {
        const msg = messages.value.find(m => m.id === aiMsgId)
        if (msg) msg.hasCoT = false // 标记为淡化状态

        const reply = await fetchReply(query)
        let charIdx = 0
        const typingTimer = setInterval(() => {
          const m = messages.value.find(x => x.id === aiMsgId)
          if (!m) { clearInterval(typingTimer); return }
          if (charIdx < reply.length) {
            m.content = reply.substring(0, charIdx + 1)
            charIdx++
            scrollToBottom()
          } else {
            clearInterval(typingTimer)
            m.typing = false
            scrollToBottom()
          }
        }, 25)
      }, 300)
    }
  }
  setTimeout(renderNextStep, 700)
}

// 初始化欢迎消息
onMounted(() => {
  messages.value = [{
    id: 'welcome',
    role: 'ai',
    content: '您好！我是"灵枢"AI数字人大堂经理，很高兴为您服务。我可以为您推荐最合适的网点、查询实时排队情况、指导业务办理流程和所需材料。请问今天想办理什么业务？',
    showQuickCmds: true,
  }]
  scrollToBottom()
})
</script>

<template>
  <div class="p-6 h-[calc(100vh-120px)] flex flex-col">
    <div class="bg-white/50 backdrop-blur-2xl p-6 rounded-3xl border border-slate-200/60 shadow-sm flex flex-col h-full min-h-[580px]">

      <!-- 1. 标题头部 -->
      <div class="flex items-center justify-between border-b border-slate-200/60 pb-3 shrink-0">
        <div class="flex items-center space-x-2">
          <div class="w-8 h-8 rounded-lg bg-blue-500/10 flex items-center justify-center text-blue-600">
            <i class="fa-solid fa-robot text-base animate-pulse"></i>
          </div>
          <div>
            <h3 class="text-sm font-black text-slate-800">"灵枢" AI数字人大堂经理</h3>
            <p class="text-xs text-slate-400 font-bold">智能引导 · 网点推荐 · 业务咨询 · 全程陪伴</p>
          </div>
        </div>
        <span class="text-xs font-mono font-bold text-emerald-600 bg-emerald-50 border border-emerald-200 px-2 py-0.5 rounded-md">
          <i class="fa-solid fa-shield-halved"></i> 隐私保护：数据未离端
        </span>
      </div>

      <!-- 2. 消息滚动区 -->
      <div ref="chatContainer" class="flex-grow overflow-y-auto py-4 space-y-4 pr-1 text-sm font-semibold scrollbar-thin">
        <div v-for="msg in messages" :key="msg.id"
          :class="['flex items-start gap-2.5 mb-4', msg.role === 'user' ? 'justify-end' : '']">

          <!-- AI 头像（欢迎/普通AI消息） -->
          <div v-if="msg.role === 'ai'" class="w-8 h-8 rounded-lg bg-blue-600 text-white flex items-center justify-center font-bold text-xs shrink-0 shadow-sm shadow-blue-500/20"
            :class="{ 'animate-pulse': msg.hasCoT && msg.cotSteps && msg.cotSteps.length < 4 }">
            AI
          </div>

          <!-- AI 气泡内容 -->
          <div v-if="msg.role === 'ai'" class="flex-grow max-w-[80%] space-y-2">
            <!-- 思维链展示区 (CoT) -->
            <div v-if="msg.cotSteps && msg.cotSteps.length > 0"
              :class="[
                'bg-slate-50 border border-slate-200/60 rounded-xl p-2.5 text-[11px] font-mono space-y-1 transition-all duration-300',
                !msg.hasCoT ? 'opacity-45' : ''
              ]">
              <div class="flex items-center space-x-1.5 text-slate-500 font-bold mb-1">
                <i :class="['fa-solid fa-brain-circuit text-cyan-500', msg.hasCoT ? 'fa-spin' : '']"></i>
                <span>"灵枢"端侧思维链推理中 (CoT)...</span>
              </div>
              <div class="thought-content space-y-1 pl-3 border-l border-slate-200/80">
                <div v-for="(step, idx) in msg.cotSteps" :key="idx"
                  class="animate-fade-in flex items-center space-x-1">
                  <i class="fa-solid fa-angle-right text-cyan-500"></i>
                  <span class="text-slate-400">{{ step }}</span>
                </div>
              </div>
            </div>

            <!-- 真实回答区（欢迎消息或打字完成后的内容） -->
            <div v-if="msg.id === 'welcome' || (!msg.hasCoT && msg.content) || (msg.hasCoT && msg.cotSteps && msg.cotSteps.length === 4)"
              :class="[
                'p-3.5 rounded-2xl shadow-sm leading-relaxed',
                msg.id === 'welcome' ? 'bg-blue-600 text-white rounded-tl-none' :
                msg.hasCoT ? 'bg-blue-600 text-white rounded-tl-none' :
                'bg-blue-600 text-white rounded-tl-none'
              ]">
              <!-- 文字内容 + 打字光标 -->
              <span>{{ msg.content }}</span>
              <span v-if="msg.typing" class="typing-cursor inline-block w-1.5 h-4 bg-white/90 ml-0.5 animate-pulse">|</span>

              <!-- 欢迎消息内嵌快捷指令 -->
              <template v-if="msg.showQuickCmds">
                <br>
                <span class="mt-1 block text-xs text-cyan-200 font-bold">💡 快捷指令推荐：</span>
                <div class="flex flex-wrap gap-1.5 mt-1.5">
                  <button v-for="q in quickCommands" :key="q.cmd" @click="useQuick(q.cmd)"
                    class="bg-white/20 hover:bg-white/30 text-white text-xs px-2 py-0.5 rounded border border-white/20 transition-all cursor-pointer">
                    {{ q.label }}
                  </button>
                </div>
              </template>
            </div>
          </div>

          <!-- 用户气泡 -->
          <template v-if="msg.role === 'user'">
            <div class="bg-slate-100 text-slate-800 p-3 rounded-2xl rounded-tr-none max-w-[80%] border border-slate-200/60 shadow-xs leading-relaxed text-sm">
              {{ msg.content }}
            </div>
            <div class="w-8 h-8 rounded-lg bg-slate-200 text-slate-700 flex items-center justify-center font-bold text-xs shrink-0 border border-slate-300">您</div>
          </template>

        </div>
      </div>

      <!-- 3. 底部输入控制栏 -->
      <div class="flex items-center space-x-2 pt-3 border-t border-slate-200/60 shrink-0">
        <input v-model="inputMsg" @keyup.enter="sendMessage" type="text"
          placeholder="请问您想办理什么业务？如：办卡、外汇、取现、理财..."
          class="flex-1 bg-white/80 border border-slate-200 text-sm font-bold rounded-xl px-4 py-3 focus:outline-none focus:border-blue-500 transition-all placeholder:text-slate-400">
        <button @click="sendMessage"
          class="bg-gradient-to-r from-blue-600 to-cyan-500 text-white text-sm font-bold px-5 py-3 rounded-xl shadow-md hover:opacity-90 cursor-pointer transition-all shrink-0">
          发送指令
        </button>
      </div>

    </div>
  </div>
</template>

<style scoped>
.scrollbar-thin::-webkit-scrollbar {
  width: 4px;
}
.scrollbar-thin::-webkit-scrollbar-track {
  background: transparent;
}
.scrollbar-thin::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 2px;
}
.scrollbar-thin::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}
</style>
