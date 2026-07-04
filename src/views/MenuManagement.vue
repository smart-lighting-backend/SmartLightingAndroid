<script setup>
import { ref, onMounted } from 'vue'
import {
  ElButton, ElTable, ElTableColumn, ElTag, ElDialog, ElForm, ElFormItem, ElInput, ElSelect, ElOption, ElSwitch, ElMessage, ElMessageBox, ElTree, ElCard, ElInputNumber
} from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import { fetchMenuTree, fetchMenuList, createMenu, updateMenu, deleteMenu } from '../api/menu.js'
import { useUserInfo } from '../composables/useUserInfo.js'

const { hasPerm } = useUserInfo()

const menuList = ref([])
const treeData = ref([])
const loading = ref(false)

const dialogVisible = ref(false)
const dialogType = ref('add')
const formRef = ref(null)
const formData = ref({
  id: null,
  parentId: null,
  name: '',
  permissionCode: '',
  icon: '',
  path: '',
  component: '',
  sort: 0,
  enabled: true
})

const rules = {
  name: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  path: [{ required: true, message: '请输入路由路径', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const treeRes = await fetchMenuTree()
    if (treeRes?.code === 200) {
      treeData.value = treeRes.data || []
    } else {
      treeData.value = treeRes || []
    }
    const listRes = await fetchMenuList()
    if (listRes?.code === 200) {
      menuList.value = listRes.data || []
    } else {
      menuList.value = listRes || []
    }
  } catch (error) {
    ElMessage.error('获取菜单数据失败')
  } finally {
    loading.value = false
  }
}

const flatMenuTree = (nodes, result = []) => {
  for (const node of nodes) {
    result.push(node)
    if (node.children && node.children.length > 0) {
      flatMenuTree(node.children, result)
    }
  }
  return result
}

const handleAdd = (parent) => {
  dialogType.value = 'add'
  formData.value = {
    id: null,
    parentId: parent?.id || null,
    name: '',
    permissionCode: '',
    icon: '',
    path: '',
    component: '',
    sort: 0,
    enabled: true
  }
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogType.value = 'edit'
  formData.value = {
    id: row.id,
    parentId: row.parentId || null,
    name: row.name,
    permissionCode: row.permissionCode || '',
    icon: row.icon || '',
    path: row.path || '',
    component: row.component || '',
    sort: row.sort ?? 0,
    enabled: row.enabled !== false
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
          await createMenu(payload)
          ElMessage.success('新增菜单成功')
        } else {
          await updateMenu(payload.id, payload)
          ElMessage.success('修改菜单成功')
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
  ElMessageBox.confirm(`确定要删除菜单 "${row.name}" 吗？`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteMenu(row.id)
      ElMessage.success('删除成功')
      loadData()
    } catch (error) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

const statusTag = (enabled) => {
  return enabled
    ? { type: 'success', text: '启用' }
    : { type: 'danger', text: '停用' }
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
  <div class="menu-container">
    <div class="page-header">
      <h2 class="page-title">菜单管理</h2>
      <ElButton v-if="hasPerm('menu:create')" type="primary" @click="handleAdd(null)"><Plus /> 新增菜单</ElButton>
    </div>

    <div class="content-grid">
      <!-- 菜单树 -->
      <ElCard class="tree-card" shadow="never">
        <template #header>
          <span class="card-title">菜单树结构</span>
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
                <ElTag v-if="data.permissionCode" size="small" type="info" class="tree-tag">
                  {{ data.permissionCode }}
                </ElTag>
                <ElTag size="small" :type="data.enabled !== false ? 'success' : 'danger'" class="tree-tag">
                  {{ data.enabled !== false ? '启用' : '停用' }}
                </ElTag>
                <span class="tree-actions">
                  <ElButton v-if="hasPerm('menu:create')" size="small" type="primary" link @click.stop="handleAdd(data)">新增子菜单</ElButton>
                  <ElButton v-if="hasPerm('menu:update')" size="small" type="warning" link @click.stop="handleEdit(data)">编辑</ElButton>
                  <ElButton v-if="hasPerm('menu:delete')" size="small" type="danger" link @click.stop="handleDelete(data)">删除</ElButton>
                </span>
              </span>
            </template>
          </ElTree>
          <div v-if="!loading && treeData.length === 0" class="empty-hint">
            暂无数据，请点击"新增菜单"添加
          </div>
        </div>
      </ElCard>

      <!-- 菜单列表 -->
      <ElCard class="list-card" shadow="never">
        <template #header>
          <span class="card-title">菜单列表</span>
        </template>
        <ElTable :data="menuList" border stripe style="width: 100%" v-loading="loading" max-height="500">
          <ElTableColumn prop="id" label="ID" width="60" />
          <ElTableColumn prop="name" label="菜单名称" min-width="120" />
          <ElTableColumn prop="path" label="路由路径" min-width="120" />
          <ElTableColumn prop="permissionCode" label="权限编码" min-width="120">
            <template #default="{ row }">
              <span v-if="row.permissionCode" class="code-text">{{ row.permissionCode }}</span>
              <span v-else class="no-code">-</span>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="icon" label="图标" width="70" />
          <ElTableColumn prop="sort" label="排序" width="60" />
          <ElTableColumn label="状态" width="70">
            <template #default="{ row }">
              <ElTag :type="statusTag(row.enabled !== false).type" size="small">
                {{ statusTag(row.enabled !== false).text }}
              </ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <ElButton v-if="hasPerm('menu:update')" type="primary" link size="small" @click="handleEdit(row)"><Edit /> 编辑</ElButton>
              <ElButton v-if="hasPerm('menu:delete')" type="danger" link size="small" @click="handleDelete(row)"><Delete /> 删除</ElButton>
            </template>
          </ElTableColumn>
        </ElTable>
      </ElCard>
    </div>

    <!-- 新增/编辑弹窗 -->
    <ElDialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? '新增菜单' : '编辑菜单'"
      width="550px"
      custom-class="dark-dialog"
    >
      <ElForm ref="formRef" :model="formData" :rules="rules" label-width="100px">
        <ElFormItem label="菜单名称" prop="name">
          <ElInput v-model="formData.name" placeholder="如：用户管理" />
        </ElFormItem>
        <ElFormItem label="父级菜单" prop="parentId">
          <ElSelect v-model="formData.parentId" placeholder="不选则为顶级菜单" clearable style="width: 100%">
            <ElOption
              v-for="item in flatMenuTree(treeData)"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="路由路径" prop="path">
          <ElInput v-model="formData.path" placeholder="如：/system/menu" />
        </ElFormItem>
        <ElFormItem label="组件路径" prop="component">
          <ElInput v-model="formData.component" placeholder="如：views/MenuManagement.vue" />
        </ElFormItem>
        <ElFormItem label="权限编码" prop="permissionCode">
          <ElInput v-model="formData.permissionCode" placeholder="用于控制菜单可见性" />
        </ElFormItem>
        <ElFormItem label="图标" prop="icon">
          <ElInput v-model="formData.icon" placeholder="图标名称（可选）" />
        </ElFormItem>
        <ElFormItem label="排序号" prop="sort">
          <ElInputNumber v-model="formData.sort" :min="0" :max="999" style="width: 120px" />
        </ElFormItem>
        <ElFormItem label="启用状态" prop="enabled">
          <ElSwitch v-model="formData.enabled" />
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
.menu-container {
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

.code-text {
  font-family: monospace;
  color: #80c8f0;
  font-size: 12px;
}

.no-code {
  color: rgba(180, 200, 220, 0.3);
}

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
