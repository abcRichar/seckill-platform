import { defineStore } from 'pinia'
import { adminLogin, getMe } from '@/api'

const TOKEN_KEY = 'admin_token'
const USER_KEY = 'admin_user'

export const useAdminStore = defineStore('admin', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: JSON.parse(localStorage.getItem(USER_KEY) || 'null'),
    permissions: JSON.parse(localStorage.getItem('admin_perms') || '[]')
  }),
  getters: {
    isLogin: (state) => Boolean(state.token),
    nickname: (state) => state.user?.nickname || state.user?.username || '管理员',
    isSuper: (state) => state.permissions.includes('*')
  },
  actions: {
    async login(payload) {
      const data = await adminLogin(payload)
      this.token = data.token
      this.user = {
        id: data.userId,
        username: data.username,
        nickname: data.nickname,
        avatar: data.avatar,
        roleType: data.roleType
      }
      this.permissions = data.permissions || []
      localStorage.setItem(TOKEN_KEY, data.token)
      localStorage.setItem(USER_KEY, JSON.stringify(this.user))
      localStorage.setItem('admin_perms', JSON.stringify(this.permissions))
      return data
    },
    async loadMe() {
      if (!this.token) return null
      try {
        const data = await getMe()
        this.user = data
        this.permissions = data.permissions || []
        localStorage.setItem(USER_KEY, JSON.stringify(data))
        localStorage.setItem('admin_perms', JSON.stringify(this.permissions))
        return data
      } catch (e) {
        this.logout()
        return null
      }
    },
    hasPerm(perm) {
      if (!perm) return true
      return this.permissions.includes('*') || this.permissions.includes(perm)
    },
    logout() {
      this.token = ''
      this.user = null
      this.permissions = []
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      localStorage.removeItem('admin_perms')
    }
  }
})
