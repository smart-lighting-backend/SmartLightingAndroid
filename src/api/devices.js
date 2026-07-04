/**
 * api/devices.js — 设备管理相关接口
 * 对齐接口文档：开发变更日志 v1.0.0 三、设备管理模块 + 设备管理CRUD接口开发日志
 *
 * 接口列表:
 *   POST   /api/devices/list            设备组合查询（分页，8个筛选条件）
 *   GET    /api/devices/{deviceId}      查询设备详情
 *   POST   /api/devices                 新增设备
 *   PUT    /api/devices/{deviceId}      修改设备
 *   DELETE /api/devices/{deviceId}      删除设备（逻辑删除）
 *   GET    /api/telemetry/latest/{deviceId}   最新遥测数据
 *   POST   /api/telemetry/history             历史遥测数据
 *   GET    /api/devices/statistics/status     状态统计
 *   GET    /api/devices/statistics/area-status 区域状态统计
 *   POST   /api/devices/{deviceId}/control   手动控制
 *
 * 设备状态: 0 停用 | 1 在线 | 2 离线 | 3 异常
 */
import request from './request.js'
import { reportMock } from '../utils/mockStore.js'

// ── 状态映射工具 ───────────────────────────────────────────────────────────
export const STATUS_MAP = {
  0: { label: '停用', cls: 'disabled', color: '#888' },
  1: { label: '在线', cls: 'online',   color: '#4caf50' },
  2: { label: '离线', cls: 'offline',  color: '#9e9e9e' },
  3: { label: '异常', cls: 'warning',  color: '#ffa726' },
}
export const STATUS_QUERY_MAP = { '全部': undefined, '在线': 1, '离线': 2, '异常': 3, '停用': 0 }

// ── Mock 数据 ──────────────────────────────────────────────────────────────
const MOCK_DEVICES = [
  { id: 1, deviceId: 'SL-001', name: '南门-01',     area: 'A区', location: '106.5622,29.5621', status: 1, healthScore: 98.50, topicPrefix: 'streetlight', lastHeartbeatAt: '2026-07-02T09:18:06', enabled: true, deleted: false },
  { id: 2, deviceId: 'SL-002', name: '东门-02',     area: 'A区', location: '106.5630,29.5630', status: 1, healthScore: 85.00, topicPrefix: 'streetlight', lastHeartbeatAt: '2026-07-02T09:17:30', enabled: true, deleted: false },
  { id: 3, deviceId: 'SL-003', name: '创业大道-01', area: 'B区', location: '106.5700,29.5700', status: 2, healthScore: 32.00, topicPrefix: 'streetlight', lastHeartbeatAt: '2026-07-01T22:10:00', enabled: true, deleted: false },
  { id: 4, deviceId: 'SL-004', name: '人民广场-01', area: 'C区', location: '106.5660,29.5660', status: 1, healthScore: 78.00, topicPrefix: 'streetlight', lastHeartbeatAt: '2026-07-02T09:15:00', enabled: true, deleted: false },
  { id: 5, deviceId: 'SL-005', name: '工业园-01',   area: 'D区', location: '106.5800,29.5800', status: 1, healthScore: 88.00, topicPrefix: 'streetlight', lastHeartbeatAt: '2026-07-02T09:16:00', enabled: true, deleted: false },
  { id: 6, deviceId: 'SL-006', name: '学院路-01',   area: 'E区', location: '106.5900,29.5900', status: 1, healthScore: 95.00, topicPrefix: 'streetlight', lastHeartbeatAt: '2026-07-02T09:14:00', enabled: true, deleted: false },
]

const MOCK_TELEMETRY = {
  'SL-001': { id: 1, deviceId: 'SL-001', illuminance: 42.5, temperature: 37.3, humidity: 69, pm25: 18, aqi: 45, pir: 1, trafficFlow: 128, collectedAt: '2026-07-02T09:18:00' },
  'SL-004': { id: 2, deviceId: 'SL-004', illuminance: 85.0, temperature: 35.0, humidity: 60, pm25: 30, aqi: 80, pir: 0, trafficFlow: 56,  collectedAt: '2026-07-02T09:15:00' },
}

async function safeCall(apiFn, mockData, endpoint) {
  try {
    return await apiFn()
  } catch (e) {
    // 业务错误（后端返回 code != 200）继续抛出，其他错误（网络/404/500）降级 Mock
    if (e?.bizCode) throw e
    if (endpoint) reportMock(endpoint)
    return { code: 200, msg: 'mock', data: mockData }
  }
}

// ── 设备组合查询（分页）POST /api/devices/list ────────────────────────────
/**
 * 接口文档：POST /api/devices/list
 * 支持 8 个筛选条件:
 *   deviceId 精确 | name LIKE | area 精确 | location LIKE
 *   status 精确 | enabled 精确 | healthScoreMin >= | healthScoreMax <=
 *
 * @param {{
 *   pageNum?: number,
 *   pageSize?: number,
 *   keyword?: string,
 *   deviceId?: string,
 *   name?: string,
 *   area?: string,
 *   location?: string,
 *   status?: number,
 *   enabled?: boolean,
 *   healthScoreMin?: number,
 *   healthScoreMax?: number
 * }} params
 */
export function fetchDevicePage(params = {}) {
  const body = {
    pageNum:  params.pageNum  || 1,
    pageSize: params.pageSize || 10,
  }
  // keyword 同时匹配 deviceId/name/location（前端传 keyword，后端对应 name LIKE）
  if (params.keyword)           body.name          = params.keyword
  if (params.deviceId)          body.deviceId      = params.deviceId
  if (params.name)              body.name          = params.name
  if (params.area)              body.area          = params.area
  if (params.location)          body.location      = params.location
  if (params.status !== undefined && params.status !== null) body.status = params.status
  if (params.enabled !== undefined && params.enabled !== null) body.enabled = params.enabled
  if (params.healthScoreMin !== undefined) body.healthScoreMin = params.healthScoreMin
  if (params.healthScoreMax !== undefined) body.healthScoreMax = params.healthScoreMax

  // Mock 数据客户端过滤
  let list = MOCK_DEVICES.filter(d => !d.deleted)
  if (params.keyword) {
    const kw = params.keyword.toLowerCase()
    list = list.filter(d =>
      d.deviceId.toLowerCase().includes(kw) ||
      d.name.toLowerCase().includes(kw) ||
      d.location.toLowerCase().includes(kw)
    )
  }
  if (body.area)    list = list.filter(d => d.area === body.area)
  if (body.status !== undefined) list = list.filter(d => d.status === body.status)
  if (body.enabled !== undefined) list = list.filter(d => d.enabled === body.enabled)

  const total   = list.length
  const size    = body.pageSize
  const current = body.pageNum
  const start   = (current - 1) * size
  const paged   = list.slice(start, start + size)

  return safeCall(
    () => request.post('/api/devices/list', body),
    { records: paged, total, size, current, pages: Math.ceil(total / size) },
    'POST /api/devices/list'
  )
}

// ── 兼容旧接口：fetchDeviceList（某些页面可能仍使用） ─────────────────────
/**
 * 轻量版列表查询，供 Dashboard 等简单场景使用
 * @param {{ area?: string, status?: number }} params
 */
export async function fetchDeviceList(params = {}) {
  const body = {}
  if (params.area !== undefined && params.area !== null)   body.area   = params.area
  if (params.status !== undefined && params.status !== null) body.status = params.status

  let mockList = MOCK_DEVICES.filter(d => !d.deleted)
  if (body.area)   mockList = mockList.filter(d => d.area   === body.area)
  if (body.status !== undefined) mockList = mockList.filter(d => d.status === body.status)

  const res = await safeCall(
    () => request.post('/api/devices/list', { pageNum: 1, pageSize: 100, ...body }),
    mockList,
    'POST /api/devices/list (light)'
  )
  // 兼容后端分页格式 { records: [...] } 和直接返回数组
  if (res?.data?.records) return res.data.records
  if (Array.isArray(res)) return res
  if (Array.isArray(res?.data)) return res.data
  return res || []
}

// ── 设备详情 GET /api/devices/{deviceId} ─────────────────────────────────
/**
 * @param {string} deviceId  例如 'SL-001'
 */
export function fetchDeviceDetail(deviceId) {
  return safeCall(
    () => request.get(`/api/devices/${deviceId}`),
    MOCK_DEVICES.find(d => d.deviceId === deviceId) || MOCK_DEVICES[0],
    `GET /api/devices/${deviceId}`
  )
}

// ── 新增设备 POST /api/devices ────────────────────────────────────────────
/**
 * @param {{
 *   deviceId: string,
 *   name?: string,
 *   area?: string,
 *   location?: string,
 *   status?: number,
 *   healthScore?: number,
 *   topicPrefix?: string,
 *   enabled?: boolean
 * }} data
 */
export function createDevice(data) {
  return safeCall(
    () => request.post('/api/devices', data),
    { id: Date.now(), deleted: false, status: 1, healthScore: 100.00, topicPrefix: 'streetlight', enabled: true, ...data },
    'POST /api/devices'
  )
}

// ── 修改设备 PUT /api/devices/{deviceId} ─────────────────────────────────
/**
 * @param {string} deviceId
 * @param {{
 *   name?: string,
 *   area?: string,
 *   location?: string,
 *   status?: number,
 *   healthScore?: number,
 *   topicPrefix?: string,
 *   enabled?: boolean
 * }} data
 */
export function updateDevice(deviceId, data) {
  return safeCall(
    () => request.put(`/api/devices/${deviceId}`, data),
    { ...(MOCK_DEVICES.find(d => d.deviceId === deviceId) || {}), ...data },
    `PUT /api/devices/${deviceId}`
  )
}

// ── 删除设备（逻辑删除）DELETE /api/devices/{deviceId} ───────────────────
/**
 * @param {string} deviceId
 */
export function deleteDevice(deviceId) {
  return safeCall(
    () => request.delete(`/api/devices/${deviceId}`),
    null,
    `DELETE /api/devices/${deviceId}`
  )
}

// ── 最新遥测数据 GET /api/telemetry/latest/{deviceId} ────────────────────
/**
 * @param {string} deviceId
 */
export function fetchLatestTelemetry(deviceId) {
  return safeCall(
    () => request.get(`/api/telemetry/latest/${deviceId}`),
    MOCK_TELEMETRY[deviceId] || {      deviceId,
      illuminance: Math.round(Math.random() * 500 + 10),
      temperature: +(Math.random() * 20 + 25).toFixed(1),
      humidity:    Math.round(Math.random() * 50 + 30),
      pm25:        Math.round(Math.random() * 80 + 5),
      aqi:         Math.round(Math.random() * 100 + 10),
      pir:         Math.round(Math.random()),
      trafficFlow: Math.round(Math.random() * 200),
      collectedAt: new Date().toISOString(),
    },
    `GET /api/telemetry/latest/${deviceId}`
  )
}

// ── 历史遥测数据 POST /api/telemetry/history ─────────────────────────────
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
    mock,
    'POST /api/telemetry/history'
  )
}

// ── 状态统计 GET /api/devices/statistics/status ───────────────────────────
export function fetchStatusStatistics(area) {
  const params = area ? { area } : {}
  return safeCall(
    () => request.get('/api/devices/statistics/status', { params }),
    [{ status: 1, count: 4 }, { status: 2, count: 1 }, { status: 3, count: 1 }],
    'GET /api/devices/statistics/status'
  )
}

// ── 区域状态统计 GET /api/devices/statistics/area-status ──────────────────
export function fetchAreaStatusStatistics() {
  return safeCall(
    () => request.get('/api/devices/statistics/area-status'),
    [
      { area: 'A区', status: 1, count: 2 },
      { area: 'B区', status: 2, count: 1 },
      { area: 'C区', status: 1, count: 1 },
      { area: 'D区', status: 1, count: 1 },
      { area: 'E区', status: 1, count: 1 },
    ],
    'GET /api/devices/statistics/area-status'
  )
}

// ── 手动控制设备 POST /api/devices/{deviceId}/control ─────────────────────
/**
 * @param {string} deviceId
 * @param {{ action: 'ON'|'OFF'|'DIMMING', brightness?: number }} payload
 */
export function controlDevice(deviceId, payload) {
  return safeCall(
    () => request.post(`/api/devices/${deviceId}/control`, payload),
    { code: 200, msg: 'mock', data: { id: Date.now(), deviceId, ...payload, status: 'SENT', source: 'MANUAL', issuedAt: new Date().toISOString() } },
    `POST /api/devices/${deviceId}/control`
  )
}

// ── 节点列表（手动控制弹窗用） ─────────────────────────────────────────────
export async function fetchDeviceNodes() {
  const mockNodes = MOCK_DEVICES.map(d => ({
    deviceId: d.deviceId,
    name:     d.name,
    location: d.area + ' ' + d.location,
    status:   d.status,
  }))
  const res = await safeCall(
    () => request.post('/api/devices/list', { pageNum: 1, pageSize: 100 }),
    mockNodes,
    'POST /api/devices/list (nodes)'
  )
  // 兼容后端分页格式 { records: [...] } 和直接返回数组
  if (res?.data?.records) return res.data.records
  if (Array.isArray(res)) return res
  if (Array.isArray(res?.data)) return res.data
  return res || []
}

