import { http } from './request'

export const listProducts = (params) => http.get('/products', params)
export const getProduct = (productId) => http.get(`/products/${productId}`)
export const createProduct = (payload) => http.post('/admin/products', payload)
export const updateProduct = (productId, payload) => http.put(`/admin/products/${productId}`, payload)
export const updateProductStatus = (productId, payload) =>
  http.patch(`/admin/products/${productId}/status`, payload)
export const adjustProductStock = (productId, payload) =>
  http.patch(`/admin/products/${productId}/stock`, payload)
export const deleteProduct = (productId) => http.delete(`/admin/products/${productId}`)
