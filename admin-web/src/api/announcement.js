import { http } from './request'

export const listAdminAnnouncements = (params) => http.get('/admin/announcements', params)
export const createAnnouncement = (payload) => http.post('/admin/announcements', payload)
export const updateAnnouncement = (announcementId, payload) =>
  http.put(`/admin/announcements/${announcementId}`, payload)
export const updateAnnouncementStatus = (announcementId, payload) =>
  http.patch(`/admin/announcements/${announcementId}/status`, payload)
export const deleteAnnouncement = (announcementId) =>
  http.delete(`/admin/announcements/${announcementId}`)
