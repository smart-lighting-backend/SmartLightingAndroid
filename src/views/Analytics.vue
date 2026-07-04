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
    grid: { top: 30, bottom: 40, left: 55, right: 20 },
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(4,20,50,0.9)', borderColor: 'rgba(0,150,220,0.3)', textStyle: { color: '#d0eaf8', fontSize: 12 } },
    legend: { top: 2, right: 0, textStyle: { color: 'rgba(140,190,220,0.7)', fontSize: 11 }, data: ['实际能耗(kWh)', '节省量(kWh)'] },
    xAxis: { type: 'category', data: monthlyData.value.months, axisLine: { lineStyle: { color: 'rgba(0,120,200,0.2)' } }, axisLabel: { color: 'rgba(140,190,220,0.6)', fontSize: 11 }, splitLine: { show: false } },
    yAxis: { type: 'value', axisLabel: { color: 'rgba(140,190,220,0.6)', fontSize: 10 }, splitLine: { lineStyle: { color: 'rgba(0,80,140,0.15)' } } },
    series: [
      { name: '实际能耗(kWh)', type: 'bar', data: monthlyData.value.consumption, itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(77,208,225,0.9)' }, { offset: 1, color: 'rgba(0,100,180,0.5)' }]), borderRadius: [3, 3, 0, 0] } },
      { name: '节省量(kWh)', type: 'bar', data: monthlyData.value.savings, itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(76,175,80,0.9)' }, { offset: 1, color: 'rgba(0,120,60,0.5)' }]), borderRadius: [3, 3, 0, 0] } },
    ],
  }
}

function buildPieOption() {
  return {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'item', backgroundColor: 'rgba(4,20,50,0.9)', borderColor: 'rgba(0,150,220,0.3)', textStyle: { color: '#d0eaf8' } },
    legend: { bottom: 0, textStyle: { color: 'rgba(140,190,220,0.7)', fontSize: 11 } },
    series: [{
      type: 'pie', radius: ['40%', '65%'], center: ['50%', '44%'],
      label: { color: 'rgba(140,190,220,0.8)', fontSize: 11 },
      data: districtData.value.map((d, i) => ({ ...d, itemStyle: { color: COLORS[i % COLORS.length] } })),
    }],
  }
}

function initBarChart() {
  if (!energyRef.value) return
  if (!chart1) chart1 = echarts.init(energyRef.value, 'dark')
  chart1.setOption(scaleOption(buildBarOption()), true)
  chart1.resize()
}

function initPieChart() {
  if (!pieRef.value) return
  if (!chart2) chart2 = echarts.init(pieRef.value, 'dark')
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
.analytics-page { padding: 24px 28px; }
.page-header { margin-bottom: 20px; }
.page-title { font-size: 22px; font-weight: 700; color: #e0f4ff; margin-bottom: 4px; }
.page-sub { font-size: 13px; color: rgba(140,190,220,0.6); }
.metrics-row { display: grid; grid-template-columns: repeat(4,1fr); gap: calc(14px * var(--scale-ratio, 1)); margin-bottom: 16px; }
.metric-card {
  background: rgba(8,20,45,0.8);
  border: 1px solid rgba(0,120,200,0.15);
  border-radius: 10px; padding: 16px 18px;
}
.metric-val { font-size: 22px; font-weight: 700; margin-bottom: 4px; }
.metric-label { font-size: 13px; color: rgba(160,210,235,0.8); margin-bottom: 3px; }
.metric-sub { font-size: 11px; color: rgba(140,190,220,0.55); min-height: 16px; }
.charts-grid { display: grid; grid-template-columns: 2fr 1fr; gap: 14px; }
.chart-card { background: rgba(8,20,45,0.8); border: 1px solid rgba(0,120,200,0.15); border-radius: 10px; padding: 16px 18px; }
.card-header { margin-bottom: 12px; }
.card-title { font-size: 14px; font-weight: 600; color: #d0eaf8; }
.chart-area-lg { height: calc(260px * var(--scale-ratio, 1)); }
.loading-state, .error-state, .empty-state { display: flex; align-items: center; justify-content: center; height: 260px; color: rgba(140,190,220,0.5); font-size: 14px; }
.error-state { color: rgba(255,100,100,0.7); }
</style>
