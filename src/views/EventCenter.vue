<script setup>
import { ref, onMounted } from 'vue'
import { fetchVisionEvents, fetchVoiceEvents } from '../api/events.js'
import { ElInput, ElSelect, ElOption, ElPagination, ElTag, ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'

const activeTab = ref('vision')
const loading = ref(false)
const visionList = ref([])
const voiceList = ref([])
const visionTotal = ref(0)
const voiceTotal = ref(0)
const visionPage = ref(1)
const voicePage = ref(1)
const pageSize = ref(20)

// 筛选条件
const filterDeviceId = ref('')
const filterVisionType = ref('')
const filterVoiceType = ref('')

async function loadVisionEvents() {
  loading.value = true
  try {
    const res = await fetchVisionEvents({
      page: visionPage.value,
      size: pageSize.value,
      deviceId: filterDeviceId.value || undefined,
      eventType: filterVisionType.value || undefined,
    })
    if (res && res.data) {
      visionList.value = res.data.records || []
      visionTotal.value = res.data.total || 0
    }
  } catch (e) {
    ElMessage.error('加载视觉事件失败')
  } finally {
    loading.value = false
  }
}

async function loadVoiceEvents() {
  loading.value = true
  try {
    const res = await fetchVoiceEvents({
      page: voicePage.value,
      size: pageSize.value,
      deviceId: filterDeviceId.value || undefined,
      type: filterVoiceType.value || undefined,
    })
    if (res && res.data) {
      voiceList.value = res.data.records || []
      voiceTotal.value = res.data.total || 0
    }
  } catch (e) {
    ElMessage.error('加载语音事件失败')
  } finally {
    loading.value = false
  }
}

function switchTab(tab) {
  activeTab.value = tab
  if (tab === 'vision') loadVisionEvents()
  else loadVoiceEvents()
}

function handleSearch() {
  visionPage.value = 1
  voicePage.value = 1
  if (activeTab.value === 'vision') loadVisionEvents()
  else loadVoiceEvents()
}

function visionTypeTag(type) {
  const map = { '行人检测': 'warning', '车辆通行': 'success', '异常停车': 'danger', '危险场景': 'danger' }
  return map[type] || 'info'
}

function voiceTypeTag(type) {
  const map = { '警告': 'danger', '播报': 'primary', '广播': 'info' }
  return map[type] || 'info'
}

function confidenceColor(val) {
  if (val >= 0.9) return '#4caf50'
  if (val >= 0.7) return '#ff9800'
  return '#f44336'
}

onMounted(loadVisionEvents)
</script>

<template>
  <div class="event-center-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">事件中心</h1>
        <p class="page-sub">查看视觉识别事件与语音告警播报记录</p>
      </div>
    </div>

    <!-- Tab 切换 -->
    <div class="tab-bar">
      <button class="tab-btn" :class="{ active: activeTab === 'vision' }" @click="switchTab('vision')">
        👁 视觉事件
      </button>
      <button class="tab-btn" :class="{ active: activeTab === 'voice' }" @click="switchTab('voice')">
        🔊 语音事件
      </button>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <ElInput v-model="filterDeviceId" placeholder="设备编号" clearable style="width: 180px" />
      <template v-if="activeTab === 'vision'">
        <ElSelect v-model="filterVisionType" placeholder="事件类型" clearable style="width: 140px">
          <ElOption label="行人检测" value="行人检测" />
          <ElOption label="车辆通行" value="车辆通行" />
          <ElOption label="异常停车" value="异常停车" />
          <ElOption label="危险场景" value="危险场景" />
        </ElSelect>
      </template>
      <template v-else>
        <ElSelect v-model="filterVoiceType" placeholder="语音类型" clearable style="width: 140px">
          <ElOption label="播报" value="播报" />
          <ElOption label="广播" value="广播" />
          <ElOption label="警告" value="警告" />
        </ElSelect>
      </template>
      <button class="search-btn" @click="handleSearch"><el-icon><Search /></el-icon> 查询</button>
    </div>

    <!-- 视觉事件表格 -->
    <div v-if="activeTab === 'vision'" class="table-wrap">
      <div v-if="loading" class="loading-state">加载中...</div>
      <table v-else class="event-table">
        <thead>
          <tr>
            <th>设备编号</th>
            <th>事件类型</th>
            <th>置信度</th>
            <th>截图引用</th>
            <th>发生时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="e in visionList" :key="e.id">
            <td class="td-device">{{ e.deviceId }}</td>
            <td><ElTag :type="visionTypeTag(e.eventType)" size="small">{{ e.eventType }}</ElTag></td>
            <td>
              <div class="confidence-cell">
                <div class="confidence-bar">
                  <div class="confidence-fill" :style="{ width: (e.confidence * 100) + '%', background: confidenceColor(e.confidence) }"></div>
                </div>
                <span class="confidence-pct">{{ (e.confidence * 100).toFixed(0) }}%</span>
              </div>
            </td>
            <td class="td-snapshot">{{ e.snapshotRef || '--' }}</td>
            <td>{{ e.occurredAt || '--' }}</td>
          </tr>
          <tr v-if="visionList.length === 0">
            <td colspan="5" class="empty-cell">暂无视觉事件</td>
          </tr>
        </tbody>
      </table>
      <div class="pagination-wrap" v-if="visionTotal > 0">
        <ElPagination v-model:current-page="visionPage" v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]" background layout="total, sizes, prev, pager, next"
          :total="visionTotal" @size-change="loadVisionEvents" @current-change="loadVisionEvents" />
      </div>
    </div>

    <!-- 语音事件表格 -->
    <div v-if="activeTab === 'voice'" class="table-wrap">
      <div v-if="loading" class="loading-state">加载中...</div>
      <table v-else class="event-table">
        <thead>
          <tr>
            <th>设备编号</th>
            <th>类型</th>
            <th>播报内容</th>
            <th>来源</th>
            <th>发生时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="e in voiceList" :key="e.id">
            <td class="td-device">{{ e.deviceId }}</td>
            <td><ElTag :type="voiceTypeTag(e.type)" size="small">{{ e.type }}</ElTag></td>
            <td class="td-content">{{ e.content }}</td>
            <td>{{ e.source }}</td>
            <td>{{ e.occurredAt || '--' }}</td>
          </tr>
          <tr v-if="voiceList.length === 0">
            <td colspan="5" class="empty-cell">暂无语音事件</td>
          </tr>
        </tbody>
      </table>
      <div class="pagination-wrap" v-if="voiceTotal > 0">
        <ElPagination v-model:current-page="voicePage" v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]" background layout="total, sizes, prev, pager, next"
          :total="voiceTotal" @size-change="loadVoiceEvents" @current-change="loadVoiceEvents" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.event-center-page { padding: 24px 28px; }
.page-title { font-size: 22px; font-weight: 700; color: #e0f4ff; margin-bottom: 4px; }
.page-sub { font-size: 13px; color: rgba(140,190,220,0.6); }

/* Tabs */
.tab-bar { display: flex; gap: 6px; margin: 20px 0 16px; }
.tab-btn {
  padding: 9px 22px;
  background: rgba(8,20,45,0.6);
  border: 1px solid rgba(0,120,200,0.2);
  border-radius: 8px;
  color: rgba(140,190,220,0.7);
  font-size: 14px; cursor: pointer;
  transition: all 0.2s;
}
.tab-btn.active {
  background: rgba(0,120,200,0.15);
  border-color: rgba(77,208,225,0.4);
  color: #4dd0e1;
}
.tab-btn:hover:not(.active) { border-color: rgba(0,120,200,0.35); color: #d0eaf8; }

/* Filter */
.filter-bar {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 16px;
  background: rgba(8,20,45,0.5);
  border: 1px solid rgba(0,120,200,0.12);
  border-radius: 8px;
  margin-bottom: 16px;
}
.search-btn {
  display: flex; align-items: center; gap: 4px;
  padding: 7px 16px;
  background: rgba(0,120,200,0.2);
  border: 1px solid rgba(0,150,230,0.3);
  border-radius: 6px;
  color: rgba(200,230,255,0.9);
  font-size: 13px; cursor: pointer;
  transition: all 0.2s;
}
.search-btn:hover { background: rgba(0,150,230,0.3); }

/* Table */
.table-wrap {
  background: rgba(8,20,45,0.6);
  border: 1px solid rgba(0,120,200,0.12);
  border-radius: 10px;
  overflow: hidden;
}
.loading-state { text-align: center; padding: 40px; color: rgba(140,190,220,0.4); }
.event-table { width: 100%; border-collapse: collapse; }
.event-table th {
  text-align: left; padding: 12px 16px;
  font-size: 12px; font-weight: 600;
  color: rgba(140,190,220,0.7);
  background: rgba(0,30,70,0.5);
  border-bottom: 1px solid rgba(0,120,200,0.15);
}
.event-table td {
  padding: 10px 16px;
  font-size: 13px; color: rgba(200,220,240,0.85);
  border-bottom: 1px solid rgba(0,120,200,0.06);
}
.td-device { font-weight: 500; color: #4dd0e1; }
.td-content { max-width: 400px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.td-snapshot { font-size: 11px; color: rgba(140,190,220,0.5); }
.empty-cell { text-align: center; padding: 40px; color: rgba(140,190,220,0.35); }

/* Confidence bar */
.confidence-cell { display: flex; align-items: center; gap: 8px; }
.confidence-bar {
  width: 60px; height: 5px;
  background: rgba(0,60,120,0.5);
  border-radius: 3px; overflow: hidden;
}
.confidence-fill { height: 100%; border-radius: 3px; transition: width 0.3s; }
.confidence-pct { font-size: 12px; color: rgba(180,210,230,0.8); }

/* Pagination */
.pagination-wrap { padding: 12px 16px; display: flex; justify-content: flex-end; }
</style>
