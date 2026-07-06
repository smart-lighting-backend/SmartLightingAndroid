/**
 * api/events.js — 视觉事件 & 语音事件查询接口
 *
 * GET /api/vision-events/page       分页查询视觉事件
 * GET /api/vision-events/device/{id} 按设备查询视觉事件
 * GET /api/voice-events/page        分页查询语音事件
 * GET /api/voice-events/device/{id}  按设备查询语音事件
 */
import request from './request.js'

const LEGACY_EVENT_TEXT = {
  // 兼容旧数据：部分 MQTT 事件曾被后端按系统默认编码解码，数据库中留下了乱码文本。
  '琛屼汉妫�娴�': '行人检测',
  '杞﹁締閫氳': '车辆通行',
  '寮傚父鍋滆溅': '异常停车',
  '鍗遍櫓鍦烘櫙': '危险场景',
  '棰勮': '预警',
  '璀﹀憡': '警告',
  '鎾姤': '播报',
  '骞挎挱': '广播',
  '鑷姩': '自动',
  '璇锋敞鎰忥紝鍓嶆柟璺鐓ф槑宸插紑鍚紝琛屼汉璇锋敞鎰忓畨鍏�': '请注意，前方路段照明已开启，行人请注意安全',
  '褰撳墠鍖哄煙鍏夌収涓嶈冻锛岃矾鐏凡鑷姩璋冧寒鑷�80%': '当前区域光照不足，路灯已自动调亮至80%',
  '闆ㄩ浘澶╂皵棰勮锛岃鍑忛�熸參琛岋紝寮�鍚浘鐏�': '雨雾天气预警，请减速慢行，开启雾灯',
  '璁惧鑷瀹屾垚锛屾墍鏈夋ā鍧楄繍琛屾甯�': '设备自检完成，所有模块运行正常',
  '澶滈棿鑺傝兘妯″紡宸插惎鍔紝璺伅浜害闄嶈嚦30%': '夜间节能模式已启动，路灯亮度降至30%',
  '閬撹矾鏂藉伐鍖哄煙锛岃娉ㄦ剰閬胯': '道路施工区域，请注意避让',
  '璇ゅ尯鍩熻溅娴侀噺杈冨ぇ锛屽凡鍒囨崲涓洪珮宄颁寒鐏ā寮�': '该区域车流量较大，已切换为高峰亮灯模式',
  '绌烘皵璐ㄩ噺寮傚父锛屽缓璁噺灏戞埛澶栨椿鍔�': '空气质量异常，建议减少户外活动',
}

function normalizeEventText(value) {
  if (typeof value !== 'string') return value
  return LEGACY_EVENT_TEXT[value] || LEGACY_EVENT_TEXT[value.trim()] || value
}

function normalizeEventPage(res, normalizeRecord) {
  const records = res?.data?.records
  if (Array.isArray(records)) {
    res.data.records = records.map(normalizeRecord)
  }
  return res
}

function normalizeVisionRecord(record) {
  return {
    ...record,
    eventType: normalizeEventText(record.eventType),
  }
}

function normalizeVoiceRecord(record) {
  return {
    ...record,
    type: normalizeEventText(record.type),
    content: normalizeEventText(record.content),
    source: normalizeEventText(record.source),
  }
}

// ── 视觉事件 ────────────────────────────────────────────────
export function fetchVisionEvents(params = {}) {
  return request
    .get('/api/vision-events/page', { params })
    .then(res => normalizeEventPage(res, normalizeVisionRecord))
}

export function fetchVisionEventsByDevice(deviceId, params = {}) {
  return request
    .get(`/api/vision-events/device/${deviceId}`, { params })
    .then(res => normalizeEventPage(res, normalizeVisionRecord))
}

// ── 语音事件 ────────────────────────────────────────────────
export function fetchVoiceEvents(params = {}) {
  return request
    .get('/api/voice-events/page', { params })
    .then(res => normalizeEventPage(res, normalizeVoiceRecord))
}

export function fetchVoiceEventsByDevice(deviceId, params = {}) {
  return request
    .get(`/api/voice-events/device/${deviceId}`, { params })
    .then(res => normalizeEventPage(res, normalizeVoiceRecord))
}
