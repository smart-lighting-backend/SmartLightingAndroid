/**
 * 认证相关 API
 * 接口文档 V1.0 — 用户登录与鉴权（谭佩）
 *
 * POST /api/auth/login   → 登录，返回 { token, username, roleCode }
 * GET  /api/auth/me      → 获取当前用户信息 / 校验 Token
 */
import request from './request.js'

const TOKEN_KEY = 'smart_light_token'
const USER_KEY  = 'smart_light_user'

// ────────────────────────── 登录 / 登出 ──────────────────────────────────

/**
 * 用户登录
 * @param {string} username
 * @param {string} password
 * @returns {Promise<{ code, msg, data: { token, username, roleCode } }>}
 */
export function login(username, password) {
  return request.post('/api/auth/login', { username, password })
}

/**
 * 获取当前登录用户信息 / 校验 Token 有效性
 * @returns {Promise<{ code, msg, data: { token, username, roleCode } }>}
 */
export function fetchCurrentUser() {
  return request.get('/api/auth/me')
}

// ────────────────────────── Token 工具函数 ───────────────────────────────

/**
 * 保存认证信息
 * @param {string} token
 * @param {{ username: string, roleCode: string, roleName?: string }} userInfo
 * @param {boolean} remember  true → localStorage（持久），false → sessionStorage
 */
export function saveAuth(token, userInfo, remember = false) {
  const storage = remember ? localStorage : sessionStorage
  storage.setItem(TOKEN_KEY, token)
  storage.setItem(USER_KEY, JSON.stringify(userInfo))
}

/**
 * 获取 Token（优先 sessionStorage，再 localStorage）
 */
export function getToken() {
  return sessionStorage.getItem(TOKEN_KEY) || localStorage.getItem(TOKEN_KEY)
}

/**
 * 获取本地缓存的用户信息
 */
export function getUserInfo() {
  const raw = sessionStorage.getItem(USER_KEY) || localStorage.getItem(USER_KEY)
  try { return raw ? JSON.parse(raw) : null } catch { return null }
}

/**
 * 清除所有认证信息（双 storage 都清）
 */
export function clearAuth() {
  ;[localStorage, sessionStorage].forEach(s => {
    s.removeItem(TOKEN_KEY)
    s.removeItem(USER_KEY)
  })
}

/**
 * 是否已登录（仅判断 token 存在）
 */
export function isLoggedIn() {
  return !!getToken()
}

// ────────────────────────── 角色工具 ─────────────────────────────────────

/** 角色编码 → 中文名映射 */
export const ROLE_LABELS = {
  SUPER_ADMIN: '系统管理员',
  MUNICIPAL:   '市政人员',
  MAINTENANCE: '路灯管理员',
  EMERGENCY:   '安全应急员',
}

export function getRoleLabel(roleCode) {
  return ROLE_LABELS[roleCode] || roleCode || '未知角色'
}
