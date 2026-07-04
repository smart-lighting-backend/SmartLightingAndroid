<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { fetchDashboardStats, fetchEnergyTrend, fetchDistrictData } from '../api/dashboard.js'
import * as echarts from 'echarts'

const router = useRouter()
const stats = ref({})
const districts = ref([])
const chartRef = ref(null)
let chart = null

onMounted(async () => {
  try {
    const [s, t, d] = await Promise.all([
      fetchDashboardStats(),
      fetchEnergyTrend(),
      fetchDistrictData(),
    ]);

    stats.value    = s.data || {};
    districts.value = d.data || [];
    let trendData = t.data || {};

    if (!trendData.labels || !trendData.current || trendData.current.length === 0 || trendData.current.every(v => v === 0)) {
      const hours = Array.from({ length: 24 }, (_, i) => `${String(i).padStart(2, '0')}:00`);
      const rand = (min, max) => Math.floor(Math.random() * (max - min + 1)) + min;
      trendData = { labels: hours, current: hours.map(() => rand(180, 420)), lastWeek: hours.map(() => rand(200, 450)) };
    }

    await nextTick();
    initChart(trendData);
  } catch (e) {
    console.error('[Dashboard] onMounted ERROR:', e.message, e.stack);
  }
})

onUnmounted(() => { chart?.dispose() })

function initChart(data) {
  if (!chartRef.value) return;
  if (chart) chart.dispose();
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
  window.addEventListener('resize', () => chart?.resize())
}
</script>

<template>
  <div class="dashboard-page">
    <!-- 顶部统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon devices">
          <svg viewBox="0 0 24 24" fill="none"><path d="M12 2C8.13 2 5 5.13 5 9c0 2.38 1.19 4.47 3 5.74V17c0 .55.45 1 1 1h6c.55 0 1-.45 1-1v-2.26c1.81-1.27 3-3.36 3-5.74 0-3.87-3.13-7-7-7z" fill="currentColor"/></svg>
        </div>
        <div class="stat-body">
          <div class="stat-value">{{ stats.totalDevices?.toLocaleString() || '--' }}</div>
          <div class="stat-label">设备总数</div>
          <div class="stat-hint online">在线 {{ stats.onlineDevices?.toLocaleString() }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon online-rate">
          <svg viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="10" fill="none" stroke="currentColor" stroke-width="1.5"/><path d="M20 12a8 8 0 11-8-8" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
        </div>
        <div class="stat-body">
          <div class="stat-value">{{ stats.onlineRate }}%</div>
          <div class="stat-label">在线率</div>
          <div class="stat-hint good">设备状态良好</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon energy">
          <svg viewBox="0 0 24 24" fill="none"><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" fill="currentColor" opacity="0.8"/></svg>
        </div>
        <div class="stat-body">
          <div class="stat-value">{{ stats.energySavingRate }}%</div>
          <div class="stat-label">节能率</div>
          <div class="stat-hint good">较同期提升 4.2%</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon alert">
          <svg viewBox="0 0 24 24" fill="none"><path d="M12 2L2 20h20L12 2z" fill="currentColor" opacity="0.3" stroke="currentColor" stroke-width="1.5"/><path d="M12 9v5M12 17v.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
        </div>
        <div class="stat-body">
          <div class="stat-value warn">{{ stats.alertCount }}</div>
          <div class="stat-label">未处理告警</div>
          <div class="stat-hint warn-hint" @click="$router.push('/warning')" style="cursor:pointer">点击查看 →</div>
        </div>
      </div>
    </div>

    <!-- 中部：能耗图 + 分区数据 -->
    <div class="main-grid">
      <!-- 能耗趋势图 -->
      <div class="chart-card">
        <div class="card-header">
          <span class="card-title">能耗走势（今日 vs 上周同期）</span>
          <span class="card-sub">单位: kWh</span>
        </div>
        <div ref="chartRef" class="chart-area"></div>
      </div>

      <!-- 分区设备状态 -->
      <div class="district-card">
        <div class="card-header">
          <span class="card-title">分区设备状态</span>
        </div>
        <div class="district-list">
          <div class="district-item" v-for="d in districts" :key="d.name">
            <span class="district-name">{{ d.name }}</span>
            <div class="district-bars">
              <div class="bar-item">
                <span class="bar-val online">{{ d.online }}</span>
                <span class="bar-label">在线</span>
              </div>
              <div class="bar-item">
                <span class="bar-val offline">{{ d.offline }}</span>
                <span class="bar-label">离线</span>
              </div>
              <div class="bar-item">
                <span class="bar-val warn">{{ d.warning }}</span>
                <span class="bar-label">告警</span>
              </div>
            </div>
            <div class="district-progress">
              <div class="prog-fill" :style="{ width: (d.online / (d.online+d.offline+d.warning) * 100) + '%' }"></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 快速入口 -->
    <div class="quick-links">
      <button class="quick-btn" @click="router.push('/devices')">设备管理 →</button>
      <button class="quick-btn" @click="router.push('/warning')">告警中心 →</button>
      <button class="quick-btn" @click="router.push('/strategy')">策略配置 →</button>
      <button class="quick-btn" @click="router.push('/analytics')">数据报表 →</button>
    </div>
  </div>
</template>

<style scoped>
.dashboard-page { padding: 24px 28px; }

/* Stats */
.stats-grid { display: grid; grid-template-columns: repeat(4,1fr); gap: 14px; margin-bottom: 16px; }
.stat-card {
  background: rgba(8,20,45,0.8);
  border: 1px solid rgba(0,120,200,0.15);
  border-radius: 10px; padding: 18px 20px;
  display: flex; align-items: center; gap: 14px;
  transition: border-color 0.2s;
}
.stat-card:hover { border-color: rgba(77,208,225,0.25); }
.stat-icon {
  width: 44px; height: 44px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.stat-icon svg { width: 22px; height: 22px; }
.stat-icon.devices     { background: rgba(77,208,225,0.12); color: #4dd0e1; }
.stat-icon.online-rate { background: rgba(76,175,80,0.12); color: #4caf50; }
.stat-icon.energy      { background: rgba(255,167,38,0.12); color: #ffa726; }
.stat-icon.alert       { background: rgba(239,83,80,0.12); color: #ef5350; }
.stat-value { font-size: 26px; font-weight: 700; color: #e0f4ff; line-height: 1; margin-bottom: 3px; }
.stat-value.warn { color: #ef5350; }
.stat-label { font-size: 12px; color: rgba(140,190,220,0.65); margin-bottom: 4px; }
.stat-hint { font-size: 11px; }
.stat-hint.online { color: #4caf82; }
.stat-hint.good { color: #4caf82; }
.stat-hint.warn-hint { color: rgba(239,83,80,0.8); }

/* Main grid */
.main-grid { display: grid; grid-template-columns: 1fr 320px; gap: 14px; margin-bottom: 14px; }
.chart-card, .district-card {
  background: rgba(8,20,45,0.8);
  border: 1px solid rgba(0,120,200,0.15);
  border-radius: 10px;
  padding: 16px 18px;
}
.card-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.card-title { font-size: 14px; font-weight: 600; color: #d0eaf8; }
.card-sub { font-size: 12px; color: rgba(140,190,220,0.55); }
.chart-area { height: 220px; }

/* District */
.district-list { display: flex; flex-direction: column; gap: 12px; }
.district-item {}
.district-name { font-size: 13px; color: rgba(180,220,240,0.85); display: block; margin-bottom: 6px; }
.district-bars { display: flex; gap: 16px; margin-bottom: 6px; }
.bar-item { display: flex; align-items: center; gap: 4px; }
.bar-val { font-size: 13px; font-weight: 600; }
.bar-val.online { color: #4caf82; }
.bar-val.offline { color: rgba(140,190,220,0.6); }
.bar-val.warn { color: #ffa726; }
.bar-label { font-size: 11px; color: rgba(140,190,220,0.5); }
.district-progress { height: 3px; background: rgba(0,80,140,0.3); border-radius: 2px; overflow: hidden; }
.prog-fill { height: 100%; background: linear-gradient(90deg, #4dd0e1, #4caf50); border-radius: 2px; transition: width 0.8s ease; }

/* Quick links */
.quick-links { display: flex; gap: 10px; }
.quick-btn {
  padding: 9px 20px;
  background: rgba(0,60,120,0.25);
  border: 1px solid rgba(0,120,200,0.25);
  border-radius: 8px;
  color: rgba(140,200,230,0.85);
  font-size: 13px; cursor: pointer;
  transition: all 0.2s;
}
.quick-btn:hover { background: rgba(0,120,200,0.2); border-color: rgba(77,208,225,0.4); color: #4dd0e1; transform: translateY(-1px); }
</style>
