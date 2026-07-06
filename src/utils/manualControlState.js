export function parseLatestData(raw) {
  if (!raw) return null
  try {
    return typeof raw === 'string' ? JSON.parse(raw) : raw
  } catch {
    return null
  }
}

export function stateFromAction(action, brightness, fallbackBrightness = 75) {
  if (!action) return null

  if (action === 'OFF') {
    return { power: false, brightness: brightness || 0 }
  }
  if (action === 'ON') {
    return { power: true, brightness: brightness || 100 }
  }
  if (action.startsWith && action.startsWith('DIMMING')) {
    const match = action.match(/DIMMING\((\d+)\)/)
    return {
      power: true,
      brightness: match ? parseInt(match[1]) : (brightness || fallbackBrightness),
    }
  }

  return null
}

export function stateFromLatestData(latestData, fallbackBrightness = 75) {
  const data = parseLatestData(latestData)
  return stateFromAction(data?.action, data?.brightness, fallbackBrightness)
}

export function isManualModeActive(node, now = new Date()) {
  if (!node?.manualMode) return false
  if (!node?.manualExpireAt) return false
  return new Date(node.manualExpireAt) > now
}

export function stateFromControlHistoryRecord(record, fallbackBrightness = 75) {
  if (!record?.command) return null

  if (record.command === 'turn_on') {
    return stateFromAction('ON', record.params?.brightness, fallbackBrightness)
  }
  if (record.command === 'turn_off') {
    return stateFromAction('OFF', record.params?.brightness, fallbackBrightness)
  }
  if (record.command === 'dim') {
    return stateFromAction('DIMMING', record.params?.brightness, fallbackBrightness)
  }
  if (record.command.startsWith && record.command.startsWith('dimming')) {
    return stateFromAction(record.command.toUpperCase(), record.params?.brightness, fallbackBrightness)
  }

  return null
}

export function resolveManualControlState(node, latestHistoryRecord, fallbackBrightness = 75) {
  // 始终优先从控制历史解析（无论手动/自动模式），因为 latestData 遥测快照不含 action 字段
  const historyState = stateFromControlHistoryRecord(latestHistoryRecord, fallbackBrightness)
  if (historyState) return historyState
  return stateFromLatestData(node?.latestData, fallbackBrightness)
}
