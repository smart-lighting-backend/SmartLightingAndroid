/**
 * api/events.js — 视觉事件 & 语音事件查询接口
 *
 * GET /api/vision-events/page       分页查询视觉事件
 * GET /api/vision-events/device/{id} 按设备查询视觉事件
 * GET /api/voice-events/page        分页查询语音事件
 * GET /api/voice-events/device/{id}  按设备查询语音事件
 */
import request from './request.js'

// ── 视觉事件 ────────────────────────────────────────────────
export function fetchVisionEvents(params = {}) {
  return request.get('/api/vision-events/page', { params })
}

export function fetchVisionEventsByDevice(deviceId, params = {}) {
  return request.get(`/api/vision-events/device/${deviceId}`, { params })
}

// ── 语音事件 ────────────────────────────────────────────────
export function fetchVoiceEvents(params = {}) {
  return request.get('/api/voice-events/page', { params })
}

export function fetchVoiceEventsByDevice(deviceId, params = {}) {
  return request.get(`/api/voice-events/device/${deviceId}`, { params })
}
