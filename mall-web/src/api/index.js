import http from './http'

// ---------- 认证 ----------
export const login = (data) => http.post('/auth/login', data)
export const register = (data) => http.post('/auth/register', data)

// ---------- 商品 ----------
export const getCategories = () => http.get('/mall/categories')
export const getProducts = (params) => http.get('/mall/products', { params })
export const getProduct = (id) => http.get(`/mall/products/${id}`)

// ---------- 秒杀 ----------
export const getSeckillList = () => http.get('/mall/seckill/list')
export const getSeckillDetail = (id) => http.get(`/mall/seckill/${id}`)
export const getSeckillToken = (id, path) =>
  http.post(`/mall/seckill/${id}/token`, null, { params: { path } })
export const placeSeckillOrder = (data) => http.post('/mall/seckill/order', data)
export const getSeckillResult = (orderNo) =>
  http.get('/mall/seckill/result', { params: { orderNo } })
export const getMyActivityStatus = (id) => http.get(`/mall/seckill/${id}/mine`)

// ---------- 用户 ----------
export const getMe = () => http.get('/mall/user/me')
export const updateProfile = (data) => http.put('/mall/user/profile', data)
export const changePassword = (data) => http.put('/mall/user/password', data)

// ---------- 订单 ----------
export const getOrders = (params) => http.get('/mall/orders', { params })
export const getOrder = (id) => http.get(`/mall/orders/${id}`)
export const payOrder = (id) => http.post(`/mall/orders/${id}/pay`)
export const cancelOrder = (id) => http.post(`/mall/orders/${id}/cancel`)
export const applyRefund = (id, data) => http.post(`/mall/orders/${id}/refund`, data)
export const getOrderRefund = (id) => http.get(`/mall/orders/${id}/refund`)
