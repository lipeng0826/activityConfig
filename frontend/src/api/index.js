import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器：自动带上 token
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器：统一处理错误
api.interceptors.response.use(
  res => res.data,
  err => {
    const data = err.response?.data
    if (err.response?.status === 401) {
      localStorage.removeItem('token')
    }
    return Promise.reject(data || { message: '网络错误' })
  }
)

export const authApi = {
  login: (username, password) =>
    api.post('/auth/login', { username, password }),

  register: (username, password, nickname) =>
    api.post('/auth/register', { username, password, nickname }),

  getMe: () =>
    api.get('/auth/me')
}

export const categoryApi = {
  list: () => api.get('/categories'),
  getById: (id) => api.get(`/categories/${id}`),
  create: (data) => api.post('/categories', data),
  update: (id, data) => api.put(`/categories/${id}`, data),
  delete: (id) => api.delete(`/categories/${id}`)
}

export const bookApi = {
  page: (params) => api.get('/books', { params }),
  getById: (id) => api.get(`/books/${id}`),
  create: (data) => api.post('/books', data),
  update: (id, data) => api.put(`/books/${id}`, data),
  delete: (id) => api.delete(`/books/${id}`)
}

export const borrowApi = {
  borrow: (bookId) => api.post('/borrows', { bookId }),
  return: (id) => api.put(`/borrows/${id}/return`),
  renew: (id) => api.put(`/borrows/${id}/renew`),
  myBorrows: () => api.get('/borrows/my')
}

export const userApi = {
  page: (params) => api.get('/users', { params }),
  disable: (id) => api.put(`/users/${id}/disable`),
  enable: (id) => api.put(`/users/${id}/enable`),
  assignRoles: (id, roleIds) => api.put(`/users/${id}/roles`, { roleIds }),
  allRoles: () => api.get('/users/roles')
}

export const logApi = {
  page: (params) => api.get('/logs', { params })
}

export default api
