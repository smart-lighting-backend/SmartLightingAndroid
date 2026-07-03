/**
 * api/strategy.js — 照明策略管理接口
 * 对齐接口文档：开发变更日志 v1.0.0 四、照明策略管理模块
 *
 * 接口列表:
 *   GET    /api/policies           策略列表
 *   GET    /api/policies/{id}      策略详情
 *   POST   /api/policies           新增策略
 *   PUT    /api/policies/{id}      更新策略
 *   DELETE /api/policies/{id}      删除策略
 *   PUT    /api/policies/{id}/toggle  启用/禁用策略
 */
import request from './request.js'

// ── Mock 数据 ─────────────────────────────────────────────────────────────
const MOCK_POLICIES = [
  {
    id: 1, name: '深夜节能模式',   group: '主干道节能组',
    startTime: '23:00', endTime: '05:00',
    enabled: true,  triggerCount: 243, lastTrigger: '2023-10-27T23:00:00',
    description: '深夜时段自动降低路灯亮度至 30%，节约能耗',
  },
  {
    id: 2, name: '雨雾增亮补偿',   group: '全域组',
    startTime: '00:00', endTime: '23:59',
    enabled: true,  triggerCount: 56,  lastTrigger: '2023-10-26T07:32:00',
    description: '检测到雨雾天气时自动提升亮度至 100%',
  },
  {
    id: 3, name: '节假日景观模式', group: '景观灯组',
    startTime: '18:00', endTime: '23:00',
    enabled: false, triggerCount: 12,  lastTrigger: '2023-10-01T18:00:00',
    description: '节假日开启景观灯彩色模式',
  },
  {
    id: 4, name: '交通高峰亮灯',   group: '主干道节能组',
    startTime: '07:00', endTime: '09:00',
    enabled: true,  triggerCount: 189, lastTrigger: '2023-10-27T07:00:00',
    description: '早晚高峰时段自动开启全部路灯',
  },
]

async function safeCall(apiFn, mockData) {
  try {
    return await apiFn()
  } catch (e) {
    const isNetworkErr = !e?.response && !e?.bizCode
    if (isNetworkErr) return { code: 200, msg: 'mock', data: mockData }
    throw e
  }
}

// ── 策略列表 GET /api/policies ─────────────────────────────────────────────
export function fetchStrategyList() {
  return safeCall(
    () => request.get('/api/policies'),
    MOCK_POLICIES
  )
}

// ── 策略详情 GET /api/policies/{id} ───────────────────────────────────────
export function fetchStrategyDetail(id) {
  return safeCall(
    () => request.get(`/api/policies/${id}`),
    MOCK_POLICIES.find(p => p.id === id) || MOCK_POLICIES[0]
  )
}

// ── 新增策略 POST /api/policies ───────────────────────────────────────────
export function createStrategy(data) {
  return safeCall(
    () => request.post('/api/policies', data),
    { id: Date.now(), enabled: true, triggerCount: 0, ...data }
  )
}

// ── 更新策略 PUT /api/policies/{id} ───────────────────────────────────────
export function updateStrategy(id, data) {
  return safeCall(
    () => request.put(`/api/policies/${id}`, data),
    { id, ...data }
  )
}

// ── 删除策略 DELETE /api/policies/{id} ────────────────────────────────────
export function deleteStrategy(id) {
  return safeCall(
    () => request.delete(`/api/policies/${id}`),
    null
  )
}

// ── 启用/禁用策略 PUT /api/policies/{id}/toggle ───────────────────────────
/**
 * @param {number} id
 * @param {boolean} enabled  是否启用
 */
export function toggleStrategy(id, enabled) {
  return safeCall(
    () => request.put(`/api/policies/${id}/toggle`, { enabled }),
    { id, enabled }
  )
}

// ── 策略组列表（仅 Mock，后端暂无此接口） ──────────────────────────────────
export function fetchStrategyGroups() {
  return safeCall(
    () => request.get('/api/policies/groups'),
    ['主干道节能组', '景观灯组', '全域组', '园区灯组', '校区灯组']
  )
}
