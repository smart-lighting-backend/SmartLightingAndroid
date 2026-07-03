/**
 * api/warnings.js — 告警中心接口
 * 对齐接口文档：设备离线告警通知接口开发日志
 *
 * 接口：
 *   GET /api/alarms/page    告警分页查询
 *   GET /api/alarms/{id}    告警详情
 *
 * 告警状态（后端字符串）:
 *   ACTIVE | ACKNOWLEDGED | RECOVERED
 *
 * 告警级别:
 *   CRITICAL | MAJOR | MINOR | WARNING | INFO
 *
 * 告警类型:
 *   OFFLINE | FAULT | SECURITY | VISION | ...
 */
import request from './request.js'

// ── 状态/级别映射 ──────────────────────────────────────────────────────────
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
  { id: 1, deviceId: 'SL-001', type: 'FAULT',    level: 'CRITICAL', status: 'ACTIVE',       reason: '电源模块输出电压异常，超出额定范围 ±15%',       startAt: '2023-10-27T14:32:05', recoverAt: null,                handler: null },
  { id: 2, deviceId: 'SL-003', type: 'OFFLINE',   level: 'MAJOR',    status: 'ACKNOWLEDGED', reason: '心跳中断超过 5 分钟，最后心跳时间：2023-10-27 12:10:00', startAt: '2023-10-27T12:15:22', recoverAt: null,                handler: '张工' },
  { id: 3, deviceId: 'SL-004', type: 'VISION',    level: 'MINOR',    status: 'RECOVERED',    reason: '摄像头视场被轻微遮挡，影像质量下降',               startAt: '2023-10-26T18:40:11', recoverAt: '2023-10-26T19:00:00', handler: 'system' },
  { id: 4, deviceId: 'SL-005', type: 'SECURITY',  level: 'CRITICAL', status: 'ACKNOWLEDGED', reason: '检测到设备门被非授权方式打开',                     startAt: '2023-10-26T03:12:55', recoverAt: null,                handler: '安保组-王五' },
  { id: 5, deviceId: 'SL-006', type: 'FAULT',     level: 'WARNING',  status: 'RECOVERED',    reason: '驱动板温度持续超过阈值，触发降功率保护',             startAt: '2023-10-25T22:08:33', recoverAt: '2023-10-25T23:00:00', handler: '李工' },
  { id: 6, deviceId: 'SL-007', type: 'OFFLINE',   level: 'MAJOR',    status: 'RECOVERED',    reason: '设备未上报心跳，判定为离线',                       startAt: '2023-10-25T18:50:11', recoverAt: '2023-10-25T19:30:00', handler: 'system' },
  { id: 7, deviceId: 'SL-008', type: 'VISION',    level: 'INFO',     status: 'RECOVERED',    reason: '夜间巡检图像亮度异常，疑似灯具衰减',               startAt: '2023-10-25T02:33:40', recoverAt: '2023-10-25T08:00:00', handler: '赵工' },
  { id: 8, deviceId: 'SL-001', type: 'SECURITY',  level: 'CRITICAL', status: 'RECOVERED',    reason: '检测到可疑人员在设备周围长时间徘徊',               startAt: '2023-10-24T23:15:18', recoverAt: '2023-10-24T23:45:00', handler: '安保组' },
]

async function safeCall(apiFn, mockData) {
  try {
    const result = await apiFn()
    // 后端返回空数据时降级到 Mock
    const isEmpty = result === null || result === undefined ||
                   (Array.isArray(result) && result.length === 0) ||
                   (typeof result === 'object' && result.data !== undefined && Array.isArray(result.data) && result.data.length === 0)
    if (isEmpty) return { code: 200, msg: 'mock', data: mockData }
    return result
  } catch (e) {
    // 网络不可达 或 代理 502/503/504 时降级到 Mock
    const httpStatus = e?.response?.status
    const isNetworkErr = (!e?.response && !e?.bizCode) || (httpStatus != null && httpStatus >= 502 && httpStatus <= 504)
    if (isNetworkErr) return { code: 200, msg: 'mock', data: mockData }
    throw e
  }
}

// ── 告警分页查询 ───────────────────────────────────────────────────────────
/**
 * @param {{
 *   pageNum?: number,
 *   pageSize?: number,
 *   deviceId?: string,
 *   type?: string,
 *   level?: string,
 *   status?: string,
 *   startTime?: string,
 *   endTime?: string
 * }} params
 */
export function fetchAlarmPage(params = {}) {
  // 构造真实 API 参数
  const query = { pageNum: params.pageNum || 1, pageSize: params.pageSize || 10 }
  if (params.deviceId)  query.deviceId  = params.deviceId
  if (params.type  && params.type  !== 'ALL') query.type  = params.type
  if (params.level && params.level !== 'ALL') query.level = params.level
  if (params.status && params.status !== 'ALL') query.status = params.status
  if (params.startTime) query.startTime = params.startTime
  if (params.endTime)   query.endTime   = params.endTime

  // Mock 数据客户端过滤
  let list = [...MOCK_ALARMS]
  if (query.deviceId) list = list.filter(a => a.deviceId.includes(query.deviceId))
  if (query.type)     list = list.filter(a => a.type === query.type)
  if (query.level)    list = list.filter(a => a.level === query.level)
  if (query.status)   list = list.filter(a => a.status === query.status)

  const total  = 128
  const size   = query.pageSize
  const current = query.pageNum

  return safeCall(
    () => request.get('/api/alarms/page', { params: query }),
    { records: list, total, size, current, pages: Math.ceil(total / size) }
  )
}

// ── 告警详情 ──────────────────────────────────────────────────────────────
export function fetchAlarmDetail(id) {
  return safeCall(
    () => request.get(`/api/alarms/${id}`),
    MOCK_ALARMS.find(a => a.id === id) || MOCK_ALARMS[0]
  )
}