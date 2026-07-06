<script setup>
import { ref, computed, watch, onUnmounted, nextTick } from 'vue'
import { useAMap } from '../composables/useAMap.js'
import { parseLocation } from '../utils/coordinate.js'

const props = defineProps({
  modelValue: { type: Object, default: () => ({ lng: '', lat: '' }) },
  visible: { type: Boolean, default: false },
  devices: { type: Array, default: () => [] },
})

const emit = defineEmits(['update:modelValue', 'update:visible', 'confirm'])

const { AMap: AMapRef, loaded, loading, error } = useAMap()
const mapContainerRef = ref(null)

const currentLng = ref('')
const currentLat = ref('')
const confirmMsg = ref('')

const searchDeviceId = ref('')
const addressInput = ref('')
const addressResults = ref([])
const searching = ref(false)

let map = null
let pinMarker = null       // 红色大头针（新选点）
let origMarker = null      // 粉色圆点（原始位置）
let deviceMarkers = []     // 已有设备标记
let geocoder = null

const COLORS = { 0: '#888', 1: '#4caf82', 2: '#9e9e9e', 3: '#ffa726' }
const deviceOptions = computed(() =>
  props.devices.map(d => ({ value: d.deviceId, label: `${d.name || d.deviceId} (${d.deviceId})` }))
)

// ── 选点标记 ──
function updateCoords(lng, lat) {
  const a = Number(lng), b = Number(lat)
  if (isNaN(a) || isNaN(b)) return
  confirmMsg.value = ''
  currentLng.value = a.toFixed(6)
  currentLat.value = b.toFixed(6)
  placePin(a, b)
}

function placePin(lng, lat) {
  if (!map || !AMapRef.value) return
  if (pinMarker) {
    pinMarker.setPosition([lng, lat])
  } else {
    const html = `<div style="width:24px;height:34px;position:relative">
      <div style="position:absolute;bottom:0;left:50%;margin-left:-10px;width:20px;height:20px;background:#ff4444;border:2.5px solid #fff;border-radius:50%;box-shadow:0 2px 10px rgba(255,50,50,0.5);"></div>
      <div style="position:absolute;bottom:17px;left:50%;margin-left:-2px;width:4px;height:14px;background:#cc0000;border-radius:1px;"></div>
    </div>`
    pinMarker = new AMapRef.value.Marker({
      position: [lng, lat],
      content: html,
      anchor: 'bottom-center',
      zIndex: 300,
    })
    pinMarker.setMap(map)
  }
}

// 原始位置标记——粉色圆点，编辑模式下显示设备原来在哪
function placeOrigMarker(lng, lat) {
  if (!map || !AMapRef.value) return
  if (origMarker) origMarker.setMap(null)
  const html = `<div title="原始位置" style="width:16px;height:16px;background:#ff69b4;border:2.5px solid #fff;border-radius:50%;box-shadow:0 0 12px rgba(255,105,180,0.6);"></div>`
  origMarker = new AMapRef.value.Marker({
    position: [lng, lat],
    content: html,
    anchor: "center",
    zIndex: 250,
  })
  origMarker.setMap(map)
}

function onMapClick(e) {
  updateCoords(e.lnglat.getLng(), e.lnglat.getLat())
}

// ── 已有设备标记 ──
function addDeviceMarkers() {
  deviceMarkers.forEach(m => m.setMap(null))
  deviceMarkers = []

  props.devices.forEach(d => {
    const pos = parseLocation(d.location)
    if (!pos) return
    const color = COLORS[d.status] || '#888'
    const html = `<div title="${d.name || d.deviceId}" style="width:11px;height:11px;background:${color};border:2px solid rgba(255,255,255,0.8);border-radius:50%;box-shadow:0 0 5px ${color};cursor:pointer;"></div>`
    const m = new AMapRef.value.Marker({
      position: [pos.lng, pos.lat],
      content: html,
      anchor: 'center',
      zIndex: 100,
    })
    m.on('click', () => {
      updateCoords(pos.lng, pos.lat)
      map.setZoomAndCenter(17, [pos.lng, pos.lat])
    })
    m.setMap(map)
    deviceMarkers.push(m)
  })
}

// ── 设备搜索 ──
function onDeviceSelect(deviceId) {
  const d = props.devices.find(x => x.deviceId === deviceId)
  if (!d) return
  const pos = parseLocation(d.location)
  if (!pos) return
  updateCoords(pos.lng, pos.lat)
  map.setZoomAndCenter(17, [pos.lng, pos.lat])
}

// ── 地址搜索 ──
let searchTimer = null
function onAddressInput() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(doAddressSearch, 400)
}
async function doAddressSearch() {
  const q = addressInput.value.trim()
  if (!q || q.length < 2) { addressResults.value = []; return }
  if (!geocoder && AMapRef.value) {
    await new Promise(resolve => {
      AMapRef.value.plugin('AMap.Geocoder', () => {
        geocoder = new AMapRef.value.Geocoder({ city: '全国' })
        resolve()
      })
    })
  }
  if (!geocoder) return
  searching.value = true
  geocoder.getLocation(q, (status, result) => {
    searching.value = false
    if (status === 'complete' && result.info === 'OK') {
      addressResults.value = result.geocodes.map(g => ({
        label: g.formattedAddress || g.name,
        lng: g.location.getLng(),
        lat: g.location.getLat(),
      }))
    } else {
      addressResults.value = []
    }
  })
}
function selectAddress(item) {
  updateCoords(item.lng, item.lat)
  map.setZoomAndCenter(17, [item.lng, item.lat])
  addressResults.value = []
  addressInput.value = ''
}

// ── 确认 / 取消 ──
function confirm() {
  if (!currentLng.value || !currentLat.value) {
    confirmMsg.value = '请在地图上点击选择设备安装位置'
    return
  }
  emit('update:modelValue', { lng: currentLng.value, lat: currentLat.value })
  emit('confirm', { lng: currentLng.value, lat: currentLat.value })
  emit('update:visible', false)
}
function cancel() { emit('update:visible', false) }

// ── 地图初始化 ──
function initMap() {
  if (!mapContainerRef.value || !AMapRef.value || map) return
  map = new AMapRef.value.Map(mapContainerRef.value, { mapStyle: 'amap://styles/dark', zoom: 5, center: [104, 35] })
  map.on('click', onMapClick)

  addDeviceMarkers()

  const iLng = parseFloat(props.modelValue.lng)
  const iLat = parseFloat(props.modelValue.lat)
  if (!isNaN(iLng) && !isNaN(iLat)) {
    placeOrigMarker(iLng, iLat)
    updateCoords(iLng, iLat)
    map.setZoomAndCenter(17, [iLng, iLat])
  }
}

watch(loaded, (ok) => { if (ok) nextTick(initMap) })
watch(() => props.visible, (v) => {
  if (v) {
    currentLng.value = props.modelValue.lng || ''
    currentLat.value = props.modelValue.lat || ''
    confirmMsg.value = ''
    addressInput.value = ''
    addressResults.value = []
    searchDeviceId.value = ''
    nextTick(() => { if (loaded.value) { initMap(); addDeviceMarkers() } })
  }
})

onUnmounted(() => {
  deviceMarkers.forEach(m => m.setMap(null))
  deviceMarkers = []
  if (origMarker) { origMarker.setMap(null); origMarker = null }
  if (pinMarker) { pinMarker.setMap(null); pinMarker = null }
  if (map) { map.destroy(); map = null }
})
</script>

<template>
  <Teleport to="body">
    <div v-if="visible" class="lp-overlay">
      <!-- 顶部工具栏 -->
      <div class="lp-topbar">
        <span class="lp-title">地图选点 — 点击地图放置路灯位置</span>
        <div class="lp-search-group">
          <el-select
            v-model="searchDeviceId" filterable clearable
            placeholder="搜索已有设备..." size="small" class="lp-device-select"
            @change="onDeviceSelect"
          >
            <el-option v-for="opt in deviceOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <div class="lp-addr-wrap">
            <input v-model="addressInput" class="lp-addr-input"
              placeholder="输入地址搜索，如 重庆市渝中区..." @input="onAddressInput" />
            <div v-if="addressResults.length" class="lp-addr-results">
              <div v-for="(item, i) in addressResults" :key="i" class="lp-addr-item" @click="selectAddress(item)">
                📍 {{ item.label }}
              </div>
            </div>
            <span v-if="searching" class="lp-searching">搜索中...</span>
          </div>
        </div>
        <div class="lp-top-actions">
          <button class="lp-btn-cancel" @click="cancel">取消</button>
          <button class="lp-btn-confirm" @click="confirm">确认选择</button>
        </div>
      </div>

      <!-- 地图 -->
      <div class="lp-map-wrap">
        <div v-if="!loaded" class="lp-loading">
          <span v-if="loading">地图加载中...</span>
          <span v-else-if="error">{{ error }}</span>
        </div>
        <div ref="mapContainerRef" class="lp-map"></div>
      </div>

      <!-- 底部坐标栏 -->
      <div class="lp-bottombar">
        <div class="lp-info">
          <svg viewBox="0 0 24 24" fill="none" width="18" height="18"><path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z" fill="#4dd0e1"/></svg>
          <template v-if="currentLng && currentLat">
            经度 <strong>{{ currentLng }}°E</strong> &nbsp; 纬度 <strong>{{ currentLat }}°N</strong>
          </template>
          <template v-else>点击地图放置标记，或搜索设备/地址定位</template>
        </div>
        <div class="lp-legend">
          <span class="lp-legend-item"><i style="background:#4caf82"></i> 在线</span>
          <span class="lp-legend-item"><i style="background:#9e9e9e"></i> 离线</span>
          <span class="lp-legend-item"><i style="background:#ffa726"></i> 异常</span>
          <span class="lp-legend-item"><i style="background:#ff4444"></i> 新选点</span>
        </div>
        <div v-if="confirmMsg" class="lp-warn">{{ confirmMsg }}</div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.lp-overlay {
  position: fixed; inset: 0; z-index: 3000;
  display: flex; flex-direction: column;
  background: #060e1f;
}

.lp-topbar {
  display: flex; align-items: center; gap: 14px;
  padding: 12px 20px; flex-shrink: 0;
  background: rgba(8,20,45,0.95);
  border-bottom: 1px solid rgba(0,120,200,0.2);
}
.lp-title { font-size: 15px; font-weight: 600; color: #d0eaf8; white-space: nowrap; }
.lp-search-group { display: flex; align-items: center; gap: 8px; flex: 1; }
.lp-device-select { width: 220px; }
.lp-device-select :deep(.el-input__wrapper) {
  background: rgba(0,30,70,0.6); border-color: rgba(0,120,200,0.25); box-shadow: none;
}
.lp-device-select :deep(.el-input__inner) { color: #d0eaf8; }

.lp-addr-wrap { flex: 1; position: relative; }
.lp-addr-input {
  width: 100%; padding: 6px 12px;
  background: rgba(0,30,70,0.6); border: 1px solid rgba(0,120,200,0.25);
  border-radius: 6px; color: #d0eaf8; font-size: 13px; outline: none;
}
.lp-addr-input::placeholder { color: rgba(100,160,200,0.4); }
.lp-addr-input:focus { border-color: rgba(77,208,225,0.5); }
.lp-addr-results {
  position: absolute; top: 100%; left: 0; right: 0; z-index: 20;
  background: #0d1b33; border: 1px solid rgba(0,120,200,0.3);
  border-radius: 6px; max-height: 200px; overflow-y: auto; margin-top: 4px;
}
.lp-addr-item {
  padding: 8px 12px; cursor: pointer; font-size: 13px; color: #d0eaf8;
}
.lp-addr-item:hover { background: rgba(0,100,180,0.2); }
.lp-searching { font-size: 12px; color: rgba(140,190,220,0.4); margin-top: 4px; display: block; }

.lp-top-actions { display: flex; gap: 8px; flex-shrink: 0; }
.lp-btn-cancel {
  padding: 8px 18px; border-radius: 6px; font-size: 13px;
  background: rgba(0,50,100,0.3); border: 1px solid rgba(0,100,160,0.3);
  color: rgba(180,210,230,0.7); cursor: pointer;
}
.lp-btn-cancel:hover { background: rgba(0,80,140,0.3); }
.lp-btn-confirm {
  padding: 8px 18px; border-radius: 6px; font-size: 13px;
  background: linear-gradient(135deg, #0077cc, #0099e6);
  border: none; color: #fff; cursor: pointer; font-weight: 500;
}
.lp-btn-confirm:hover { box-shadow: 0 2px 12px rgba(0,150,230,0.3); }

.lp-map-wrap { flex: 1; position: relative; }
.lp-map { width: 100%; height: 100%; }
.lp-loading {
  position: absolute; inset: 0; z-index: 2;
  display: flex; align-items: center; justify-content: center;
  background: rgba(8,20,45,0.6); color: rgba(140,190,220,0.6); font-size: 14px;
}

.lp-bottombar {
  padding: 10px 20px; flex-shrink: 0;
  background: rgba(8,20,45,0.95);
  border-top: 1px solid rgba(0,120,200,0.2);
  display: flex; align-items: center; gap: 20px;
}
.lp-info {
  display: flex; align-items: center; gap: 10px;
  font-size: 14px; color: rgba(180,210,240,0.7); flex: 1;
}
.lp-info strong { color: #4dd0e1; font-family: monospace; font-size: 15px; }

.lp-legend { display: flex; gap: 14px; flex-shrink: 0; }
.lp-legend-item { font-size: 12px; color: rgba(140,190,220,0.55); display: flex; align-items: center; gap: 4px; }
.lp-legend-item i { display: inline-block; width: 9px; height: 9px; border-radius: 50%; border: 1.5px solid rgba(255,255,255,0.7); }

.lp-warn { color: #ef5350; font-size: 13px; flex-shrink: 0; }
</style>
