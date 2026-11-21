import { defineStore } from 'pinia'
import { getMe, login as loginApi, register as registerApi } from '@/api'

const TOKEN_KEY = 'mall_token'
const USER_KEY = 'mall_user'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  }),
  getters: {
    isLogin: (state) => Boolean(state.token),
    nickname: (state) => state.user?.nickname || state.user?.username || '未登录'
  },
  actions: {
    setAuth(data) {
      this.token = data.token
      this.user = {
        id: data.userId,
        username: data.username,
        nickname: data.nickname,
        avatar: data.avatar,
        roleType: data.roleType
      }
      localStorage.setItem(TOKEN_KEY, this.token)
      localStorage.setItem(USER_KEY, JSON.stringify(this.user))
    },
    async login(payload) {
      const data = await loginApi(payload)
      this.setAuth(data)
      return data
    },
    async register(payload) {
      const data = await registerApi(payload)
      this.setAuth(data)
      return data
    },
    async loadMe() {
      if (!this.token) return null
      try {
        const data = await getMe()
        this.user = data
        localStorage.setItem(USER_KEY, JSON.stringify(data))
        return data
      } catch (e) {
        this.logout()
        return null
      }
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    }
  }
})
