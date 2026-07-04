<script setup>
import { ref, onMounted, computed } from 'vue'
import { getSystemLogs } from '../api/log.js'
import { ElMessage } from 'element-plus'

const logs = ref([])
const total = ref(0)
const loading = ref(false)
const page = ref(1)
const size = ref(20)

// ── 筛选条件 ──────────────────────────────────────────────────────────────
const filters = ref({
  operator: '',
  action: '',
  targetType: '',
  result: '',
  dateFrom: null,
  dateTo: null,
})

const actionOptions = [
  { label: '全部操作', value: '' },
  { label: 'LOGIN — 登录', value: 'LOGIN' },
  { label: 'DEVICE_CREATE — 新增设备', value: 'DEVICE_CREATE' },
  { label: 'DEVICE_UPDATE — 更新设备', value: 'DEVICE_UPDATE' },
  { label: 'DEVICE_DELETE — 删除设备', value: 'DEVICE_DELETE' },
  { label: 'CONTROL — 设备控制', value: 'CONTROL' },
  { label: 'THRESHOLD_SET — 阈值设置', value: 'THRESHOLD_SET' },
  { label: 'POLICY_CREATE — 新增策略', value: 'POLICY_CREATE' },
  { label: 'POLICY_TOGGLE — 策略启停', value: 'POLICY_TOGGLE' },
  { label: 'ALARM_CREATE — 新增告警', value: 'ALARM_CREATE' },
  { label: 'ALARM_HANDLE — 处理告警', value: 'ALARM_HANDLE' },
  { label: 'USER_CREATE — 新增用户', value: 'USER_CREATE' },
  { label: 'USER_UPDATE — 更新用户', value: 'USER_UPDATE' },
  { label: 'ROLE_CREATE — 新增角色', value: 'ROLE_CREATE' },
  { label: 'ROLE_PERMISSION — 分配权限', value: 'ROLE_PERMISSION' },
  { label: 'PERM_CREATE — 新增权限', value: 'PERM_CREATE' },
  { label: 'PERM_DELETE — 删除权限', value: 'PERM_DELETE' },
]

const targetOptions = [
  { label: '全部类型', value: '' },
  { label: 'SYSTEM — 系统', value: 'SYSTEM' },
  { label: 'DEVICE — 设备', value: 'DEVICE' },
  { label: 'POLICY — 策略', value: 'POLICY' },
  { label: 'THRESHOLD — 阈值', value: 'THRESHOLD' },
  { label: 'ALARM — 告警', value: 'ALARM' },
  { label: 'USER — 用户', value: 'USER' },
  { label: 'ROLE — 角色', value: 'ROLE' },
  { label: 'PERMISSION — 权限', value: 'PERMISSION' },
]

const resultOptions = [
  { label: '全部结果', value: '' },
  { label: 'SUCCESS — 成功', value: 'SUCCESS' },
  { label: 'FAIL — 失败', value: 'FAIL' },
]

const levelFilter = ref('全部')
const levels = ['全部', 'info', 'warn', 'error']
const levelLabels = { info: '信息', warn: '警告', error: '错误' }

const loadLogs = async () => {
  loading.value = true
  try {
    const f = filters.value
    const res = await getSystemLogs(page.value, size.value, {
      operator: f.operator || undefined,
      action: f.action || undefined,
      targetType: f.targetType || undefined,
      result: f.result || undefined,
      dateFrom: f.dateFrom ? new Date(f.dateFrom).toISOString() : undefined,
      dateTo: f.dateTo ? new Date(f.dateTo).toISOString() : undefined,
    })
    if (res.code === 200) {
      logs.value = res.data.list
      total.value = res.data.total
    } else {
      ElMessage.error(res.message || '获取系统日志失败')
    }
  } catch (e) {
    ElMessage.error('网络错误')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  loadLogs()
}

const handleReset = () => {
  filters.value = { operator: '', action: '', targetType: '', result: '', dateFrom: null, dateTo: null }
  page.value = 1
  loadLogs()
}

const handlePageChange = (p) => {
  page.value = p
  loadLogs()
}

const handleSizeChange = (s) => {
  size.value = s
  page.value = 1
  loadLogs()
}

const filtered = computed(() =>
  levelFilter.value === '全部'
    ? logs.value
    : logs.value.filter(l => l.level === levelFilter.value)
)

onMounted(() => {
  loadLogs()
})
</script>

<template>
  <div class="log-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">系统日志</h1>
        <p class="page-sub">记录所有系统操作、事件触发与告警记录</p>
      </div>
      <div class="level-tabs">
        <button
          v-for="l in levels"
          :key="l"
          class="level-tab"
          :class="{ active: levelFilter === l }"
          @click="levelFilter = l"
        >
          {{ l === '全部' ? '全部' : levelLabels[l] }}
        </button>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-row">
        <div class="filter-item">
          <label>操作人</label>
          <input
            v-model="filters.operator"
            placeholder="输入操作人"
            class="filter-input"
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="filter-item">
          <label>操作类型</label>
          <select v-model="filters.action" class="filter-select">
            <option v-for="opt in actionOptions" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </option>
          </select>
        </div>
        <div class="filter-item">
          <label>目标类型</label>
          <select v-model="filters.targetType" class="filter-select">
            <option v-for="opt in targetOptions" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </option>
          </select>
        </div>
        <div class="filter-item">
          <label>结果</label>
          <select v-model="filters.result" class="filter-select">
            <option v-for="opt in resultOptions" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </option>
          </select>
        </div>
        <div class="filter-item">
          <label>开始时间</label>
          <input
            v-model="filters.dateFrom"
            type="datetime-local"
            class="filter-input"
          />
        </div>
        <div class="filter-item">
          <label>结束时间</label>
          <input
            v-model="filters.dateTo"
            type="datetime-local"
            class="filter-input"
          />
        </div>
        <div class="filter-actions">
          <button class="btn-search" @click="handleSearch">查询</button>
          <button class="btn-reset" @click="handleReset">重置</button>
        </div>
      </div>
    </div>

    <!-- 日志列表 -->
    <div class="log-card">
      <div v-if="loading" class="loading-state">加载中...</div>
      <div v-else-if="filtered.length === 0" class="empty-state">
        <div class="empty-icon">📋</div>
        <p>暂无系统日志</p>
        <p class="empty-hint">当前筛选条件下没有操作记录，请调整筛选条件或等待更多操作产生日志</p>
      </div>
      <div v-else class="log-list">
        <div v-for="log in filtered" :key="log.id" class="log-row" :class="log.level">
          <span class="log-level-badge" :class="log.level">{{ levelLabels[log.level] || log.level }}</span>
          <span class="log-time">{{ log.time }}</span>
          <span class="log-user">{{ log.user }}</span>
          <span class="log-action">{{ log.action }}</span>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="total > size" class="pagination-bar">
      <div class="page-info">共 {{ total }} 条记录</div>
      <div class="page-controls">
        <button
          class="page-btn"
          :disabled="page <= 1"
          @click="handlePageChange(page - 1)"
        >
          上一页
        </button>
        <span class="page-num">{{ page }}</span>
        <button
          class="page-btn"
          :disabled="page >= Math.ceil(total / size)"
          @click="handlePageChange(page + 1)"
        >
          下一页
        </button>
        <select class="page-size" :value="size" @change="e => handleSizeChange(Number(e.target.value))">
          <option :value="10">10条/页</option>
          <option :value="20">20条/页</option>
          <option :value="50">50条/页</option>
          <option :value="100">100条/页</option>
        </select>
      </div>
    </div>
  </div>
</template>

<style scoped>
.log-page { padding: 24px 28px; }
.page-header { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 20px; }
.page-title { font-size: 22px; font-weight: 700; color: #e0f4ff; margin-bottom: 4px; }
.page-sub { font-size: 13px; color: rgba(140,190,220,0.6); }
.level-tabs { display: flex; gap: 4px; }
.level-tab { padding: 6px 14px; background: rgba(0,30,70,0.5); border: 1px solid rgba(0,80,140,0.2); border-radius: 6px; color: rgba(140,190,220,0.7); font-size: 12px; cursor: pointer; transition: all 0.2s; }
.level-tab.active { background: rgba(0,120,220,0.2); border-color: rgba(77,208,225,0.4); color: #4dd0e1; }

/* 筛选栏 */
.filter-bar { background: rgba(8,20,45,0.6); border: 1px solid rgba(0,120,200,0.12); border-radius: 8px; padding: 16px 18px; margin-bottom: 16px; }
.filter-row { display: flex; flex-wrap: wrap; gap: 12px; align-items: flex-end; }
.filter-item { display: flex; flex-direction: column; gap: 4px; }
.filter-item label { font-size: 11px; color: rgba(140,190,220,0.6); }
.filter-input, .filter-select {
  padding: 6px 10px; background: rgba(0,30,70,0.5); border: 1px solid rgba(0,80,140,0.25);
  border-radius: 5px; color: #c8e6f5; font-size: 12px; outline: none; min-width: 130px;
}
.filter-input:focus, .filter-select:focus { border-color: rgba(77,208,225,0.5); }
.filter-select option { background: #0a1a30; color: #c8e6f5; }
.filter-actions { display: flex; gap: 8px; align-items: flex-end; padding-bottom: 1px; }
.btn-search, .btn-reset {
  padding: 6px 16px; border-radius: 5px; font-size: 12px; cursor: pointer; border: none; transition: all 0.2s;
}
.btn-search { background: rgba(0,120,220,0.25); border: 1px solid rgba(77,208,225,0.3); color: #4dd0e1; }
.btn-search:hover { background: rgba(0,120,220,0.4); }
.btn-reset { background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1); color: rgba(140,190,220,0.6); }
.btn-reset:hover { background: rgba(255,255,255,0.1); }

/* 日志卡片 */
.log-card { background: rgba(8,20,45,0.8); border: 1px solid rgba(0,120,200,0.15); border-radius: 10px; overflow: hidden; min-height: 200px; }
.log-list { display: flex; flex-direction: column; }
.log-row { display: flex; align-items: flex-start; gap: 14px; padding: 12px 18px; border-bottom: 1px solid rgba(0,60,120,0.1); transition: background 0.15s; font-size: 13px; }
.log-row:last-child { border-bottom: none; }
.log-row:hover { background: rgba(0,60,120,0.08); }
.log-level-badge { padding: 1px 8px; border-radius: 4px; font-size: 11px; font-weight: 600; white-space: nowrap; flex-shrink: 0; }
.log-level-badge.info  { background: rgba(77,208,225,0.15); border: 1px solid rgba(77,208,225,0.25); color: #4dd0e1; }
.log-level-badge.warn  { background: rgba(255,167,38,0.15); border: 1px solid rgba(255,167,38,0.3); color: #ffa726; }
.log-level-badge.error { background: rgba(239,83,80,0.15); border: 1px solid rgba(239,83,80,0.3); color: #ef5350; }
.log-time { font-size: 12px; color: rgba(140,190,220,0.6); white-space: nowrap; flex-shrink: 0; font-family: monospace; }
.log-user { min-width: 70px; font-size: 12px; color: rgba(140,190,220,0.7); flex-shrink: 0; }
.log-action { color: rgba(180,220,240,0.85); flex: 1; line-height: 1.4; }
.log-row.error .log-action { color: rgba(239,130,130,0.9); }
.log-row.warn .log-action { color: rgba(255,200,120,0.9); }

/* 加载 & 空状态 */
.loading-state, .empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 20px; color: rgba(140,190,220,0.5); }
.empty-icon { font-size: 48px; margin-bottom: 12px; }
.empty-state p { margin: 4px 0; font-size: 14px; }
.empty-hint { font-size: 12px !important; color: rgba(140,190,220,0.35); }

/* 分页 */
.pagination-bar { display: flex; align-items: center; justify-content: space-between; margin-top: 16px; padding: 0 4px; }
.page-info { font-size: 12px; color: rgba(140,190,220,0.5); }
.page-controls { display: flex; align-items: center; gap: 8px; }
.page-btn {
  padding: 5px 12px; background: rgba(0,30,70,0.5); border: 1px solid rgba(0,80,140,0.25);
  border-radius: 5px; color: rgba(140,190,220,0.7); font-size: 12px; cursor: pointer; transition: all 0.2s;
}
.page-btn:hover:not(:disabled) { background: rgba(0,80,160,0.3); border-color: rgba(77,208,225,0.3); }
.page-btn:disabled { opacity: 0.35; cursor: not-allowed; }
.page-num { color: #4dd0e1; font-size: 13px; min-width: 24px; text-align: center; }
.page-size {
  padding: 5px 8px; background: rgba(0,30,70,0.5); border: 1px solid rgba(0,80,140,0.25);
  border-radius: 5px; color: rgba(140,190,220,0.7); font-size: 12px; cursor: pointer; outline: none;
}
.page-size option { background: #0a1a30; color: #c8e6f5; }
</style>
