<template>
  <ScreenScale>
    <RouterView />
    <MockIndicator />
  </ScreenScale>
</template>

<script setup>
import ScreenScale from './components/ScreenScale.vue'
import MockIndicator from './components/MockIndicator.vue'
</script>
