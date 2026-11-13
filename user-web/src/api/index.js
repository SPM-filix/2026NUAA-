import request from './request'

export const authApi = {
  registerUser: (data) => request.post('/auth/users/register', data),
  loginUser: (data) => request.post('/auth/users/login', data),
  logout: () => request.post('/auth/logout'),
  changePassword: (data) => request.put('/auth/password', data),
}

export const userApi = {
  getMe: () => request.get('/users/me'),
  updateMe: (data) => request.put('/users/me', data),
  getQrCode: () => request.get('/users/me/qrcode'),
  getPoints: () => request.get('/users/me/points'),
  getPointsLogs: (params) => request.get('/users/me/points/logs', { params }),
  getDeliveries: (params) => request.get('/users/me/deliveries', { params }),
  getExchanges: (params) => request.get('/users/me/exchanges', { params }),
  getExchange: (orderId) => request.get(`/users/me/exchanges/${orderId}`),
  createAppeal: (data) => request.post('/users/me/appeals', data),
  getAppeals: (params) => request.get('/users/me/appeals', { params }),
}

/**
 * 契约中没有「用户端投递详情」接口（API-DLV-07 仅限回收员/管理员），
 * 这里通过本人投递列表逐页查找，保证刷新详情页也能正确展示。
 */
userApi.getDeliveryById = async (recordId) => {
  for (let page = 1; page <= 5; page += 1) {
    const data = await userApi.getDeliveries({ page, size: 50 })
    const rows = data?.list || []
    const hit = rows.find((item) => String(item.recordId) === String(recordId))
    if (hit) return hit
    if (rows.length < 50 || page * 50 >= (data?.total || 0)) break
  }
  const error = new Error('投递记录不存在或不属于本人')
  error.code = 3006
  throw error
}

export const mallApi = {
  getProducts: (params) => request.get('/products', { params }),
  getProduct: (productId) => request.get(`/products/${productId}`),
  exchange: (data) => request.post('/exchanges', data),
}

export const catalogApi = {
  getCategories: (params) => request.get('/categories', { params }),
}

export const contentApi = {
  getAnnouncements: (params) => request.get('/announcements', { params }),
  getAnnouncement: (announcementId) => request.get(`/announcements/${announcementId}`),
}

export const fileApi = {
  upload: (file, scene) => {
    const form = new FormData()
    form.append('file', file)
    form.append('scene', scene)
    return request.post('/files', form)
  },
}
