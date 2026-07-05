<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { fetchDashboardStats, fetchEnergyTrend, fetchDistrictData, fetchEdgeStatus, triggerEdgeSimulation, fetchEdgeRecent, triggerEnergyCalc, genTestData } from '../api/dashboard.js'
import { useChartScale } from '../composables/useChartScale.js'
import * as echarts from 'echarts'

const router = useRouter()
const { scaleOption, onScaleChange } = useChartScale()
const stats = ref({})
const edgeStatus = ref({})
const edgeRecent = ref([])
const edgeLoading = ref(false)
const districts = ref([])
const chartRef = ref(null)
let chart = null
let trendData = null
let stopScaleWatch = null
const calcLoading = ref(false)
const genLoading = ref(false)

async function handleCalcEnergy() {
  if (calcLoading.value) return
  calcLoading.value = true
  try {
    await triggerEnergyCalc()
    alert('当日能耗计算完成')
    window.location.reload()
  } catch (e) {
    alert('计算失败: ' + (e.response?.data?.msg || e.message))
  } finally {
    calcLoading.value = false
  }
}

async function handleGenData() {
  if (genLoading.value) return
  genLoading.value = true
  try {
    await genTestData(10)
    alert('历史测试数据生成完成（过去10天）')
    window.location.reload()
  } catch (e) {
    alert('生成失败: ' + (e.response?.data?.msg || e.message))
  } finally {
    genLoading.value = false
  }
}  // ← handleGenData 结束

// ── 边缘AI决策 ──
async function refreshEdgeStatus() {
  try {
    const [s, r] = await Promise.all([fetchEdgeStatus(), fetchEdgeRecent()])
    edgeStatus.value = s.data || {}
    edgeRecent.value = r.data || []
  } catch {}
}
async function handleTriggerEdge() {
  edgeLoading.value = true
  try {
    const res = await triggerEdgeSimulation()
    edgeStatus.value = res.data || {}
    await refreshEdgeStatus()
  } catch (e) {
    alert('边缘模拟失败: ' + (e.response?.data?.msg || e.message))
  } finally {
    edgeLoading.value = false
  }
}

onMounted(async () => {
  try {
    const [s, t, d, e] = await Promise.all([
      fetchDashboardStats(),
      fetchEnergyTrend(),
      fetchDistrictData(),
      fetchEdgeStatus(),
    ]);

    stats.value    = s.data || {};
    districts.value = d.data || [];
    edgeStatus.value = e.data || {};
    refreshEdgeStatus();
    let trendData = t.data || {};

    if (!trendData.labels || !trendData.current || trendData.current.length === 0 || trendData.current.every(v => v === 0)) {
      const hours = Array.from({ length: 24 }, (_, i) => `${String(i).padStart(2, '0')}:00`);
      const rand = (min, max) => Math.floor(Math.random() * (max - min + 1)) + min;
      trendData = { labels: hours, current: hours.map(() => rand(180, 420)), lastWeek: hours.map(() => rand(200, 450)) };
    }

    await nextTick();
    initChart(trendData);
    stopScaleWatch = onScaleChange(() => initChart(trendData));
    window.addEventListener('resize', handleChartResize)
  } catch (e) {
    console.error('[Dashboard] onMounted ERROR:', e.message, e.stack);
  }
})

onUnmounted(() => {
  stopScaleWatch?.()
  window.removeEventListener('resize', handleChartResize)
  chart?.dispose()
})

function handleChartResize() {
  chart?.resize()
}

function buildChartOption(data) {
  return {
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
  }
}

function initChart(data) {
  if (!chartRef.value) return;
  trendData = data
  if (!chart) chart = echarts.init(chartRef.value, 'dark');
  chart.setOption(scaleOption(buildChartOption(data)), true);
  handleChartResize()
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
      <div class="stat-card edge">
        <div class="stat-icon edge-ai">
          <svg viewBox="0 0 24 24" fill="none"><rect x="2" y="2" width="20" height="20" rx="3" stroke="currentColor" stroke-width="1.5"/><circle cx="9" cy="12" r="1.5" fill="currentColor"/><circle cx="15" cy="12" r="1.5" fill="currentColor"/><path d="M9 17h6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
        </div>
        <div class="stat-body">
          <div class="stat-value">{{ edgeStatus.totalDecisions || 0 }}</div>
          <div class="stat-label">
            边缘AI决策
            <button class="edge-trigger-btn" :disabled="edgeLoading" @click="handleTriggerEdge" :title="'手动触发一次边缘决策模拟'">
              {{ edgeLoading ? '...' : '▶' }}
            </button>
          </div>
          <div class="stat-hint" :class="edgeStatus.hitCount > 0 ? 'good' : ''">
            {{ edgeStatus.hitCount > 0 ? '命中 ' + edgeStatus.hitCount + ' 次' : '模拟运行中' }}
          </div>
        </div>
      </div>
    </div>

    <!-- 边缘决策最近记录 -->
    <div class="edge-recent" v-if="edgeRecent.length">
      <div class="card-title" style="margin-bottom:8px">边缘决策最近记录</div>
      <div class="edge-log-list">
        <div v-for="(r, i) in edgeRecent.slice(0, 5)" :key="i" class="edge-log-item">
          <span class="el-time">{{ r.createTime ? r.createTime.replace('T',' ').slice(5,16) : '--' }}</span>
          <span class="el-device">{{ r.deviceId }}</span>
          <span :class="r.matchedPolicy ? 'el-match' : 'el-nomatch'">{{ r.matchedPolicy || '未命中' }}</span>
          <span class="el-action">{{ r.actionTaken || '—' }}</span>
        </div>
      </div>
    </div>

    <!-- 中部：能耗图 + 分区数据 -->
    <div class="main-grid">
      <!-- 能耗趋势图 -->
      <div class="chart-card">
        <div class="card-header">
          <span class="card-title">能耗走势（今日 vs 上周同期）</span>
          <div class="card-header-right">
            <button class="mini-btn" :disabled="calcLoading" @click="handleCalcEnergy">
              {{ calcLoading ? '计算中...' : '手动计算' }}
            </button>
            <button class="mini-btn" :disabled="genLoading" @click="handleGenData">
              {{ genLoading ? '生成中...' : '生成测试数据' }}
            </button>
            <span class="card-sub">单位: kWh</span>
          </div>
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


  </div>
</template>

<style scoped>
.dashboard-page { padding: 24px 28px; }

/* Stats */
.stats-grid { display: grid; grid-template-columns: repeat(5,1fr); gap: 14px; margin-bottom: 16px; }
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
.stat-value { font-size: calc(26px * var(--scale-ratio, 1)); font-weight: 700; color: #e0f4ff; line-height: 1; margin-bottom: 3px; }
.stat-value.warn { color: #ef5350; }
.stat-label { font-size: 12px; color: rgba(140,190,220,0.65); margin-bottom: 4px; }
.stat-hint { font-size: 11px; }
.stat-hint.online { color: #4caf82; }
.stat-hint.good { color: #4caf82; }
.stat-hint.warn-hint { color: rgba(239,83,80,0.8); }

/* Main grid */
.main-grid { display: grid; grid-template-columns: 1.5fr 1fr; gap: calc(14px * var(--scale-ratio, 1)); margin-bottom: 14px; }
.chart-card, .district-card {
  background: rgba(8,20,45,0.8);
  border: 1px solid rgba(0,120,200,0.15);
  border-radius: 10px;
  padding: 16px 18px;
}
.card-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.card-title { font-size: 14px; font-weight: 600; color: #d0eaf8; }
.card-header-right { display: flex; align-items: center; gap: 6px; }
.card-sub { font-size: 12px; color: rgba(140,190,220,0.55); }
.mini-btn {
  padding: calc(2px * var(--scale-ratio, 1)) calc(8px * var(--scale-ratio, 1));
  font-size: calc(11px * var(--scale-ratio, 1));
  line-height: 1.5;
  background: rgba(0,100,180,0.2);
  border: 1px solid rgba(0,120,200,0.3);
  border-radius: 4px;
  color: rgba(140,200,230,0.7);
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}
.mini-btn:hover:not(:disabled) {
  background: rgba(0,120,200,0.35);
  border-color: rgba(77,208,225,0.5);
  color: #4dd0e1;
}
.mini-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.chart-area { height: calc(220px * var(--scale-ratio, 1)); }

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

/* 边缘AI决策 */
.edge-trigger-btn {
  display: inline-block; margin-left: 6px; width: 20px; height: 20px;
  background: rgba(0,200,180,0.15); border: 1px solid rgba(0,200,180,0.3);
  border-radius: 50%; color: rgba(150,240,230,0.9); font-size: 9px;
  cursor: pointer; transition: all 0.2s; vertical-align: middle;
}
.edge-trigger-btn:hover:not(:disabled) { background: rgba(0,200,180,0.3); }
.edge-trigger-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.edge-recent {
  background: rgba(8,20,45,0.8); border: 1px solid rgba(0,120,200,0.15);
  border-radius: 10px; padding: 14px 18px; margin-bottom: 16px;
}
.edge-log-list { display: flex; flex-direction: column; gap: 4px; }
.edge-log-item {
  display: flex; align-items: center; gap: 10px; padding: 5px 8px;
  background: rgba(0,30,70,0.3); border-radius: 4px; font-size: 12px;
}
.el-time { color: rgba(140,190,220,0.5); font-family: monospace; min-width: 60px; }
.el-device { color: rgba(140,190,220,0.7); font-weight: 600; min-width: 60px; }
.el-match { color: #4caf82; flex: 1; }
.el-nomatch { color: rgba(140,190,220,0.35); flex: 1; }
.el-action { color: rgba(200,220,240,0.6); font-family: monospace; }

</style>
