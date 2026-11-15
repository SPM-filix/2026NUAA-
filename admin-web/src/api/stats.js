import { http } from './request'

export const getOverview = () => http.get('/admin/stats/overview')
export const getCategoryStats = (params) => http.get('/admin/stats/categories', params)
export const getTrend = (days) => http.get('/admin/stats/trend', { days })
export const getTop = (type, limit = 5) => http.get('/admin/stats/top', { type, limit })
export const getWorkbench = () => http.get('/recyclers/me/workbench')
