/**
 * api/alarm.js — 告警接口（兼容旧版，实际委托 warnings.js）
 */
export { fetchAlarmPage, fetchAlarmDetail, createAlarm, updateAlarm, deleteAlarm,
         handleAlarm, batchHandleAlarm, batchDeleteAlarm,
         ALARM_STATUS_MAP, ALARM_LEVEL_MAP, ALARM_TYPE_MAP }
  from './warnings.js'
