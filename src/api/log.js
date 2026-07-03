import request from './request.js'

/**
 * 获取系统日志列表
 * @param {number} page 
 * @param {number} size 
 */
export async function getSystemLogs(page = 1, size = 50) {
  try {
    const res = await request.get('/api/logs/system', {
      params: { page, size }
    })
    
    // 映射后端 AuditLog 实体为前端字段
    // AuditLog: id, operator, action, targetType, targetId, detail, result, ipAddress, operatedAt
    const list = (res.data?.records || []).map(log => {
      let level = 'info'
      if (log.result === 'FAIL' || log.result === 'ERROR') level = 'error'
      else if (log.action && log.action.includes('警告')) level = 'warn'
      
      return {
        id: log.id,
        time: log.operatedAt ? log.operatedAt.replace('T', ' ') : '--',
        level: level,
        user: log.operator || '系统',
        action: `[${log.targetType || 'SYSTEM'}] ${log.detail || log.action}`
      }
    })
    
    return {
      code: 200,
      data: {
        list,
        total: res.data?.total || 0
      }
    }
  } catch (e) {
    if (e?.bizCode) throw e
    return {
      code: 200,
      data: { list: [], total: 0 }
    }
  }
}
