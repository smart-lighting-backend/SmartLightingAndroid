<script setup>
import { computed, onBeforeUnmount, onMounted, provide, ref } from 'vue'
import { useRoute } from 'vue-router'

const props = defineProps({
  designWidth: {
    type: Number,
    default: 1920,
  },
  maxScale: {
    type: Number,
    default: 2,
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  excludePaths: {
    type: Array,
    default: () => ['/login'],
  },
})

const route = useRoute()
const scaleRatio = ref(1)
const viewportHeight = ref(window.innerHeight)
let resizeTimer = null

const shouldScale = computed(() => {
  return !props.disabled && !props.excludePaths.includes(route.path)
})

const contentStyle = computed(() => {
  const ratio = scaleRatio.value || 1

  return {
    width: `${props.designWidth}px`,
    height: `${viewportHeight.value / ratio}px`,
    transform: `scale(${ratio})`,
    '--scale-ratio': ratio,
  }
})

function updateScale() {
  viewportHeight.value = window.innerHeight
  scaleRatio.value = Math.min(window.innerWidth / props.designWidth, props.maxScale)
}

function handleResize() {
  window.clearTimeout(resizeTimer)
  resizeTimer = window.setTimeout(updateScale, 100)
}

provide('screenScaleRatio', scaleRatio)

onMounted(() => {
  updateScale()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.clearTimeout(resizeTimer)
  window.removeEventListener('resize', handleResize)
})
</script>

<template>
  <slot v-if="!shouldScale" />
  <div v-else class="screen-scale-wrapper">
    <div class="screen-scale-content" :style="contentStyle">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.screen-scale-wrapper {
  width: 100%;
  height: 100%;
  overflow: hidden auto;
  display: flex;
  justify-content: center;
  background: #060e1f;
}

.screen-scale-content {
  flex: 0 0 auto;
  transform-origin: top center;
  overflow: hidden;
}
</style>
