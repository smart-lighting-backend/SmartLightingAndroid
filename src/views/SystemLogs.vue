<script setup>
import { ref, onMounted, onUnmounted, shallowRef } from 'vue'
import * as echarts from 'echarts'

// ── 标签切换 ──────────────────────────────────────────
const activeTab = ref('all')
const tabs = [
  { key: 'all',    label: '全部日志' },
  { key: 'error',  label: '仅异常' },
  { key: 'manual', label: '人工干预' },
]

// ── 日志条目 ──────────────────────────────────────────
const logs = ref([
  {
    id: 1,
    time: '14:32:45.092',
    trace: 'TRACE-99A1',
    type: 'normal',
    expanded: true,
    inputs: [
      { label: '光照:', value: '350 lx（突降）', highlight: true },
      { label: '车流:', value: '中等（12辆/min）' },
      { label: '天气:', value: '雷阵雨预警' },
    ],
    strategy: { name: '[极端天气应急模式]', tag: '#POL-WEATHER-01', confidence: '99.8%', type: 'emergency' },
    result: { status: 'success', text: '执行指令下发：A区 1-50号灯组亮度上调至 100%', time: '耗时: 42ms' },
  },
  {
    id: 2,
    time: '14:15:00.005',
    trace: 'TRACE-99A0',
    type: 'normal',
    expanded: false,
    inputs: [
      { label: '光照:', value: '1200 lx' },
      { label: '时段:', value: '下午' },
    ],
    strategy: { name: '[日同休省模式]', tag: '#POL-DAY-00', confidence: null, type: 'normal' },
    result: { status: 'hold', text: '维持现状，无动作', time: '耗时: 12ms' },
  },
  {
    id: 3,
    time: '12:30:12.441',
    trace: 'TRACE-998F',
    type: 'error',
    expanded: false,
    inputs: [
      { label: '节点:', value: 'N-B22 失联', highlight: true },
      { label: '心跳:', value: 'Timeout > 30s', highlight: true },
    ],
    strategy: { name: '[离网自控模式]', tag: '#POL-FALLBACK', confidence: null, type: 'fallback' },
    result: { status: 'warn', text: '边缘网关接管，启动本地定时叫策略', time: '耗时: 105ms' },
  },
])

const filteredLogs = ref([])

function applyFilter() {
  if (activeTab.value === 'all')    filteredLogs.value = logs.value
  else if (activeTab.value === 'error')  filteredLogs.value = logs.value.filter(l => l.type === 'error')
  else if (activeTab.value === 'manual') filteredLogs.value = logs.value.filter(l => l.type === 'manual')
}

function setTab(key) {
  activeTab.value = key
  applyFilter()
}

function toggleLog(id) {
  const log = logs.value.find(l => l.id === id)
  if (log) log.expanded = !log.expanded
  applyFilter()
}

// ── AI 决策延迟仪表盘 ──────────────────────────────────
const gaugeRef = shallowRef(null)
let gaugeChart = null

function initGauge() {
  if (!gaugeRef.value) return
  gaugeChart = echarts.init(gaugeRef.value, null, { renderer: 'canvas' })
  gaugeChart.setOption({
    backgroundColor: 'transparent',
    series: [{
      type: 'gauge',
      radius: '88%',
      startAngle: 200,
      endAngle: -20,
      min: 0,
      max: 200,
      progress: { show: true, width: 10, itemStyle: { color: '#22c55e' } },
      axisLine: { lineStyle: { width: 10, color: [[1, 'rgba(0,100,60,0.2)']] } },
      axisTick: { show: false },
      splitLine: { show: false },
      axisLabel: { show: false },
      pointer: { show: false },
      anchor: { show: false },
      title: { show: false },
      detail: {
        valueAnimation: true,
        formatter: '{value}',
        color: '#22c55e',
        fontSize: 36,
        fontWeight: 700,
        offsetCenter: [0, '5%'],
      },
      data: [{ value: 42, name: 'ms' }],
    }],
  })
}

// ── CPU/NPU 进度 ──────────────────────────────────────
const cpuUsage  = ref(32)
const npuUsage  = ref(68)
const memUsed   = ref(4.2)
const memTotal  = ref(8)
const coreTemp  = ref(56)

function handleResize() { gaugeChart?.resize() }

onMounted(() => {
  applyFilter()
  initGauge()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  gaugeChart?.dispose()
})
</script>

<template>
  <div class="logs-page">
    <!-- 主日志区域 -->
    <div class="logs-main">
      <!-- 页面标题 -->
      <div class="logs-header">
        <div class="title-wrap">
          <div class="title-icon">
            <svg viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="9" stroke="#4dd0e1" stroke-width="1.5"/><path d="M12 7v5l3 3" stroke="#4dd0e1" stroke-width="1.5" stroke-linecap="round"/><circle cx="12" cy="4" r="1" fill="#4dd0e1"/></svg>
          </div>
          <div>
            <h1 class="page-title">AI 自动控制日志</h1>
            <p class="page-sub">实时追踪边缘计算节点决策流与设备执行状态</p>
          </div>
        </div>
        <div class="status-bar">
          <span class="status-dot" />
          <span class="status-text">系统运行正常</span>
          <span class="sync-text">同步时间：刚刚</span>
        </div>
      </div>

      <!-- 标签栏 -->
      <div class="tabs-bar">
        <div class="tabs-left">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            class="tab-btn"
            :class="{ active: activeTab === tab.key }"
            @click="setTab(tab.key)"
          >{{ tab.label }}</button>
        </div>
        <button class="filter-btn">
          <svg viewBox="0 0 24 24" fill="none"><path d="M3 6h18M7 12h10M11 18h2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
          筛选
        </button>
      </div>

      <!-- 日志列表 -->
      <div class="logs-list">
        <div
          v-for="log in filteredLogs"
          :key="log.id"
          class="log-card"
          :class="log.type"
        >
          <!-- 顶部 -->
          <div class="log-top" @click="toggleLog(log.id)">
            <div class="log-dot" :class="log.type" />
            <span class="log-time">{{ log.time }}</span>
            <span class="log-trace">{{ log.trace }}</span>
          </div>

          <!-- 展开内容 -->
          <div v-if="log.expanded" class="log-body">
            <div class="log-row">
              <!-- 输入条件 -->
              <div class="log-section">
                <div class="section-header">
                  <svg viewBox="0 0 24 24" fill="none"><path d="M9 3H5a2 2 0 00-2 2v4M9 3h6M9 3v18M15 3h4a2 2 0 012 2v4M15 3v18M3 9v10a2 2 0 002 2h4M21 9v10a2 2 0 01-2 2h-4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
                  <span>{{ log.type === 'error' ? '异常输入' : '输入条件' }}</span>
                </div>
                <div class="input-list">
                  <div v-for="inp in log.inputs" :key="inp.label" class="input-item">
                    <span class="input-label">{{ inp.label }}</span>
                    <span class="input-value" :class="{ highlight: inp.highlight }">{{ inp.value }}</span>
                  </div>
                </div>
              </div>

              <div class="arrow-col">
                <svg viewBox="0 0 24 24" fill="none"><path d="M5 12h14M13 6l6 6-6 6" stroke="rgba(0,150,220,0.5)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
              </div>

              <!-- 命中策略 -->
              <div class="log-section">
                <div class="section-header">
                  <svg viewBox="0 0 24 24" fill="none"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/></svg>
                  <span>命中策略</span>
                </div>
                <div class="strategy-box" :class="log.strategy.type">
                  <div class="strategy-name">{{ log.strategy.name }}</div>
                  <div class="strategy-tag">{{ log.strategy.tag }}</div>
                  <div v-if="log.strategy.confidence" class="strategy-conf">置信度: {{ log.strategy.confidence }}</div>
                </div>
              </div>
            </div>

            <!-- 执行结果 -->
            <div class="log-result" :class="log.result.status">
              <svg v-if="log.result.status==='success'" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="9" stroke="#22c55e" stroke-width="1.5"/><path d="M8 12l3 3 5-5" stroke="#22c55e" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
              <svg v-else-if="log.result.status==='warn'" viewBox="0 0 24 24" fill="none"><path d="M12 2L2 20h20L12 2z" stroke="#f59e0b" stroke-width="1.5" stroke-linejoin="round"/><line x1="12" y1="9" x2="12" y2="13" stroke="#f59e0b" stroke-width="1.5" stroke-linecap="round"/><circle cx="12" cy="17" r="0.8" fill="#f59e0b"/></svg>
              <svg v-else viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="9" stroke="rgba(120,180,220,0.5)" stroke-width="1.5"/><path d="M12 8v4M12 16h.01" stroke="rgba(120,180,220,0.5)" stroke-width="1.5" stroke-linecap="round"/></svg>
              <span class="result-text">{{ log.result.text }}</span>
              <span class="result-time">{{ log.result.time }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧面板 -->
    <div class="logs-panel">
      <!-- AI 决策延迟 -->
      <div class="panel-card gauge-card">
        <div class="panel-card-title">
          <svg viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="9" stroke="#4dd0e1" stroke-width="1.5"/><path d="M12 7v5l3 3" stroke="#4dd0e1" stroke-width="1.5" stroke-linecap="round"/></svg>
          AI 决策延迟监控
        </div>
        <div ref="gaugeRef" class="gauge-area" />
        <div class="gauge-unit">ms</div>
        <div class="sla-badge">SLA 阈值 &lt; 100ms</div>
      </div>

      <!-- 边缘计算节点状态 -->
      <div class="panel-card node-card">
        <div class="panel-card-title">
          <svg viewBox="0 0 24 24" fill="none"><rect x="2" y="3" width="20" height="14" rx="2" stroke="#4dd0e1" stroke-width="1.5"/><path d="M8 21h8M12 17v4" stroke="#4dd0e1" stroke-width="1.5" stroke-linecap="round"/></svg>
          边缘计算节点状态
        </div>

        <!-- CPU -->
        <div class="metric-row">
          <div class="metric-label-row">
            <span class="metric-label">CPU 占用率</span>
            <span class="metric-val">{{ cpuUsage }}%</span>
          </div>
          <div class="progress-bar">
            <div class="progress-fill cpu" :style="{ width: cpuUsage + '%' }" />
          </div>
        </div>

        <!-- NPU -->
        <div class="metric-row">
          <div class="metric-label-row">
            <span class="metric-label">NPU 算力负载（推理引擎）</span>
            <span class="metric-val npu">{{ npuUsage }}%</span>
          </div>
          <div class="progress-bar">
            <div class="progress-fill npu" :style="{ width: npuUsage + '%' }" />
          </div>
        </div>

        <!-- 内存 + 温度 -->
        <div class="detail-grid">
          <div class="detail-item">
            <span class="detail-label">内存使用</span>
            <span class="detail-val">{{ memUsed }} / {{ memTotal }} GB</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">核心温度</span>
            <span class="detail-val">{{ coreTemp }}°C</span>
          </div>
        </div>

        <!-- 节点健康 -->
        <div class="node-health">
          <svg viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="9" stroke="#22c55e" stroke-width="1.5"/><path d="M8 12l3 3 5-5" stroke="#22c55e" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
          <div class="health-text">
            <div class="health-title">节点运行健康</div>
            <div class="health-sub">模型版本: V2.4.1 (YOLO-Lite)</div>
            <div class="health-sub">已连续运行: 45天 12小时</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.logs-page {
  display: flex;
  height: 100%;
  background: #060f1e;
  overflow: hidden;
  gap: 0;
}

/* ── 主日志区域 ── */
.logs-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 20px 20px 16px 24px;
  gap: 14px;
}

/* 标题 */
.logs-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  flex-shrink: 0;
}

.title-wrap {
  display: flex;
  align-items: center;
  gap: 14px;
}

.title-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: rgba(0, 80, 150, 0.2);
  border: 1px solid rgba(77, 208, 225, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.title-icon svg { width: 22px; height: 22px; }

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #e0f4ff;
  letter-spacing: 0.5px;
}
.page-sub {
  font-size: 11px;
  color: rgba(120, 180, 220, 0.65);
  margin-top: 3px;
}

.status-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 6px #22c55e;
  flex-shrink: 0;
}

.status-text { color: #22c55e; font-weight: 500; }
.sync-text   { color: rgba(120, 180, 220, 0.5); }

/* 标签栏 */
.tabs-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
  border-bottom: 1px solid rgba(0, 150, 220, 0.12);
  padding-bottom: 0;
}

.tabs-left {
  display: flex;
  gap: 0;
}

.tab-btn {
  padding: 8px 16px;
  font-size: 13px;
  color: rgba(150, 200, 230, 0.6);
  background: none;
  border: none;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
  margin-bottom: -1px;
}
.tab-btn.active {
  color: #4dd0e1;
  border-bottom-color: #4dd0e1;
}
.tab-btn:hover:not(.active) {
  color: rgba(200, 230, 250, 0.8);
}

.filter-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 5px 12px;
  background: none;
  border: 1px solid rgba(0, 120, 180, 0.3);
  border-radius: 6px;
  color: rgba(160, 210, 240, 0.7);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 4px;
}
.filter-btn:hover { border-color: rgba(77, 208, 225, 0.4); color: #4dd0e1; }
.filter-btn svg { width: 14px; height: 14px; }

/* 日志列表 */
.logs-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-right: 4px;
}
.logs-list::-webkit-scrollbar { width: 4px; }
.logs-list::-webkit-scrollbar-thumb { background: rgba(0,150,220,0.2); border-radius: 2px; }

.log-card {
  background: rgba(5, 18, 40, 0.7);
  border: 1px solid rgba(0, 150, 220, 0.15);
  border-radius: 10px;
  overflow: hidden;
  transition: border-color 0.2s;
}
.log-card.error {
  border-color: rgba(239, 68, 68, 0.2);
  background: rgba(20, 5, 5, 0.5);
}

.log-top {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  cursor: pointer;
  user-select: none;
}
.log-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.log-dot.normal { background: rgba(0, 180, 255, 0.8); }
.log-dot.error  { background: rgba(239, 68, 68, 0.9); box-shadow: 0 0 6px rgba(239,68,68,0.5); }

.log-time {
  font-size: 14px;
  font-weight: 700;
  color: #4dd0e1;
  font-family: 'Courier New', monospace;
}

.log-trace {
  margin-left: auto;
  font-size: 11px;
  color: rgba(100, 160, 200, 0.5);
  font-family: 'Courier New', monospace;
}

/* 日志详情 */
.log-body {
  padding: 0 16px 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  border-top: 1px solid rgba(0, 100, 180, 0.1);
}

.log-row {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding-top: 10px;
}

.log-section { flex: 1; }

.section-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: rgba(120, 180, 220, 0.7);
  margin-bottom: 8px;
}
.section-header svg { width: 13px; height: 13px; opacity: 0.7; }

.input-list { display: flex; flex-direction: column; gap: 5px; }
.input-item {
  display: flex;
  gap: 6px;
  font-size: 12px;
}
.input-label { color: rgba(120, 180, 220, 0.65); width: 42px; flex-shrink: 0; }
.input-value { color: rgba(200, 230, 255, 0.85); }
.input-value.highlight { color: #ff8080; font-weight: 600; }

.arrow-col {
  display: flex;
  align-items: center;
  padding-top: 20px;
  flex-shrink: 0;
}
.arrow-col svg { width: 20px; height: 20px; }

/* 策略盒子 */
.strategy-box {
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 12px;
}
.strategy-box.emergency {
  background: rgba(0, 100, 180, 0.2);
  border: 1px solid rgba(0, 150, 220, 0.35);
}
.strategy-box.normal {
  background: rgba(0, 60, 120, 0.15);
  border: 1px solid rgba(0, 120, 180, 0.2);
}
.strategy-box.fallback {
  background: rgba(120, 50, 0, 0.2);
  border: 1px solid rgba(200, 100, 0, 0.3);
}

.strategy-name {
  font-weight: 700;
  color: #4dd0e1;
  margin-bottom: 4px;
}
.strategy-tag  { color: rgba(120, 180, 220, 0.6); font-size: 11px; }
.strategy-conf { color: rgba(120, 200, 120, 0.8); font-size: 11px; margin-top: 4px; }

/* 执行结果 */
.log-result {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 12px;
}
.log-result.success {
  background: rgba(0, 80, 30, 0.2);
  border: 1px solid rgba(34, 197, 94, 0.2);
  color: rgba(180, 240, 180, 0.85);
}
.log-result.hold {
  background: rgba(0, 40, 80, 0.15);
  border: 1px solid rgba(0, 100, 160, 0.2);
  color: rgba(160, 200, 230, 0.7);
}
.log-result.warn {
  background: rgba(80, 40, 0, 0.2);
  border: 1px solid rgba(245, 158, 11, 0.25);
  color: rgba(240, 190, 100, 0.85);
}
.log-result svg { width: 16px; height: 16px; flex-shrink: 0; }
.result-text { flex: 1; }
.result-time {
  font-size: 10px;
  color: rgba(120, 180, 220, 0.5);
  white-space: nowrap;
}

/* ── 右侧面板 ── */
.logs-panel {
  width: 260px;
  flex-shrink: 0;
  background: rgba(5, 14, 28, 0.9);
  border-left: 1px solid rgba(0, 150, 220, 0.12);
  display: flex;
  flex-direction: column;
  padding: 16px 14px;
  gap: 14px;
  overflow-y: auto;
}

.panel-card {
  background: rgba(0, 25, 55, 0.65);
  border: 1px solid rgba(0, 150, 220, 0.18);
  border-radius: 12px;
  padding: 14px;
}

.panel-card-title {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 12px;
  font-weight: 600;
  color: rgba(160, 210, 240, 0.85);
  margin-bottom: 10px;
}
.panel-card-title svg { width: 16px; height: 16px; }

/* 仪表盘 */
.gauge-card { position: relative; }

.gauge-area {
  width: 100%;
  height: 160px;
}

.gauge-unit {
  text-align: center;
  font-size: 14px;
  color: rgba(100, 180, 220, 0.6);
  margin-top: -20px;
  margin-bottom: 8px;
}

.sla-badge {
  text-align: center;
  font-size: 11px;
  color: rgba(100, 180, 220, 0.55);
  background: rgba(0, 60, 120, 0.2);
  border: 1px solid rgba(0, 120, 180, 0.2);
  border-radius: 20px;
  padding: 4px 12px;
  display: inline-block;
  width: 100%;
}

/* CPU/NPU 进度 */
.metric-row { margin-bottom: 12px; }
.metric-label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.metric-label { font-size: 11px; color: rgba(150, 200, 230, 0.7); }
.metric-val   { font-size: 12px; color: rgba(200, 230, 255, 0.85); font-weight: 600; }
.metric-val.npu { color: #22c55e; }

.progress-bar {
  height: 5px;
  background: rgba(0, 60, 120, 0.3);
  border-radius: 3px;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.5s ease;
}
.progress-fill.cpu { background: linear-gradient(90deg, #0088cc, #4dd0e1); }
.progress-fill.npu { background: linear-gradient(90deg, #16a34a, #22c55e); }

/* 详情网格 */
.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 12px;
}
.detail-item {
  background: rgba(0, 40, 90, 0.3);
  border: 1px solid rgba(0, 100, 180, 0.15);
  border-radius: 8px;
  padding: 8px 10px;
}
.detail-label { font-size: 10px; color: rgba(120, 180, 220, 0.6); margin-bottom: 4px; }
.detail-val   { font-size: 13px; font-weight: 600; color: rgba(200, 230, 255, 0.85); }

/* 节点健康 */
.node-health {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px;
  background: rgba(0, 60, 30, 0.2);
  border: 1px solid rgba(34, 197, 94, 0.2);
  border-radius: 8px;
}
.node-health svg { width: 18px; height: 18px; flex-shrink: 0; margin-top: 2px; }
.health-title { font-size: 12px; font-weight: 600; color: #22c55e; margin-bottom: 4px; }
.health-sub { font-size: 10px; color: rgba(120, 200, 150, 0.7); line-height: 1.5; }
</style>
