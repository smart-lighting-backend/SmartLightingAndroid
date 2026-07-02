import axios from 'axios';

export function sendControlCommand(deviceId, command, params = {}) {
  return new Promise((resolve) => {
    setTimeout(() => {
      const commands = {
        turn_on: {
          success: true,
          message: '开灯指令已下发',
          feedback: {
            status: 'success',
            message: '设备已响应，灯光已开启',
            executed_at: new Date().toLocaleString('zh-CN')
          }
        },
        turn_off: {
          success: true,
          message: '关灯指令已下发',
          feedback: {
            status: 'success',
            message: '设备已响应，灯光已关闭',
            executed_at: new Date().toLocaleString('zh-CN')
          }
        },
        dim: {
          success: true,
          message: '调光指令已下发',
          feedback: {
            status: 'success',
            message: `设备已响应，亮度已调整为 ${params.brightness}%`,
            executed_at: new Date().toLocaleString('zh-CN')
          }
        },
        flash: {
          success: true,
          message: '闪烁指令已下发',
          feedback: {
            status: 'success',
            message: '设备已响应，灯光开始闪烁',
            executed_at: new Date().toLocaleString('zh-CN')
          }
        },
        restart: {
          success: true,
          message: '重启指令已下发',
          feedback: {
            status: 'success',
            message: '设备已响应，正在重启...',
            executed_at: new Date().toLocaleString('zh-CN')
          }
        }
      };

      const result = commands[command];
      if (result) {
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
      const history = [
        {
          id: 'CTL001',
          device_id: deviceId,
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
          device_id: deviceId,
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
          device_id: deviceId,
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
          device_id: deviceId,
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
          device_id: deviceId,
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

      const total = history.length;
      const start = (page - 1) * pageSize;
      const end = start + pageSize;

      resolve({
        code: 200,
        message: 'success',
        data: {
          list: history.slice(start, end),
          total,
          page,
          pageSize
        }
      });
    }, 500);
  });
}