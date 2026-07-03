/**
 * api/telemetry.js — 遥测数据接口
 *
 * 后端格式（TelemetryController）：
 *   GET /api/telemetry/latest/{deviceId}
 *     → data: { deviceId, data: { illuminance, temperature, ... }, lastHeartbeatAt }
 *   POST /api/telemetry/history
 *     → data: { records: [...], total, size, current, pages } (IPage)
 *
 * API 层负责将后端格式归一化为前端消费格式：
 *   fetchLatestTelemetry → data: { illuminance, temperature, humidity, pir, updateTime }
 *   fetchTelemetryHistory → data: { list: [...], total }
 */
import request from './request.js'
import { reportMock } from '../utils/mockStore.js'

// ── Mock 数据 ─────────────────────────────────────────────────────────────
const MOCK_LATEST = {
  'SL-001': { deviceId: 'SL-001', illuminance: 1256, temperature: 26.8, humidity: 45, pir: 1, updateTime: '2026-07-03 14:35:00' },
  'SL-002': { deviceId: 'SL-002', illuminance: 890,  temperature: 27.2, humidity: 42, pir: 0, updateTime: '2026-07-03 14:34:55' },
  'SL-003': { deviceId: 'SL-003', illuminance: 1520, temperature: 25.5, humidity: 48, pir: 1, updateTime: '2026-07-01 22:10:00' },
  'SL-004': { deviceId: 'SL-004', illuminance: 980,  temperature: 26.1, humidity: 44, pir: 0, updateTime: '2026-07-03 14:34:40' },
  'SL-005': { deviceId: 'SL-005', illuminance: 1100, temperature: 27.5, humidity: 40, pir: 1, updateTime: '2026-07-03 14:34:30' },
  'SL-006': { deviceId: 'SL-006', illuminance: 750,  temperature: 28.0, humidity: 38, pir: 0, updateTime: '2026-07-02 22:08:12' },
  'SL-007': { deviceId: 'SL-007', illuminance: 1350, temperature: 26.0, humidity: 46, pir: 1, updateTime: '2026-07-03 14:34:50' },
  'SL-008': { deviceId: 'SL-008', illuminance: 920,  temperature: 26.9, humidity: 43, pir: 0, updateTime: '2026-07-03 14:34:25' },
}

function genHistoryMock(deviceId, timeRange) {
  const now = new Date()
  const data = []
  const interval = timeRange === '24h' ? 3600000 : timeRange === '7d' ? 86400000 : 300000
  const count = timeRange === '24h' ? 24 : timeRange === '7d' ? 7 : 12

  for (let i = count - 1; i >= 0; i--) {
    const ts = new Date(now.getTime() - i * interval)
    const hour = ts.getHours()
    const base = 1000 + Math.random() * 600
    const lux = hour >= 6 && hour <= 18
      ? base + Math.sin((hour - 6) / 12 * Math.PI) * 800
      : base * 0.3 + Math.random() * 100

    const pad = (n) => String(n).padStart(2, '0')
    data.push({
      time: `${ts.getFullYear()}-${pad(ts.getMonth() + 1)}-${pad(ts.getDate())} ${pad(ts.getHours())}:${pad(ts.getMinutes())}:00`,
      illuminance: Math.round(lux),
      pir: Math.random() > 0.6 ? 1 : 0,
      temperature: Math.round((25 + Math.random() * 5) * 10) / 10,
      humidity: Math.round(40 + Math.random() * 10),
    })
  }
  return data
}

async function safeCall(apiFn, mockData, endpoint) {
  try {
    return await apiFn()
  } catch (e) {
    if (e?.bizCode) throw e
    if (endpoint) reportMock(endpoint)
    return { code: 200, msg: 'mock', data: mockData }
  }
}

// ── 最新遥测 GET /api/telemetry/latest/{deviceId} ────────────────────────
export function fetchLatestTelemetry(deviceId) {
  return safeCall(
    async () => {
      const res = await request.get(`/api/telemetry/latest/${deviceId}`)
      // 后端 data.data 为 latestData JSON 反序列化的遥测读数
      const tele = res.data?.data || {}
      return {
        code: 200,
        msg: 'success',
        data: {
          deviceId:   res.data?.deviceId || deviceId,
          illuminance: tele.illuminance ?? 0,
          temperature: tele.temperature ?? 0,
          humidity:    tele.humidity ?? 0,
          pir:         tele.pir ?? 0,
          updateTime:  res.data?.lastHeartbeatAt || tele.collectedAt || new Date().toISOString(),
        },
      }
    },
    MOCK_LATEST[deviceId] || { deviceId, illuminance: 0, temperature: 0, humidity: 0, pir: 0, updateTime: '--' },
    `GET /api/telemetry/latest/${deviceId}`
  )
}

// ── 历史遥测 POST /api/telemetry/history ─────────────────────────────────
export function fetchTelemetryHistory(params) {
  const { deviceId, timeRange = '1h' } = params
  return safeCall(
    async () => {
      const res = await request.post('/api/telemetry/history', { deviceId })
      // 后端返回 MyBatis-Plus IPage：{ records, total, size, current, pages }
      return {
        code: 200,
        msg: 'success',
        data: {
          list:  (res.data?.records || []).map(r => {
            let formattedTime = '--'
            if (Array.isArray(r.collectedAt) && r.collectedAt.length >= 6) {
              const [y, m, d, h, min, s] = r.collectedAt
              const pad = (n) => String(n).padStart(2, '0')
              formattedTime = `${y}-${pad(m)}-${pad(d)} ${pad(h)}:${pad(min)}:${pad(s)}`
            } else if (typeof r.collectedAt === 'string') {
              formattedTime = r.collectedAt.replace('T', ' ')
              if (formattedTime.indexOf('.') > -1) {
                formattedTime = formattedTime.substring(0, formattedTime.indexOf('.'))
              }
            }
            return {
              ...r,
              time: formattedTime
            }
          }),
          total: res.data?.total || 0,
        }
      }
    },
    { list: genHistoryMock(deviceId, timeRange), total: 24 },
    'POST /api/telemetry/history'
  )
}
