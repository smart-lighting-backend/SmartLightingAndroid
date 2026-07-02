/**
 * Vue Router 配置
 * - /login       公开路由
 * - /digital-twin、/reports、/ai-assistant、/system-logs 等需要登录（嵌套在 MainLayout 下）
 * - beforeEach 守卫：检查 Token
 */
import { createRouter, createWebHistory } from 'vue-router'
import { getToken, clearAuth } from '../api/auth.js'

// ─── 路由定义 ─────────────────────────────────────────────────────────────
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
  // ── 主应用壳（需要登录）──
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        redirect: '/digital-twin',
      },
      {
        path: 'digital-twin',
        name: 'DigitalTwin',
        component: () => import('../views/DigitalTwin.vue'),
        meta: { requiresAuth: true, title: '数字孪生' },
      },
      {
        path: 'devices',
        name: 'Devices',
        component: () => import('../views/DigitalTwin.vue'), // 占位
        meta: { requiresAuth: true, title: '设备管理' },
      },
      {
        path: 'reports',
        name: 'Reports',
        component: () => import('../views/Reports.vue'),
        meta: { requiresAuth: true, title: '数据报表' },
      },
      {
        path: 'alerts',
        name: 'Alerts',
        component: () => import('../views/SystemLogs.vue'), // 占位
        meta: { requiresAuth: true, title: '告警中心' },
      },
      {
        path: 'strategy',
        name: 'Strategy',
        component: () => import('../views/SystemLogs.vue'), // 占位
        meta: { requiresAuth: true, title: '策略配置' },
      },
      {
        path: 'ai-assistant',
        name: 'AiAssistant',
        component: () => import('../views/AiAssistant.vue'),
        meta: { requiresAuth: true, title: '智能助手' },
      },
      {
        path: 'system-logs',
        name: 'SystemLogs',
        component: () => import('../views/SystemLogs.vue'),
        meta: { requiresAuth: true, title: '系统日志' },
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
router.beforeEach(async (to, from, next) => {
  const token = getToken()

  // 公开路由直接放行；已登录时访问 /login 自动跳转
  if (to.meta.public) {
    if (token && to.path === '/login') {
      return next('/digital-twin')
    }
    return next()
  }

  // 受保护路由：无 Token 直接拦截
  if (!token) {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }

  return next()
})

export default router
