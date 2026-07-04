<script setup>
import { ref, onMounted } from 'vue'
import {
  ElButton, ElTable, ElTableColumn, ElTag, ElDialog, ElForm, ElFormItem, ElInput, ElSelect, ElOption, ElMessage, ElMessageBox, ElTree, ElCard
} from 'element-plus'
import { Plus, Edit, Delete, Search } from '@element-plus/icons-vue'
import { fetchPermissionTree, fetchPermissionList, createPermission, updatePermission, deletePermission } from '../api/permission.js'
import { useUserInfo } from '../composables/useUserInfo.js'

const { hasPerm } = useUserInfo()

const permissionList = ref([])
const treeData = ref([])
const loading = ref(false)

const dialogVisible = ref(false)
const dialogType = ref('add')
const formRef = ref(null)
const formData = ref({
  id: null,
  name: '',
  code: '',
  type: 'ACTION',
  parentId: null,
  description: ''
})

const rules = {
  name: [{ required: true, message: '请输入权限名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入权限编码', trigger: 'blur' }],
  type: [{ required: true, message: '请选择权限类型', trigger: 'change' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const treeRes = await fetchPermissionTree()
    if (treeRes?.code === 200) {
      treeData.value = treeRes.data || []
    } else {
      treeData.value = treeRes || []
    }
    const listRes = await fetchPermissionList()
    if (listRes?.code === 200) {
      permissionList.value = listRes.data || []
    } else {
      permissionList.value = listRes || []
    }
  } catch (error) {
    ElMessage.error('获取权限数据失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = (parent) => {
  dialogType.value = 'add'
  formData.value = {
    id: null,
    name: '',
    code: '',
    type: parent?.type === 'MODULE' ? 'ACTION' : 'ACTION',
    parentId: parent?.id || null,
    description: ''
  }
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogType.value = 'edit'
  formData.value = {
    id: row.id,
    name: row.name,
    code: row.code,
    type: row.type || 'ACTION',
    parentId: row.parentId || null,
    description: row.description || ''
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const payload = { ...formData.value }
        if (dialogType.value === 'add') {
          await createPermission(payload)
          ElMessage.success('新增权限成功')
        } else {
          await updatePermission(payload.id, payload)
          ElMessage.success('修改权限成功')
        }
        dialogVisible.value = false
        loadData()
      } catch (error) {
        ElMessage.error(error.message || '操作失败')
      }
    }
  })
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除权限 "${row.name}" 吗？`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deletePermission(row.id)
      ElMessage.success('删除成功')
      loadData()
    } catch (error) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

const typeTag = (type) => {
  return type === 'MODULE'
    ? { type: 'primary', text: '模块' }
    : { type: 'success', text: '操作' }
}

const defaultProps = {
  children: 'children',
  label: 'name'
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="permission-container">
    <div class="page-header">
      <h2 class="page-title">权限管理</h2>
      <ElButton v-if="hasPerm('permission:create')" type="primary" @click="handleAdd(null)"><Plus /> 新增权限</ElButton>
    </div>

    <div class="content-grid">
      <!-- 权限树 -->
      <ElCard class="tree-card" shadow="never">
        <template #header>
          <span class="card-title">权限树结构</span>
        </template>
        <div class="tree-wrapper" v-loading="loading">
          <ElTree
            :data="treeData"
            :props="defaultProps"
            node-key="id"
            default-expand-all
            highlight-current
          >
            <template #default="{ node, data }">
              <span class="tree-node">
                <span class="tree-label">{{ data.name }}</span>
                <ElTag size="small" :type="typeTag(data.type).type" class="tree-tag">
                  {{ typeTag(data.type).text }}
                </ElTag>
                <span class="tree-code">{{ data.code }}</span>
                <span class="tree-actions">
                  <ElButton v-if="hasPerm('permission:create')" size="small" type="primary" link @click.stop="handleAdd(data)">新增子权限</ElButton>
                  <ElButton v-if="hasPerm('permission:update')" size="small" type="warning" link @click.stop="handleEdit(data)">编辑</ElButton>
                  <ElButton v-if="hasPerm('permission:delete')" size="small" type="danger" link @click.stop="handleDelete(data)">删除</ElButton>
                </span>
              </span>
            </template>
          </ElTree>
          <div v-if="!loading && treeData.length === 0" class="empty-hint">
            暂无数据，请点击"新增权限"添加
          </div>
        </div>
      </ElCard>

      <!-- 权限列表 -->
      <ElCard class="list-card" shadow="never">
        <template #header>
          <span class="card-title">权限列表</span>
        </template>
        <ElTable :data="permissionList" border stripe style="width: 100%" v-loading="loading" max-height="500">
          <ElTableColumn prop="id" label="ID" width="70" />
          <ElTableColumn prop="name" label="名称" min-width="140" />
          <ElTableColumn prop="code" label="权限编码" min-width="150" />
          <ElTableColumn label="类型" width="80">
            <template #default="{ row }">
              <ElTag :type="typeTag(row.type).type" size="small">{{ typeTag(row.type).text }}</ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="description" label="描述" min-width="160" />
          <ElTableColumn label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <ElButton v-if="hasPerm('permission:update')" type="primary" link size="small" @click="handleEdit(row)"><Edit /> 编辑</ElButton>
              <ElButton v-if="hasPerm('permission:delete')" type="danger" link size="small" @click="handleDelete(row)"><Delete /> 删除</ElButton>
            </template>
          </ElTableColumn>
        </ElTable>
      </ElCard>
    </div>

    <!-- 新增/编辑弹窗 -->
    <ElDialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? '新增权限' : '编辑权限'"
      width="500px"
      custom-class="dark-dialog"
    >
      <ElForm ref="formRef" :model="formData" :rules="rules" label-width="100px">
        <ElFormItem label="权限名称" prop="name">
          <ElInput v-model="formData.name" placeholder="如：设备管理" />
        </ElFormItem>
        <ElFormItem label="权限编码" prop="code">
          <ElInput v-model="formData.code" placeholder="如：device:read" />
        </ElFormItem>
        <ElFormItem label="权限类型" prop="type">
          <ElSelect v-model="formData.type" style="width: 100%">
            <ElOption label="模块 (MODULE)" value="MODULE" />
            <ElOption label="操作 (ACTION)" value="ACTION" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="父权限" prop="parentId">
          <ElSelect v-model="formData.parentId" placeholder="不选则为顶级" clearable style="width: 100%">
            <ElOption
              v-for="item in permissionList.filter(p => p.type === 'MODULE')"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="描述" prop="description">
          <ElInput v-model="formData.description" type="textarea" :rows="3" placeholder="权限说明" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="handleSubmit">确定</ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.permission-container {
  padding: 24px;
  min-height: 100vh;
  background: linear-gradient(135deg, #0f0f1e 0%, #1a1a2e 40%, #16213e 100%);
  color: #e0e0e0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #e0f4ff;
  margin: 0;
}

.content-grid {
  display: flex;
  gap: 20px;
}

.tree-card, .list-card {
  flex: 1;
  background: rgba(25, 25, 45, 0.6);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #c0d8e8;
}

.tree-wrapper {
  padding: 4px 0;
  min-height: 200px;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 2px 0;
}

.tree-label {
  font-size: 13px;
  color: #e0e0e0;
  font-weight: 500;
}

.tree-tag {
  font-size: 10px;
}

.tree-code {
  font-size: 11px;
  color: rgba(120, 180, 210, 0.5);
  font-family: monospace;
}

.tree-actions {
  margin-left: auto;
  display: flex;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.2s;
}

.tree-node:hover .tree-actions {
  opacity: 1;
}

.empty-hint {
  text-align: center;
  color: rgba(180, 200, 220, 0.5);
  padding: 40px 0;
  font-size: 13px;
}

/* Deep Element Plus dark theme */
:deep(.el-card__header) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  padding: 14px 20px;
}

:deep(.el-tree) {
  background: transparent;
  color: #e0e0e0;
}

:deep(.el-tree-node__content) {
  background: transparent;
}

:deep(.el-tree-node__content:hover) {
  background: rgba(255, 255, 255, 0.05);
}

:deep(.el-tree--highlight-current .el-tree-node.is-current > .el-tree-node__content) {
  background: rgba(0, 150, 220, 0.15);
}

:deep(.el-form-item__label) {
  color: #c0c4cc;
}

:deep(.el-input__wrapper), :deep(.el-select__wrapper) {
  background-color: rgba(15, 15, 30, 0.6);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.1) inset;
}

:deep(.el-input__inner) {
  color: #e0e0e0;
}

:deep(.el-table) {
  background-color: transparent;
  --el-table-border-color: rgba(255, 255, 255, 0.08);
  --el-table-header-bg-color: rgba(30, 30, 50, 0.8);
  --el-table-header-text-color: #e0e0e0;
  --el-table-text-color: #c0c4cc;
  --el-table-row-hover-bg-color: rgba(255, 255, 255, 0.05);
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
}

:deep(.el-table th.el-table__cell) {
  background-color: rgba(30, 30, 50, 0.8) !important;
}

:deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}
</style>
