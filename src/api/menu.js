/**
 * 菜单管理 API
 *
 * GET    /api/menus/tree     → 完整菜单树（管理用）
 * GET    /api/menus/visible  → 用户可见菜单（导航用）
 * GET    /api/menus          → 扁平列表
 * POST   /api/menus          → 新增
 * PUT    /api/menus/{id}     → 修改
 * DELETE /api/menus/{id}     → 删除
 */
import request from './request.js'

/** 完整菜单树（管理用） */
export function fetchMenuTree() {
  return request.get('/api/menus/tree')
}

/** 用户可见菜单（导航用） */
export function fetchVisibleMenus() {
  return request.get('/api/menus/visible')
}

/** 扁平列表 */
export function fetchMenuList() {
  return request.get('/api/menus')
}

/** 新增菜单 */
export function createMenu(data) {
  return request.post('/api/menus', data)
}

/** 修改菜单 */
export function updateMenu(id, data) {
  return request.put(`/api/menus/${id}`, data)
}

/** 删除菜单 */
export function deleteMenu(id) {
  return request.delete(`/api/menus/${id}`)
}
