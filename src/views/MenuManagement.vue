<script setup>
import { ref, computed, onMounted, nextTick, inject } from 'vue'
import {
  ElButton, ElTable, ElTableColumn, ElTag, ElDialog, ElForm, ElFormItem, ElInput,
  ElSelect, ElOption, ElMessage, ElTree, ElCard,
  ElEmpty, ElTooltip
} from 'element-plus'
import { Edit, Search, Refresh, FolderOpened } from '@element-plus/icons-vue'
import { fetchMenuTree, fetchMenuList, updateMenu, fetchVisibleMenus } from '../api/menu.js'
import { saveMenus } from '../api/auth.js'
import { useUserInfo } from '../composables/useUserInfo.js'

const { hasPerm } = useUserInfo()

const menuList = ref([])
const treeData = ref([])
const loading = ref(false)
const searchText = ref('')

const dialogVisible = ref(false)
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

// 编辑模式下需要排除自身及子孙，防止循环引用
const editingId = ref(null)

// 收集某个节点的所有子孙 ID
const collectDescendantIds = (nodes, targetId) => {
  for (const node of nodes) {
    if (node.id === targetId) {
      const ids = []
      const walk = (n) => { ids.push(n.id); n.children?.forEach(walk) }
      node.children?.forEach(walk)
      return ids
    }
    if (node.children?.length) {
      const found = collectDescendantIds(node.children, targetId)
      if (found) return found
    }
  }
  return []
}

const parentOptions = computed(() => {
  if (!editingId.value) return flatOptions.value
  const excludeIds = new Set([editingId.value, ...collectDescendantIds(treeData.value, editingId.value)])
  return flatOptions.value.filter(o => !excludeIds.has(o.id))
})

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

// ─── 编辑 ───────────────────────────
const handleEdit = (row) => {
  editingId.value = row.id  // 排除自身及子孙，防止循环引用
  formData.value = {
    id: row.id,
    parentId: row.parentId ?? 0,  // 0 表示无上级（顶级菜单）
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
    // 将 0（前端哨兵值）转为 null 发送给后端
    if (payload.parentId === 0) payload.parentId = null

    // 编辑：沿用原有所有字段，只更新 name 和 parentId
    const original = menuList.value.find(m => m.id === payload.id)
    if (original) {
      payload.path = original.path || ''
      payload.component = original.component || ''
      payload.permissionCode = original.permissionCode || ''
      payload.icon = original.icon || ''
      payload.sort = original.sort ?? 0
      payload.enabled = original.enabled !== false
      // 注意：path 不随 parentId 变化而改变，
      // 因为 path 对应真实路由（如 /system/permission），
      // parentId 只控制侧边栏层级位置。
    }
    await updateMenu(payload.id, payload)
    ElMessage.success('修改菜单成功')
    dialogVisible.value = false
    await loadData()
    await refreshSidebar()
  } catch (error) {
    console.error('[Menu] handleSubmit error:', error)
    ElMessage.error(error?.response?.data?.msg || error?.message || '操作失败，请查看控制台日志')
  }
}

const statusTag = (enabled) => enabled
  ? { type: 'success', text: '启用' }
  : { type: 'danger', text: '停用' }

// 从 MainLayout 获取侧边栏刷新方法
const reloadSidebarMenus = inject('reloadSidebarMenus', null)

// 修改菜单后同步刷新左侧导航栏
const refreshSidebar = async () => {
  try {
    const res = await fetchVisibleMenus()
    const menus = res?.data || res
    if (menus && Array.isArray(menus)) {
      const inLocal = !!localStorage.getItem('smart_light_token')
      saveMenus(menus, inLocal)
      // 直接通知 MainLayout 重新加载菜单
      if (reloadSidebarMenus) {
        await reloadSidebarMenus()
      }
    }
  } catch (e) {
    console.warn('[Menu] refreshSidebar error:', e)
    // 静默失败，不影响主流程
  }
}

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
            <ElTableColumn label="操作" width="80" fixed="right" align="center">
              <template #default="{ row }">
                <ElButton v-if="hasPerm('menu:update')" size="small" text type="primary" @click="handleEdit(row)">
                  <Edit /> 编辑
                </ElButton>
              </template>
            </ElTableColumn>
          </ElTable>
          <ElEmpty v-else description="暂无匹配的菜单" />
        </div>
      </ElCard>
    </div>

    <!-- 编辑弹窗 -->
    <ElDialog
      v-model="dialogVisible"
      title="编辑菜单"
      width="500px"
      custom-class="dark-dialog"
      :close-on-click-modal="false"
    >
      <ElForm ref="formRef" :model="formData" :rules="rules" label-position="top">
        <div class="form-row">
          <ElFormItem label="菜单名称" prop="name" style="flex:2">
            <ElInput v-model="formData.name" placeholder="如：用户管理" />
          </ElFormItem>
          <ElFormItem label="上级菜单" prop="parentId" style="flex:1">
            <ElSelect v-model="formData.parentId" placeholder="顶级菜单（无上级）" clearable style="width:100%">
              <ElOption label="── 顶级菜单（无上级）──" :value="0" />
              <ElOption
                v-for="item in parentOptions"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </ElSelect>
          </ElFormItem>
        </div>

        <div class="auto-hint">
          路径、权限编码、图标等将沿用现有数据；修改上级菜单后路径会自动调整。
        </div>
      </ElForm>
      <template #footer>
        <div class="dialog-footer">
          <ElButton @click="dialogVisible = false">取消</ElButton>
          <ElButton type="primary" @click="handleSubmit">保存修改</ElButton>
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
