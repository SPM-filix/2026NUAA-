import { http } from './request'

export const listCategories = (params) => http.get('/categories', params)
export const createCategory = (payload) => http.post('/admin/categories', payload)
export const updateCategory = (categoryId, payload) =>
  http.put(`/admin/categories/${categoryId}`, payload)
export const deleteCategory = (categoryId) => http.delete(`/admin/categories/${categoryId}`)
export const updateCategoryStatus = (categoryId, payload) =>
  http.patch(`/admin/categories/${categoryId}/status`, payload)
export const setCategoryRule = (categoryId, payload) =>
  http.put(`/admin/categories/${categoryId}/rule`, payload)
export const listCategoryRules = (categoryId, params) =>
  http.get(`/admin/categories/${categoryId}/rules`, params)
