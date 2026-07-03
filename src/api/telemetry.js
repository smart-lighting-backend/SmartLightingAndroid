import axios from 'axios'

const mockLatestTelemetry = {
  'SL-001': {
    deviceId: 'SL-001',
    illuminance: 1256,
    temperature: 26.8,
    humidity: 45,
    pir: 1,
    updateTime: '2026-07-03 14:35:00'
  },
  'SL-002': {
    deviceId: 'SL-002',
    illuminance: 890,
    temperature: 27.2,
    humidity: 42,
    pir: 0,
    updateTime: '2026-07-03 14:34:55'
  },
  'SL-003': {
    deviceId: 'SL-003',
    illuminance: 1520,
    temperature: 25.5,
    humidity: 48,
    pir: 1,
    updateTime: '2026-07-03 10:15:33'
  },
  'SL-004': {
    deviceId: 'SL-004',
    illuminance: 980,
    temperature: 26.1,
    humidity: 44,
    pir: 0,
    updateTime: '2026-07-03 14:34:40'
  },
  'SL-005': {
    deviceId: 'SL-005',
    illuminance: 1100,
    temperature: 27.5,
    humidity: 40,
    pir: 1,
    updateTime: '2026-07-03 14:34:30'
  },
  'SL-006': {
    deviceId: 'SL-006',
    illuminance: 750,
    temperature: 28.0,
    humidity: 38,
    pir: 0,
    updateTime: '2026-07-02 22:08:12'
  },
  'SL-007': {
    deviceId: 'SL-007',
    illuminance: 1350,
    temperature: 26.0,
    humidity: 46,
    pir: 1,
    updateTime: '2026-07-03 14:34:50'
  },
  'SL-008': {
    deviceId: 'SL-008',
    illuminance: 920,
    temperature: 26.9,
    humidity: 43,
    pir: 0,
    updateTime: '2026-07-03 14:34:25'
  }
}

const generateHistoryData = (deviceId, timeRange) => {
  const now = new Date()
  const data = []
  let interval = 5 * 60 * 1000
  let count = 12

  if (timeRange === '24h') {
    interval = 60 * 60 * 1000
    count = 24
  } else if (timeRange === '7d') {
    interval = 24 * 60 * 60 * 1000
    count = 7
  }

  for (let i = count - 1; i >= 0; i--) {
    const timestamp = new Date(now.getTime() - i * interval)
    const baseIlluminance = 1000 + Math.random() * 600
    const hour = timestamp.getHours()
    let illuminance = baseIlluminance

    if (hour >= 6 && hour <= 18) {
      illuminance = baseIlluminance + Math.sin((hour - 6) / 12 * Math.PI) * 800
    } else {
      illuminance = baseIlluminance * 0.3 + Math.random() * 100
    }

    data.push({
      time: timestamp.toLocaleString('zh-CN', {
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      }),
      illuminance: Math.round(illuminance),
      pir: Math.random() > 0.6 ? 1 : 0,
      temperature: Math.round((25 + Math.random() * 5) * 10) / 10,
      humidity: Math.round(40 + Math.random() * 10)
    })
  }

  return data
}

export const fetchLatestTelemetry = (deviceId) => {
  return new Promise((resolve) => {
    setTimeout(() => {
      const data = mockLatestTelemetry[deviceId] || {
        deviceId,
        illuminance: 0,
        temperature: 0,
        humidity: 0,
        pir: 0,
        updateTime: '--'
      }
      resolve({
        code: 200,
        message: 'success',
        data
      })
    }, 500)
  })
}

export const fetchTelemetryHistory = (params) => {
  const { deviceId, timeRange = '1h' } = params
  return new Promise((resolve) => {
    setTimeout(() => {
      const data = generateHistoryData(deviceId, timeRange)
      resolve({
        code: 200,
        message: 'success',
        data: {
          list: data,
          total: data.length
        }
      })
    }, 500)
  })
}