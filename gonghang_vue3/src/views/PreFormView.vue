<script setup lang="ts">
import { ref, computed, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuditLogStore } from '../stores/auditLog'
import request from '@/utils/request'

const router = useRouter()
const auditLog = useAuditLogStore()

// 业务类型 → 中文名（原先局部定义在 proceedGenerateQR 内，列表渲染也要用，提到顶层）
const bizTypeNameMap: Record<string, string> = {
  cash_reserve: '大额取现预约',
  open_card: '办卡开户',
  corp_transfer: '对公跨行转账汇款',
  cash_deposit: '对公现金缴款',
  fx_exchange: '外币兑换',
}

// AI 折叠面板状态
const aiPanelOpen = ref(false)
function toggleAIPanel() {
  aiPanelOpen.value = !aiPanelOpen.value
}

// 源文本输入
const srcText = ref('')

// 状态信息
const statusText = ref('静态就绪，等待输入触发或直接人工填写')
const statusIcon = ref('fa-solid fa-circle-info')
const statusColor = ref('text-slate-400')

// 当前业务类型
type BizType = 'cash_reserve' | 'open_card' | 'corp_transfer' | 'cash_deposit' | 'fx_exchange' | ''
const currentBizType = ref<BizType>('')

// 业务类型下拉选项
const bizTypeOptions = [
  { value: 'cash_reserve' as BizType, label: '大额预约取现单' },
  { value: 'open_card' as BizType, label: '新开卡/开户申请单' },
  { value: 'corp_transfer' as BizType, label: '对公跨行转账汇款' },
  { value: 'cash_deposit' as BizType, label: '对公现金缴款' },
  { value: 'fx_exchange' as BizType, label: '外币兑换' },
]

// 业务类型 → 主题色映射
const bizTheme: Record<Exclude<BizType, ''>, { bg: string; border: string; iconBg: string; iconColor: string; icon: string; title: string }> = {
  cash_reserve: { bg: 'bg-amber-50/40', border: 'border-amber-100/70', iconBg: 'text-amber-600', iconColor: 'text-amber-600', icon: 'fa-solid fa-vault', title: '业务专项要素：' },
  open_card: { bg: 'bg-emerald-50/40', border: 'border-emerald-100/70', iconBg: 'text-emerald-600', iconColor: 'text-emerald-600', icon: 'fa-solid fa-credit-card', title: '业务专项要素：' },
  corp_transfer: { bg: 'bg-indigo-50/40', border: 'border-indigo-100/70', iconBg: 'text-indigo-600', iconColor: 'text-indigo-600', icon: 'fa-solid fa-building-columns', title: '业务专项要素：' },
  cash_deposit: { bg: 'bg-orange-50/40', border: 'border-orange-100/70', iconBg: 'text-orange-600', iconColor: 'text-orange-600', icon: 'fa-solid fa-money-bill-wave', title: '业务专项要素：' },
  fx_exchange: { bg: 'bg-purple-50/40', border: 'border-purple-100/70', iconBg: 'text-purple-600', iconColor: 'text-purple-600', icon: 'fa-solid fa-coins', title: '业务专项要素：' },
}

// 基础身份要素（AI 自动提取）
const baseInfo = ref({
  userName: '',
  idCard: '',
  phone: '',
})

// 专项业务要素
const cashReserveFields = ref({
  amount: '',
  currency: 'CNY',
  date: '',
  time: '',
  branch: '',
  purpose: '',
  fundSource: '',
  denomination: '100',
})

const openCardFields = ref({
  cardType: 'debit_standard',
  accountType: 'type1',
  initialDeposit: '',
  onlineBanking: 'yes',
  mobileBanking: 'yes',
  deliveryMethod: 'branch',
  emergencyContact: '',
  address: '',
})

const corpTransferFields = ref({
  payerName: '',
  payerAccount: '',
  recvName: '',
  recvCard: '',
  recvBank: '',
  amount: '',
  urgent: 'no',
  purpose: '',
})

const cashDepositFields = ref({
  payerName: '',
  payerAccount: '',
  amount: '',
  depositMethod: 'cash',
  purpose: '',
  branch: '',
})

const fxExchangeFields = ref({
  fxCurrency: 'usd',
  fxDirection: 'buy',
  fxAmount: '',
  amount: '',
  purpose: '',
  time: '',
  branch: '',
})

// 电子签名/盖章状态（存储签名/公章图片dataURL）
const signatureState = ref({
  operatorSigned: '',
  authorizerSigned: '',
  corpSeal: '',
  financeSeal: '',
  personalSigned: '',
})

function resetSignatures() {
  signatureState.value = {
    operatorSigned: '',
    authorizerSigned: '',
    corpSeal: '',
    financeSeal: '',
    personalSigned: '',
  }
}

// ====== 手写签名弹窗 ======
const sigModalOpen = ref(false)
const sigHasContent = ref(false)
let sigCanvas: HTMLCanvasElement | null = null
let sigCtx: CanvasRenderingContext2D | null = null
let sigDrawing = false
const sigTarget = ref<keyof typeof signatureState.value | ''>('')

function openSignaturePad(target: keyof typeof signatureState.value) {
  sigTarget.value = target
  sigModalOpen.value = true
  sigHasContent.value = false
  nextTick(() => {
    sigCanvas = document.getElementById('signature-canvas') as HTMLCanvasElement
    if (!sigCanvas) return
    sigCtx = sigCanvas.getContext('2d')
    if (!sigCtx) return
    sigCtx.strokeStyle = '#1e293b'
    sigCtx.lineWidth = 2.5
    sigCtx.lineCap = 'round'
    sigCtx.lineJoin = 'round'
    clearSignature()
  })
}

function sigStartDraw(x: number, y: number) {
  if (!sigCtx) return
  sigDrawing = true
  sigCtx.beginPath()
  sigCtx.moveTo(x, y)
  sigHasContent.value = true
}

function sigDraw(x: number, y: number) {
  if (!sigCtx || !sigDrawing) return
  sigCtx.lineTo(x, y)
  sigCtx.stroke()
}

function sigStopDraw() {
  sigDrawing = false
}

function onCanvasMouseDown(e: MouseEvent) {
  const rect = sigCanvas!.getBoundingClientRect()
  sigStartDraw(e.clientX - rect.left, e.clientY - rect.top)
}
function onCanvasMouseMove(e: MouseEvent) {
  if (!sigDrawing) return
  const rect = sigCanvas!.getBoundingClientRect()
  sigDraw(e.clientX - rect.left, e.clientY - rect.top)
}
function onCanvasTouchStart(e: TouchEvent) {
  e.preventDefault()
  const rect = sigCanvas!.getBoundingClientRect()
  const t = e.touches[0]!
  sigStartDraw(t.clientX - rect.left, t.clientY - rect.top)
}
function onCanvasTouchMove(e: TouchEvent) {
  e.preventDefault()
  if (!sigDrawing) return
  const rect = sigCanvas!.getBoundingClientRect()
  const t = e.touches[0]!
  const scaleX = sigCanvas!.width / rect.width
  const scaleY = sigCanvas!.height / rect.height
  sigDraw((t.clientX - rect.left) * scaleX, (t.clientY - rect.top) * scaleY)
}
function onCanvasTouchEnd(e: TouchEvent) {
  e.preventDefault()
  sigStopDraw()
}

function clearSignature() {
  if (!sigCtx || !sigCanvas) return
  sigCtx.clearRect(0, 0, sigCanvas.width, sigCanvas.height)
  sigHasContent.value = false
}

function closeSignaturePad() {
  sigModalOpen.value = false
  sigTarget.value = ''
}

function confirmSignature() {
  if (!sigHasContent.value || !sigCanvas) return
  const dataUrl = sigCanvas.toDataURL('image/png')
  if (sigTarget.value) {
    signatureState.value[sigTarget.value] = dataUrl
  }
  closeSignaturePad()
}

// ====== 电子签章 - 上传公章图片 ======
const sealTarget = ref<keyof typeof signatureState.value | ''>('')

function stampSeal(target: keyof typeof signatureState.value) {
  sealTarget.value = target
  // 触发隐藏的file input
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/png,image/jpeg,image/jpg'
  input.style.display = 'none'
  input.onchange = (e) => {
    const file = (e.target as HTMLInputElement).files?.[0]
    if (!file) return
    const reader = new FileReader()
    reader.onload = (event) => {
      const result = event.target?.result as string
      if (result && sealTarget.value) {
        signatureState.value[sealTarget.value] = result
      }
    }
    reader.readAsDataURL(file)
  }
  document.body.appendChild(input)
  input.click()
  document.body.removeChild(input)
}

// 是否对公业务
const isCorporate = computed(() => currentBizType.value === 'corp_transfer' || currentBizType.value === 'cash_deposit')

// 下方人工直填
const manualInfo = ref({
  userName: '',
  idCard: '',
  phone: '',
  notes: '',
  frontUploaded: false,
  backUploaded: false,
})

// 二维码生成
const showQRCode = ref(false)
const qrSn = ref('ICBC-2026-XXXX')

// AI 材料预检弹窗
const matModalOpen = ref(false)
const matBizName = ref('')
const matList = ref<{ name: string; required: boolean; icon: string }[]>([])

const bizMaterialsMap: Record<string, { name: string; materials: { name: string; required: boolean; icon: string }[] }> = {
  transfer: {
    name: '转账汇款', materials: [
      { name: '身份证原件', required: true, icon: 'fa-id-card' },
      { name: '银行卡（汇出账户）', required: true, icon: 'fa-credit-card' },
      { name: '收款人姓名与账号', required: true, icon: 'fa-user' },
      { name: '收款人开户行信息', required: false, icon: 'fa-building-columns' }
    ]
  },
  cash_reserve: {
    name: '大额取现预约', materials: [
      { name: '身份证原件', required: true, icon: 'fa-id-card' },
      { name: '银行卡', required: true, icon: 'fa-credit-card' },
      { name: '预约确认短信', required: true, icon: 'fa-comment-sms' },
      { name: '现金用途说明（5万以上）', required: false, icon: 'fa-file-lines' }
    ]
  },
  open_card: {
    name: '办卡开户', materials: [
      { name: '身份证原件', required: true, icon: 'fa-id-card' },
      { name: '实名认证手机号', required: true, icon: 'fa-mobile-screen' },
      { name: '初始存入资金', required: true, icon: 'fa-coins' },
      { name: '住址证明（部分情况）', required: false, icon: 'fa-house' },
      { name: '紧急联系人信息', required: false, icon: 'fa-address-book' }
    ]
  },
  corp_transfer: {
    name: '对公跨行转账汇款', materials: [
      { name: '营业执照原件', required: true, icon: 'fa-file-contract' },
      { name: '法人身份证原件', required: true, icon: 'fa-id-card' },
      { name: '公章 + 财务章', required: true, icon: 'fa-stamp' },
      { name: '对公账户信息', required: true, icon: 'fa-building-columns' },
      { name: '收款方完整信息', required: true, icon: 'fa-user' },
      { name: '转账用途说明', required: false, icon: 'fa-file-lines' }
    ]
  },
  cash_deposit: {
    name: '对公现金缴款', materials: [
      { name: '营业执照原件', required: true, icon: 'fa-file-contract' },
      { name: '法人身份证原件', required: true, icon: 'fa-id-card' },
      { name: '公章 + 财务章', required: true, icon: 'fa-stamp' },
      { name: '缴款单', required: true, icon: 'fa-file-lines' },
      { name: '现金（按面额整理）', required: true, icon: 'fa-coins' }
    ]
  },
  fx_exchange: {
    name: '外币兑换', materials: [
      { name: '身份证原件', required: true, icon: 'fa-id-card' },
      { name: '银行卡或人民币现金', required: true, icon: 'fa-credit-card' },
      { name: '外汇用途说明', required: true, icon: 'fa-file-lines' },
      { name: '额度证明（等值1万美元以上）', required: false, icon: 'fa-file-shield' }
    ]
  },
}

function generateQRCode() {
  if (!currentBizType.value) {
    statusText.value = '请先选择业务模板或运行 AI 解析'
    statusIcon.value = 'fa-solid fa-triangle-exclamation'
    statusColor.value = 'text-amber-500'
    return
  }
  // 弹出AI材料预检
  const info = bizMaterialsMap[currentBizType.value] || bizMaterialsMap.transfer!
  matBizName.value = info.name
  matList.value = info.materials
  matModalOpen.value = true
}

function closeMatModal() {
  matModalOpen.value = false
}

// 我的预填单（后端 /api/preforms/my）
const preFormList = ref<any[]>([])

async function loadMyPreForms() {
  preFormList.value = (await request.get('/api/preforms/my')) as any[]
}

/** 材料图片标记：目前没有真实上传，只把「传了哪几项」记下来 */
function buildImageUrls(): string | undefined {
  const urls: string[] = []
  if (manualInfo.value.frontUploaded) urls.push('idcard_front')
  if (manualInfo.value.backUploaded) urls.push('idcard_back')
  return urls.length > 0 ? JSON.stringify(urls) : undefined
}

/** 组装 PreFormRequest */
function buildPreFormPayload(bizType: string, extraDataMap: Record<string, object>) {
  // 签名/公章存的是 canvas.toDataURL() 全量字符串（动辄几万字符），
  // 而 pre_forms.signature_url 只有 VARCHAR(255)，发过去会触发
  // MySQL Data too long，被全局异常处理兜成 90000 系统繁忙 —— 看不出任何原因。
  // 因此只记录「签了哪几项」，不传图片本体。
  const signatureKeys = Object.entries(signatureState.value)
    .filter(([, v]) => !!v)
    .map(([k]) => k)

  return {
    businessType: bizType,
    // 从模板下拉直接提交时 srcText 是空的，而 rawText 后端是 @NotBlank，
    // 没有这句兜底会直接吃一个 10001 参数错误
    rawText: srcText.value.trim() || `[手动录入] ${bizTypeNameMap[bizType] || '其他业务'}`,
    parsedJson: JSON.stringify({
      bizType,
      baseInfo: { ...baseInfo.value },
      fields: extraDataMap[bizType] || {},
      manual: {
        notes: manualInfo.value.notes,
        frontUploaded: manualInfo.value.frontUploaded,
        backUploaded: manualInfo.value.backUploaded,
      },
      signatureKeys,
    }),
    imageUrls: buildImageUrls(),
  }
}

async function proceedGenerateQR() {
  closeMatModal()
  // 生成防伪业务流水号
  const randomId = Math.floor(1000 + Math.random() * 9000)
  const timestamp = new Date().toISOString().slice(0, 10).replace(/-/g, '')
  qrSn.value = `ICBC-${timestamp}-${randomId}`
  showQRCode.value = true

  const bizType = currentBizType.value || 'cash_reserve'
  const extraDataMap: Record<string, object> = {
    cash_reserve: { ...cashReserveFields.value },
    open_card: { ...openCardFields.value },
    corp_transfer: { ...corpTransferFields.value },
    cash_deposit: { ...cashDepositFields.value },
    fx_exchange: { ...fxExchangeFields.value },
  }

  // 预填单落库。失败就停在这里保留现场，不能跳走假装成功
  try {
    const created = (await request.post('/api/preforms',
      buildPreFormPayload(bizType, extraDataMap))) as any
    preFormList.value.unshift(created)
  } catch (e: any) {
    statusText.value = '预填单保存失败：' + (e?.message || '未知错误')
    statusIcon.value = 'fa-solid fa-triangle-exclamation'
    statusColor.value = 'text-rose-500'
    return
  }

  // 保存预填单记录到审计日志，供历史预约查询页面展示
  auditLog.add({
    bizType: bizType as 'cash_reserve' | 'open_card' | 'corp_transfer' | 'cash_deposit' | 'fx_exchange',
    bizTypeName: bizTypeNameMap[bizType] || '其他业务',
    userName: baseInfo.value.userName,
    idCardMasked: baseInfo.value.idCard,
    phoneMasked: baseInfo.value.phone,
    extraData: extraDataMap[bizType] || {},
    timestamp: new Date().toLocaleString('zh-CN'),
    status: '已提交',
  })

  // 跳转到业务直通码页面
  router.push('/qrcode')
}

onMounted(() => {
  // 失败静默：拦截器已经 alert 过了，这里只避免未处理的 rejection
  loadMyPreForms().catch(() => {})
})

// 快速测试模版
function applyTestTemplate(idx: number) {
  const templates = [
    '本人李四，身份证号310104199008085678，电话13812345678，想办一张工银白金信用卡，开通网银和手机银行，预存500元，卡片邮寄到北京市朝阳区建国门外大街1号。',
    '代办人王五，身份证号110101199505051234，因购车急需现金，资金来源为储蓄存款，预约明天上午10点去工行北京分行营业部大额取现五十万元，要100元面额。',
    '本人张明，身份证号310104198810101234，电话13912345678，办理对公跨行转账汇款，付款单位上海宏达贸易有限公司，付款账号1001234567890123，收款单位北京中科科技股份有限公司，收款账号6222021234567890123，收款银行中国建设银行北京分行营业部，转账金额500000元，用途为货款，需要加急办理。',
  ]
  srcText.value = templates[idx]!
}

// 核心：AI NLP 解析（复刻 runLocalNLPParse）
function runNLPParse() {
  const src = srcText.value.trim()
  if (!src) {
    statusText.value = '请先在左侧粘贴或输入非结构化文本！'
    statusIcon.value = 'fa-solid fa-triangle-exclamation'
    statusColor.value = 'text-amber-500'
    return
  }

  showQRCode.value = false
  statusText.value = '端侧轻量级 NLP 引擎正在提取业务要素...'
  statusIcon.value = 'fa-solid fa-spinner animate-spin'
  statusColor.value = 'text-cyan-600'

  // 1. 提取公共身份要素
  let name = ''
  let idCard = ''
  let phone = ''

  const nameMatch = src.match(/(?:我是|本人|代办人|叫|客户)\s*([\u4e00-\u9fa5]{2,4})/)
  if (nameMatch) name = nameMatch[1] ?? ''

  const idMatch = src.match(/(\d{17}[\dXx]|\d{15})/)
  if (idMatch) idCard = idMatch[1] ?? ''

  const phoneMatch = src.match(/(1[3-9]\d{9})/)
  if (phoneMatch) phone = phoneMatch[1] ?? ''

  baseInfo.value = { userName: name, idCard, phone }

  // 2. 智能路由判定业务类型
  let bizType: Exclude<BizType, ''> = 'cash_reserve'
  if (/(开卡|办卡|开户|新办|信用卡|社保卡|医保卡|储蓄卡|借记卡|工资卡|校园卡|学生卡)/.test(src)) {
    bizType = 'open_card'
  } else if (/(对公.*转账|跨行.*汇款|公司.*转账|企业.*转账|对公.*汇款)/.test(src)) {
    bizType = 'corp_transfer'
  } else if (/(现金缴款|对公.*缴款|单位.*缴款|公司.*缴款)/.test(src)) {
    bizType = 'cash_deposit'
  } else if (/(外币|兑换|购汇|结汇|换汇|美元|欧元|英镑|日元|港币)/.test(src)) {
    bizType = 'fx_exchange'
  } else if (/(大额|取现|预约取|提取现金|大额头寸|取钱|领现金)/.test(src)) {
    bizType = 'cash_reserve'
  }

  currentBizType.value = bizType
  resetSignatures()
  manualInfo.value = { userName: '', idCard: '', phone: '', notes: '', frontUploaded: false, backUploaded: false }

  // 3. 各业务专项要素提取
  if (bizType === 'cash_reserve') {
    const amountMatch = src.match(/取现(\d+|[一二三四五六七八九十百千万]+)元/)
    cashReserveFields.value.amount = amountMatch ? (amountMatch[1] ?? '') : ''
    const timeMatch = src.match(/(明天上午|今天下午|上午\d+点|\d+月\d+日|\d+点)/)
    cashReserveFields.value.time = timeMatch ? (timeMatch[1] ?? '') : ''
    const branchMatch = src.match(/(工行|银行)([\u4e00-\u9fa5]+营业部|[\u4e00-\u9fa5]+支行|[\u4e00-\u9fa5]+分行)/)
    cashReserveFields.value.branch = branchMatch ? (branchMatch[1] ?? '') + (branchMatch[2] ?? '') : ''
    const purposeMatch = src.match(/因([\u4e00-\u9fa5]+)急需现金|现金用途([\u4e00-\u9fa5]+)|用于([\u4e00-\u9fa5]+)/)
    const purposeStr = purposeMatch ? ((purposeMatch[1] ?? '') || (purposeMatch[2] ?? '') || (purposeMatch[3] ?? '') || '') : ''
    cashReserveFields.value.purpose = purposeStr.includes('购') ? 'purchase' : purposeStr.includes('医') ? 'medical' : purposeStr.includes('学') || purposeStr.includes('教育') ? 'education' : purposeStr.includes('经营') ? 'business' : purposeStr.includes('旅游') ? 'travel' : ''
    const fundMatch = src.match(/资金来源为?(工资|储蓄|投资|经营|继承|其他)/)
    if (fundMatch) {
      const map: Record<string, string> = { 工资: 'salary', 储蓄: 'saving', 投资: 'investment', 经营: 'business', 继承: 'inheritance', 其他: 'other' }
      const key = fundMatch[1] ?? ''
      cashReserveFields.value.fundSource = map[key as keyof typeof map] ?? ''
    } else {
      cashReserveFields.value.fundSource = ''
    }
    const denomMatch = src.match(/(\d+)元面额|要(\d+)元/)
    cashReserveFields.value.denomination = denomMatch ? ((denomMatch[1] ?? '') || (denomMatch[2] ?? '')) : '100'
  } else if (bizType === 'open_card') {
    let selectedType = 'debit_standard'
    if (/(信用卡|白金卡)/.test(src)) selectedType = 'platinum'
    else if (/(工资卡|代发卡)/.test(src)) selectedType = 'salary'
    else if (/(储蓄卡|借记卡)/.test(src)) selectedType = 'debit_gold'
    else if (/(校园|学生|青春)/.test(src)) selectedType = 'student'
    else if (/(社保|医保|健康)/.test(src)) selectedType = 'social'
    openCardFields.value.cardType = selectedType

    const addrMatch = src.match(/(寄到|寄往|地址是|住址|寄送至)([\u4e00-\u9fa5\w\d]+号)/)
    openCardFields.value.address = addrMatch ? (addrMatch[2] ?? '') : ''

    const depositMatch = src.match(/预存(\d+)元/)
    openCardFields.value.initialDeposit = depositMatch ? (depositMatch[1] ?? '') : ''

    openCardFields.value.onlineBanking = /(不开网银|不要网银)/.test(src) ? 'no' : 'yes'
    openCardFields.value.mobileBanking = /(不开手机银行|不要手机银行)/.test(src) ? 'no' : 'yes'
    openCardFields.value.deliveryMethod = /(邮寄|寄到|寄往|寄送)/.test(src) ? 'mail' : 'branch'
  } else if (bizType === 'corp_transfer') {
    const amountMatch = src.match(/转账金额(\d+|[一二三四五六七八九十百千万]+)元|金额(\d+|[一二三四五六七八九十百千万]+)元|(\d+|[一二三四五六七八九十百千万]+)元/)
    corpTransferFields.value.amount = amountMatch ? ((amountMatch[1] ?? '') || (amountMatch[2] ?? '') || (amountMatch[3] ?? '')) : ''
    const purposeMatch = src.match(/用途为?(货款|服务费|保证金|工资|其他)/)
    corpTransferFields.value.purpose = purposeMatch ? (purposeMatch[1] ?? '') : ''
    const payerNameMatch = src.match(/付款单位([\u4e00-\u9fa5]+(?:有限公司|股份有限公司|有限责任公司|集团|公司))/)
    corpTransferFields.value.payerName = payerNameMatch ? (payerNameMatch[1] ?? '') : ''
    const payerAccMatch = src.match(/付款账号(\d{10,19})/)
    corpTransferFields.value.payerAccount = payerAccMatch ? (payerAccMatch[1] ?? '') : ''
    const recvNameMatch = src.match(/收款单位([\u4e00-\u9fa5]+(?:有限公司|股份有限公司|有限责任公司|集团|公司))/)
    corpTransferFields.value.recvName = recvNameMatch ? (recvNameMatch[1] ?? '') : ''
    const recvAccMatch = src.match(/收款账号(\d{10,19})/)
    corpTransferFields.value.recvCard = recvAccMatch ? (recvAccMatch[1] ?? '') : ''
    const recvBankMatch = src.match(/收款银行([\u4e00-\u9fa5]+(?:分行|支行|营业部|分理处|储蓄所|银行))/)
    corpTransferFields.value.recvBank = recvBankMatch ? (recvBankMatch[1] ?? '') : ''
    corpTransferFields.value.urgent = /加急/.test(src) ? 'yes' : 'no'
  } else if (bizType === 'cash_deposit') {
    const amountMatch = src.match(/缴款(\d+|[一二三四五六七八九十百千万]+)元|(\d+|[一二三四五六七八九十百千万]+)元缴款/)
    cashDepositFields.value.amount = amountMatch ? ((amountMatch[1] ?? '') || (amountMatch[2] ?? '')) : ''
  } else if (bizType === 'fx_exchange') {
    let fxCurrency = 'usd'
    if (/欧元|EUR/i.test(src)) fxCurrency = 'eur'
    else if (/英镑|GBP/i.test(src)) fxCurrency = 'gbp'
    else if (/日元|JPY/i.test(src)) fxCurrency = 'jpy'
    else if (/港币|HKD/i.test(src)) fxCurrency = 'hkd'
    else if (/澳元|AUD/i.test(src)) fxCurrency = 'aud'
    else if (/加元|CAD/i.test(src)) fxCurrency = 'cad'
    fxExchangeFields.value.fxCurrency = fxCurrency
    fxExchangeFields.value.fxDirection = /(结汇|外币.*换.*人民币)/.test(src) ? 'sell' : 'buy'
    const fxAmountMatch = src.match(/(\d+)\s*(美元|欧元|英镑|日元|港币|澳元|加元|USD|EUR|GBP|JPY|HKD|AUD|CAD)/i)
    fxExchangeFields.value.fxAmount = fxAmountMatch ? (fxAmountMatch[1] ?? '') : ''
  }

  setTimeout(() => {
    statusText.value = `已切换至 [${bizTypeOptions.find(o => o.value === bizType)?.label}] 模板，AI 已完成要素提取，请核验并补充`
    statusIcon.value = 'fa-solid fa-circle-check'
    statusColor.value = 'text-emerald-600'
  }, 400)
}

// 手动切换业务模板（复刻 switchBusinessFormLayout）
function switchBusinessFormLayout(type: BizType) {
  if (!type) return
  currentBizType.value = type
  srcText.value = ''
  showQRCode.value = false
  resetSignatures()

  // 重置 AI 提取的基础信息
  baseInfo.value = { userName: '', idCard: '', phone: '' }
  // 重置下方手动录入
  manualInfo.value = { userName: '', idCard: '', phone: '', notes: '', frontUploaded: false, backUploaded: false }

  // 重置对应业务字段为空
  if (type === 'cash_reserve') {
    cashReserveFields.value = { amount: '', currency: 'CNY', date: '', time: '', branch: '', purpose: '', fundSource: '', denomination: '100' }
  } else if (type === 'open_card') {
    openCardFields.value = { cardType: 'debit_standard', accountType: 'type1', initialDeposit: '', onlineBanking: 'yes', mobileBanking: 'yes', deliveryMethod: 'branch', emergencyContact: '', address: '' }
  } else if (type === 'corp_transfer') {
    corpTransferFields.value = { payerName: '', payerAccount: '', recvName: '', recvCard: '', recvBank: '', amount: '', urgent: 'no', purpose: '' }
  } else if (type === 'cash_deposit') {
    cashDepositFields.value = { payerName: '', payerAccount: '', amount: '', depositMethod: 'cash', purpose: '', branch: '' }
  } else if (type === 'fx_exchange') {
    fxExchangeFields.value = { fxCurrency: 'usd', fxDirection: 'buy', fxAmount: '', amount: '', purpose: '', time: '', branch: '' }
  }

  statusText.value = `已切换至 [${bizTypeOptions.find(o => o.value === type)?.label}] 模板，您可以直接在下方手动录入`
  statusIcon.value = 'fa-solid fa-circle-info'
  statusColor.value = 'text-slate-500'
}

</script>

<template>
  <div class="p-6 space-y-4">
    <div class="bg-white/50 backdrop-blur-2xl p-6 rounded-3xl border border-slate-200/60 shadow-sm space-y-6">
      <!-- 头部标题 -->
      <div class="flex items-center space-x-2 border-b border-slate-200/60 pb-3">
        <div class="w-8 h-8 rounded-lg bg-cyan-500/10 flex items-center justify-center text-cyan-600">
          <i class="fa-regular fa-file-lines text-base"></i>
        </div>
        <div>
          <h3 class="text-sm font-black text-slate-800">智能多模态预填单解析中心</h3>
          <p class="text-xs text-slate-400 font-bold">端侧轻量级 NLP 抽取引擎 · 业务要素人工核验与补充</p>
        </div>
      </div>

      <div class="grid grid-cols-1 gap-6">
        <!-- AI 输入与指令台（可折叠） -->
        <div class="rounded-2xl border border-slate-200/60 overflow-hidden">
          <button @click="toggleAIPanel"
            class="w-full flex items-center justify-between px-4 py-3 bg-gradient-to-r from-cyan-50 to-blue-50 hover:from-cyan-100 hover:to-blue-100 transition-all cursor-pointer">
            <div class="flex items-center space-x-2">
              <div class="w-7 h-7 rounded-lg bg-cyan-500/10 flex items-center justify-center text-cyan-600">
                <i class="fa-solid fa-wand-magic-sparkles text-xs"></i>
              </div>
              <div class="text-left">
                <span class="text-sm font-black text-slate-800">AI 智能文本解析</span>
                <span class="text-[10px] text-slate-400 font-bold ml-1">大白话一键填单</span>
              </div>
            </div>
            <div class="flex items-center space-x-2">
              <span class="text-sm text-slate-500 font-bold">{{ aiPanelOpen ? '点击收起' : '点击展开' }}</span>
              <i :class="['fa-solid fa-chevron-down text-sm text-slate-400 transition-transform duration-300', aiPanelOpen ? 'rotate-180' : '']"></i>
            </div>
          </button>

          <div v-show="aiPanelOpen" class="p-4 space-y-4 bg-white/60">
            <div class="space-y-1.5">
              <label class="text-xs font-bold text-slate-500 uppercase tracking-wider block">请粘贴或输入非结构化文本单据：</label>
              <textarea v-model="srcText" rows="6"
                class="w-full bg-white/80 border border-slate-200 text-sm font-bold rounded-xl p-3 focus:outline-none focus:border-cyan-500 transition-all placeholder:text-slate-400 leading-relaxed font-sans"
                placeholder="例如：我是王五，电话13911112222，证件号110101199505051234，明天上午打算去工行分行取现五十万元买车..."></textarea>
            </div>

            <button @click="runNLPParse"
              class="w-full bg-gradient-to-r from-cyan-600 to-blue-600 text-white text-sm font-bold py-2.5 rounded-xl shadow-md hover:opacity-95 cursor-pointer transition-all flex items-center justify-center gap-2">
              <i class="fa-solid fa-wand-magic-sparkles"></i> 启动 AI 结构化解析
            </button>

            <!-- 快速测试模版 -->
            <div class="bg-slate-50/50 border border-slate-200/40 rounded-xl p-3 space-y-2">
              <span class="text-[10px] font-bold text-slate-400 block">💡 快速测试模版：</span>
              <div class="flex flex-col gap-1.5">
                <button @click="applyTestTemplate(0)"
                  class="text-[11px] text-left bg-white hover:bg-slate-100 text-slate-600 font-semibold px-2 py-1.5 rounded-lg border border-slate-200 transition-all cursor-pointer truncate">
                  📂 模拟新开卡开户文本
                </button>
                <button @click="applyTestTemplate(1)"
                  class="text-[11px] text-left bg-white hover:bg-slate-100 text-slate-600 font-semibold px-2 py-1.5 rounded-lg border border-slate-200 transition-all cursor-pointer truncate">
                  📂 模拟大额取现预约文本
                </button>
                <button @click="applyTestTemplate(2)"
                  class="text-[11px] text-left bg-white hover:bg-slate-100 text-slate-600 font-semibold px-2 py-1.5 rounded-lg border border-slate-200 transition-all cursor-pointer truncate">
                  📂 模拟对公跨行转账汇款文本
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- 智能核对表单与手动修改区 -->
        <div class="bg-slate-950/5 rounded-2xl p-4 border border-slate-200/40 flex flex-col justify-between space-y-4">
          <div class="space-y-4">
            <div class="flex items-center justify-between">
              <span class="text-xs font-bold text-slate-500 uppercase tracking-wider block">
                <i class="fa-solid fa-file-invoice-dollar text-blue-600 mr-1"></i> 智能业务核对与录入表：
              </span>
              <div class="flex items-center space-x-2">
                <span class="text-xs font-bold text-slate-400">表单模板:</span>
                <select v-model="currentBizType" @change="switchBusinessFormLayout(currentBizType)"
                  class="bg-white border border-slate-200 rounded-lg text-sm font-bold px-2 py-1 focus:outline-none">
                  <option value="" disabled>请选择模板</option>
                  <option v-for="opt in bizTypeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                </select>
              </div>
            </div>

            <!-- 动态表单挂载区 -->
            <div class="text-slate-700 bg-white/80 border border-slate-200 p-3 rounded-xl min-h-[140px]">
              <div v-if="!currentBizType" class="text-slate-400 text-center py-10 font-bold text-xs">
                期待解析指令输入或选择表单模板...
              </div>
              <div v-else class="space-y-3 font-sans pt-1 text-sm">
                <!-- 通用基础要素栏 -->
                <div class="grid grid-cols-2 gap-2">
                  <div>
                    <label class="text-xs font-bold text-slate-400 block mb-1">客户姓名 (AI自动)</label>
                    <input type="text" v-model="baseInfo.userName" placeholder="[未提取] 等待AI或下方手填"
                      class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                  </div>
                  <div>
                    <label class="text-xs font-bold text-slate-400 block mb-1">身份证号 (AI自动)</label>
                    <input type="text" v-model="baseInfo.idCard" placeholder="[未提取] 等待AI或下方手填"
                      class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                  </div>
                  <div class="col-span-2">
                    <label class="text-xs font-bold text-slate-400 block mb-1">手机号码 (AI自动)</label>
                    <input type="text" v-model="baseInfo.phone" placeholder="[未提取] 等待AI或下方手填"
                      class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                  </div>
                </div>
                <div class="border-t border-slate-200/60 my-2"></div>

                <!-- 业务专项要素 -->
                <!-- 大额预约取现 -->
                <div v-if="currentBizType === 'cash_reserve'" :class="[bizTheme.cash_reserve.bg, 'p-3 rounded-2xl border', bizTheme.cash_reserve.border, 'space-y-2.5']">
                  <div :class="['text-[11px] font-black uppercase tracking-wider flex items-center', bizTheme.cash_reserve.iconColor]">
                    <i :class="[bizTheme.cash_reserve.icon, 'mr-1']"></i> {{ bizTheme.cash_reserve.title }}
                  </div>
                  <div class="grid grid-cols-2 gap-2">
                    <div>
                      <label class="text-xs font-bold text-slate-400 block mb-1">预约取现金额 (元)</label>
                      <input type="text" v-model="cashReserveFields.amount" placeholder="请输入取现金额"
                        class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                    </div>
                    <div>
                      <label class="text-xs font-bold text-slate-400 block mb-1">币种</label>
                      <select v-model="cashReserveFields.currency"
                        class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                        <option value="CNY">人民币 (CNY)</option>
                        <option value="USD">美元 (USD)</option>
                        <option value="HKD">港币 (HKD)</option>
                        <option value="EUR">欧元 (EUR)</option>
                      </select>
                    </div>
                    <div>
                      <label class="text-xs font-bold text-slate-400 block mb-1">预约领取日期</label>
                      <input type="date" v-model="cashReserveFields.date"
                        class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                    </div>
                    <div>
                      <label class="text-xs font-bold text-slate-400 block mb-1">预约领取时间段</label>
                      <input type="text" v-model="cashReserveFields.time" placeholder="如：上午 10:00-11:00"
                        class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                    </div>
                    <div class="col-span-2">
                      <label class="text-xs font-bold text-slate-400 block mb-1">预约提取网点</label>
                      <input type="text" v-model="cashReserveFields.branch" placeholder="请输入预约提取网点"
                        class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                    </div>
                    <div class="col-span-2">
                      <label class="text-xs font-bold text-slate-400 block mb-1">现金用途申报</label>
                      <select v-model="cashReserveFields.purpose"
                        class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                        <option value="" disabled>请选择现金用途</option>
                        <option value="purchase">购房/购车</option>
                        <option value="medical">医疗费用</option>
                        <option value="education">教育缴费</option>
                        <option value="business">经营周转</option>
                        <option value="travel">因私旅游</option>
                        <option value="other">其他</option>
                      </select>
                    </div>
                    <div>
                      <label class="text-xs font-bold text-slate-400 block mb-1">资金来源</label>
                      <select v-model="cashReserveFields.fundSource"
                        class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                        <option value="" disabled>请选择资金来源</option>
                        <option value="salary">工资收入</option>
                        <option value="saving">储蓄存款</option>
                        <option value="investment">投资收益</option>
                        <option value="business">经营收入</option>
                        <option value="inheritance">继承/赠与</option>
                        <option value="other">其他</option>
                      </select>
                    </div>
                    <div>
                      <label class="text-xs font-bold text-slate-400 block mb-1">面额偏好</label>
                      <select v-model="cashReserveFields.denomination"
                        class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                        <option value="100">100元</option>
                        <option value="50">50元</option>
                        <option value="mixed">混合面额</option>
                        <option value="large">大面额优先</option>
                      </select>
                    </div>
                  </div>
                </div>

                <!-- 新开卡/开户 -->
                <div v-else-if="currentBizType === 'open_card'" :class="[bizTheme.open_card.bg, 'p-3 rounded-2xl border', bizTheme.open_card.border, 'space-y-2.5']">
                  <div :class="['text-[11px] font-black uppercase tracking-wider flex items-center', bizTheme.open_card.iconColor]">
                    <i :class="[bizTheme.open_card.icon, 'mr-1']"></i> {{ bizTheme.open_card.title }}
                  </div>
                  <div class="grid grid-cols-2 gap-2">
                    <div class="col-span-2">
                      <label class="text-xs font-bold text-slate-400 block mb-1">拟申办卡片产品类型</label>
                      <select v-model="openCardFields.cardType"
                        class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                        <option value="debit_standard">工银标准借记卡 (Ⅰ类储蓄卡)</option>
                        <option value="debit_gold">工银多币种借记卡 (Ⅰ类国际卡)</option>
                        <option value="salary">特约代发工资卡 (免年费/小额账户费)</option>
                        <option value="platinum">工银白金信用卡 (额度申请型)</option>
                        <option value="student">工银校园青春卡 (学生专属)</option>
                        <option value="social">社保联名一卡通 (医保/社保)</option>
                      </select>
                    </div>
                    <div>
                      <label class="text-xs font-bold text-slate-400 block mb-1">账户类型</label>
                      <select v-model="openCardFields.accountType"
                        class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                        <option value="type1">Ⅰ类户 (全功能)</option>
                        <option value="type2">Ⅱ类户 (限制额度)</option>
                        <option value="type3">Ⅲ类户 (小额消费)</option>
                      </select>
                    </div>
                    <div>
                      <label class="text-xs font-bold text-slate-400 block mb-1">预存金额 (元)</label>
                      <input type="number" v-model="openCardFields.initialDeposit" placeholder="如：100"
                        class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                    </div>
                    <div>
                      <label class="text-xs font-bold text-slate-400 block mb-1">是否开通网上银行</label>
                      <select v-model="openCardFields.onlineBanking"
                        class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                        <option value="yes">是</option>
                        <option value="no">否</option>
                      </select>
                    </div>
                    <div>
                      <label class="text-xs font-bold text-slate-400 block mb-1">是否开通手机银行</label>
                      <select v-model="openCardFields.mobileBanking"
                        class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                        <option value="yes">是</option>
                        <option value="no">否</option>
                      </select>
                    </div>
                    <div>
                      <label class="text-xs font-bold text-slate-400 block mb-1">领卡方式</label>
                      <select v-model="openCardFields.deliveryMethod"
                        class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                        <option value="branch">网点自取</option>
                        <option value="mail">邮寄送达</option>
                      </select>
                    </div>
                    <div>
                      <label class="text-xs font-bold text-slate-400 block mb-1">紧急联系人姓名</label>
                      <input type="text" v-model="openCardFields.emergencyContact" placeholder="请输入紧急联系人姓名"
                        class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                    </div>
                    <div class="col-span-2">
                      <label class="text-xs font-bold text-slate-400 block mb-1">领卡网点 / 邮寄收件地址</label>
                      <input type="text" v-model="openCardFields.address" placeholder="请输入领卡网点或邮寄地址"
                        class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                    </div>
                  </div>
                </div>

                <!-- 对公跨行转账 -->
                <div v-else-if="currentBizType === 'corp_transfer'" :class="[bizTheme.corp_transfer.bg, 'p-3 rounded-2xl border', bizTheme.corp_transfer.border, 'space-y-2.5']">
                  <div :class="['text-[11px] font-black uppercase tracking-wider flex items-center', bizTheme.corp_transfer.iconColor]">
                    <i :class="[bizTheme.corp_transfer.icon, 'mr-1']"></i> {{ bizTheme.corp_transfer.title }}
                  </div>
                  <div class="grid grid-cols-2 gap-2">
                    <div>
                      <label class="text-xs font-bold text-slate-400 block mb-1">付款单位名称</label>
                      <input type="text" v-model="corpTransferFields.payerName" placeholder="请输入付款单位全称"
                        class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                    </div>
                    <div>
                      <label class="text-xs font-bold text-slate-400 block mb-1">付款账号</label>
                      <input type="text" v-model="corpTransferFields.payerAccount" placeholder="请输入付款账号"
                        class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                    </div>
                    <div>
                      <label class="text-xs font-bold text-slate-400 block mb-1">收款单位名称</label>
                      <input type="text" v-model="corpTransferFields.recvName" placeholder="请输入收款单位全称"
                        class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                    </div>
                    <div>
                      <label class="text-xs font-bold text-slate-400 block mb-1">收款账号</label>
                      <input type="text" v-model="corpTransferFields.recvCard" placeholder="请输入收款账号"
                        class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                    </div>
                    <div class="col-span-2">
                      <label class="text-xs font-bold text-slate-400 block mb-1">收款银行名称</label>
                      <input type="text" v-model="corpTransferFields.recvBank" placeholder="请输入收款银行全称及支行"
                        class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                    </div>
                    <div>
                      <label class="text-xs font-bold text-slate-400 block mb-1">转账金额 (元)</label>
                      <input type="text" v-model="corpTransferFields.amount" placeholder="请输入转账金额"
                        class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                    </div>
                    <div>
                      <label class="text-xs font-bold text-slate-400 block mb-1">是否加急</label>
                      <select v-model="corpTransferFields.urgent"
                        class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                        <option value="no">普通汇款</option>
                        <option value="yes">加急汇款</option>
                      </select>
                    </div>
                    <div class="col-span-2">
                      <label class="text-xs font-bold text-slate-400 block mb-1">资金用途</label>
                      <input type="text" v-model="corpTransferFields.purpose" placeholder="如：货款、服务费、保证金等"
                        class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                    </div>
                  </div>
                </div>

                <!-- 对公现金缴款 -->
                <div v-else-if="currentBizType === 'cash_deposit'" :class="[bizTheme.cash_deposit.bg, 'p-3 rounded-2xl border', bizTheme.cash_deposit.border, 'space-y-2.5']">
                  <div :class="['text-[11px] font-black uppercase tracking-wider flex items-center', bizTheme.cash_deposit.iconColor]">
                    <i :class="[bizTheme.cash_deposit.icon, 'mr-1']"></i> {{ bizTheme.cash_deposit.title }}
                  </div>
                  <div class="grid grid-cols-2 gap-2">
                    <div>
                      <label class="text-xs font-bold text-slate-400 block mb-1">缴款单位名称</label>
                      <input type="text" v-model="cashDepositFields.payerName" placeholder="请输入缴款单位全称"
                        class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                    </div>
                    <div>
                      <label class="text-xs font-bold text-slate-400 block mb-1">缴款账号</label>
                      <input type="text" v-model="cashDepositFields.payerAccount" placeholder="请输入单位账号"
                        class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                    </div>
                    <div>
                      <label class="text-xs font-bold text-slate-400 block mb-1">缴款金额 (元)</label>
                      <input type="text" v-model="cashDepositFields.amount" placeholder="请输入缴款金额"
                        class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                    </div>
                    <div>
                      <label class="text-xs font-bold text-slate-400 block mb-1">缴款方式</label>
                      <select v-model="cashDepositFields.depositMethod"
                        class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                        <option value="cash">现金缴款</option>
                        <option value="check">转账支票</option>
                        <option value="draft">银行汇票</option>
                      </select>
                    </div>
                    <div class="col-span-2">
                      <label class="text-xs font-bold text-slate-400 block mb-1">款项来源/用途说明</label>
                      <input type="text" v-model="cashDepositFields.purpose" placeholder="如：营业款、税款、保证金等"
                        class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                    </div>
                    <div class="col-span-2">
                      <label class="text-xs font-bold text-slate-400 block mb-1">预约办理网点</label>
                      <input type="text" v-model="cashDepositFields.branch" placeholder="请输入预约办理网点"
                        class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                    </div>
                  </div>
                </div>

                <!-- 外币兑换 -->
                <div v-else-if="currentBizType === 'fx_exchange'" :class="[bizTheme.fx_exchange.bg, 'p-3 rounded-2xl border', bizTheme.fx_exchange.border, 'space-y-2.5']">
                  <div :class="['text-[11px] font-black uppercase tracking-wider flex items-center', bizTheme.fx_exchange.iconColor]">
                    <i :class="[bizTheme.fx_exchange.icon, 'mr-1']"></i> {{ bizTheme.fx_exchange.title }}
                  </div>
                  <div class="grid grid-cols-2 gap-2">
                    <div>
                      <label class="text-xs font-bold text-slate-400 block mb-1">兑换币种</label>
                      <select v-model="fxExchangeFields.fxCurrency"
                        class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                        <option value="usd">美元 (USD)</option>
                        <option value="eur">欧元 (EUR)</option>
                        <option value="gbp">英镑 (GBP)</option>
                        <option value="jpy">日元 (JPY)</option>
                        <option value="hkd">港币 (HKD)</option>
                        <option value="aud">澳元 (AUD)</option>
                        <option value="cad">加元 (CAD)</option>
                      </select>
                    </div>
                    <div>
                      <label class="text-xs font-bold text-slate-400 block mb-1">兑换方向</label>
                      <select v-model="fxExchangeFields.fxDirection"
                        class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                        <option value="buy">购汇（人民币→外币）</option>
                        <option value="sell">结汇（外币→人民币）</option>
                      </select>
                    </div>
                    <div>
                      <label class="text-xs font-bold text-slate-400 block mb-1">外币金额</label>
                      <input type="text" v-model="fxExchangeFields.fxAmount" placeholder="请输入外币金额"
                        class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                    </div>
                    <div>
                      <label class="text-xs font-bold text-slate-400 block mb-1">折合人民币 (元)</label>
                      <input type="number" v-model="fxExchangeFields.amount" placeholder="自动估算或手动输入"
                        class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                    </div>
                    <div class="col-span-2">
                      <label class="text-xs font-bold text-slate-400 block mb-1">用汇用途</label>
                      <select v-model="fxExchangeFields.purpose"
                        class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                        <option value="" disabled>请选择用途</option>
                        <option value="travel">因私旅游</option>
                        <option value="business">因公出差</option>
                        <option value="study">留学缴费</option>
                        <option value="medical">境外就医</option>
                        <option value="trade">货物贸易</option>
                        <option value="other">其他</option>
                      </select>
                    </div>
                    <div>
                      <label class="text-xs font-bold text-slate-400 block mb-1">预约领取时间</label>
                      <input type="text" v-model="fxExchangeFields.time" placeholder="如：明日上午 10:00"
                        class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                    </div>
                    <div>
                      <label class="text-xs font-bold text-slate-400 block mb-1">预约办理网点</label>
                      <input type="text" v-model="fxExchangeFields.branch" placeholder="请输入预约办理网点"
                        class="w-full bg-white border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 transition-all shadow-xs text-slate-800">
                    </div>
                  </div>
                </div>

                <!-- 电子签名/签章 -->
                <div v-if="currentBizType" class="bg-slate-50/60 p-3 rounded-2xl border border-slate-200/70 space-y-2.5">
                  <div class="text-xs font-black text-slate-600 uppercase tracking-wider flex items-center">
                    <i class="fa-solid fa-signature mr-1"></i> 电子签名确认
                  </div>

                  <!-- 对公业务：经办人 + 授权人 + 单位公章 + 财务专用章 -->
                  <template v-if="isCorporate">
                    <div class="grid grid-cols-2 gap-2">
                      <div>
                        <label class="text-xs font-bold text-slate-400 block mb-1">经办人签名</label>
                        <div v-if="!signatureState.operatorSigned"
                          @click="openSignaturePad('operatorSigned')"
                          class="bg-white border-2 border-dashed border-slate-300 rounded-xl h-20 flex items-center justify-center cursor-pointer hover:border-cyan-400 transition-all">
                          <span class="text-slate-400 text-xs"><i class="fa-solid fa-hand-pointer mr-1"></i>点击签名</span>
                        </div>
                        <div v-else class="bg-white border-2 border-cyan-400 rounded-xl h-20 flex items-center justify-center overflow-hidden">
                          <img :src="signatureState.operatorSigned" class="h-full w-full object-contain" alt="签名">
                        </div>
                      </div>
                      <div>
                        <label class="text-xs font-bold text-slate-400 block mb-1">授权人签名</label>
                        <div v-if="!signatureState.authorizerSigned"
                          @click="openSignaturePad('authorizerSigned')"
                          class="bg-white border-2 border-dashed border-slate-300 rounded-xl h-20 flex items-center justify-center cursor-pointer hover:border-cyan-400 transition-all">
                          <span class="text-slate-400 text-xs"><i class="fa-solid fa-hand-pointer mr-1"></i>点击签名</span>
                        </div>
                        <div v-else class="bg-white border-2 border-cyan-400 rounded-xl h-20 flex items-center justify-center overflow-hidden">
                          <img :src="signatureState.authorizerSigned" class="h-full w-full object-contain" alt="签名">
                        </div>
                      </div>
                    </div>
                    <div class="border-t border-slate-200/60 my-1"></div>
                    <div class="text-xs font-black text-red-500 uppercase tracking-wider flex items-center">
                      <i class="fa-solid fa-stamp mr-1"></i> 电子签章确认
                    </div>
                    <div class="grid grid-cols-2 gap-2">
                      <div>
                        <label class="text-xs font-bold text-slate-400 block mb-1">单位公章</label>
                        <div v-if="!signatureState.corpSeal"
                          @click="stampSeal('corpSeal')"
                          class="bg-white border-2 border-dashed border-red-300 rounded-xl h-20 flex items-center justify-center cursor-pointer hover:border-red-400 transition-all">
                          <span class="text-slate-400 text-xs"><i class="fa-solid fa-stamp mr-1"></i>点击盖章</span>
                        </div>
                        <div v-else class="bg-white border-2 border-red-400 rounded-xl h-20 flex items-center justify-center overflow-hidden">
                          <img :src="signatureState.corpSeal" class="h-full w-full object-contain" alt="电子公章">
                        </div>
                      </div>
                      <div>
                        <label class="text-xs font-bold text-slate-400 block mb-1">财务专用章</label>
                        <div v-if="!signatureState.financeSeal"
                          @click="stampSeal('financeSeal')"
                          class="bg-white border-2 border-dashed border-red-300 rounded-xl h-20 flex items-center justify-center cursor-pointer hover:border-red-400 transition-all">
                          <span class="text-slate-400 text-xs"><i class="fa-solid fa-stamp mr-1"></i>点击盖章</span>
                        </div>
                        <div v-else class="bg-white border-2 border-red-400 rounded-xl h-20 flex items-center justify-center overflow-hidden">
                          <img :src="signatureState.financeSeal" class="h-full w-full object-contain" alt="电子公章">
                        </div>
                      </div>
                    </div>
                    <div class="text-[10px] text-slate-400">经办人及授权人确认以上信息真实有效，加盖单位公章及财务专用章后生效</div>
                  </template>

                  <!-- 个人业务：单签名 -->
                  <template v-else>
                    <div v-if="!signatureState.personalSigned"
                      @click="openSignaturePad('personalSigned')"
                      class="bg-white border-2 border-dashed border-slate-300 rounded-xl h-24 flex items-center justify-center cursor-pointer hover:border-cyan-400 transition-all">
                      <span class="text-slate-400 text-sm"><i class="fa-solid fa-hand-pointer mr-1"></i> 点击此处进行电子签名</span>
                    </div>
                    <div v-else class="bg-white border-2 border-cyan-400 rounded-xl h-24 flex items-center justify-center relative overflow-hidden">
                      <img :src="signatureState.personalSigned" class="h-full w-full object-contain" alt="签名">
                      <span class="absolute bottom-1 right-2 text-[10px] text-slate-400">签名时间：{{ new Date().toLocaleString('zh-CN') }}</span>
                    </div>
                    <div class="text-[10px] text-slate-400">本人确认以上信息真实有效，自愿办理此业务</div>
                  </template>
                </div>
              </div>
            </div>

            <!-- 线下人工直填与核验卡片 -->
            <div class="bg-white/90 p-3.5 rounded-xl border border-slate-200 shadow-xs space-y-3 font-sans">
              <div class="text-sm font-black text-slate-600 uppercase tracking-wider flex items-center justify-between border-b border-slate-100 pb-2">
                <span><i class="fa-solid fa-pen-to-square text-cyan-600 mr-1"></i> 线下业务直填与证件核验（手动录入）</span>
                <span class="text-xs bg-slate-100 text-slate-500 px-2 py-0.5 rounded font-bold">待填状态</span>
              </div>

              <div class="grid grid-cols-2 gap-3">
                <div>
                  <label class="text-xs font-bold text-slate-400 block mb-1">客户姓名 (手动)</label>
                  <input type="text" v-model="manualInfo.userName" placeholder="[未填写] 请录入姓名"
                    class="w-full bg-slate-50 border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 focus:bg-white transition-all text-slate-800 placeholder:text-slate-300">
                </div>
                <div>
                  <label class="text-xs font-bold text-slate-400 block mb-1">身份证号 (手动)</label>
                  <input type="text" v-model="manualInfo.idCard" placeholder="[未填写] 请录入18位证件号"
                    class="w-full bg-slate-50 border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 focus:bg-white transition-all text-slate-800 placeholder:text-slate-300">
                </div>
                <div class="col-span-2">
                  <label class="text-xs font-bold text-slate-400 block mb-1">手机号码 (手动)</label>
                  <input type="text" v-model="manualInfo.phone" placeholder="[未填写] 请录入11位手机号"
                    class="w-full bg-slate-50 border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 focus:bg-white transition-all text-slate-800 placeholder:text-slate-300">
                </div>
              </div>

              <!-- 身份证影像上传 -->
              <div>
                <label class="text-xs font-bold text-slate-400 block mb-1.5">📷 线下核验必备：身份证影像上传</label>
                <div class="grid grid-cols-2 gap-3">
                  <label class="border border-dashed border-slate-200 bg-slate-50/50 hover:bg-slate-100 transition-all rounded-lg p-3 flex flex-col items-center justify-center gap-1 cursor-pointer min-h-[64px] relative group">
                    <input type="file" accept="image/*" class="hidden" @change="manualInfo.frontUploaded = true">
                    <i class="fa-solid fa-camera text-slate-400 group-hover:text-cyan-600 text-sm"></i>
                    <span :class="['text-xs font-bold', manualInfo.frontUploaded ? 'text-cyan-600' : 'text-slate-400']">
                      {{ manualInfo.frontUploaded ? '✅ 已挂载(正面)' : '[未上传人像面]' }}
                    </span>
                  </label>
                  <label class="border border-dashed border-slate-200 bg-slate-50/50 hover:bg-slate-100 transition-all rounded-lg p-3 flex flex-col items-center justify-center gap-1 cursor-pointer min-h-[64px] relative group">
                    <input type="file" accept="image/*" class="hidden" @change="manualInfo.backUploaded = true">
                    <i class="fa-solid fa-camera text-slate-400 group-hover:text-cyan-600 text-sm"></i>
                    <span :class="['text-xs font-bold', manualInfo.backUploaded ? 'text-cyan-600' : 'text-slate-400']">
                      {{ manualInfo.backUploaded ? '✅ 已挂载(国徽面)' : '[未上传国徽面]' }}
                    </span>
                  </label>
                </div>
              </div>

              <!-- 个性化备注 -->
              <div>
                <label class="text-xs font-bold text-slate-400 block mb-1">个性化特殊备注（随直通码一并打包）</label>
                <textarea v-model="manualInfo.notes" placeholder="[无特殊备注] 可手动敲入额外需求..."
                  class="w-full bg-slate-50 border border-slate-200 text-sm font-bold rounded-lg px-3 py-2 focus:outline-none focus:border-cyan-500 focus:bg-white transition-all text-slate-800 resize-none h-14 placeholder:text-slate-300"></textarea>
              </div>
            </div>
          </div>

          <!-- 底部状态与操作动作条 -->
          <div class="mt-2 border-t border-slate-200/60 pt-3">
            <div :class="['text-sm font-bold mb-2 flex items-center gap-1', statusColor]">
              <i :class="statusIcon"></i>
              <span>{{ statusText }}</span>
            </div>

            <button @click="generateQRCode"
              class="w-full bg-blue-600 hover:bg-blue-700 text-white font-bold py-3 rounded-xl text-sm tracking-wider transition-all flex items-center justify-center gap-1 shadow-md shadow-blue-200 cursor-pointer">
              <span>确认表单信息，生成业务直通码</span>
              <i class="fa-solid fa-qrcode"></i>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- ================= 我的预填单 ================= -->
    <div class="bg-white/50 backdrop-blur-2xl p-4 rounded-2xl border border-slate-200/60">
      <div class="flex items-center justify-between mb-3">
        <div class="flex items-center gap-2">
          <i class="fa-solid fa-file-lines text-indigo-500"></i>
          <h3 class="text-sm font-black text-slate-800">我的预填单</h3>
          <span class="text-[10px] font-bold text-slate-400">共 {{ preFormList.length }} 条</span>
        </div>
        <button @click="loadMyPreForms()"
          class="flex items-center gap-1.5 text-xs font-bold text-slate-600 hover:text-blue-600 bg-slate-100 hover:bg-blue-50 px-3 py-1.5 rounded-lg border border-slate-200 transition-all cursor-pointer">
          <i class="fa-solid fa-rotate text-sm"></i><span>刷新</span>
        </button>
      </div>

      <div v-if="preFormList.length === 0" class="text-center py-6">
        <i class="fa-solid fa-inbox text-2xl text-slate-300 mb-2"></i>
        <p class="text-xs font-bold text-slate-400">暂无预填单记录</p>
      </div>

      <div v-else class="space-y-2">
        <div v-for="p in preFormList" :key="p.id"
          class="flex items-center justify-between bg-white/70 px-3 py-2 rounded-xl border border-slate-200/60">
          <div class="min-w-0 flex-1">
            <div class="flex items-center gap-2">
              <span class="text-xs font-black text-slate-800">
                {{ bizTypeNameMap[p.businessType] || p.businessType }}
              </span>
              <span class="px-1.5 py-0.5 rounded text-[9px] font-bold bg-slate-100 text-slate-500">
                {{ p.status === 'DRAFT' ? '草稿' : p.status }}
              </span>
            </div>
            <div class="text-[10px] text-slate-400 truncate mt-0.5">{{ p.rawText }}</div>
          </div>
          <div class="text-[10px] font-bold text-slate-400 shrink-0 ml-3">{{ p.createdAt }}</div>
        </div>
      </div>
    </div>

    <!-- ================= AI 材料预检弹窗 ================= -->
    <Teleport to="body">
      <div v-if="matModalOpen"
        class="fixed inset-0 bg-black/50 backdrop-blur-sm z-[9999] flex items-center justify-center p-4">
        <div class="bg-white rounded-3xl w-full max-w-sm shadow-2xl border border-slate-200/80 overflow-hidden flex flex-col">
          <div class="bg-gradient-to-r from-indigo-500 via-purple-500 to-pink-500 p-4 shrink-0">
            <div class="flex items-center justify-between">
              <div class="flex items-center space-x-2">
                <div class="w-8 h-8 rounded-lg bg-white/20 flex items-center justify-center text-white shrink-0">
                  <i class="fa-solid fa-wand-magic-sparkles text-sm"></i>
                </div>
                <div>
                  <span class="text-sm font-black text-white block">AI 材料预检</span>
                  <span class="text-[10px] text-white/80 font-bold">基于您的业务类型智能推荐</span>
                </div>
              </div>
              <button @click="closeMatModal"
                class="w-8 h-8 rounded-lg bg-white/20 flex items-center justify-center text-white hover:bg-white/30 transition-all cursor-pointer">
                <i class="fa-solid fa-xmark text-sm"></i>
              </button>
            </div>
          </div>
          <div class="p-5 space-y-3 overflow-y-auto flex-1">
            <div class="text-center">
              <span class="inline-block text-xs font-black text-indigo-600 bg-indigo-50 px-3 py-1 rounded-full border border-indigo-200">{{ matBizName }}</span>
            </div>
            <div class="bg-amber-50 rounded-xl p-3 border border-amber-200/60 flex items-start gap-2">
              <i class="fa-solid fa-triangle-exclamation text-amber-500 text-sm mt-0.5"></i>
              <p class="text-[11px] font-bold text-amber-700 leading-relaxed">请确认已携带以下材料，缺件将无法办理，需重新排队</p>
            </div>
            <div class="space-y-2">
              <div v-for="(m, i) in matList" :key="i"
                class="flex items-center gap-2.5 bg-slate-50 rounded-xl p-2.5 border border-slate-100">
                <div class="w-8 h-8 rounded-lg bg-white flex items-center justify-center text-indigo-500 border border-slate-200 shrink-0">
                  <i :class="'fa-solid ' + m.icon + ' text-xs'"></i>
                </div>
                <span class="text-xs font-bold text-slate-700 flex-1">{{ m.name }}</span>
                <span v-if="m.required" class="text-[8px] font-black bg-rose-100 text-rose-600 px-1.5 py-0.5 rounded-full border border-rose-200">必需</span>
                <span v-else class="text-[8px] font-black bg-slate-100 text-slate-500 px-1.5 py-0.5 rounded-full border border-slate-200">视情况</span>
              </div>
            </div>
            <div class="bg-emerald-50 rounded-xl p-3 border border-emerald-200/60 flex items-start gap-2">
              <i class="fa-solid fa-circle-check text-emerald-500 text-sm mt-0.5"></i>
              <p class="text-[11px] font-bold text-emerald-700 leading-relaxed">材料齐全可享"免填单"直通服务，柜面扫码即办</p>
            </div>
          </div>
          <div class="border-t border-slate-200/60 p-3 shrink-0 bg-white space-y-2">
            <button @click="proceedGenerateQR"
              class="w-full px-4 py-2.5 text-xs font-bold text-white bg-gradient-to-r from-indigo-500 to-purple-500 hover:from-indigo-600 hover:to-purple-600 rounded-xl transition-all cursor-pointer flex items-center justify-center gap-1.5">
              <i class="fa-solid fa-qrcode"></i> 材料已备齐，生成直通码
            </button>
            <button @click="closeMatModal"
              class="w-full px-4 py-2 text-[11px] font-bold text-slate-500 hover:text-slate-700 transition-all cursor-pointer">
              稍后准备，先不生成
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ================= 手写签名弹窗 ================= -->
    <Teleport to="body">
      <div v-if="sigModalOpen"
        class="fixed inset-0 bg-black/50 backdrop-blur-sm z-[9999] flex items-center justify-center p-4">
        <div class="bg-white rounded-3xl w-full max-w-lg shadow-2xl border border-slate-200/80 overflow-hidden">
          <div class="bg-gradient-to-r from-cyan-500 to-blue-600 p-4">
            <div class="flex items-center justify-between">
              <h3 class="text-white font-bold text-lg flex items-center gap-2">
                <i class="fa-solid fa-signature"></i> 手写签名
              </h3>
              <button @click="closeSignaturePad"
                class="text-white/80 hover:text-white text-xl w-8 h-8 flex items-center justify-center rounded-full hover:bg-white/20 transition-all cursor-pointer">
                <i class="fa-solid fa-xmark"></i>
              </button>
            </div>
          </div>
          <div class="p-5 space-y-3">
            <p class="text-xs text-slate-500 text-center">请在下方区域手写签名，签名后点击"确认签名"</p>
            <div class="relative bg-slate-50 border-2 border-dashed border-slate-300 rounded-2xl overflow-hidden">
              <canvas id="signature-canvas" width="480" height="200" class="w-full touch-none cursor-crosshair"
                style="background: repeating-linear-gradient(45deg, #f8fafc, #f8fafc 10px, #f1f5f9 10px, #f1f5f9 20px);"
                @mousedown="onCanvasMouseDown"
                @mousemove="onCanvasMouseMove"
                @mouseup="sigStopDraw()"
                @mouseleave="sigStopDraw()"
                @touchstart="onCanvasTouchStart"
                @touchmove="onCanvasTouchMove"
                @touchend="onCanvasTouchEnd"></canvas>
              <div v-if="!sigHasContent" class="absolute inset-0 flex items-center justify-center pointer-events-none">
                <span class="text-slate-300 text-sm"><i class="fa-solid fa-pen mr-1"></i> 在此区域手写签名</span>
              </div>
            </div>
            <div class="flex items-center justify-between gap-2 pt-1">
              <button @click="clearSignature"
                class="px-4 py-2 text-sm font-bold text-slate-600 bg-slate-100 hover:bg-slate-200 rounded-xl transition-all flex items-center gap-1 cursor-pointer">
                <i class="fa-solid fa-eraser"></i> 清除重签
              </button>
              <div class="flex gap-2">
                <button @click="closeSignaturePad"
                  class="px-4 py-2 text-sm font-bold text-slate-500 bg-white border border-slate-200 hover:bg-slate-50 rounded-xl transition-all cursor-pointer">
                  取消
                </button>
                <button @click="confirmSignature"
                  class="px-6 py-2 text-sm font-bold text-white bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-600 hover:to-blue-700 rounded-xl transition-all shadow-lg flex items-center gap-1 cursor-pointer">
                  <i class="fa-solid fa-check"></i> 确认签名
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
