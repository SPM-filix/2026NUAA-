import { http } from './request'

export const listUsers = (params) => http.get('/admin/accounts/users', params)
export const setUserStatus = (userId, payload) =>
  http.patch(`/admin/accounts/users/${userId}/status`, payload)
export const resetUserPassword = (userId) =>
  http.post(`/admin/accounts/users/${userId}/password/reset`)

export const listRecyclers = (params) => http.get('/admin/accounts/recyclers', params)
export const approveRecycler = (recyclerId, payload) =>
  http.post(`/admin/accounts/recyclers/${recyclerId}/approve`, payload)
export const rejectRecycler = (recyclerId, payload) =>
  http.post(`/admin/accounts/recyclers/${recyclerId}/reject`, payload)
export const setRecyclerStatus = (recyclerId, payload) =>
  http.patch(`/admin/accounts/recyclers/${recyclerId}/status`, payload)
export const resetRecyclerPassword = (recyclerId) =>
  http.post(`/admin/accounts/recyclers/${recyclerId}/password/reset`)

export const adjustPoints = (payload) => http.post('/admin/points/adjust', payload)

export const listAppeals = (params) => http.get('/admin/appeals', params)
export const handleAppeal = (appealId, payload) =>
  http.post(`/admin/appeals/${appealId}/handle`, payload)
