<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import {
  ElButton, ElTable, ElTableColumn, ElTag, ElDialog, ElForm, ElFormItem, ElInput,
  ElSelect, ElOption, ElMessage, ElMessageBox, ElTree, ElCard, ElTabs, ElTabPane,
  ElPopconfirm, ElEmpty, ElBadge, ElTooltip, ElCheckbox
} from 'element-plus'
import { Search, Refresh, Check } from '@element-plus/icons-vue'
import { fetchRoleList, fetchRoleById, assignRolePermissions } from '../api/role.js'
import { refreshPermissionsAndMenus } from '../api/auth.js'

// ═══════════════════ 角色权限分配 ═══════════════════
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
  loadRoles()
})

const typeTag = (type) => {
  return type === 'MODULE'
    ? { type: '', text: '模块', class: 'tag-module' }
    : { type: 'success', text: '操作', class: 'tag-action' }
}
</script>

<template>
  <div class="permission-container">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">权限管理</h2>
        <span class="page-subtitle">为角色分配功能权限</span>
      </div>
    </div>

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

/* ─── 标签 ─────────────────────── */
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
</style>
