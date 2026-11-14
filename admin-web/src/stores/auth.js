import { defineStore } from 'pinia'
import { loadAuth, saveAuth, clearAuthStorage } from '@/utils/authStorage'
import {
  loginAdmin as apiLoginAdmin,
  loginRecycler as apiLoginRecycler,
  logout as apiLogout,
  changePassword as apiChangePassword,
  getAdminMe,
  getRecyclerMe
} from '@/api/auth'

export const useAuthStore = defineStore('auth', {
  state: () => {
    const cached = loadAuth() || {}
    return {
      token: cached.token || '',
      role: cached.role || '',
      profile: cached.profile || null,
      mustChangePassword: Boolean(cached.mustChangePassword),
      siteName: cached.siteName || ''
    }
  },

  getters: {
    isAdmin: (state) => state.role === 'ADMIN',
    isRecycler: (state) => state.role === 'RECYCLER',
    displayName: (state) =>
      state.role === 'ADMIN' ? state.profile?.username || '管理员' : state.profile?.name || '回收员',
    roleLabel: (state) => (state.role === 'ADMIN' ? '系统管理员' : '回收员'),
    homePath: (state) => (state.role === 'ADMIN' ? '/dashboard' : '/workbench')
  },

  actions: {
    persist() {
      saveAuth({
        token: this.token,
        role: this.role,
        profile: this.profile,
        mustChangePassword: this.mustChangePassword,
        siteName: this.siteName
      })
    },

    applySession(role, data) {
      this.token = data.token
      this.role = role
      this.mustChangePassword = Boolean(data.mustChangePassword)
      this.profile = role === 'ADMIN' ? data.admin : data.recycler
      this.siteName = role === 'RECYCLER' ? data.recycler.siteName || '' : ''
      this.persist()
    },

    async loginAsAdmin(credentials) {
      const data = await apiLoginAdmin(credentials)
      this.applySession('ADMIN', data)
      try {
        this.profile = await getAdminMe()
        this.persist()
      } catch (error) {
        this.clearSession()
        throw error
      }
      return data
    },

    async loginAsRecycler(credentials) {
      const data = await apiLoginRecycler(credentials)
      this.applySession('RECYCLER', data)
      try {
        this.profile = await getRecyclerMe()
        this.siteName = this.profile.siteName || ''
        this.persist()
      } catch (error) {
        this.clearSession()
        throw error
      }
      return data
    },

    async refreshProfile() {
      if (this.role === 'ADMIN') {
        this.profile = await getAdminMe()
      } else if (this.role === 'RECYCLER') {
        this.profile = await getRecyclerMe()
        this.siteName = this.profile.siteName || ''
      } else {
        throw new Error('未知角色')
      }
      this.persist()
    },

    async changePassword(payload) {
      await apiChangePassword(payload)
    },

    clearSession() {
      this.token = ''
      this.role = ''
      this.profile = null
      this.mustChangePassword = false
      this.siteName = ''
      clearAuthStorage()
    },

    async logout() {
      try {
        await apiLogout()
      } catch {
        // 令牌已失效也要清理本地状态
      }
      this.clearSession()
    }
  }
})
