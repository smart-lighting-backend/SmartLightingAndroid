/**
 * 坐标解析与 WGS84 → GCJ-02 转换工具
 *
 * 高德地图使用 GCJ-02（火星坐标系），若后端存储的是 WGS84（GPS 原始坐标），
 * 需调用 wgs84ToGcj02 转换，否则地图上会有约 500 米偏移。
 * 当前后端 seed data 的坐标在重庆主城区，默认假设已是 GCJ-02。
 */

const PI = Math.PI
const A = 6378245.0
const EE = 0.00669342162296594323

function transformLat(x, y) {
  let ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x))
  ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0
  ret += (20.0 * Math.sin(y * PI) + 40.0 * Math.sin(y / 3.0 * PI)) * 2.0 / 3.0
  ret += (160.0 * Math.sin(y / 12.0 * PI) + 320.0 * Math.sin(y * PI / 30.0)) * 2.0 / 3.0
  return ret
}

function transformLng(x, y) {
  let ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x))
  ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0
  ret += (20.0 * Math.sin(x * PI) + 40.0 * Math.sin(x / 3.0 * PI)) * 2.0 / 3.0
  ret += (150.0 * Math.sin(x / 12.0 * PI) + 300.0 * Math.sin(x / 30.0 * PI)) * 2.0 / 3.0
  return ret
}

function isOutOfChina(lng, lat) {
  return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271
}

/**
 * 解析 "lng,lat" 字符串 → { lng, lat }
 * @param {string} str 如 "106.5622,29.5621"
 * @returns {{ lng: number, lat: number } | null}
 */
export function parseLocation(str) {
  if (!str || typeof str !== 'string') return null
  const parts = str.split(',').map(s => s.trim())
  if (parts.length < 2) return null
  const lng = parseFloat(parts[0])
  const lat = parseFloat(parts[1])
  if (isNaN(lng) || isNaN(lat)) return null
  return { lng, lat }
}

/**
 * WGS84 → GCJ-02 标准火星坐标转换
 * @param {number} lng 经度
 * @param {number} lat 纬度
 * @returns {{ lng: number, lat: number }}
 */
export function wgs84ToGcj02(lng, lat) {
  if (isOutOfChina(lng, lat)) return { lng, lat }

  let dLat = transformLat(lng - 105.0, lat - 35.0)
  let dLng = transformLng(lng - 105.0, lat - 35.0)
  const radLat = lat / 180.0 * PI
  let magic = Math.sin(radLat)
  magic = 1 - EE * magic * magic
  const sqrtMagic = Math.sqrt(magic)
  dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI)
  dLng = (dLng * 180.0) / (A / sqrtMagic * Math.cos(radLat) * PI)

  return { lng: lng + dLng, lat: lat + dLat }
}

/**
 * 自动判断并转换到 GCJ-02
 * @param {number} lng
 * @param {number} lat
 * @param {boolean} assumeGcj02 默认 true（假设已是 GCJ-02，不转换）
 * @returns {{ lng: number, lat: number }}
 */
export function autoToGcj02(lng, lat, assumeGcj02 = true) {
  if (assumeGcj02) return { lng, lat }
  return wgs84ToGcj02(lng, lat)
}
