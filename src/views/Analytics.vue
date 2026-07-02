<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'

const energyRef = ref(null)
const pieRef = ref(null)
let chart1 = null, chart2 = null

const MONTHS = ['1月','2月','3月','4月','5月','6月','7月','8月','9月','10月','11月','12月']
const rand = (a,b) => Math.floor(Math.random()*(b-a+1))+a

onMounted(() => {
  chart1 = echarts.init(energyRef.value, 'dark')
  chart1.setOption({
    backgroundColor: 'transparent',
    grid: { top: 30, bottom: 40, left: 55, right: 20 },
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(4,20,50,0.9)', borderColor: 'rgba(0,150,220,0.3)', textStyle: { color: '#d0eaf8', fontSize: 12 } },
    legend: { top: 2, right: 0, textStyle: { color: 'rgba(140,190,220,0.7)', fontSize: 11 }, data: ['实际能耗(kWh)','节省量(kWh)'] },
    xAxis: { type: 'category', data: MONTHS, axisLine: { lineStyle: { color: 'rgba(0,120,200,0.2)' } }, axisLabel: { color: 'rgba(140,190,220,0.6)', fontSize: 11 }, splitLine: { show: false } },
    yAxis: { type: 'value', axisLabel: { color: 'rgba(140,190,220,0.6)', fontSize: 10 }, splitLine: { lineStyle: { color: 'rgba(0,80,140,0.15)' } } },
    series: [
      { name: '实际能耗(kWh)', type: 'bar', data: MONTHS.map(()=>rand(8000,14000)), itemStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'rgba(77,208,225,0.9)'},{offset:1,color:'rgba(0,100,180,0.5)'}]), borderRadius:[3,3,0,0] } },
      { name: '节省量(kWh)', type: 'bar', data: MONTHS.map(()=>rand(2000,5000)), itemStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'rgba(76,175,80,0.9)'},{offset:1,color:'rgba(0,120,60,0.5)'}]), borderRadius:[3,3,0,0] } },
    ],
  })

  chart2 = echarts.init(pieRef.value, 'dark')
  chart2.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'item', backgroundColor: 'rgba(4,20,50,0.9)', borderColor: 'rgba(0,150,220,0.3)', textStyle: { color: '#d0eaf8' } },
    legend: { bottom: 0, textStyle: { color: 'rgba(140,190,220,0.7)', fontSize: 11 } },
    series: [{
      type: 'pie', radius: ['40%','65%'], center: ['50%','44%'],
      label: { color: 'rgba(140,190,220,0.8)', fontSize: 11 },
      data: [
        { name:'高新区', value:32, itemStyle:{color:'#4dd0e1'} },
        { name:'市中心', value:25, itemStyle:{color:'#42a5f5'} },
        { name:'工业园', value:20, itemStyle:{color:'#66bb6a'} },
        { name:'学院路', value:13, itemStyle:{color:'#ffa726'} },
        { name:'其他',   value:10, itemStyle:{color:'#ab47bc'} },
      ],
    }],
  })
  const resize = () => { chart1?.resize(); chart2?.resize() }
  window.addEventListener('resize', resize)
})
onUnmounted(() => { chart1?.dispose(); chart2?.dispose() })
</script>

<template>
  <div class="analytics-page">
    <div class="page-header">
      <h1 class="page-title">数据报表</h1>
      <p class="page-sub">全域路灯能耗统计与节能效果分析</p>
    </div>

    <!-- 汇总指标 -->
    <div class="metrics-row">
      <div class="metric-card" v-for="m in [
        { label:'年度总能耗', val:'51,438 kWh', sub:'同比降低 12.3%', color:'#4dd0e1' },
        { label:'年度节省',   val:'16,820 kWh', sub:'折合 ¥18,502', color:'#4caf50' },
        { label:'碳减排量',   val:'8.4 吨',     sub:'CO₂等效', color:'#66bb6a' },
        { label:'平均在线率', val:'95.0%',       sub:'较去年 +2.1%', color:'#ffa726' },
      ]" :key="m.label">
        <div class="metric-val" :style="{ color: m.color }">{{ m.val }}</div>
        <div class="metric-label">{{ m.label }}</div>
        <div class="metric-sub">{{ m.sub }}</div>
      </div>
    </div>

    <!-- 图表区 -->
    <div class="charts-grid">
      <div class="chart-card wide">
        <div class="card-header">
          <span class="card-title">月度能耗 vs 节省量（kWh）</span>
        </div>
        <div ref="energyRef" class="chart-area-lg"></div>
      </div>
      <div class="chart-card">
        <div class="card-header">
          <span class="card-title">分区能耗占比</span>
        </div>
        <div ref="pieRef" class="chart-area-lg"></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.analytics-page { padding: 24px 28px; }
.page-header { margin-bottom: 20px; }
.page-title { font-size: 22px; font-weight: 700; color: #e0f4ff; margin-bottom: 4px; }
.page-sub { font-size: 13px; color: rgba(140,190,220,0.6); }
.metrics-row { display: grid; grid-template-columns: repeat(4,1fr); gap: 14px; margin-bottom: 16px; }
.metric-card {
  background: rgba(8,20,45,0.8);
  border: 1px solid rgba(0,120,200,0.15);
  border-radius: 10px; padding: 16px 18px;
}
.metric-val { font-size: 22px; font-weight: 700; margin-bottom: 4px; }
.metric-label { font-size: 13px; color: rgba(160,210,235,0.8); margin-bottom: 3px; }
.metric-sub { font-size: 11px; color: rgba(140,190,220,0.55); }
.charts-grid { display: grid; grid-template-columns: 2fr 1fr; gap: 14px; }
.chart-card { background: rgba(8,20,45,0.8); border: 1px solid rgba(0,120,200,0.15); border-radius: 10px; padding: 16px 18px; }
.card-header { margin-bottom: 12px; }
.card-title { font-size: 14px; font-weight: 600; color: #d0eaf8; }
.chart-area-lg { height: 260px; }
</style>
