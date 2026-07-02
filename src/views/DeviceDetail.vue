<script setup>import { ref, onMounted, onBeforeUnmount, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElButton, ElCard, ElTag, ElProgress, ElDescriptions, ElDescriptionsItem, ElRadioGroup, ElRadioButton, ElRow, ElCol, ElSlider, ElTable, ElTableColumn, ElMessage, ElMessageBox } from 'element-plus';
import { ArrowLeft, Battery, Sunny, Moon, Refresh, Warning } from '@element-plus/icons-vue';
import * as echarts from 'echarts';
import { fetchDeviceDetail } from '../api/device';
import { fetchLatestTelemetry, fetchTelemetryHistory } from '../api/telemetry';
import { sendControlCommand, getControlHistory } from '../api/control';
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
const goBack = () => {
 router.push('/device/list');
};
const loadDeviceInfo = async () => {
 loading.value = true;
 const res = await fetchDeviceDetail(deviceId.value);
 if (res.code === 200) {
 deviceInfo.value = res.data;
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
 axisLabel: {
 color: '#909399',
 rotate: timeRange.value === '7d' ? 0 : 45
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
 axisLabel: {
 color: '#909399',
 rotate: timeRange.value === '7d' ? 0 : 45
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
 if (!deviceInfo.value || deviceInfo.value.status !== 'online') {
 ElMessage.warning('设备离线，无法执行远程控制');
 return;
 }
 controlLoading.value = true;
 try {
 const params = command === 'dim' ? { brightness: brightness.value } : {};
 const response = await sendControlCommand(deviceId.value, command, params);
 if (response.code === 200) {
 ElMessage.success(response.message);
 ElMessage.info(`执行反馈: ${response.data.feedback.message}`);
 loadControlHistory();
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
onMounted(() => {
 deviceId.value = route.params.id;
 loadDeviceInfo();
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
    <div class="breadcrumb-bar">
      <ElButton @click="goBack" type="primary" plain>
        <ArrowLeft />
        返回列表
      </ElButton>
      <div class="breadcrumb">
        <span>设备管理</span>
        <span class="separator">/</span>
        <span>设备详情</span>
      </div>
    </div>

    <div class="content-area" v-loading="loading">
      <ElCard class="device-info-card">
        <div class="card-header">
          <h2 class="device-title">{{ deviceInfo?.name || '--' }}</h2>
          <ElTag :type="statusTag.type" size="large">
            {{ statusTag.text }}
          </ElTag>
        </div>
        
        <ElDescriptions :column="2" border>
          <ElDescriptionsItem label="设备ID">
            {{ deviceInfo?.id || '--' }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="所在区域">
            {{ deviceInfo?.region || '--' }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="健康评分">
            <div class="health-item">
              <span 
                class="health-value" 
                :style="{ color: healthScoreColor }"
              >
                {{ deviceInfo?.healthScore || '--' }}
              </span>
              <ElProgress
                :percentage="deviceInfo?.healthScore || 0"
                :color="healthScoreColor"
                :stroke-width="8"
                :show-text="false"
                style="width: 150px"
              />
            </div>
          </ElDescriptionsItem>
          <ElDescriptionsItem label="最后心跳">
            {{ formatTime(deviceInfo?.lastHeartbeat) }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="安装时间">
            {{ formatTime(deviceInfo?.installTime) }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="固件版本">
            {{ deviceInfo?.firmwareVersion || '--' }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="IP地址">
            {{ deviceInfo?.ipAddress || '--' }}
          </ElDescriptionsItem>
          <ElDescriptionsItem label="坐标位置">
            {{ deviceInfo?.latitude || '--' }}, {{ deviceInfo?.longitude || '--' }}
          </ElDescriptionsItem>
        </ElDescriptions>
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

      <ElCard class="chart-card">
        <div ref="chartRef" class="chart-container"></div>
      </ElCard>

      <ElCard class="chart-card">
        <div ref="tempHumidityChartRef" class="chart-container"></div>
      </ElCard>

      <div class="section-title">
        <h3>远程控制</h3>
        <ElTag v-if="deviceInfo?.status === 'online'" type="success">
          设备在线，可执行控制
        </ElTag>
        <ElTag v-else type="danger">
          设备离线，无法控制
        </ElTag>
      </div>

      <ElCard class="control-card">
        <div class="control-grid">
          <div class="control-item">
            <ElButton
              type="success"
              size="large"
              :loading="controlLoading"
              :disabled="deviceInfo?.status !== 'online'"
              @click="handleControlCommand('turn_on')"
              class="control-btn"
            >
              <Sunny />
              开灯
            </ElButton>
          </div>
          <div class="control-item">
            <ElButton
              type="danger"
              size="large"
              :loading="controlLoading"
              :disabled="deviceInfo?.status !== 'online'"
              @click="handleControlCommand('turn_off')"
              class="control-btn"
            >
              <Moon />
              关灯
            </ElButton>
          </div>
          <div class="control-item">
            <ElButton
              type="primary"
              size="large"
              :loading="controlLoading"
              :disabled="deviceInfo?.status !== 'online'"
              @click="handleControlCommand('dim')"
              class="control-btn"
            >
              <Battery />
              调光 {{ brightness }}%
            </ElButton>
            <div class="brightness-slider">
              <ElSlider
                v-model="brightness"
                :min="0"
                :max="100"
                :disabled="deviceInfo?.status !== 'online'"
                show-input
                size="small"
              />
            </div>
          </div>
          <div class="control-item">
            <ElButton
              type="warning"
              size="large"
              :loading="controlLoading"
              :disabled="deviceInfo?.status !== 'online'"
              @click="handleControlCommand('restart')"
              class="control-btn"
            >
              <Refresh />
              重启设备
            </ElButton>
          </div>
        </div>
      </ElCard>

      <div class="section-title">
        <h3>控制历史</h3>
      </div>

      <ElCard class="control-history-card">
        <ElTable :data="controlHistory" style="width: 100%" border>
          <ElTableColumn prop="command_label" label="指令" width="100" />
          <ElTableColumn prop="status_label" label="状态" width="100">
            <template #default="scope">
              <ElTag :type="scope.row.status === 'success' ? 'success' : 'danger'">
                {{ scope.row.status_label }}
              </ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="message" label="反馈信息" min-width="200" />
          <ElTableColumn prop="created_at" label="下发时间" width="180" />
          <ElTableColumn prop="executed_at" label="执行时间" width="180" />
        </ElTable>
      </ElCard>
    </div>
  </div>
</template>

<style scoped>
.device-detail-container {
  padding: 24px;
  min-height: 100vh;
  background-color: #1a1a2e;
}

.breadcrumb-bar {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
}

.breadcrumb {
  display: flex;
  align-items: center;
  font-size: 14px;
  color: #909399;
}

.breadcrumb .separator {
  margin: 0 8px;
}

.content-area {
  max-width: 1400px;
}

.device-info-card {
  margin-bottom: 24px;
  background: rgba(30, 30, 50, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.device-title {
  font-size: 24px;
  font-weight: 600;
  color: #ffffff;
  margin: 0;
}

.health-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.health-value {
  font-size: 18px;
  font-weight: 700;
  min-width: 40px;
}

.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-title h3 {
  font-size: 18px;
  font-weight: 600;
  color: #ffffff;
  margin: 0;
}

.update-time {
  font-size: 12px;
  color: #909399;
}

.telemetry-grid {
  margin-bottom: 24px;
}

.telemetry-card {
  height: 100%;
  text-align: center;
  transition: all 0.3s ease;
  background: rgba(30, 30, 50, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.telemetry-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.telemetry-icon {
  width: 48px;
  height: 48px;
  margin: 0 auto 12px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.light-card .telemetry-icon {
  background-color: rgba(230, 162, 60, 0.2);
  color: #e6a23c;
}

.temp-card .telemetry-icon {
  background-color: rgba(245, 108, 108, 0.2);
  color: #f56c6c;
}

.humidity-card .telemetry-icon {
  background-color: rgba(64, 158, 255, 0.2);
  color: #409eff;
}

.pir-card .telemetry-icon {
  background-color: rgba(103, 194, 58, 0.2);
  color: #67c23a;
}

.telemetry-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.telemetry-value {
  font-size: 32px;
  font-weight: 700;
  color: #ffffff;
}

.telemetry-value .unit {
  font-size: 14px;
  font-weight: 400;
  color: #909399;
  margin-left: 4px;
}

.time-range-group {
  background-color: rgba(30, 30, 50, 0.8);
  padding: 4px;
  border-radius: 4px;
}

.chart-card {
  padding: 24px;
  background: rgba(30, 30, 50, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.chart-container {
 width: 100%;
 height: 400px;
}

.control-card {
 padding: 24px;
 background: rgba(30, 30, 50, 0.8);
 border: 1px solid rgba(255, 255, 255, 0.05);
 margin-bottom: 24px;
}

.control-grid {
 display: grid;
 grid-template-columns: repeat(4, 1fr);
 gap: 20px;
}

.control-item {
 display: flex;
 flex-direction: column;
 align-items: center;
 gap: 12px;
}

.control-btn {
 width: 100%;
 height: 60px;
 display: flex;
 align-items: center;
 justify-content: center;
 gap: 8px;
 font-size: 16px;
}

.brightness-slider {
 width: 100%;
}

.control-history-card {
 padding: 24px;
 background: rgba(30, 30, 50, 0.8);
 border: 1px solid rgba(255, 255, 255, 0.05);
}

@media (max-width: 1200px) {
  .telemetry-grid :deep(.el-col) {
    flex: 0 0 50%;
    max-width: 50%;
  }
}

@media (max-width: 768px) {
  .device-detail-container {
    padding: 12px;
  }

  .breadcrumb-bar {
    flex-wrap: wrap;
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

  .chart-container {
 height: 300px;
 }

 .control-grid {
 grid-template-columns: repeat(2, 1fr);
 }
}

@media (max-width: 480px) {
 .control-grid {
 grid-template-columns: 1fr;
 }
}
</style>