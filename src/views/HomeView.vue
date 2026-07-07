<script setup>
import { ref } from 'vue'
import { useThreeScene } from '../composables/useThreeScene.js'

const containerRef = ref(null)
useThreeScene(containerRef)
</script>

<template>
  <div class="home-view">
    <div ref="containerRef" class="three-container" />
  </div>
</template>

<style scoped>
.home-view {
  width: 100vw;
  height: 100vh;
  overflow: hidden;
}

.three-container {
  width: 100%;
  height: 100%;
}
</style>
