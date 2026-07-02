/**
 * Axios 请求封装
 * - 自动附加 Authorization: Bearer <token>
 * - 响应 401 时自动跳回登录页
 */
import axios from 'axios'
import { getToken, clearAuth } from './auth.js'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 10000,
})

// ─── 请求拦截器：注入 Token ───────────────────────────────────────────────
request.interceptors.request.use(
  config => {
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// ─── 响应拦截器：处理 401 ─────────────────────────────────────────────────
request.interceptors.response.use(
  response => response.data,
  error => {
    if (error.response?.status === 401) {
      clearAuth()
      // 避免循环引用，直接使用 window.location 跳转
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default request
