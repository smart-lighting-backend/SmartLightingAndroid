import axios from 'axios';

const mockAlarms = [
  {
    id: 'ALM001',
    device_id: 'DEV001',
    device_name: '高新南区-主干道路灯',
    region: '高新南区',
    type: 'offline',
    type_label: '设备离线',
    level: 'high',
    level_label: '严重',
    message: '设备心跳超时，已超过10分钟未上报数据',
    status: 'unhandled',
    status_label: '未处理',
    created_at: '2026-07-02 14:32:15',
    updated_at: '2026-07-02 14:32:15'
  },
  {
    id: 'ALM002',
    device_id: 'DEV003',
    device_name: '科技园-道路照明03',
    region: '科技园',
    type: 'health',
    type_label: '健康异常',
    level: 'medium',
    level_label: '中等',
    message: '设备健康评分低于60分，当前评分58分',
    status: 'unhandled',
    status_label: '未处理',
    created_at: '2026-07-02 13:45:30',
    updated_at: '2026-07-02 13:45:30'
  },
  {
    id: 'ALM003',
    device_id: 'DEV005',
    device_name: '软件园-支路路灯',
    region: '软件园',
    type: 'temperature',
    type_label: '温度异常',
    level: 'medium',
    level_label: '中等',
    message: '设备芯片温度过高，当前温度48°C',
    status: 'processing',
    status_label: '处理中',
    created_at: '2026-07-02 12:20:00',
    updated_at: '2026-07-02 12:35:00'
  },
  {
    id: 'ALM004',
    device_id: 'DEV007',
    device_name: '创业路-主干道灯',
    region: '创业路',
    type: 'offline',
    type_label: '设备离线',
    level: 'high',
    level_label: '严重',
    message: '设备心跳超时，已超过15分钟未上报数据',
    status: 'handled',
    status_label: '已处理',
    created_at: '2026-07-02 10:15:22',
    updated_at: '2026-07-02 11:00:00'
  },
  {
    id: 'ALM005',
    device_id: 'DEV002',
    device_name: '天府大道-照明02',
    region: '天府大道',
    type: 'power',
    type_label: '供电异常',
    level: 'low',
    level_label: '轻微',
    message: '设备供电电压偏低，当前电压198V',
    status: 'handled',
    status_label: '已处理',
    created_at: '2026-07-01 18:30:45',
    updated_at: '2026-07-01 19:15:00'
  },
  {
    id: 'ALM006',
    device_id: 'DEV004',
    device_name: '金融城-景观灯',
    region: '金融城',
    type: 'humidity',
    type_label: '湿度异常',
    level: 'low',
    level_label: '轻微',
    message: '设备内部湿度偏高，当前湿度82%',
    status: 'handled',
    status_label: '已处理',
    created_at: '2026-07-01 16:20:00',
    updated_at: '2026-07-01 17:00:00'
  },
  {
    id: 'ALM007',
    device_id: 'DEV006',
    device_name: '武侯区-次要道路灯',
    region: '武侯区',
    type: 'offline',
    type_label: '设备离线',
    level: 'high',
    level_label: '严重',
    message: '设备心跳超时，已超过20分钟未上报数据',
    status: 'unhandled',
    status_label: '未处理',
    created_at: '2026-07-02 09:45:10',
    updated_at: '2026-07-02 09:45:10'
  },
  {
    id: 'ALM008',
    device_id: 'DEV008',
    device_name: '锦江区-步行街灯',
    region: '锦江区',
    type: 'health',
    type_label: '健康异常',
    level: 'medium',
    level_label: '中等',
    message: '设备健康评分下降明显，当前评分65分',
    status: 'processing',
    status_label: '处理中',
    created_at: '2026-07-02 08:30:00',
    updated_at: '2026-07-02 08:45:00'
  }
];

const alarmTypes = [
  { value: 'offline', label: '设备离线' },
  { value: 'health', label: '健康异常' },
  { value: 'temperature', label: '温度异常' },
  { value: 'humidity', label: '湿度异常' },
  { value: 'power', label: '供电异常' },
  { value: 'illuminance', label: '光照异常' }
];

const alarmLevels = [
  { value: 'high', label: '严重' },
  { value: 'medium', label: '中等' },
  { value: 'low', label: '轻微' }
];

const alarmStatuses = [
  { value: 'unhandled', label: '未处理' },
  { value: 'processing', label: '处理中' },
  { value: 'handled', label: '已处理' }
];

export function fetchAlarmList(params = {}) {
  return new Promise((resolve) => {
    setTimeout(() => {
      let result = [...mockAlarms];

      if (params.deviceId) {
        result = result.filter(item => 
          item.device_id.toLowerCase().includes(params.deviceId.toLowerCase()) ||
          item.device_name.toLowerCase().includes(params.deviceId.toLowerCase())
        );
      }

      if (params.type) {
        result = result.filter(item => item.type === params.type);
      }

      if (params.level) {
        result = result.filter(item => item.level === params.level);
      }

      if (params.status) {
        result = result.filter(item => item.status === params.status);
      }

      if (params.startTime) {
        result = result.filter(item => item.created_at >= params.startTime);
      }

      if (params.endTime) {
        result = result.filter(item => item.created_at <= params.endTime);
      }

      const page = params.page || 1;
      const pageSize = params.pageSize || 10;
      const total = result.length;
      const start = (page - 1) * pageSize;
      const end = start + pageSize;

      resolve({
        code: 200,
        message: 'success',
        data: {
          list: result.slice(start, end),
          total,
          page,
          pageSize
        }
      });
    }, 500);
  });
}

export function fetchAlarmDetail(alarmId) {
  return new Promise((resolve) => {
    setTimeout(() => {
      const alarm = mockAlarms.find(item => item.id === alarmId);
      if (alarm) {
        resolve({
          code: 200,
          message: 'success',
          data: {
            ...alarm,
            device_info: {
              id: alarm.device_id,
              name: alarm.device_name,
              region: alarm.region,
              status: alarm.type === 'offline' ? 0 : 1,
              health_score: alarm.type === 'health' ? 58 : 85,
              install_time: '2024-03-15',
              firmware_version: 'v2.1.0',
              ip_address: '192.168.1.101',
              coordinates: '30.5728, 104.0668'
            },
            handle_log: [
              {
                time: alarm.created_at,
                action: '告警生成',
                operator: '系统',
                remark: '设备心跳超时，自动触发告警'
              },
              ...(alarm.status !== 'unhandled' ? [{
                time: alarm.status === 'processing' ? alarm.updated_at : '2026-07-02 10:00:00',
                action: alarm.status === 'processing' ? '开始处理' : '处理完成',
                operator: '管理员',
                remark: alarm.status === 'processing' ? '已派单给维护人员' : '设备已恢复正常'
              }] : [])
            ]
          }
        });
      } else {
        resolve({
          code: 404,
          message: '告警不存在',
          data: null
        });
      }
    }, 500);
  });
}

export function updateAlarmStatus(alarmId, status, remark = '') {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        code: 200,
        message: '操作成功',
        data: {
          alarmId,
          status,
          remark
        }
      });
    }, 500);
  });
}

export function getAlarmTypes() {
  return alarmTypes;
}

export function getAlarmLevels() {
  return alarmLevels;
}

export function getAlarmStatuses() {
  return alarmStatuses;
}