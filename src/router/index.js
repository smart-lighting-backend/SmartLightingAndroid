/**
 * Vue Router 配置
 * - /login          公开路由
 * - /dashboard 等需要登录 → 使用 MainLayout 布局
 * - beforeEach 守卫：检查 Token 是否存在
 */
import { createRouter, createWebHistory } from 'vue-router'
import { getToken, clearAuth, saveAuth, fetchCurrentUser } from '../api/auth.js'

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
        path: 'city/3d',
        name: 'SmartCity3D',
        component: () => import('../views/SmartCity3D.vue'),
        meta: { title: '3D可视化' },
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
router.beforeEach(async (to, from) => {
  const token = getToken()

  if (to.meta.public) {
    if (token && to.path === '/login') return '/dashboard'
    return true
  }

  if (!token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  const isMock = token.startsWith('mock-token-dev-')
  if (!isMock && from.path === '/login') {
    try {
      const res = await fetchCurrentUser()
      if (res?.data) {
        const fresh = { username: res.data.username, roleCode: res.data.roleCode }
        const inLocal = !!localStorage.getItem('smart_light_token')
        saveAuth(token, fresh, inLocal)
      }
    } catch {
      clearAuth()
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }

  return true
})

export default router