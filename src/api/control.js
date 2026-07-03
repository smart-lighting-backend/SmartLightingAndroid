import axios from 'axios';

// ── 模块级可变的模拟历史记录 ─────────────────────────────────────────
let historyIdCounter = 6;
const commandLabels = {
  turn_on:  '开灯',
  turn_off: '关灯',
  dim:      '调光',
  flash:    '闪烁',
  restart:  '重启',
};

const mockHistory = [
  {
    id: 'CTL001',
    device_id: 'SL-001',
    command: 'turn_on',
    command_label: '开灯',
    params: {},
    status: 'success',
    status_label: '执行成功',
    message: '设备已响应，灯光已开启',
    created_at: '2026-07-02 14:30:00',
    executed_at: '2026-07-02 14:30:05'
  },
  {
    id: 'CTL002',
    device_id: 'SL-001',
    command: 'dim',
    command_label: '调光',
    params: { brightness: 75 },
    status: 'success',
    status_label: '执行成功',
    message: '设备已响应，亮度已调整为 75%',
    created_at: '2026-07-02 14:25:00',
    executed_at: '2026-07-02 14:25:03'
  },
  {
    id: 'CTL003',
    device_id: 'SL-001',
    command: 'turn_off',
    command_label: '关灯',
    params: {},
    status: 'success',
    status_label: '执行成功',
    message: '设备已响应，灯光已关闭',
    created_at: '2026-07-02 10:00:00',
    executed_at: '2026-07-02 10:00:04'
  },
  {
    id: 'CTL004',
    device_id: 'SL-001',
    command: 'restart',
    command_label: '重启',
    params: {},
    status: 'success',
    status_label: '执行成功',
    message: '设备已响应，正在重启...',
    created_at: '2026-07-01 18:00:00',
    executed_at: '2026-07-01 18:00:06'
  },
  {
    id: 'CTL005',
    device_id: 'SL-001',
    command: 'flash',
    command_label: '闪烁',
    params: {},
    status: 'success',
    status_label: '执行成功',
    message: '设备已响应，灯光开始闪烁',
    created_at: '2026-07-01 16:30:00',
    executed_at: '2026-07-01 16:30:02'
  }
];

export function sendControlCommand(deviceId, command, params = {}) {
  return new Promise((resolve) => {
    setTimeout(() => {
      const commands = {
        turn_on:  { success: true, message: '开灯指令已下发', feedback: { status: 'success', message: '设备已响应，灯光已开启', executed_at: new Date().toLocaleString('zh-CN') } },
        turn_off: { success: true, message: '关灯指令已下发', feedback: { status: 'success', message: '设备已响应，灯光已关闭', executed_at: new Date().toLocaleString('zh-CN') } },
        dim:      { success: true, message: '调光指令已下发', feedback: { status: 'success', message: `设备已响应，亮度已调整为 ${params.brightness}%`, executed_at: new Date().toLocaleString('zh-CN') } },
        flash:    { success: true, message: '闪烁指令已下发', feedback: { status: 'success', message: '设备已响应，灯光开始闪烁', executed_at: new Date().toLocaleString('zh-CN') } },
        restart:  { success: true, message: '重启指令已下发', feedback: { status: 'success', message: '设备已响应，正在重启...', executed_at: new Date().toLocaleString('zh-CN') } },
      };

      const result = commands[command];
      if (result) {
        // 将新指令实时追加到历史记录头部
        const now = new Date();
        const pad = (n) => String(n).padStart(2, '0');
        const ts = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;
        mockHistory.unshift({
          id: `CTL${pad(historyIdCounter++).padStart(3, '0')}`,
          device_id: deviceId,
          command,
          command_label: commandLabels[command] || command,
          params: { ...params },
          status: 'success',
          status_label: '执行成功',
          message: result.feedback.message,
          created_at: ts,
          executed_at: result.feedback.executed_at,
        });

        resolve({
          code: 200,
          message: result.message,
          data: {
            deviceId,
            command,
            params,
            feedback: result.feedback
          }
        });
      } else {
        resolve({
          code: 400,
          message: '未知指令',
          data: null
        });
      }
    }, 1500);
  });
}

export function getControlHistory(deviceId, page = 1, pageSize = 10) {
  return new Promise((resolve) => {
    setTimeout(() => {
      // 按设备 ID 过滤
      const filtered = mockHistory.filter(h => h.device_id === deviceId);
      const total = filtered.length;
      const start = (page - 1) * pageSize;
      const end = start + pageSize;

      resolve({
        code: 200,
        message: 'success',
        data: {
          list: filtered.slice(start, end),
          total,
          page,
          pageSize
        }
      });
    }, 500);
  });
}