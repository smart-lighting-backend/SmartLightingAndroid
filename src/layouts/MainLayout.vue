<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getUserInfo, clearAuth } from '../api/auth.js'

const router = useRouter()
const route  = useRoute()

const userInfo = ref(getUserInfo() || { username: 'Admin User', role: 'System Operator' })

const navItems = [
  { name: '数字孪生', path: '/digital-twin', icon: 'twin' },
  { name: '设备管理', path: '/devices',      icon: 'device' },
  { name: '数据报表', path: '/reports',      icon: 'report' },
  { name: '告警中心', path: '/alerts',       icon: 'alert' },
  { name: '策略配置', path: '/strategy',     icon: 'strategy' },
  { name: '智能助手', path: '/ai-assistant', icon: 'ai' },
  { name: '系统日志', path: '/system-logs',  icon: 'log' },
]

const isActive = (path) => route.path === path

function navigate(path) {
  router.push(path)
}

function handleLogout() {
  clearAuth()
  router.push('/login')
}

// 顶部 Tab（根据当前路由动态显示）
const topTabs = computed(() => {
  const p = route.path
  if (p === '/digital-twin') return ['数字孪生', '能耗看板']
  if (p === '/reports')      return ['实时监控', '能耗看板']
  if (p === '/system-logs')  return ['实时监控', '能耗看板']
  return []
})

const pageTitle = computed(() => {
  const item = navItems.find(n => n.path === route.path)
  return item?.name || '智慧路灯节能系统'
})
</script>

<template>
  <div class="app-shell">
    <!-- ── 左侧导航 ── -->
    <aside class="sidebar">
      <div class="sidebar-logo">
        <div class="logo-icon-wrap">
          <svg viewBox="0 0 24 24" fill="none" class="logo-svg">
            <circle cx="12" cy="12" r="10" stroke="#4dd0e1" stroke-width="1.5" opacity="0.4"/>
            <path d="M12 3C8.13 3 5 6.13 5 10c0 2.38 1.19 4.47 3 5.74V18c0 .55.45 1 1 1h6c.55 0 1-.45 1-1v-2.26c1.81-1.27 3-3.36 3-5.74 0-3.87-3.13-7-7-7z" fill="#4dd0e1" opacity="0.9"/>
            <path d="M9 21c0 .55.45 1 1 1h4c.55 0 1-.45 1-1v-1H9v1z" fill="#4dd0e1" opacity="0.7"/>
          </svg>
        </div>
        <div class="logo-text">
          <span class="logo-main">智慧路灯管理</span>
          <span class="logo-sub">智能节能系统</span>
        </div>
      </div>

      <nav class="sidebar-nav">
        <div
          v-for="item in navItems"
          :key="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
          @click="navigate(item.path)"
        >
          <!-- Icons -->
          <span class="nav-icon">
            <svg v-if="item.icon==='twin'" viewBox="0 0 24 24" fill="none"><rect x="3" y="3" width="8" height="8" rx="1.5" fill="currentColor" opacity="0.8"/><rect x="13" y="3" width="8" height="8" rx="1.5" fill="currentColor" opacity="0.5"/><rect x="3" y="13" width="8" height="8" rx="1.5" fill="currentColor" opacity="0.5"/><rect x="13" y="13" width="8" height="8" rx="1.5" fill="currentColor" opacity="0.3"/></svg>
            <svg v-else-if="item.icon==='device'" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="3" fill="currentColor"/><path d="M12 2a10 10 0 100 20A10 10 0 0012 2zm0 18a8 8 0 110-16 8 8 0 010 16z" fill="currentColor" opacity="0.6"/></svg>
            <svg v-else-if="item.icon==='report'" viewBox="0 0 24 24" fill="none"><rect x="3" y="3" width="18" height="18" rx="2" stroke="currentColor" stroke-width="1.5"/><path d="M7 14l3-3 3 3 4-5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
            <svg v-else-if="item.icon==='alert'" viewBox="0 0 24 24" fill="none"><path d="M12 2L2 20h20L12 2z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/><line x1="12" y1="9" x2="12" y2="13" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/><circle cx="12" cy="17" r="0.8" fill="currentColor"/></svg>
            <svg v-else-if="item.icon==='strategy'" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="1.5"/><path d="M12 2v2M12 20v2M2 12h2M20 12h2M5.64 5.64l1.41 1.41M16.95 16.95l1.41 1.41M5.64 18.36l1.41-1.41M16.95 7.05l1.41-1.41" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
            <svg v-else-if="item.icon==='ai'" viewBox="0 0 24 24" fill="none"><rect x="3" y="5" width="18" height="14" rx="2" stroke="currentColor" stroke-width="1.5"/><path d="M8 10h8M8 14h5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
            <svg v-else-if="item.icon==='log'" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.5"/><path d="M12 7v5l3 3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
          </span>
          <span class="nav-label">{{ item.name }}</span>
          <span v-if="isActive(item.path)" class="nav-active-bar" />
        </div>
      </nav>

      <div class="sidebar-user">
        <div class="user-avatar">
          <svg viewBox="0 0 24 24" fill="none"><circle cx="12" cy="8" r="4" fill="currentColor" opacity="0.8"/><path d="M4 20c0-4 3.58-7 8-7s8 3 8 7" fill="currentColor" opacity="0.5"/></svg>
        </div>
        <div class="user-info">
          <span class="user-name">{{ userInfo.username || 'Admin User' }}</span>
          <span class="user-role">System Operator</span>
        </div>
      </div>
    </aside>

    <!-- ── 主内容区 ── -->
    <div class="main-area">
      <!-- 顶部 Header -->
      <header class="top-header">
        <div class="header-left">
          <span class="header-title">智慧路灯节能系统</span>
          <nav class="header-tabs" v-if="topTabs.length">
            <a
              v-for="(tab, i) in topTabs"
              :key="tab"
              class="header-tab"
              :class="{ active: i === 0 }"
            >{{ tab }}</a>
          </nav>
        </div>
        <div class="header-right">
          <div class="search-box" v-if="route.path !== '/ai-assistant'">
            <svg viewBox="0 0 24 24" fill="none"><circle cx="11" cy="11" r="7" stroke="currentColor" stroke-width="1.5"/><path d="M16.5 16.5L21 21" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
            <input type="text" placeholder="搜索设备/区域..." />
          </div>
          <button class="manual-btn">
            <svg viewBox="0 0 24 24" fill="none"><path d="M12 2a10 10 0 110 20A10 10 0 0112 2z" stroke="currentColor" stroke-width="1.5"/><path d="M12 6v6l4 2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
            手动控制
          </button>
          <button class="icon-btn">
            <svg viewBox="0 0 24 24" fill="none"><path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 01-3.46 0" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
          </button>
          <button class="icon-btn">
            <svg viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="1.5"/><path d="M12 2v2M12 20v2M2 12h2M20 12h2M5.64 5.64l1.41 1.41M16.95 16.95l1.41 1.41M5.64 18.36l1.41-1.41M16.95 7.05l1.41-1.41" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
          </button>
          <button class="icon-btn user-btn" @click="handleLogout">
            <svg viewBox="0 0 24 24" fill="none"><circle cx="12" cy="8" r="4" stroke="currentColor" stroke-width="1.5"/><path d="M4 20c0-4 3.58-7 8-7s8 3 8 7" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
          </button>
        </div>
      </header>

      <!-- 页面内容 -->
      <main class="page-content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.app-shell {
  display: flex;
  width: 100vw;
  height: 100vh;
  background: #050d1a;
  overflow: hidden;
  font-family: 'PingFang SC', 'Microsoft YaHei', system-ui, sans-serif;
}

/* ── 侧边栏 ── */
.sidebar {
  width: 200px;
  flex-shrink: 0;
  background: #071525;
  border-right: 1px solid rgba(0, 150, 220, 0.12);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 16px 18px;
  border-bottom: 1px solid rgba(0, 150, 220, 0.1);
}

.logo-icon-wrap {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: rgba(0, 150, 220, 0.15);
  border: 1px solid rgba(77, 208, 225, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.logo-svg {
  width: 20px;
  height: 20px;
}

.logo-text {
  display: flex;
  flex-direction: column;
}

.logo-main {
  font-size: 13px;
  font-weight: 700;
  color: #e0f4ff;
  line-height: 1.3;
  letter-spacing: 0.5px;
}

.logo-sub {
  font-size: 10px;
  color: rgba(77, 208, 225, 0.6);
  letter-spacing: 0.3px;
}

/* 导航 */
.sidebar-nav {
  flex: 1;
  padding: 12px 8px;
  overflow-y: auto;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
  color: rgba(160, 200, 230, 0.7);
  margin-bottom: 2px;
  user-select: none;
}

.nav-item:hover {
  background: rgba(0, 150, 220, 0.1);
  color: rgba(200, 230, 255, 0.9);
}

.nav-item.active {
  background: rgba(0, 120, 200, 0.2);
  color: #4dd0e1;
  border: 1px solid rgba(0, 150, 220, 0.25);
}

.nav-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
}
.nav-icon svg { width: 100%; height: 100%; }

.nav-label {
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.3px;
}

.nav-active-bar {
  position: absolute;
  right: 0;
  top: 20%;
  bottom: 20%;
  width: 3px;
  background: #4dd0e1;
  border-radius: 2px 0 0 2px;
}

/* 用户信息 */
.sidebar-user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-top: 1px solid rgba(0, 150, 220, 0.1);
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(0, 150, 220, 0.2);
  border: 1px solid rgba(77, 208, 225, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #4dd0e1;
}
.user-avatar svg { width: 18px; height: 18px; }

.user-info {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.user-name {
  font-size: 12px;
  font-weight: 600;
  color: #c8e6ff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-role {
  font-size: 10px;
  color: rgba(100, 160, 200, 0.6);
}

/* ── 顶部 Header ── */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.top-header {
  height: 56px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: rgba(7, 21, 37, 0.95);
  border-bottom: 1px solid rgba(0, 150, 220, 0.12);
  gap: 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 24px;
}

.header-title {
  font-size: 14px;
  font-weight: 600;
  color: #e0f4ff;
  white-space: nowrap;
}

.header-tabs {
  display: flex;
  gap: 4px;
}

.header-tab {
  padding: 5px 14px;
  font-size: 13px;
  color: rgba(160, 200, 230, 0.65);
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s;
  text-decoration: none;
  border-bottom: 2px solid transparent;
}

.header-tab.active {
  color: #4dd0e1;
  border-bottom-color: #4dd0e1;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  height: 34px;
  background: rgba(0, 30, 60, 0.6);
  border: 1px solid rgba(0, 120, 180, 0.3);
  border-radius: 8px;
  color: rgba(100, 170, 210, 0.5);
  min-width: 180px;
}

.search-box svg {
  width: 15px;
  height: 15px;
  flex-shrink: 0;
}

.search-box input {
  background: none;
  border: none;
  outline: none;
  font-size: 12px;
  color: rgba(160, 210, 240, 0.8);
  width: 100%;
}
.search-box input::placeholder { color: rgba(100, 160, 200, 0.45); }

.manual-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 14px;
  height: 34px;
  background: rgba(0, 100, 180, 0.25);
  border: 1px solid rgba(0, 150, 220, 0.5);
  border-radius: 8px;
  color: #4dd0e1;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
}
.manual-btn:hover {
  background: rgba(0, 130, 200, 0.35);
  border-color: #4dd0e1;
}
.manual-btn svg { width: 14px; height: 14px; }

.icon-btn {
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 30, 60, 0.4);
  border: 1px solid rgba(0, 120, 180, 0.2);
  border-radius: 8px;
  color: rgba(160, 210, 240, 0.7);
  cursor: pointer;
  transition: all 0.2s;
}
.icon-btn:hover {
  background: rgba(0, 80, 150, 0.3);
  color: #4dd0e1;
  border-color: rgba(77, 208, 225, 0.4);
}
.icon-btn svg { width: 16px; height: 16px; }

/* ── 页面内容 ── */
.page-content {
  flex: 1;
  overflow: hidden;
}
</style>
