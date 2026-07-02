<script setup>
import { ref, onMounted, onUnmounted, shallowRef } from 'vue'
import * as echarts from 'echarts'

// ── 筛选条件 ─────────────────────────────────────────
const region    = ref('全部区域')
const startDate = ref('')
const endDate   = ref('')

const regions = ['全部区域', 'A区', 'B区', 'C区', 'D区']
const showRegionDrop = ref(false)

function selectRegion(r) {
  region.value = r
  showRegionDrop.value = false
}

// ── 底部统计数据 ──────────────────────────────────────
const stats = ref([
  { label: '总能耗 (kWh)', value: '12,450', color: '#f59e0b', icon: 'energy', highlight: false },
  { label: '平均节能率',   value: '34.2%',  color: '#22c55e', icon: 'leaf',   highlight: true  },
  { label: '碳减排 (kg)',  value: '8,920',  color: '#f97316', icon: 'co2',    highlight: false },
])

// ── ECharts 图表 ──────────────────────────────────────
const luxChartRef    = ref(null)
const energyChartRef = ref(null)
let luxChart    = null
let energyChart = null

function initLuxChart() {
  if (!luxChartRef.value) return
  luxChart = echarts.init(luxChartRef.value, null, { renderer: 'canvas' })

  const hours = ['18:00','19:00','20:00','21:00','22:00','23:00','00:00','01:00','02:00','03:00','04:00','05:00','06:00']
  const luxData = [4800, 4200, 3500, 2600, 1800, 1200, 800, 600, 500, 480, 600, 1500, 3600]

  luxChart.setOption({
    backgroundColor: 'transparent',
    grid: { top: 30, right: 20, bottom: 40, left: 55 },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(0,20,50,0.9)',
      borderColor: 'rgba(0,150,220,0.4)',
      textStyle: { color: '#c8e6ff', fontSize: 12 },
      formatter: (p) => `${p[0].axisValue}<br/>照度: <b>${p[0].value} Lux</b>`,
    },
    xAxis: {
      type: 'category',
      data: hours,
      axisLine:  { lineStyle: { color: 'rgba(0,150,220,0.2)' } },
      axisTick:  { show: false },
      axisLabel: { color: 'rgba(120,180,220,0.7)', fontSize: 11 },
      splitLine: { show: false },
    },
    yAxis: {
      type: 'value',
      name: '照度 (Lux)',
      nameTextStyle: { color: 'rgba(120,180,220,0.7)', fontSize: 11 },
      min: 0,
      max: 5000,
      interval: 1000,
      axisLine:  { show: false },
      axisTick:  { show: false },
      axisLabel: { color: 'rgba(120,180,220,0.7)', fontSize: 11 },
      splitLine: { lineStyle: { color: 'rgba(0,100,180,0.12)', type: 'dashed' } },
    },
    series: [{
      type: 'line',
      data: luxData,
      smooth: true,
      symbol: 'none',
      lineStyle: { color: '#4dd0e1', width: 2 },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(77,208,225,0.25)' },
          { offset: 1, color: 'rgba(77,208,225,0.02)' },
        ]),
      },
    }],
  })
}

function initEnergyChart() {
  if (!energyChartRef.value) return
  energyChart = echarts.init(energyChartRef.value, null, { renderer: 'canvas' })

  const days = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  const kwhData    = [320, 310, 295, 380, 350, 420, 415]
  const effData    = [38, 42, 45, 41, 44, 48, 46]

  energyChart.setOption({
    backgroundColor: 'transparent',
    legend: {
      top: 0,
      right: 0,
      data: [
        { name: '日耗电量 (kWh)', icon: 'roundRect' },
        { name: '节能率 (%)',     icon: 'roundRect' },
      ],
      textStyle: { color: 'rgba(160,210,240,0.8)', fontSize: 11 },
      itemWidth: 12,
      itemHeight: 8,
    },
    grid: { top: 40, right: 55, bottom: 40, left: 55 },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(0,20,50,0.9)',
      borderColor: 'rgba(0,150,220,0.4)',
      textStyle: { color: '#c8e6ff', fontSize: 12 },
      axisPointer: { type: 'shadow' },
    },
    xAxis: {
      type: 'category',
      data: days,
      axisLine:  { lineStyle: { color: 'rgba(0,150,220,0.2)' } },
      axisTick:  { show: false },
      axisLabel: { color: 'rgba(120,180,220,0.7)', fontSize: 11 },
      splitLine: { show: false },
    },
    yAxis: [
      {
        type: 'value',
        name: '能耗 (kWh)',
        nameTextStyle: { color: 'rgba(120,180,220,0.7)', fontSize: 11 },
        min: 0, max: 500, interval: 100,
        axisLine:  { show: false },
        axisTick:  { show: false },
        axisLabel: { color: 'rgba(120,180,220,0.7)', fontSize: 11 },
        splitLine: { lineStyle: { color: 'rgba(0,100,180,0.12)', type: 'dashed' } },
      },
      {
        type: 'value',
        name: '节能率 (%)',
        nameTextStyle: { color: 'rgba(120,180,220,0.7)', fontSize: 11 },
        min: 0, max: 100, interval: 20,
        axisLine:  { show: false },
        axisTick:  { show: false },
        axisLabel: { color: 'rgba(120,180,220,0.7)', fontSize: 11, formatter: '{value} %' },
        splitLine: { show: false },
      },
    ],
    series: [
      {
        name: '日耗电量 (kWh)',
        type: 'bar',
        yAxisIndex: 0,
        data: kwhData,
        barWidth: '55%',
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(0,180,255,0.9)' },
            { offset: 1, color: 'rgba(0,80,180,0.3)' },
          ]),
          borderRadius: [3, 3, 0, 0],
        },
      },
      {
        name: '节能率 (%)',
        type: 'line',
        yAxisIndex: 1,
        data: effData,
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        lineStyle: { color: '#22c55e', width: 2 },
        itemStyle: { color: '#22c55e' },
      },
    ],
  })
}

function handleResize() {
  luxChart?.resize()
  energyChart?.resize()
}

onMounted(() => {
  initLuxChart()
  initEnergyChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  luxChart?.dispose()
  energyChart?.dispose()
})
</script>

<template>
  <div class="reports-page" @click="showRegionDrop = false">
    <!-- 页面标题 -->
    <div class="page-header">
      <div>
        <h1 class="page-title">数据报表分析</h1>
        <p class="page-sub">查看路灯运行状态及能耗趋势</p>
      </div>
      <div class="filter-bar" @click.stop>
        <!-- 区域选择 -->
        <div class="filter-select" @click="showRegionDrop = !showRegionDrop">
          <span>{{ region }}</span>
          <svg viewBox="0 0 24 24" fill="none" :style="{ transform: showRegionDrop ? 'rotate(180deg)' : '', transition: 'transform .2s' }">
            <path d="M7 10l5 5 5-5z" fill="currentColor"/>
          </svg>
          <div class="drop-list" v-if="showRegionDrop" @click.stop>
            <div v-for="r in regions" :key="r" class="drop-item" :class="{ active: r === region }" @click="selectRegion(r)">{{ r }}</div>
          </div>
        </div>

        <!-- 日期范围 -->
        <div class="date-range">
          <svg viewBox="0 0 24 24" fill="none"><rect x="3" y="4" width="18" height="18" rx="2" stroke="currentColor" stroke-width="1.5"/><path d="M16 2v4M8 2v4M3 10h18" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
          <input type="date" v-model="startDate" class="date-input" placeholder="mm/dd/yyyy" />
          <span>–</span>
          <input type="date" v-model="endDate" class="date-input" placeholder="mm/dd/yyyy" />
          <svg viewBox="0 0 24 24" fill="none"><rect x="3" y="4" width="18" height="18" rx="2" stroke="currentColor" stroke-width="1.5"/><path d="M16 2v4M8 2v4M3 10h18" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
        </div>

        <button class="query-btn">查询</button>
        <button class="export-btn">
          <svg viewBox="0 0 24 24" fill="none"><path d="M12 3v12M8 11l4 4 4-4M3 18v2a1 1 0 001 1h16a1 1 0 001-1v-2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
          导出
        </button>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-row">
      <!-- 历史光照强度趋势 -->
      <div class="chart-card">
        <div class="chart-card-header">
          <div class="chart-title-wrap">
            <span class="chart-accent" />
            <span class="chart-title">历史光照强度趋势</span>
          </div>
          <span class="chart-badge">Lux vs Time</span>
        </div>
        <div ref="luxChartRef" class="chart-area" />
      </div>

      <!-- 能耗与节能率趋势 -->
      <div class="chart-card">
        <div class="chart-card-header">
          <div class="chart-title-wrap">
            <span class="chart-accent" />
            <span class="chart-title">能耗与节能率趋势</span>
          </div>
          <span class="chart-badge">双轴分析</span>
        </div>
        <div ref="energyChartRef" class="chart-area" />
      </div>
    </div>

    <!-- 底部统计卡片 -->
    <div class="stats-row">
      <div
        v-for="s in stats"
        :key="s.label"
        class="stat-card"
      >
        <div class="stat-icon" :style="{ background: s.color + '22', border: '1px solid ' + s.color + '55' }">
          <svg v-if="s.icon==='energy'" viewBox="0 0 24 24" fill="none"><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" fill="currentColor"/></svg>
          <svg v-else-if="s.icon==='leaf'" viewBox="0 0 24 24" fill="none"><path d="M17 8C8 10 5.9 16.17 3.82 19.34 3.34 20.06 4 21 4.9 21c.57 0 1.1-.34 1.4-.83C7.9 17.5 11 13.5 17 12c1.15 3.93-1 8-6 9.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/><path d="M2 8s4-8 16-4c0 6-4 14-16 4z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
          <svg v-else viewBox="0 0 24 24" fill="none"><rect x="3" y="3" width="8" height="8" rx="1" fill="currentColor" opacity="0.8"/><rect x="13" y="3" width="8" height="4" rx="1" fill="currentColor" opacity="0.6"/><rect x="13" y="11" width="8" height="4" rx="1" fill="currentColor" opacity="0.4"/><rect x="3" y="15" width="8" height="6" rx="1" fill="currentColor" opacity="0.5"/></svg>
        </div>
        <div class="stat-info">
          <div class="stat-label">{{ s.label }}</div>
          <div class="stat-value" :style="{ color: s.highlight ? s.color : '#e0f4ff' }">{{ s.value }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.reports-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 20px 24px 16px;
  background: #060f1e;
  gap: 16px;
  overflow: hidden;
}

/* 页面标题 */
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  flex-shrink: 0;
  flex-wrap: wrap;
  gap: 12px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #e0f4ff;
  letter-spacing: 0.5px;
}

.page-sub {
  font-size: 12px;
  color: rgba(120, 180, 220, 0.65);
  margin-top: 4px;
}

/* 筛选栏 */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.filter-select {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 12px;
  height: 36px;
  background: rgba(0, 30, 60, 0.7);
  border: 1px solid rgba(0, 120, 180, 0.35);
  border-radius: 8px;
  color: #c8e6ff;
  font-size: 13px;
  cursor: pointer;
  position: relative;
  user-select: none;
  min-width: 110px;
}
.filter-select svg { width: 16px; height: 16px; color: rgba(77,208,225,0.6); }

.drop-list {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  background: rgba(4, 18, 40, 0.97);
  border: 1px solid rgba(0, 120, 180, 0.4);
  border-radius: 8px;
  z-index: 100;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(0,0,0,0.5);
}
.drop-item {
  padding: 9px 14px;
  font-size: 13px;
  color: rgba(160,210,240,0.8);
  cursor: pointer;
  transition: background 0.15s;
}
.drop-item:hover, .drop-item.active {
  background: rgba(0,150,220,0.2);
  color: #4dd0e1;
}

/* 日期范围 */
.date-range {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  height: 36px;
  background: rgba(0, 30, 60, 0.7);
  border: 1px solid rgba(0, 120, 180, 0.35);
  border-radius: 8px;
  color: rgba(77,208,225,0.6);
}
.date-range svg { width: 14px; height: 14px; flex-shrink: 0; }
.date-range span { color: rgba(120,170,210,0.6); font-size: 12px; }

.date-input {
  background: none;
  border: none;
  outline: none;
  color: rgba(160,210,240,0.8);
  font-size: 12px;
  width: 96px;
  cursor: pointer;
}
.date-input::-webkit-calendar-picker-indicator {
  display: none;
}

.query-btn {
  padding: 0 18px;
  height: 36px;
  background: linear-gradient(135deg, #0077cc, #00aaff);
  border: none;
  border-radius: 8px;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 12px rgba(0,150,230,0.3);
}
.query-btn:hover { transform: translateY(-1px); box-shadow: 0 4px 16px rgba(0,150,230,0.5); }

.export-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 0 14px;
  height: 36px;
  background: rgba(0, 30, 60, 0.7);
  border: 1px solid rgba(0, 120, 180, 0.35);
  border-radius: 8px;
  color: rgba(160,210,240,0.8);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.export-btn:hover { border-color: rgba(77,208,225,0.5); color: #4dd0e1; }
.export-btn svg { width: 14px; height: 14px; }

/* 图表行 */
.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  flex: 1;
  min-height: 0;
}

.chart-card {
  background: rgba(0, 20, 50, 0.6);
  border: 1px solid rgba(0, 150, 220, 0.15);
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.chart-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  flex-shrink: 0;
}

.chart-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chart-accent {
  width: 3px;
  height: 16px;
  background: linear-gradient(180deg, #4dd0e1, #0077cc);
  border-radius: 2px;
}

.chart-title {
  font-size: 14px;
  font-weight: 600;
  color: #e0f4ff;
}

.chart-badge {
  font-size: 11px;
  color: rgba(77,208,225,0.7);
  background: rgba(0,150,220,0.1);
  border: 1px solid rgba(0,150,220,0.2);
  padding: 2px 8px;
  border-radius: 4px;
}

.chart-area {
  flex: 1;
  min-height: 0;
}

/* 底部统计卡片 */
.stats-row {
  display: flex;
  gap: 16px;
  flex-shrink: 0;
}

.stat-card {
  flex: 1;
  background: rgba(0, 20, 50, 0.6);
  border: 1px solid rgba(0, 150, 220, 0.15);
  border-radius: 12px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #fff;
}
.stat-icon svg { width: 22px; height: 22px; }

.stat-label {
  font-size: 12px;
  color: rgba(150,200,230,0.7);
  margin-bottom: 4px;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 0.5px;
}
</style>
