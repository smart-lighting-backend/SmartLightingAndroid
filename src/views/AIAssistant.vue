<script setup>
import { ref, reactive } from 'vue'

const messages = ref([
  { role: 'assistant', text: '你好！我是智慧路灯节能系统的 AI 助手。我可以帮助你分析设备状态、解读能耗数据、诊断故障原因，或提供策略配置建议。请告诉我你的问题。' },
])
const input = ref('')
const loading = ref(false)

const suggestions = ['当前有哪些设备异常？', '如何优化节能策略？', '分析本周能耗趋势', '告警处理建议']

async function sendMessage(text) {
  const q = text || input.value.trim()
  if (!q) return
  messages.value.push({ role: 'user', text: q })
  input.value = ''
  loading.value = true
  await new Promise(r => setTimeout(r, 800 + Math.random() * 600))
  const replies = {
    '当前有哪些设备异常？': '检测到以下异常设备：\n① SL-ND-102（离线）- 心跳包丢失超过3次\n② SL-ND-530（告警）- 驱动温度偏高\n建议立即派技术人员检查 SL-ND-102 节点通信线路。',
    '如何优化节能策略？': '建议：\n• 深夜00:00-05:00开启30%亮度节能模式\n• 雨雾天气自动触发补光策略\n• 车流低峰期（02:00-06:00）降至20%亮度\n预计每月可额外节省 8-12% 能耗。',
    '分析本周能耗趋势': '本周能耗较上周降低 6.3%，主要节省来自深夜节能策略。周三因降雨触发补光策略，能耗略有上升。建议检查补光触发阈值，适当提高光照强度阈值以减少不必要触发。',
    '告警处理建议': '当前3条未处理告警中：\n• ALM-20231027-001（最高优先级）：电源模块故障，建议今日内更换\n• ALM-20231027-002：通信故障，建议检查网关连接\n• ALM-20231026-012：安全事件，已通知安保组',
  }
  const reply = replies[q] || `已收到您的问题："${q}"。根据当前系统数据分析，建议您查看设备管理页面获取最新状态，或检查策略配置是否需要调整。如需更详细分析，请提供具体设备ID。`
  messages.value.push({ role: 'assistant', text: reply })
  loading.value = false
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
          <div class="msg-bubble" style="white-space: pre-line">{{ msg.text }}</div>
        </div>
        <div v-if="loading" class="message assistant">
          <div class="msg-avatar"><svg viewBox="0 0 24 24" fill="none"><rect x="4" y="8" width="16" height="12" rx="2" fill="currentColor" opacity="0.6" stroke="currentColor" stroke-width="1.5"/></svg></div>
          <div class="msg-bubble typing"><span></span><span></span><span></span></div>
        </div>
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
</style>
