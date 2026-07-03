/**
 * api/strategy.js — 策略配置相关接口
 */
import request from './request.js'

const MOCK_STRATEGIES = [
  { id: 1, name: '深夜节能模式',   group: '主干道节能组', startTime: '23:00', endTime: '05:00', status: 'active',    triggerCount: 243, lastTrigger: '2023-10-27 23:00' },
  { id: 2, name: '雨雾增亮补偿',   group: '全域组',       startTime: '00:00', endTime: '23:59', status: 'active',    triggerCount: 56,  lastTrigger: '2023-10-26 07:32' },
  { id: 3, name: '节假日景观模式', group: '景观灯组',     startTime: '18:00', endTime: '23:00', status: 'inactive',  triggerCount: 12,  lastTrigger: '2023-10-01 18:00' },
  { id: 4, name: '交通高峰亮灯',   group: '主干道节能组', startTime: '07:00', endTime: '09:00', status: 'active',    triggerCount: 189, lastTrigger: '2023-10-27 07:00' },
]

const MOCK_GROUPS = ['主干道节能组', '景观灯组', '全域组', '园区灯组', '校区灯组']

async function safeCall(apiFn, mockData) {
  try { return await apiFn() }
  catch { return { code: 200, msg: 'mock', data: mockData } }
}

export function fetchStrategyList() {
  return safeCall(() => request.get('/api/strategies'), { list: MOCK_STRATEGIES, total: MOCK_STRATEGIES.length })
}

export function fetchStrategyGroups() {
  return safeCall(() => request.get('/api/strategies/groups'), MOCK_GROUPS)
}

export function createStrategy(data) {
  return safeCall(() => request.post('/api/strategies', data), { id: Date.now(), ...data })
}

export function updateStrategy(id, data) {
  return safeCall(() => request.put(`/api/strategies/${id}`, data), { success: true })
}

export function deleteStrategy(id) {
  return safeCall(() => request.delete(`/api/strategies/${id}`), { success: true })
}

export function toggleStrategy(id, status) {
  return safeCall(() => request.patch(`/api/strategies/${id}/status`, { status }), { success: true })
}
