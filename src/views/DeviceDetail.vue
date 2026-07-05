<script setup>import { ref, onMounted, onBeforeUnmount, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElButton, ElCard, ElTag, ElRadioGroup, ElRadioButton, ElRow, ElCol, ElSlider, ElTable, ElTableColumn, ElMessage, ElMessageBox } from 'element-plus';
import { ArrowLeft, Lightning, Sunny, Moon, Refresh, Warning } from '@element-plus/icons-vue';
import { fetchDeviceHealth } from '../api/devices.js';
import * as echarts from 'echarts';
import { fetchDeviceDetail } from '../api/devices.js';
import { fetchLatestTelemetry, fetchTelemetryHistory } from '../api/telemetry.js';
import { sendControlCommand, getControlHistory } from '../api/control.js';
import { useUserInfo } from '../composables/useUserInfo.js';
const { hasPerm } = useUserInfo();
const route = useRoute();
const router = useRouter();
const deviceId = ref('');
const deviceInfo = ref(null);
const latestTelemetry = ref(null);
const historyData = ref([]);
const timeRange = ref('1h');
const loading = ref(false);
const chartRef = ref(null);
const tempHumidityChartRef = ref(null);
let chartInstance = null;
let tempHumidityChartInstance = null;
let resizeTimer = null;

const controlLoading = ref(false);
const controlHistory = ref([]);
const lightStatus = ref(false); // true=开灯, false=关灯
const healthDetail = ref(null); // 健康评分详情
const brightness = ref(80);
const controlPagination = ref({ page: 1, pageSize: 5 });
const controlTotal = ref(0);
const timeRangeOptions = [
 { label: '近1小时', value: '1h' },
 { label: '近24小时', value: '24h' },
 { label: '近7天', value: '7d' }
];
const statusTag = computed(() => {
 if (!deviceInfo.value)
 return { type: 'info', text: '--' };
 return deviceInfo.value.status === 'online'
 ? { type: 'success', text: '在线' }
 : { type: 'danger', text: '离线' };
});
const healthScoreColor = computed(() => {
 if (!deviceInfo.value)
 return '#909399';
 const score = deviceInfo.value.healthScore;
 if (score >= 80)
 return '#67c23a';
 if (score >= 60)
 return '#e6a23c';
 return '#f56c6c';
});
const pirStatus = computed(() => {
 if (!latestTelemetry.value)
 return { type: 'info', text: '--' };
 return latestTelemetry.value.pir === 1
 ? { type: 'success', text: '有人', icon: 'user' }
 : { type: 'info', text: '无人', icon: 'user-off' };
});
const dimLevelColor = computed(() => {
  const b = brightness.value;
  if (b >= 70) return '#e6a23c';
  if (b >= 40) return '#409eff';
  return '#606080';
});
const dimMarks = { 0: '0%', 25: '25%', 50: '50%', 75: '75%', 100: '100%' };
const formatTime = (date) => {
 if (!date)
 return '--';
 return new Date(date).toLocaleString('zh-CN', {
 month: '2-digit',
 day: '2-digit',
 hour: '2-digit',
 minute: '2-digit',
 second: '2-digit'
 });
};

const formatDateOnly = (dateRaw) => {
  if (!dateRaw) return '--';
  let dateArr = dateRaw;
  if (typeof dateRaw === 'string' && dateRaw.includes(',')) {
    dateArr = dateRaw.split(',').filter(x => x !== '').map(Number);
  }
  if (Array.isArray(dateArr) && dateArr.length >= 3) {
    const [year, month, day] = dateArr;
    return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
  }
  try {
    const d = new Date(dateRaw);
    if (isNaN(d.getTime())) return String(dateRaw);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  } catch(e) {
    return String(dateRaw);
  }
};
const goBack = () => {
  router.push('/devices');
};
const STATUS_NUM_MAP = { 0: 'disabled', 1: 'online', 2: 'offline', 3: 'abnormal' }

const mapDeviceInfo = (raw) => {
  if (!raw) return null
  const [lat = '--', lng = '--'] = (raw.location || '').split(',').map(s => s.trim())
  return {
    id: raw.id,
    deviceId: raw.deviceId,
    name: raw.name || '--',
    status: STATUS_NUM_MAP[raw.status] || 'offline',
    statusNum: raw.status,
    healthScore: raw.healthScore ?? 0,
    region: raw.area || '--',
    lastHeartbeat: raw.lastHeartbeatAt || '--',
    installTime: raw.installTime || '--',
    firmwareVersion: raw.firmwareVersion || 'V2.1.0',
    ipAddress: raw.ipAddress || raw.ip || '192.168.1.' + raw.id,
    latitude: lat,
    longitude: lng,
    latestData: raw.latestData,
  }
}

function parseLatestData(raw) {
  if (!raw) return null
  try { return typeof raw === 'string' ? JSON.parse(raw) : raw } catch { return null }
}

function applyControlState(data) {
  if (!data || !data.action) return
  if (data.action === 'OFF') {
    lightStatus.value = false
    brightness.value = data.brightness || 0
  } else if (data.action === 'ON') {
    lightStatus.value = true
    brightness.value = data.brightness || 100
  } else if (data.action.startsWith && data.action.startsWith('DIMMING')) {
    lightStatus.value = true
    const m = data.action.match(/DIMMING\((\d+)\)/)
    brightness.value = m ? parseInt(m[1]) : (data.brightness || 80)
  }
}

const loadHealth = async () => {
  try {
    const res = await fetchDeviceHealth(deviceId.value);
    if (res?.data) healthDetail.value = res.data;
  } catch { healthDetail.value = null; }
};

const loadDeviceInfo = async () => {
 loading.value = true;
 const res = await fetchDeviceDetail(deviceId.value);
 if (res.code === 200) {
   deviceInfo.value = mapDeviceInfo(res.data);
   // 从 latestData 恢复控制状态
   const state = parseLatestData(res.data?.latestData);
   applyControlState(state);
 }
 loading.value = false;
};
const loadLatestTelemetry = async () => {
 loading.value = true;
 const res = await fetchLatestTelemetry(deviceId.value);
 if (res.code === 200) {
 latestTelemetry.value = res.data;
 }
 loading.value = false;
};
const loadHistoryData = async () => {
 loading.value = true;
 const res = await fetchTelemetryHistory({
 deviceId: deviceId.value,
 timeRange: timeRange.value
 });
 if (res.code === 200) {
 historyData.value = res.data.list;
 updateChart();
 updateTempHumidityChart();
 }
 loading.value = false;
};
const initChart = () => {
 if (!chartRef.value)
 return;
 chartInstance = echarts.init(chartRef.value);
 updateChart();
 if (!tempHumidityChartRef.value)
 return;
 tempHumidityChartInstance = echarts.init(tempHumidityChartRef.value);
 updateTempHumidityChart();
};
const updateChart = () => {
 if (!chartInstance || historyData.value.length === 0)
 return;
 const option = {
 backgroundColor: 'transparent',
 title: {
 text: '光照度/PIR 历史趋势',
 left: 'center',
 textStyle: {
 fontSize: 16,
 fontWeight: 500,
 color: '#ffffff'
 }
 },
 tooltip: {
 trigger: 'axis',
 axisPointer: {
 type: 'cross',
 crossStyle: {
 color: '#606266'
 }
 },
 backgroundColor: 'rgba(30, 30, 50, 0.9)',
 borderColor: 'rgba(255, 255, 255, 0.1)',
 textStyle: {
 color: '#e0e0e0'
 }
 },
 legend: {
 data: ['光照度(lux)', 'PIR(有人=1)'],
 top: 30,
 textStyle: {
 color: '#909399'
 }
 },
 grid: {
 left: '3%',
 right: '4%',
 bottom: '3%',
 top: 80,
 containLabel: true
 },
 xAxis: {
 type: 'category',
 data: historyData.value.map(item => item.time),
 name: historyData.value.length > 0 ? ('\n\n\n' + historyData.value[0].time.substring(0, 4) + '年') : '',
 nameLocation: 'start',
 nameTextStyle: {
 color: '#909399',
 fontSize: 12,
 padding: [0, 5, 0, 0]
 },
 axisLabel: {
 color: '#909399',
 rotate: timeRange.value === '7d' ? 0 : 45,
 formatter: function(value) {
 if (!value) return '';
 const match = value.match(/^\d{4}-(\d{2}-\d{2})/);
 return match ? match[1] : value;
 }
 },
 axisLine: {
 lineStyle: {
 color: '#404050'
 }
 },
 axisTick: {
 show: false
 }
 },
 yAxis: [
 {
 type: 'value',
 name: '光照度(lux)',
 position: 'left',
 nameTextStyle: {
 color: '#909399'
 },
 axisLabel: {
 color: '#909399',
 formatter: '{value}'
 },
 axisLine: {
 show: true,
 lineStyle: {
 color: '#404050'
 }
 },
 splitLine: {
 lineStyle: {
 color: '#303040'
 }
 }
 },
 {
 type: 'value',
 name: 'PIR',
 position: 'right',
 min: 0,
 max: 1.5,
 interval: 0.5,
 nameTextStyle: {
 color: '#909399'
 },
 axisLabel: {
 color: '#909399',
 formatter: (value) => value === 1 ? '有人' : value === 0 ? '无人' : ''
 },
 axisLine: {
 show: true,
 lineStyle: {
 color: '#404050'
 }
 },
 splitLine: {
 show: false
 }
 }
 ],
 series: [
 {
 name: '光照度(lux)',
 type: 'line',
 data: historyData.value.map(item => item.illuminance),
 smooth: true,
 symbol: 'circle',
 symbolSize: 6,
 lineStyle: {
 color: '#409eff',
 width: 2
 },
 itemStyle: {
 color: '#409eff'
 },
 areaStyle: {
 color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
 { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
 { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
 ])
 }
 },
 {
 name: 'PIR(有人=1)',
 type: 'line',
 yAxisIndex: 1,
 data: historyData.value.map(item => item.pir),
 step: 'middle',
 symbol: 'circle',
 symbolSize: 6,
 lineStyle: {
 color: '#67c23a',
 width: 2
 },
 itemStyle: {
 color: '#67c23a'
 },
 areaStyle: {
 color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
 { offset: 0, color: 'rgba(103, 194, 58, 0.3)' },
 { offset: 1, color: 'rgba(103, 194, 58, 0.05)' }
 ])
 }
 }
 ]
 };
 chartInstance.setOption(option, true);
};
const updateTempHumidityChart = () => {
 if (!tempHumidityChartInstance || historyData.value.length === 0)
 return;
 const option = {
 backgroundColor: 'transparent',
 title: {
 text: '温度/湿度 历史趋势',
 left: 'center',
 textStyle: {
 fontSize: 16,
 fontWeight: 500,
 color: '#ffffff'
 }
 },
 tooltip: {
 trigger: 'axis',
 axisPointer: {
 type: 'cross',
 crossStyle: {
 color: '#606266'
 }
 },
 backgroundColor: 'rgba(30, 30, 50, 0.9)',
 borderColor: 'rgba(255, 255, 255, 0.1)',
 textStyle: {
 color: '#e0e0e0'
 }
 },
 legend: {
 data: ['温度(°C)', '湿度(%)'],
 top: 30,
 textStyle: {
 color: '#909399'
 }
 },
 grid: {
 left: '3%',
 right: '4%',
 bottom: '3%',
 top: 80,
 containLabel: true
 },
 xAxis: {
 type: 'category',
 data: historyData.value.map(item => item.time),
 name: historyData.value.length > 0 ? ('\n\n\n' + historyData.value[0].time.substring(0, 4) + '年') : '',
 nameLocation: 'start',
 nameTextStyle: {
 color: '#909399',
 fontSize: 12,
 padding: [0, 5, 0, 0]
 },
 axisLabel: {
 color: '#909399',
 rotate: timeRange.value === '7d' ? 0 : 45,
 formatter: function(value) {
 if (!value) return '';
 const match = value.match(/^\d{4}-(\d{2}-\d{2})/);
 return match ? match[1] : value;
 }
 },
 axisLine: {
 lineStyle: {
 color: '#404050'
 }
 },
 axisTick: {
 show: false
 }
 },
 yAxis: [
 {
 type: 'value',
 name: '温度(°C)',
 position: 'left',
 nameTextStyle: {
 color: '#909399'
 },
 axisLabel: {
 color: '#909399',
 formatter: '{value}'
 },
 axisLine: {
 show: true,
 lineStyle: {
 color: '#404050'
 }
 },
 splitLine: {
 lineStyle: {
 color: '#303040'
 }
 }
 },
 {
 type: 'value',
 name: '湿度(%)',
 position: 'right',
 min: 0,
 max: 100,
 nameTextStyle: {
 color: '#909399'
 },
 axisLabel: {
 color: '#909399',
 formatter: '{value}%'
 },
 axisLine: {
 show: true,
 lineStyle: {
 color: '#404050'
 }
 },
 splitLine: {
 show: false
 }
 }
 ],
 series: [
 {
 name: '温度(°C)',
 type: 'line',
 data: historyData.value.map(item => item.temperature),
 smooth: true,
 symbol: 'circle',
 symbolSize: 6,
 lineStyle: {
 color: '#f56c6c',
 width: 2
 },
 itemStyle: {
 color: '#f56c6c'
 },
 areaStyle: {
 color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
 { offset: 0, color: 'rgba(245, 108, 108, 0.3)' },
 { offset: 1, color: 'rgba(245, 108, 108, 0.05)' }
 ])
 }
 },
 {
 name: '湿度(%)',
 type: 'line',
 yAxisIndex: 1,
 data: historyData.value.map(item => item.humidity),
 smooth: true,
 symbol: 'circle',
 symbolSize: 6,
 lineStyle: {
 color: '#409eff',
 width: 2
 },
 itemStyle: {
 color: '#409eff'
 },
 areaStyle: {
 color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
 { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
 { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
 ])
 }
 }
 ]
 };
 tempHumidityChartInstance.setOption(option, true);
};
const handleResize = () => {
 if (resizeTimer) {
 clearTimeout(resizeTimer);
 }
 resizeTimer = setTimeout(() => {
 if (chartInstance) {
 chartInstance.resize();
 }
 if (tempHumidityChartInstance) {
 tempHumidityChartInstance.resize();
 }
 }, 300);
};
const handleTimeRangeChange = () => {
 loadHistoryData();
};
const handleControlCommand = async (command) => {
 if (!deviceInfo.value) {
 return;
 }
 controlLoading.value = true;
 try {
 const params = command === 'dim' ? { brightness: brightness.value } : {};
 const response = await sendControlCommand(deviceId.value, command, params);
 if (response.code === 200) {
 ElMessage.success(response.message);
 ElMessage.info(`执行反馈: ${response.data.feedback.message}`);
 // 更新灯光状态反馈
 if (command === 'turn_on') lightStatus.value = true;
 else if (command === 'turn_off') lightStatus.value = false;
 loadControlHistory();
 // 重新读取设备状态，同步 latestData 中的控制状态
 loadDeviceInfo();
 } else {
 ElMessage.error(response.message);
 }
 } catch (error) {
 ElMessage.error('发送指令失败');
 } finally {
 controlLoading.value = false;
 }
};
const loadControlHistory = async () => {
 try {
 const response = await getControlHistory(deviceId.value, controlPagination.value.page, controlPagination.value.pageSize);
 if (response.code === 200) {
 controlHistory.value = response.data.list;
 controlTotal.value = response.data.total;
 }
 } catch (error) {
 console.error('加载控制历史失败');
 }
};
async function initControlState() {
  // 优先从 latestData 读取控制状态
  const state = parseLatestData(deviceInfo.value?.latestData);
  if (state && state.action) {
    applyControlState(state);
    return;
  }
  // 兜底：从控制历史最后一条指令推断
  try {
    const res = await getControlHistory(deviceId.value, 1, 1);
    if (res.code === 200 && res.data.list.length > 0) {
      const last = res.data.list[0];
      if (last.command === 'turn_on') lightStatus.value = true;
      else if (last.command === 'turn_off') lightStatus.value = false;
      if (last.command === 'dim' && last.params?.brightness != null) {
        lightStatus.value = true;
        brightness.value = last.params.brightness;
      }
    }
  } catch { /* ignore */ }
}

onMounted(async () => {
 deviceId.value = route.params.id;
 await loadDeviceInfo();
 await initControlState();
 loadHealth();
 loadLatestTelemetry();
 loadHistoryData();
 loadControlHistory();
 setTimeout(() => {
 initChart();
 window.addEventListener('resize', handleResize);
 }, 100);
});
onBeforeUnmount(() => {
 if (resizeTimer) {
 clearTimeout(resizeTimer);
 }
 window.removeEventListener('resize', handleResize);
 if (chartInstance) {
 chartInstance.dispose();
 chartInstance = null;
 }
 if (tempHumidityChartInstance) {
 tempHumidityChartInstance.dispose();
 tempHumidityChartInstance = null;
 }
});
</script>

<template>
  <div class="device-detail-container">
    <!-- ========== 顶部导航栏 ========== -->
    <div class="top-nav">
      <button class="back-btn" @click="goBack" title="返回列表">
        <ArrowLeft />
        <span>返回</span>
      </button>
      <div class="nav-device-info">
        <span class="nav-status-dot" :class="deviceInfo?.status || 'offline'"></span>
        <span class="nav-device-name">{{ deviceInfo?.name || '设备详情' }}</span>
        <span class="nav-device-id">#{{ deviceInfo?.id || '--' }}</span>
      </div>
      <div class="nav-breadcrumb">
        <span>设备管理</span>
        <span class="sep">/</span>
        <span class="current">设备详情</span>
      </div>
    </div>

    <div class="content-area" v-loading="loading">
      <ElCard class="device-info-card">
        <div class="card-header">
          <div class="card-header-left">
            <span class="dev-status-badge" :class="deviceInfo?.status || 'offline'"></span>
            <h2 class="device-title">{{ deviceInfo?.name || '--' }}</h2>
            <ElTag :type="statusTag.type" size="large" class="dev-status-tag">
              {{ statusTag.text }}
            </ElTag>
          </div>
          <div class="card-header-right">
            <span class="update-time">
              最后更新: {{ formatTime(deviceInfo?.lastHeartbeat) }}
            </span>
          </div>
        </div>

        <div class="dev-info-body">
          <div class="dev-info-left">
            <div class="health-ring-wrap">
              <svg class="health-ring" viewBox="0 0 120 120">
                <circle cx="60" cy="60" r="50" fill="none" stroke="rgba(255,255,255,0.04)" stroke-width="8"/>
                <circle cx="60" cy="60" r="50" fill="none" :stroke="healthScoreColor" stroke-width="8"
                  stroke-linecap="round" transform="rotate(-90, 60, 60)"
                  :stroke-dasharray="314.16"
                  :stroke-dashoffset="314.16 * (1 - (deviceInfo?.healthScore || 0) / 100)" />
              </svg>
              <div class="health-ring-text">
                <span class="health-num">{{ deviceInfo?.healthScore || '--' }}</span>
                <span class="health-lbl">健康分</span>
              </div>
            </div>
            <div class="dev-short-meta">
              <div class="short-item">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="3" width="18" height="18" rx="2"/>
                </svg>
                <span>#{{ deviceInfo?.id || '--' }}</span>
              </div>
              <div class="short-item">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
                  <circle cx="12" cy="10" r="3"/>
                </svg>
                <span>{{ deviceInfo?.region || '--' }}</span>
              </div>
            </div>
          </div>

          <div class="dev-info-right">
            <div class="info-grid">
              <div class="info-cell">
                <span class="info-label">固件版本</span>
                <span class="info-value">{{ deviceInfo?.firmwareVersion || '--' }}</span>
              </div>
              <div class="info-cell">
                <span class="info-label">IP地址</span>
                <span class="info-value">{{ deviceInfo?.ipAddress || '--' }}</span>
              </div>
              <div class="info-cell">
                <span class="info-label">安装时间</span>
                <span class="info-value">{{ formatTime(deviceInfo?.installTime) }}</span>
              </div>
              <div class="info-cell">
                <span class="info-label">坐标位置</span>
                <span class="info-value">{{ deviceInfo?.latitude || '--' }}, {{ deviceInfo?.longitude || '--' }}</span>
              </div>
            </div>
          </div>
        </div>
      </ElCard>

      <!-- 健康评分详情 -->
      <ElCard v-if="healthDetail" class="health-detail-card">
        <div class="card-header">
          <h3>设备健康评分</h3>
          <span class="update-time">评估时间: {{ healthDetail?.evaluatedAt || '--' }}</span>
        </div>
        <div class="health-detail-body">
          <div class="health-overview">
            <div class="health-big-ring">
              <svg viewBox="0 0 120 120" class="health-big-svg">
                <circle cx="60" cy="60" r="50" fill="none" stroke="rgba(255,255,255,0.05)" stroke-width="10"/>
                <circle cx="60" cy="60" r="50" fill="none" :stroke="healthDetail?.levelColor" stroke-width="10"
                  stroke-linecap="round" transform="rotate(-90, 60, 60)"
                  :stroke-dasharray="314.16"
                  :stroke-dashoffset="314.16 * (1 - healthDetail.overallScore / 100)" />
              </svg>
              <div class="health-big-text">
                <span class="health-big-num" :style="{ color: healthDetail?.levelColor }">{{ healthDetail?.overallScore }}</span>
                <span class="health-big-lvl" :style="{ color: healthDetail?.levelColor }">{{ healthDetail?.level }}</span>
              </div>
            </div>
            <div class="health-suggestion">{{ healthDetail?.suggestion }}</div>
          </div>
          <div class="health-dimensions">
            <div v-for="d in healthDetail?.dimensions" :key="d.name" class="health-dim-row">
              <div class="dim-header">
                <span class="dim-name">{{ d.name }}</span>
                <span class="dim-weight">权重 {{ d.weight }}</span>
              </div>
              <div class="dim-bar-wrap">
                <div class="dim-bar" :style="{ width: d.score + '%', background: d.score >= 80 ? '#4caf50' : d.score >= 50 ? '#ff9800' : '#f44336' }"></div>
              </div>
              <div class="dim-footer">
                <span class="dim-score">{{ d.score }} 分</span>
                <span class="dim-reason" v-if="d.reason">{{ d.reason }}</span>
              </div>
            </div>
          </div>
        </div>
      </ElCard>

      <div class="section-title">
        <h3>实时遥测数据</h3>
        <span class="update-time">
          更新时间: {{ formatTime(latestTelemetry?.updateTime) }}
        </span>
      </div>

      <ElRow :gutter="20" class="telemetry-grid">
        <ElCol :span="6">
          <ElCard class="telemetry-card light-card">
            <div class="telemetry-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="5"/>
                <line x1="12" y1="1" x2="12" y2="3"/>
                <line x1="12" y1="21" x2="12" y2="23"/>
                <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/>
                <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>
                <line x1="1" y1="12" x2="3" y2="12"/>
                <line x1="21" y1="12" x2="23" y2="12"/>
                <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/>
                <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
              </svg>
            </div>
            <div class="telemetry-label">光照度</div>
            <div class="telemetry-value">
              {{ latestTelemetry?.illuminance || '--' }}
              <span class="unit">lux</span>
            </div>
          </ElCard>
        </ElCol>

        <ElCol :span="6">
          <ElCard class="telemetry-card temp-card">
            <div class="telemetry-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 14.76V3.5a2.5 2.5 0 0 0-5 0v11.26a4.5 4.5 0 1 0 5 0z"/>
              </svg>
            </div>
            <div class="telemetry-label">温度</div>
            <div class="telemetry-value">
              {{ latestTelemetry?.temperature || '--' }}
              <span class="unit">°C</span>
            </div>
          </ElCard>
        </ElCol>

        <ElCol :span="6">
          <ElCard class="telemetry-card humidity-card">
            <div class="telemetry-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"/>
              </svg>
            </div>
            <div class="telemetry-label">湿度</div>
            <div class="telemetry-value">
              {{ latestTelemetry?.humidity || '--' }}
              <span class="unit">%</span>
            </div>
          </ElCard>
        </ElCol>

        <ElCol :span="6">
          <ElCard class="telemetry-card pir-card">
            <div class="telemetry-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                <circle cx="12" cy="7" r="4"/>
              </svg>
            </div>
            <div class="telemetry-label">人体感应(PIR)</div>
            <div class="telemetry-value">
              <ElTag :type="pirStatus.type" size="small">
                {{ pirStatus.text }}
              </ElTag>
            </div>
          </ElCard>
        </ElCol>
      </ElRow>

      <div class="section-title">
        <h3>历史趋势</h3>
        <ElRadioGroup v-model="timeRange" @change="handleTimeRangeChange" class="time-range-group">
          <ElRadioButton v-for="option in timeRangeOptions" :key="option.value" :label="option.value">
            {{ option.label }}
          </ElRadioButton>
        </ElRadioGroup>
      </div>

      <ElRow :gutter="20" class="chart-row">
        <ElCol :span="12" class="chart-col">
          <ElCard class="chart-card">
            <div ref="chartRef" class="chart-container"></div>
          </ElCard>
        </ElCol>
        <ElCol :span="12" class="chart-col">
          <ElCard class="chart-card">
            <div ref="tempHumidityChartRef" class="chart-container"></div>
          </ElCard>
        </ElCol>
      </ElRow>

      <!-- ========== 远程控制面板 ========== -->
      <template v-if="hasPerm('device:control')">
        <div class="section-title">
          <h3>远程控制</h3>
        </div>

        <div class="control-panel">
        <!-- 设备状态栏 -->
        <div class="ctrl-status-bar">
          <div class="ctrl-status-left">
            <div class="ctrl-device-status" :class="deviceInfo?.status === 'online' ? 'is-online' : 'is-offline'">
              <span class="ctrl-pulse-dot"></span>
              <span>{{ deviceInfo?.status === 'online' ? '设备在线' : '设备离线' }}</span>
            </div>
            <div class="ctrl-light-status" :class="lightStatus ? 'light-on' : 'light-off'">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="light-status-icon">
                <circle cx="12" cy="12" r="5"/>
                <line x1="12" y1="1" x2="12" y2="3"/>
                <line x1="12" y1="21" x2="12" y2="23"/>
                <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/>
                <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>
                <line x1="1" y1="12" x2="3" y2="12"/>
                <line x1="21" y1="12" x2="23" y2="12"/>
              </svg>
              <span>{{ lightStatus ? '已开灯' : '已关灯' }}</span>
            </div>
          </div>
          <div class="ctrl-status-right">
            <span class="ctrl-last-action" v-if="controlHistory.length">
              上次操作: {{ controlHistory[0]?.command_label }} · {{ formatDateOnly(controlHistory[0]?.created_at) }}
            </span>
            <span class="ctrl-brightness-val" v-if="deviceInfo?.status === 'online'">
              亮度: {{ brightness }}%
            </span>
          </div>
        </div>

        <!-- 控制按钮网格 -->
        <div class="ctrl-btn-grid">
          <!-- 开灯 -->
          <button
            class="ctrl-btn ctrl-on"
            :class="{ loading: controlLoading, active: lightStatus }"
            @click="handleControlCommand('turn_on')"
          >
            <div class="ctrl-btn-icon"><Sunny /></div>
            <div class="ctrl-btn-label">开灯</div>
            <div class="ctrl-btn-hint">开启照明</div>
          </button>

          <!-- 关灯 -->
          <button
            class="ctrl-btn ctrl-off"
            :class="{ loading: controlLoading, active: !lightStatus }"
            @click="handleControlCommand('turn_off')"
          >
            <div class="ctrl-btn-icon"><Moon /></div>
            <div class="ctrl-btn-label">关灯</div>
            <div class="ctrl-btn-hint">关闭照明</div>
          </button>

          <!-- 调光 -->
          <div class="ctrl-dim-card">
            <div class="dim-header">
              <div class="dim-header-left">
                <div class="dim-icon"><Lightning /></div>
                <div class="dim-info">
                  <span class="dim-label">亮度调节</span>
                  <span class="dim-hint">拖动滑块调整亮度</span>
                </div>
              </div>
              <div class="dim-value-wrap">
                <span class="dim-value" :style="{ color: dimLevelColor }">{{ brightness }}</span>
                <span class="dim-unit">%</span>
              </div>
            </div>
            <div class="dim-slider-area">
              <div class="dim-level-icons">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="dim-level-icon low">
                  <circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/>
                  <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>
                  <line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/>
                </svg>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="dim-level-icon high">
                  <circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/>
                  <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>
                  <line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/>
                  <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
                </svg>
              </div>
              <ElSlider
                v-model="brightness"
                :min="0"
                :max="100"
                :marks="dimMarks"
                class="dim-slider-el"
                @change="handleControlCommand('dim')"
              />
            </div>
          </div>

          <!-- 重启 -->
          <button
            class="ctrl-btn ctrl-restart"
            :class="{ loading: controlLoading }"
            @click="handleControlCommand('restart')"
          >
            <div class="ctrl-btn-icon"><Refresh /></div>
            <div class="ctrl-btn-label">重启</div>
            <div class="ctrl-btn-hint">重启设备</div>
          </button>
        </div>
      </div>
      </template>

      <ElCard class="control-history-card">
        <template #header>
          <div class="history-header">
            <div class="history-header-left">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="history-icon">
                <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
              </svg>
              <span>控制历史</span>
            </div>
            <div class="history-header-right">
              <span class="history-count-badge success">
                <span class="badge-dot" />成功 {{ controlHistory.filter(h => h.status === 'success').length }}
              </span>
              <span class="history-count-badge danger">
                <span class="badge-dot" />失败 {{ controlHistory.filter(h => h.status === 'failed').length }}
              </span>
            </div>
          </div>
        </template>
        <ElTable
          :data="controlHistory"
          style="width: 100%"
          stripe
          size="small"
          empty-text="暂无控制记录"
        >
          <ElTableColumn prop="command_label" label="指令" width="100">
            <template #default="scope">
              <span class="cmd-cell">
                <span :class="['cmd-dot', scope.row.command]"></span>
                {{ scope.row.command_label }}
              </span>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="status_label" label="状态" width="90" align="center">
            <template #default="scope">
              <span :class="['status-tag', scope.row.status === 'success' ? 'status-success' : 'status-failed']">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" class="status-icon">
                  <polyline v-if="scope.row.status === 'success'" points="20 6 9 17 4 12"/>
                  <g v-else><circle cx="12" cy="12" r="2"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></g>
                </svg>
                {{ scope.row.status_label }}
              </span>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="message" label="反馈信息" min-width="180" show-overflow-tooltip />
          <ElTableColumn prop="created_at" label="下发时间" width="170" align="center">
            <template #default="scope">
              <span class="time-cell">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="time-icon"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                {{ formatDateOnly(scope.row.created_at) }}
              </span>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="executed_at" label="执行时间" width="170" align="center">
            <template #default="scope">
              <span class="time-cell">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="time-icon"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
                {{ formatDateOnly(scope.row.executed_at) }}
              </span>
            </template>
          </ElTableColumn>
        </ElTable>
      </ElCard>
    </div>
  </div>
</template>

<style scoped>
/* ============ 全局容器 ============ */
.device-detail-container {
  padding: 28px 32px;
  min-height: 100vh;
  background: linear-gradient(135deg, #0f0f1e 0%, #1a1a2e 40%, #16213e 100%);
}

.content-area {
  max-width: 1440px;
  margin: 0 auto;
}

/* ============ 顶部导航栏 ============ */
.top-nav {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 28px;
  padding: 10px 20px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 14px;
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 10px;
  color: #a0a0b0;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.25s ease;
  white-space: nowrap;
}

.back-btn:hover {
  background: rgba(64, 158, 255, 0.12);
  border-color: rgba(64, 158, 255, 0.25);
  color: #409eff;
  transform: translateX(-2px);
}

.back-btn svg {
  width: 18px;
  height: 18px;
}

.nav-device-info {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.nav-status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
  position: relative;
}

.nav-status-dot.online {
  background: #4caf50;
  box-shadow: 0 0 8px rgba(76, 175, 80, 0.5);
}

.nav-status-dot.offline {
  background: #9e9e9e;
}

.nav-status-dot.abnormal {
  background: #ffa726;
  box-shadow: 0 0 8px rgba(255, 167, 38, 0.5);
}

.nav-status-dot.disabled {
  background: #666;
}

.nav-device-name {
  font-size: 16px;
  font-weight: 600;
  color: #ffffff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.nav-device-id {
  font-size: 12px;
  color: #606080;
  background: rgba(255, 255, 255, 0.04);
  padding: 2px 10px;
  border-radius: 6px;
  border: 1px solid rgba(255, 255, 255, 0.05);
  white-space: nowrap;
}

.nav-breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #606080;
  white-space: nowrap;
}

.nav-breadcrumb .sep {
  color: #404050;
}

.nav-breadcrumb .current {
  color: #409eff;
  font-weight: 500;
}

/* ============ 健康评分卡片 ============ */
.health-detail-card {
  margin-bottom: 24px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 16px;
  overflow: hidden;
  backdrop-filter: blur(20px);
}
.health-detail-body { display: flex; gap: 32px; align-items: flex-start; padding: 0 0 16px 0; }
.health-overview { flex-shrink: 0; text-align: center; width: 150px; }
.health-big-svg { width: 100px; height: 100px; }
.health-big-ring { position: relative; display: inline-block; }
.health-big-text { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); text-align: center; }
.health-big-num { font-size: 26px; font-weight: 700; display: block; }
.health-big-lvl { font-size: 12px; font-weight: 500; }
.health-suggestion { margin-top: 8px; font-size: 12px; color: rgba(200,210,230,0.7); line-height: 1.5; padding: 0 8px; }
.health-dimensions { flex: 1; display: flex; flex-direction: column; gap: 10px; padding-right: 16px; }
.health-dim-row { padding-bottom: 6px; border-bottom: 1px solid rgba(255,255,255,0.03); }
.dim-header { display: flex; justify-content: space-between; margin-bottom: 4px; }
.dim-name { font-size: 13px; color: rgba(200,220,240,0.85); }
.dim-weight { font-size: 11px; color: rgba(140,190,220,0.5); }
.dim-bar-wrap { height: 5px; background: rgba(255,255,255,0.06); border-radius: 3px; overflow: hidden; }
.dim-bar { height: 100%; border-radius: 3px; transition: width 0.5s; }
.dim-footer { display: flex; gap: 10px; margin-top: 2px; }
.dim-score { font-size: 11px; font-weight: 600; color: rgba(180,210,235,0.8); }
.dim-reason { font-size: 11px; color: rgba(200,140,80,0.7); }

/* ============ 设备信息卡片 ============ */
.device-info-card {
  margin-bottom: 28px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 16px;
  overflow: hidden;
  backdrop-filter: blur(20px);
  transition: all 0.3s ease;
}

.device-info-card:hover {
  border-color: rgba(64, 158, 255, 0.2);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

/* 卡片头部 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px 0;
}

.card-header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.card-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.dev-status-badge {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex-shrink: 0;
  position: relative;
}

.dev-status-badge.online {
  background: #4caf50;
  box-shadow: 0 0 10px rgba(76, 175, 80, 0.6);
  animation: pulse-dot 2s ease-in-out infinite;
}

.dev-status-badge.offline {
  background: #9e9e9e;
}

.dev-status-badge.abnormal {
  background: #ffa726;
  box-shadow: 0 0 10px rgba(255, 167, 38, 0.6);
}

.dev-status-badge.disabled {
  background: #666;
}

@keyframes pulse-dot {
  0%, 100% { box-shadow: 0 0 0 0 rgba(76, 175, 80, 0.5); }
  50% { box-shadow: 0 0 0 8px rgba(76, 175, 80, 0); }
}

.device-title {
  font-size: 26px;
  font-weight: 700;
  color: #ffffff;
  margin: 0;
  letter-spacing: 0.5px;
  background: linear-gradient(90deg, #ffffff, #a0c4ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.dev-status-tag {
  flex-shrink: 0;
}

/* 卡片主体 - 左右布局 */
.dev-info-body {
  display: flex;
  gap: 24px;
  padding: 20px 24px 24px;
}

/* 左侧：健康环 + 快捷信息 */
.dev-info-left {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  min-width: 160px;
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.02);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.04);
}

.health-ring-wrap {
  position: relative;
  width: 110px;
  height: 110px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.health-ring {
  width: 110px;
  height: 110px;
  transform: rotate(0deg);
}

.health-ring-text {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
  line-height: 1;
}

.health-num {
  font-size: 28px;
  font-weight: 800;
  color: #ffffff;
  letter-spacing: -1px;
}

.health-lbl {
  font-size: 11px;
  color: #606080;
  margin-top: 4px;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.dev-short-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.short-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #a0a0b0;
  padding: 6px 10px;
  background: rgba(255, 255, 255, 0.02);
  border-radius: 8px;
}

.short-item svg {
  width: 14px;
  height: 14px;
  color: #606080;
  flex-shrink: 0;
}

/* 右侧：信息网格 */
.dev-info-right {
  flex: 1;
  min-width: 0;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  height: 100%;
}

.info-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 18px;
  background: rgba(255, 255, 255, 0.02);
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.04);
  transition: all 0.25s ease;
}

.info-cell:hover {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(64, 158, 255, 0.15);
}

.info-label {
  font-size: 12px;
  font-weight: 500;
  color: #606080;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.info-value {
  font-size: 14px;
  font-weight: 500;
  color: #e0e0e0;
  word-break: break-all;
}

/* ============ 遥测卡片网格 ============ */
.telemetry-grid {
  margin-bottom: 28px;
}

.telemetry-card {
  height: 100%;
  text-align: center;
  padding: 24px 16px !important;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 16px;
  backdrop-filter: blur(20px);
  overflow: hidden;
  position: relative;
}

.telemetry-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  opacity: 0;
  transition: opacity 0.4s ease;
}

.light-card::before { background: linear-gradient(90deg, #e6a23c, #f0c040); }
.temp-card::before  { background: linear-gradient(90deg, #f56c6c, #fc8b8b); }
.humidity-card::before { background: linear-gradient(90deg, #409eff, #6ab0ff); }
.pir-card::before { background: linear-gradient(90deg, #67c23a, #8de060); }

.telemetry-card:hover::before {
  opacity: 1;
}

.telemetry-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.4);
  border-color: rgba(255, 255, 255, 0.12);
}

.telemetry-icon {
  width: 56px;
  height: 56px;
  margin: 0 auto 14px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.4s ease;
  position: relative;
}

.telemetry-card:hover .telemetry-icon {
  transform: scale(1.1) rotate(5deg);
}

.telemetry-icon svg {
  width: 26px;
  height: 26px;
}

.light-card .telemetry-icon {
  background: linear-gradient(135deg, rgba(230, 162, 60, 0.25), rgba(230, 162, 60, 0.05));
  color: #e6a23c;
  box-shadow: 0 4px 15px rgba(230, 162, 60, 0.15);
}

.temp-card .telemetry-icon {
  background: linear-gradient(135deg, rgba(245, 108, 108, 0.25), rgba(245, 108, 108, 0.05));
  color: #f56c6c;
  box-shadow: 0 4px 15px rgba(245, 108, 108, 0.15);
}

.humidity-card .telemetry-icon {
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.25), rgba(64, 158, 255, 0.05));
  color: #409eff;
  box-shadow: 0 4px 15px rgba(64, 158, 255, 0.15);
}

.pir-card .telemetry-icon {
  background: linear-gradient(135deg, rgba(103, 194, 58, 0.25), rgba(103, 194, 58, 0.05));
  color: #67c23a;
  box-shadow: 0 4px 15px rgba(103, 194, 58, 0.15);
}

.telemetry-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 10px;
  text-transform: uppercase;
  letter-spacing: 1px;
  font-weight: 500;
}

.telemetry-value {
  font-size: 34px;
  font-weight: 800;
  color: #ffffff;
  letter-spacing: -1px;
  line-height: 1.1;
}

.telemetry-value .unit {
  font-size: 14px;
  font-weight: 400;
  color: #606080;
  margin-left: 4px;
  letter-spacing: 0;
}

/* ============ 通用区块标题 ============ */
.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
  padding-left: 4px;
}

.section-title h3 {
  font-size: 18px;
  font-weight: 600;
  color: #ffffff;
  margin: 0;
  position: relative;
  padding-left: 14px;
}

.section-title h3::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 18px;
  background: linear-gradient(180deg, #409eff, #6ab0ff);
  border-radius: 2px;
}

.update-time {
  font-size: 12px;
  color: #606080;
  background: rgba(255, 255, 255, 0.03);
  padding: 4px 12px;
  border-radius: 6px;
  border: 1px solid rgba(255, 255, 255, 0.04);
}

/* ============ 时间选择器 ============ */
.time-range-group {
  background: rgba(255, 255, 255, 0.03);
  padding: 4px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.time-range-group :deep(.el-radio-button__inner) {
  background: transparent !important;
  border: none !important;
  color: #909399 !important;
  padding: 8px 20px;
  border-radius: 8px !important;
  transition: all 0.3s ease;
  box-shadow: none !important;
}

.time-range-group :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: rgba(64, 158, 255, 0.2) !important;
  color: #409eff !important;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15) !important;
}

.time-range-group :deep(.el-radio-button__inner:hover) {
  color: #a0c4ff !important;
}

/* ============ 图表卡片 ============ */
.chart-row {
  margin-bottom: 28px !important;
}

.chart-col {
  margin-bottom: 0 !important;
}

.chart-card {
  padding: 20px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 16px;
  backdrop-filter: blur(20px);
  transition: all 0.3s ease;
}

.chart-card:hover {
  border-color: rgba(64, 158, 255, 0.15);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.chart-container {
  width: 100%;
  height: 380px;
}

/* ============ 远程控制面板 ============ */
.control-panel {
  margin-bottom: 28px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 16px;
  backdrop-filter: blur(20px);
  overflow: hidden;
  transition: all 0.3s ease;
}

.control-panel:hover {
  border-color: rgba(64, 158, 255, 0.15);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

/* 状态栏 */
.ctrl-status-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 24px;
  background: rgba(255, 255, 255, 0.02);
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
}

.ctrl-status-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ctrl-device-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 500;
  padding: 4px 14px;
  border-radius: 20px;
}

.ctrl-device-status.is-online {
  color: #4caf50;
  background: rgba(76, 175, 80, 0.1);
}

.ctrl-device-status.is-offline {
  color: #9e9e9e;
  background: rgba(158, 158, 158, 0.1);
}

.ctrl-pulse-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
}

.ctrl-device-status.is-online .ctrl-pulse-dot {
  animation: pulse-online 1.8s ease-in-out infinite;
}

/* 灯光状态指示器 */
.ctrl-light-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  padding: 4px 14px;
  border-radius: 20px;
  transition: all 0.4s ease;
}

.ctrl-light-status.light-on {
  color: #ffd666;
  background: rgba(255, 214, 102, 0.12);
  box-shadow: 0 0 20px rgba(255, 214, 102, 0.08);
}

.ctrl-light-status.light-off {
  color: #808090;
  background: rgba(128, 128, 144, 0.08);
}

.light-status-icon {
  width: 16px;
  height: 16px;
  transition: all 0.4s ease;
}

.ctrl-light-status.light-on .light-status-icon {
  color: #ffd666;
  filter: drop-shadow(0 0 6px rgba(255, 214, 102, 0.5));
  animation: light-pulse 2s ease-in-out infinite;
}

.ctrl-light-status.light-off .light-status-icon {
  color: #606080;
}

@keyframes light-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

@keyframes pulse-online {
  0%, 100% { box-shadow: 0 0 0 0 rgba(76, 175, 80, 0.5); }
  50% { box-shadow: 0 0 0 6px rgba(76, 175, 80, 0); }
}

.ctrl-status-right {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 12px;
  color: #606080;
}

.ctrl-last-action {
  padding: 4px 10px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 6px;
  border: 1px solid rgba(255, 255, 255, 0.04);
}

.ctrl-brightness-val {
  padding: 4px 10px;
  background: rgba(64, 158, 255, 0.08);
  border-radius: 6px;
  color: #409eff;
  font-weight: 500;
}

/* 按钮网格 */
.ctrl-btn-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  padding: 20px 24px 24px;
}

.ctrl-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 20px 16px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  background: rgba(255, 255, 255, 0.02);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  user-select: none;
  min-height: 130px;
}

button.ctrl-btn {
  font-family: inherit;
  color: inherit;
}

.ctrl-btn:not(:disabled):hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.35);
}

.ctrl-btn:disabled,
.ctrl-btn.disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.ctrl-btn:disabled:hover,
.ctrl-btn.disabled:hover {
  transform: none;
  box-shadow: none;
}

.ctrl-btn.loading {
  pointer-events: none;
  opacity: 0.7;
}

/* 各按钮主题色 */
.ctrl-on:not(:disabled) {
  border-color: rgba(103, 194, 58, 0.2);
}

.ctrl-on:not(:disabled):hover {
  background: rgba(103, 194, 58, 0.08);
  border-color: rgba(103, 194, 58, 0.35);
}

.ctrl-on.active {
  background: rgba(103, 194, 58, 0.15);
  border-color: rgba(103, 194, 58, 0.5);
  box-shadow: 0 0 20px rgba(103, 194, 58, 0.12), inset 0 0 20px rgba(103, 194, 58, 0.04);
}

.ctrl-on .ctrl-btn-icon {
  color: #67c23a;
}

.ctrl-off:not(:disabled) {
  border-color: rgba(245, 108, 108, 0.2);
}

.ctrl-off:not(:disabled):hover {
  background: rgba(245, 108, 108, 0.08);
  border-color: rgba(245, 108, 108, 0.35);
}

.ctrl-off.active {
  background: rgba(245, 108, 108, 0.12);
  border-color: rgba(245, 108, 108, 0.4);
  box-shadow: 0 0 20px rgba(245, 108, 108, 0.1), inset 0 0 20px rgba(245, 108, 108, 0.03);
}

.ctrl-off .ctrl-btn-icon {
  color: #f56c6c;
}

.ctrl-dim:not(.disabled) {
  border-color: rgba(64, 158, 255, 0.2);
}

.ctrl-dim:not(.disabled):hover {
  border-color: rgba(64, 158, 255, 0.35);
}

.ctrl-dim:not(.disabled):hover .ctrl-btn-inner {
  background: rgba(64, 158, 255, 0.08);
}

.ctrl-dim .ctrl-btn-icon {
  color: #409eff;
}

.ctrl-restart:not(:disabled) {
  border-color: rgba(230, 162, 60, 0.2);
}

.ctrl-restart:not(:disabled):hover {
  background: rgba(230, 162, 60, 0.08);
  border-color: rgba(230, 162, 60, 0.35);
}

.ctrl-restart .ctrl-btn-icon {
  color: #e6a23c;
}

/* ============ 调光卡片 ============ */
.ctrl-dim-card {
  display: flex;
  flex-direction: column;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  background: rgba(255, 255, 255, 0.02);
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  min-height: 130px;
}

.ctrl-dim-card.disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.ctrl-dim-card:not(.disabled):hover {
  border-color: rgba(64, 158, 255, 0.35);
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.35);
  transform: translateY(-4px);
}

/* 调光头部 */
.dim-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 18px 12px;
}

.dim-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.dim-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.04);
  color: #409eff;
  transition: all 0.3s ease;
}

.ctrl-dim-card:not(.disabled):hover .dim-icon {
  transform: scale(1.1);
  background: rgba(64, 158, 255, 0.12);
}

.dim-icon svg {
  width: 22px;
  height: 22px;
}

.dim-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.dim-label {
  font-size: 15px;
  font-weight: 600;
  color: #ffffff;
}

.dim-hint {
  font-size: 11px;
  color: #606080;
}

.dim-value-wrap {
  display: flex;
  align-items: baseline;
  gap: 2px;
  padding: 8px 14px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.dim-value {
  font-size: 26px;
  font-weight: 800;
  letter-spacing: -1px;
  line-height: 1;
  transition: color 0.3s ease;
}

.dim-unit {
  font-size: 13px;
  font-weight: 500;
  color: #606080;
}

/* 调光滑块区域 */
.dim-slider-area {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 18px 16px;
}

.dim-level-icons {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.dim-level-icon {
  width: 16px;
  height: 16px;
  color: #606080;
}

.dim-level-icon.high {
  color: #e6a23c;
}

.dim-slider-el {
  flex: 1;
}

.dim-slider-el :deep(.el-slider__runway) {
  background: rgba(255, 255, 255, 0.08);
  height: 10px;
  border-radius: 5px;
  cursor: pointer;
}

.dim-slider-el :deep(.el-slider__bar) {
  background: linear-gradient(90deg, #606080, #409eff, #e6a23c);
  height: 10px;
  border-radius: 5px;
}

.dim-slider-el :deep(.el-slider__button-wrapper) {
  display: none;
}

.dim-slider-el :deep(.el-slider__runway:hover) {
  height: 10px;
  background: rgba(255, 255, 255, 0.12);
}

.dim-slider-el :deep(.el-slider__marks) {
  margin-top: 4px;
}

.dim-slider-el :deep(.el-slider__marks-text) {
  font-size: 10px;
  color: #505060;
  white-space: nowrap;
}

/* ============ 控制历史表格 ============ */
.control-history-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 16px;
  backdrop-filter: blur(20px);
  transition: all 0.3s ease;
  overflow: hidden;
}

.control-history-card:hover {
  border-color: rgba(64, 158, 255, 0.15);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

/* 卡片头部 */
.control-history-card :deep(.el-card__header) {
  padding: 16px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  background: rgba(255, 255, 255, 0.02);
}

.history-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.history-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
  color: #e0e0e0;
}

.history-icon {
  width: 20px;
  height: 20px;
  color: #409eff;
}

.history-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.history-count-badge {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  font-weight: 500;
  padding: 3px 10px;
  border-radius: 20px;
}

.history-count-badge.success {
  color: #67c23a;
  background: rgba(103, 194, 58, 0.08);
}

.history-count-badge.danger {
  color: #f56c6c;
  background: rgba(245, 108, 108, 0.08);
}

.badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  display: inline-block;
}

.history-count-badge.success .badge-dot {
  background: #67c23a;
  box-shadow: 0 0 6px rgba(103, 194, 58, 0.5);
}

.history-count-badge.danger .badge-dot {
  background: #f56c6c;
  box-shadow: 0 0 6px rgba(245, 108, 108, 0.5);
}

/* 表格暗色主题 */
.control-history-card :deep(.el-table) {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(255, 255, 255, 0.02);
  --el-table-row-hover-bg-color: rgba(64, 158, 255, 0.04);
  --el-table-border-color: transparent;
  --el-table-text-color: #d0d0d8;
  --el-table-header-text-color: #808090;
}

.control-history-card :deep(.el-table__header-wrapper) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
}

.control-history-card :deep(.el-table th.el-table__cell) {
  background: rgba(255, 255, 255, 0.02) !important;
  font-weight: 600;
  font-size: 12px;
  letter-spacing: 0.5px;
  text-transform: uppercase;
  padding: 12px 8px;
}

.control-history-card :deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.03);
  padding: 10px 8px;
  font-size: 13px;
}

.control-history-card :deep(.el-table__row) {
  transition: background 0.2s ease;
}

.control-history-card :deep(.el-table__row:last-child td) {
  border-bottom: none;
}

/* 指令单元格 */
.cmd-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.cmd-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.cmd-dot.turn_on {
  background: #67c23a;
  box-shadow: 0 0 8px rgba(103, 194, 58, 0.4);
}

.cmd-dot.turn_off {
  background: #909399;
  box-shadow: 0 0 8px rgba(144, 147, 153, 0.3);
}

.cmd-dot.dim {
  background: #409eff;
  box-shadow: 0 0 8px rgba(64, 158, 255, 0.4);
}

.cmd-dot.restart {
  background: #e6a23c;
  box-shadow: 0 0 8px rgba(230, 162, 60, 0.4);
}

/* 状态标签 */
.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 20px;
  letter-spacing: 0.3px;
}

.status-success {
  color: #67c23a;
  background: rgba(103, 194, 58, 0.08);
}

.status-failed {
  color: #f56c6c;
  background: rgba(245, 108, 108, 0.08);
}

.status-icon {
  width: 12px;
  height: 12px;
}

/* 时间单元格 */
.time-cell {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #808090;
  font-family: 'SF Mono', 'Cascadia Code', monospace;
  letter-spacing: 0.2px;
}

.time-icon {
  width: 13px;
  height: 13px;
  color: #606080;
  flex-shrink: 0;
}

/* 空状态 */
.control-history-card :deep(.el-table__empty-text) {
  color: #505060;
  font-size: 14px;
  padding: 30px 0;
  letter-spacing: 0.5px;
}

/* 斑马条纹 */
.control-history-card :deep(.el-table__body tr.el-table__row--striped) {
  background: rgba(255, 255, 255, 0.015);
}

/* ============ 响应式适配 ============ */
@media (max-width: 1200px) {
  .telemetry-grid :deep(.el-col) {
    flex: 0 0 50%;
    max-width: 50%;
  }

  .ctrl-btn-grid {
    grid-template-columns: 1fr;
  }

  .ctrl-dim-card {
    order: -1;
  }
}

@media (max-width: 992px) {
  .dev-info-body {
    flex-direction: column;
    align-items: stretch;
  }

  .dev-info-left {
    flex-direction: row;
    min-width: auto;
    padding: 16px 20px;
  }

  .health-ring-wrap {
    width: 80px;
    height: 80px;
    flex-shrink: 0;
  }

  .health-ring {
    width: 80px;
    height: 80px;
  }

  .health-num {
    font-size: 22px;
  }

  .chart-col {
    flex: 0 0 100%;
    max-width: 100%;
  }
}

@media (max-width: 768px) {
  .device-detail-container {
    padding: 16px;
  }

  .top-nav {
    flex-wrap: wrap;
    padding: 10px 14px;
    gap: 10px;
  }

  .nav-device-info {
    order: -1;
    flex: 0 0 100%;
    margin-bottom: 2px;
  }

  .nav-breadcrumb {
    margin-left: auto;
  }

  .telemetry-grid :deep(.el-col) {
    flex: 0 0 100%;
    max-width: 100%;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .section-title {
    flex-wrap: wrap;
    gap: 12px;
  }

  .chart-container {
    height: 280px;
  }

  .ctrl-btn-grid {
    grid-template-columns: 1fr;
  }

  .ctrl-dim-card {
    order: -1;
  }

  .ctrl-status-bar {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
    padding: 12px 16px;
  }

  .ctrl-status-right {
    width: 100%;
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .device-title {
    font-size: 22px;
  }

  .telemetry-value {
    font-size: 28px;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .dev-info-left {
    flex-direction: row;
    flex-wrap: wrap;
    justify-content: center;
  }

  .dim-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .dim-value-wrap {
    align-self: flex-start;
  }
}

@media (max-width: 480px) {
  .device-detail-container {
    padding: 12px;
  }

  .telemetry-card {
    padding: 18px 12px !important;
  }

  .ctrl-btn {
    min-height: 110px;
  }
}
</style>