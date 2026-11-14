import { http } from './request'

export const loginAdmin = (payload) => http.post('/auth/admins/login', payload, { silent: true })
export const loginRecycler = (payload) => http.post('/auth/recyclers/login', payload, { silent: true })
export const logout = () => http.post('/auth/logout')
export const changePassword = (payload) => http.put('/auth/password', payload)
export const getAdminMe = () => http.get('/admins/me')
export const getRecyclerMe = () => http.get('/recyclers/me')
