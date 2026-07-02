/**
 * 认证相关 API
 * 调用后端 /api/auth/login 接口，成功后将 Token 存入 localStorage
 */
import request from './request.js'

const TOKEN_KEY = 'smart_light_token'
const USER_KEY = 'smart_light_user'

/**
 * 登录接口
 * @param {string} username
 * @param {string} password
 * @param {string} role
 */
export function login(username, password, role) {
  return request.post('/api/auth/login', { username, password, role })
}

/**
 * 校验 Token 是否有效（调用后端接口）
 */
export function checkToken() {
  return request.get('/api/auth/check')
}

/**
 * 登出
 */
export function logout() {
  return request.post('/api/auth/logout').finally(() => {
    clearAuth()
  })
}

// ──────────────────────────── Token 工具函数 ────────────────────────────

/**
 * 保存认证信息
 * @param {string} token
 * @param {object} userInfo
 * @param {boolean} remember  是否记住账号（持久化到 localStorage）
 */
export function saveAuth(token, userInfo, remember = false) {
  const storage = remember ? localStorage : sessionStorage
  storage.setItem(TOKEN_KEY, token)
  storage.setItem(USER_KEY, JSON.stringify(userInfo))
}

/**
 * 获取当前 Token（优先 sessionStorage，再 localStorage）
 */
export function getToken() {
  return sessionStorage.getItem(TOKEN_KEY) || localStorage.getItem(TOKEN_KEY)
}

/**
 * 获取当前用户信息
 */
export function getUserInfo() {
  const raw = sessionStorage.getItem(USER_KEY) || localStorage.getItem(USER_KEY)
  try {
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

/**
 * 清除认证信息（双 storage 都清）
 */
export function clearAuth() {
  ;[localStorage, sessionStorage].forEach(s => {
    s.removeItem(TOKEN_KEY)
    s.removeItem(USER_KEY)
  })
}

/**
 * 是否已登录（仅判断 token 是否存在）
 */
export function isLoggedIn() {
  return !!getToken()
}
