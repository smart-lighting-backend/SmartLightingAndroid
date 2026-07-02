<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { fetchDeviceList, STATUS_MAP, STATUS_QUERY_MAP } from '../api/devices.js'

const router = useRouter()
const devices  = ref([])
const loading  = ref(false)
const search   = ref('')
const statusFilter = ref('全部')
const statuses = ['全部', '在线', '离线', '异常', '停用']

async function loadDevices() {
  loading.value = true
  try {
    const statusVal = STATUS_QUERY_MAP[statusFilter.value]
    const res = await fetchDeviceList({ status: statusVal })
    // 后端返回数组或 data 字段
    const raw = Array.isArray(res) ? res : (res.data || [])
    devices.value = raw
  } finally {
    loading.value = false
  }
}

onMounted(loadDevices)

const filtered = computed(() => {
  const kw = search.value.toLowerCase()
  return devices.value.filter(d => {
    const matchSearch = !kw || d.deviceId?.toLowerCase().includes(kw) || d.name?.toLowerCase().includes(kw) || d.area?.toLowerCase().includes(kw)
    if (statusFilter.value === '全部') return matchSearch
    const statusVal = STATUS_QUERY_MAP[statusFilter.value]
    return matchSearch && d.status === statusVal
  })
})

function doFilterChange() { loadDevices() }

function healthColor(score) {
  if (!score && score !== 0) return '#888'
  if (score >= 80) return '#4caf50'
  if (score >= 60) return '#ffa726'
  return '#ef5350'
}

// 格式化最后心跳时间
function formatTime(iso) {
  if (!iso) return '--'
  return iso.replace('T', ' ').slice(0, 16)
}
</script>

<template>
  <div class="devices-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">设备管理</h1>
        <p class="page-sub">管理全域智慧路灯节点设备</p>
      </div>
      <div class="header-actions">
        <div class="search-wrap">
          <svg viewBox="0 0 24 24" fill="none"><circle cx="11" cy="11" r="8" stroke="currentColor" stroke-width="1.5"/><path d="M21 21l-4.35-4.35" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
          <input v-model="search" class="search-input" placeholder="搜索设备ID、名称或区域" />
        </div>
        <div class="status-tabs">
          <button
            v-for="s in statuses" :key="s"
            class="status-tab"
            :class="{ active: statusFilter === s }"
            @click="statusFilter = s; doFilterChange()"
          >{{ s }}</button>
        </div>
      </div>
    </div>

    <!-- 统计条 -->
    <div class="summary-bar">
      <span class="summary-item">
        共 <strong>{{ filtered.length }}</strong> 台设备
      </span>
      <span class="summary-item online">
        在线 <strong>{{ filtered.filter(d=>d.status===1).length }}</strong>
      </span>
      <span class="summary-item offline">
        离线 <strong>{{ filtered.filter(d=>d.status===2).length }}</strong>
      </span>
      <span class="summary-item warning">
        异常 <strong>{{ filtered.filter(d=>d.status===3).length }}</strong>
      </span>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <span>加载设备列表...</span>
    </div>

    <div v-else-if="!filtered.length" class="empty-state">
      <svg viewBox="0 0 24 24" fill="none" width="40" height="40"><circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="1.5" opacity="0.4"/><path d="M12 8v4M12 16v.01" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.4"/></svg>
      <p>暂无符合条件的设备</p>
    </div>

    <div v-else class="device-grid">
      <div
        v-for="d in filtered"
        :key="d.deviceId"
        class="device-card"
        @click="router.push(`/devices/${d.deviceId}`)"
      >
        <div class="dc-header">
          <div class="dc-icon" :class="STATUS_MAP[d.status]?.cls">
            <svg viewBox="0 0 24 24" fill="none"><path d="M12 2C8.13 2 5 5.13 5 9c0 2.38 1.19 4.47 3 5.74V17c0 .55.45 1 1 1h6c.55 0 1-.45 1-1v-2.26c1.81-1.27 3-3.36 3-5.74 0-3.87-3.13-7-7-7z" fill="currentColor"/></svg>
          </div>
          <div class="status-pill" :class="STATUS_MAP[d.status]?.cls">
            <span class="dot"></span>
            {{ STATUS_MAP[d.status]?.label || '未知' }}
          </div>
        </div>
        <div class="dc-name">{{ d.name }}</div>
        <div class="dc-id">{{ d.deviceId }}</div>
        <div class="dc-location">
          <svg viewBox="0 0 24 24" fill="none" width="11" height="11"><path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z" fill="currentColor"/></svg>
          {{ d.area }}
        </div>
        <div class="dc-metrics">
          <div class="metric">
            <span class="metric-label">健康分</span>
            <span class="metric-val" :style="{ color: healthColor(d.healthScore) }">
              {{ d.healthScore != null ? d.healthScore.toFixed(0) : '--' }}
            </span>
          </div>
          <div class="metric">
            <span class="metric-label">启用</span>
            <span class="metric-val" :class="d.enabled ? 'enabled' : 'disabled-text'">{{ d.enabled ? '是' : '否' }}</span>
          </div>
          <div class="metric">
            <span class="metric-label">心跳</span>
            <span class="metric-val heartbeat">{{ formatTime(d.lastHeartbeatAt) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.devices-page { padding: 24px 28px; }
.page-header { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 16px; flex-wrap: wrap; gap: 12px; }
.page-title { font-size: 22px; font-weight: 700; color: #e0f4ff; margin-bottom: 4px; }
.page-sub { font-size: 13px; color: rgba(140,190,220,0.6); }
.header-actions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.search-wrap {
  display: flex; align-items: center; gap: 8px;
  background: rgba(0,20,50,0.7);
  border: 1px solid rgba(0,100,160,0.3);
  border-radius: 7px; padding: 0 12px; height: 36px;
}
.search-wrap svg { width: 14px; height: 14px; color: rgba(140,190,220,0.5); flex-shrink: 0; }
.search-input { background: none; border: none; color: #d0eaf8; font-size: 13px; outline: none; width: 180px; }
.search-input::placeholder { color: rgba(100,160,200,0.4); }
.status-tabs { display: flex; gap: 4px; }
.status-tab {
  padding: 6px 14px; background: rgba(0,30,70,0.5);
  border: 1px solid rgba(0,80,140,0.2); border-radius: 6px;
  color: rgba(140,190,220,0.7); font-size: 12px; cursor: pointer; transition: all 0.2s;
}
.status-tab.active { background: rgba(0,120,220,0.2); border-color: rgba(77,208,225,0.4); color: #4dd0e1; }

/* Summary */
.summary-bar { display: flex; gap: 20px; margin-bottom: 16px; }
.summary-item { font-size: 13px; color: rgba(140,190,220,0.6); }
.summary-item strong { font-weight: 700; color: rgba(200,230,245,0.9); margin: 0 2px; }
.summary-item.online strong { color: #4caf82; }
.summary-item.offline strong { color: #9e9e9e; }
.summary-item.warning strong { color: #ffa726; }

/* Loading/empty */
.loading-state { display: flex; align-items: center; justify-content: center; gap: 10px; padding: 60px; color: rgba(140,190,220,0.5); }
.loading-spinner { width: 20px; height: 20px; border: 2px solid rgba(77,208,225,0.3); border-top-color: #4dd0e1; border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px; color: rgba(140,190,220,0.5); gap: 12px; }
.empty-state p { font-size: 14px; }

/* Grid */
.device-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px,1fr)); gap: 14px; }
.device-card {
  background: rgba(8,20,45,0.8);
  border: 1px solid rgba(0,120,200,0.15);
  border-radius: 10px; padding: 16px 18px;
  cursor: pointer; transition: all 0.2s;
}
.device-card:hover { border-color: rgba(77,208,225,0.3); transform: translateY(-2px); box-shadow: 0 8px 24px rgba(0,0,0,0.3); }
.dc-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.dc-icon { width: 32px; height: 32px; border-radius: 7px; display: flex; align-items: center; justify-content: center; }
.dc-icon svg { width: 17px; height: 17px; }
.dc-icon.online  { background: rgba(76,175,80,0.15); color: #4caf50; }
.dc-icon.offline { background: rgba(100,100,100,0.15); color: #9e9e9e; }
.dc-icon.warning { background: rgba(255,167,38,0.15); color: #ffa726; }
.dc-icon.disabled { background: rgba(80,80,80,0.15); color: #666; }
.status-pill { display: flex; align-items: center; gap: 4px; padding: 2px 8px; border-radius: 10px; font-size: 11px; font-weight: 600; }
.status-pill.online  { background: rgba(0,200,100,0.12); border: 1px solid rgba(0,200,100,0.25); color: #4caf82; }
.status-pill.offline { background: rgba(100,100,100,0.15); border: 1px solid rgba(120,120,120,0.25); color: rgba(180,180,180,0.8); }
.status-pill.warning { background: rgba(255,167,38,0.12); border: 1px solid rgba(255,167,38,0.3); color: #ffa726; }
.status-pill.disabled { background: rgba(80,80,80,0.1); border: 1px solid rgba(100,100,100,0.2); color: #777; }
.dot { width: 5px; height: 5px; border-radius: 50%; background: currentColor; }
.dc-name { font-size: 13px; font-weight: 600; color: #d0eaf8; margin-bottom: 3px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.dc-id { font-size: 11px; font-family: monospace; color: rgba(100,180,220,0.6); margin-bottom: 6px; }
.dc-location { display: flex; align-items: center; gap: 4px; font-size: 11px; color: rgba(140,190,220,0.55); margin-bottom: 12px; }
.dc-metrics { display: flex; gap: 0; border-top: 1px solid rgba(0,80,140,0.15); padding-top: 10px; }
.metric { flex: 1; text-align: center; }
.metric:not(:last-child) { border-right: 1px solid rgba(0,80,140,0.12); }
.metric-label { display: block; font-size: 10px; color: rgba(140,190,220,0.5); margin-bottom: 3px; }
.metric-val { display: block; font-size: 13px; font-weight: 600; color: #d0eaf8; }
.metric-val.enabled { color: #4caf82; }
.metric-val.disabled-text { color: #777; }
.metric-val.heartbeat { font-size: 9px; font-family: monospace; color: rgba(140,190,220,0.6); font-weight: 400; }
</style>
