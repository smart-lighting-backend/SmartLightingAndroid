import request from './request.js'

/**
 * 获取用户列表（分页、组合查询）
 */
export async function fetchUserList(query = {}) {
  try {
    const res = await request.post('/api/users/list', query)
    return res.data
  } catch (error) {
    throw error
  }
}

/**
 * 获取单个用户详情
 */
export async function fetchUserDetail(id) {
  try {
    const res = await request.get(`/api/users/${id}`)
    return res.data
  } catch (error) {
    throw error
  }
}

/**
 * 新增用户
 */
export async function createUser(data) {
  try {
    const res = await request.post('/api/users', data)
    return res
  } catch (error) {
    throw error
  }
}

/**
 * 更新用户
 */
export async function updateUser(id, data) {
  try {
    const res = await request.put(`/api/users/${id}`, data)
    return res
  } catch (error) {
    throw error
  }
}

/**
 * 删除用户
 */
export async function deleteUser(id) {
  try {
    const res = await request.delete(`/api/users/${id}`)
    return res
  } catch (error) {
    throw error
  }
}

/**
 * 获取所有角色列表
 */
export async function fetchAllRoles() {
  try {
    const res = await request.get('/api/users/roles')
    return res.data
  } catch (error) {
    throw error
  }
}
