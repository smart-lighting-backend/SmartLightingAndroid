<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import { createDevice, deleteDevice, fetchDeviceList, updateDevice, STATUS_MAP, STATUS_QUERY_MAP } from '../api/devices.js'
import { useUserInfo } from '../composables/useUserInfo.js'

const router = useRouter()
const { hasPerm } = useUserInfo()
const devices  = ref([])
const loading  = ref(false)
const togglingDeviceId = ref('')
const search   = ref('')
const statusFilter = ref('全部')
const statuses = ['全部', '在线', '离线', '异常', '停用']
const createDialogVisible = ref(false)
const createFormRef = ref(null)
const creatingDevice = ref(false)
const createDialogMode = ref('create')
const editingDeviceId = ref('')
const deletingDeviceId = ref('')
const createStatusOptions = [
  { label: '在线', value: 1 },
  { label: '离线', value: 2 },
  { label: '异常', value: 3 },
  { label: '停用', value: 0 },
]
const createForm = ref(buildCreateForm())

const createRules = {
  deviceId: [{ required: true, message: '请输入设备编号', trigger: 'blur' }],
  status: [{ required: true, message: '请选择设备状态', trigger: 'change' }],
  healthScore: [
    {
      validator: (_rule, value, callback) => {
        if (value === '' || value === null || value === undefined) {
          callback()
          return
        }
        const score = Number(value)
        if (Number.isNaN(score) || score < 0 || score > 100) {
          callback(new Error('健康分需在 0 到 100 之间'))
          return
        }
        callback()
      },
      trigger: 'change',
    },
  ],
}

function buildCreateForm() {
  return {
    deviceId: '',
    name: '',
    area: '',
    location: '',
    status: 1,
    healthScore: 100,
    topicPrefix: 'streetlight',
    enabled: true,
  }
}

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
    return matchSearch && displayStatus(d) === statusVal
  })
})

function doFilterChange() { loadDevices() }

function healthColor(score) {
  if (!score && score !== 0) return '#888'
  if (score >= 80) return '#4caf50'
  if (score >= 60) return '#ffa726'
  return '#ef5350'
}

function displayStatus(device) {
  return device?.enabled === false ? 0 : device?.status
}

function getStatusMeta(device) {
  return STATUS_MAP[displayStatus(device)] || { label: '未知', cls: 'offline' }
}

function openCreateDialog() {
  createDialogMode.value = 'create'
  editingDeviceId.value = ''
  createForm.value = buildCreateForm()
  createDialogVisible.value = true
}

function openEditDialog(device) {
  createDialogMode.value = 'edit'
  editingDeviceId.value = device.deviceId
  createForm.value = {
    deviceId: device.deviceId || '',
    name: device.name || '',
    area: device.area || '',
    location: device.location || '',
    status: displayStatus(device) ?? 1,
    healthScore: device.healthScore ?? 100,
    topicPrefix: device.topicPrefix || 'streetlight',
    enabled: device.enabled !== false,
  }
  createDialogVisible.value = true
}

function resetCreateForm() {
  createForm.value = buildCreateForm()
  createDialogMode.value = 'create'
  editingDeviceId.value = ''
  createFormRef.value?.clearValidate?.()
}

function normalizeOptionalText(value) {
  const text = `${value ?? ''}`.trim()
  return text || undefined
}

function buildCreatePayload() {
  const formStatus = Number(createForm.value.status ?? 1)
  const enabled = formStatus === 0 ? false : createForm.value.enabled !== false
  return {
    deviceId: createForm.value.deviceId.trim(),
    name: normalizeOptionalText(createForm.value.name),
    area: normalizeOptionalText(createForm.value.area),
    location: normalizeOptionalText(createForm.value.location),
    status: enabled ? formStatus : 0,
    healthScore: createForm.value.healthScore === '' || createForm.value.healthScore === null
      ? undefined
      : Number(createForm.value.healthScore),
    topicPrefix: normalizeOptionalText(createForm.value.topicPrefix),
    enabled,
  }
}

function upsertCreatedDevice(device) {
  if (!device?.deviceId) return
  const idx = devices.value.findIndex(d => d.deviceId === device.deviceId)
  if (idx === -1) {
    devices.value.unshift(device)
    return
  }
  devices.value.splice(idx, 1, { ...devices.value[idx], ...device })
}

async function submitCreateDevice() {
  if (!createFormRef.value) return
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return

  creatingDevice.value = true
  try {
    const payload = buildCreatePayload()
    if (createDialogMode.value === 'edit') {
      const { deviceId: _deviceId, ...updatePayload } = payload
      const res = await updateDevice(editingDeviceId.value, updatePayload)
      const updated = res?.data || { deviceId: editingDeviceId.value, ...updatePayload }
      upsertCreatedDevice(updated)
      ElMessage.success('修改设备成功')
    } else {
      const res = await createDevice(payload)
      const created = res?.data || { id: Date.now(), deleted: false, ...payload }
      upsertCreatedDevice(created)
      ElMessage.success('新增设备成功')
    }
    createDialogVisible.value = false
  } catch (error) {
    ElMessage.error(error?.message || (createDialogMode.value === 'edit' ? '修改设备失败' : '新增设备失败'))
  } finally {
    creatingDevice.value = false
  }
}

async function removeDevice(device) {
  try {
    await ElMessageBox.confirm(
      `确认删除设备“${device.name || device.deviceId}”？删除后设备将从列表中移除。`,
      '删除设备',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
  } catch {
    return
  }

  deletingDeviceId.value = device.deviceId
  try {
    await deleteDevice(device.deviceId)
    devices.value = devices.value.filter(d => d.deviceId !== device.deviceId)
    ElMessage.success('删除设备成功')
  } catch (error) {
    ElMessage.error(error?.message || '删除设备失败')
  } finally {
    deletingDeviceId.value = ''
  }
}

async function toggleEnabled(device) {
  const currentEnabled = device.enabled !== false
  const nextEnabled = !currentEnabled
  const actionText = nextEnabled ? '启用' : '停用'
  const nextStatus = nextEnabled ? 2 : 0

  try {
    await ElMessageBox.confirm(
      `确认${actionText}设备“${device.name || device.deviceId}”？`,
      `${actionText}设备`,
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: nextEnabled ? 'info' : 'warning',
      }
    )
  } catch {
    return
  }

  togglingDeviceId.value = device.deviceId
  try {
    const res = await updateDevice(device.deviceId, { enabled: nextEnabled, status: nextStatus })
    const updated = res?.data || {}
    const idx = devices.value.findIndex(d => d.deviceId === device.deviceId)
    if (idx !== -1) {
      devices.value.splice(idx, 1, {
        ...devices.value[idx],
        ...updated,
        enabled: nextEnabled,
        status: nextStatus,
      })
    }
    ElMessage.success(`${actionText}成功`)
  } catch (error) {
    ElMessage.error(error?.message || `${actionText}失败`)
  } finally {
    togglingDeviceId.value = ''
  }
}

// 格式化最后心跳时间
function formatTime(iso) {
  if (!iso) return '--'
  // 兼容后端 LocalDateTime 数组格式 [2026,7,3,10,39,6]
  if (Array.isArray(iso)) {
    const [y, m, d, h, mi] = iso
    return `${y}-${String(m).padStart(2,'0')}-${String(d).padStart(2,'0')} ${String(h).padStart(2,'0')}:${String(mi).padStart(2,'0')}`
  }
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
        <button v-if="hasPerm('device:create')" class="create-device-btn" @click="openCreateDialog">
          <Plus class="create-device-icon" />
          新增设备
        </button>
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
        在线 <strong>{{ filtered.filter(d=>displayStatus(d)===1).length }}</strong>
      </span>
      <span class="summary-item offline">
        离线 <strong>{{ filtered.filter(d=>displayStatus(d)===2).length }}</strong>
      </span>
      <span class="summary-item warning">
        异常 <strong>{{ filtered.filter(d=>displayStatus(d)===3).length }}</strong>
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
          <div class="dc-icon" :class="getStatusMeta(d).cls">
            <svg viewBox="0 0 24 24" fill="none"><path d="M12 2C8.13 2 5 5.13 5 9c0 2.38 1.19 4.47 3 5.74V17c0 .55.45 1 1 1h6c.55 0 1-.45 1-1v-2.26c1.81-1.27 3-3.36 3-5.74 0-3.87-3.13-7-7-7z" fill="currentColor"/></svg>
          </div>
          <div class="status-pill" :class="getStatusMeta(d).cls">
            <span class="dot"></span>
            {{ getStatusMeta(d).label }}
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
            <span class="metric-val" :class="d.enabled !== false ? 'enabled' : 'disabled-text'">{{ d.enabled !== false ? '是' : '否' }}</span>
          </div>
          <div class="metric">
            <span class="metric-label">心跳</span>
            <span class="metric-val heartbeat">{{ formatTime(d.lastHeartbeatAt) }}</span>
          </div>
        </div>
        <div class="dc-actions" v-if="hasPerm('device:update') || hasPerm('device:delete')">
          <button
            v-if="hasPerm('device:update')"
            class="device-action-btn edit"
            @click.stop="openEditDialog(d)"
          >
            <Edit class="device-action-icon" />
            编辑设备
          </button>
          <button
            v-if="hasPerm('device:delete')"
            class="device-action-btn delete"
            :disabled="deletingDeviceId === d.deviceId"
            @click.stop="removeDevice(d)"
          >
            <Delete class="device-action-icon" />
            {{ deletingDeviceId === d.deviceId ? '删除中...' : '删除设备' }}
          </button>
          <button
            v-if="hasPerm('device:update')"
            class="device-toggle-btn"
            :class="d.enabled !== false ? 'stop' : 'start'"
            :disabled="togglingDeviceId === d.deviceId"
            @click.stop="toggleEnabled(d)"
          >
            {{ togglingDeviceId === d.deviceId ? '处理中...' : d.enabled !== false ? '停用设备' : '启用设备' }}
          </button>
        </div>
      </div>
    </div>

    <ElDialog
      v-model="createDialogVisible"
      :title="createDialogMode === 'edit' ? '编辑设备' : '新增设备'"
      width="560px"
      class="device-create-dialog"
      @closed="resetCreateForm"
    >
      <ElForm ref="createFormRef" :model="createForm" :rules="createRules" label-width="96px" class="device-create-form">
        <ElFormItem label="设备编号" prop="deviceId">
          <ElInput
            v-model.trim="createForm.deviceId"
            :disabled="createDialogMode === 'edit'"
            placeholder="如 SL-007"
            maxlength="50"
            show-word-limit
          />
        </ElFormItem>
        <ElFormItem label="设备名称" prop="name">
          <ElInput v-model.trim="createForm.name" placeholder="如 北门-01" />
        </ElFormItem>
        <ElFormItem label="所属区域" prop="area">
          <ElInput v-model.trim="createForm.area" placeholder="如 A区" />
        </ElFormItem>
        <ElFormItem label="安装位置" prop="location">
          <ElInput v-model.trim="createForm.location" placeholder="如 106.5622,29.5621" />
        </ElFormItem>
        <ElFormItem label="设备状态" prop="status">
          <ElSelect v-model="createForm.status" style="width: 100%">
            <ElOption v-for="option in createStatusOptions" :key="option.value" :label="option.label" :value="option.value" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="健康分" prop="healthScore">
          <ElInputNumber
            v-model="createForm.healthScore"
            :min="0"
            :max="100"
            :precision="2"
            :step="1"
            controls-position="right"
            style="width: 100%"
          />
        </ElFormItem>
        <ElFormItem label="主题前缀" prop="topicPrefix">
          <ElInput v-model.trim="createForm.topicPrefix" placeholder="streetlight" />
        </ElFormItem>
        <ElFormItem label="启用状态" prop="enabled">
          <ElSwitch v-model="createForm.enabled" active-text="启用" inactive-text="停用" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <div class="device-dialog-footer">
          <ElButton @click="createDialogVisible = false">取消</ElButton>
          <ElButton type="primary" :loading="creatingDevice" @click="submitCreateDevice">
            {{ createDialogMode === 'edit' ? '保存修改' : '确定新增' }}
          </ElButton>
        </div>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.devices-page { padding: 24px 28px; }
.page-header { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 16px; flex-wrap: wrap; gap: 12px; }
.page-title { font-size: 22px; font-weight: 700; color: #e0f4ff; margin-bottom: 4px; }
.page-sub { font-size: 13px; color: rgba(140,190,220,0.6); }
.header-actions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.create-device-btn {
  display: flex; align-items: center; gap: 6px;
  height: 36px; padding: 0 14px;
  background: linear-gradient(135deg, #0077cc, #0099e6);
  border: none; border-radius: 7px;
  color: #fff; font-size: 13px; font-weight: 500;
  cursor: pointer; transition: all 0.2s;
  box-shadow: 0 2px 12px rgba(0,150,230,0.25);
}
.create-device-icon { width: 15px; height: 15px; flex-shrink: 0; }
.create-device-btn:hover { transform: translateY(-1px); box-shadow: 0 4px 18px rgba(0,150,230,0.45); }
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
.dc-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 12px; flex-wrap: wrap; }
.device-action-btn,
.device-toggle-btn {
  height: 28px;
  padding: 0 10px;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}
.device-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.device-action-icon { width: 13px; height: 13px; flex-shrink: 0; }
.device-action-btn.edit {
  background: rgba(0,80,140,0.18);
  border: 1px solid rgba(0,120,200,0.28);
  color: rgba(140,200,230,0.9);
}
.device-action-btn.delete {
  background: rgba(180,30,30,0.1);
  border: 1px solid rgba(200,60,60,0.25);
  color: rgba(220,100,100,0.9);
}
.device-toggle-btn.stop {
  background: rgba(180,30,30,0.1);
  border: 1px solid rgba(200,60,60,0.25);
  color: rgba(220,100,100,0.9);
}
.device-toggle-btn.start {
  background: rgba(0,120,80,0.15);
  border: 1px solid rgba(0,180,120,0.25);
  color: rgba(140,220,180,0.9);
}
.device-action-btn:hover:not(:disabled),
.device-toggle-btn:hover:not(:disabled) { transform: translateY(-1px); }
.device-action-btn.edit:hover:not(:disabled) { background: rgba(0,120,200,0.22); color: #4dd0e1; }
.device-action-btn.delete:hover:not(:disabled) { background: rgba(180,30,30,0.2); color: #ff7070; }
.device-toggle-btn.stop:hover:not(:disabled) { background: rgba(180,30,30,0.2); color: #ff7070; }
.device-toggle-btn.start:hover:not(:disabled) { background: rgba(0,180,120,0.22); color: #4caf82; }
.device-action-btn:disabled,
.device-toggle-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.device-create-form :deep(.el-form-item__label) { color: rgba(190,220,240,0.85); }
.device-create-form :deep(.el-input__wrapper),
.device-create-form :deep(.el-input-number),
.device-create-form :deep(.el-select__wrapper) {
  background: rgba(8,20,45,0.72);
}
.device-dialog-footer { display: flex; justify-content: flex-end; gap: 10px; }
</style>
