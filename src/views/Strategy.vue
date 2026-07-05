<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { fetchStrategyList, fetchStrategyHistory, toggleStrategy, deleteStrategy } from '../api/strategy.js'
import { ElForm, ElFormItem, ElInput, ElSelect, ElOption, ElInputNumber, ElButton, ElPagination, ElIcon, ElMessage, ElDialog } from 'element-plus'
import { Search, Refresh, Timer } from '@element-plus/icons-vue'
import { useUserInfo } from '../composables/useUserInfo.js'

const { hasPerm } = useUserInfo()

const router = useRouter()
const strategies = ref([])
const loading = ref(false)
const historyVisible = ref(false)
const historyLoading = ref(false)
const historyData = ref(null)
const historyPolicyName = ref('')

async function showHistory(s) {
  historyPolicyName.value = s.name
  historyVisible.value = true
  historyLoading.value = true
  try {
    const res = await fetchStrategyHistory(s.id, 7)
    historyData.value = res?.data || null
  } catch { historyData.value = null }
  historyLoading.value = false
}

const searchForm = ref({
  name: '',
  policyType: '',
  enabled: null,
  priorityMin: null,
  priorityMax: null,
  effectiveTime: ''
})
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

async function loadData() {
  loading.value = true
  const query = {
    page: currentPage.value,
    size: pageSize.value,
    name: searchForm.value.name || undefined,
    policyType: searchForm.value.policyType || undefined,
    enabled: searchForm.value.enabled !== null ? searchForm.value.enabled : undefined,
    priorityMin: searchForm.value.priorityMin !== null ? searchForm.value.priorityMin : undefined,
    priorityMax: searchForm.value.priorityMax !== null ? searchForm.value.priorityMax : undefined,
    effectiveTime: searchForm.value.effectiveTime || undefined,
  }
  
  try {
    const res = await fetchStrategyList(query)
    if (res && res.data) {
      const list = Array.isArray(res.data) ? res.data : (res.data.records || res.data.list || [])
      strategies.value = list.map(item => {
        let group = '--', startTime = '--', endTime = '--'
        if (item.conditions && typeof item.conditions === 'string') {
          try {
            const cond = JSON.parse(item.conditions)
            if (cond.group) group = cond.group
            if (cond.startTime) startTime = cond.startTime
            if (cond.endTime) endTime = cond.endTime
          } catch (e) {}
        }
        return {
          ...item,
          group,
          startTime,
          endTime,
          lastTrigger: item.lastTriggerTime || item.lastTrigger || '--',
          triggerCount: item.triggerCount || 0
        }
      })
      total.value = Array.isArray(res.data) ? res.data.length : (res.data.total || 0)
    } else {
      strategies.value = Array.isArray(res) ? res : []
    }
  } catch (error) {
    ElMessage.error(error?.message || '加载策略列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  loadData()
}

function handleReset() {
  searchForm.value = {
    name: '', policyType: '', enabled: null, priorityMin: null, priorityMax: null, effectiveTime: ''
  }
  handleSearch()
}

onMounted(loadData)

async function toggle(s) {
  s.enabled = !s.enabled
  await toggleStrategy(s.id, s.enabled)
}
async function remove(s) {
  if (!confirm(`确认删除策略"${s.name}"？`)) return
  try {
    await deleteStrategy(s.id)
    ElMessage.success('删除成功')
    await loadData()
  } catch (e) {
    ElMessage.error(e?.message || '删除失败')
  }
}
</script>

<template>
  <div class="strategy-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">策略配置</h1>
        <p class="page-sub">管理路灯自动调节规则，基于环境感知与时间调度</p>
      </div>
      <button v-if="hasPerm('policy:create')" class="create-btn" @click="router.push('/strategy/create')">
        <svg viewBox="0 0 24 24" fill="none"><path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
        新建策略
      </button>
    </div>

    <!-- 搜索表单 -->
    <div class="search-bar">
      <ElForm :inline="true" :model="searchForm" class="search-form">
        <ElFormItem label="名称">
          <ElInput v-model="searchForm.name" placeholder="模糊查询" clearable style="width: 140px" />
        </ElFormItem>
        <ElFormItem label="类型">
          <ElSelect v-model="searchForm.policyType" placeholder="全部" clearable style="width: 120px">
            <ElOption label="时间(TIME)" value="TIME" />
            <ElOption label="传感(SENSOR)" value="SENSOR" />
            <ElOption label="场景(SCENE)" value="SCENE" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="状态">
          <ElSelect v-model="searchForm.enabled" placeholder="全部" clearable style="width: 100px">
            <ElOption label="已启用" :value="true" />
            <ElOption label="已停用" :value="false" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="优先级">
          <ElInputNumber v-model="searchForm.priorityMin" :min="1" :max="100" placeholder="最小" style="width: 80px" :controls="false" />
          <span style="margin: 0 8px; color: rgba(255,255,255,0.5)">-</span>
          <ElInputNumber v-model="searchForm.priorityMax" :min="1" :max="100" placeholder="最大" style="width: 80px" :controls="false" />
        </ElFormItem>
        <ElFormItem label="时段">
          <ElInput v-model="searchForm.effectiveTime" placeholder="如: 22:00" clearable style="width: 120px" />
        </ElFormItem>
        <ElFormItem>
          <ElButton type="primary" @click="handleSearch"><ElIcon><Search /></ElIcon>&nbsp;查询</ElButton>
          <ElButton @click="handleReset"><ElIcon><Refresh /></ElIcon>&nbsp;重置</ElButton>
        </ElFormItem>
      </ElForm>
    </div>

    <div class="strategy-list">
      <div v-if="loading" class="loading-state">加载中...</div>
      <div v-for="s in strategies" :key="s.id" class="strategy-card">
        <div class="sc-left">
          <div class="sc-name">{{ s.name }}</div>
          <div class="sc-meta">
            <span class="sc-tag">{{ s.group }}</span>
            <span class="sc-time">{{ s.startTime }} — {{ s.endTime }}</span>
          </div>
          <div class="sc-stats">
            触发次数：<strong>{{ s.triggerCount }}</strong> &nbsp;·&nbsp;
            最近触发：{{ s.lastTrigger }}
          </div>
        </div>
        <div class="sc-right">
          <div class="toggle-wrap">
            <span class="toggle-label" :class="s.enabled ? 'active' : 'inactive'">{{ s.enabled ? '启用' : '停用' }}</span>
            <div class="toggle-switch" :class="{ on: s.enabled }" @click="toggle(s)">
              <div class="toggle-thumb"></div>
            </div>
          </div>
          <button class="sc-btn hist" @click="showHistory(s)">历史</button>
          <button v-if="hasPerm('policy:update')" class="sc-btn edit" @click="router.push('/strategy/edit/' + s.id)">编辑</button>
          <button v-if="hasPerm('policy:delete')" class="sc-btn del" @click="remove(s)">删除</button>
        </div>
      </div>
    </div>
    
    <!-- 执行历史弹窗 -->
    <ElDialog v-model="historyVisible" :title="'执行历史 — ' + historyPolicyName" width="700px" top="5vh">
      <div v-if="historyLoading" class="loading-state">加载中...</div>
      <div v-else-if="historyData">
        <div class="history-summary">近7天共触发 <strong>{{ historyData.totalTriggers }}</strong> 次</div>
        <div class="history-list" v-if="historyData.records?.length">
          <div v-for="(r, i) in historyData.records.slice(0, 30)" :key="i" class="history-item">
            <span class="hi-time">{{ r.createTime }}</span>
            <span class="hi-device">{{ r.deviceId }}</span>
            <span class="hi-action">{{ r.actionTaken }}</span>
          </div>
        </div>
        <div v-else class="empty-hint">暂无执行记录 — 可能还没有遥测数据触发该策略，或策略条件尚未满足</div>
      </div>
    </ElDialog>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="total > 0">
      <ElPagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>
  </div>
</template>

<style scoped>
.strategy-page { padding: 24px 28px; }
.page-header { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 20px; }
.search-bar {
  background: rgba(8,20,45,0.6);
  border: 1px solid rgba(0,120,200,0.15);
  border-radius: 8px;
  padding: 16px 20px 0 20px;
  margin-bottom: 16px;
}
.search-form :deep(.el-form-item__label) { color: #8cbedc; }
.page-title { font-size: 22px; font-weight: 700; color: #e0f4ff; margin-bottom: 4px; }
.page-sub { font-size: 13px; color: rgba(140,190,220,0.6); }
.create-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 9px 18px;
  background: linear-gradient(135deg, #0077cc, #0099e6);
  border: none; border-radius: 8px;
  color: #fff; font-size: 13px; font-weight: 500;
  cursor: pointer; transition: all 0.2s;
  box-shadow: 0 2px 12px rgba(0,150,230,0.3);
}
.create-btn svg { width: 15px; height: 15px; }
.create-btn:hover { transform: translateY(-1px); box-shadow: 0 4px 18px rgba(0,150,230,0.5); }
.loading-state { text-align: center; padding: 40px; color: rgba(140,190,220,0.5); }
.strategy-list { display: flex; flex-direction: column; gap: 10px; }
.strategy-card {
  background: rgba(8,20,45,0.8);
  border: 1px solid rgba(0,120,200,0.15);
  border-radius: 10px; padding: 18px 22px;
  display: flex; align-items: center; justify-content: space-between; gap: 16px;
  transition: border-color 0.2s;
}
.strategy-card:hover { border-color: rgba(77,208,225,0.2); }
.sc-name { font-size: 15px; font-weight: 600; color: #d0eaf8; margin-bottom: 6px; }
.sc-meta { display: flex; gap: 12px; margin-bottom: 6px; }
.sc-tag { padding: 2px 8px; background: rgba(0,120,200,0.15); border: 1px solid rgba(0,120,200,0.25); border-radius: 10px; font-size: 11px; color: rgba(140,200,230,0.8); }
.sc-time { font-size: 12px; color: rgba(140,190,220,0.6); }
.sc-stats { font-size: 12px; color: rgba(140,190,220,0.55); }
.sc-stats strong { color: #4dd0e1; }
.sc-right { display: flex; align-items: center; gap: 12px; flex-shrink: 0; }
.toggle-wrap { display: flex; align-items: center; gap: 6px; }
.toggle-label { font-size: 12px; }
.toggle-label.active { color: #4caf82; }
.toggle-label.inactive { color: rgba(140,190,220,0.5); }
.toggle-switch {
  width: 38px; height: 20px;
  background: rgba(0,60,120,0.4);
  border-radius: 10px; cursor: pointer;
  position: relative; transition: background 0.3s;
}
.toggle-switch.on { background: rgba(0,180,120,0.35); }
.toggle-thumb {
  position: absolute;
  top: 3px; left: 3px;
  width: 14px; height: 14px; border-radius: 50%;
  background: rgba(140,190,220,0.6);
  transition: all 0.25s;
}
.toggle-switch.on .toggle-thumb { left: 21px; background: #4caf50; box-shadow: 0 0 6px #4caf50; }
.sc-btn {
  padding: 5px 12px;
  border-radius: 5px; font-size: 12px; cursor: pointer; transition: all 0.2s;
}
.sc-btn.hist { background: rgba(0,120,80,0.15); border: 1px solid rgba(0,180,120,0.25); color: rgba(140,220,180,0.8); }
.sc-btn.hist:hover { background: rgba(0,180,120,0.2); color: #4caf50; }
.sc-btn.edit { background: rgba(0,80,140,0.2); border: 1px solid rgba(0,120,200,0.3); color: rgba(140,200,230,0.9); }
.sc-btn.edit:hover { background: rgba(0,120,200,0.2); color: #4dd0e1; }
.sc-btn.del { background: rgba(180,30,30,0.1); border: 1px solid rgba(200,60,60,0.25); color: rgba(220,100,100,0.8); }
.sc-btn.del { background: rgba(180,30,30,0.1); border: 1px solid rgba(200,60,60,0.25); color: rgba(220,100,100,0.8); }
.sc-btn.del:hover { background: rgba(180,30,30,0.2); color: #ff7070; }
.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.history-summary { font-size: 14px; color: rgba(200,220,240,0.9); margin-bottom: 12px; }
.history-summary strong { color: #4dd0e1; font-size: 18px; }
.history-list { display: flex; flex-direction: column; gap: 4px; max-height: 400px; overflow-y: auto; }
.history-item {
  display: flex; gap: 16px; padding: 6px 10px;
  background: rgba(0,20,50,0.4); border-radius: 4px;
  font-size: 12px;
}
.hi-time { color: rgba(140,190,220,0.6); min-width: 140px; }
.hi-device { color: #4dd0e1; min-width: 80px; }
.hi-action { color: rgba(200,220,240,0.8); }
.empty-hint { text-align: center; padding: 24px; color: rgba(140,190,220,0.4); }
</style>
