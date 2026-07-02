/**
 * 设备控制 API
 * 接口文档 V1.0 — FR-05 手动远程控制 / IR-11 安全可信控制
 *
 * POST /api/devices/{deviceId}/control
 *   Body: { action: "ON"|"OFF"|"DIMMING", brightness?: 0-100 }
 */
import request from './request.js'

/**
 * 手动控制单个设备
 * @param {string} deviceId   - 设备编号，例如 "SL-001"
 * @param {'ON'|'OFF'|'DIMMING'} action  - 指令类型
 * @param {number|null} brightness       - 仅 DIMMING 时必填（0-100）
 * @returns {Promise<{ code, msg, data: null }>}
 */
export function controlDevice(deviceId, action, brightness = null) {
  const body = { action }
  if (action === 'DIMMING' && brightness !== null) {
    body.brightness = brightness
  }
  return request.post(`/api/devices/${deviceId}/control`, body)
}
