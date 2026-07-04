/**
 * composables/useUserInfo.js
 * 读取本地缓存的用户信息、权限列表、菜单树
 */
import { computed } from 'vue'
import { getUserInfo, getPermissions, getMenus } from '../api/auth.js'

export function useUserInfo() {
  const userInfo = computed(() => getUserInfo() || { username: 'Admin', roleCode: 'SUPER_ADMIN', roleName: '系统管理员' })
  const username = computed(() => userInfo.value?.username || 'Admin')
  const roleName = computed(() => userInfo.value?.roleName || '系统管理员')
  const permissions = computed(() => getPermissions())
  const menus = computed(() => getMenus())

  /**
   * 检查是否拥有指定权限
   * @param {string} code 权限编码，如 "device:create"
   */
  function hasPerm(code) {
    return permissions.value.includes(code)
  }

  return { userInfo, username, roleName, permissions, menus, hasPerm }
}
