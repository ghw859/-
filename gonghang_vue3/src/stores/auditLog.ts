import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export type AuditStatus = '已提交' | '已完成' | '已取消'

export interface AuditExtraData {
  amount?: string
  currency?: string
  date?: string
  time?: string
  branch?: string
  purpose?: string
  fundSource?: string
  denomination?: string
  cardType?: string
  accountType?: string
  initialDeposit?: string
  onlineBanking?: string
  mobileBanking?: string
  deliveryMethod?: string
  emergencyContact?: string
  address?: string
  payerName?: string
  payerAccount?: string
  recvName?: string
  recvCard?: string
  recvBank?: string
  urgent?: string
  depositMethod?: string
  fxCurrency?: string
  fxDirection?: string
  fxAmount?: string
}

export interface AuditLog {
  id: number
  sn: string                // 流水号
  bizType: BizType
  bizTypeName: string
  userName: string
  idCardMasked: string      // 身份证号脱敏显示
  phoneMasked: string       // 手机号脱敏显示
  extraData: AuditExtraData
  timestamp: string         // 创建时间（本地字符串）
  hash: string              // 存证凭证哈希
  status: AuditStatus
}

export type BizType = 'cash_reserve' | 'open_card' | 'corp_transfer' | 'cash_deposit' | 'fx_exchange'

const BIZ_TYPE_NAMES: Record<BizType, string> = {
  cash_reserve: '大额取现预约',
  open_card: '办卡开户',
  corp_transfer: '对公跨行转账汇款',
  cash_deposit: '对公现金缴款',
  fx_exchange: '外币兑换',
}

export const useAuditLogStore = defineStore('auditLog', () => {
  // 一次性清理旧的示例数据（版本变更时触发）
  const AUDIT_LOG_VERSION = 'v2_real'
  if (localStorage.getItem('icbc_audit_logs_version') !== AUDIT_LOG_VERSION) {
    localStorage.removeItem('icbc_audit_logs')
    localStorage.setItem('icbc_audit_logs_version', AUDIT_LOG_VERSION)
  }
  const auditLogs = ref<AuditLog[]>(JSON.parse(localStorage.getItem('icbc_audit_logs') || '[]'))

  const completedCount = computed(() =>
    auditLogs.value.filter(l => l.status === '已完成').length
  )
  const pendingCount = computed(() =>
    auditLogs.value.filter(l => l.status === '已提交').length
  )

  function saveToStorage() {
    localStorage.setItem('icbc_audit_logs', JSON.stringify(auditLogs.value))
  }

  /** 添加一条预填单记录（由 PreFormView 调用） */
  function add(log: Omit<AuditLog, 'id' | 'sn' | 'hash'>) {
    const randomId = Math.floor(1000 + Math.random() * 9000)
    const ts = new Date().toISOString().slice(0, 10).replace(/-/g, '')
    const sn = `ICBC-${ts}-${randomId}`
    const hash = '0x' + Math.random().toString(16).substring(2, 10) + randomId.toString(16)

    const newLog: AuditLog = {
      ...log,
      id: Date.now(),
      sn,
      hash,
      timestamp: new Date().toLocaleString('zh-CN'),
      idCardMasked: log.idCardMasked ? log.idCardMasked.slice(0, 4) + '********' + log.idCardMasked.slice(-4) : '',
      phoneMasked: log.phoneMasked ? log.phoneMasked.slice(0, 3) + '****' + log.phoneMasked.slice(-4) : '',
    }
    auditLogs.value.unshift(newLog)
    saveToStorage()
    return newLog
  }

  /** 取消某条记录 */
  function cancel(id: number) {
    const log = auditLogs.value.find(l => l.id === id)
    if (log && log.status !== '已完成') {
      log.status = '已取消'
      saveToStorage()
    }
  }

  /** 完成某条记录 */
  function complete(id: number) {
    const log = auditLogs.value.find(l => l.id === id)
    if (log) {
      log.status = '已完成'
      saveToStorage()
    }
  }

  /** 删除某条记录 */
  function remove(id: number) {
    const idx = auditLogs.value.findIndex(l => l.id === id)
    if (idx > -1) {
      auditLogs.value.splice(idx, 1)
      saveToStorage()
    }
  }

  /** 初始化示例数据（用于展示） */
  function initSampleData() {
    if (auditLogs.value.length > 0) return
    const now = new Date()
    const makeDate = (offsetDays: number): string => {
      const d = new Date(now.getTime() - offsetDays * 86400000)
      return d.toLocaleString('zh-CN')
    }

    const samples: Omit<AuditLog, 'id' | 'sn' | 'hash'>[] = [
      {
        bizType: 'cash_reserve',
        bizTypeName: BIZ_TYPE_NAMES.cash_reserve,
        userName: '王五',
        idCardMasked: '110101199505051234',
        phoneMasked: '13812345678',
        extraData: {
          amount: '500000',
          currency: 'CNY',
          date: '明天',
          time: '上午10点',
          branch: '工行北京分行营业部',
          purpose: 'purchase',
          fundSource: 'saving',
          denomination: '100',
        },
        timestamp: makeDate(1),
        status: '已提交',
      },
      {
        bizType: 'open_card',
        bizTypeName: BIZ_TYPE_NAMES.open_card,
        userName: '李四',
        idCardMasked: '310104199008085678',
        phoneMasked: '13987654321',
        extraData: {
          cardType: 'platinum',
          accountType: 'type1',
          initialDeposit: '500',
          onlineBanking: 'yes',
          mobileBanking: 'yes',
          deliveryMethod: 'mail',
          emergencyContact: '张伟',
          address: '北京市朝阳区建国门外大街1号',
        },
        timestamp: makeDate(3),
        status: '已完成',
      },
      {
        bizType: 'corp_transfer',
        bizTypeName: BIZ_TYPE_NAMES.corp_transfer,
        userName: '张明',
        idCardMasked: '310104198810101234',
        phoneMasked: '13912345678',
        extraData: {
          payerName: '上海宏达贸易有限公司',
          payerAccount: '1001234567890123',
          recvName: '北京中科科技股份有限公司',
          recvCard: '6222021234567890123',
          recvBank: '中国建设银行北京分行营业部',
          amount: '500000',
          urgent: 'yes',
          purpose: '货款',
        },
        timestamp: makeDate(5),
        status: '已提交',
      },
      {
        bizType: 'fx_exchange',
        bizTypeName: BIZ_TYPE_NAMES.fx_exchange,
        userName: '赵六',
        idCardMasked: '440106199201015678',
        phoneMasked: '13600001111',
        extraData: {
          fxCurrency: 'usd',
          fxDirection: 'buy',
          fxAmount: '5000',
          amount: '35000',
          purpose: 'travel',
          time: '明日上午 10:00',
          branch: '工行中关村支行',
        },
        timestamp: makeDate(7),
        status: '已取消',
      },
      {
        bizType: 'cash_deposit',
        bizTypeName: BIZ_TYPE_NAMES.cash_deposit,
        userName: '钱七',
        idCardMasked: '510107198805051234',
        phoneMasked: '13500002222',
        extraData: {
          payerName: '成都创新科技有限公司',
          payerAccount: '2001234567890123',
          amount: '100000',
          depositMethod: 'cash',
          purpose: '营业款',
          branch: '工行成都春熙路支行',
        },
        timestamp: makeDate(10),
        status: '已完成',
      },
      {
        bizType: 'cash_reserve',
        bizTypeName: BIZ_TYPE_NAMES.cash_reserve,
        userName: '孙八',
        idCardMasked: '320106199102021234',
        phoneMasked: '13700003333',
        extraData: {
          amount: '200000',
          currency: 'CNY',
          date: '下周一',
          time: '下午14点',
          branch: '工行南京新街口支行',
          purpose: 'medical',
          fundSource: 'salary',
          denomination: 'mixed',
        },
        timestamp: makeDate(14),
        status: '已提交',
      },
    ]

    auditLogs.value = samples.map((s, i) => ({
      ...s,
      id: Date.now() + i,
      sn: `ICBC-2026${String(8 - i).padStart(2, '0')}${String(1000 + i).padStart(4, '0')}`,
      hash: '0x' + String(i).padStart(8, '0') + Math.random().toString(16).substring(2, 10),
    }))
    saveToStorage()
  }

  return {
    auditLogs,
    completedCount,
    pendingCount,
    add,
    cancel,
    complete,
    remove,
    initSampleData,
  }
})
