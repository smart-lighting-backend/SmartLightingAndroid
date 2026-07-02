<script setup>
import { ref } from 'vue'

const MOCK_LOGS = [
  { id: 1, time: '2023-10-27 14:32:10', level: 'error',  user: '系统',     action: '设备 SL-ND-102 断线告警触发，自动发送通知' },
  { id: 2, time: '2023-10-27 14:31:05', level: 'info',   user: 'admin',   action: '手动下发指令：设备 SL-ND-084 亮度调整至 75%' },
  { id: 3, time: '2023-10-27 12:15:22', level: 'warn',   user: '系统',     action: '设备 SL-ND-102 心跳包连续3次丢失' },
  { id: 4, time: '2023-10-27 08:00:00', level: 'info',   user: '系统',     action: '策略「深夜节能模式」执行结束，已恢复标准亮度' },
  { id: 5, time: '2023-10-26 23:00:00', level: 'info',   user: '系统',     action: '策略「深夜节能模式」触发，设备组亮度调降至 30%' },
  { id: 6, time: '2023-10-26 18:40:11', level: 'warn',   user: '系统',     action: '摄像头视场遮挡告警：SL-ND-200，已自动标记' },
  { id: 7, time: '2023-10-26 15:22:33', level: 'info',   user: 'zhang_g', action: '处理告警 ALM-20231027-002，标记为"处理中"' },
  { id: 8, time: '2023-10-26 10:00:00', level: 'info',   user: 'admin',   action: '新建策略配置「雨雾增亮补偿」' },
  { id: 9, time: '2023-10-25 22:08:33', level: 'error',  user: '系统',     action: '设备 SL-ND-412 驱动板过热，触发降功率保护' },
  { id:10, time: '2023-10-25 09:00:00', level: 'info',   user: 'admin',   action: '系统管理员登录，IP: 192.168.1.100' },
]

const logs = ref(MOCK_LOGS)
const levelFilter = ref('全部')
const levels = ['全部', 'info', 'warn', 'error']
const levelLabels = { info: '信息', warn: '警告', error: '错误' }

const filtered = () => levelFilter.value === '全部' ? logs.value : logs.value.filter(l => l.level === levelFilter.value)
</script>

<template>
  <div class="log-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">系统日志</h1>
        <p class="page-sub">记录所有系统操作、事件触发与告警记录</p>
      </div>
      <div class="level-tabs">
        <button v-for="l in levels" :key="l" class="level-tab" :class="{ active: levelFilter === l }" @click="levelFilter = l">
          {{ l === '全部' ? '全部' : levelLabels[l] }}
        </button>
      </div>
    </div>

    <div class="log-card">
      <div class="log-list">
        <div v-for="log in filtered()" :key="log.id" class="log-row" :class="log.level">
          <span class="log-level-badge" :class="log.level">{{ levelLabels[log.level] || log.level }}</span>
          <span class="log-time">{{ log.time }}</span>
          <span class="log-user">{{ log.user }}</span>
          <span class="log-action">{{ log.action }}</span>
        </div>
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
.log-card { background: rgba(8,20,45,0.8); border: 1px solid rgba(0,120,200,0.15); border-radius: 10px; overflow: hidden; }
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
</style>
