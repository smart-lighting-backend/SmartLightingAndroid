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

const COLORS = { 0: '#6b7f93', 1: '#10b981', 2: '#7b8794', 3: '#f59e0b' }
const LABELS = { 0: '停用', 1: '在线', 2: '离线', 3: '异常' }

const noLocationCount = computed(() =>
  props.devices.filter(d => !parseLocation(d.location)).length
)

const mapStats = computed(() => {
  const total = props.devices.length
  return {
    total,
    visible: total - noLocationCount.value,
    online: props.devices.filter(d => d.status === 1).length,
    warning: props.devices.filter(d => d.status === 3).length,
    offline: props.devices.filter(d => d.status === 2 || d.status === 0).length,
  }
})

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
  const headR = Math.round(w * 0.21)   // 灯泡半径
  const headY = headR + 6              // 灯泡圆心 Y
  const armW = Math.round(w * 0.46)    // 灯臂宽
  const armH = Math.round(w * 0.1)     // 灯臂高
  const armY = headY + headR + 2       // 灯臂 Y
  const poleW = Math.max(3, Math.round(w * 0.1))
  const poleY = armY + armH
  const poleH = h - poleY - 4

  ctx.shadowColor = color
  ctx.shadowBlur = w * 0.42

  // ─ 光晕（径向渐变） ─
  const glow = ctx.createRadialGradient(cx, headY, headR * 0.25, cx, headY, headR * 2.8)
  glow.addColorStop(0, color)
  glow.addColorStop(0.35, color + 'aa')
  glow.addColorStop(0.72, color + '33')
  glow.addColorStop(1, 'transparent')
  ctx.fillStyle = glow
  ctx.beginPath()
  ctx.arc(cx, headY, headR * 2.8, 0, Math.PI * 2)
  ctx.fill()

  ctx.shadowBlur = 0
  ctx.strokeStyle = color + '66'
  ctx.lineWidth = Math.max(1, w * 0.035)
  ctx.beginPath()
  ctx.arc(cx, headY, headR * 1.55, 0, Math.PI * 2)
  ctx.stroke()

  ctx.strokeStyle = 'rgba(255,255,255,0.88)'
  ctx.lineWidth = Math.max(1, w * 0.025)
  ctx.beginPath()
  ctx.arc(cx, headY, headR * 1.12, 0, Math.PI * 2)
  ctx.stroke()

  // ─ 灯泡 ─
  const bulb = ctx.createRadialGradient(cx - headR * 0.3, headY - headR * 0.3, headR * 0.1, cx, headY, headR)
  bulb.addColorStop(0, '#ffffff')
  bulb.addColorStop(0.42, color + 'ee')
  bulb.addColorStop(1, color)
  ctx.fillStyle = bulb
  ctx.beginPath()
  ctx.arc(cx, headY, headR, 0, Math.PI * 2)
  ctx.fill()
  ctx.strokeStyle = 'rgba(255,255,255,0.95)'
  ctx.lineWidth = Math.max(1.5, w * 0.05)
  ctx.stroke()

  // ─ 灯臂 ─
  const stem = ctx.createLinearGradient(cx, armY, cx, h)
  stem.addColorStop(0, color)
  stem.addColorStop(1, color + '88')
  ctx.fillStyle = stem
  roundRect(ctx, cx - armW / 2, armY, armW, armH, 3)

  // ─ 灯杆 ─
  roundRect(ctx, cx - poleW / 2, poleY, poleW, poleH, poleW / 2)

  ctx.fillStyle = color + '22'
  ctx.beginPath()
  ctx.ellipse(cx, h - 3, w * 0.2, 3, 0, 0, Math.PI * 2)
  ctx.fill()

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
    mapStyle: 'amap://styles/whitesmoke',
    zoom: 14,
    center: [106.5622, 29.5621],
    animateEnable: true,
    resizeEnable: true,
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
    <div class="dm-hud">
      <div class="dm-hud-title">
        <span class="dm-pulse-dot"></span>
        <span>LIGHT GRID</span>
      </div>
      <div class="dm-hud-stats">
        <span><b>{{ mapStats.visible }}</b> 点位</span>
        <span><b>{{ mapStats.online }}</b> 在线</span>
        <span><b>{{ mapStats.warning }}</b> 异常</span>
      </div>
    </div>
    <div v-if="!loaded" class="dm-overlay">
      <span v-if="loading">地图加载中...</span>
      <span v-else-if="error" class="dm-error">{{ error }}</span>
    </div>
    <div ref="mapContainerRef" class="dm-container"></div>
    <div class="dm-legend">
      <span><i class="online"></i>在线</span>
      <span><i class="warning"></i>异常</span>
      <span><i class="offline"></i>离线/停用</span>
    </div>
    <div v-if="noLocationCount > 0" class="dm-no-loc-hint">
      {{ noLocationCount }} 台设备无位置信息，未在地图上显示
    </div>
  </div>
</template>

<style>
.dm-iw {
  min-width: 176px;
  padding: 8px 10px;
  background: rgba(255,255,255,0.96);
  border: 1px solid rgba(0,141,230,0.18);
  border-radius: 8px;
  color: #1d3148;
  font-size: 12px;
  line-height: 1.9;
  box-shadow: 0 16px 34px rgba(30, 86, 130, 0.18);
  backdrop-filter: blur(16px);
}
.dm-iw-name {
  font-size: 14px; font-weight: 600; margin-bottom: 2px;
  color: #0d1b2d; border-bottom: 1px solid rgba(0,141,230,0.16); padding-bottom: 4px;
}
.dm-iw-row { display: flex; justify-content: space-between; gap: 14px; padding: 0 2px; }
.dm-iw-link { display: inline-block; margin-top: 6px; color: #006fc2; cursor: pointer; font-size: 12px; font-weight: 600; }
.dm-iw-link:hover { text-decoration: underline; color: #008de6; }
.amap-info-content {
  padding: 0 !important;
  background: transparent !important;
  border-radius: 8px !important;
  box-shadow: none !important;
}
.amap-info-close {
  top: 8px !important;
  right: 8px !important;
  color: #40566f !important;
}
</style>

<style scoped>
.dm-wrapper {
  position: relative;
  width: 100%;
  border-radius: 14px;
  overflow: hidden;
  background:
    linear-gradient(135deg, rgba(0,141,230,0.18), rgba(22,199,232,0.08)),
    #f8fcff;
  border: 1px solid rgba(0,141,230,0.2);
  box-shadow:
    0 22px 52px rgba(30, 86, 130, 0.16),
    inset 0 1px 0 rgba(255,255,255,0.95);
}
.dm-wrapper::before {
  content: "";
  position: absolute;
  inset: 12px;
  z-index: 3;
  pointer-events: none;
  border: 1px solid rgba(0,141,230,0.14);
  border-radius: 10px;
  box-shadow: inset 0 0 34px rgba(0,141,230,0.08);
}
.dm-wrapper::after {
  content: "";
  position: absolute;
  inset: 0;
  z-index: 2;
  pointer-events: none;
  background:
    linear-gradient(90deg, transparent, rgba(0,141,230,0.16), transparent) 0 0 / 100% 2px no-repeat,
    radial-gradient(circle at 18% 12%, rgba(22,199,232,0.12), transparent 28%),
    radial-gradient(circle at 78% 82%, rgba(0,141,230,0.12), transparent 30%);
  animation: dm-scan 5s ease-in-out infinite;
}
.dm-container {
  width: 100%;
  height: 100%;
  border-radius: 14px;
  overflow: hidden;
  filter: saturate(1.08) contrast(1.02);
}
.dm-overlay {
  position: absolute; inset: 0; z-index: 8;
  display: flex; align-items: center; justify-content: center;
  background: rgba(255,255,255,0.78);
  color: #40566f;
  font-size: 13px;
  backdrop-filter: blur(10px);
}
.dm-error { color: #ef5350; }
.dm-hud {
  position: absolute;
  top: 22px;
  left: 22px;
  z-index: 6;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 9px 12px;
  border: 1px solid rgba(0,141,230,0.2);
  border-radius: 8px;
  color: #0d1b2d;
  background: rgba(255,255,255,0.86);
  box-shadow: 0 12px 28px rgba(30,86,130,0.12);
  backdrop-filter: blur(14px) saturate(1.2);
}
.dm-hud-title {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 12px;
  font-weight: 800;
  color: #006fc2;
}
.dm-pulse-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 0 4px rgba(16,185,129,0.16), 0 0 16px rgba(16,185,129,0.65);
}
.dm-hud-stats {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #40566f;
  font-size: 12px;
}
.dm-hud-stats b {
  color: #0d1b2d;
  font-size: 13px;
}
.dm-legend {
  position: absolute;
  right: 22px;
  bottom: 22px;
  z-index: 6;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border: 1px solid rgba(0,141,230,0.18);
  border-radius: 8px;
  background: rgba(255,255,255,0.86);
  box-shadow: 0 12px 28px rgba(30,86,130,0.12);
  backdrop-filter: blur(14px) saturate(1.2);
}
.dm-legend span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: #40566f;
  font-size: 12px;
  white-space: nowrap;
}
.dm-legend i {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  box-shadow: 0 0 12px currentColor;
}
.dm-legend i.online { background: #10b981; color: #10b981; }
.dm-legend i.warning { background: #f59e0b; color: #f59e0b; }
.dm-legend i.offline { background: #7b8794; color: #7b8794; }
.dm-no-loc-hint {
  position: absolute;
  left: 50%;
  bottom: 22px;
  z-index: 7;
  transform: translateX(-50%);
  padding: 7px 12px;
  text-align: center;
  font-size: 12px;
  color: #9a6500;
  background: rgba(255,248,229,0.92);
  border: 1px solid rgba(245,158,11,0.28);
  border-radius: 999px;
  box-shadow: 0 10px 24px rgba(154,101,0,0.12);
  backdrop-filter: blur(12px);
}
@keyframes dm-scan {
  0%, 100% { background-position: 0 18%, 0 0, 0 0; opacity: 0.85; }
  50% { background-position: 0 82%, 0 0, 0 0; opacity: 1; }
}

@media (max-width: 900px) {
  .dm-hud {
    right: 18px;
    flex-wrap: wrap;
  }
  .dm-legend {
    left: 18px;
    right: auto;
    flex-wrap: wrap;
  }
}
</style>
