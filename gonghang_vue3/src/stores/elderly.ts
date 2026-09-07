import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useElderlyStore = defineStore('elderly', () => {
  const isElderly = ref(localStorage.getItem('elderly_mode') === 'true')

  function toggle() {
    isElderly.value = !isElderly.value
    localStorage.setItem('elderly_mode', isElderly.value.toString())
    if (isElderly.value) {
      document.documentElement.classList.add('elderly-mode')
    } else {
      document.documentElement.classList.remove('elderly-mode')
    }
  }

  function init() {
    if (isElderly.value) {
      document.documentElement.classList.add('elderly-mode')
    }
  }

  return { isElderly, toggle, init }
})
