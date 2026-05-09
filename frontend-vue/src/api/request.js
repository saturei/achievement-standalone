import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

api.interceptors.request.use(config => {
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
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

export default api
