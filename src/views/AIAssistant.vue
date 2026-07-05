<script setup>
import { ref, reactive, onMounted } from 'vue'
import { sendChatMessage, diagnoseDevice } from '../api/assistant.js'
import { fetchDeviceList } from '../api/devices.js'

const messages = ref([
  { role: 'assistant', text: '你好！我是智慧路灯节能系统的 AI 助手。基于 MaxKB 知识库，我可以帮助你解答路灯故障排查，也可以帮你动态调整系统阈值（例如："把阈值调到30"）。请问有什么可以帮您？' },
])
const input = ref('')
const loading = ref(false)

const suggestions = ['把阈值调到30', '亮度调到60%', '灯不亮怎么办', '如何优化节能策略？']
const deviceList = ref([])
const selectedDevice = ref('')

onMounted(async () => {
  try {
    const list = await fetchDeviceList()
    deviceList.value = Array.isArray(list) ? list : (list?.data?.records || [])
  } catch { deviceList.value = [] }
})

async function runDiagnose() {
  if (!selectedDevice.value) return
  loading.value = true
  const label = '诊断设备: ' + selectedDevice.value
  messages.value.push({ role: 'user', text: label })
  try {
    const res = await diagnoseDevice(selectedDevice.value, '')
    if (res?.data) {
      messages.value.push({ role: 'assistant', text: res.data.content || '诊断完成', type: 'KNOWLEDGE_QA' })
    }
  } catch {
    messages.value.push({ role: 'assistant', text: '诊断请求失败，请检查服务状态。' })
  }
  loading.value = false
}

// 过滤 action 中的非参数 key（name/policyId/policyName 是元信息）
function actionParams(action) {
  if (!action) return {}
  const meta = ['name', 'policyId', 'policyName']
  return Object.fromEntries(Object.entries(action).filter(([k]) => !meta.includes(k)))
}

const PARAM_LABELS = {
  luxLt: '开灯阈值', lux_lt: '开灯阈值',
  luxGt: '关灯阈值', lux_gt: '关灯阈值',
  tempLt: '低温触发', temp_lt: '低温触发',
  startTime: '开始时间', endTime: '结束时间',
  brightness: '调光亮度', enabled: '启用状态',
}

function paramLabel(key) { return PARAM_LABELS[key] || key }

function formatParam(key, val) {
  if (['luxLt','lux_lt','luxGt','lux_gt'].includes(key)) return val + ' lux'
  if (['tempLt','temp_lt'].includes(key)) return val + '℃'
  if (key === 'brightness') return val + '%'
  return val
}

async function sendMessage(text) {
  const q = text || input.value.trim()
  if (!q) return
  messages.value.push({ role: 'user', text: q })
  input.value = ''
  loading.value = true
  
  try {
    const res = await sendChatMessage(q)
    if (res && res.data) {
      const type = res.data.type
      const content = res.data.content || ''
      const action = res.data.action
      
      messages.value.push({ role: 'assistant', text: content, type, action })
    }
  } catch (e) {
    messages.value.push({ role: 'assistant', text: '抱歉，系统通信出现异常，请稍后重试。' })
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="assistant-page">
    <div class="page-header">
      <h1 class="page-title">
        <svg viewBox="0 0 24 24" fill="none" width="22" height="22"><rect x="4" y="8" width="16" height="12" rx="2" fill="currentColor" opacity="0.2" stroke="currentColor" stroke-width="1.5"/><circle cx="9" cy="13" r="1.5" fill="currentColor"/><circle cx="15" cy="13" r="1.5" fill="currentColor"/><path d="M9 17h6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/><path d="M12 8V5M10 5h4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
        智能助手
      </h1>
      <p class="page-sub">AI 驱动的设备诊断与运维建议</p>
    </div>

    <div class="chat-container">
      <div class="messages-area">
        <div v-for="(msg, i) in messages" :key="i" class="message" :class="msg.role">
          <div class="msg-avatar">
            <svg v-if="msg.role==='assistant'" viewBox="0 0 24 24" fill="none"><rect x="4" y="8" width="16" height="12" rx="2" fill="currentColor" opacity="0.6" stroke="currentColor" stroke-width="1.5"/><circle cx="9" cy="13" r="1.5" fill="currentColor"/><circle cx="15" cy="13" r="1.5" fill="currentColor"/></svg>
            <svg v-else viewBox="0 0 24 24" fill="none"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2M12 11a4 4 0 100-8 4 4 0 000 8z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
          </div>
          <div class="msg-bubble" style="white-space: pre-line">
            {{ msg.text }}
            <div v-if="msg.action" class="action-card">
              <div class="ac-title">
                <svg viewBox="0 0 24 24" fill="none" width="14" height="14"><path d="M20 6L9 17l-5-5" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
                指令已执行: {{ msg.action.name }}
              </div>
              <div class="ac-detail">▸ 目标策略：{{ msg.action.policyName }} (ID: {{ msg.action.policyId }})</div>
              <div v-for="(val, key) in actionParams(msg.action)" :key="key" class="ac-detail">
                ▸ {{ paramLabel(key) }}：<span class="ac-highlight">{{ formatParam(key, val) }}</span>
              </div>
              <router-link :to="'/strategy/edit/' + msg.action.policyId" class="ac-link">查看策略 →</router-link>
            </div>
          </div>
        </div>
        <div v-if="loading" class="message assistant">
          <div class="msg-avatar"><svg viewBox="0 0 24 24" fill="none"><rect x="4" y="8" width="16" height="12" rx="2" fill="currentColor" opacity="0.6" stroke="currentColor" stroke-width="1.5"/></svg></div>
          <div class="msg-bubble typing"><span></span><span></span><span></span></div>
        </div>
      </div>

      <div class="diagnose-bar">
        <select v-model="selectedDevice" class="device-select">
          <option value="">— 选择设备 —</option>
          <option v-for="d in deviceList" :key="d.deviceId || d.id" :value="d.deviceId">{{ d.deviceId }} {{ d.name }}</option>
        </select>
        <button class="diagnose-btn" @click="runDiagnose" :disabled="loading || !selectedDevice">一键诊断</button>
      </div>

      <div class="suggestions">
        <button v-for="s in suggestions" :key="s" class="suggestion-chip" @click="sendMessage(s)">{{ s }}</button>
      </div>

      <div class="input-area">
        <input v-model="input" class="chat-input" placeholder="输入问题，例如：当前有哪些设备异常？" @keydown.enter="sendMessage()" :disabled="loading" />
        <button class="send-btn" @click="sendMessage()" :disabled="loading || !input.trim()">
          <svg viewBox="0 0 24 24" fill="none"><path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.assistant-page { padding: 24px 28px; height: calc(100vh - 56px); display: flex; flex-direction: column; }
.page-header { margin-bottom: 16px; }
.page-title { display: flex; align-items: center; gap: 10px; font-size: 22px; font-weight: 700; color: #e0f4ff; margin-bottom: 4px; }
.page-sub { font-size: 13px; color: rgba(140,190,220,0.6); }
.chat-container {
  flex: 1; display: flex; flex-direction: column;
  background: rgba(8,20,45,0.8);
  border: 1px solid rgba(0,120,200,0.15);
  border-radius: 12px; overflow: hidden;
}
.messages-area { flex: 1; overflow-y: auto; padding: 20px; display: flex; flex-direction: column; gap: 14px; }
.message { display: flex; gap: 10px; }
.message.user { flex-direction: row-reverse; }
.msg-avatar {
  width: 32px; height: 32px; border-radius: 50%; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
}
.message.assistant .msg-avatar { background: rgba(0,120,200,0.2); border: 1px solid rgba(77,208,225,0.25); color: #4dd0e1; }
.message.user .msg-avatar { background: rgba(0,80,160,0.3); border: 1px solid rgba(0,120,200,0.3); color: rgba(140,190,220,0.8); }
.msg-avatar svg { width: 18px; height: 18px; }
.msg-bubble {
  max-width: 70%; padding: 11px 14px; border-radius: 10px;
  font-size: 13px; line-height: 1.6;
}
.message.assistant .msg-bubble { background: rgba(0,30,70,0.7); border: 1px solid rgba(0,100,160,0.2); color: rgba(200,230,245,0.9); border-radius: 2px 10px 10px 10px; }
.message.user .msg-bubble { background: rgba(0,100,200,0.25); border: 1px solid rgba(77,208,225,0.2); color: #d0eaf8; border-radius: 10px 2px 10px 10px; }
.typing { display: flex; align-items: center; gap: 5px; padding: 14px; }
.typing span { width: 6px; height: 6px; border-radius: 50%; background: rgba(77,208,225,0.6); animation: bounce 1.2s infinite; }
.typing span:nth-child(2) { animation-delay: 0.2s; }
.typing span:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce { 0%,60%,100%{transform:translateY(0)} 30%{transform:translateY(-6px)} }
.diagnose-bar { display: flex; gap: 8px; padding: 10px 16px; border-top: 1px solid rgba(0,120,200,0.08); }
.device-select { flex: 1; height: 36px; padding: 0 10px; background: rgba(0,20,50,0.7); border: 1px solid rgba(0,100,160,0.3); border-radius: 7px; color: #d0eaf8; font-size: 12px; outline: none; cursor: pointer; }
.device-select:focus { border-color: rgba(77,208,225,0.4); }
.diagnose-btn { padding: 7px 18px; background: linear-gradient(135deg, rgba(0,150,136,0.3), rgba(0,200,180,0.2)); border: 1px solid rgba(0,200,180,0.3); border-radius: 7px; color: rgba(150,240,230,0.9); font-size: 13px; cursor: pointer; white-space: nowrap; transition: all 0.2s; }
.diagnose-btn:hover:not(:disabled) { background: rgba(0,200,180,0.3); border-color: rgba(0,255,220,0.5); }
.diagnose-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.suggestions { padding: 10px 16px; display: flex; gap: 8px; flex-wrap: wrap; border-top: 1px solid rgba(0,80,140,0.1); }
.suggestion-chip {
  padding: 5px 12px; background: rgba(0,60,120,0.25);
  border: 1px solid rgba(0,120,200,0.25); border-radius: 20px;
  font-size: 12px; color: rgba(140,200,230,0.85); cursor: pointer; transition: all 0.2s;
}
.suggestion-chip:hover { background: rgba(0,120,200,0.2); color: #4dd0e1; border-color: rgba(77,208,225,0.4); }
.input-area { display: flex; gap: 10px; padding: 14px 16px; border-top: 1px solid rgba(0,80,140,0.1); }
.chat-input {
  flex: 1; height: 40px; padding: 0 14px;
  background: rgba(0,20,50,0.7); border: 1px solid rgba(0,100,160,0.3);
  border-radius: 8px; color: #d0eaf8; font-size: 13px; outline: none; transition: border-color 0.2s;
}
.chat-input:focus { border-color: rgba(77,208,225,0.4); }
.chat-input::placeholder { color: rgba(100,160,200,0.4); }
.chat-input:disabled { opacity: 0.6; }
.send-btn {
  width: 40px; height: 40px;
  background: linear-gradient(135deg, #0077cc, #0099e6);
  border: none; border-radius: 8px;
  color: #fff; cursor: pointer; transition: all 0.2s;
  display: flex; align-items: center; justify-content: center;
}
.send-btn svg { width: 17px; height: 17px; }
.send-btn:hover:not(:disabled) { transform: scale(1.05); box-shadow: 0 4px 12px rgba(0,150,230,0.4); }
.send-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.action-card {
  margin-top: 10px; padding: 10px 12px;
  background: rgba(0,60,120,0.3); border: 1px solid rgba(77,208,225,0.3);
  border-radius: 6px; border-left: 3px solid #4dd0e1;
}
.ac-title { font-weight: 600; color: #4dd0e1; font-size: 13px; display: flex; align-items: center; gap: 5px; margin-bottom: 4px; }
.ac-detail { font-size: 12px; color: rgba(200,230,245,0.8); line-height: 1.5; margin-left: 19px; }
.ac-highlight { color: #f0c040; font-weight: 600; }
.ac-link { display: inline-block; margin-top: 8px; font-size: 12px; color: #4dd0e1; text-decoration: none; transition: color 0.2s; }
.ac-link:hover { color: #80e8f0; text-decoration: underline; }
</style>
