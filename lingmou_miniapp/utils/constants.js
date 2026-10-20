// utils/constants.js - 常量定义（与前端保持一致）

// 业务类型
const BIZ_TYPES = [
  { value: 'cash_reserve', label: '大额预约取现单' },
  { value: 'open_card', label: '新开卡/开户申请单' },
  { value: 'corp_transfer', label: '对公跨行转账汇款' },
  { value: 'cash_deposit', label: '对公现金缴款' },
  { value: 'fx_exchange', label: '外币兑换' },
]

const BIZ_TYPE_NAMES = {
  cash_reserve: '大额取现预约',
  open_card: '办卡开户',
  corp_transfer: '对公跨行转账汇款',
  cash_deposit: '对公现金缴款',
  fx_exchange: '外币兑换',
}

// 网点数据（与前端 branch store 保持一致）
const BRANCHES = [
  {
    id: 'b1', backendId: 1, name: '北京分行营业部',
    address: '北京市西城区复兴门内大街28号',
    distance: 2.5, status: 'busy', waitTime: 25, busyFactor: 1.8,
    services: ['cash_reserve', 'corp_transfer', 'fx_exchange'],
    trend: [12, 15, 18, 22, 25, 28, 25, 20],
  },
  {
    id: 'b2', backendId: 2, name: '中关村科技园区支行',
    address: '北京市海淀区中关村大街1号',
    distance: 3.8, status: 'moderate', waitTime: 12, busyFactor: 1.2,
    services: ['open_card', 'cash_reserve'],
    trend: [8, 10, 12, 14, 12, 10, 8, 6],
  },
  {
    id: 'b3', backendId: 3, name: '国贸CBD中心支行',
    address: '北京市朝阳区建国门外大街1号',
    distance: 5.2, status: 'moderate', waitTime: 15, busyFactor: 1.3,
    services: ['corp_transfer', 'cash_deposit', 'fx_exchange'],
    trend: [10, 12, 15, 18, 15, 12, 10, 8],
  },
  {
    id: 'b4', backendId: 4, name: '三里屯国际社区支行',
    address: '北京市朝阳区三里屯路19号',
    distance: 6.1, status: 'free', waitTime: 5, busyFactor: 0.6,
    services: ['open_card', 'fx_exchange'],
    trend: [5, 4, 3, 5, 6, 4, 3, 2],
  },
  {
    id: 'b5', backendId: 5, name: '金融街私人银行旗舰支行',
    address: '北京市西城区金融大街7号',
    distance: 2.1, status: 'free', waitTime: 3, busyFactor: 0.4,
    services: ['cash_reserve', 'corp_transfer', 'fx_exchange', 'cash_deposit'],
    trend: [3, 2, 1, 2, 3, 2, 1, 1],
  },
  {
    id: 'b6', backendId: 6, name: '望京SOHO社区支行',
    address: '北京市朝阳区望京街8号',
    distance: 8.5, status: 'free', waitTime: 5, busyFactor: 0.5,
    services: ['open_card', 'cash_reserve'],
    trend: [4, 3, 2, 3, 5, 3, 2, 2],
  },
]

// 状态映射（与前端 appointment store 保持一致）
const STATUS_MAP = {
  COMPLETED: 'completed',
  EXPIRED: 'expired',
  VIRTUAL: 'virtual',
  ACTIVE: 'active',
  CALLED: 'active',
  PROCESSING: 'active',
  CANCELED: 'canceled',
}

// 状态显示文案
const STATUS_TEXT = {
  virtual: '虚拟号',
  active: '激活号',
  completed: '已完成',
  canceled: '已取消',
  expired: '已失效',
}

// 状态徽章class
const STATUS_BADGE = {
  virtual: 'badge-virtual',
  active: 'badge-active',
  completed: 'badge-completed',
  canceled: 'badge-canceled',
  expired: 'badge-expired',
}

// 进度步骤
const PROGRESS_STEPS = [
  { name: '取号', icon: '\ue800' },
  { name: '排队中', icon: '\ue801' },
  { name: '叫号', icon: '\ue802' },
  { name: '办理中', icon: '\ue803' },
  { name: '完成', icon: '\ue804' },
]

// AI本地知识库（与前端 getAIReply 保持一致）
function getAIReply(query) {
  let responseText = '您好，我是灵枢AI数字人大堂经理。我可以为您推荐网点、查询排队、指导业务办理。请告诉我您想办理什么业务？'

  if (/网点|人少|附近|哪个/.test(query)) {
    responseText = '【AI网点推荐】根据实时客流分析，目前金融街私人银行旗舰支行客流最少（畅通状态），AI预测办理时长仅约6分钟，距您2.1km。其次是望京SOHO社区支行，同样畅通，预计5分钟。建议您优先选择客流畅通的网点，点击"智能预约排队"即可查看详情并预约取号。'
  } else if (/外汇|汇款|跨境|国外/.test(query)) {
    responseText = '【外汇业务指引】跨境汇款需分两步办理：①购汇（人民币兑外币，约10分钟）②跨境汇款（填写收款信息，约20分钟）。所需材料：身份证、银行卡、收款人姓名及地址、收款银行SWIFT代码、汇款用途说明。推荐前往国贸CBD中心支行或北京分行营业部，均支持外汇业务。'
  } else if (/材料|带什么|证件/.test(query)) {
    responseText = '【业务材料清单】不同业务所需材料不同：①开户办卡：身份证+手机号 ②大额取现：身份证+银行卡（须预约）③跨境汇款：身份证+收款人信息+SWIFT代码 ④理财咨询：身份证+银行卡 ⑤挂失补卡：身份证+手机号。建议办理前确认材料齐全，避免白跑一趟。'
  } else if (/排队|等多久|等待/.test(query)) {
    responseText = '【实时排队查询】目前各网点等待情况：北京分行营业部约25分钟（繁忙）、长安街支行约8分钟（适中）、金融街支行约3分钟（畅通）、中关村支行约12分钟（适中）。建议选择畅通网点预约取号，到场扫码激活后享优先叫号，无需久等。'
  } else if (/预约|取号/.test(query)) {
    responseText = '【预约取号指引】线上预约流程：①点击"智能预约排队"选择网点 ②选择日期和时段（每半小时放10个号） ③选择业务类型 ④确认后生成虚拟号（灰色） ⑤到店扫码激活为预约号（红色）享优先叫号。注意：未按时到店激活将自动失效并扣除信誉分10分。'
  } else if (/开户|办卡|新卡/.test(query)) {
    responseText = '【开户办卡指引】个人开户需携带身份证原件和手机号，办理时长约15分钟。流程：①填写开户申请（5分钟）②柜面核验制卡（10分钟）。推荐前往长安街智慧示范支行，支持自助发卡机快速办理，AI预测仅约4分钟。点击"智能预约排队"即可预约。'
  } else if (/取现|现金|取钱/.test(query)) {
    responseText = '【大额取现指引】大额现金提取须提前预约，办理时长约15分钟。所需材料：身份证+银行卡。流程：①线上预约提现金额和时段 ②到网点柜面核验取款。推荐北京分行营业部，设有大额现金专柜。建议避开上午高峰（10:00-11:00），下午14:00后客流较少。'
  } else if (/挂失|丢了|补卡/.test(query)) {
    responseText = '【挂失补卡指引】银行卡遗失请尽快挂失，办理时长约13分钟。流程：①紧急挂失冻结卡片（3分钟）②补办新卡（10分钟）。所需材料：身份证+手机号+挂失手续费。推荐长安街智慧示范支行，支持自助发卡机快速补办。建议立即预约以免资金风险。'
  } else if (/密码|忘记|重置/.test(query)) {
    responseText = '【密码重置指引】银行卡密码重置需到柜面办理，时长约8分钟。所需材料：身份证+银行卡+手机验证码。流程：①身份核验（3分钟）②密码重置（5分钟）。推荐前往设有VTM自助终端的网点，如长安街智慧示范支行，可快速办理。'
  } else if (/你好|hi|hello|您好/.test(query)) {
    responseText = '您好！我是灵枢AI数字人大堂经理，很高兴为您服务。我可以为您推荐最合适的网点、查询实时排队情况、指导业务办理流程和所需材料。请问您今天想办理什么业务？'
  } else if (/帮助|功能|能做/.test(query)) {
    responseText = '【大堂经理服务清单】我可以为您提供：①智能网点推荐（按客流+距离+业务匹配）②实时排队查询 ③业务办理流程指引 ④所需材料清单 ⑤预约取号指导 ⑥办理进度追踪。您也可点击"AI助手"按钮，输入自然语言快速获取业务指引。'
  } else if (/谢谢|感谢/.test(query)) {
    responseText = '不用客气！很高兴能帮到您。如果您在办理业务过程中有任何疑问，随时可以来找我。祝您办理顺利！'
  }

  return responseText
}

// 思维链步骤（与前端 getThoughtSteps 保持一致）
function getThoughtSteps(query) {
  if (/配置|优化|理财/.test(query)) {
    return [
      '正在穿透隔离级别账户数据安全舱...',
      '正在提取资产全景：储蓄 17.6%、基金 31.6%、理财 50.8%...',
      '正在拉取 2026 年最新一季工银天天盈与权益基金波动率矩阵...',
      '正在计算最优风险对冲系数，生成资产组合再平衡策略...',
    ]
  } else if (/风险|风控|安全/.test(query)) {
    return [
      '正在调取端侧国密安全审计模块...',
      '正在扫描当前连接指纹与异常设备签名...',
      '正在对最近三笔大额流水实施区块链可信哈希二次校验...',
      '确认无封控隐患，开始合成可信风控报告摘要...',
    ]
  }
  return [
    '正在穿透隔离级别账户数据安全舱...',
    '正在唤醒端侧轻量化 NLP 实体识别解析内核...',
    '正在读取多因子资产配置雷达与风控准入模型...',
    '正在拟合决策树，准备输出安全应答...',
  ]
}

// 本地热力图兜底算法（与前端 buildHeatmap 保持一致）
function buildHeatmap() {
  // 6行×8列 = 48个网格，值 10-100
  const grid = []
  const baseline = [20, 35, 55, 75, 85, 70, 45, 25] // 8个时段基准
  for (let i = 0; i < 6; i++) {
    const row = []
    for (let j = 0; j < 8; j++) {
      const delta = ((i * 7 + j * 13) % 11) - 5
      const val = Math.max(10, Math.min(100, baseline[j] + delta + (i % 3) * 3))
      row.push(val)
    }
    grid.push(row)
  }
  return grid
}

// 时间标签
const TIME_LABELS = ['09:00', '09:30', '10:00', '10:30', '11:00', '11:30', '12:00', '12:30']

// 网点行标签
const BRANCH_LABELS = ['北京分行', '中关村', '国贸CBD', '三里屯', '金融街', '望京SOHO']

module.exports = {
  BIZ_TYPES,
  BIZ_TYPE_NAMES,
  BRANCHES,
  STATUS_MAP,
  STATUS_TEXT,
  STATUS_BADGE,
  PROGRESS_STEPS,
  getAIReply,
  getThoughtSteps,
  buildHeatmap,
  TIME_LABELS,
  BRANCH_LABELS,
}
