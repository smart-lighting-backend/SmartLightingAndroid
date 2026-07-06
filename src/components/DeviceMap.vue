<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useAMap } from '../composables/useAMap.js'
import { parseLocation } from '../utils/coordinate.js'

const router = useRouter()
const { AMap: AMapRef, loaded, loading, error } = useAMap()

const props = defineProps({
  devices: { type: Array, default: () => [] },
  highlightDeviceId: { type: String, default: '' },
  height: { type: String, default: '360px' },
})

const emit = defineEmits(['marker-click'])

const wrapperRef = ref(null)
const mapContainerRef = ref(null)

let map = null
const markerMap = new Map()
let currentHighlight = null
let infoWindow = null
let resizeObserver = null

const COLORS = { 0: '#888888', 1: '#4caf82', 2: '#9e9e9e', 3: '#ffa726' }
const LABELS = { 0: '停用', 1: '在线', 2: '离线', 3: '异常' }

const noLocationCount = computed(() =>
  props.devices.filter(d => !parseLocation(d.location)).length
)

// ── 简易圆形图标（Canvas → data URI） ──
// ── Canvas 路灯图标 ──
const iconCache = {}

function makeIcon(color, w, h) {
  const k = `${color}_${w}x${h}`
  if (iconCache[k]) return iconCache[k]

  const canvas = document.createElement('canvas')
  canvas.width = w
  canvas.height = h
  const ctx = canvas.getContext('2d')
  const cx = w / 2
  const headR = Math.round(w * 0.24)   // 灯泡半径
  const headY = headR + 2              // 灯泡圆心 Y
  const armW = Math.round(w * 0.48)    // 灯臂宽
  const armH = Math.round(w * 0.12)    // 灯臂高
  const armY = headY + headR + 1       // 灯臂 Y
  const poleW = Math.max(3, Math.round(w * 0.1))
  const poleY = armY + armH
  const poleH = h - poleY

  // ─ 光晕（径向渐变） ─
  const glow = ctx.createRadialGradient(cx, headY, headR * 0.4, cx, headY, headR * 2.2)
  glow.addColorStop(0, color)
  glow.addColorStop(0.4, color + '99')
  glow.addColorStop(1, 'transparent')
  ctx.fillStyle = glow
  ctx.beginPath()
  ctx.arc(cx, headY, headR * 2.2, 0, Math.PI * 2)
  ctx.fill()

  // ─ 灯泡 ─
  const bulb = ctx.createRadialGradient(cx - headR * 0.3, headY - headR * 0.3, headR * 0.1, cx, headY, headR)
  bulb.addColorStop(0, '#ffffff')
  bulb.addColorStop(0.35, color)
  bulb.addColorStop(1, color)
  ctx.fillStyle = bulb
  ctx.beginPath()
  ctx.arc(cx, headY, headR, 0, Math.PI * 2)
  ctx.fill()
  ctx.strokeStyle = 'rgba(255,255,255,0.85)'
  ctx.lineWidth = Math.max(1.5, w * 0.05)
  ctx.stroke()

  // ─ 灯臂 ─
  ctx.fillStyle = color
  roundRect(ctx, cx - armW / 2, armY, armW, armH, 3)

  // ─ 灯杆 ─
  roundRect(ctx, cx - poleW / 2, poleY, poleW, poleH, poleW / 2)

  iconCache[k] = canvas.toDataURL('image/png')
  return iconCache[k]
}

function roundRect(ctx, x, y, w, h, r) {
  ctx.beginPath()
  ctx.moveTo(x + r, y)
  ctx.lineTo(x + w - r, y)
  ctx.quadraticCurveTo(x + w, y, x + w, y + r)
  ctx.lineTo(x + w, y + h)
  ctx.lineTo(x, y + h)
  ctx.lineTo(x, y + r)
  ctx.quadraticCurveTo(x, y, x + r, y)
  ctx.closePath()
  ctx.fill()
}

function getIcon(status, hl) {
  const color = COLORS[status] || '#888888'
  const w = hl ? 52 : 38
  const h = hl ? 70 : 52
  return new AMapRef.value.Icon({
    image: makeIcon(color, w, h),
    imageSize: new AMapRef.value.Size(w, h),
    size: new AMapRef.value.Size(w, h),
  })
}

// ── 标记 ──
function addMarkers(AMap) {
  markerMap.forEach(m => { m.setMap(null); m.remove() })
  markerMap.clear()

  props.devices.forEach((d) => {
    const pos = parseLocation(d.location)
    if (!pos) return

    const marker = new AMap.Marker({
      position: [pos.lng, pos.lat],
      icon: getIcon(d.status, false),
      anchor: 'bottom-center',
      zIndex: 100,
    })

    marker.on('click', () => {
      emit('marker-click', d)
      highlightDevice(d.deviceId)
      openInfoWindow(d, pos)
    })

    marker.__deviceData = d
    markerMap.set(d.deviceId, marker)
    marker.setMap(map)
  })

  if (markerMap.size > 0) {
    map.setFitView(Array.from(markerMap.values()))
  }
}

function openInfoWindow(device, pos) {
  if (!infoWindow) return
  const color = COLORS[device.status] || '#888888'
  const label = LABELS[device.status] || '未知'
  const did = device.deviceId
  infoWindow.setContent(`
    <div class="dm-iw">
      <div class="dm-iw-name">${device.name || did}</div>
      <div class="dm-iw-row"><span>状态</span><span style="color:${color}">${label}</span></div>
      <div class="dm-iw-row"><span>健康分</span><span>${device.healthScore ?? '--'}</span></div>
      <div class="dm-iw-row"><span>区域</span><span>${device.area || '--'}</span></div>
      <span class="dm-iw-link" onclick="window.__dmNav && window.__dmNav('${did}')">查看详情 →</span>
    </div>
  `)
  infoWindow.open(map, [pos.lng, pos.lat])
}

function highlightDevice(deviceId) {
  if (currentHighlight) {
    const prev = currentHighlight
    prev.setIcon(getIcon(prev.__deviceData.status, false))
    currentHighlight = null
  }
  if (!deviceId) return
  const marker = markerMap.get(deviceId)
  if (!marker) return
  currentHighlight = marker
  marker.setIcon(getIcon(marker.__deviceData.status, true))
  map.setZoomAndCenter(18, marker.getPosition())
}

function clearHighlight() { highlightDevice(null) }

function initMap() {
  if (!mapContainerRef.value || !AMapRef.value) return
  if (map) return

  map = new AMapRef.value.Map(mapContainerRef.value, {
    mapStyle: 'amap://styles/dark',
    zoom: 15,
    center: [106.5622, 29.5621],
  })

  infoWindow = new AMapRef.value.InfoWindow({ offset: new AMapRef.value.Pixel(0, -40) })
  addMarkers(AMapRef.value)
}

// ── 监听 ──
watch(loaded, (ok) => { if (ok) nextTick(initMap) })

watch(() => props.devices, () => {
  if (map && AMapRef.value) addMarkers(AMapRef.value)
}, { deep: true })

watch(() => props.highlightDeviceId, (id) => {
  if (id) highlightDevice(id)
  else clearHighlight()
})

onMounted(() => {
  window.__dmNav = (id) => router.push(`/devices/${id}`)
  if (loaded.value) nextTick(initMap)
  if (wrapperRef.value) {
    resizeObserver = new ResizeObserver(() => { if (map) map.resize() })
    resizeObserver.observe(wrapperRef.value)
  }
})

onUnmounted(() => {
  resizeObserver?.disconnect()
  if (map) { map.destroy(); map = null }
  markerMap.clear()
  currentHighlight = null
  delete window.__dmNav
})

defineExpose({ highlightDevice, clearHighlight, fitBounds: () => {} })
</script>

<template>
  <div ref="wrapperRef" class="dm-wrapper" :style="{ height }">
    <div v-if="!loaded" class="dm-overlay">
      <span v-if="loading">地图加载中...</span>
      <span v-else-if="error" class="dm-error">{{ error }}</span>
    </div>
    <div ref="mapContainerRef" class="dm-container"></div>
    <div v-if="noLocationCount > 0" class="dm-no-loc-hint">
      {{ noLocationCount }} 台设备无位置信息，未在地图上显示
    </div>
  </div>
</template>

<style>
.dm-iw {
  min-width: 150px; padding: 2px 2px;
  background: rgba(8,20,45,0.95); border-radius: 6px;
  color: #d0eaf8; font-size: 12px; line-height: 1.9;
}
.dm-iw-name {
  font-size: 14px; font-weight: 600; margin-bottom: 2px;
  color: #fff; border-bottom: 1px solid rgba(255,255,255,0.1); padding-bottom: 4px;
}
.dm-iw-row { display: flex; justify-content: space-between; gap: 14px; padding: 0 2px; }
.dm-iw-link { display: inline-block; margin-top: 6px; color: #4dd0e1; cursor: pointer; font-size: 12px; }
.dm-iw-link:hover { text-decoration: underline; color: #80e8f0; }
</style>

<style scoped>
.dm-wrapper { position: relative; width: 100%; border-radius: 8px; overflow: hidden; background: rgba(0,10,30,0.5); }
.dm-container { width: 100%; height: 100%; }
.dm-overlay {
  position: absolute; inset: 0; z-index: 1;
  display: flex; align-items: center; justify-content: center;
  background: rgba(8,20,45,0.55); color: rgba(140,190,220,0.7); font-size: 13px;
}
.dm-error { color: #ef5350; }
.dm-no-loc-hint {
  position: absolute; bottom: 0; left: 0; right: 0; z-index: 10;
  padding: 6px 12px; text-align: center; font-size: 12px;
  color: rgba(255,200,100,0.85); background: rgba(0,10,30,0.8);
  border-top: 1px solid rgba(255,167,38,0.3);
}
</style>
