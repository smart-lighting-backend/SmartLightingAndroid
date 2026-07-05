/**
 * api/device.js — 设备查询接口（兼容旧版，实际委托 devices.js）
 */
export { fetchDeviceList, fetchDeviceDetail, fetchLatestTelemetry,
         createDevice, updateDevice, deleteDevice, STATUS_MAP, STATUS_QUERY_MAP }
  from './devices.js'
