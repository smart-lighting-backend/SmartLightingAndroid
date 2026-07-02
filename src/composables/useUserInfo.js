/**
 * composables/useUserInfo.js
 * 读取本地缓存的用户信息（无响应式依赖，轻量工具函数）
 */
import { computed } from 'vue'
import { getUserInfo } from '../api/auth.js'

export function useUserInfo() {
  const userInfo = computed(() => getUserInfo() || { username: 'Admin', roleCode: 'SUPER_ADMIN', roleName: '系统管理员' })
  const username = computed(() => userInfo.value?.username || 'Admin')
  const roleName = computed(() => userInfo.value?.roleName || '系统管理员')
  return { userInfo, username, roleName }
}
