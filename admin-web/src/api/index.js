import http from './http'

// ---------- 认证 ----------
export const adminLogin = (data) => http.post('/auth/admin/login', data)
export const getMe = () => http.get('/admin/auth/me')

// ---------- 工作台 ----------
export const getDashboard = () => http.get('/admin/dashboard')

// ---------- 商品 ----------
export const getProducts = (params) => http.get('/admin/products', { params })
export const getProduct = (id) => http.get(`/admin/products/${id}`)
export const createProduct = (data) => http.post('/admin/products', data)
export const updateProduct = (id, data) => http.put(`/admin/products/${id}`, data)
export const updateProductStatus = (id, status) =>
  http.put(`/admin/products/${id}/status`, null, { params: { status } })
export const deleteProduct = (id) => http.delete(`/admin/products/${id}`)
export const uploadImage = (file) => {
  const form = new FormData()
  form.append('file', file)
  return http.post('/admin/upload', form, { headers: { 'Content-Type': 'multipart/form-data' } })
}

// ---------- 分类 ----------
export const getAdminCategories = () => http.get('/admin/categories')
export const getPublicCategories = () => http.get('/mall/categories')
export const createCategory = (data) => http.post('/admin/categories', data)
export const updateCategory = (id, data) => http.put(`/admin/categories/${id}`, data)
export const deleteCategory = (id) => http.delete(`/admin/categories/${id}`)

// ---------- 秒杀 ----------
export const getActivities = (params) => http.get('/admin/seckill', { params })
export const getActivity = (id) => http.get(`/admin/seckill/${id}`)
export const createActivity = (data) => http.post('/admin/seckill', data)
export const updateActivity = (id, data) => http.put(`/admin/seckill/${id}`, data)
export const deleteActivity = (id) => http.delete(`/admin/seckill/${id}`)
export const publishActivity = (id) => http.put(`/admin/seckill/${id}/publish`)
export const stopActivity = (id) => http.put(`/admin/seckill/${id}/stop`)

// ---------- 订单 ----------
export const getOrders = (params) => http.get('/admin/orders', { params })
export const getOrder = (id) => http.get(`/admin/orders/${id}`)
export const cancelOrder = (id) => http.post(`/admin/orders/${id}/cancel`)
export const refundOrder = (id, data) => http.post(`/admin/orders/${id}/refund`, data)
export const exportOrders = (params) =>
  http.get('/admin/orders/export', { params, responseType: 'blob' })

// ---------- 退款审核 ----------
export const getRefunds = (params) => http.get('/admin/refunds', { params })
export const getRefundSummary = () => http.get('/admin/refunds/summary')
export const getRefund = (id) => http.get(`/admin/refunds/${id}`)
export const approveRefund = (id, data) => http.post(`/admin/refunds/${id}/approve`, data)
export const rejectRefund = (id, data) => http.post(`/admin/refunds/${id}/reject`, data)

// ---------- 用户 / 角色 / 日志 ----------
export const getUsers = (params) => http.get('/admin/users', { params })
export const updateUserStatus = (id, status) =>
  http.put(`/admin/users/${id}/status`, null, { params: { status } })
export const resetPassword = (id, password) =>
  http.put(`/admin/users/${id}/reset-password`, null, { params: { password } })
export const getUserRoles = (id) => http.get(`/admin/users/${id}/roles`)
export const assignUserRoles = (id, roleIds) => http.put(`/admin/users/${id}/roles`, roleIds)

export const getRoles = () => http.get('/admin/roles')
export const getMenus = () => http.get('/admin/roles/menus')
export const getRoleMenus = (id) => http.get(`/admin/roles/${id}/menus`)
export const createRole = (data) => http.post('/admin/roles', data)
export const updateRole = (id, data) => http.put(`/admin/roles/${id}`, data)
export const deleteRole = (id) => http.delete(`/admin/roles/${id}`)
export const assignRoleMenus = (id, menuIds) => http.put(`/admin/roles/${id}/menus`, { menuIds })

export const getLogs = (params) => http.get('/admin/logs', { params })
