import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

api.interceptors.request.use(config => {
  // 优先使用 JWT Token
  const token = localStorage.getItem('dingtalk_token')
  if (token) {
    config.headers['Authorization'] = 'Bearer ' + token
    return config
  }
  // 降级：开发环境使用 X-Current-User（未登录时兼容旧逻辑）
  const stored = localStorage.getItem('currentUser')
  if (stored) {
    try {
      const user = JSON.parse(stored)
      if (user.username) {
        config.headers['X-Current-User'] = user.username
      }
    } catch (e) {}
  }
  return config
})

api.interceptors.response.use(
  response => response.data,
  error => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('dingtalk_token')
      localStorage.removeItem('currentUser')
      // 重定向到登录（如果在钉钉内则重新获取 authCode）
      window.location.reload()
    }
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

export default api
