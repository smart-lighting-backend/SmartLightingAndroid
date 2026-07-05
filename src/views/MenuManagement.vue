<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import {
  ElButton, ElTable, ElTableColumn, ElTag, ElDialog, ElForm, ElFormItem, ElInput,
  ElSelect, ElOption, ElSwitch, ElMessage, ElMessageBox, ElTree, ElCard,
  ElPopconfirm, ElEmpty, ElTooltip
} from 'element-plus'
import { Plus, Edit, Delete, Search, Refresh, FolderOpened } from '@element-plus/icons-vue'
import { fetchMenuTree, fetchMenuList, createMenu, updateMenu, deleteMenu } from '../api/menu.js'
import { useUserInfo } from '../composables/useUserInfo.js'

const { hasPerm } = useUserInfo()

const menuList = ref([])
const treeData = ref([])
const loading = ref(false)
const searchText = ref('')

const dialogVisible = ref(false)
const dialogType = ref('add')
const formRef = ref(null)
const formData = ref({
  id: null, parentId: null, name: '',
  path: '', component: '', permissionCode: '',
  icon: '', sort: 0, enabled: true
})

const rules = {
  name: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
}

const loadData = async () => {
  loading.value = true
  try {
    const [treeRes, listRes] = await Promise.all([
      fetchMenuTree(),
      fetchMenuList()
    ])
    treeData.value = treeRes?.code === 200 ? (treeRes.data || []) : (treeRes || [])
    menuList.value = listRes?.code === 200 ? (listRes.data || []) : (listRes || [])
  } catch {
    ElMessage.error('获取菜单数据失败')
  } finally {
    loading.value = false
  }
}

// 扁平化树（下拉选项用）
const flatMenuTree = (nodes, result = []) => {
  for (const node of nodes) {
    result.push(node)
    if (node.children?.length > 0) flatMenuTree(node.children, result)
  }
  return result
}

const flatOptions = computed(() => flatMenuTree(treeData.value))

// 搜索过滤树
const filteredTreeData = computed(() => {
  if (!searchText.value) return treeData.value
  const kw = searchText.value.toLowerCase()
  const filter = (nodes) => {
    return nodes.reduce((acc, node) => {
      const match = node.name?.toLowerCase().includes(kw) || node.path?.toLowerCase().includes(kw)
      const filteredChildren = node.children ? filter(node.children) : []
      if (match || filteredChildren.length > 0) {
        acc.push({ ...node, children: filteredChildren.length > 0 ? filteredChildren : node.children })
      }
      return acc
    }, [])
  }
  return filter(treeData.value)
})

// 搜索过滤列表
const filteredList = computed(() => {
  if (!searchText.value) return menuList.value
  const kw = searchText.value.toLowerCase()
  return menuList.value.filter(m =>
    m.name?.toLowerCase().includes(kw) || m.path?.toLowerCase().includes(kw) || m.permissionCode?.toLowerCase().includes(kw)
  )
})

const defaultProps = { children: 'children', label: 'name' }

// ─── 新增 / 编辑 ───────────────────────────
const handleAdd = (parent) => {
  dialogType.value = 'add'
  const parentPath = parent?.path || ''
  const name = ''
  formData.value = {
    id: null,
    parentId: parent?.id || null,
    name: '',
    path: '',
    component: '',
    permissionCode: '',
    icon: '',
    sort: 0,
    enabled: true
  }
  // 预填父级路径，方便用户参考
  if (parent) {
    formData.value.path = parentPath.endsWith('/') ? parentPath : parentPath + '/'
  }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const handleEdit = (row) => {
  dialogType.value = 'edit'
  formData.value = {
    id: row.id,
    parentId: row.parentId || null,
    name: row.name || '',
    path: row.path || '',
    component: row.component || '',
    permissionCode: row.permissionCode || '',
    icon: row.icon || '',
    sort: row.sort ?? 0,
    enabled: row.enabled !== false
  }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    const payload = { ...formData.value }
    // 新增时自动生成 path（如果未填写）
    if (dialogType.value === 'add' && !payload.path) {
      const parent = flatOptions.value.find(o => o.id === payload.parentId)
      const parentPath = parent?.path || ''
      const slug = payload.name.replace(/[^\w\u4e00-\u9fa5]/g, '-').toLowerCase()
      payload.path = parentPath ? `${parentPath}/${slug}` : `/${slug}`
    }
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
    if (error?.message) ElMessage.error(error.message || '操作失败')
  }
}

const handleDelete = async (row) => {
  try {
    await deleteMenu(row.id)
    ElMessage.success(`已删除菜单「${row.name}」`)
    loadData()
  } catch {
    ElMessage.error('删除失败')
  }
}

const statusTag = (enabled) => enabled
  ? { type: 'success', text: '启用' }
  : { type: 'danger', text: '停用' }

onMounted(() => loadData())
</script>

<template>
  <div class="menu-container">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">菜单管理</h2>
        <span class="page-subtitle">管理系统导航菜单结构</span>
      </div>
    </div>

    <!-- 操作栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <ElButton v-if="hasPerm('menu:create')" type="primary" @click="handleAdd(null)">
          <Plus /> 新增菜单
        </ElButton>
      </div>
      <div class="toolbar-right">
        <ElInput
          v-model="searchText"
          placeholder="搜索菜单名称或路径..."
          :prefix-icon="Search"
          clearable
          class="search-input"
        />
        <ElTooltip content="刷新">
          <ElButton :icon="Refresh" circle @click="loadData" :loading="loading" />
        </ElTooltip>
      </div>
    </div>

    <!-- 双栏 -->
    <div class="content-grid">
      <!-- 左侧：菜单树 -->
      <ElCard class="tree-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span class="card-title"><FolderOpened /> 菜单树</span>
            <ElTag size="small" type="info">{{ filteredList.length }} 个菜单</ElTag>
          </div>
        </template>
        <div class="tree-wrapper" v-loading="loading">
          <ElTree
            v-if="filteredTreeData.length > 0"
            :data="filteredTreeData"
            :props="defaultProps"
            node-key="id"
            default-expand-all
            highlight-current
          >
            <template #default="{ data }">
              <span class="tree-node">
                <span class="tree-node-left">
                  <span class="tree-label">{{ data.name }}</span>
                  <span class="tree-path">{{ data.path }}</span>
                  <span
                    v-if="data.children && data.children.length > 0"
                    class="tree-child-count"
                  >{{ data.children.length }} 个子菜单</span>
                </span>
                <span class="tree-node-actions">
                  <ElButton
                    v-if="hasPerm('menu:update')"
                    size="small" text type="warning"
                    @click.stop="handleEdit(data)"
                  >
                    <Edit style="width:14px;height:14px" />
                  </ElButton>
                  <ElPopconfirm
                    v-if="hasPerm('menu:delete')"
                    title="确定删除此菜单？子菜单也将一并删除"
                    @confirm="handleDelete(data)"
                  >
                    <template #reference>
                      <ElButton size="small" text type="danger" @click.stop>
                        <Delete style="width:14px;height:14px" />
                      </ElButton>
                    </template>
                  </ElPopconfirm>
                </span>
              </span>
            </template>
          </ElTree>
          <ElEmpty v-else description="暂无菜单数据" />
        </div>
      </ElCard>

      <!-- 右侧：菜单列表 -->
      <ElCard class="list-card" shadow="never">
        <template #header>
          <span class="card-title">菜单列表</span>
        </template>
        <div class="table-wrapper" v-loading="loading">
          <ElTable
            v-if="filteredList.length > 0"
            :data="filteredList"
            border stripe size="small"
            style="width: 100%" max-height="500"
          >
            <ElTableColumn prop="id" label="ID" width="55" align="center" />
            <ElTableColumn prop="name" label="菜单名称" min-width="130" show-overflow-tooltip />
            <ElTableColumn prop="path" label="路由路径" min-width="130" show-overflow-tooltip>
              <template #default="{ row }">
                <code class="path-code">{{ row.path }}</code>
              </template>
            </ElTableColumn>
            <ElTableColumn prop="permissionCode" label="权限编码" min-width="110">
              <template #default="{ row }">
                <code v-if="row.permissionCode" class="perm-code">{{ row.permissionCode }}</code>
                <span v-else class="no-code">-</span>
              </template>
            </ElTableColumn>
            <ElTableColumn prop="sort" label="排序" width="60" align="center" />
            <ElTableColumn label="状态" width="65" align="center">
              <template #default="{ row }">
                <span :class="['inline-tag', row.enabled !== false ? 'tag-on' : 'tag-off']">
                  {{ row.enabled !== false ? '启用' : '停用' }}
                </span>
              </template>
            </ElTableColumn>
            <ElTableColumn label="操作" width="120" fixed="right" align="center">
              <template #default="{ row }">
                <ElButton v-if="hasPerm('menu:update')" size="small" text type="primary" @click="handleEdit(row)">
                  <Edit /> 编辑
                </ElButton>
                <ElPopconfirm
                  v-if="hasPerm('menu:delete')"
                  title="确认删除？"
                  @confirm="handleDelete(row)"
                >
                  <template #reference>
                    <ElButton size="small" text type="danger"><Delete /> 删除</ElButton>
                  </template>
                </ElPopconfirm>
              </template>
            </ElTableColumn>
          </ElTable>
          <ElEmpty v-else description="暂无匹配的菜单" />
        </div>
      </ElCard>
    </div>

    <!-- 新增/编辑弹窗 -->
    <ElDialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? '新增菜单' : '编辑菜单'"
      width="500px"
      custom-class="dark-dialog"
      :close-on-click-modal="false"
    >
      <ElForm ref="formRef" :model="formData" :rules="rules" label-position="top">
        <!-- 名称 + 父级（新增时这两项最核心） -->
        <div class="form-row">
          <ElFormItem label="菜单名称" prop="name" style="flex:2">
            <ElInput v-model="formData.name" placeholder="如：用户管理" />
          </ElFormItem>
          <ElFormItem label="上级菜单" prop="parentId" style="flex:1">
            <ElSelect v-model="formData.parentId" placeholder="顶级菜单" clearable style="width:100%">
              <ElOption
                v-for="item in flatOptions"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </ElSelect>
          </ElFormItem>
        </div>

        <!-- 新增模式：提示自动填充 -->
        <div v-if="dialogType === 'add'" class="auto-hint">
          路由路径和权限编码将根据菜单名称和上级菜单自动生成，也可在编辑后手动修改。
        </div>

        <!-- 编辑模式：显示全部字段 -->
        <template v-if="dialogType === 'edit'">
          <div class="form-row">
            <ElFormItem label="路由路径" prop="path" style="flex:1">
              <ElInput v-model="formData.path" placeholder="如：/system/menu" />
            </ElFormItem>
            <ElFormItem label="权限编码" prop="permissionCode" style="flex:1">
              <ElInput v-model="formData.permissionCode" placeholder="如：menu" />
            </ElFormItem>
          </div>
          <div class="form-row">
            <ElFormItem label="组件路径" prop="component" style="flex:1">
              <ElInput v-model="formData.component" placeholder="如：views/MenuManagement.vue" />
            </ElFormItem>
            <ElFormItem label="图标" prop="icon" style="flex:1">
              <ElInput v-model="formData.icon" placeholder="如：grid / bulb / setting" />
            </ElFormItem>
          </div>
          <div class="form-row">
            <ElFormItem label="排序号" prop="sort" style="flex:1">
              <ElInput
                v-model.number="formData.sort"
                type="number"
                :min="0"
                placeholder="0"
              />
            </ElFormItem>
            <ElFormItem label="启用状态" prop="enabled" style="flex:1">
              <ElSwitch v-model="formData.enabled" />
            </ElFormItem>
          </div>
        </template>
      </ElForm>
      <template #footer>
        <div class="dialog-footer">
          <ElButton @click="dialogVisible = false">取消</ElButton>
          <ElButton type="primary" @click="handleSubmit">
            {{ dialogType === 'add' ? '立即创建' : '保存修改' }}
          </ElButton>
        </div>
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

.page-header { margin-bottom: 20px; }
.header-left { display: flex; align-items: baseline; gap: 14px; }
.page-title { font-size: 22px; font-weight: 700; color: #e0f4ff; margin: 0; }
.page-subtitle { font-size: 13px; color: rgba(160, 200, 230, 0.5); }

/* ─── 操作栏 ─────────────────────── */
.toolbar {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px; gap: 12px;
}
.toolbar-left, .toolbar-right { display: flex; align-items: center; gap: 8px; }
.search-input { width: 220px; }

/* ─── 双栏 ─────────────────────── */
.content-grid { display: flex; gap: 18px; }
.tree-card { width: 420px; min-width: 420px; }
.list-card { flex: 1; min-width: 0; }
.tree-card, .list-card {
  background: rgba(22, 22, 42, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
}
.card-header { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.card-title {
  font-size: 14px; font-weight: 600; color: #c0d8e8;
  display: flex; align-items: center; gap: 6px;
}
.tree-wrapper { min-height: 280px; max-height: 540px; overflow-y: auto; }
.table-wrapper { min-height: 280px; }

/* ─── 树节点 ─────────────────────── */
.tree-node {
  display: flex; align-items: center; justify-content: space-between;
  width: 100%; padding-right: 4px;
}
.tree-node-left { display: flex; align-items: center; gap: 6px; min-width: 0; }
.tree-label { font-weight: 500; color: #d0e0f0; white-space: nowrap; }
.tree-path {
  font-size: 11px; color: rgba(255, 255, 255, 0.28);
  font-family: monospace; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.tree-child-count {
  font-size: 10px; color: rgba(100, 180, 220, 0.5);
  background: rgba(100, 180, 220, 0.08);
  padding: 1px 6px; border-radius: 8px;
}
.tree-node-actions {
  display: flex; gap: 0; opacity: 0; transition: opacity 0.15s; flex-shrink: 0;
}
.tree-node:hover .tree-node-actions { opacity: 1; }

/* ─── 表格 ─────────────────────── */
.path-code {
  font-size: 12px; background: rgba(255, 255, 255, 0.05);
  padding: 1px 6px; border-radius: 3px; font-family: monospace; color: #90caf9;
}
.perm-code {
  font-size: 11px; background: rgba(100, 200, 100, 0.08);
  padding: 1px 6px; border-radius: 3px; font-family: monospace; color: #80c880;
}
.no-code { color: rgba(255, 255, 255, 0.2); }
.inline-tag {
  font-size: 10px; padding: 1px 6px; border-radius: 4px; font-weight: 600;
}
.tag-on { background: rgba(103, 194, 58, 0.12); color: #67c23a; border: 1px solid rgba(103, 194, 58, 0.2); }
.tag-off { background: rgba(245, 108, 108, 0.1); color: #f56c6c; border: 1px solid rgba(245, 108, 108, 0.15); }

/* ─── 弹窗 ─────────────────────── */
.form-row { display: flex; gap: 16px; }
.auto-hint {
  font-size: 12px; color: rgba(160, 200, 230, 0.5);
  background: rgba(100, 150, 220, 0.06);
  padding: 8px 12px; border-radius: 6px; margin-bottom: 12px;
  border-left: 3px solid rgba(100, 150, 220, 0.3);
}
.dialog-footer { display: flex; justify-content: flex-end; gap: 8px; }

/* ─── Element Plus 覆盖 ─────────────────────── */
:deep(.el-card__header) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  padding: 14px 20px;
}
:deep(.el-tree) { background: transparent; color: #e0e0e0; }
:deep(.el-tree-node__content:hover) { background: rgba(255, 255, 255, 0.05); }
:deep(.el-tree--highlight-current .el-tree-node.is-current > .el-tree-node__content) {
  background: rgba(0, 150, 220, 0.15);
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
:deep(.el-table th.el-table__cell) { background-color: rgba(30, 30, 50, 0.8) !important; }
:deep(.el-table td.el-table__cell) { border-bottom: 1px solid rgba(255, 255, 255, 0.05); }
</style>
