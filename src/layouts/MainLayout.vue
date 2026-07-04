<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserInfo } from '../composables/useUserInfo.js'
import { clearAuth } from '../api/auth.js'
import ManualControlModal from '../components/ManualControlModal.vue'

const route  = useRoute()
const router = useRouter()
const { username } = useUserInfo()

const showManual = ref(false)
const showUserMenu = ref(false)
const showSettings = ref(false)

const navItems = [
  { name: 'dashboard',       label: '数字孪生',  icon: 'grid',     path: '/dashboard' },
  { name: 'devices',         label: '设备管理',  icon: 'bulb',     path: '/devices' },
  { name: 'analytics',       label: '数据报表',  icon: 'chart',    path: '/analytics' },
  { name: 'energy-trend',   label: '能耗走势',  icon: 'energy',   path: '/energy-trend' },
  { name: 'warning',         label: '告警中心',  icon: 'warning',  path: '/warning' },
  { name: 'strategy',        label: '策略配置',  icon: 'strategy', path: '/strategy' },
  { name: 'assistant',       label: '智能助手',  icon: 'robot',    path: '/assistant' },
  { name: 'logs',            label: '系统日志',  icon: 'history',  path: '/logs' },
  { name: 'users',           label: '用户管理',  icon: 'user',     path: '/users' },
]

const activeNav = computed(() => {
  const p = route.path
  if (p.startsWith('/devices')) return 'devices'
  if (p.startsWith('/strategy')) return 'strategy'
  const match = navItems.find(n => p === n.path)
  return match?.name || 'dashboard'
})

function logout() {
  clearAuth()
  router.push('/login')
}
</script>

<template>
  <div class="main-layout">
    <!-- ═══ 左侧导航栏 ═══════════════════════════════════════════ -->
    <aside class="sidebar">
      <div class="sidebar-brand">
        <div class="brand-logo">
          <svg viewBox="0 0 24 24" fill="none"><path d="M12 2C8.13 2 5 5.13 5 9c0 2.38 1.19 4.47 3 5.74V17c0 .55.45 1 1 1h6c.55 0 1-.45 1-1v-2.26c1.81-1.27 3-3.36 3-5.74 0-3.87-3.13-7-7-7z" fill="currentColor"/><path d="M9 21c0 .55.45 1 1 1h4c.55 0 1-.45 1-1v-1H9v1z" fill="currentColor" opacity="0.7"/></svg>
        </div>
        <div class="brand-text">
          <span class="brand-title">智慧路灯管理</span>
          <span class="brand-sub">智能节能系统</span>
        </div>
      </div>

      <nav class="sidebar-nav">
        <router-link
          v-for="item in navItems"
          :key="item.name"
          :to="item.path"
          class="nav-item"
          :class="{ active: activeNav === item.name }"
        >
          <!-- Grid icon -->
          <svg v-if="item.icon === 'grid'" class="nav-icon" viewBox="0 0 24 24" fill="none">
            <rect x="3" y="3" width="7" height="7" rx="1" fill="currentColor" opacity="0.85"/>
            <rect x="14" y="3" width="7" height="7" rx="1" fill="currentColor" opacity="0.85"/>
            <rect x="3" y="14" width="7" height="7" rx="1" fill="currentColor" opacity="0.85"/>
            <rect x="14" y="14" width="7" height="7" rx="1" fill="currentColor" opacity="0.85"/>
          </svg>
          <!-- Bulb icon -->
          <svg v-else-if="item.icon === 'bulb'" class="nav-icon" viewBox="0 0 24 24" fill="none">
            <path d="M12 2C8.13 2 5 5.13 5 9c0 2.38 1.19 4.47 3 5.74V17c0 .55.45 1 1 1h6c.55 0 1-.45 1-1v-2.26c1.81-1.27 3-3.36 3-5.74 0-3.87-3.13-7-7-7z" fill="currentColor" opacity="0.85"/>
            <path d="M9 21c0 .55.45 1 1 1h4c.55 0 1-.45 1-1v-1H9v1z" fill="currentColor" opacity="0.6"/>
          </svg>
          <!-- Chart icon -->
          <svg v-else-if="item.icon === 'chart'" class="nav-icon" viewBox="0 0 24 24" fill="none">
            <rect x="3" y="12" width="4" height="9" rx="1" fill="currentColor" opacity="0.6"/>
            <rect x="10" y="7" width="4" height="14" rx="1" fill="currentColor" opacity="0.8"/>
            <rect x="17" y="3" width="4" height="18" rx="1" fill="currentColor"/>
          </svg>
          <!-- Warning icon -->
          <!-- Energy icon -->
          <svg v-else-if="item.icon === 'energy'" class="nav-icon" viewBox="0 0 24 24" fill="none">
            <path d="M3 20l3-6h3L6 4h2l6 10h-3l2 6H7l-4-8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            <circle cx="20" cy="18" r="2" stroke="currentColor" stroke-width="1.2"/>
            <path d="M20 7v7" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
          <!-- Warning icon -->
          <svg v-else-if="item.icon === 'warning'" class="nav-icon" viewBox="0 0 24 24" fill="none">
            <path d="M12 2L2 20h20L12 2z" fill="currentColor" opacity="0.2" stroke="currentColor" stroke-width="1.5"/>
            <path d="M12 9v5M12 16.5v.5" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
          <!-- Strategy icon -->
          <svg v-else-if="item.icon === 'strategy'" class="nav-icon" viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="3" fill="currentColor"/>
            <path d="M12 2v3M12 19v3M2 12h3M19 12h3M5.64 5.64l2.12 2.12M16.24 16.24l2.12 2.12M5.64 18.36l2.12-2.12M16.24 7.76l2.12-2.12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
          <!-- Robot icon -->
          <svg v-else-if="item.icon === 'robot'" class="nav-icon" viewBox="0 0 24 24" fill="none">
            <rect x="4" y="8" width="16" height="12" rx="2" fill="currentColor" opacity="0.2" stroke="currentColor" stroke-width="1.5"/>
            <circle cx="9" cy="13" r="1.5" fill="currentColor"/>
            <circle cx="15" cy="13" r="1.5" fill="currentColor"/>
            <path d="M9 17h6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            <path d="M12 8V5M10 5h4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
          <!-- History icon -->
          <svg v-else-if="item.icon === 'history'" class="nav-icon" viewBox="0 0 24 24" fill="none">
            <path d="M12 8v4l3 3M3 12a9 9 0 1 0 18 0A9 9 0 0 0 3 12z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <!-- User icon -->
          <svg v-else-if="item.icon === 'user'" class="nav-icon" viewBox="0 0 24 24" fill="none">
            <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2M12 11a4 4 0 100-8 4 4 0 000 8z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          </svg>

          <span class="nav-label">{{ item.label }}</span>
          <span v-if="item.name === 'warning'" class="nav-badge">3</span>
        </router-link>
      </nav>

      <!-- 底部用户信息 -->
      <div class="sidebar-user" @click="showUserMenu = !showUserMenu">
        <div class="user-avatar">
          <svg viewBox="0 0 24 24" fill="none"><path d="M12 12c2.7 0 4-1.8 4-4s-1.3-4-4-4-4 1.8-4 4 1.3 4 4 4zm0 2c-2.67 0-8 1.34-8 4v1a1 1 0 001 1h14a1 1 0 001-1v-1c0-2.66-5.33-4-8-4z" fill="currentColor"/></svg>
        </div>
        <div class="user-info">
          <span class="user-name">{{ username }}</span>
          <span class="user-role">系统管理员</span>
        </div>
        <transition name="fade-up">
          <div v-if="showUserMenu" class="user-menu">
            <div class="user-menu-item" @click.stop="logout">
              <svg viewBox="0 0 24 24" fill="none"><path d="M16 17l5-5-5-5M21 12H9M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
              退出登录
            </div>
          </div>
        </transition>
      </div>
    </aside>

    <!-- ═══ 右侧主体 ════════════════════════════════════════════ -->
    <div class="main-body">
      <!-- 顶部导航栏 -->
      <header class="top-bar">
        <div class="top-bar-left">
          <span class="top-brand">智慧路灯节能系统</span>
          <nav class="top-nav">
            <router-link to="/dashboard" class="top-nav-link" :class="{ active: activeNav === 'dashboard' || activeNav === 'analytics' }">实时监控</router-link>
            <router-link to="/analytics" class="top-nav-link" :class="{ active: activeNav === 'analytics' }">能耗看板</router-link>
          </nav>
        </div>
        <div class="top-bar-right">
          <button class="manual-btn" @click="showManual = true">
            <svg viewBox="0 0 24 24" fill="none"><path d="M4 6h16M4 12h10M4 18h7" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
            手动控制
          </button>
          <button class="icon-btn" @click="$router.push('/warning')">
            <svg viewBox="0 0 24 24" fill="none"><path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 01-3.46 0" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
            <span class="badge-dot">3</span>
          </button>
          <button class="icon-btn" @click="showSettings = !showSettings">
            <svg viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="1.5"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z" stroke="currentColor" stroke-width="1.5"/></svg>
            <transition name="fade-up">
              <div v-if="showSettings" class="settings-menu">
                <div class="settings-menu-item" @click.stop="router.push('/strategy')">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>
                  策略配置
                </div>
                <div class="settings-menu-item" @click.stop="showSettings = false">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 01-2.83 2.83l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z"/></svg>
                  系统设置
                </div>
              </div>
            </transition>
          </button>
          <button class="icon-btn avatar-btn" @click="showUserMenu = !showUserMenu">
            <svg viewBox="0 0 24 24" fill="none"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2M12 11a4 4 0 100-8 4 4 0 000 8z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
          </button>
        </div>
      </header>

      <!-- 页面内容 -->
      <main class="page-content">
        <RouterView />
      </main>
    </div>

    <!-- 手动控制弹窗 -->
    <ManualControlModal v-if="showManual" @close="showManual = false" />
  </div>
</template>

<style scoped>
/* ─── 整体布局 ─────────────────────────────────────────────────────────── */
.main-layout {
  display: flex;
  width: 100vw;
  height: 100vh;
  background: #060e1f;
  overflow: hidden;
}

/* ─── 侧边栏 ───────────────────────────────────────────────────────────── */
.sidebar {
  width: 200px;
  min-width: 200px;
  height: 100vh;
  background: linear-gradient(180deg, #081428 0%, #060e1f 100%);
  border-right: 1px solid rgba(0, 120, 200, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
  z-index: 10;
}

/* 品牌区 */
.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 16px 16px;
  border-bottom: 1px solid rgba(0, 120, 200, 0.1);
}
.brand-logo {
  width: 36px; height: 36px;
  background: linear-gradient(135deg, rgba(0, 150, 220, 0.3), rgba(0, 80, 160, 0.5));
  border: 1px solid rgba(77, 208, 225, 0.3);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.brand-logo svg { width: 20px; height: 20px; color: #4dd0e1; }
.brand-text { display: flex; flex-direction: column; }
.brand-title { font-size: 13px; font-weight: 700; color: #e0f4ff; line-height: 1.3; }
.brand-sub { font-size: 10px; color: rgba(120, 180, 220, 0.6); margin-top: 1px; }

/* 导航 */
.sidebar-nav {
  flex: 1;
  padding: 12px 10px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  color: rgba(140, 190, 220, 0.7);
  text-decoration: none;
  font-size: 13px;
  transition: all 0.2s ease;
  position: relative;
}
.nav-item:hover {
  background: rgba(0, 120, 200, 0.12);
  color: rgba(180, 220, 240, 0.9);
}
.nav-item.active {
  background: rgba(0, 150, 220, 0.18);
  color: #4dd0e1;
  border: 1px solid rgba(77, 208, 225, 0.2);
}
.nav-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 20%;
  bottom: 20%;
  width: 3px;
  background: #4dd0e1;
  border-radius: 0 2px 2px 0;
}
.nav-icon { width: 16px; height: 16px; flex-shrink: 0; }
.nav-label { flex: 1; }
.nav-badge {
  background: #e53935;
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  min-width: 18px;
  height: 18px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
}

/* 底部用户 */
.sidebar-user {
  padding: 12px 16px;
  border-top: 1px solid rgba(0, 120, 200, 0.1);
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  position: relative;
  transition: background 0.2s;
}
.sidebar-user:hover { background: rgba(0,120,200,0.08); }
.user-avatar {
  width: 32px; height: 32px;
  background: rgba(0, 120, 200, 0.25);
  border: 1px solid rgba(77, 208, 225, 0.3);
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
}
.user-avatar svg { width: 18px; height: 18px; color: #4dd0e1; }
.user-info { display: flex; flex-direction: column; flex: 1; }
.user-name { font-size: 12px; font-weight: 600; color: #d0eaf8; }
.user-role { font-size: 10px; color: rgba(120, 180, 210, 0.55); margin-top: 1px; }

.user-menu {
  position: absolute;
  bottom: calc(100% + 4px);
  left: 10px; right: 10px;
  background: rgba(4, 20, 50, 0.96);
  border: 1px solid rgba(0, 120, 180, 0.4);
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(0,0,0,0.5);
  backdrop-filter: blur(16px);
  z-index: 100;
}
.user-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  font-size: 13px;
  color: rgba(180, 220, 240, 0.85);
  cursor: pointer;
  transition: background 0.2s;
}
.user-menu-item svg { width: 16px; height: 16px; }
.user-menu-item:hover { background: rgba(0,150,220,0.15); color: #4dd0e1; }

/* 设置下拉菜单 */
.settings-menu {
  position: absolute;
  top: calc(100% + 6px);
  right: 0;
  width: 160px;
  background: rgba(4, 20, 50, 0.96);
  border: 1px solid rgba(0, 120, 180, 0.4);
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(0,0,0,0.5);
  backdrop-filter: blur(16px);
  z-index: 100;
}
.settings-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  font-size: 13px;
  color: rgba(180, 220, 240, 0.85);
  cursor: pointer;
  transition: background 0.2s;
}
.settings-menu-item svg { width: 16px; height: 16px; }
.settings-menu-item:hover { background: rgba(0,150,220,0.15); color: #4dd0e1; }
.fade-up-enter-active, .fade-up-leave-active { transition: all 0.2s; }
.fade-up-enter-from, .fade-up-leave-to { opacity: 0; transform: translateY(6px); }

/* ─── 主体区域 ─────────────────────────────────────────────────────────── */
.main-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

/* ─── 顶部导航栏 ───────────────────────────────────────────────────────── */
.top-bar {
  height: 56px;
  min-height: 56px;
  background: rgba(6, 14, 31, 0.95);
  border-bottom: 1px solid rgba(0, 120, 200, 0.15);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  backdrop-filter: blur(12px);
  z-index: 9;
}
.top-bar-left { display: flex; align-items: center; gap: 32px; }
.top-brand {
  font-size: 15px;
  font-weight: 700;
  color: #d0eaf8;
  letter-spacing: 1px;
  white-space: nowrap;
}
.top-nav { display: flex; gap: 4px; }
.top-nav-link {
  padding: 5px 14px;
  border-radius: 6px;
  font-size: 13px;
  color: rgba(140, 190, 220, 0.7);
  text-decoration: none;
  transition: all 0.2s;
}
.top-nav-link:hover { color: #d0eaf8; background: rgba(0,120,200,0.1); }
.top-nav-link.active { color: #4dd0e1; background: rgba(0, 150, 220, 0.15); }

.top-bar-right { display: flex; align-items: center; gap: 8px; }
.manual-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 16px;
  background: linear-gradient(135deg, #0077cc, #0099e6);
  border: none;
  border-radius: 8px;
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  box-shadow: 0 2px 12px rgba(0, 150, 230, 0.3);
}
.manual-btn svg { width: 15px; height: 15px; }
.manual-btn:hover { transform: translateY(-1px); box-shadow: 0 4px 18px rgba(0, 150, 230, 0.5); }

.icon-btn {
  width: 36px; height: 36px;
  background: rgba(0, 80, 140, 0.2);
  border: 1px solid rgba(0, 120, 200, 0.2);
  border-radius: 8px;
  color: rgba(140, 190, 220, 0.8);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  transition: all 0.2s;
}
.icon-btn svg { width: 18px; height: 18px; }
.icon-btn:hover { background: rgba(0,120,200,0.25); color: #4dd0e1; }
.badge-dot {
  position: absolute;
  top: 4px; right: 4px;
  background: #e53935;
  color: #fff;
  font-size: 9px;
  font-weight: 700;
  min-width: 14px;
  height: 14px;
  border-radius: 7px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 2px;
  line-height: 1;
}

/* ─── 页面内容 ─────────────────────────────────────────────────────────── */
.page-content {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
}
</style>