import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'

export type BranchStatus = 'free' | 'moderate' | 'busy'

export interface Branch {
  id: string
  name: string
  status: BranchStatus
  distance: number // 米
  services: string[]
  wait: number // 分钟
  flow: number // 在店人数
  reserve: number // 线上预约人数
  window: string // 例如 "8/10"
  trend: number[]
  address: string
  phone: string
  hours: string
  icon: string
  iconBg: string
  iconColor: string
  favorite: boolean
}

// 业务标签显示文本映射
export const serviceTagText: Record<string, string> = {
  '大额现金': '大额现金柜面',
  '外汇': '跨境外汇办理',
  '自助发卡': '智能自助发卡舱',
  'VTM': 'VTM 远程柜员',
  'VIP': '财富管理VIP室',
  '对公': '对公业务专窗',
  '无障碍': '无障碍绿色通道',
}

/** 后端繁忙度 → 前端状态 */
function mapBusyLevel(level?: string): BranchStatus {
  switch (level) {
    case 'BUSY': return 'busy'
    case 'MODERATE': return 'moderate'
    case 'IDLE':
    default: return 'free'
  }
}

/** 前端网点ID → 后端网点ID 映射（预约提交时用） */
const BRANCH_ID_MAP: Record<string, number> = { b1: 1, b2: 2, b3: 3, b4: 4, b5: 1, b6: 2 }

export function toBackendBranchId(frontendId: string): number {
  return BRANCH_ID_MAP[frontendId] || 1
}

export const useBranchStore = defineStore('branch', () => {
  // 6 个网点数据（UI 展示字段保留 mock，真实字段由后端覆盖）
  const branches = ref<Branch[]>([
    {
      id: 'b1',
      name: '北京分行营业部',
      status: 'busy',
      distance: 850,
      services: ['大额现金', '外汇', '无障碍'],
      wait: 25,
      flow: 42,
      reserve: 18,
      window: '8/10',
      trend: [18, 22, 28, 20, 25, 25],
      address: '北京市西城区复兴门内大街55号',
      phone: '010-66695588',
      hours: '09:00 - 17:00',
      icon: 'fa-solid fa-landmark',
      iconBg: 'bg-blue-50',
      iconColor: 'text-blue-600',
      favorite: false,
    },
    {
      id: 'b2',
      name: '长安街智慧示范支行',
      status: 'moderate',
      distance: 1400,
      services: ['自助发卡', 'VTM'],
      wait: 8,
      flow: 19,
      reserve: 7,
      window: '5/6',
      trend: [12, 10, 6, 9, 7, 8],
      address: '北京市东城区东长安街1号',
      phone: '010-65129588',
      hours: '09:00 - 17:00',
      icon: 'fa-solid fa-robot',
      iconBg: 'bg-cyan-50',
      iconColor: 'text-cyan-600',
      favorite: false,
    },
    {
      id: 'b3',
      name: '金融街私人银行旗舰支行',
      status: 'free',
      distance: 2100,
      services: ['VIP'],
      wait: 3,
      flow: 8,
      reserve: 3,
      window: '6/6',
      trend: [5, 4, 3, 2, 3, 3],
      address: '北京市西城区金融大街15号',
      phone: '010-66299588',
      hours: '09:00 - 17:30',
      icon: 'fa-solid fa-crown',
      iconBg: 'bg-emerald-50',
      iconColor: 'text-emerald-600',
      favorite: false,
    },
    {
      id: 'b4',
      name: '中关村科技创新特色支行',
      status: 'moderate',
      distance: 3800,
      services: ['对公'],
      wait: 12,
      flow: 25,
      reserve: 12,
      window: '6/8',
      trend: [8, 15, 10, 14, 11, 12],
      address: '北京市海淀区中关村大街22号',
      phone: '010-62599588',
      hours: '09:00 - 17:00',
      icon: 'fa-solid fa-microchip',
      iconBg: 'bg-purple-50',
      iconColor: 'text-purple-600',
      favorite: false,
    },
    {
      id: 'b5',
      name: '望京SOHO社区支行',
      status: 'free',
      distance: 3200,
      services: ['自助发卡', '无障碍'],
      wait: 5,
      flow: 10,
      reserve: 4,
      window: '4/4',
      trend: [8, 6, 4, 7, 5, 5],
      address: '北京市朝阳区望京街10号望京SOHO塔1座',
      phone: '010-59799588',
      hours: '09:00 - 17:00',
      icon: 'fa-solid fa-shop',
      iconBg: 'bg-emerald-50',
      iconColor: 'text-emerald-600',
      favorite: false,
    },
    {
      id: 'b6',
      name: '国贸CBD中心支行',
      status: 'busy',
      distance: 1800,
      services: ['外汇', 'VIP', '对公'],
      wait: 20,
      flow: 38,
      reserve: 15,
      window: '7/8',
      trend: [15, 18, 22, 17, 20, 20],
      address: '北京市朝阳区建国门外大街1号国贸大厦',
      phone: '010-65059588',
      hours: '09:00 - 17:00',
      icon: 'fa-solid fa-city',
      iconBg: 'bg-blue-50',
      iconColor: 'text-blue-600',
      favorite: false,
    },
  ])

  /**
   * 从后端拉取网点真实数据，合并到本地列表
   * 后端只有4个网点，按顺序覆盖前4个；UI专用字段（trend/icon/window等）保留 mock
   */
  async function fetchBranches() {
    try {
      const list = await request.get('/api/branches')
      if (!Array.isArray(list)) return
      list.slice(0, branches.value.length).forEach((raw: Record<string, any>, idx: number) => {
        const local = branches.value[idx]
        if (!local) return
        if (raw.name) local.name = raw.name
        if (raw.address) local.address = raw.address
        if (raw.businessHours) local.hours = raw.businessHours.replace('-', ' - ')
        if (raw.busyLevel) local.status = mapBusyLevel(raw.busyLevel)
        if (raw.currentQueue !== undefined) local.flow = raw.currentQueue
      })
    } catch {
      // 后端不可用时保留 mock 数据，不影响页面
    }
  }

  function toggleFavorite(id: string) {
    const b = branches.value.find(x => x.id === id)
    if (b) b.favorite = !b.favorite
  }

  return { branches, fetchBranches, toggleFavorite, toBackendBranchId }
})
