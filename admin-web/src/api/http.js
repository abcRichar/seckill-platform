import axios from 'axios'
import { ElMessage } from 'element-plus'

const TOKEN_KEY = 'admin_token'

const http = axios.create({
  baseURL: '/api',
  timeout: 20000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

async function extractErrorBody(error) {
  const data = error.response?.data
  if (data instanceof Blob) {
    try {
      return JSON.parse(await data.text())
    } catch (e) {
      return null
    }
  }
  return data
}

http.interceptors.response.use(
  (response) => {
    if (response.config.responseType === 'blob') {
      return response.data
    }
    const body = response.data
    if (body && body.code >= 200 && body.code < 300) {
      return body.data
    }
    const message = body?.message || '请求失败'
    ElMessage.error(message)
    return Promise.reject(Object.assign(new Error(message), { code: body?.code, bizCode: body?.bizCode }))
  },
  async (error) => {
    const body = await extractErrorBody(error)
    const status = error.response?.status
    const code = body?.code
    const bizCode = body?.bizCode
    const message =
      body?.message ||
      (status ? `请求失败（HTTP ${status}）` : '网络异常，请检查后端是否启动')

    if (status === 401 || code === 401) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem('admin_user')
      localStorage.removeItem('admin_perms')
      ElMessage.error(message)
      if (!location.pathname.startsWith('/login')) {
        location.href = `/login?redirect=${encodeURIComponent(location.pathname + location.search)}`
      }
      return Promise.reject(Object.assign(new Error(message), { code: 401, bizCode, status: 401 }))
    }

    // 403 只提示，不跳登录；页面可能只是缺少某个权限
    ElMessage.error(message)
    return Promise.reject(Object.assign(new Error(message), { code, bizCode, status }))
  }
)

export default http
