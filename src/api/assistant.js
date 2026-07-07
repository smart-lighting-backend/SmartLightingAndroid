import request from './request.js'
import { reportMock } from '../utils/mockStore.js'

function buildMockResponse(message) {
  // 阈值相关
  const thresholdMatch = message.match(/(?:阈值|lux).*?(\d+)/i)
  if (thresholdMatch) {
    return {
      type: 'THRESHOLD_UPDATED',
      content: '已将光照触发阈值调整为 ' + thresholdMatch[1] + ' lux。',
      action: {
        name: 'SET_LUX_LT_THRESHOLD',
        luxLt: parseInt(thresholdMatch[1]),
        policyId: 2,
        policyName: '深夜节能调光'
      }
    }
  }
  // 亮度相关
  const brightnessMatch = message.match(/(?:亮度|调光).*?(\d+)/i)
  if (brightnessMatch) {
    return {
      type: 'THRESHOLD_UPDATED',
      content: '已将调光亮度调整为 ' + brightnessMatch[1] + '%。',
      action: {
        name: 'AI_UPDATE_POLICY',
        brightness: parseInt(brightnessMatch[1]),
        policyId: 2,
        policyName: '深夜节能调光'
      }
    }
  }
  // 时间相关
  const timeMatch = message.match(/(?:开始|结束|时段).*?(\d{1,2}:\d{2})/i)
  if (timeMatch) {
    return {
      type: 'THRESHOLD_UPDATED',
      content: '已调整策略时间参数。',
      action: {
        name: 'AI_UPDATE_POLICY',
        startTime: timeMatch[1],
        policyId: 2,
        policyName: '深夜节能调光'
      }
    }
  }
  return null
}

const MOCK_REPLIES = {
  '灯不亮怎么办': {
    type: 'KNOWLEDGE_QA',
    content: '1. 检查网关通信是否正常。\n2. 检查电源模块是否损坏。\n3. 请参考《路灯常见故障手册》进行排查。'
  }
}

export async function sendChatMessage(message) {
  try {
    const res = await request.post('/api/assistant/chat', { message }, { timeout: 60000 })
    // request.post 返回的是 axios 拦截器解包后的 data
    return res
  } catch (error) {
    if (error?.bizCode) throw error
    reportMock('POST /api/assistant/chat')
    
    // 降级为 Mock
    let mockData = MOCK_REPLIES[message] || buildMockResponse(message)
    if (!mockData) {
      mockData = {
        type: 'KNOWLEDGE_QA',
        content: `已收到您的问题："${message}"。目前后端服务未响应，我是模拟助手。您可以输入 "把阈值调到30"、"亮度调到60" 等指令，或者 "灯不亮怎么办" 等故障问题。`
      }
    }
    return { code: 200, msg: 'mock', data: mockData }
  }
}

/** 设备一键诊断 POST /api/assistant/diagnose */
export async function diagnoseDevice(deviceId, question) {
  return request.post('/api/assistant/diagnose', { deviceId, question: question || '' }, { timeout: 60000 })
}
