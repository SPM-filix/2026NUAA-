import { http } from './request'

export const listSites = (params) => http.get('/sites', params)
export const createSite = (payload) => http.post('/admin/sites', payload)
export const updateSite = (siteId, payload) => http.put(`/admin/sites/${siteId}`, payload)
export const updateSiteStatus = (siteId, payload) =>
  http.patch(`/admin/sites/${siteId}/status`, payload)
