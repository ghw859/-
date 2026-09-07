import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export type ApptStatus = 'virtual' | 'active' | 'expired' | 'completed'

export interface Appointment {
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

export const useAppointmentStore = defineStore('appointment', () => {
  const appointments = ref<Appointment[]>(JSON.parse(localStorage.getItem('appointments') || '[]'))
  const creditScore = ref<number>(parseInt(localStorage.getItem('credit_score') || '100'))

  const activeAppointments = computed(() =>
    appointments.value.filter(a => a.status === 'virtual' || a.status === 'active')
  )

  function saveToStorage() {
    localStorage.setItem('appointments', JSON.stringify(appointments.value))
    localStorage.setItem('credit_score', creditScore.value.toString())
  }

  function book(data: Omit<Appointment, 'voucherNum' | 'status' | 'createdAt'>) {
    const conflict = appointments.value.find(a =>
      a.date === data.date && a.timeSlot === data.timeSlot &&
      a.branchName !== data.branchName &&
      a.status !== 'expired' && a.status !== 'completed'
    )
    if (conflict) throw new Error('同一时段已在其他网点预约，请先取消原预约')

    const slotCount = appointments.value.filter(a =>
      a.branchName === data.branchName && a.date === data.date && a.timeSlot === data.timeSlot &&
      a.status !== 'expired'
    ).length
    if (slotCount >= 10) throw new Error('该时段已约满')

    const voucherPrefix = ['A', 'B', 'C', 'D', 'E'][Math.floor(Math.random() * 5)]
    const voucherNum = voucherPrefix + String(Date.now()).slice(-3).padStart(3, '0')
    const newAppt: Appointment = {
      ...data,
      voucherNum,
      status: 'virtual',
      createdAt: new Date().toISOString(),
      step: 1,
      aheadCount: Math.floor(Math.random() * 8) + 2,
      window: ''
    }
    appointments.value.push(newAppt)
    saveToStorage()
    return newAppt
  }

  function updateStatus(voucherNum: string, status: Appointment['status']) {
    const appt = appointments.value.find(a => a.voucherNum === voucherNum)
    if (appt) {
      appt.status = status
      if (status === 'active' && appt.step === 1) appt.step = 2
      if (status === 'completed') appt.step = 5
      saveToStorage()
    }
  }

  // 推进一步进度（叫号/办理中/完成）
  function stepForward(voucherNum: string) {
    const appt = appointments.value.find(a => a.voucherNum === voucherNum)
    if (!appt) return
    if (appt.step === undefined) appt.step = 1
    if (appt.step < 5) {
      appt.step += 1
      if (appt.step === 3) {
        // 叫号：分配窗口
        appt.window = `窗口 ${Math.floor(Math.random() * 8) + 1}`
        if (appt.aheadCount) appt.aheadCount = 0
      }
      if (appt.step === 5) {
        appt.status = 'completed'
      }
      saveToStorage()
    }
  }

  // 设置评分
  function setRating(voucherNum: string, rating: number) {
    const appt = appointments.value.find(a => a.voucherNum === voucherNum)
    if (appt) {
      appt.rating = rating
      saveToStorage()
    }
  }

  function cancel(voucherNum: string) {
    const idx = appointments.value.findIndex(a => a.voucherNum === voucherNum)
    if (idx > -1) {
      appointments.value.splice(idx, 1)
      saveToStorage()
    }
  }

  function deductScore(_reason: string) {
    creditScore.value = Math.max(0, creditScore.value - 10)
    saveToStorage()
  }

  function addScore(_reason: string) {
    creditScore.value = Math.min(100, creditScore.value + 5)
    saveToStorage()
  }

  return {
    appointments, creditScore, activeAppointments,
    book, updateStatus, stepForward, setRating, cancel, deductScore, addScore
  }
})
