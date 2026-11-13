import { http } from './request'

export const lookupUser = (params) => http.get('/users/lookup', params)
export const createDelivery = (payload) => http.post('/deliveries', payload)
export const listMyDeliveries = (params) => http.get('/recyclers/me/deliveries', params)
export const revokeMyDelivery = (recordId, payload) =>
  http.post(`/deliveries/${recordId}/revoke`, payload)
export const getDelivery = (recordId) => http.get(`/deliveries/${recordId}`)

export const listAdminDeliveries = (params) => http.get('/admin/deliveries', params)
export const revokeDeliveryAsAdmin = (recordId, payload) =>
  http.post(`/admin/deliveries/${recordId}/revoke`, payload)
