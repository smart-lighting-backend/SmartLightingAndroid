<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchDeviceDetail, fetchLatestTelemetry, STATUS_MAP } from '../api/devices.js'

const route   = useRoute()
const router  = useRouter()
const device  = ref(null)
const telemetry = ref(null)
const loading = ref(true)

onMounted(async () => {
  const deviceId = route.params.id
  try {
    const [devRes, telRes] = await Promise.allSettled([
      fetchDeviceDetail(deviceId),
      fetchLatestTelemetry(deviceId),
    ])
    // 兼容直接返回对象或 { data: ... } 格式
    const devData = devRes.status === 'fulfilled' ? (devRes.value?.data ?? devRes.value) : null
    device.value  = devData
    const telData = telRes.status === 'fulfilled' ? (telRes.value?.data ?? telRes.value) : null
    telemetry.value = telData
  } finally {
    loading.value = false
  }
})

function healthColor(score) {
  if (!score && score !== 0) return '#4dd0e1'
  if (score >= 80) return '#4caf50'
  if (score >= 60) return '#ffa726'
  return '#ef5350'
}

function aqiLabel(aqi) {
  if (!aqi) return '--'
  if (aqi <= 50)  return '优'
  if (aqi <= 100) return '良'
  if (aqi <= 150) return '轻度'
  if (aqi <= 200) return '中度'
  return '重度'
}

function aqiClass(aqi) {
  if (!aqi) return ''
  if (aqi <= 50)  return 'excellent'
  if (aqi <= 100) return 'good'
  if (aqi <= 150) return 'mild'
  return 'heavy'
}

function formatCoord(location) {
  if (!location) return '未知位置'
  // 如果是经纬度格式 "106.5622,29.5621"，转为可读文字
  if (/^[\d.,]+$/.test(location)) return `${location}（经纬度）`
  return location
}
</script>

<template>
  <div class="detail-page">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <button class="back-btn" @click="router.push('/devices')">
        <svg viewBox="0 0 24 24" fill="none"><path d="M19 12H5M12 5l-7 7 7 7" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
      </button>
      <span class="bc-link" @click="router.push('/devices')">设备管理</span>
      <span class="bc-sep">›</span>
      <span class="bc-cur">{{ route.params.id }} 详情</span>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div> 加载设备信息...
    </div>

    <template v-else-if="device">
      <!-- 设备头部信息卡 -->
      <div class="device-header-card">
        <div class="device-icon-wrap">
          <svg viewBox="0 0 24 24" fill="none"><path d="M12 2C8.13 2 5 5.13 5 9c0 2.38 1.19 4.47 3 5.74V17c0 .55.45 1 1 1h6c.55 0 1-.45 1-1v-2.26c1.81-1.27 3-3.36 3-5.74 0-3.87-3.13-7-7-7z" fill="currentColor"/><path d="M9 21c0 .55.45 1 1 1h4c.55 0 1-.45 1-1v-1H9v1z" fill="currentColor" opacity="0.6"/></svg>
        </div>
        <div class="device-meta">
          <h1 class="device-name">{{ device.name }} ({{ device.deviceId }})</h1>
          <div class="device-tags">
            <span class="tag">
              <svg viewBox="0 0 24 24" fill="none" width="12" height="12"><path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z" fill="currentColor"/></svg>
              {{ formatCoord(device.location) }}
            </span>
            <span class="tag">
              <svg viewBox="0 0 24 24" fill="none" width="12" height="12"><rect x="2" y="2" width="20" height="20" rx="2" fill="none" stroke="currentColor" stroke-width="1.5"/><path d="M8 12h8M12 8v8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
              区域: {{ device.area }}
            </span>
            <span class="tag" v-if="device.topicPrefix">
              <svg viewBox="0 0 24 24" fill="none" width="12" height="12"><path d="M22 16.92v3a2 2 0 01-2.18 2 19.79 19.79 0 01-8.63-3.07A19.5 19.5 0 013.07 9.81 19.79 19.79 0 01.02 1.18 2 2 0 012 0h3a2 2 0 012 1.72c.127.96.361 1.903.7 2.81a2 2 0 01-.45 2.11L6.91 7.91A16 16 0 0016.09 17.09l1.27-1.27a2 2 0 012.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0124 18.92z" stroke="currentColor" stroke-width="1.5"/></svg>
              MQTT: {{ device.topicPrefix }}/{{ device.deviceId }}
            </span>
          </div>
        </div>
        <div class="device-status-area">
          <div class="status-pill" :class="STATUS_MAP[device.status]?.cls">
            <span class="dot-pulse" :class="STATUS_MAP[device.status]?.cls"></span>
            {{ STATUS_MAP[device.status]?.label || '未知' }}
          </div>
          <div class="health-info">
            <div class="bi-label">设备健康分</div>
            <div class="bi-value" :style="{ color: healthColor(device.healthScore) }">
              {{ device.healthScore != null ? device.healthScore.toFixed(0) : '--' }}
            </div>
          </div>
        </div>
      </div>

      <!-- 传感器数据网格（来自遥测接口） -->
      <div v-if="telemetry" class="sensor-grid">
        <!-- 环境照度 -->
        <div class="sensor-card wide">
          <div class="sensor-header">
            <span class="sensor-icon sun">
              <svg viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="5" fill="currentColor"/><path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
            </span>
            <span class="sensor-title">环境照度</span>
            <span class="real-time-tag">实时</span>
          </div>
          <div class="sensor-main-value">
            <span class="big-num">{{ telemetry.illuminance ?? '--' }}</span>
            <span class="big-unit">Lux</span>
          </div>
          <div class="sensor-hint">
            PIR感应：{{ telemetry.pir === 1 ? '检测到人体' : '无人' }}
          </div>
        </div>

        <!-- 设备温湿度 -->
        <div class="sensor-card">
          <div class="sensor-header">
            <span class="sensor-icon temp">
              <svg viewBox="0 0 24 24" fill="none"><path d="M12 2a2 2 0 00-2 2v10.58A4 4 0 1014 16V4a2 2 0 00-2-2z" fill="currentColor" opacity="0.3" stroke="currentColor" stroke-width="1.5"/></svg>
            </span>
            <span class="sensor-title">温湿度</span>
          </div>
          <div class="temp-row">
            <div>
              <div class="temp-label">温度</div>
              <div class="temp-val">{{ telemetry.temperature ?? '--' }}°C</div>
            </div>
            <div>
              <div class="temp-label">湿度</div>
              <div class="temp-val">{{ telemetry.humidity ?? '--' }}%</div>
            </div>
          </div>
          <div class="temp-status normal">
            <svg viewBox="0 0 24 24" fill="none" width="13" height="13"><path d="M20 6L9 17l-5-5" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
            散热正常
          </div>
        </div>

        <!-- 健康评估 -->
        <div class="sensor-card health-card">
          <div class="sensor-header">
            <span class="sensor-icon shield">
              <svg viewBox="0 0 24 24" fill="none"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" fill="currentColor" opacity="0.15" stroke="currentColor" stroke-width="1.5"/></svg>
            </span>
            <span class="sensor-title">设备健康评估</span>
          </div>
          <div class="health-circle-wrap">
            <svg class="health-circle" viewBox="0 0 80 80">
              <circle cx="40" cy="40" r="34" fill="none" stroke="rgba(0,80,140,0.3)" stroke-width="6"/>
              <circle
                cx="40" cy="40" r="34" fill="none"
                :stroke="healthColor(device.healthScore)" stroke-width="6"
                stroke-linecap="round"
                :stroke-dasharray="`${(device.healthScore ?? 0) / 100 * 213.6} 213.6`"
                stroke-dashoffset="53.4"
                style="transition:stroke-dasharray 1s ease"
              />
            </svg>
            <div class="health-score-inner">
              <span class="health-score" :style="{ color: healthColor(device.healthScore) }">
                {{ device.healthScore != null ? device.healthScore.toFixed(0) : '--' }}
              </span>
              <span class="health-sub">综合评分</span>
            </div>
          </div>
        </div>

        <!-- 车流量 -->
        <div class="sensor-card wide">
          <div class="sensor-header">
            <span class="sensor-icon car">
              <svg viewBox="0 0 24 24" fill="none"><path d="M5 17H3a2 2 0 01-2-2V9a2 2 0 012-2h14a2 2 0 012 2v6a2 2 0 01-2 2h-2M5 17a2 2 0 104 0 2 2 0 00-4 0zm10 0a2 2 0 104 0 2 2 0 00-4 0zM3 9h18" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
            </span>
            <span class="sensor-title">车流量（近期）</span>
            <span class="trend-up">↗</span>
          </div>
          <div class="sensor-main-value">
            <span class="big-num">{{ telemetry.trafficFlow ?? '--' }}</span>
            <span class="big-unit">辆</span>
          </div>
          <div class="sensor-hint">
            数据采集时间：{{ telemetry.collectedAt?.replace('T', ' ').slice(0, 16) || '--' }}
          </div>
        </div>

        <!-- PM2.5 / AQI -->
        <div class="sensor-card">
          <div class="sensor-header">
            <span class="sensor-icon wind">
              <svg viewBox="0 0 24 24" fill="none"><path d="M9.59 4.59A2 2 0 1111 8H2m10.59 11.41A2 2 0 1114 16H2m15.73-8.27A2.5 2.5 0 1119.5 12H2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
            </span>
            <span class="sensor-title">PM2.5 / AQI</span>
          </div>
          <div class="sensor-main-value">
            <span class="big-num">{{ telemetry.pm25 ?? '--' }}</span>
            <span class="big-unit">μg/m³</span>
          </div>
          <div class="air-quality-tag">
            空气质量：
            <span class="aq-badge" :class="aqiClass(telemetry.aqi)">{{ aqiLabel(telemetry.aqi) }}</span>
            <span class="aqi-val">AQI {{ telemetry.aqi ?? '--' }}</span>
          </div>
        </div>
      </div>

      <!-- 无遥测数据时的提示 -->
      <div v-else class="no-telemetry">
        <svg viewBox="0 0 24 24" fill="none" width="32" height="32"><circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="1.5" opacity="0.4"/><path d="M12 8v4M12 16v.01" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.4"/></svg>
        <p>该设备暂无遥测数据，可能尚未上报或设备离线</p>
      </div>

      <!-- 设备基本信息表格 -->
      <div class="info-card">
        <div class="info-header">设备台账信息</div>
        <div class="info-grid">
          <div class="info-row"><span class="info-key">设备编号</span><span class="info-val mono">{{ device.deviceId }}</span></div>
          <div class="info-row"><span class="info-key">设备名称</span><span class="info-val">{{ device.name }}</span></div>
          <div class="info-row"><span class="info-key">所属区域</span><span class="info-val">{{ device.area }}</span></div>
          <div class="info-row"><span class="info-key">安装位置</span><span class="info-val mono">{{ device.location || '--' }}</span></div>
          <div class="info-row"><span class="info-key">MQTT主题</span><span class="info-val mono">{{ device.topicPrefix }}/{{ device.deviceId }}</span></div>
          <div class="info-row"><span class="info-key">最后心跳</span><span class="info-val">{{ device.lastHeartbeatAt?.replace('T', ' ').slice(0,16) || '--' }}</span></div>
          <div class="info-row"><span class="info-key">是否启用</span><span class="info-val" :class="device.enabled ? 'text-green' : 'text-gray'">{{ device.enabled ? '是' : '否' }}</span></div>
          <div class="info-row"><span class="info-key">设备状态</span><span class="info-val" :class="STATUS_MAP[device.status]?.cls">{{ STATUS_MAP[device.status]?.label || '未知' }}</span></div>
        </div>
      </div>
    </template>

    <div v-else class="error-state">
      <p>设备 {{ route.params.id }} 不存在或已被删除</p>
      <button class="back-btn-lg" @click="router.push('/devices')">← 返回设备列表</button>
    </div>
  </div>
</template>

<style scoped>
.detail-page { padding: 20px 28px 28px; }
.breadcrumb { display: flex; align-items: center; gap: 8px; margin-bottom: 20px; }
.back-btn { width: 32px; height: 32px; background: rgba(0,80,140,0.2); border: 1px solid rgba(0,120,200,0.25); border-radius: 7px; color: rgba(140,190,220,0.8); cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all 0.2s; }
.back-btn:hover { background: rgba(0,120,200,0.25); color: #4dd0e1; }
.back-btn svg { width: 16px; height: 16px; }
.bc-link { font-size: 13px; color: rgba(140,190,220,0.7); cursor: pointer; }
.bc-link:hover { color: #4dd0e1; }
.bc-sep { color: rgba(140,190,220,0.4); font-size: 13px; }
.bc-cur { font-size: 13px; color: #d0eaf8; font-weight: 500; }
.loading-state { display: flex; align-items: center; gap: 10px; padding: 60px; color: rgba(140,190,220,0.5); justify-content: center; }
.loading-spinner { width: 18px; height: 18px; border: 2px solid rgba(77,208,225,0.3); border-top-color: #4dd0e1; border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* Header */
.device-header-card { background: rgba(8,20,45,0.85); border: 1px solid rgba(0,120,200,0.2); border-radius: 12px; padding: 20px 24px; display: flex; align-items: center; gap: 16px; margin-bottom: 16px; }
.device-icon-wrap { width: 52px; height: 52px; background: rgba(0,120,200,0.15); border: 1px solid rgba(77,208,225,0.25); border-radius: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.device-icon-wrap svg { width: 28px; height: 28px; color: #4dd0e1; }
.device-meta { flex: 1; }
.device-name { font-size: 18px; font-weight: 700; color: #e0f4ff; margin-bottom: 8px; }
.device-tags { display: flex; gap: 12px; flex-wrap: wrap; }
.tag { display: flex; align-items: center; gap: 4px; font-size: 12px; color: rgba(140,190,220,0.65); }
.device-status-area { text-align: right; flex-shrink: 0; }
.status-pill { display: inline-flex; align-items: center; gap: 6px; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 600; margin-bottom: 8px; }
.status-pill.online { background: rgba(0,200,100,0.12); border: 1px solid rgba(0,200,100,0.3); color: #4caf82; }
.status-pill.offline { background: rgba(100,100,100,0.1); border: 1px solid rgba(120,120,120,0.2); color: #9e9e9e; }
.status-pill.warning { background: rgba(255,167,38,0.12); border: 1px solid rgba(255,167,38,0.3); color: #ffa726; }
.status-pill.disabled { background: rgba(80,80,80,0.1); border: 1px solid rgba(100,100,100,0.2); color: #777; }
.dot-pulse { width: 6px; height: 6px; border-radius: 50%; background: currentColor; }
.dot-pulse.online { box-shadow: 0 0 6px currentColor; animation: blink 2s infinite; }
@keyframes blink { 0%,100%{opacity:1} 50%{opacity:0.4} }
.health-info { text-align: right; }
.bi-label { font-size: 11px; color: rgba(140,190,220,0.6); }
.bi-value { font-size: 28px; font-weight: 700; line-height: 1.2; }

/* Sensor Grid */
.sensor-grid { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 14px; margin-bottom: 16px; }
.sensor-card { background: rgba(8,20,45,0.8); border: 1px solid rgba(0,120,200,0.15); border-radius: 10px; padding: 16px 18px; }
.sensor-header { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.sensor-icon { width: 28px; height: 28px; border-radius: 6px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.sensor-icon svg { width: 16px; height: 16px; }
.sensor-icon.sun   { background: rgba(255,165,30,0.15); color: #ffa726; }
.sensor-icon.temp  { background: rgba(77,208,225,0.12); color: #4dd0e1; }
.sensor-icon.shield{ background: rgba(120,80,240,0.12); color: #9c6fe4; }
.sensor-icon.car   { background: rgba(30,150,255,0.12); color: #42a5f5; }
.sensor-icon.wind  { background: rgba(0,200,180,0.12); color: #26c6da; }
.sensor-title { font-size: 13px; color: rgba(160,210,235,0.8); font-weight: 500; flex: 1; }
.real-time-tag { padding: 2px 7px; background: rgba(0,200,100,0.12); border: 1px solid rgba(0,200,100,0.3); border-radius: 10px; font-size: 10px; color: #4caf82; }
.trend-up { color: #4caf50; font-size: 16px; font-weight: 700; }
.sensor-main-value { display: flex; align-items: baseline; gap: 4px; margin-bottom: 6px; }
.big-num { font-size: 36px; font-weight: 700; color: #e0f4ff; line-height: 1; }
.big-unit { font-size: 14px; color: rgba(140,190,220,0.6); }
.sensor-hint { font-size: 12px; color: rgba(140,190,220,0.55); }
.temp-row { display: flex; gap: 20px; margin-bottom: 10px; }
.temp-label { font-size: 11px; color: rgba(140,190,220,0.6); margin-bottom: 3px; }
.temp-val { font-size: 22px; font-weight: 700; color: #e0f4ff; }
.temp-status { display: flex; align-items: center; gap: 5px; font-size: 12px; }
.temp-status.normal { color: #4caf82; }
.health-circle-wrap { position: relative; width: 90px; height: 90px; margin: 4px auto 8px; }
.health-circle { width: 100%; height: 100%; transform: rotate(-90deg); }
.health-score-inner { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; }
.health-score { font-size: 24px; font-weight: 700; line-height: 1; }
.health-sub { font-size: 10px; color: rgba(140,190,220,0.6); margin-top: 2px; }
.air-quality-tag { font-size: 12px; color: rgba(140,190,220,0.65); margin-top: 4px; display: flex; align-items: center; gap: 6px; }
.aq-badge { padding: 1px 8px; border-radius: 10px; font-size: 11px; font-weight: 600; }
.aq-badge.excellent { background: rgba(0,200,100,0.15); border: 1px solid rgba(0,200,100,0.3); color: #4caf82; }
.aq-badge.good      { background: rgba(77,208,225,0.12); border: 1px solid rgba(77,208,225,0.25); color: #4dd0e1; }
.aq-badge.mild      { background: rgba(255,167,38,0.15); border: 1px solid rgba(255,167,38,0.3); color: #ffa726; }
.aq-badge.heavy     { background: rgba(220,50,50,0.15); border: 1px solid rgba(220,80,80,0.3); color: #ff7070; }
.aqi-val { font-size: 11px; color: rgba(140,190,220,0.5); }

/* No telemetry */
.no-telemetry { background: rgba(8,20,45,0.5); border: 1px dashed rgba(0,100,160,0.2); border-radius: 10px; padding: 30px; text-align: center; color: rgba(140,190,220,0.5); margin-bottom: 16px; display: flex; align-items: center; justify-content: center; gap: 12px; }
.no-telemetry p { font-size: 13px; }

/* Info card */
.info-card { background: rgba(8,20,45,0.8); border: 1px solid rgba(0,120,200,0.15); border-radius: 10px; overflow: hidden; }
.info-header { padding: 12px 18px; font-size: 14px; font-weight: 600; color: #d0eaf8; border-bottom: 1px solid rgba(0,80,140,0.15); }
.info-grid { display: grid; grid-template-columns: 1fr 1fr; }
.info-row { display: flex; align-items: center; gap: 12px; padding: 11px 18px; border-bottom: 1px solid rgba(0,60,120,0.1); }
.info-row:last-child, .info-row:nth-last-child(2) { border-bottom: none; }
.info-key { font-size: 12px; color: rgba(140,190,220,0.6); min-width: 70px; flex-shrink: 0; }
.info-val { font-size: 13px; color: rgba(200,230,245,0.9); }
.info-val.mono { font-family: monospace; font-size: 12px; color: rgba(140,190,220,0.8); }
.info-val.online { color: #4caf82; }
.info-val.offline { color: #9e9e9e; }
.info-val.warning { color: #ffa726; }
.info-val.text-green { color: #4caf82; }
.info-val.text-gray { color: #777; }

/* Error */
.error-state { text-align: center; padding: 60px; color: rgba(140,190,220,0.5); }
.back-btn-lg { margin-top: 16px; padding: 10px 24px; background: rgba(0,80,140,0.2); border: 1px solid rgba(0,120,200,0.3); border-radius: 8px; color: rgba(140,200,230,0.9); font-size: 13px; cursor: pointer; transition: all 0.2s; }
.back-btn-lg:hover { background: rgba(0,120,200,0.2); color: #4dd0e1; }
</style>
