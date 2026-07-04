<script setup>
import { ref, onMounted } from 'vue'
import {
  ElInput, ElButton, ElTable, ElTableColumn, ElTag, ElCard, ElDialog, ElForm, ElFormItem, ElSelect, ElOption, ElMessage, ElMessageBox, ElPagination
} from 'element-plus'
import { Search, Plus, Edit, Delete } from '@element-plus/icons-vue'
import { fetchUserList, fetchAllRoles, createUser, updateUser, deleteUser } from '../api/user'
import { useUserInfo } from '../composables/useUserInfo.js'

const { hasPerm } = useUserInfo()

const searchForm = ref({
  username: '',
  realName: '',
  roleId: null,
  department: ''
})

const userList = ref([])
const roleList = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const dialogVisible = ref(false)
const dialogType = ref('add') // 'add' or 'edit'
const formRef = ref(null)

const formData = ref({
  id: null,
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: '',
  department: '',
  areaCode: '',
  roleId: null,
  enabled: true
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码至少 8 位', trigger: 'blur' }
  ],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const getRoleTag = (roleCode) => {
  switch (roleCode) {
    case 'SUPER_ADMIN': return { type: 'danger', text: '系统管理员' }
    case 'MUNICIPAL': return { type: 'warning', text: '市政人员' }
    case 'MAINTENANCE': return { type: 'primary', text: '路灯管理员' }
    case 'EMERGENCY': return { type: 'success', text: '安全应急员' }
    default: return { type: 'info', text: '普通用户' }
  }
}

const loadRoles = async () => {
  try {
    const res = await fetchAllRoles()
    if (res && res.code === 200) {
      roleList.value = res.data
    } else {
      roleList.value = res || []
    }
  } catch (error) {
    console.error('获取角色失败', error)
  }
}

const loadUsers = async () => {
  loading.value = true
  try {
    const query = {
      page: currentPage.value,
      size: pageSize.value,
      ...searchForm.value
    }
    const res = await fetchUserList(query)
    if (res) {
      userList.value = res.records || res.list || []
      total.value = res.total || 0
    }
  } catch (error) {
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadUsers()
}

const handleReset = () => {
  searchForm.value = {
    username: '',
    realName: '',
    roleId: null,
    department: ''
  }
  handleSearch()
}

const handleAdd = () => {
  dialogType.value = 'add'
  formData.value = {
    id: null, username: '', password: '', realName: '', phone: '', email: '', department: '', areaCode: '', roleId: null, enabled: true
  }
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogType.value = 'edit'
  formData.value = { ...row, password: '' }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      // Edit form drops password if empty
      const payload = { ...formData.value }
      if (dialogType.value === 'edit' && !payload.password) {
        delete payload.password
      }
      
      try {
        if (dialogType.value === 'add') {
          await createUser(payload)
          ElMessage.success('新增用户成功')
        } else {
          await updateUser(payload.id, payload)
          ElMessage.success('修改用户成功')
        }
        dialogVisible.value = false
        loadUsers()
      } catch (error) {
        ElMessage.error(error.message || '操作失败')
      }
    }
  })
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除用户 "${row.username}" 吗？`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteUser(row.id)
      ElMessage.success('删除成功')
      loadUsers()
    } catch (error) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

const formatDateTime = (dateRaw) => {
  if (!dateRaw) return '--';
  let dateArr = dateRaw;
  if (typeof dateRaw === 'string' && dateRaw.includes(',')) {
    dateArr = dateRaw.split(',').filter(x => x !== '').map(Number);
  }
  if (Array.isArray(dateArr) && dateArr.length >= 6) {
    const [y, m, d, h, min, s] = dateArr;
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')} ${String(h).padStart(2, '0')}:${String(min).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  }
  try {
    const d = new Date(dateRaw);
    if (isNaN(d.getTime())) return String(dateRaw);
    const pad = (n) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
  } catch(e) {
    return String(dateRaw);
  }
};

onMounted(() => {
  loadRoles()
  loadUsers()
})
</script>

<template>
  <div class="user-list-container">
    <div class="search-bar">
      <ElForm :inline="true" :model="searchForm" class="search-form">
        <ElFormItem label="用户名">
          <ElInput v-model="searchForm.username" placeholder="请输入用户名" clearable />
        </ElFormItem>
        <ElFormItem label="姓名">
          <ElInput v-model="searchForm.realName" placeholder="请输入姓名" clearable />
        </ElFormItem>
        <ElFormItem label="角色">
          <ElSelect v-model="searchForm.roleId" placeholder="请选择角色" clearable style="width: 180px">
            <ElOption v-for="role in roleList" :key="role.id" :label="role.name" :value="role.id" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem>
          <ElButton type="primary" @click="handleSearch"><Search /> 查询</ElButton>
          <ElButton @click="handleReset">重置</ElButton>
          <ElButton v-if="hasPerm('user:create')" type="success" @click="handleAdd"><Plus /> 新增用户</ElButton>
        </ElFormItem>
      </ElForm>
    </div>

    <div class="user-content" v-loading="loading">
      <ElTable :data="userList" border stripe style="width: 100%">
        <ElTableColumn prop="id" label="ID" width="80" />
        <ElTableColumn prop="username" label="用户名" min-width="120" />
        <ElTableColumn prop="realName" label="真实姓名" min-width="100" />
        <ElTableColumn label="角色" min-width="120">
          <template #default="{ row }">
            <ElTag v-if="row.roleCode" :type="getRoleTag(row.roleCode).type">
              {{ row.roleName }}
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="department" label="部门" min-width="120" />
        <ElTableColumn prop="phone" label="联系电话" min-width="120" />
        <ElTableColumn label="状态" width="100">
          <template #default="{ row }">
            <ElTag :type="row.enabled ? 'success' : 'danger'">
              {{ row.enabled ? '正常' : '停用' }}
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn label="创建时间" min-width="160">
          <template #default="{ row }">
            <span class="time-cell">
              {{ formatDateTime(row.createTime) }}
            </span>
          </template>
        </ElTableColumn>
        <ElTableColumn label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <ElButton v-if="hasPerm('user:update')" type="primary" link @click="handleEdit(row)"><Edit /> 编辑</ElButton>
            <ElButton v-if="hasPerm('user:delete')" type="danger" link @click="handleDelete(row)"><Delete /> 删除</ElButton>
          </template>
        </ElTableColumn>
      </ElTable>

      <div class="pagination-wrapper" v-if="total > 0">
        <ElPagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="loadUsers"
          @current-change="loadUsers"
        />
      </div>
    </div>

    <!-- 用户表单弹窗 -->
    <ElDialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? '新增用户' : '编辑用户'"
      width="500px"
      custom-class="dark-dialog"
    >
      <ElForm ref="formRef" :model="formData" :rules="rules" label-width="100px">
        <ElFormItem label="用户名" prop="username">
          <ElInput v-model="formData.username" :disabled="dialogType === 'edit'" placeholder="请输入用户名" />
        </ElFormItem>
        <ElFormItem label="密码" :prop="dialogType === 'add' ? 'password' : ''">
          <ElInput v-model="formData.password" type="password" placeholder="请输入密码" show-password />
          <div v-if="dialogType === 'edit'" class="form-hint">留空表示不修改密码</div>
        </ElFormItem>
        <ElFormItem label="真实姓名" prop="realName">
          <ElInput v-model="formData.realName" placeholder="请输入真实姓名" />
        </ElFormItem>
        <ElFormItem label="角色" prop="roleId">
          <ElSelect v-model="formData.roleId" placeholder="请选择角色" style="width: 100%">
            <ElOption v-for="role in roleList" :key="role.id" :label="role.name" :value="role.id" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="联系电话" prop="phone">
          <ElInput v-model="formData.phone" placeholder="请输入联系电话" />
        </ElFormItem>
        <ElFormItem label="部门" prop="department">
          <ElInput v-model="formData.department" placeholder="请输入所属部门" />
        </ElFormItem>
        <ElFormItem label="账号状态" prop="enabled">
          <ElSelect v-model="formData.enabled" style="width: 100%">
            <ElOption label="正常" :value="true" />
            <ElOption label="停用" :value="false" />
          </ElSelect>
        </ElFormItem>
      </ElForm>
      <template #footer>
        <span class="dialog-footer">
          <ElButton @click="dialogVisible = false">取消</ElButton>
          <ElButton type="primary" @click="handleSubmit">确定</ElButton>
        </span>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.user-list-container {
  padding: 24px;
  min-height: 100vh;
  background: linear-gradient(135deg, #0f0f1e 0%, #1a1a2e 40%, #16213e 100%);
  color: #e0e0e0;
}

.search-bar {
  background-color: rgba(25, 25, 45, 0.6);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.user-content {
  background-color: rgba(25, 25, 45, 0.6);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.form-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.pagination-wrapper {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}

.time-cell {
  font-family: monospace;
  color: #a0a5b0;
  letter-spacing: 0.5px;
}

/* Deep styling for Element Plus to match dark theme */
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
  font-weight: 600;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

:deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

:deep(.el-pagination) {
  --el-pagination-bg-color: rgba(30, 30, 50, 0.6);
  --el-pagination-text-color: #e0e0e0;
  --el-pagination-button-color: #e0e0e0;
  --el-pagination-button-disabled-bg-color: rgba(20, 20, 35, 0.6);
}
</style>
