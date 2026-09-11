import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api'

export type ApptStatus = 'virtual' | 'active' | 'expired' | 'completed'

export interface Appointment {
  /** 后端预约ID（调API用） */
  id?: number
  voucherNum: string
  branchName: string
  date: string
  timeSlot: string
  businessType: string
  status: ApptStatus
  createdAt: string
  // 办理进度：1=取号 2=排队 3=叫号 4=办理中 5=完成
  step?: number
  // 前方等待人数
  aheadCount?: number
  // 分配窗口
  window?: string
  // 评分（1-5）
  rating?: number
}

/** 后端状态 → 前端状态 */
function mapStatus(backendStatus: string): ApptStatus {
  switch (backendStatus) {
    case 'COMPLETED': return 'completed'
    case 'EXPIRED': return 'expired'
    case 'ACTIVE':
    case 'CALLED':
    case 'PROCESSING': return 'active'
    case 'VIRTUAL':
    default: return 'virtual'
  }
}

/** 后端预约记录 → 前端 Appointment */
function mapAppointment(raw: Record<string, any>): Appointment {
  return {
    id: raw.id,
    voucherNum: raw.voucherNum || '',
    branchName: raw.branchName || '',
    date: raw.appointmentDate || '',
    timeSlot: raw.timeSlot || '',
    businessType: raw.businessType || '',
    status: mapStatus(raw.status),
    createdAt: raw.createdAt || '',
    step: raw.progressStep !== undefined ? raw.progressStep + 1 : 1,
    aheadCount: 0,
    window: '',
  }
}

export const useAppointmentStore = defineStore('appointment', () => {
  const appointments = ref<Appointment[]>([])
  const creditScore = ref<number>(100)
  const loaded = ref(false)

  const activeAppointments = computed(() =>
    appointments.value.filter(a => a.status === 'virtual' || a.status === 'active')
  )

  /** 从后端加载我的预约列表 */
  async function fetchMy() {
    const data = await api.get('/appointments/my')
    appointments.value = (data as any[]).map(mapAppointment)
    loaded.value = true
  }

  /** 从后端加载信用分 */
  async function fetchCredit() {
    const data = await api.get('/credit')
    creditScore.value = data.creditScore ?? 100
  }

  /**
   * 创建预约（真实API）
   * @param data.branchId  后端网点ID（必填）
   * @param data.branchName 网点名称（前端展示用）
   */
  async function book(data: {
    branchId: number
    branchName: string
    date: string       // ISO格式: 2026-09-12
    timeSlot: string   // 09:00-09:30
    businessType: string
  }): Promise<Appointment> {
    const raw = await api.post('/appointments', {
      branchId: data.branchId,
      businessType: data.businessType,
      appointmentDate: data.date,
      timeSlot: data.timeSlot,
    })
    const appt = mapAppointment(raw)
    appt.branchName = data.branchName
    appointments.value.unshift(appt)
    return appt
  }

  /** 取消预约（真实API，按 voucherNum 查找后端ID） */
  async function cancel(voucherNum: string) {
    const appt = appointments.value.find(a => a.voucherNum === voucherNum)
    if (!appt) return
    if (appt.id) {
      await api.delete(`/appointments/${appt.id}`)
    }
    const idx = appointments.value.findIndex(a => a.voucherNum === voucherNum)
    if (idx > -1) appointments.value.splice(idx, 1)
    // 后端会自动扣5分，同步一下信用分
    await fetchCredit()
  }

  /**
   * 更新状态
   * 'active' = 取号报到（调后端 advanceProgress: VIRTUAL→ACTIVE）
   */
  async function updateStatus(voucherNum: string, status: Appointment['status']) {
    const appt = appointments.value.find(a => a.voucherNum === voucherNum)
    if (!appt) return

    if (status === 'active' && appt.status === 'virtual' && appt.id) {
      // 取号报到 = 推进一步进度
      const raw = await api.put(`/appointments/${appt.id}/progress`)
      Object.assign(appt, mapAppointment(raw))
      return
    }

    // 其他状态（expired 等）仅本地更新
    appt.status = status
  }

  /** 推进一步进度（叫号/办理中/完成） */
  async function stepForward(voucherNum: string) {
    const appt = appointments.value.find(a => a.voucherNum === voucherNum)
    if (!appt || !appt.id) return

    const raw = await api.put(`/appointments/${appt.id}/progress`)
    Object.assign(appt, mapAppointment(raw))

    // 办理完成时后端自动+3分，同步信用分
    if (appt.status === 'completed') {
      await fetchCredit()
    }
  }

  // 设置评分（本地）
  function setRating(voucherNum: string, rating: number) {
    const appt = appointments.value.find(a => a.voucherNum === voucherNum)
    if (appt) appt.rating = rating
  }

  // 以下方法仅为兼容旧视图，实际扣分已由后端自动完成
  function deductScore(_reason: string) {
    creditScore.value = Math.max(0, creditScore.value - 10)
  }

  function addScore(_reason: string) {
    creditScore.value = Math.min(100, creditScore.value + 5)
  }

  return {
    appointments, creditScore, activeAppointments, loaded,
    fetchMy, fetchCredit,
    book, updateStatus, stepForward, setRating, cancel, deductScore, addScore,
  }
})
