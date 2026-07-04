/**
 * api/strategy.js — 照明策略管理接口
 * 对齐接口文档：开发变更日志 v1.0.0 四、照明策略管理模块
 *
 * 接口列表:
 *   POST   /api/policies/list      分页条件查询策略列表
 *   GET    /api/policies/{id}      策略详情
 *   POST   /api/policies           新增策略
 *   PUT    /api/policies/{id}      更新策略
 *   DELETE /api/policies/{id}      删除策略
 *   PUT    /api/policies/{id}/toggle  启用/禁用策略
 *   GET    /api/policies/lux-threshold 查询光照阈值
 *   PUT    /api/policies/lux-threshold 更新光照阈值
 */
import request from './request.js'
import { reportMock } from '../utils/mockStore.js'

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

async function safeCall(apiFn, mockData, endpoint) {
  try {
    return await apiFn()
  } catch (e) {
    if (e?.bizCode) throw e
    if (endpoint) reportMock(endpoint)
    return { code: 200, msg: 'mock', data: mockData }
  }
}

// ── 策略列表 GET /api/policies ─────────────────────────────────────────────
export function fetchStrategyList(query = { page: 1, size: 20 }) {
  return safeCall(
    () => request.get('/api/policies', { params: query }),
    { records: MOCK_POLICIES, total: MOCK_POLICIES.length },
    'GET /api/policies'
  )
}

// ── 策略详情 GET /api/policies/{id} ───────────────────────────────────────
export function fetchStrategyDetail(id) {
  return safeCall(
    () => request.get(`/api/policies/${id}`),
    MOCK_POLICIES.find(p => p.id === id) || MOCK_POLICIES[0],
    `GET /api/policies/${id}`
  )
}

// ── 新增策略 POST /api/policies ───────────────────────────────────────────
export function createStrategy(data) {
  return safeCall(
    () => request.post('/api/policies', data),
    { id: Date.now(), enabled: true, triggerCount: 0, ...data },
    'POST /api/policies'
  )
}

// ── 更新策略 PUT /api/policies/{id} ───────────────────────────────────────
export function updateStrategy(id, data) {
  return safeCall(
    () => request.put(`/api/policies/${id}`, data),
    { id, ...data },
    `PUT /api/policies/${id}`
  )
}

// ── 删除策略 DELETE /api/policies/{id} ────────────────────────────────────
export function deleteStrategy(id) {
  return request.delete(`/api/policies/${id}`)
}

// ── 启用/禁用策略 PUT /api/policies/{id}/toggle ───────────────────────────
/**
 * @param {number} id
 * @param {boolean} enabled  是否启用
 */
export function toggleStrategy(id, enabled) {
  return safeCall(
    () => request.put(`/api/policies/${id}/toggle`, { enabled }),
    { id, enabled },
    `PUT /api/policies/${id}/toggle`
  )
}

// ── 策略组列表（仅 Mock，后端暂无此接口） ──────────────────────────────────
export function fetchStrategyGroups() {
  return safeCall(
    () => request.get('/api/policies/groups'),
    ['主干道节能组', '景观灯组', '全域组', '园区灯组', '校区灯组'],
    'GET /api/policies/groups'
  )
}

// ── 查询光照阈值 GET /api/policies/lux-threshold ──────────────────────────
export function getLuxThreshold() {
  return safeCall(
    () => request.get('/api/policies/lux-threshold'),
    { policyId: 1, policyName: '光照联动自动开关', luxLt: 50, luxGt: 200, conditions: '{"lux_lt":50,"lux_gt":200}', enabled: true, priority: 1 },
    'GET /api/policies/lux-threshold'
  )
}

// ── 更新光照阈值 PUT /api/policies/lux-threshold ──────────────────────────
export function updateLuxThreshold(data) {
  return safeCall(
    () => request.put('/api/policies/lux-threshold', data),
    { policyId: 1, policyName: '光照联动自动开关', luxLt: data.luxLt, luxGt: data.luxGt, conditions: `{"lux_lt":${data.luxLt},"lux_gt":${data.luxGt}}`, enabled: true, priority: 1 },
    'PUT /api/policies/lux-threshold'
  )
}
