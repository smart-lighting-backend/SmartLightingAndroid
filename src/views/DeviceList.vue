<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  ElInput,
  ElButton,
  ElTable,
  ElTableColumn,
  ElTag,
  ElProgress,
  ElCard,
  ElRow,
  ElCol,
  ElIcon
} from 'element-plus'
import {
  Search,
  Refresh,
  Grid,
  List,
  View,
  Camera,
  Bell
} from '@element-plus/icons-vue'
import { fetchDeviceList } from '../api/device'

const router = useRouter()

const searchKeyword = ref('')
const deviceList = ref([])
const loading = ref(false)
const viewMode = ref('table')

const getStatusTag = (status) => {
  return status === 'online'
    ? { type: 'success', text: '在线' }
    : { type: 'danger', text: '离线' }
}

const getHealthScoreColor = (score) => {
  if (score >= 80) return '#67c23a'
  if (score >= 60) return '#e6a23c'
  return '#f56c6c'
}

const handleSearch = async () => {
  loading.value = true
  const params = {}
  if (searchKeyword.value.trim()) {
    params.keyword = searchKeyword.value.trim()
  }
  const res = await fetchDeviceList(params)
  if (res.code === 200) {
    deviceList.value = res.data.list
  }
  loading.value = false
}

const handleReset = () => {
  searchKeyword.value = ''
  handleSearch()
}

const handleViewDetail = (deviceId) => {
  router.push(`/device/detail/${deviceId}`)
}

const toggleViewMode = (mode) => {
  viewMode.value = mode
}

onMounted(() => {
  handleSearch()
})
</script>

<template>
  <div class="device-list-container">
    <div class="search-bar">
      <div class="search-group">
        <ElInput
          v-model="searchKeyword"
          placeholder="按设备ID、名称或区域搜索"
          clearable
          prefix-icon="Search"
          style="width: 360px"
          @keyup.enter="handleSearch"
        />
        <ElButton type="primary" @click="handleSearch" :loading="loading">
          <Search />
          搜索
        </ElButton>
        <ElButton @click="handleReset">
          <Refresh />
          重置
        </ElButton>
      </div>
    </div>

    <div class="view-toggle">
      <ElButton
        :type="viewMode === 'table' ? 'primary' : 'default'"
        @click="toggleViewMode('table')"
      >
        <List />
        列表视图
      </ElButton>
      <ElButton
        :type="viewMode === 'card' ? 'primary' : 'default'"
        @click="toggleViewMode('card')"
      >
        <Grid />
        卡片视图
      </ElButton>
      <ElButton type="success" @click="router.push('/city/3d')">
        <Camera />
        3D可视化
      </ElButton>
      <ElButton type="warning" @click="router.push('/alarm/list')">
        <Bell />
        告警日志
      </ElButton>
    </div>

    <div class="device-content" v-loading="loading">
      <ElTable
        v-if="viewMode === 'table'"
        :data="deviceList"
        border
        stripe
        style="width: 100%"
        @row-click="(row) => handleViewDetail(row.id)"
        row-class-name="clickable-row"
      >
        <ElTableColumn prop="id" label="设备ID" width="120" />
        <ElTableColumn prop="name" label="设备名称" min-width="180" />
        <ElTableColumn prop="region" label="区域" min-width="180" />
        <ElTableColumn prop="status" label="状态" width="100">
          <template #default="{ row }">
            <ElTag :type="getStatusTag(row.status).type">
              {{ getStatusTag(row.status).text }}
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="healthScore" label="健康评分" width="160">
          <template #default="{ row }">
            <div class="health-cell">
              <span class="score-text">{{ row.healthScore }}</span>
              <ElProgress
                :percentage="row.healthScore"
                :color="getHealthScoreColor(row.healthScore)"
                :stroke-width="8"
                :show-text="false"
              />
            </div>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="lastHeartbeat" label="最后心跳时间" min-width="180" />
        <ElTableColumn label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <ElButton
              type="primary"
              link
              @click.stop="handleViewDetail(row.id)"
            >
              <View />
              查看详情
            </ElButton>
          </template>
        </ElTableColumn>
      </ElTable>

      <ElRow v-else :gutter="20">
        <ElCol
          v-for="device in deviceList"
          :key="device.id"
          :span="6"
          class="card-col"
        >
          <ElCard
            class="device-card"
            hover
            @click="handleViewDetail(device.id)"
          >
            <div class="card-header">
              <span class="device-id">{{ device.id }}</span>
              <ElTag :type="getStatusTag(device.status).type" size="small">
                {{ getStatusTag(device.status).text }}
              </ElTag>
            </div>
            <h3 class="device-name">{{ device.name }}</h3>
            <div class="device-region">{{ device.region }}</div>
            <div class="health-section">
              <span class="health-label">健康评分</span>
              <div class="health-info">
                <span
                  class="health-score"
                  :style="{ color: getHealthScoreColor(device.healthScore) }"
                >
                  {{ device.healthScore }}
                </span>
                <ElProgress
                  :percentage="device.healthScore"
                  :color="getHealthScoreColor(device.healthScore)"
                  :stroke-width="6"
                  :show-text="false"
                />
              </div>
            </div>
            <div class="last-heartbeat">
              <span class="label">最后心跳：</span>
              <span class="value">{{ device.lastHeartbeat }}</span>
            </div>
            <div class="card-actions">
              <ElButton type="primary" @click.stop="handleViewDetail(device.id)">
                <View />
                查看详情
              </ElButton>
            </div>
          </ElCard>
        </ElCol>
      </ElRow>

      <div v-if="deviceList.length === 0 && !loading" class="empty-state">
        <ElIcon :size="48" color="#c0c4cc">
          <Search />
        </ElIcon>
        <p>暂无设备数据</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.device-list-container {
  padding: 24px;
  min-height: 100vh;
  background-color: #1a1a2e;
}

.search-bar {
  background-color: rgba(30, 30, 50, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.05);
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 16px;
}

.search-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.view-toggle {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.device-content {
  background-color: rgba(30, 30, 50, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  padding: 24px;
}

.clickable-row {
  cursor: pointer;
}

.clickable-row:hover {
  background-color: rgba(255, 255, 255, 0.05);
}

.health-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.score-text {
  font-weight: 600;
  color: #ffffff;
  min-width: 36px;
}

.card-col {
  margin-bottom: 20px;
}

.device-card {
  height: 100%;
  cursor: pointer;
  transition: all 0.3s ease;
  background: rgba(30, 30, 50, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.device-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px 0 rgba(0, 0, 0, 0.3);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.device-id {
  font-size: 14px;
  font-weight: 600;
  color: #409eff;
}

.device-name {
  font-size: 16px;
  font-weight: 600;
  color: #ffffff;
  margin: 0 0 8px 0;
}

.device-region {
  font-size: 13px;
  color: #909399;
  margin-bottom: 16px;
}

.health-section {
  margin-bottom: 12px;
}

.health-label {
  font-size: 12px;
  color: #909399;
}

.health-info {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 4px;
}

.health-score {
  font-size: 18px;
  font-weight: 700;
  min-width: 40px;
}

.last-heartbeat {
  font-size: 12px;
  color: #909399;
  margin-bottom: 16px;
}

.last-heartbeat .label {
  margin-right: 4px;
}

.card-actions {
  text-align: right;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: #606266;
}

.empty-state p {
  margin-top: 16px;
  font-size: 14px;
}

@media (max-width: 1200px) {
  .card-col {
    flex: 0 0 33.33%;
    max-width: 33.33%;
  }
}

@media (max-width: 900px) {
  .card-col {
    flex: 0 0 50%;
    max-width: 50%;
  }

  .search-group {
    flex-wrap: wrap;
  }

  .search-group :deep(.el-input) {
    width: 100% !important;
  }
}

@media (max-width: 600px) {
  .card-col {
    flex: 0 0 100%;
    max-width: 100%;
  }

  .device-list-container {
    padding: 12px;
  }
}
</style>