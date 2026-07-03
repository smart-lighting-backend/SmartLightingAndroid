/**
 * api/devices.js — 设备管理相关接口
 * 对齐接口文档 V2.0
 *
 * 设备状态约定（后端整数）:
 *   0 停用 | 1 在线 | 2 离线 | 3 异常
 *
 * 接口列表：
 *   GET  /api/devices/list                   设备台账列表（数组，支持 area/status 过滤）
 *   GET  /api/devices/{deviceId}             单设备详情（按业务编号）
 *   GET  /api/devices/page                   分页查询（管理端）
 *   POST /api/devices                        新增设备
 *   PUT  /api/devices/{id}                   修改设备
 *   DELETE /api/devices/{id}                 删除设备（逻辑删除）
 *   GET  /api/telemetry/latest/{deviceId}    最新遥测数据
 *   POST /api/telemetry/history              历史遥测数据
 *   GET  /api/devices/statistics/status      状态统计
 *   GET  /api/devices/statistics/area-status 区域状态统计
 *   POST /api/devices/{deviceId}/control     手动控制（ON / OFF / DIMMING）
 */
import request from './request.js'

// ── 状态映射工具 ───────────────────────────────────────────────────────────
// 后端返回整数，前端展示时使用此映射
export const STATUS_MAP = {
  0: { label: '停用',  cls: 'disabled', color: '#888' },
  1: { label: '在线',  cls: 'online',   color: '#4caf50' },
  2: { label: '离线',  cls: 'offline',  color: '#9e9e9e' },
  3: { label: '异常',  cls: 'warning',  color: '#ffa726' },
}
export const STATUS_QUERY_MAP = { '全部': undefined, '在线': 1, '离线': 2, '异常': 3, '停用': 0 }

// ── Mock 降级（后端不可达时） ──────────────────────────────────────────────
const MOCK_DEVICES = [
  { id: 1, deviceId: 'SL-001', name: '南门-01',       area: 'A区', location: '106.5622,29.5621', status: 1, healthScore: 98.50, topicPrefix: 'streetlight', lastHeartbeatAt: '2026-07-02T09:18:06', enabled: true, deleted: false },
  { id: 2, deviceId: 'SL-002', name: '东门-02',       area: 'A区', location: '106.5630,29.5630', status: 1, healthScore: 85.00, topicPrefix: 'streetlight', lastHeartbeatAt: '2026-07-02T09:17:30', enabled: true, deleted: false },
  { id: 3, deviceId: 'SL-003', name: '创业大道-01',   area: 'B区', location: '106.5700,29.5700', status: 2, healthScore: 32.00, topicPrefix: 'streetlight', lastHeartbeatAt: '2026-07-01T22:10:00', enabled: true, deleted: false },
  { id: 4, deviceId: 'SL-004', name: '人民广场-01',   area: 'C区', location: '106.5660,29.5660', status: 1, healthScore: 78.00, topicPrefix: 'streetlight', lastHeartbeatAt: '2026-07-02T09:15:00', enabled: true, deleted: false },
  { id: 5, deviceId: 'SL-005', name: '工业园-01',     area: 'D区', location: '106.5800,29.5800', status: 1, healthScore: 88.00, topicPrefix: 'streetlight', lastHeartbeatAt: '2026-07-02T09:16:00', enabled: true, deleted: false },
  { id: 6, deviceId: 'SL-006', name: '学院路-01',     area: 'E区', location: '106.5900,29.5900', status: 1, healthScore: 95.00, topicPrefix: 'streetlight', lastHeartbeatAt: '2026-07-02T09:14:00', enabled: true, deleted: false },
  { id: 7, deviceId: 'SL-007', name: '滨湖大道-01',   area: 'F区', location: '106.6000,29.6000', status: 3, healthScore: 55.00, topicPrefix: 'streetlight', lastHeartbeatAt: '2026-07-02T08:50:00', enabled: true, deleted: false },
  { id: 8, deviceId: 'SL-008', name: '高铁站前-01',   area: 'G区', location: '106.6100,29.6100', status: 1, healthScore: 90.00, topicPrefix: 'streetlight', lastHeartbeatAt: '2026-07-02T09:10:00', enabled: true, deleted: false },
]

const MOCK_TELEMETRY = {
  'SL-001': { id: 1, deviceId: 'SL-001', illuminance: 42.5, temperature: 37.3, humidity: 69, pm25: 18, aqi: 45, pir: 1, trafficFlow: 128, collectedAt: '2026-07-02T09:18:00' },
  'SL-004': { id: 2, deviceId: 'SL-004', illuminance: 85.0, temperature: 35.0, humidity: 60, pm25: 30, aqi: 80, pir: 0, trafficFlow: 56,  collectedAt: '2026-07-02T09:15:00' },
}

async function safeCall(apiFn, mockData) {
  try {
    const result = await apiFn()
    // 如果后端返回空数据（空数组或空对象），也降级到 Mock
    const isEmpty = result === null || result === undefined || 
                   (Array.isArray(result) && result.length === 0) ||
                   (typeof result === 'object' && result.data !== undefined && Array.isArray(result.data) && result.data.length === 0)
    if (isEmpty) return { code: 200, msg: 'mock', data: mockData }
    return result
  } catch (e) {
    // 网络不可达 或 代理 502/503/504 时降级到 Mock；业务错误（code≠200）直接抛出
    const httpStatus = e?.response?.status
    const isNetworkErr = (!e?.response && !e?.bizCode) || (httpStatus != null && httpStatus >= 502 && httpStatus <= 504)
    if (isNetworkErr) return { code: 200, msg: 'mock', data: mockData }
    throw e
  }
}

// ── 设备列表（展示用，数组） ────────────────────────────────────────────────
/**
 * @param {{ area?: string, status?: number }} params
 * @returns {Promise<{ code, msg, data: Device[] }>}
 */
export function fetchDeviceList(params = {}) {
  const query = {}
  if (params.area)   query.area   = params.area
  if (params.status !== undefined && params.status !== null) query.status = params.status
  return safeCall(
    () => request.get('/api/devices/list', { params: query }),
    MOCK_DEVICES
  )
}

// ── 设备详情（按业务编号 deviceId） ────────────────────────────────────────
/**
 * @param {string} deviceId  例如 'SL-001'
 */
export function fetchDeviceDetail(deviceId) {
  return safeCall(
    () => request.get(`/api/devices/${deviceId}`),
    MOCK_DEVICES.find(d => d.deviceId === deviceId) || MOCK_DEVICES[0]
  )
}

// ── 最新遥测数据 ───────────────────────────────────────────────────────────
/**
 * @param {string} deviceId
 * @returns {Promise<{ code, msg, data: Telemetry }>}
 */
export function fetchLatestTelemetry(deviceId) {
  return safeCall(
    () => request.get(`/api/telemetry/latest/${deviceId}`),
    MOCK_TELEMETRY[deviceId] || {
      deviceId,
      illuminance: Math.round(Math.random() * 500 + 10),
      temperature: +(Math.random() * 20 + 25).toFixed(1),
      humidity:    Math.round(Math.random() * 50 + 30),
      pm25:        Math.round(Math.random() * 80 + 5),
      aqi:         Math.round(Math.random() * 100 + 10),
      pir:         Math.round(Math.random()),
      trafficFlow: Math.round(Math.random() * 200),
      collectedAt: new Date().toISOString(),
    }
  )
}

// ── 历史遥测数据（用于趋势图） ─────────────────────────────────────────────
/**
 * @param {{ deviceId: string, startTime: string, endTime: string }} params
 */
export function fetchTelemetryHistory(params) {
  const mock = Array.from({ length: 24 }, (_, i) => ({
    deviceId:    params.deviceId,
    illuminance: Math.round(Math.random() * 500 + 10),
    temperature: +(Math.random() * 20 + 25).toFixed(1),
    humidity:    Math.round(Math.random() * 50 + 30),
    pm25:        Math.round(Math.random() * 80 + 5),
    trafficFlow: Math.round(Math.random() * 200),
    collectedAt: new Date(Date.now() - (23 - i) * 3600000).toISOString(),
  }))
  return safeCall(
    () => request.post('/api/telemetry/history', params),
    mock
  )
}

// ── 设备分页查询（管理端） ─────────────────────────────────────────────────
/**
 * @param {{ pageNum?, pageSize?, keyword?, area?, status?, enabled? }} params
 */
export function fetchDevicePage(params = {}) {
  return safeCall(
    () => request.get('/api/devices/page', { params }),
    {
      records: MOCK_DEVICES,
      total: MOCK_DEVICES.length,
      size: params.pageSize || 10,
      current: params.pageNum || 1,
      pages: 1,
    }
  )
}

// ── 状态统计 ──────────────────────────────────────────────────────────────
export function fetchStatusStatistics(area) {
  const params = area ? { area } : {}
  return safeCall(
    () => request.get('/api/devices/statistics/status', { params }),
    [{ status: 1, count: 6 }, { status: 2, count: 1 }, { status: 3, count: 1 }]
  )
}

// ── 区域状态统计 ───────────────────────────────────────────────────────────
export function fetchAreaStatusStatistics() {
  return safeCall(
    () => request.get('/api/devices/statistics/area-status'),
    [
      { area: 'A区', status: 1, count: 2 },
      { area: 'B区', status: 2, count: 1 },
      { area: 'C区', status: 1, count: 1 },
      { area: 'D区', status: 1, count: 1 },
      { area: 'E区', status: 1, count: 1 },
      { area: 'F区', status: 3, count: 1 },
      { area: 'G区', status: 1, count: 1 },
    ]
  )
}

// ── 手动控制设备 ───────────────────────────────────────────────────────────
/**
 * @param {string} deviceId
 * @param {{ action: 'ON'|'OFF'|'DIMMING', brightness?: number }} payload
 */
export function controlDevice(deviceId, payload) {
  return safeCall(
    () => request.post(`/api/devices/${deviceId}/control`, payload),
    null
  )
}

// ── 新增设备 ──────────────────────────────────────────────────────────────
export function createDevice(data) {
  return safeCall(() => request.post('/api/devices', data), { id: Date.now(), ...data })
}

// ── 修改设备 ──────────────────────────────────────────────────────────────
export function updateDevice(id, data) {
  return safeCall(() => request.put(`/api/devices/${id}`, data), { id, ...data })
}

// ── 删除设备 ──────────────────────────────────────────────────────────────
export function deleteDevice(id) {
  return safeCall(() => request.delete(`/api/devices/${id}`), null)
}

// ── 节点列表（供手动控制弹窗使用） ────────────────────────────────────────
export function fetchDeviceNodes() {
  return safeCall(
    () => request.get('/api/devices/list'),
    MOCK_DEVICES.map(d => ({
      deviceId: d.deviceId,
      name: d.name,
      location: d.area + ' ' + d.location,
      status: d.status,
    }))
  )
}