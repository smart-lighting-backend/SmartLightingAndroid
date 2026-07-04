import { computed, inject, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const SCALE_KEYS = new Set([
  'fontSize',
  'width',
  'height',
  'padding',
  'borderRadius',
  'borderWidth',
  'itemGap',
  'itemWidth',
  'itemHeight',
  'symbolSize',
  'barWidth',
  'top',
  'right',
  'bottom',
  'left',
])

function roundScaled(value, ratio) {
  return Number((value * ratio).toFixed(2))
}

function scaleByKey(key, value, ratio) {
  if (!SCALE_KEYS.has(key)) return value
  if (typeof value === 'number') return roundScaled(value, ratio)
  if (Array.isArray(value)) {
    return value.map(item => (typeof item === 'number' ? roundScaled(item, ratio) : item))
  }
  return value
}

function scaleNode(node, ratio, key = '') {
  const scaled = scaleByKey(key, node, ratio)
  if (scaled !== node) return scaled

  if (Array.isArray(node)) {
    return node.map(item => scaleNode(item, ratio))
  }

  if (node && typeof node === 'object') {
    return Object.fromEntries(
      Object.entries(node).map(([childKey, value]) => [
        childKey,
        scaleNode(value, ratio, childKey),
      ]),
    )
  }

  return node
}

export function useChartScale(baseWidth = 1920) {
  const injectedRatio = inject('screenScaleRatio', null)
  const fallbackRatio = ref(Math.min(window.innerWidth / baseWidth, 2))
  const scaleRatio = computed(() => injectedRatio?.value ?? fallbackRatio.value)

  function updateFallbackRatio() {
    fallbackRatio.value = Math.min(window.innerWidth / baseWidth, 2)
  }

  function scaleVal(value) {
    return typeof value === 'number' ? roundScaled(value, scaleRatio.value) : value
  }

  function scaleOption(option) {
    return scaleNode(option, scaleRatio.value)
  }

  function onScaleChange(callback) {
    return watch(scaleRatio, callback)
  }

  onMounted(() => {
    if (!injectedRatio) window.addEventListener('resize', updateFallbackRatio)
  })

  onBeforeUnmount(() => {
    if (!injectedRatio) window.removeEventListener('resize', updateFallbackRatio)
  })

  return {
    scaleRatio,
    scaleVal,
    scaleOption,
    onScaleChange,
  }
}
