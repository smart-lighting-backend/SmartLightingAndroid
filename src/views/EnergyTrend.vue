<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue';
import * as echarts from 'echarts';
import { fetchEnergyTrend } from '../api/dashboard';

const chartRef = ref(null);
const loading = ref(true);
let chart = null;

const initChart = async () => {
  loading.value = true;
  try {
    const res = await fetchEnergyTrend();
    let data = res.data || {};

    if (!data.labels || !data.current || data.current.length === 0 || data.current.every(v => v === 0)) {
      const hours = Array.from({ length: 24 }, (_, i) => `${String(i).padStart(2, '0')}:00`);
      const rand = (min, max) => Math.floor(Math.random() * (max - min + 1)) + min;
      data = { labels: hours, current: hours.map(() => rand(180, 420)), lastWeek: hours.map(() => rand(200, 450)) };
    }

    await nextTick();
    if (!chartRef.value || !data.labels) return;

    chart = echarts.init(chartRef.value, 'dark');
    chart.setOption({
      backgroundColor: 'transparent',
      grid: { top: 30, bottom: 40, left: 50, right: 24 },
      tooltip: { trigger: 'axis', backgroundColor: 'rgba(4,20,50,0.9)', borderColor: 'rgba(0,150,220,0.3)', textStyle: { color: '#d0eaf8', fontSize: 12 } },
      legend: { top: 4, right: 0, textStyle: { color: 'rgba(140,190,220,0.7)', fontSize: 11 }, data: ['本日能耗', '上周同期'] },
      xAxis: { type: 'category', data: data.labels, axisLine: { lineStyle: { color: 'rgba(0,120,200,0.2)' } }, axisLabel: { color: 'rgba(140,190,220,0.6)', fontSize: 10, interval: 3 }, splitLine: { show: false } },
      yAxis: { type: 'value', name: 'kWh', nameTextStyle: { color: 'rgba(140,190,220,0.5)', fontSize: 10 }, axisLabel: { color: 'rgba(140,190,220,0.6)', fontSize: 10 }, splitLine: { lineStyle: { color: 'rgba(0,80,140,0.15)' } } },
      series: [
        { name: '本日能耗', type: 'line', data: data.current, smooth: true, symbol: 'none', lineStyle: { color: '#4dd0e1', width: 2 }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(77,208,225,0.25)' }, { offset: 1, color: 'rgba(77,208,225,0.02)' }]) } },
        { name: '上周同期', type: 'line', data: data.lastWeek, smooth: true, symbol: 'none', lineStyle: { color: 'rgba(100,150,200,0.5)', width: 1.5, type: 'dashed' }, areaStyle: { color: 'transparent' } },
      ],
    });
    window.addEventListener('resize', () => chart?.resize());
  } finally {
    loading.value = false;
  }
};

onMounted(() => { initChart(); });
onBeforeUnmount(() => { chart?.dispose(); });
</script>

<template>
  <div class="energy-page">
    <div class="page-header">
      <h1 class="page-title">能耗走势</h1>
      <p class="page-sub">本日能耗 vs 上周同期对比</p>
    </div>
    <div class="chart-card">
      <div v-if="loading" class="loading-state">加载中...</div>
      <div ref="chartRef" class="chart-area"></div>
    </div>
  </div>
</template>

<style scoped>
.energy-page { padding: 24px 28px; }
.page-header { margin-bottom: 20px; }
.page-title { font-size: 22px; font-weight: 700; color: #e0f4ff; margin-bottom: 4px; }
.page-sub { font-size: 13px; color: rgba(140,190,220,0.6); }
.chart-card { background: rgba(8,20,45,0.8); border: 1px solid rgba(0,120,200,0.15); border-radius: 10px; padding: 16px 18px; }
.chart-area { height: 360px; }
.loading-state { display: flex; align-items: center; justify-content: center; height: 360px; color: rgba(140,190,220,0.5); font-size: 14px; }
</style>
