import request from './request.js'
import { reportMock } from '../utils/mockStore.js'

const MOCK_REPLIES = {
  '灯不亮怎么办': {
    type: 'KNOWLEDGE_QA',
    content: '1. 检查网关通信是否正常。\n2. 检查电源模块是否损坏。\n3. 请参考《路灯常见故障手册》进行排查。'
  },
  '把阈值调到30': {
    type: 'THRESHOLD_UPDATED',
    content: '已将光照触发阈值调整为 30 lux。',
    action: {
      name: 'SET_LUX_LT_THRESHOLD',
      luxLt: 30,
      policyId: 1,
      policyName: '光照低于阈值自动开灯'
    }
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
    let mockData = MOCK_REPLIES[message]
    if (!mockData) {
      mockData = {
        type: 'KNOWLEDGE_QA',
        content: `已收到您的问题："${message}"。目前后端服务未响应，我是模拟助手。您可以输入 "灯不亮怎么办" 或 "把阈值调到30" 体验效果。`
      }
    }
    return { code: 200, msg: 'mock', data: mockData }
  }
}
