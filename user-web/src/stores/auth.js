import { defineStore } from 'pinia'
import { authApi, userApi } from '@/api'
import {
  clearSession,
  getMustChangePassword,
  getStoredUser,
  getToken,
  saveSession,
  saveUser,
} from '@/utils/session'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: getToken(),
    user: getStoredUser(),
    mustChangePassword: getMustChangePassword(),
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    balance: (state) => state.user?.pointsBalance ?? 0,
  },
  actions: {
    applySession({ token, user, mustChangePassword }) {
      if (token) this.token = token
      if (user) this.user = user
      if (mustChangePassword !== undefined) this.mustChangePassword = Boolean(mustChangePassword)
      saveSession({ token, user, mustChangePassword })
    },
    async login({ phone, password }) {
      const data = await authApi.loginUser({ phone, password })
      this.applySession({
        token: data.token,
        user: data.user || null,
        mustChangePassword: data.mustChangePassword,
      })
      // 临时密码登录只允许调用 PUT /api/auth/password，请求 /users/me 会失败，
      // 因此直接进入强制改密流程，避免 login 被 reject。
      if (!this.mustChangePassword) {
        await this.fetchMe()
      }
      return data
    },
    async register(payload) {
      const data = await authApi.registerUser(payload)
      this.applySession({ token: data.token, mustChangePassword: false })
      await this.fetchMe()
      return data
    },
    async fetchMe() {
      const user = await userApi.getMe()
      this.user = user
      saveUser(user)
      return user
    },
    async logout() {
      try {
        await authApi.logout()
      } finally {
        this.clear()
      }
    },
    clear() {
      this.token = ''
      this.user = null
      this.mustChangePassword = false
      clearSession()
    },
  },
})
