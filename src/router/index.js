/**
 * Vue Router 配置
 * - /login          公开路由
 * - /dashboard 等需要登录 → 使用 MainLayout 布局
 * - beforeEach 守卫：检查 Token 是否存在
 * - 登录后从 /me 刷新 permissions 和 menus
 */
import { createRouter, createWebHistory } from 'vue-router'
import { getToken, clearAuth, saveAuth, savePermissions, saveMenus, fetchCurrentUser, getUserInfo, refreshPermissionsAndMenus } from '../api/auth.js'

const routes = [
  {
    path: '/',
    redirect: '/login',
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { public: true },
  },
  // ── 登录后主布局 ─────────────────────────────────────────────────────────
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { title: '数字孪生' },
      },
      {
        path: 'devices',
        name: 'Devices',
        component: () => import('../views/Devices.vue'),
        meta: { title: '设备管理' },
      },
      {
        path: 'devices/:id',
        name: 'DeviceDetail',
        component: () => import('../views/DeviceDetail.vue'),
        meta: { title: '设备详情' },
      },
      {
        path: 'analytics',
        name: 'Analytics',
        component: () => import('../views/Analytics.vue'),
        meta: { title: '数据报表' },
      },
      {
        path: 'warning',
        name: 'Warning',
        component: () => import('../views/Warning.vue'),
        meta: { title: '告警中心' },
      },
      {
        path: 'strategy',
        name: 'Strategy',
        component: () => import('../views/Strategy.vue'),
        meta: { title: '策略配置' },
      },
      {
        path: 'strategy/create',
        name: 'StrategyCreate',
        component: () => import('../views/StrategyCreate.vue'),
        meta: { title: '新建策略' },
      },
      {
        path: 'strategy/edit/:id',
        name: 'StrategyEdit',
        component: () => import('../views/StrategyCreate.vue'),
        meta: { title: '编辑策略' },
      },
      {
        path: 'assistant',
        name: 'AIAssistant',
        component: () => import('../views/AIAssistant.vue'),
        meta: { title: '智能助手' },
      },
      {
        path: 'logs',
        name: 'SystemLog',
        component: () => import('../views/SystemLog.vue'),
        meta: { title: '系统日志' },
      },
      {
        path: 'users',
        name: 'UserManagement',
        component: () => import('../views/UserManagement.vue'),
        meta: { title: '用户管理' },
      },
      // ── 系统管理子页面（仅超级管理员可访问）────────────────────────────────
      {
        path: 'system/permission',
        name: 'PermissionManagement',
        component: () => import('../views/PermissionManagement.vue'),
        meta: { title: '权限管理', adminOnly: true },
      },
      {
        path: 'system/menu',
        name: 'MenuManagement',
        component: () => import('../views/MenuManagement.vue'),
        meta: { title: '菜单管理', adminOnly: true },
      },
      // ── 队友新增页面路由 ────────────────────────────────────────────────────
      {
        path: 'device/list',
        name: 'DeviceList',
        component: () => import('../views/DeviceList.vue'),
        meta: { title: '设备列表' },
      },
      {
        path: 'device/detail/:id',
        name: 'DeviceDetailV2',
        component: () => import('../views/DeviceDetail.vue'),
        meta: { title: '设备详情' },
      },
      {
        path: 'alarm/list',
        name: 'AlarmList',
        component: () => import('../views/AlarmList.vue'),
        meta: { title: '告警日志' },
      },
      {
        path: 'alarm/detail/:id',
        name: 'AlarmDetail',
        component: () => import('../views/AlarmDetail.vue'),
        meta: { title: '告警详情' },
      },
      {
        path: 'energy',
        name: 'EnergyTrend',
        component: () => import('../views/EnergyTrend.vue'),
        meta: { title: '能耗走势' },
      },
      {
        path: 'events',
        name: 'EventCenter',
        component: () => import('../views/EventCenter.vue'),
        meta: { title: '事件中心' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// ─── 导航守卫 ─────────────────────────────────────────────────────────────
let lastRefreshTime = 0
const REFRESH_INTERVAL = 30 * 1000 // 30 秒刷新一次权限

router.beforeEach(async (to, from) => {
  const token = getToken()

  if (to.meta.public) {
    if (token && to.path === '/login') return '/dashboard'
    return true
  }

  if (!token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  // 定期从 /me 刷新权限和菜单（30 秒间隔，确保角色权限变更后及时生效）
  if (Date.now() - lastRefreshTime > REFRESH_INTERVAL) {
    try {
      const res = await fetchCurrentUser()
      if (res?.data) {
        const { username, roleCode, permissions, menus } = res.data
        const inLocal = !!localStorage.getItem('smart_light_token')
        saveAuth(token, { username, roleCode }, inLocal)
        savePermissions(permissions || [], inLocal)
        saveMenus(menus || [], inLocal)
      }
      lastRefreshTime = Date.now()
    } catch {
      clearAuth()
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }

  // 管理员专属页面检查
  if (to.meta.adminOnly) {
    const user = getUserInfo()
    if (!user || user.roleCode !== 'SUPER_ADMIN') {
      console.warn('[Router] 非管理员尝试访问系统管理页面，已重定向')
      return { path: '/dashboard' }
    }
  }

  return true
})

export default router
