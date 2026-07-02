/**
 * Vue Router 配置
 * - /login   公开路由
 * - /dashboard、/devices 等需要登录
 * - beforeEach 守卫：检查 Token 并可选调用后端校验接口
 */
import { createRouter, createWebHistory } from 'vue-router'
import { getToken, clearAuth } from '../api/auth.js'
import request from '../api/request.js'

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
    // 占位页面，后续开发
    component: () => import('../views/HomeView.vue'),
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

  // 公开路由直接放行；已登录时访问 /login 自动跳转到 dashboard
  if (to.meta.public) {
    if (token && to.path === '/login') {
      return next('/dashboard')
    }
    return next()
  }

  // 受保护路由：无 Token 直接拦截
  if (!token) {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }

  // 可选：调用后端接口验证 Token 有效性（取消注释即可启用）
  // try {
  //   await request.get('/api/auth/check')
  // } catch {
  //   clearAuth()
  //   return next({ path: '/login', query: { redirect: to.fullPath } })
  // }

  return next()
})

export default router
