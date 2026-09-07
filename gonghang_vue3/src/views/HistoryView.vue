<script setup lang="ts">
import { ref, computed } from 'vue'
import { useAuditLogStore, type AuditLog, type AuditStatus, type BizType } from '../stores/auditLog'

const auditLog = useAuditLogStore()

// 筛选条件
const filterBizType = ref<BizType | 'all'>('all')
const filterStatus = ref<'all' | AuditStatus>('all')
const filterTime = ref<'all' | 'today' | 'week' | 'month'>('all')
const searchText = ref('')

// 详情弹窗
const detailModalOpen = ref(false)
const selectedLog = ref<AuditLog | null>(null)

// 全选
const selectAll = ref(false)
const selectedSet = ref<Set<number>>(new Set())

// 业务类型选项
const bizTypeOptions = [
  { value: 'all' as const, label: '全部类型' },
  { value: 'cash_reserve' as const, label: '大额取现预约' },
  { value: 'open_card' as const, label: '办卡开户' },
  { value: 'corp_transfer' as const, label: '对公跨行转账汇款' },
  { value: 'cash_deposit' as const, label: '对公现金缴款' },
  { value: 'fx_exchange' as const, label: '外币兑换' },
]

// 统计数据
const stats = computed(() => {
  const all = filteredList.value
  const total = all.length
  // 转账汇款 = 对公跨行转账 + 对公现金缴款
  const transfer = all.filter(l => l.bizType === 'corp_transfer' || l.bizType === 'cash_deposit').length
  const other = total - transfer
  return { total, transfer, other }
})

// 时间范围判断
function inTimeRange(timestamp: string, range: string): boolean {
  if (range === 'all') return true
  const logDate = new Date(timestamp.replace(/\//g, '-'))
  const now = new Date()
  const diff = now.getTime() - logDate.getTime()
  const oneDay = 24 * 60 * 60 * 1000
  if (range === 'today') return diff < oneDay && diff >= 0
  if (range === 'week') return diff < 7 * oneDay
  if (range === 'month') return diff < 30 * oneDay
  return true
}

// 筛选后的列表
const filteredList = computed(() => {
  let list = [...auditLog.auditLogs]
  // 业务类型筛选
  if (filterBizType.value !== 'all') {
    list = list.filter(a => a.bizType === filterBizType.value)
  }
  // 状态筛选
  if (filterStatus.value !== 'all') {
    list = list.filter(a => a.status === filterStatus.value)
  }
  // 时间筛选
  list = list.filter(a => inTimeRange(a.timestamp, filterTime.value))
  // 搜索流水号
  if (searchText.value.trim()) {
    const kw = searchText.value.trim().toLowerCase()
    list = list.filter(a => a.sn.toLowerCase().includes(kw))
  }
  // 倒序（最新在前）
  return list.reverse()
})

// 切换全选
function toggleSelectAll() {
  if (selectAll.value) {
    selectedSet.value = new Set(filteredList.value.map(l => l.id))
  } else {
    selectedSet.value = new Set()
  }
}

// 切换单选
function toggleSelect(id: number) {
  if (selectedSet.value.has(id)) {
    selectedSet.value.delete(id)
  } else {
    selectedSet.value.add(id)
  }
  selectAll.value = filteredList.value.length > 0 &&
    filteredList.value.every(l => selectedSet.value.has(l.id))
}

// 时间筛选项标签
const timeLabels: Record<string, string> = {
  all: '全部时间',
  today: '今天',
  week: '近一周',
  month: '近一个月',
}

// 查看凭证详情
function viewDetail(log: AuditLog) {
  selectedLog.value = log
  detailModalOpen.value = true
}

function closeDetailModal() {
  detailModalOpen.value = false
  selectedLog.value = null
}

// 通用下载文件函数
function downloadFile(content: string, filename: string, type: string) {
  const blob = new Blob([content], { type })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

// 导出单条PDF凭证
function exportSinglePDF(logId: number) {
  const log = auditLog.auditLogs.find(l => l.id === logId)
  if (!log) return

  const fieldsText = getFieldsForBizType(log).map(f => `  ${f.label}: ${f.value}`).join('\n')

  const pdfContent = `ICBC · 灵枢 - 预约凭证

==================================================
流水号: ${log.sn}
业务类型: ${log.bizTypeName}
状态: ${log.status}
提交时间: ${log.timestamp}

客户信息:
  客户姓名: ${log.userName || '未填写'}
  身份证号: ${log.idCardMasked || '未填写'}
  手机号码: ${log.phoneMasked || '未填写'}

业务详情:
${fieldsText}

存证凭证号: ${log.hash}
==================================================
此凭证已通过金融级加密技术存证，确保数据安全防篡改。

ICBC · 灵枢智慧银行服务平台`

  downloadFile(pdfContent, `${log.sn}.txt`, 'text/plain')
  closeDetailModal()
}

// 导出选中的PDF凭证
function exportSelectedPDF() {
  if (selectedSet.value.size === 0) {
    alert('请先选择要导出的记录')
    return
  }

  const selectedLogs = auditLog.auditLogs.filter(l => selectedSet.value.has(l.id))

  let pdfContent = `ICBC · 灵枢 - 预约凭证汇总

==================================================
导出时间: ${new Date().toLocaleString('zh-CN')}
共 ${selectedLogs.length} 条记录
==================================================\n\n`

  selectedLogs.forEach((log, index) => {
    pdfContent += `【${index + 1}】${log.bizTypeName}
流水号: ${log.sn}
状态: ${log.status}
时间: ${log.timestamp}
客户: ${log.userName || '未填写'}
存证号: ${log.hash}
--------------------------------------------------\n`
  })

  pdfContent += `\n==================================================
此凭证已通过金融级加密技术存证，确保数据安全防篡改。

ICBC · 灵枢智慧银行服务平台`

  downloadFile(pdfContent, `预约凭证汇总_${new Date().toLocaleDateString('zh-CN')}.txt`, 'text/plain')
}

// 批量删除选中的记录
function deleteSelected() {
  if (selectedSet.value.size === 0) {
    alert('请先选择要删除的记录')
    return
  }
  if (!confirm(`确定删除选中的 ${selectedSet.value.size} 条记录吗？此操作不可撤销。`)) return
  selectedSet.value.forEach(id => auditLog.remove(id))
  selectedSet.value = new Set()
  selectAll.value = false
}

// ========== 各业务类型的额外字段展示 ==========

// 大额取现
function renderCashReserveExtra(log: AuditLog) {
  const ed = log.extraData
  const purposeMap: Record<string, string> = { purchase: '购房/购车', medical: '医疗费用', education: '教育缴费', business: '经营周转', travel: '因私旅游', other: '其他' }
  const fundMap: Record<string, string> = { salary: '工资收入', saving: '储蓄存款', investment: '投资收益', business: '经营收入', inheritance: '继承/赠与', other: '其他' }

  return [
    { label: '金额', value: ed.amount ? `${ed.amount}元` : '未填写' },
    { label: '币种', value: ed.currency || '未填写' },
    { label: '日期', value: ed.date || '未填写' },
    { label: '时间', value: ed.time || '未填写' },
    { label: '网点', value: ed.branch || '未填写', span: 2 },
    { label: '用途', value: purposeMap[ed.purpose || ''] || ed.purpose || '未填写' },
    { label: '资金来源', value: fundMap[ed.fundSource || ''] || ed.fundSource || '未填写' },
    { label: '面额偏好', value: ed.denomination || '未填写' },
  ]
}

// 办卡开户
function renderOpenCardExtra(log: AuditLog) {
  const cardTypeMap: Record<string, string> = { debit_standard: '工银标准借记卡', debit_gold: '工银多币种借记卡', salary: '代发工资卡', platinum: '白金信用卡', student: '校园青春卡', social: '社保联名卡' }

  return [
    { label: '卡类型', value: cardTypeMap[log.extraData.cardType || ''] || log.extraData.cardType?.replace(/_/g, ' ') || '未填写' },
    { label: '账户类型', value: log.extraData.accountType || '未填写' },
    { label: '预存金额', value: log.extraData.initialDeposit ? `${log.extraData.initialDeposit}元` : '未填写' },
    { label: '网上银行', value: log.extraData.onlineBanking === 'yes' ? '是' : '否' },
    { label: '手机银行', value: log.extraData.mobileBanking === 'yes' ? '是' : '否' },
    { label: '领卡方式', value: log.extraData.deliveryMethod === 'branch' ? '网点自取' : log.extraData.deliveryMethod === 'mail' ? '邮寄送达' : '未填写' },
    { label: '紧急联系人', value: log.extraData.emergencyContact || '未填写' },
    { label: '地址/网点', value: log.extraData.address || '未填写', span: 2 },
  ]
}

// 对公跨行转账
function renderCorpTransferExtra(log: AuditLog) {
  return [
    { label: '付款单位', value: log.extraData.payerName || '未填写' },
    { label: '付款账号', value: log.extraData.payerAccount || '未填写' },
    { label: '收款单位', value: log.extraData.recvName || '未填写' },
    { label: '收款账号', value: log.extraData.recvCard || '未填写' },
    { label: '收款银行', value: log.extraData.recvBank || '未填写', span: 2 },
    { label: '转账金额', value: log.extraData.amount ? `${log.extraData.amount}元` : '未填写' },
    { label: '是否加急', value: log.extraData.urgent === 'yes' ? '是' : '否' },
    { label: '资金用途', value: log.extraData.purpose || '未填写', span: 2 },
  ]
}

// 对公现金缴款
function renderCashDepositExtra(log: AuditLog) {
  const depositMap: Record<string, string> = { cash: '现金', check: '转账支票', draft: '银行汇票' }

  return [
    { label: '缴款单位', value: log.extraData.payerName || '未填写' },
    { label: '缴款账号', value: log.extraData.payerAccount || '未填写' },
    { label: '缴款金额', value: log.extraData.amount ? `${log.extraData.amount}元` : '未填写' },
    { label: '缴款方式', value: depositMap[log.extraData.depositMethod || ''] || log.extraData.depositMethod || '未填写' },
    { label: '款项来源', value: log.extraData.purpose || '未填写', span: 2 },
    { label: '办理网点', value: log.extraData.branch || '未填写', span: 2 },
  ]
}

// 外币兑换
function renderFxExchangeExtra(log: AuditLog) {
  const currencyMap: Record<string, string> = { usd: '美元', eur: '欧元', gbp: '英镑', jpy: '日元', hkd: '港币', aud: '澳元', cad: '加元' }
  const purposeMap: Record<string, string> = { travel: '因私旅游', business: '因公出差', study: '留学缴费', medical: '境外就医', trade: '货物贸易', other: '其他' }

  return [
    { label: '兑换币种', value: currencyMap[log.extraData.fxCurrency || ''] || log.extraData.fxCurrency || '未填写' },
    { label: '兑换方向', value: log.extraData.fxDirection === 'buy' ? '购汇' : log.extraData.fxDirection === 'sell' ? '结汇' : '未填写' },
    { label: '外币金额', value: log.extraData.fxAmount || '未填写' },
    { label: '折合人民币', value: log.extraData.amount ? `${log.extraData.amount}元` : '未填写' },
    { label: '用汇用途', value: purposeMap[log.extraData.purpose || ''] || log.extraData.purpose || '未填写' },
    { label: '领取时间', value: log.extraData.time || '未填写' },
    { label: '办理网点', value: log.extraData.branch || '未填写', span: 2 },
  ]
}

interface FieldItem { label: string; value: string; span?: number }

function getFieldsForBizType(log: AuditLog): FieldItem[] {
  switch (log.bizType) {
    case 'cash_reserve': return renderCashReserveExtra(log)
    case 'open_card': return renderOpenCardExtra(log)
    case 'corp_transfer': return renderCorpTransferExtra(log)
    case 'cash_deposit': return renderCashDepositExtra(log)
    case 'fx_exchange': return renderFxExchangeExtra(log)
    default: return []
  }
}
</script>

<template>
  <div class="p-6 space-y-4">
    <!-- 顶部双卡片 -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
      <!-- 左侧统计卡片 -->
      <div class="bg-white/45 backdrop-blur-2xl p-4 rounded-2xl border border-slate-200/80 shadow-sm">
        <div class="flex items-center space-x-3 mb-3">
          <div class="w-9 h-9 rounded-xl bg-blue-500/10 flex items-center justify-center text-blue-600 shrink-0">
            <i class="fa-solid fa-file-lines text-base"></i>
          </div>
          <div>
            <h3 class="text-sm font-black text-slate-800">历史预约单总览</h3>
            <p class="text-xs text-slate-500 font-medium">我的业务预约记录 · 加密存证防篡改</p>
          </div>
        </div>
        <div class="grid grid-cols-3 gap-3">
          <div class="text-center p-3 bg-slate-50/60 rounded-xl border border-slate-100">
            <div class="text-xl font-black text-slate-800">{{ stats.total }}</div>
            <div class="text-[10px] font-bold text-slate-500">总预约数</div>
          </div>
          <div class="text-center p-3 bg-blue-50/60 rounded-xl border border-blue-100">
            <div class="text-xl font-black text-blue-600">{{ stats.transfer }}</div>
            <div class="text-[10px] font-bold text-blue-500">转账汇款</div>
          </div>
          <div class="text-center p-3 bg-amber-50/60 rounded-xl border border-amber-100">
            <div class="text-xl font-black text-amber-600">{{ stats.other }}</div>
            <div class="text-[10px] font-bold text-amber-500">其他业务</div>
          </div>
        </div>
      </div>

      <!-- 右侧存证说明卡片 -->
      <div class="bg-gradient-to-br from-blue-500/5 to-cyan-500/5 backdrop-blur-2xl p-4 rounded-2xl border border-blue-200/50 shadow-sm">
        <div class="flex items-start space-x-3">
          <div class="w-9 h-9 rounded-xl bg-blue-500/10 flex items-center justify-center text-blue-600 shrink-0">
            <i class="fa-solid fa-shield-halved text-base"></i>
          </div>
          <div>
            <h3 class="text-sm font-black text-slate-800">金融单据加密存证</h3>
            <p class="text-xs text-slate-600 font-medium mt-1">
              您的每一笔预约记录都已通过金融级加密技术进行存证，确保数据安全、防篡改。所有记录均具备唯一电子凭证，可随时查阅验证。
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- 筛选区域 -->
    <div class="bg-white/45 backdrop-blur-2xl p-4 rounded-2xl border border-slate-200/80 shadow-sm">
      <div class="grid grid-cols-1 md:grid-cols-4 gap-3">
        <!-- 业务类型筛选 -->
        <div>
          <label class="text-[10px] font-bold text-slate-500 block mb-1">业务类型</label>
          <select v-model="filterBizType"
            class="w-full bg-white/80 border border-slate-200 text-xs font-bold rounded-xl px-3 py-2 focus:outline-none focus:border-blue-500 transition-all text-slate-800 cursor-pointer">
            <option v-for="opt in bizTypeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>
        </div>
        <!-- 状态筛选 -->
        <div>
          <label class="text-[10px] font-bold text-slate-500 block mb-1">办理状态</label>
          <select v-model="filterStatus"
            class="w-full bg-white/80 border border-slate-200 text-xs font-bold rounded-xl px-3 py-2 focus:outline-none focus:border-blue-500 transition-all text-slate-800 cursor-pointer">
            <option value="all">全部状态</option>
            <option value="已提交">待审核</option>
            <option value="已完成">已完成</option>
            <option value="已取消">已取消</option>
          </select>
        </div>
        <!-- 时间筛选 -->
        <div>
          <label class="text-[10px] font-bold text-slate-500 block mb-1">时间范围</label>
          <select v-model="filterTime"
            class="w-full bg-white/80 border border-slate-200 text-xs font-bold rounded-xl px-3 py-2 focus:outline-none focus:border-blue-500 transition-all text-slate-800 cursor-pointer">
            <option v-for="(label, key) in timeLabels" :key="key" :value="key">{{ label }}</option>
          </select>
        </div>
        <!-- 搜索框 -->
        <div>
          <label class="text-[10px] font-bold text-slate-500 block mb-1">搜索流水号</label>
          <div class="relative">
            <input v-model="searchText" type="text" placeholder="输入流水号"
              class="w-full bg-white/80 border border-slate-200 text-xs font-bold rounded-xl px-3 py-2 pr-8 focus:outline-none focus:border-blue-500 transition-all text-slate-800">
            <i class="fa-solid fa-search text-[10px] text-slate-400 absolute right-2 top-2.5"></i>
          </div>
        </div>
      </div>
    </div>

    <!-- 操作栏 -->
    <div class="flex items-center justify-between">
      <div class="flex items-center space-x-2">
        <label class="flex items-center space-x-2 cursor-pointer">
          <input type="checkbox" v-model="selectAll" @change="toggleSelectAll"
            class="w-4 h-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500 cursor-pointer">
          <span class="text-[10px] font-bold text-slate-600">全选</span>
        </label>
        <span class="text-[10px] font-bold text-slate-400">已选 {{ selectedSet.size }} 条</span>
      </div>
      <div class="flex items-center space-x-2">
        <button @click="exportSelectedPDF"
          class="bg-white border border-slate-200 text-xs font-bold px-3 py-1.5 rounded-xl hover:bg-slate-50 transition-all cursor-pointer flex items-center gap-1.5 text-slate-700">
          <i class="fa-solid fa-file-pdf text-rose-500"></i>
          <span>导出凭证</span>
        </button>
        <button @click="deleteSelected"
          class="bg-white border border-red-200 text-xs font-bold px-3 py-1.5 rounded-xl hover:bg-red-50 transition-all cursor-pointer flex items-center gap-1.5 text-red-600">
          <i class="fa-solid fa-trash-can"></i>
          <span>批量删除</span>
        </button>
      </div>
    </div>

    <!-- 个人预约历史流水 -->
    <div class="space-y-3">
      <div class="text-xs font-bold text-slate-500 uppercase tracking-wider flex items-center justify-between px-1">
        <span><i class="fa-solid fa-list-check text-blue-500 mr-1"></i> 个人预约历史流水</span>
        <span class="text-[10px] text-slate-400">共 {{ filteredList.length }} 条记录</span>
      </div>

      <!-- 流水列表 -->
      <div v-if="filteredList.length > 0" class="space-y-3">
        <div v-for="log in filteredList" :key="log.id"
          class="bg-white/70 backdrop-blur-md p-4 rounded-2xl border border-slate-200/80 shadow-xs space-y-2 font-mono cursor-pointer hover:border-blue-300 transition-all"
          @click="viewDetail(log)">
          <!-- 第一行：复选框 + 业务名称 + 状态 + 时间 -->
          <div class="flex items-center justify-between">
            <div class="flex items-center space-x-2">
              <input type="checkbox" :checked="selectedSet.has(log.id)" @click.stop="toggleSelect(log.id)"
                class="log-checkbox w-4 h-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500 cursor-pointer">
              <span class="w-2.5 h-2.5 rounded-full bg-emerald-500"></span>
              <span class="text-sm font-bold text-slate-800">{{ log.bizTypeName }}</span>
              <span class="text-xs bg-slate-100 text-slate-500 font-bold px-2 py-0.5 rounded">{{ log.status }}</span>
            </div>
            <div class="flex items-center space-x-2">
              <span class="text-xs text-slate-400 font-bold">{{ log.timestamp }}</span>
              <i class="fa-solid fa-chevron-right text-xs text-slate-400"></i>
            </div>
          </div>

          <!-- 第二行：流水号 + 客户名 -->
          <div class="text-sm font-sans text-slate-600 font-medium">
            流水号: <span class="font-bold text-slate-800">{{ log.sn }}</span>
            <template v-if="log.userName"> · 客户: {{ log.userName }}</template>
          </div>

          <!-- 第三行：各业务类型的扩展字段 -->
          <div v-if="getFieldsForBizType(log).length > 0" class="grid grid-cols-2 gap-2 text-xs">
            <template v-for="(field, idx) in getFieldsForBizType(log)" :key="idx">
              <div :class="{ 'col-span-2': field.span === 2 }">
                <span class="text-slate-400">{{ field.label }}:</span>
                <span class="font-bold text-slate-700 ml-1">{{ field.value }}</span>
              </div>
            </template>
          </div>

          <!-- 底部：存证信息 -->
          <div class="flex items-center justify-between text-xs text-slate-400 pt-1 border-t border-slate-100">
            <span class="font-bold">存证凭证号</span>
            <span class="font-mono break-all">{{ log.hash }}</span>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="text-center py-12 bg-white/45 backdrop-blur-2xl rounded-2xl border border-slate-200/80">
        <div class="w-16 h-16 mx-auto mb-4 rounded-full bg-slate-100 flex items-center justify-center">
          <i class="fa-solid fa-file-circle-question text-slate-400 text-2xl"></i>
        </div>
        <p class="text-sm font-bold text-slate-500">暂无预约记录</p>
        <p class="text-xs text-slate-400 mt-1">您可以在"智能预填单解析"页面提交业务预约</p>
      </div>
    </div>

    <!-- ================= 详情弹窗 ================= -->
    <Teleport to="body">
      <div v-if="detailModalOpen && selectedLog"
        class="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4"
        @click.self="closeDetailModal">
        <div class="bg-white rounded-3xl w-full max-w-md shadow-2xl border border-slate-200/80 overflow-hidden flex flex-col max-h-[85vh]">
          <!-- 弹窗头部 -->
          <div class="bg-gradient-to-r from-blue-600 via-cyan-500 to-emerald-500 p-4 shrink-0">
            <div class="flex items-center justify-between">
              <div class="flex items-center space-x-2">
                <div class="w-8 h-8 rounded-lg bg-white/20 flex items-center justify-center text-white">
                  <i class="fa-solid fa-file-lines text-sm"></i>
                </div>
                <span class="text-sm font-black text-white">ICBC · 灵枢 · 预约凭证详情</span>
              </div>
              <button @click="closeDetailModal"
                class="w-8 h-8 rounded-lg bg-white/20 flex items-center justify-center text-white hover:bg-white/30 transition-all cursor-pointer">
                <i class="fa-solid fa-xmark text-sm"></i>
              </button>
            </div>
          </div>

          <!-- 弹窗内容 -->
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
                  <span class="text-slate-400">SN:</span> <span class="text-blue-600">{{ selectedLog.sn }}</span>
                </div>
              </div>
              <p class="text-[9px] text-slate-400 mt-1.5">到店出示此码，柜面扫码免填单快速办理</p>
            </div>

            <div class="border-t border-slate-200/60 my-2"></div>

            <!-- 预约详情 -->
            <div class="flex items-center justify-between mb-4">
              <div>
                <h3 class="text-sm font-black text-slate-800">预约详情</h3>
                <p class="text-xs text-slate-500">业务类型: {{ selectedLog.bizTypeName }}</p>
              </div>
              <span class="text-[9px] bg-slate-100 text-slate-500 font-bold px-2 py-1 rounded">{{ selectedLog.status }}</span>
            </div>

            <div class="space-y-3">
              <!-- 基本信息 -->
              <div class="grid grid-cols-2 gap-3 text-xs">
                <div><span class="text-slate-400">流水号:</span> <span class="font-bold text-slate-800">{{ selectedLog.sn }}</span></div>
                <div><span class="text-slate-400">提交时间:</span> <span class="font-bold text-slate-800">{{ selectedLog.timestamp }}</span></div>
                <div><span class="text-slate-400">客户姓名:</span> <span class="font-bold text-slate-800">{{ selectedLog.userName || '未填写' }}</span></div>
                <div><span class="text-slate-400">身份证号:</span> <span class="font-bold text-slate-800">{{ selectedLog.idCardMasked || '未填写' }}</span></div>
                <div class="col-span-2"><span class="text-slate-400">手机号码:</span> <span class="font-bold text-slate-800">{{ selectedLog.phoneMasked || '未填写' }}</span></div>
              </div>

              <div class="border-t border-slate-200/60 my-2"></div>

              <!-- 业务详情 -->
              <div>
                <span class="text-[10px] font-bold text-slate-500 uppercase tracking-wider">业务详情</span>
                <div class="grid grid-cols-2 gap-3 text-xs mt-2">
                  <template v-for="(field, idx) in getFieldsForBizType(selectedLog)" :key="idx">
                    <div :class="{ 'col-span-2': field.span === 2 }">
                      <span class="text-slate-400">{{ field.label }}:</span>
                      <span class="font-bold text-slate-800 ml-1">{{ field.value }}</span>
                    </div>
                  </template>
                </div>
              </div>
            </div>
          </div>

          <!-- 弹窗底部操作 -->
          <div class="border-t border-slate-200/60 p-4 shrink-0 bg-white space-y-3">
            <div class="bg-blue-50/60 p-3 rounded-xl border border-blue-100">
              <div class="flex items-center justify-between text-[10px]">
                <span class="text-blue-600 font-bold">存证凭证号</span>
                <span class="text-slate-700 font-mono break-all">{{ selectedLog.hash }}</span>
              </div>
              <p class="text-[9px] text-slate-500 mt-1">此记录已通过金融级加密技术存证，确保数据安全防篡改</p>
            </div>
            <div class="flex items-center justify-end space-x-2">
              <button @click="closeDetailModal"
                class="bg-white border border-slate-200 text-xs font-bold px-4 py-2 rounded-xl hover:bg-slate-50 transition-all cursor-pointer text-slate-700">
                关闭
              </button>
              <button @click="exportSinglePDF(selectedLog.id)"
                class="bg-blue-600 text-white text-xs font-bold px-4 py-2 rounded-xl hover:bg-blue-700 transition-all cursor-pointer flex items-center gap-1.5">
                <i class="fa-solid fa-file-pdf"></i> 导出凭证
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
