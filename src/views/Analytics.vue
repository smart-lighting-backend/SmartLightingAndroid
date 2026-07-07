<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { fetchYearlyStats, fetchMonthlyEnergy, fetchDistrictEnergy } from '../api/dashboard'
import { useChartScale } from '../composables/useChartScale.js'

const year = new Date().getFullYear()
const { scaleOption, onScaleChange } = useChartScale()
const loading = ref(true)
const error = ref('')
const yearlyStats = ref({})
const monthlyData = ref({ months: [], consumption: [], savings: [] })
const districtData = ref([])

const energyRef = ref(null)
const pieRef = ref(null)
let chart1 = null, chart2 = null
let stopScaleWatch = null

const COLORS = ['#4dd0e1', '#42a5f5', '#66bb6a', '#ffa726', '#ab47bc', '#ef5350', '#26c6da', '#7e57c2']

function calcChange(current, last) {
  if (current == null || last == null || last === 0 || isNaN(current) || isNaN(last)) return ''
  const pct = ((current - last) / last * 100)
  const sign = pct > 0 ? '+' : ''
  return `较去年 ${sign}${pct.toFixed(1)}%`
}

const cards = computed(() => {
  const s = yearlyStats.value
  const totalKwh = s.totalKwh != null ? Number(s.totalKwh) : 0
  const savedKwh = s.savedKwh != null ? Number(s.savedKwh) : 0
  const carbonKg = s.carbonReductionKg != null ? Number(s.carbonReductionKg) : 0
  const onlineRate = s.avgOnlineRate || '0.0'

  return [
    { label: '年度总能耗', val: totalKwh.toLocaleString() + ' kWh', sub: calcChange(totalKwh, s.lastYear?.totalKwh), color: '#4dd0e1' },
    { label: '年度节省', val: savedKwh.toLocaleString() + ' kWh', sub: '', color: '#4caf50' },
    { label: '碳减排量', val: (carbonKg / 1000).toFixed(1) + ' 吨', sub: 'CO₂等效', color: '#66bb6a' },
    { label: '平均在线率', val: onlineRate + '%', sub: '', color: '#ffa726' },
  ]
})

function buildBarOption() {
  return {
    backgroundColor: 'transparent',
    grid: { top: 46, bottom: 56, left: 68, right: 32 },
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(255,255,255,0.96)', borderColor: 'rgba(0,141,230,0.24)', textStyle: { color: '#1d3148', fontSize: 14, fontWeight: 700 } },
    legend: {
      top: 4,
      right: 0,
      itemWidth: 30,
      itemHeight: 14,
      itemGap: 18,
      textStyle: { color: '#31516f', fontSize: 14, fontWeight: 700 },
      data: ['实际能耗(kWh)', '节省量(kWh)'],
    },
    xAxis: {
      type: 'category',
      data: monthlyData.value.months,
      axisLine: { lineStyle: { color: 'rgba(0,141,230,0.24)' } },
      axisLabel: { color: '#31516f', fontSize: 14, fontWeight: 700, margin: 16 },
      splitLine: { show: false },
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#31516f', fontSize: 13, fontWeight: 700, margin: 12 },
      splitLine: { lineStyle: { color: 'rgba(16,126,196,0.12)' } },
    },
    series: [
      { name: '实际能耗(kWh)', type: 'bar', data: monthlyData.value.consumption, itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(77,208,225,0.9)' }, { offset: 1, color: 'rgba(0,100,180,0.5)' }]), borderRadius: [3, 3, 0, 0] } },
      { name: '节省量(kWh)', type: 'bar', data: monthlyData.value.savings, itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(76,175,80,0.9)' }, { offset: 1, color: 'rgba(0,120,60,0.5)' }]), borderRadius: [3, 3, 0, 0] } },
    ],
  }
}

function buildPieOption() {
  return {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'item', backgroundColor: 'rgba(255,255,255,0.96)', borderColor: 'rgba(0,141,230,0.24)', textStyle: { color: '#1d3148', fontSize: 14, fontWeight: 700 } },
    legend: {
      bottom: 2,
      left: 'center',
      itemWidth: 28,
      itemHeight: 14,
      itemGap: 18,
      textStyle: { color: '#31516f', fontSize: 14, fontWeight: 700 },
    },
    series: [{
      type: 'pie', radius: ['34%', '58%'], center: ['50%', '43%'],
      label: { color: '#31516f', fontSize: 14, fontWeight: 700 },
      labelLine: { length: 24, length2: 34, lineStyle: { width: 2 } },
      data: districtData.value.map((d, i) => ({ ...d, itemStyle: { color: COLORS[i % COLORS.length] } })),
    }],
  }
}

function initBarChart() {
  if (!energyRef.value) return
  if (!chart1) chart1 = echarts.init(energyRef.value)
  chart1.setOption(scaleOption(buildBarOption()), true)
  chart1.resize()
}

function initPieChart() {
  if (!pieRef.value) return
  if (!chart2) chart2 = echarts.init(pieRef.value)
  chart2.setOption(scaleOption(buildPieOption()), true)
  chart2.resize()
}

function handleChartResize() {
  chart1?.resize()
  chart2?.resize()
}

onMounted(async () => {
  try {
    const [statsRes, monthlyRes, districtRes] = await Promise.all([
      fetchYearlyStats(year),
      fetchMonthlyEnergy(year),
      fetchDistrictEnergy(year),
    ])
    yearlyStats.value = statsRes.data || {}
    monthlyData.value = monthlyRes.data || { months: [], consumption: [], savings: [] }
    districtData.value = districtRes.data || []
  } catch (e) {
    error.value = '数据加载失败，请检查后端服务'
    console.error(e)
  } finally {
    loading.value = false
    await nextTick()
    initBarChart()
    initPieChart()
  }
  stopScaleWatch = onScaleChange(() => {
    initBarChart()
    initPieChart()
  })
  window.addEventListener('resize', handleChartResize)
})

onUnmounted(() => {
  stopScaleWatch?.()
  window.removeEventListener('resize', handleChartResize)
  chart1?.dispose()
  chart2?.dispose()
})
</script>

<template>
  <div class="analytics-page">
    <div class="page-header">
      <h1 class="page-title">数据报表</h1>
      <p class="page-sub">全域路灯能耗统计与节能效果分析</p>
    </div>

    <div v-if="loading" class="loading-state">加载中...</div>
    <div v-else-if="error" class="error-state">{{ error }}</div>
    <template v-else>
      <div class="metrics-row">
        <div class="metric-card" v-for="m in cards" :key="m.label">
          <div class="metric-val" :style="{ color: m.color }">{{ m.val }}</div>
          <div class="metric-label">{{ m.label }}</div>
          <div class="metric-sub">{{ m.sub }}</div>
        </div>
      </div>

      <div class="charts-grid">
        <div class="chart-card wide">
          <div class="card-header">
            <span class="card-title">月度能耗 vs 节省量（kWh）</span>
          </div>
          <div v-if="monthlyData.consumption.every(v => Number(v) === 0)" class="empty-state">暂无能耗数据</div>
          <div ref="energyRef" class="chart-area-lg"></div>
        </div>
        <div class="chart-card">
          <div class="card-header">
            <span class="card-title">分区能耗占比</span>
          </div>
          <div v-if="districtData.length === 0" class="empty-state">暂无分区数据</div>
          <div ref="pieRef" class="chart-area-lg"></div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.analytics-page { padding: 24px 28px; color: #1d3148; }
.page-header { margin-bottom: 20px; }
.page-title { font-size: 22px; font-weight: 800; color: #0d1b2d; margin-bottom: 4px; }
.page-sub { font-size: 13px; color: #40566f; font-weight: 600; }
.metrics-row { display: grid; grid-template-columns: repeat(4,1fr); gap: calc(14px * var(--scale-ratio, 1)); margin-bottom: 16px; }
.metric-card {
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(0, 141, 230, 0.15);
  border-radius: 8px; padding: 16px 18px;
  box-shadow: 0 14px 32px rgba(14, 70, 120, 0.08);
}
.metric-val { font-size: 22px; font-weight: 700; margin-bottom: 4px; }
.metric-label { font-size: 13px; color: #40566f; margin-bottom: 3px; font-weight: 700; }
.metric-sub { font-size: 11px; color: #60748a; min-height: 16px; font-weight: 600; }
.charts-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
}
.chart-card {
  width: 100%;
  min-width: 0;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(0, 141, 230, 0.16);
  border-radius: 8px;
  padding: 18px 20px;
  box-shadow: 0 18px 42px rgba(14, 70, 120, 0.10);
}
.chart-card.wide { grid-column: 1; }
.card-header { margin-bottom: 12px; }
.card-title { font-size: 18px; font-weight: 800; color: #0d1b2d; }
.chart-area-lg { height: calc(340px * var(--scale-ratio, 1)); }
.loading-state, .error-state, .empty-state { display: flex; align-items: center; justify-content: center; height: 260px; color: #40566f; font-size: 14px; font-weight: 700; }
.error-state { color: #c92a2a; }

@media (max-width: 1100px) {
  .metrics-row { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 680px) {
  .metrics-row { grid-template-columns: 1fr; }
  .chart-card { padding: 14px; }
  .chart-area-lg { height: calc(300px * var(--scale-ratio, 1)); }
}
</style>
