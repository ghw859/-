<script setup lang="ts">
import { useElderlyStore } from './stores/elderly'
import { onMounted } from 'vue'

const elderly = useElderlyStore()

onMounted(() => {
  elderly.init()
})
</script>

<template>
  <RouterView />
</template>
