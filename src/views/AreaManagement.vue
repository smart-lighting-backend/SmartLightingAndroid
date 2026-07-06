<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import {
  ElButton, ElDialog, ElForm, ElFormItem, ElInput,
  ElSelect, ElOption, ElMessage, ElTree, ElMessageBox,
  ElCard, ElEmpty, ElTable, ElTableColumn, ElTag,
  ElCheckbox
} from 'element-plus'
import { Plus, Edit, Delete, Refresh, Search } from '@element-plus/icons-vue'
import { fetchAreaTree, createArea, updateArea, deleteArea } from '../api/area.js'
import { fetchDeviceList, batchDeviceArea } from '../api/devices.js'
import { useUserInfo } from '../composables/useUserInfo.js'

const { hasPerm } = useUserInfo()

const treeData = ref([])
const loading = ref(false)
const selectedNode = ref(null)
const areaKeyword = ref('')

/** 根据关键词过滤树（匹配名称或名称含关键词的节点及其父路径） */
const filteredTreeData = computed(() => {
  const kw = areaKeyword.value.trim().toLowerCase()
  if (!kw) return treeData.value

  function filterNodes(nodes) {
    return nodes.reduce((acc, n) => {
      const match = (n.name || '').toLowerCase().includes(kw)
      const filteredChildren = n.children?.length ? filterNodes(n.children) : []
      if (match || filteredChildren.length) {
        acc.push({ ...n, children: filteredChildren.length ? filteredChildren : (match ? n.children : []) })
      }
      return acc
    }, [])
  }
  return filterNodes(treeData.value)
})

// ── 对话框 ────────────────────────────────────────────────────────────────
const dialogVisible = ref(false)
const dialogMode = ref('create')
const formRef = ref(null)
const editingId = ref(null)
const submitting = ref(false)

const formData = ref({
  name: '',
  description: '',
  parentId: null
})

const rules = {
  name: [{ required: true, message: '请输入区域名称', trigger: 'blur' }]
}

// ── 树形选择器用平铺选项（排除自身及子孙） ──────────────────────────────
const parentOptions = computed(() => {
  const result = []
  function walk(nodes) {
    for (const n of nodes) {
      if (editingId.value && isDescendantOrSelf(n, editingId.value)) continue
      result.push({ id: n.id, name: n.name, parentId: n.parentId })
      if (n.children?.length) walk(n.children)
    }
  }
  walk(treeData.value)
  return result
})

function isDescendantOrSelf(node, targetId) {
  if (node.id === targetId) return true
  if (!node.children?.length) return false
  return node.children.some(child => isDescendantOrSelf(child, targetId))
}

// ── 节点路径显示 ──────────────────────────────────────────────────────────
const selectedNodePath = computed(() => {
  if (!selectedNode.value) return ''
  const parts = []
  function find(node, targetId) {
    if (node.id === targetId) {
      parts.unshift(node.name)
      return true
    }
    if (node.children) {
      for (const child of node.children) {
        if (find(child, targetId)) {
          parts.unshift(node.name)
          return true
        }
      }
    }
    return false
  }
  for (const root of treeData.value) {
    if (find(root, selectedNode.value.id)) break
  }
  return parts.join(' / ')
})

// ── 该区域下的设备 ─────────────────────────────────────────────────────────
const areaDevices = ref([])
const areaDevicesLoading = ref(false)

async function loadAreaDevices(areaId) {
  if (!areaId) {
    areaDevices.value = []
    return
  }
  areaDevicesLoading.value = true
  try {
    const res = await fetchDeviceList({ areaId, pageSize: 200 })
    areaDevices.value = Array.isArray(res) ? res : (res?.data || [])
  } catch {
    areaDevices.value = []
  } finally {
    areaDevicesLoading.value = false
  }
}

watch(selectedNode, (val) => {
  loadAreaDevices(val?.id)
})

// ── 批量分配 ──────────────────────────────────────────────────────────────
const batchDialogVisible = ref(false)
const batchKeyword = ref('')
const showUnassignedOnly = ref(true)
const allDevices = ref([])
const devicesLoading = ref(false)
const selectedDeviceIds = ref([])
const batchSubmitting = ref(false)

const filteredDevices = computed(() => {
  let list = allDevices.value
  if (showUnassignedOnly.value) {
    list = list.filter(d => d.areaId === undefined || d.areaId === null)
  }
  if (batchKeyword.value) {
    const kw = batchKeyword.value.toLowerCase()
    list = list.filter(d =>
      (d.deviceId?.toLowerCase() || '').includes(kw) ||
      (d.name?.toLowerCase() || '').includes(kw)
    )
  }
  return list
})

async function openBatchAssign() {
  selectedDeviceIds.value = []
  batchKeyword.value = ''
  showUnassignedOnly.value = true
  batchDialogVisible.value = true
  devicesLoading.value = true
  try {
    const res = await fetchDeviceList({ pageSize: 500 })
    allDevices.value = Array.isArray(res) ? res : (res?.data || [])
  } catch {
    allDevices.value = []
  } finally {
    devicesLoading.value = false
  }
}

function onSelectionChange(selection) {
  selectedDeviceIds.value = selection.map(d => d.id)
}

async function confirmBatchAssign() {
  if (!selectedDeviceIds.value.length) {
    ElMessage.warning('请至少选择一台设备')
    return
  }
  batchSubmitting.value = true
  try {
    await batchDeviceArea({
      deviceIds: selectedDeviceIds.value,
      areaId: selectedNode.value.id
    })
    ElMessage.success(`已分配 ${selectedDeviceIds.value.length} 台设备到「${selectedNode.value.name}」`)
    batchDialogVisible.value = false
    // 刷新当前区域设备列表
    await loadAreaDevices(selectedNode.value.id)
    // 全量刷新列表，让左侧树更新（可在 detail 中展示设备数）
  } catch (error) {
    ElMessage.error(error?.message || '批量分配失败')
  } finally {
    batchSubmitting.value = false
  }
}

async function confirmBatchClear() {
  if (!areaDevices.value.length) {
    ElMessage.info('该区域下无设备')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认清除该区域下全部 ${areaDevices.value.length} 台设备的区域关联？`,
      '批量清除区域',
      { confirmButtonText: '确认清除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  batchSubmitting.value = true
  try {
    await batchDeviceArea({
      deviceIds: areaDevices.value.map(d => d.id),
      areaId: null
    })
    ElMessage.success(`已清除 ${areaDevices.value.length} 台设备的区域`)
    await loadAreaDevices(selectedNode.value.id)
  } catch (error) {
    ElMessage.error(error?.message || '批量清除失败')
  } finally {
    batchSubmitting.value = false
  }
}

const statusTag = (s) => {
  const map = { 1: 'success', 2: 'info', 3: 'danger', 0: 'info' }
  return map[s] || 'info'
}
const statusLabel = (s) => {
  const map = { 0: '停用', 1: '在线', 2: '离线', 3: '异常' }
  return map[s] || '未知'
}

// ── 数据加载 ──────────────────────────────────────────────────────────────
async function loadTree() {
  loading.value = true
  try {
    const res = await fetchAreaTree()
    treeData.value = res?.data || []
  } finally {
    loading.value = false
  }
}

// ── 新增 ──────────────────────────────────────────────────────────────────
function openCreate(parent) {
  dialogMode.value = 'create'
  editingId.value = null
  formData.value = {
    name: '',
    description: '',
    parentId: parent?.id ?? null
  }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate?.())
}

// ── 编辑 ──────────────────────────────────────────────────────────────────
function openEdit(node) {
  dialogMode.value = 'edit'
  editingId.value = node.id
  formData.value = {
    name: node.name || '',
    description: node.description || '',
    parentId: node.parentId ?? null
  }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate?.())
}

// ── 提交流单 ──────────────────────────────────────────────────────────────
async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = { ...formData.value }

    if (dialogMode.value === 'edit') {
      await updateArea(editingId.value, payload)
      ElMessage.success('修改区域成功')
    } else {
      await createArea(payload)
      ElMessage.success('新增区域成功')
    }

    dialogVisible.value = false
    await loadTree()
  } catch (error) {
    ElMessage.error(error?.message || (dialogMode.value === 'edit' ? '修改区域失败' : '新增区域失败'))
  } finally {
    submitting.value = false
  }
}

// ── 删除 ──────────────────────────────────────────────────────────────────
async function handleDelete(node) {
  try {
    await ElMessageBox.confirm(
      `确认删除区域”${node.name}”？${
        node.children?.length ? '该区域下有子区域，无法删除。' : '若该区域已被设备引用，将拒绝删除。'
      }`,
      '删除区域',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
  } catch {
    return
  }

  try {
    await deleteArea(node.id)
    ElMessage.success('删除区域成功')
    if (selectedNode.value?.id === node.id) selectedNode.value = null
    await loadTree()
  } catch (error) {
    const msg = error?.response?.data?.msg || error?.message || '删除区域失败'
    if (msg.includes('设备')) {
      await ElMessageBox.alert(msg, '无法删除', { confirmButtonText: '知道了', type: 'warning' })
    } else {
      ElMessage.error(msg)
    }
  }
}

onMounted(loadTree)
</script>

<template>
  <div class="area-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div>
        <h1 class="page-title">分区管理</h1>
        <p class="page-sub">管理设备分区区域，支持树形层级结构</p>
      </div>
      <div class="header-actions">
        <button class="refresh-btn" @click="loadTree">
          <Refresh class="btn-icon" />
          刷新
        </button>
        <button v-if="hasPerm('device:update')" class="add-btn" @click="openCreate(null)">
          <Plus class="btn-icon" />
          新增区域
        </button>
      </div>
    </div>

    <div class="content-wrap">
      <!-- 左侧：区域树 -->
      <ElCard class="tree-card" :body-style="{ padding: '12px' }">
        <div class="tree-header">
          <span class="tree-title">区域结构</span>
          <button class="tree-refresh-btn" @click="loadTree" title="刷新">
            <Refresh class="btn-icon" />
          </button>
        </div>
        <div class="tree-search">
          <Search class="tree-search-icon" />
          <input
            v-model="areaKeyword"
            class="tree-search-input"
            placeholder="输入区域名称搜索"
          />
        </div>
        <ElEmpty v-if="!loading && !filteredTreeData.length" :image-size="60" description="暂无区域数据" />
        <div v-else-if="loading" class="loading-state">
          <div class="loading-spinner"></div>
          <span>加载中...</span>
        </div>
        <ElTree
          v-else-if="filteredTreeData.length"
          ref="treeRef"
          :data="filteredTreeData"
          node-key="id"
          :props="{ children: 'children', label: 'name' }"
          default-expand-all
          highlight-current
          @node-click="(data) => selectedNode = data"
        />
      </ElCard>

      <!-- 右侧：详情 / 设备列表 -->
      <div class="right-panel">
        <ElCard class="detail-card" :body-style="{ padding: '16px 20px' }">
          <template v-if="selectedNode">
            <div class="detail-header">
              <h3 class="detail-title">{{ selectedNode.name }}</h3>
              <div class="detail-actions">
                <button
                  v-if="hasPerm('device:update')"
                  class="detail-btn edit"
                  @click="openEdit(selectedNode)"
                >
                  <Edit class="btn-icon" />
                  编辑
                </button>
                <button
                  v-if="hasPerm('device:delete')"
                  class="detail-btn delete"
                  @click="handleDelete(selectedNode)"
                >
                  <Delete class="btn-icon" />
                  删除
                </button>
              </div>
            </div>
            <div class="detail-body">
              <div class="detail-row">
                <span class="detail-label">节点路径</span>
                <span class="detail-value">{{ selectedNodePath || '--' }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">区域名称</span>
                <span class="detail-value">{{ selectedNode.name }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">区域描述</span>
                <span class="detail-value">{{ selectedNode.description || '--' }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">上级区域</span>
                <span class="detail-value">{{ selectedNode.parentId ? `ID: ${selectedNode.parentId}` : '无（顶级区域）' }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">子区域数</span>
                <span class="detail-value">{{ selectedNode.children?.length || 0 }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">设备数</span>
                <span class="detail-value">{{ areaDevices.length }}</span>
              </div>
            </div>
          </template>
          <ElEmpty v-else :image-size="60" description="请在左侧选择一个区域" />
        </ElCard>

        <!-- 设备列表 & 批量操作 -->
        <ElCard v-if="selectedNode" class="device-card" :body-style="{ padding: '12px 16px' }">
          <div class="device-header">
            <span class="device-title">
              归属设备
              <span class="device-count">{{ areaDevices.length }} 台</span>
            </span>
            <div class="device-header-actions">
              <button
                v-if="hasPerm('device:update')"
                class="detail-btn primary"
                @click="openBatchAssign"
              >
                <Plus class="btn-icon" />
                批量分配
              </button>
              <button
                v-if="hasPerm('device:update') && areaDevices.length"
                class="detail-btn delete"
                @click="confirmBatchClear"
              >
                批量清除
              </button>
            </div>
          </div>

          <div v-if="areaDevicesLoading" class="loading-state">
            <div class="loading-spinner"></div>
            <span>加载中...</span>
          </div>
          <ElEmpty v-else-if="!areaDevices.length" :image-size="50" description="该区域暂无设备" />
          <ElTable v-else :data="areaDevices" stripe size="small" class="area-device-table">
            <ElTableColumn prop="deviceId" label="设备编号" width="110" />
            <ElTableColumn prop="name" label="设备名称" min-width="130" />
            <ElTableColumn prop="area" label="区域" width="100">
              <template #default="{ row }">
                <ElTag size="small" effect="plain">{{ row.area || '--' }}</ElTag>
              </template>
            </ElTableColumn>
            <ElTableColumn label="状态" width="80">
              <template #default="{ row }">
                <ElTag :type="statusTag(row.status)" size="small" effect="light">
                  {{ statusLabel(row.status) }}
                </ElTag>
              </template>
            </ElTableColumn>
          </ElTable>
        </ElCard>
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <ElDialog
      v-model="dialogVisible"
      :title="dialogMode === 'edit' ? '编辑区域' : '新增区域'"
      width="480px"
      class="area-dialog"
    >
      <ElForm ref="formRef" :model="formData" :rules="rules" label-width="80px">
        <ElFormItem label="区域名称" prop="name">
          <ElInput v-model.trim="formData.name" placeholder="如 A区、南门" maxlength="50" show-word-limit />
        </ElFormItem>
        <ElFormItem label="区域描述" prop="description">
          <ElInput
            v-model.trim="formData.description"
            type="textarea"
            :rows="3"
            placeholder="如 A区 — 主干道照明区域（南门）"
            maxlength="200"
            show-word-limit
          />
        </ElFormItem>
        <ElFormItem label="上级区域" prop="parentId">
          <ElSelect v-model="formData.parentId" placeholder="选择上级区域（留空为顶级区域）" clearable style="width: 100%">
            <ElOption label="── 顶级区域（无上级）──" :value="null" />
            <ElOption
              v-for="opt in parentOptions"
              :key="opt.id"
              :label="opt.name"
              :value="opt.id"
            />
          </ElSelect>
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="submitting" @click="handleSubmit">
          {{ dialogMode === 'edit' ? '保存修改' : '确定新增' }}
        </ElButton>
      </template>
    </ElDialog>

    <!-- 批量分配设备对话框 -->
    <ElDialog
      v-model="batchDialogVisible"
      :title="`批量分配设备 → ${selectedNode?.name || ''}`"
      width="800px"
      top="4vh"
      class="batch-dialog"
      destroy-on-close
    >
      <div class="batch-filter">
        <div class="batch-filter-left">
          <ElInput
            v-model="batchKeyword"
            placeholder="搜索设备编号/名称"
            clearable
            size="small"
            :prefix-icon="Search"
            class="search-input"
          />
          <label class="unassigned-toggle">
            <ElCheckbox v-model="showUnassignedOnly" size="small" />
            <span>仅显示未分配区域的设备</span>
          </label>
        </div>
        <span class="batch-selected-info">
          已选 {{ selectedDeviceIds.length }} 台
        </span>
      </div>

      <div v-if="devicesLoading" class="loading-state">
        <div class="loading-spinner"></div>
        <span>加载设备列表中...</span>
      </div>
      <template v-else>
        <ElTable
          :data="filteredDevices"
          stripe
          size="small"
          max-height="360"
          @selection-change="onSelectionChange"
          class="batch-device-table"
        >
          <ElTableColumn type="selection" width="44" />
          <ElTableColumn prop="deviceId" label="设备编号" width="120" />
          <ElTableColumn prop="name" label="设备名称" min-width="140" />
          <ElTableColumn prop="area" label="当前区域" width="120">
            <template #default="{ row }">
              <ElTag v-if="row.area" size="small" effect="plain">{{ row.area }}</ElTag>
              <span v-else class="no-area">未分配</span>
            </template>
          </ElTableColumn>
          <ElTableColumn label="状态" width="80">
            <template #default="{ row }">
              <ElTag :type="statusTag(row.status)" size="small" effect="light">
                {{ statusLabel(row.status) }}
              </ElTag>
            </template>
          </ElTableColumn>
        </ElTable>
        <div v-if="!filteredDevices.length" class="batch-empty">
          <ElEmpty :image-size="40" description="没有符合条件的设备" />
        </div>
      </template>

      <template #footer>
        <ElButton @click="batchDialogVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="batchSubmitting" @click="confirmBatchAssign">
          确认分配 ({{ selectedDeviceIds.length }})
        </ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.area-page {
  padding: 24px 28px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 16px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #e0f4ff;
  margin-bottom: 4px;
}

.page-sub {
  font-size: 13px;
  color: rgba(140, 190, 220, 0.6);
}

.header-actions {
  display: flex;
  gap: 8px;
}

.add-btn,
.refresh-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  height: 34px;
  padding: 0 14px;
  border: none;
  border-radius: 7px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.add-btn {
  background: linear-gradient(135deg, #0077cc, #0099e6);
  color: #fff;
  box-shadow: 0 2px 12px rgba(0, 150, 230, 0.25);
}

.add-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 18px rgba(0, 150, 230, 0.45);
}

.refresh-btn {
  background: rgba(0, 30, 70, 0.5);
  border: 1px solid rgba(0, 80, 140, 0.2);
  color: rgba(140, 190, 220, 0.7);
}

.refresh-btn:hover {
  background: rgba(0, 60, 120, 0.4);
  color: #c0e0f8;
}

.btn-icon {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}

.content-wrap {
  flex: 1;
  display: flex;
  gap: 16px;
  min-height: 0;
}

.tree-card {
  width: 280px;
  flex-shrink: 0;
  background: rgba(8, 20, 45, 0.8);
  border: 1px solid rgba(0, 120, 200, 0.15);
}

.tree-header {
  padding: 0 4px 10px;
  border-bottom: 1px solid rgba(0, 80, 140, 0.15);
  margin-bottom: 8px;
}

.tree-title {
  font-size: 14px;
  font-weight: 600;
  color: #c0dff0;
}

.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
}

.detail-card {
  background: rgba(8, 20, 45, 0.8);
  border: 1px solid rgba(0, 120, 200, 0.15);
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(0, 80, 140, 0.15);
  margin-bottom: 16px;
}

.detail-title {
  font-size: 17px;
  font-weight: 600;
  color: #e0f4ff;
  margin: 0;
}

.detail-actions {
  display: flex;
  gap: 8px;
}

.detail-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  height: 30px;
  padding: 0 12px;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.detail-btn.edit {
  background: rgba(0, 120, 220, 0.15);
  color: #4dd0e1;
  border: 1px solid rgba(77, 208, 225, 0.2);
}

.detail-btn.edit:hover {
  background: rgba(0, 120, 220, 0.3);
}

.detail-btn.delete {
  background: rgba(220, 60, 60, 0.15);
  color: #ef5350;
  border: 1px solid rgba(239, 83, 80, 0.2);
}

.detail-btn.delete:hover {
  background: rgba(220, 60, 60, 0.3);
}

.detail-btn.primary {
  background: rgba(0, 180, 120, 0.15);
  color: #4dd0e1;
  border: 1px solid rgba(77, 208, 225, 0.2);
}

.detail-btn.primary:hover {
  background: rgba(0, 180, 120, 0.3);
}

.detail-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.detail-label {
  font-size: 12px;
  color: rgba(140, 190, 220, 0.5);
  min-width: 80px;
  flex-shrink: 0;
}

.detail-value {
  font-size: 13px;
  color: #c0dff0;
}

.device-card {
  flex: 1;
  background: rgba(8, 20, 45, 0.8);
  border: 1px solid rgba(0, 120, 200, 0.15);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.device-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(0, 80, 140, 0.15);
  margin-bottom: 8px;
}

.device-title {
  font-size: 14px;
  font-weight: 600;
  color: #c0dff0;
}

.device-count {
  font-size: 12px;
  font-weight: 400;
  color: rgba(140, 190, 220, 0.5);
  margin-left: 6px;
}

.device-header-actions {
  display: flex;
  gap: 6px;
}

.area-device-table {
  width: 100%;
}

/* ── 批量分配对话框 ─────────────────────────────────────── */
.batch-dialog :deep(.el-dialog__body) {
  padding-top: 12px;
  padding-bottom: 8px;
}

.batch-filter {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.batch-filter-left {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 1;
}

.search-input {
  width: 240px;
}

.unassigned-toggle {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: rgba(140, 190, 220, 0.6);
  cursor: pointer;
  white-space: nowrap;
}

.batch-selected-info {
  font-size: 12px;
  color: #4dd0e1;
  white-space: nowrap;
}

.batch-device-table {
  width: 100%;
}

.batch-empty {
  padding: 20px 0;
}

.no-area {
  color: rgba(140, 190, 220, 0.4);
  font-size: 12px;
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 40px;
  color: rgba(140, 190, 220, 0.5);
}

.loading-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(77, 208, 225, 0.3);
  border-top-color: #4dd0e1;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

/* 树搜索 */
.tree-search {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  margin-bottom: 6px;
  background: rgba(8, 20, 45, 0.6);
  border: 1px solid rgba(0, 80, 140, 0.3);
  border-radius: 6px;
  transition: border-color 0.2s;
}
.tree-search:focus-within {
  border-color: rgba(77, 208, 225, 0.4);
}
.tree-search-icon {
  width: 14px;
  height: 14px;
  color: rgba(140, 190, 220, 0.4);
  flex-shrink: 0;
}
.tree-search-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  color: #d0eaf8;
  font-size: 12px;
}
.tree-search-input::placeholder { color: rgba(140, 190, 220, 0.35); }
.tree-refresh-btn {
  display: inline-flex; align-items: center; justify-content: center;
  width: 22px; height: 22px;
  background: transparent; border: none;
  color: rgba(140, 190, 220, 0.5); cursor: pointer;
  border-radius: 4px; transition: all 0.2s;
}
.tree-refresh-btn:hover { background: rgba(0, 80, 140, 0.2); color: #4dd0e1; }
.tree-refresh-btn .btn-icon { width: 13px; height: 13px; }

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
