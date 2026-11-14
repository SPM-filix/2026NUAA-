import { http } from './request'

export const previewRedeem = (redeemCode) => http.get('/exchanges/lookup', { redeemCode })
export const redeem = (redeemCode) => http.post('/exchanges/redeem', { redeemCode })
export const listMyRedemptions = (params) => http.get('/recyclers/me/redemptions', params)

export const listOrders = (params) => http.get('/admin/exchanges', params)
export const getOrder = (orderId) => http.get(`/exchanges/${orderId}`)
export const cancelOrder = (orderId, payload) =>
  http.post(`/admin/exchanges/${orderId}/cancel`, payload)
