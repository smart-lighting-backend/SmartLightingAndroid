/**
 * api/device.js — 设备查询接口（供 DeviceList.vue / SmartCity3D.vue 使用）
 *
 * 与 api/devices.js 不同的返回格式：
 *   { code: 200, data: { list: [...], total } }
 *
 * 字段命名使用字符串形式（status: 'online'/'offline'），
 * 对齐 DeviceList.vue 和 SmartCity3D.vue 的消费方式。
 */

const MOCK_DEVICES = [
  { id: 'SL-001', name: '南门-01',     region: 'A区', status: 'online',  healthScore: 98, lastHeartbeat: '2026-07-03 09:18:06' },
  { id: 'SL-002', name: '东门-02',     region: 'A区', status: 'online',  healthScore: 85, lastHeartbeat: '2026-07-03 09:17:30' },
  { id: 'SL-003', name: '创业大道-01', region: 'B区', status: 'offline', healthScore: 32, lastHeartbeat: '2026-07-01 22:10:00' },
  { id: 'SL-004', name: '人民广场-01', region: 'C区', status: 'online',  healthScore: 78, lastHeartbeat: '2026-07-03 09:15:00' },
  { id: 'SL-005', name: '工业园-01',   region: 'D区', status: 'online',  healthScore: 88, lastHeartbeat: '2026-07-03 09:16:00' },
  { id: 'SL-006', name: '学院路-01',   region: 'E区', status: 'online',  healthScore: 95, lastHeartbeat: '2026-07-03 09:14:00' },
]

export function fetchDeviceList(params = {}) {
  return new Promise((resolve) => {
    setTimeout(() => {
      let result = [...MOCK_DEVICES]

      if (params.keyword) {
        const kw = params.keyword.toLowerCase()
        result = result.filter(d =>
          d.id.toLowerCase().includes(kw) ||
          d.name.toLowerCase().includes(kw) ||
          d.region.toLowerCase().includes(kw)
        )
      }

      resolve({
        code: 200,
        message: 'success',
        data: {
          list: result,
          total: result.length,
        },
      })
    }, 300)
  })
}
