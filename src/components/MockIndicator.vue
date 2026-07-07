<script setup>
import { ref, computed } from 'vue'
import mockStore from '../utils/mockStore.js'

const expanded = ref(false)
const hidden = ref(false)

const isEmpty = computed(() => !mockStore.active)

const entries = computed(() =>
  Object.entries(mockStore.details).sort((a, b) => b[1] - a[1])
)

function toggle() {
  expanded.value = !expanded.value
}
</script>

<template>
  <div v-if="!hidden && !isEmpty" class="mock-indicator" :class="{ expanded }">
    <!-- 浮动球 -->
    <button class="mock-ball" @click="toggle" :title="`共 ${mockStore.total} 次 Mock 降级`">
      <svg viewBox="0 0 24 24" fill="none" width="16" height="16">
        <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"
          stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/>
      </svg>
      <span class="mock-count">{{ mockStore.total }}</span>
    </button>

    <!-- 展开面板 -->
    <div v-if="expanded" class="mock-panel">
      <div class="mock-panel-header">
        <span class="mock-panel-title">Mock 数据降级</span>
        <span class="mock-panel-hint">接口未对接后端，自动使用模拟数据</span>
      </div>
      <div class="mock-list">
        <div v-for="[ep, count] in entries" :key="ep" class="mock-item">
          <code class="mock-ep">{{ ep }}</code>
          <span class="mock-count-badge">{{ count }}次</span>
        </div>
      </div>
      <div class="mock-panel-footer">
        <button class="mock-close-btn" @click="hidden = true">我知道了</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.mock-indicator {
  position: fixed;
  bottom: 24px;
  left: 24px;
  z-index: 9999;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

.mock-ball {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 8px 14px;
  background: rgba(255, 167, 38, 0.15);
  border: 1px solid rgba(255, 167, 38, 0.4);
  border-radius: 20px;
  color: #ffa726;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  backdrop-filter: blur(8px);
  transition: all 0.2s;
  box-shadow: 0 2px 12px rgba(255, 167, 38, 0.15);
}
.mock-ball:hover {
  background: rgba(255, 167, 38, 0.25);
  box-shadow: 0 4px 20px rgba(255, 167, 38, 0.25);
  transform: translateY(-1px);
}
.mock-ball svg { flex-shrink: 0; }

.mock-panel {
  position: absolute;
  bottom: calc(100% + 10px);
  left: 0;
  width: 340px;
  background: rgba(10, 25, 55, 0.97);
  border: 1px solid rgba(255, 167, 38, 0.3);
  border-radius: 10px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
  overflow: hidden;
  animation: slideUp 0.2s ease;
}
@keyframes slideUp {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.mock-panel-header {
  padding: 12px 14px 8px;
  border-bottom: 1px solid rgba(255, 167, 38, 0.1);
}
.mock-panel-title {
  font-size: 13px;
  font-weight: 700;
  color: #ffa726;
  display: block;
  margin-bottom: 2px;
}
.mock-panel-hint {
  font-size: 11px;
  color: rgba(140, 190, 220, 0.5);
}

.mock-list {
  max-height: 200px;
  overflow-y: auto;
  padding: 6px 0;
}
.mock-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 14px;
  transition: background 0.15s;
}
.mock-item:hover { background: rgba(255, 167, 38, 0.05); }
.mock-ep {
  font-size: 11px;
  color: rgba(180, 220, 240, 0.8);
  font-family: 'Consolas', 'Courier New', monospace;
  word-break: break-all;
  flex: 1;
  margin-right: 8px;
}
.mock-count-badge {
  font-size: 10px;
  color: rgba(255, 167, 38, 0.8);
  background: rgba(255, 167, 38, 0.1);
  padding: 2px 7px;
  border-radius: 10px;
  white-space: nowrap;
  flex-shrink: 0;
}

.mock-panel-footer {
  padding: 8px 14px;
  border-top: 1px solid rgba(255, 167, 38, 0.1);
  text-align: right;
}
.mock-close-btn {
  font-size: 11px;
  padding: 4px 14px;
  background: rgba(255, 167, 38, 0.12);
  border: 1px solid rgba(255, 167, 38, 0.25);
  border-radius: 6px;
  color: rgba(255, 167, 38, 0.8);
  cursor: pointer;
  transition: all 0.2s;
}
.mock-close-btn:hover {
  background: rgba(255, 167, 38, 0.2);
  color: #ffa726;
}
</style>
