/**
 * Vue Router 配置
 * - /login          公开路由
 * - /dashboard、/devices 等需要登录
 * - beforeEach 守卫：检查 Token 是否存在
 */
import { createRouter, createWebHistory } from 'vue-router'
import { getToken, clearAuth, saveAuth, fetchCurrentUser } from '../api/auth.js'

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
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('../views/HomeView.vue'),
    meta: { requiresAuth: true, title: '控制台' },
  },
  {
    path: '/devices',
    name: 'Devices',
    component: () => import('../views/HomeView.vue'), // 占位，后续替换
    meta: { requiresAuth: true, title: '设备管理' },
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

  // 公开路由：已登录时访问 /login 自动跳转到主页
  if (to.meta.public) {
    if (token && to.path === '/login') {
      return next('/dashboard')
    }
    return next()
  }

  // 受保护路由：无 Token 直接拦截，跳回登录页
  if (!token) {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }

  // 可选：调用后端接口校验 Token 有效性（Mock Token 跳过）
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
      // Token 已失效 → 清除并跳回登录页
      clearAuth()
      return next({ path: '/login', query: { redirect: to.fullPath } })
    }
  }

  return next()
})

export default router
