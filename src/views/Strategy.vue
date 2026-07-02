<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { fetchStrategyList, toggleStrategy, deleteStrategy } from '../api/strategy.js'

const router = useRouter()
const strategies = ref([])
const loading = ref(false)

async function loadData() {
  loading.value = true
  const res = await fetchStrategyList()
  strategies.value = res.data?.list || []
  loading.value = false
}

onMounted(loadData)

async function toggle(s) {
  s.status = s.status === 'active' ? 'inactive' : 'active'
  await toggleStrategy(s.id, s.status)
}
async function remove(s) {
  if (!confirm(`确认删除策略"${s.name}"？`)) return
  await deleteStrategy(s.id)
  strategies.value = strategies.value.filter(x => x.id !== s.id)
}
</script>

<template>
  <div class="strategy-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">策略配置</h1>
        <p class="page-sub">管理路灯自动调节规则，基于环境感知与时间调度</p>
      </div>
      <button class="create-btn" @click="router.push('/strategy/create')">
        <svg viewBox="0 0 24 24" fill="none"><path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
        新建策略
      </button>
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
            <span class="toggle-label" :class="s.status">{{ s.status === 'active' ? '启用' : '停用' }}</span>
            <div class="toggle-switch" :class="{ on: s.status === 'active' }" @click="toggle(s)">
              <div class="toggle-thumb"></div>
            </div>
          </div>
          <button class="sc-btn edit" @click="router.push('/strategy/create')">编辑</button>
          <button class="sc-btn del" @click="remove(s)">删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.strategy-page { padding: 24px 28px; }
.page-header { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 20px; }
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
.sc-btn.edit { background: rgba(0,80,140,0.2); border: 1px solid rgba(0,120,200,0.3); color: rgba(140,200,230,0.9); }
.sc-btn.edit:hover { background: rgba(0,120,200,0.2); color: #4dd0e1; }
.sc-btn.del { background: rgba(180,30,30,0.1); border: 1px solid rgba(200,60,60,0.25); color: rgba(220,100,100,0.8); }
.sc-btn.del:hover { background: rgba(180,30,30,0.2); color: #ff7070; }
</style>
