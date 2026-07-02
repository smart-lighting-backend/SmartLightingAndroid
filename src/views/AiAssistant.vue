<script setup>
import { ref, nextTick, onMounted } from 'vue'

// ── 对话历史列表 ──────────────────────────────────────
const history = ref({
  today: [
    { id: 1, title: 'A区3号路灯离线原因排查', active: true },
    { id: 2, title: '节能策略优化建议生成',   active: false },
  ],
  yesterday: [
    { id: 3, title: '网关G-102电压异常分析',    active: false },
    { id: 4, title: '批量升级控制器固件流程',   active: false },
  ],
})

// ── 聊天消息列表 ──────────────────────────────────────
const messages = ref([
  {
    id: 1,
    role: 'user',
    content: '系统报A区3号路灯离线，请帮我分析可能的原因并提供排障步骤。',
    time: '今天 14:23',
  },
  {
    id: 2,
    role: 'ai',
    content: `针对 A区3号路灯（设备ID: LD-A-003）的离线告警，基于当前运行数据与历史拓扑，为您整理了以下结构化排障建议：

**1. 检查物理供电与线缆**

• 现场检查集中供电柜 A-1 的对应空开是否跳闸。

• 使用万用表测量路灯底部接线盒处的输入电压，正常应在 220V±10% 范围内。

**2. 通信模块状态排查**

• 如果采用 PLC（电力线载波）通信，请检查该段线缆是否存在严重的电磁干扰，或查看汇聚网关的信噪比数据。

• 如果采用 NB-IoT，请重启单灯控制器，观察指示灯是否能正常驻网（通常蓝灯常亮）。

**3. 远程下发重启指令`,
    time: '今天 14:23',
  },
])

const inputText  = ref('')
const chatBodyRef = ref(null)
const isLoading  = ref(false)
let msgId = 3

function selectHistory(item) {
  history.value.today.forEach(h => h.active = (h.id === item.id))
  history.value.yesterday.forEach(h => h.active = (h.id === item.id))
}

function newChat() {
  messages.value = []
  history.value.today.forEach(h => h.active = false)
  history.value.yesterday.forEach(h => h.active = false)
  inputText.value = ''
}

function scrollToBottom() {
  nextTick(() => {
    if (chatBodyRef.value) {
      chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
    }
  })
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || isLoading.value) return

  messages.value.push({ id: ++msgId, role: 'user', content: text, time: formatTime() })
  inputText.value = ''
  isLoading.value = true
  scrollToBottom()

  // 模拟 AI 回复延迟
  await new Promise(r => setTimeout(r, 1200))

  messages.value.push({
    id: ++msgId,
    role: 'ai',
    content: `已收到您关于「${text.slice(0, 20)}...」的咨询。\n\n正在分析设备运行数据及历史日志，请稍候...\n\n**分析结果**\n\n• 当前系统运行状态正常，未检测到关联告警。\n• 建议检查相关设备的实时数据看板以获取最新状态。`,
    time: formatTime(),
  })
  isLoading.value = false
  scrollToBottom()
}

function formatTime() {
  const now = new Date()
  return `今天 ${String(now.getHours()).padStart(2,'0')}:${String(now.getMinutes()).padStart(2,'0')}`
}

function handleKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

// 简单 Markdown 渲染（粗体/列表）
function renderMarkdown(text) {
  return text
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/^• (.+)$/gm, '<li>$1</li>')
    .replace(/(<li>.*<\/li>\n?)+/g, m => `<ul>${m}</ul>`)
    .replace(/\n/g, '<br/>')
}

onMounted(scrollToBottom)
</script>

<template>
  <div class="ai-page">
    <!-- 左侧历史侧边栏 -->
    <aside class="ai-sidebar">
      <button class="new-chat-btn" @click="newChat">
        <svg viewBox="0 0 24 24" fill="none"><line x1="12" y1="5" x2="12" y2="19" stroke="currentColor" stroke-width="2" stroke-linecap="round"/><line x1="5" y1="12" x2="19" y2="12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
        新建诊断对话
      </button>

      <div class="history-group">
        <div class="history-label">今天</div>
        <div
          v-for="item in history.today"
          :key="item.id"
          class="history-item"
          :class="{ active: item.active }"
          @click="selectHistory(item)"
        >
          <svg viewBox="0 0 24 24" fill="none"><rect x="3" y="5" width="18" height="14" rx="2" stroke="currentColor" stroke-width="1.5"/><path d="M8 10h8M8 14h5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
          <span>{{ item.title }}</span>
        </div>
      </div>

      <div class="history-group">
        <div class="history-label">昨天</div>
        <div
          v-for="item in history.yesterday"
          :key="item.id"
          class="history-item"
          :class="{ active: item.active }"
          @click="selectHistory(item)"
        >
          <svg viewBox="0 0 24 24" fill="none"><rect x="3" y="5" width="18" height="14" rx="2" stroke="currentColor" stroke-width="1.5"/><path d="M8 10h8M8 14h5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
          <span>{{ item.title }}</span>
        </div>
      </div>
    </aside>

    <!-- 聊天主区域 -->
    <div class="chat-area">
      <!-- 消息列表 -->
      <div class="chat-body" ref="chatBodyRef">
        <template v-for="(msg, i) in messages" :key="msg.id">
          <!-- 时间分割线（首条或跨分钟） -->
          <div v-if="i === 0 || messages[i-1].time !== msg.time" class="time-divider">
            {{ msg.time }}
          </div>

          <!-- 用户消息 -->
          <div v-if="msg.role === 'user'" class="msg-row user">
            <div class="msg-bubble user-bubble">{{ msg.content }}</div>
          </div>

          <!-- AI 消息 -->
          <div v-else class="msg-row ai">
            <div class="ai-avatar">
              <svg viewBox="0 0 24 24" fill="none"><rect x="3" y="7" width="18" height="13" rx="2" stroke="#4dd0e1" stroke-width="1.5"/><path d="M9 7V5a3 3 0 016 0v2" stroke="#4dd0e1" stroke-width="1.5" stroke-linecap="round"/><circle cx="9" cy="13" r="1" fill="#4dd0e1"/><circle cx="15" cy="13" r="1" fill="#4dd0e1"/></svg>
            </div>
            <div class="msg-bubble ai-bubble" v-html="renderMarkdown(msg.content)" />
          </div>
        </template>

        <!-- 加载中 -->
        <div v-if="isLoading" class="msg-row ai">
          <div class="ai-avatar">
            <svg viewBox="0 0 24 24" fill="none"><rect x="3" y="7" width="18" height="13" rx="2" stroke="#4dd0e1" stroke-width="1.5"/><path d="M9 7V5a3 3 0 016 0v2" stroke="#4dd0e1" stroke-width="1.5" stroke-linecap="round"/><circle cx="9" cy="13" r="1" fill="#4dd0e1"/><circle cx="15" cy="13" r="1" fill="#4dd0e1"/></svg>
          </div>
          <div class="msg-bubble ai-bubble loading-bubble">
            <span class="dot-1" /><span class="dot-2" /><span class="dot-3" />
          </div>
        </div>
      </div>

      <!-- 输入栏 -->
      <div class="input-area">
        <div class="input-box">
          <button class="attach-btn" title="附件">
            <svg viewBox="0 0 24 24" fill="none"><path d="M21.44 11.05l-9.19 9.19a6 6 0 01-8.49-8.49l9.19-9.19a4 4 0 015.66 5.66l-9.2 9.19a2 2 0 01-2.83-2.83l8.49-8.48" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </button>
          <textarea
            v-model="inputText"
            class="chat-input"
            placeholder="输入故障现象、设备ID或询问系统操作..."
            rows="1"
            @keydown="handleKeydown"
          />
          <button class="send-btn" :class="{ active: inputText.trim() }" @click="sendMessage">
            <svg viewBox="0 0 24 24" fill="none"><path d="M22 2L11 13M22 2L15 22l-4-9-9-4 20-7z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </button>
        </div>
        <div class="input-disclaimer">AI 助手可能生成不准确的信息，请在执行关键操作前验实物理状况。</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ai-page {
  display: flex;
  height: 100%;
  background: #060f1e;
  overflow: hidden;
}

/* ── 左侧侧边栏 ── */
.ai-sidebar {
  width: 220px;
  flex-shrink: 0;
  background: rgba(5, 15, 30, 0.8);
  border-right: 1px solid rgba(0, 150, 220, 0.1);
  display: flex;
  flex-direction: column;
  padding: 16px 12px;
  gap: 20px;
  overflow-y: auto;
}

.new-chat-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  height: 42px;
  background: rgba(0, 100, 180, 0.2);
  border: 1px dashed rgba(0, 150, 220, 0.4);
  border-radius: 10px;
  color: rgba(160, 210, 240, 0.85);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}
.new-chat-btn:hover {
  background: rgba(0, 130, 200, 0.3);
  border-color: rgba(77, 208, 225, 0.6);
  color: #4dd0e1;
}
.new-chat-btn svg { width: 16px; height: 16px; }

.history-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.history-label {
  font-size: 11px;
  color: rgba(100, 160, 200, 0.55);
  padding: 0 8px 6px;
  letter-spacing: 0.5px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 12px;
  color: rgba(150, 200, 230, 0.7);
  border: 1px solid transparent;
}
.history-item svg { width: 15px; height: 15px; flex-shrink: 0; opacity: 0.6; }
.history-item span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.history-item:hover {
  background: rgba(0, 80, 150, 0.2);
  color: rgba(200, 230, 255, 0.9);
}
.history-item.active {
  background: rgba(0, 100, 180, 0.25);
  border-color: rgba(0, 150, 220, 0.25);
  color: #c8e6ff;
}
.history-item.active svg { opacity: 1; }

/* ── 聊天主区域 ── */
.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 0 24px 0 0;
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  scroll-behavior: smooth;
}
.chat-body::-webkit-scrollbar { width: 4px; }
.chat-body::-webkit-scrollbar-track { background: transparent; }
.chat-body::-webkit-scrollbar-thumb { background: rgba(0,150,220,0.2); border-radius: 2px; }

.time-divider {
  text-align: center;
  font-size: 11px;
  color: rgba(100, 160, 200, 0.5);
  padding: 12px 0 8px;
}

.msg-row {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  margin-bottom: 12px;
}
.msg-row.user {
  justify-content: flex-end;
}
.msg-row.ai {
  justify-content: flex-start;
}

.msg-bubble {
  max-width: 68%;
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.65;
}

.user-bubble {
  background: linear-gradient(135deg, #0077cc, #00aaff);
  color: #fff;
  border-radius: 12px 12px 2px 12px;
  box-shadow: 0 2px 16px rgba(0, 150, 230, 0.35);
}

.ai-bubble {
  background: rgba(10, 30, 60, 0.8);
  border: 1px solid rgba(0, 150, 220, 0.2);
  color: rgba(210, 235, 255, 0.9);
  border-radius: 2px 12px 12px 12px;
}
.ai-bubble :deep(strong) { color: #4dd0e1; }
.ai-bubble :deep(ul) { padding-left: 14px; margin: 8px 0; }
.ai-bubble :deep(li) { margin: 4px 0; list-style: disc; }

.ai-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: rgba(0, 80, 150, 0.3);
  border: 1px solid rgba(77, 208, 225, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.ai-avatar svg { width: 18px; height: 18px; }

/* 打点动画 */
.loading-bubble {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 14px 18px;
}
.loading-bubble span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: rgba(77, 208, 225, 0.7);
  animation: bounce 1.2s ease-in-out infinite;
}
.dot-2 { animation-delay: 0.2s !important; }
.dot-3 { animation-delay: 0.4s !important; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0.7); opacity: 0.5; }
  40%            { transform: scale(1);   opacity: 1;   }
}

/* ── 输入栏 ── */
.input-area {
  flex-shrink: 0;
  padding: 12px 24px 16px;
}

.input-box {
  display: flex;
  align-items: center;
  gap: 10px;
  background: rgba(8, 25, 55, 0.8);
  border: 1px solid rgba(0, 120, 180, 0.35);
  border-radius: 14px;
  padding: 8px 10px 8px 14px;
  transition: border-color 0.2s;
}
.input-box:focus-within {
  border-color: rgba(77, 208, 225, 0.5);
  box-shadow: 0 0 0 3px rgba(77, 208, 225, 0.07);
}

.attach-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  color: rgba(100, 170, 220, 0.6);
  cursor: pointer;
  flex-shrink: 0;
  transition: color 0.2s;
}
.attach-btn:hover { color: #4dd0e1; }
.attach-btn svg { width: 18px; height: 18px; }

.chat-input {
  flex: 1;
  background: none;
  border: none;
  outline: none;
  resize: none;
  color: rgba(210, 235, 255, 0.9);
  font-size: 13px;
  line-height: 1.5;
  max-height: 120px;
  overflow-y: auto;
}
.chat-input::placeholder { color: rgba(100, 160, 200, 0.4); }

.send-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: rgba(0, 80, 150, 0.3);
  border: 1px solid rgba(0, 120, 180, 0.3);
  color: rgba(100, 170, 220, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.2s;
}
.send-btn.active {
  background: linear-gradient(135deg, #0077cc, #00aaff);
  border-color: transparent;
  color: #fff;
  box-shadow: 0 2px 12px rgba(0, 150, 230, 0.4);
}
.send-btn svg { width: 16px; height: 16px; }

.input-disclaimer {
  text-align: center;
  font-size: 11px;
  color: rgba(100, 150, 190, 0.45);
  margin-top: 8px;
}
</style>
