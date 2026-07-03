/**
 * api/warnings.js — 告警中心接口
 * 对齐接口文档：开发变更日志 v1.0.0 二、告警日志管理模块
 *
 * 接口列表:
 *   POST   /api/alarms/list            告警组合查询（分页）
 *   GET    /api/alarms/{id}            告警详情
 *   POST   /api/alarms                 新增告警
 *   PUT    /api/alarms/{id}            修改告警
 *   DELETE /api/alarms/{id}            删除告警
 *   PUT    /api/alarms/{id}/handle     处理确认
 *   GET    /api/alarms/stats           告警统计（按 level/type/status 分组）
 *   GET    /api/alarms/trend           告警趋势（按天）
 *   PUT    /api/alarms/batch/handle    批量处理
 *   DELETE /api/alarms/batch           批量删除
 *
 * 告警状态: ACTIVE | ACKNOWLEDGED | RECOVERED
 * 告警级别: CRITICAL | MAJOR | MINOR | WARNING | INFO
 * 告警类型: OFFLINE | FAULT | SECURITY | VISION
 */
import request from './request.js'
import { reportMock } from '../utils/mockStore.js'

// ── 状态/级别/类型映射 ──────────────────────────────────────────────────────
export const ALARM_STATUS_MAP = {
  ACTIVE:       { label: '待处理', cls: 'pending' },
  ACKNOWLEDGED: { label: '处理中', cls: 'processing' },
  RECOVERED:    { label: '已解决', cls: 'resolved' },
}

export const ALARM_LEVEL_MAP = {
  CRITICAL: { label: '紧急', cls: 'critical' },
  MAJOR:    { label: '严重', cls: 'critical' },
  WARNING:  { label: '警告', cls: 'warning' },
  MINOR:    { label: '提示', cls: 'info' },
  INFO:     { label: '提示', cls: 'info' },
}

export const ALARM_TYPE_MAP = {
  OFFLINE:  '离线',
  FAULT:    '故障',
  SECURITY: '安全',
  VISION:   '视觉',
}

// ── Mock 数据 ─────────────────────────────────────────────────────────────
const MOCK_ALARMS = [
  { id: 1, deviceId: 'SL-001', type: 'FAULT',    level: 'CRITICAL', status: 'ACTIVE',       reason: '电源模块输出电压异常，超出额定范围 ±15%',                    startAt: '2023-10-27T14:32:05', recoverAt: null,                  handler: null },
  { id: 2, deviceId: 'SL-003', type: 'OFFLINE',  level: 'MAJOR',    status: 'ACKNOWLEDGED', reason: '心跳中断超过 5 分钟，最后心跳时间：2023-10-27 12:10:00',      startAt: '2023-10-27T12:15:22', recoverAt: null,                  handler: '张工' },
  { id: 3, deviceId: 'SL-004', type: 'VISION',   level: 'MINOR',    status: 'RECOVERED',    reason: '摄像头视场被轻微遮挡，影像质量下降',                          startAt: '2023-10-26T18:40:11', recoverAt: '2023-10-26T19:00:00', handler: 'system' },
  { id: 4, deviceId: 'SL-005', type: 'SECURITY', level: 'CRITICAL', status: 'ACKNOWLEDGED', reason: '检测到设备门被非授权方式打开',                                startAt: '2023-10-26T03:12:55', recoverAt: null,                  handler: '安保组-王五' },
  { id: 5, deviceId: 'SL-006', type: 'FAULT',    level: 'WARNING',  status: 'RECOVERED',    reason: '驱动板温度持续超过阈值，触发降功率保护',                      startAt: '2023-10-25T22:08:33', recoverAt: '2023-10-25T23:00:00', handler: '李工' },
  { id: 6, deviceId: 'SL-007', type: 'OFFLINE',  level: 'MAJOR',    status: 'RECOVERED',    reason: '设备未上报心跳，判定为离线',                                  startAt: '2023-10-25T18:50:11', recoverAt: '2023-10-25T19:30:00', handler: 'system' },
  { id: 7, deviceId: 'SL-008', type: 'VISION',   level: 'INFO',     status: 'RECOVERED',    reason: '夜间巡检图像亮度异常，疑似灯具衰减',                          startAt: '2023-10-25T02:33:40', recoverAt: '2023-10-25T08:00:00', handler: '赵工' },
  { id: 8, deviceId: 'SL-001', type: 'SECURITY', level: 'CRITICAL', status: 'RECOVERED',    reason: '检测到可疑人员在设备周围长时间徘徊',                          startAt: '2023-10-24T23:15:18', recoverAt: '2023-10-24T23:45:00', handler: '安保组' },
]

async function safeCall(apiFn, mockData, endpoint) {
  try {
    return await apiFn()
  } catch (e) {
    if (e?.bizCode) throw e
    if (endpoint) reportMock(endpoint)
    return { code: 200, msg: 'mock', data: mockData }
  }
}

// ── 告警组合查询（分页） POST /api/alarms/list ────────────────────────────
/**
 * 接口文档：POST /api/alarms/list
 * 支持 7 个筛选条件（deviceId LIKE、type/level/status 精确、startAtFrom/startAtTo、handler LIKE）
 *
 * @param {{
 *   pageNum?: number,
 *   pageSize?: number,
 *   deviceId?: string,
 *   type?: string,
 *   level?: string,
 *   status?: string,
 *   startAtFrom?: string,
 *   startAtTo?: string,
 *   handler?: string
 * }} params
 */
export function fetchAlarmPage(params = {}) {
  const body = {
    pageNum:  params.pageNum  || 1,
    pageSize: params.pageSize || 10,
  }
  if (params.deviceId  && params.deviceId  !== 'ALL') body.deviceId  = params.deviceId
  if (params.type      && params.type      !== 'ALL') body.type      = params.type
  if (params.level     && params.level     !== 'ALL') body.level     = params.level
  if (params.status    && params.status    !== 'ALL') body.status    = params.status
  if (params.startTime || params.startAtFrom)         body.startAtFrom = params.startTime || params.startAtFrom
  if (params.endTime   || params.startAtTo)           body.startAtTo   = params.endTime   || params.startAtTo
  if (params.handler)                                 body.handler   = params.handler

  // Mock 数据客户端过滤
  let list = [...MOCK_ALARMS]
  if (body.deviceId)  list = list.filter(a => a.deviceId.includes(body.deviceId))
  if (body.type)      list = list.filter(a => a.type    === body.type)
  if (body.level)     list = list.filter(a => a.level   === body.level)
  if (body.status)    list = list.filter(a => a.status  === body.status)
  if (body.handler)   list = list.filter(a => a.handler && a.handler.includes(body.handler))

  const total   = list.length
  const size    = body.pageSize
  const current = body.pageNum
  const start   = (current - 1) * size
  const paged   = list.slice(start, start + size)

  return safeCall(
    () => request.post('/api/alarms/list', body),
    { records: paged, total, size, current, pages: Math.ceil(total / size) },
    'POST /api/alarms/list'
  )
}

// ── 告警详情 GET /api/alarms/{id} ─────────────────────────────────────────
export function fetchAlarmDetail(id) {
  return safeCall(
    () => request.get(`/api/alarms/${id}`),
    MOCK_ALARMS.find(a => a.id === id) || MOCK_ALARMS[0],
    `GET /api/alarms/${id}`
  )
}

// ── 新增告警 POST /api/alarms ─────────────────────────────────────────────
export function createAlarm(data) {
  return safeCall(
    () => request.post('/api/alarms', data),
    { id: Date.now(), ...data, startAt: new Date().toISOString() },
    'POST /api/alarms'
  )
}

// ── 修改告警 PUT /api/alarms/{id} ─────────────────────────────────────────
export function updateAlarm(id, data) {
  return safeCall(
    () => request.put(`/api/alarms/${id}`, data),
    { id, ...data },
    `PUT /api/alarms/${id}`
  )
}

// ── 删除告警 DELETE /api/alarms/{id} ─────────────────────────────────────
export function deleteAlarm(id) {
  return safeCall(
    () => request.delete(`/api/alarms/${id}`),
    null,
    `DELETE /api/alarms/${id}`
  )
}

// ── 处理确认 PUT /api/alarms/{id}/handle ─────────────────────────────────
/**
 * @param {number} id  告警记录主键
 * @param {{ handler: string, remark?: string }} data
 */
export function handleAlarm(id, data) {
  return safeCall(
    () => request.put(`/api/alarms/${id}/handle`, data),
    { id, status: 'ACKNOWLEDGED', handler: data.handler, recoverAt: new Date().toISOString() },
    `PUT /api/alarms/${id}/handle`
  )
}

// ── 告警统计 GET /api/alarms/stats ───────────────────────────────────────
/**
 * 返回按 level / type / status 分组的统计
 */
export function fetchAlarmStats() {
  const mockStats = {
    byLevel:  [
      { level: 'CRITICAL', count: 3 },
      { level: 'MAJOR',    count: 2 },
      { level: 'WARNING',  count: 1 },
      { level: 'MINOR',    count: 1 },
      { level: 'INFO',     count: 1 },
    ],
    byType:   [
      { type: 'FAULT',    count: 2 },
      { type: 'OFFLINE',  count: 2 },
      { type: 'SECURITY', count: 2 },
      { type: 'VISION',   count: 2 },
    ],
    byStatus: [
      { status: 'ACTIVE',       count: 1 },
      { status: 'ACKNOWLEDGED', count: 2 },
      { status: 'RECOVERED',    count: 5 },
    ],
  }
  return safeCall(() => request.get('/api/alarms/stats'), mockStats, 'GET /api/alarms/stats')
}

// ── 告警趋势 GET /api/alarms/trend?days=7 ────────────────────────────────
/**
 * @param {number} days  天数，默认 7
 */
export function fetchAlarmTrend(days = 7) {
  const mockTrend = Array.from({ length: days }, (_, i) => {
    const d = new Date()
    d.setDate(d.getDate() - (days - 1 - i))
    return {
      date:  d.toISOString().slice(0, 10),
      count: Math.round(Math.random() * 8),
    }
  })
  return safeCall(
    () => request.get('/api/alarms/trend', { params: { days } }),
    mockTrend,
    'GET /api/alarms/trend'
  )
}

// ── 批量处理 PUT /api/alarms/batch/handle ────────────────────────────────
/**
 * @param {{ ids: number[], handler: string, remark?: string }} data
 */
export function batchHandleAlarm(data) {
  return safeCall(
    () => request.put('/api/alarms/batch/handle', data),
    { updatedCount: data.ids?.length || 0 },
    'PUT /api/alarms/batch/handle'
  )
}

// ── 批量删除 DELETE /api/alarms/batch ────────────────────────────────────
/**
 * @param {{ ids: number[] }} data
 */
export function batchDeleteAlarm(data) {
  return safeCall(
    () => request.delete('/api/alarms/batch', { data }),
    { deletedCount: data.ids?.length || 0 },
    'DELETE /api/alarms/batch'
  )
}
