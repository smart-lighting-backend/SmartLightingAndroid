<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import {
  ElButton, ElTable, ElTableColumn, ElTag, ElDialog, ElForm, ElFormItem, ElInput,
  ElSelect, ElOption, ElMessage, ElMessageBox, ElTree, ElCard, ElTabs, ElTabPane,
  ElPopconfirm, ElEmpty, ElBadge, ElTooltip
} from 'element-plus'
import { Plus, Edit, Delete, Search, Check, Refresh, FolderOpened } from '@element-plus/icons-vue'
import { fetchPermissionTree, fetchPermissionList, createPermission, updatePermission, deletePermission } from '../api/permission.js'
import { fetchRoleList, fetchRoleById, assignRolePermissions } from '../api/role.js'
import { refreshPermissionsAndMenus } from '../api/auth.js'
import { useUserInfo } from '../composables/useUserInfo.js'

const { hasPerm } = useUserInfo()

const activeTab = ref('crud')

// ═══════════════════ 权限定义 Tab ═══════════════════
const permissionList = ref([])
const treeData = ref([])
const loading = ref(false)
const searchText = ref('')
const selectedTreeNode = ref(null)

const dialogVisible = ref(false)
const dialogType = ref('add')
const formRef = ref(null)
const formData = ref({
  id: null, name: '', code: '', type: 'ACTION', parentId: null, description: ''
})
const rules = {
  name: [{ required: true, message: '请输入权限名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入权限编码', trigger: 'blur' }],
  type: [{ required: true, message: '请选择权限类型', trigger: 'change' }]
}

// ═══════════════════ 角色权限分配 Tab ═══════════════════
const roleList = ref([])
const selectedRoleId = ref(null)
const assignTreeData = ref([])
const assignCheckedKeys = ref([])
const assignLoading = ref(false)
const assignSaving = ref(false)
const assignTreeRef = ref(null)
const assignFilterText = ref('')
const selectedRole = computed(() => roleList.value.find(r => r.id === selectedRoleId.value))

const checkedCount = ref(0)
const totalPermCount = ref(0)

const loadData = async () => {
  loading.value = true
  try {
    const [treeRes, listRes] = await Promise.all([
      fetchPermissionTree(),
      fetchPermissionList()
    ])
    treeData.value = treeRes?.code === 200 ? (treeRes.data || []) : (treeRes || [])
    permissionList.value = listRes?.code === 200 ? (listRes.data || []) : (listRes || [])
  } catch {
    ElMessage.error('获取权限数据失败')
  } finally {
    loading.value = false
  }
}

// 过滤树（搜索用）
const filteredTreeData = computed(() => {
  if (!searchText.value) return treeData.value
  const filter = (nodes) => {
    return nodes.reduce((acc, node) => {
      const matchName = node.name?.toLowerCase().includes(searchText.value.toLowerCase())
      const matchCode = node.permissionCode?.toLowerCase().includes(searchText.value.toLowerCase())
      const filteredChildren = node.children ? filter(node.children) : []
      if (matchName || matchCode || filteredChildren.length > 0) {
        acc.push({ ...node, children: filteredChildren.length > 0 ? filteredChildren : node.children })
      }
      return acc
    }, [])
  }
  return filter(treeData.value)
})

// 过滤后的列表
const filteredList = computed(() => {
  if (!searchText.value) return permissionList.value
  const kw = searchText.value.toLowerCase()
  return permissionList.value.filter(p =>
    p.name?.toLowerCase().includes(kw) ||
    p.permissionCode?.toLowerCase().includes(kw) ||
    p.description?.toLowerCase().includes(kw)
  )
})

const typeTag = (type) => {
  return type === 'MODULE'
    ? { type: '', text: '模块', class: 'tag-module' }
    : { type: 'success', text: '操作', class: 'tag-action' }
}

const defaultProps = { children: 'children', label: 'name' }

// ─── CRUD 操作 ────────────────────────────
const handleAdd = (parent) => {
  dialogType.value = 'add'
  formData.value = {
    id: null, name: '', code: '',
    type: parent?.type === 'MODULE' ? 'ACTION' : 'ACTION',
    parentId: parent?.id || null, description: ''
  }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const handleEdit = (row) => {
  dialogType.value = 'edit'
  formData.value = {
    id: row.id, name: row.name, code: row.permissionCode,
    type: row.type || 'ACTION', parentId: row.parentId || null,
    description: row.description || ''
  }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    const payload = { ...formData.value, permissionCode: formData.value.code }
    delete payload.code
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
    if (error?.message) ElMessage.error(error.message || '操作失败')
  }
}

const handleDelete = async (row) => {
  try {
    await deletePermission(row.id)
    ElMessage.success(`已删除权限「${row.name}」`)
    loadData()
  } catch {
    ElMessage.error('删除失败')
  }
}

const handleTreeSelect = (node) => {
  selectedTreeNode.value = node
}

// ═══════════════════ 角色权限分配 ═══════════════════
const loadRoles = async () => {
  try {
    const res = await fetchRoleList()
    roleList.value = res?.code === 200 ? (res.data || []) : (res || [])
  } catch {
    roleList.value = []
  }
}

const loadAssignData = async () => {
  if (!selectedRoleId.value) {
    assignTreeData.value = []
    assignCheckedKeys.value = []
    checkedCount.value = 0
    return
  }
  assignLoading.value = true
  try {
    const res = await fetchRoleById(selectedRoleId.value)
    const data = res?.code === 200 ? res.data : res
    const permCodes = data?.permissionCodes || []
    const allPerms = data?.allPermissions || []

    const checkedIds = allPerms
      .filter(p => permCodes.includes(p.permissionCode))
      .map(p => p.id)
    assignCheckedKeys.value = checkedIds
    checkedCount.value = checkedIds.length

    const buildAssignTree = (parentId) => {
      const children = allPerms.filter(p =>
        (parentId === null && (p.parentId === null || p.parentId === 0)) ||
        (parentId !== null && p.parentId === parentId)
      )
      return children
        .sort((a, b) => {
          if (a.type !== b.type) return a.type === 'MODULE' ? -1 : 1
          return (a.sort ?? 0) - (b.sort ?? 0)
        })
        .map(c => ({
          id: c.id,
          label: c.name,
          code: c.permissionCode,
          type: c.type,
          children: buildAssignTree(c.id)
        }))
    }

    assignTreeData.value = buildAssignTree(null)
    totalPermCount.value = allPerms.length

    // 确保树加载后正确设置已勾选节点
    await nextTick()
    assignTreeRef.value?.setCheckedKeys(checkedIds, false)
    updateCheckedCount()
  } catch {
    ElMessage.error('获取角色权限失败')
  } finally {
    assignLoading.value = false
  }
}

// 搜索过滤分配树
const filteredAssignTree = computed(() => {
  if (!assignFilterText.value) return assignTreeData.value
  const kw = assignFilterText.value.toLowerCase()
  const filter = (nodes) => {
    return nodes.reduce((acc, node) => {
      const match = node.label?.toLowerCase().includes(kw) || node.code?.toLowerCase().includes(kw)
      const filteredChildren = node.children ? filter(node.children) : []
      if (match || filteredChildren.length > 0) {
        acc.push({ ...node, children: filteredChildren.length > 0 ? filteredChildren : node.children })
      }
      return acc
    }, [])
  }
  return filter(assignTreeData.value)
})

const handleCheckAll = () => {
  assignTreeRef.value?.setCheckedNodes(assignTreeData.value)
  updateCheckedCount()
}

const handleUncheckAll = () => {
  assignTreeRef.value?.setCheckedKeys([])
  checkedCount.value = 0
}

const handleExpandAll = () => {
  loadAssignData()
}

const updateCheckedCount = () => {
  const keys = assignTreeRef.value?.getCheckedKeys() || []
  checkedCount.value = keys.length
}

/**
 * 自动补全 :read 权限
 * 勾选了 MODULE 但没勾选对应的 :read 子权限时，自动补上
 */
const ensureReadPermissions = (checkedKeys, treeNodes) => {
  const result = new Set(checkedKeys)
  const added = []

  const isReadAction = (child) => {
    if (child.type !== 'ACTION') return false
    const code = child.code || child.permissionCode || ''
    return code.endsWith(':read') || code.includes(':read')
  }

  const walk = (nodes) => {
    for (const node of nodes) {
      if (node.type === 'MODULE' && result.has(node.id) && node.children?.length) {
        const readChild = node.children.find(isReadAction)
        if (readChild && !result.has(readChild.id)) {
          result.add(readChild.id)
          added.push(`${node.label} → ${readChild.label}`)
        }
      }
      if (node.children?.length) walk(node.children)
    }
  }

  walk(treeNodes)

  if (added.length > 0) {
    console.log('[perm] 自动补全 :read 权限:', added.join(', '))
  }
  return Array.from(result)
}

const handleSaveAssign = async () => {
  if (!selectedRoleId.value) {
    ElMessage.warning('请先选择角色')
    return
  }
  assignSaving.value = true
  try {
    let checkedKeys = assignTreeRef.value?.getCheckedKeys() || []

    // 自动补全 :read 权限：勾选了模块但没勾选查看权限 → 自动加上
    checkedKeys = ensureReadPermissions(checkedKeys, assignTreeData.value)

    await assignRolePermissions(selectedRoleId.value, checkedKeys)
    ElMessage.success(`已为角色「${selectedRole.value?.name}」分配 ${checkedKeys.length} 个权限`)
    checkedCount.value = checkedKeys.length

    // 刷新当前用户权限（如果修改的是自己的角色）
    await refreshPermissionsAndMenus()
    loadAssignData()
  } catch (error) {
    ElMessage.error(error.message || '权限分配失败')
  } finally {
    assignSaving.value = false
  }
}

watch(selectedRoleId, () => loadAssignData())

onMounted(() => {
  loadData()
  loadRoles()
})
</script>

<template>
  <div class="permission-container">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">权限管理</h2>
        <span class="page-subtitle">管理系统权限定义与角色授权</span>
      </div>
    </div>

    <ElTabs v-model="activeTab" type="border-card" class="perm-tabs">
      <!-- ═══════════ Tab1: 权限定义 ═══════════════ -->
      <ElTabPane label="权限定义" name="crud">
        <!-- 顶部操作栏 -->
        <div class="toolbar">
          <div class="toolbar-left">
            <ElButton v-if="hasPerm('permission:create')" type="primary" @click="handleAdd(null)">
              <Plus /> 新增权限
            </ElButton>
          </div>
          <div class="toolbar-right">
            <ElInput
              v-model="searchText"
              placeholder="搜索权限名称或编码..."
              :prefix-icon="Search"
              clearable
              class="search-input"
            />
            <ElTooltip content="刷新">
              <ElButton :icon="Refresh" circle @click="loadData" :loading="loading" />
            </ElTooltip>
          </div>
        </div>

        <!-- 双栏布局 -->
        <div class="content-grid">
          <!-- 左侧：权限树 -->
          <ElCard class="tree-card" shadow="never">
            <template #header>
              <div class="card-header">
                <span class="card-title"><FolderOpened /> 权限树</span>
                <ElTag size="small" type="info">{{ filteredList.length }} 个权限</ElTag>
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
                @node-click="handleTreeSelect"
              >
                <template #default="{ data }">
                  <span class="tree-node">
                    <span class="tree-node-left">
                      <span class="tree-label">{{ data.name }}</span>
                      <span :class="['tree-tag', typeTag(data.type).class]">
                        {{ typeTag(data.type).text }}
                      </span>
                      <span class="tree-code">{{ data.permissionCode }}</span>
                    </span>
                    <span class="tree-node-actions">
                      <ElButton v-if="hasPerm('permission:create')" size="small" text type="primary" @click.stop="handleAdd(data)">
                        <Plus style="width:14px;height:14px" />
                      </ElButton>
                      <ElButton v-if="hasPerm('permission:update')" size="small" text type="warning" @click.stop="handleEdit(data)">
                        <Edit style="width:14px;height:14px" />
                      </ElButton>
                      <ElPopconfirm
                        v-if="hasPerm('permission:delete')"
                        title="确定删除此权限？"
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
              <ElEmpty v-else description="暂无权限数据" />
            </div>
          </ElCard>

          <!-- 右侧：权限表格 -->
          <ElCard class="list-card" shadow="never">
            <template #header>
              <div class="card-header">
                <span class="card-title">权限列表</span>
              </div>
            </template>
            <div class="table-wrapper" v-loading="loading">
              <ElTable
                v-if="filteredList.length > 0"
                :data="filteredList"
                border stripe
                style="width: 100%"
                max-height="500"
                size="small"
              >
                <ElTableColumn prop="id" label="ID" width="60" align="center" />
                <ElTableColumn prop="name" label="名称" min-width="130" show-overflow-tooltip />
                <ElTableColumn prop="permissionCode" label="编码" min-width="140" show-overflow-tooltip>
                  <template #default="{ row }">
                    <code class="perm-code">{{ row.permissionCode }}</code>
                  </template>
                </ElTableColumn>
                <ElTableColumn label="类型" width="75" align="center">
                  <template #default="{ row }">
                    <span :class="['inline-tag', typeTag(row.type).class]">
                      {{ typeTag(row.type).text }}
                    </span>
                  </template>
                </ElTableColumn>
                <ElTableColumn prop="description" label="描述" min-width="140" show-overflow-tooltip>
                  <template #default="{ row }">
                    <span class="desc-text">{{ row.description || '-' }}</span>
                  </template>
                </ElTableColumn>
                <ElTableColumn label="操作" width="140" fixed="right" align="center">
                  <template #default="{ row }">
                    <ElButton v-if="hasPerm('permission:update')" size="small" text type="primary" @click="handleEdit(row)">
                      <Edit /> 编辑
                    </ElButton>
                    <ElPopconfirm
                      v-if="hasPerm('permission:delete')"
                      title="确认删除？"
                      @confirm="handleDelete(row)"
                    >
                      <template #reference>
                        <ElButton size="small" text type="danger">
                          <Delete /> 删除
                        </ElButton>
                      </template>
                    </ElPopconfirm>
                  </template>
                </ElTableColumn>
              </ElTable>
              <ElEmpty v-else description="暂无匹配的权限" />
            </div>
          </ElCard>
        </div>
      </ElTabPane>

      <!-- ═══════════ Tab2: 角色权限分配 ═══════════════ -->
      <ElTabPane name="assign">
        <template #label>
          <span>角色权限分配</span>
          <ElBadge
            v-if="selectedRoleId && checkedCount > 0"
            :value="checkedCount"
            class="tab-badge"
            type="primary"
          />
        </template>

        <div class="assign-panel">
          <!-- 顶部：角色选择 + 操作按钮 -->
          <div class="assign-toolbar">
            <div class="assign-toolbar-left">
              <span class="selector-label">目标角色：</span>
              <ElSelect
                v-model="selectedRoleId"
                placeholder="请选择要分配权限的角色"
                style="width: 280px"
                clearable
                filterable
              >
                <ElOption
                  v-for="role in roleList"
                  :key="role.id"
                  :label="`${role.name}  (${role.roleCode})`"
                  :value="role.id"
                >
                  <span style="float:left">{{ role.name }}</span>
                  <span style="float:right;color:var(--el-text-color-secondary);font-size:12px">
                    {{ role.roleCode }}
                  </span>
                </ElOption>
              </ElSelect>

              <template v-if="selectedRoleId">
                <ElTag size="small" type="primary" effect="dark" round>
                  已分配 {{ checkedCount }} 个权限
                </ElTag>
              </template>
            </div>

            <div v-if="selectedRoleId" class="assign-toolbar-right">
              <ElInput
                v-model="assignFilterText"
                placeholder="筛选项..."
                :prefix-icon="Search"
                clearable
                size="small"
                style="width:180px"
              />
              <ElButton size="small" @click="handleCheckAll">全选</ElButton>
              <ElButton size="small" @click="handleUncheckAll">全不选</ElButton>
              <ElButton size="small" @click="loadAssignData" :loading="assignLoading">
                <Refresh /> 刷新
              </ElButton>
              <ElButton
                type="primary"
                size="small"
                @click="handleSaveAssign"
                :loading="assignSaving"
                :icon="Check"
              >
                保存分配
              </ElButton>
            </div>
          </div>

          <!-- 权限树 -->
          <div v-if="selectedRoleId" class="assign-tree-wrapper" v-loading="assignLoading">
            <div v-if="filteredAssignTree.length > 0" class="assign-tree-scroll">
              <ElTree
                ref="assignTreeRef"
                :data="filteredAssignTree"
                show-checkbox
                node-key="id"
                :default-expand-all="true"
                check-strictly
                @check="updateCheckedCount"
              >
                <template #default="{ data }">
                  <span class="assign-tree-node" :class="{ 'is-module': data.type === 'MODULE' }">
                    <span class="assign-node-label">{{ data.label }}</span>
                    <span :class="['tree-tag', typeTag(data.type).class]">
                      {{ typeTag(data.type).text }}
                    </span>
                    <code class="assign-node-code">{{ data.code }}</code>
                  </span>
                </template>
              </ElTree>
            </div>
            <ElEmpty v-else description="该角色暂无可用权限" />
          </div>
          <div v-else class="assign-placeholder">
            <ElEmpty description="请先选择一个角色，再为其分配功能权限" />
          </div>
        </div>
      </ElTabPane>
    </ElTabs>

    <!-- ═══════════ 新增/编辑弹窗 ═══════════════ -->
    <ElDialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? '新增权限' : '编辑权限'"
      width="520px"
      custom-class="dark-dialog"
      :close-on-click-modal="false"
    >
      <ElForm ref="formRef" :model="formData" :rules="rules" label-width="90px" label-position="top">
        <div class="form-row">
          <ElFormItem label="权限名称" prop="name" style="flex:1">
            <ElInput v-model="formData.name" placeholder="如：设备管理" />
          </ElFormItem>
          <ElFormItem label="权限编码" prop="code" style="flex:1">
            <ElInput v-model="formData.code" placeholder="如：device:read" />
          </ElFormItem>
        </div>
        <div class="form-row">
          <ElFormItem label="权限类型" prop="type" style="flex:1">
            <ElSelect v-model="formData.type" style="width: 100%">
              <ElOption label="模块" value="MODULE">
                <div class="select-option">
                  <span class="select-option-dot mod-dot"></span>
                  <span>模块（MODULE）— 作为权限容器</span>
                </div>
              </ElOption>
              <ElOption label="操作" value="ACTION">
                <div class="select-option">
                  <span class="select-option-dot act-dot"></span>
                  <span>操作（ACTION）— 具体增删改查</span>
                </div>
              </ElOption>
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="父级权限" prop="parentId" style="flex:1">
            <ElSelect
              v-model="formData.parentId"
              placeholder="不选则为顶级"
              clearable
              style="width: 100%"
            >
              <ElOption
                v-for="item in permissionList.filter(p => p.type === 'MODULE')"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </ElSelect>
          </ElFormItem>
        </div>
        <ElFormItem label="描述说明" prop="description">
          <ElInput
            v-model="formData.description"
            type="textarea"
            :rows="2"
            placeholder="可选，简要说明此权限的用途"
          />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <div class="dialog-footer">
          <ElButton @click="dialogVisible = false">取消</ElButton>
          <ElButton type="primary" @click="handleSubmit" :loading="false">
            {{ dialogType === 'add' ? '立即创建' : '保存修改' }}
          </ElButton>
        </div>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
/* ═══════════════ 整体 ═══════════════ */
.permission-container {
  padding: 24px;
  min-height: 100vh;
  background: linear-gradient(135deg, #0f0f1e 0%, #1a1a2e 40%, #16213e 100%);
  color: #e0e0e0;
}

.page-header {
  margin-bottom: 20px;
}
.header-left {
  display: flex;
  align-items: baseline;
  gap: 14px;
}
.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #e0f4ff;
  margin: 0;
}
.page-subtitle {
  font-size: 13px;
  color: rgba(160, 200, 230, 0.5);
}

.perm-tabs {
  background: rgba(25, 25, 45, 0.55);
  border: 1px solid rgba(255, 255, 255, 0.07);
  border-radius: 14px;
  overflow: hidden;
}
.tab-badge {
  margin-left: 6px;
}

/* ═══════════════ 操作栏 ═══════════════ */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 12px;
}
.toolbar-left, .toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.search-input {
  width: 220px;
}

/* ═══════════════ 双栏布局 ═══════════════ */
.content-grid {
  display: flex;
  gap: 18px;
}
.tree-card {
  width: 420px;
  min-width: 420px;
}
.list-card {
  flex: 1;
  min-width: 0;
}
.tree-card, .list-card {
  background: rgba(22, 22, 42, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #c0d8e8;
  display: flex;
  align-items: center;
  gap: 6px;
}

.tree-wrapper {
  min-height: 280px;
  max-height: 540px;
  overflow-y: auto;
}
.table-wrapper {
  min-height: 280px;
}

/* ─── 树节点 ─────────────────────── */
.tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding-right: 4px;
}
.tree-node-left {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}
.tree-label {
  font-weight: 500;
  color: #d0e0f0;
  white-space: nowrap;
}
.tree-tag {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 600;
  white-space: nowrap;
  line-height: 1.5;
}
.tag-module {
  background: rgba(64, 158, 255, 0.15);
  color: #409eff;
  border: 1px solid rgba(64, 158, 255, 0.25);
}
.tag-action {
  background: rgba(103, 194, 58, 0.12);
  color: #67c23a;
  border: 1px solid rgba(103, 194, 58, 0.2);
}
.tree-code {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.28);
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.tree-node-actions {
  display: flex;
  gap: 0;
  opacity: 0;
  transition: opacity 0.15s;
  flex-shrink: 0;
}
.tree-node:hover .tree-node-actions {
  opacity: 1;
}

/* ─── 表格 ─────────────────────── */
.perm-code {
  font-size: 12px;
  background: rgba(255, 255, 255, 0.05);
  padding: 1px 6px;
  border-radius: 3px;
  font-family: monospace;
  color: #90caf9;
}
.inline-tag {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 600;
}
.desc-text {
  color: rgba(255, 255, 255, 0.45);
  font-size: 12px;
}

/* ═══════════════ 角色权限分配 ═══════════════ */
.assign-panel {
  padding: 4px 0;
}
.assign-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: rgba(15, 20, 38, 0.4);
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.05);
}
.assign-toolbar-left, .assign-toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.selector-label {
  font-weight: 600;
  color: #b0cde0;
  white-space: nowrap;
  font-size: 14px;
}

.assign-tree-wrapper {
  padding: 16px;
  background: rgba(15, 20, 35, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 10px;
  min-height: 360px;
}
.assign-tree-scroll {
  max-height: 500px;
  overflow-y: auto;
}

.assign-tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
}
.assign-node-label {
  font-weight: 500;
  color: #d0e0f0;
}
.is-module .assign-node-label {
  font-weight: 600;
  color: #e0f0ff;
}
.is-module {
  padding: 2px 0;
}
.assign-node-code {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.3);
  font-family: monospace;
  margin-left: auto;
}

.assign-placeholder {
  padding: 80px 0;
}

/* ═══════════════ 弹窗 ═══════════════ */
.form-row {
  display: flex;
  gap: 16px;
}
.select-option {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}
.select-option-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.mod-dot { background: #409eff; }
.act-dot { background: #67c23a; }

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
