<template>
  <div class="header-wrap">
    <div class="notice">
      <div class="container notice-inner">
        <span><el-icon><Lightning /></el-icon> 新用户注册即领 ¥50 秒杀券</span>
        <span class="notice-right">全场正品保障 · 极速发货 · 7 天无理由</span>
      </div>
    </div>

    <header class="header">
      <div class="container header-inner">
        <router-link to="/" class="brand">
          <span class="brand-mark"><el-icon><Lightning /></el-icon></span>
          <span class="brand-text">
            <strong>秒杀集市</strong>
            <em>SECKILL MARKET</em>
          </span>
        </router-link>

        <nav class="nav">
          <router-link to="/" class="nav-link" :class="{ active: route.path === '/' }">首页</router-link>
          <router-link
            to="/products"
            class="nav-link"
            :class="{ active: route.path.startsWith('/products') }"
            >全部商品</router-link
          >
          <a class="nav-link" href="/#seckill">秒杀专场</a>
        </nav>

        <div class="search">
          <el-icon class="search-icon"><Search /></el-icon>
          <input
            v-model="keyword"
            placeholder="搜索好物，如：耳机 / 手机"
            @keyup.enter="goSearch"
          />
          <button class="search-btn" @click="goSearch">搜索</button>
        </div>

        <div class="actions">
          <router-link to="/orders" class="action-icon">
            <el-icon><Tickets /></el-icon>
            <span>订单</span>
          </router-link>

          <el-dropdown v-if="userStore.isLogin" trigger="click" @command="handleCommand">
            <div class="user">
              <el-avatar :size="34" :src="userStore.user?.avatar">
                {{ userStore.nickname.slice(0, 1) }}
              </el-avatar>
              <span class="user-name">{{ userStore.nickname }}</span>
              <el-icon class="caret"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="orders">
                  <el-icon><Tickets /></el-icon> 我的订单
                </el-dropdown-item>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon> 个人中心
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <div v-else class="auth">
            <router-link to="/login" class="login-link">登录</router-link>
            <router-link to="/register">
              <el-button type="primary" round size="default">免费注册</el-button>
            </router-link>
          </div>
        </div>
      </div>
    </header>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const keyword = ref(route.query.keyword || '')

watch(
  () => route.query.keyword,
  (value) => {
    keyword.value = value || ''
  }
)

function goSearch() {
  router.push({ path: '/products', query: keyword.value ? { keyword: keyword.value } : {} })
}

function handleCommand(command) {
  if (command === 'orders') {
    router.push('/orders')
  } else if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/')
  }
}
</script>

<style scoped>
.header-wrap {
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(12px);
}

.notice {
  background: linear-gradient(90deg, #e0281f, #ff5a3c 55%, #ff9f1c);
  color: #fff;
  font-size: 12px;
  height: 32px;
  display: flex;
  align-items: center;
}

.notice-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.notice-inner span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  opacity: 0.95;
}

.notice-right {
  letter-spacing: 0.4px;
}

.header {
  background: rgba(255, 255, 255, 0.92);
  border-bottom: 1px solid var(--line);
  box-shadow: 0 6px 24px rgba(16, 24, 40, 0.04);
}

.header-inner {
  height: 76px;
  display: flex;
  align-items: center;
  gap: 26px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 11px;
  flex-shrink: 0;
}

.brand-mark {
  width: 42px;
  height: 42px;
  border-radius: 13px;
  background: linear-gradient(145deg, #ff3b30, #ff7a45);
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 22px;
  box-shadow: 0 8px 18px rgba(255, 59, 48, 0.32);
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.1;
  flex-shrink: 0;
}

.brand-text strong,
.brand-text em {
  white-space: nowrap;
}

.brand-text strong {
  font-size: 20px;
  letter-spacing: 1px;
}

.brand-text em {
  font-style: normal;
  font-size: 9px;
  letter-spacing: 2.4px;
  color: var(--ink-3);
  margin-top: 3px;
}

.nav {
  display: flex;
  align-items: center;
  gap: 22px;
  flex-shrink: 0;
}

.nav-link {
  font-size: 15px;
  color: var(--ink-2);
  position: relative;
  padding: 6px 0;
  transition: color 0.2s;
}

.nav-link::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -2px;
  height: 2px;
  border-radius: 2px;
  background: var(--brand);
  transform: scaleX(0);
  transition: transform 0.22s;
}

.nav-link:hover,
.nav-link.active {
  color: var(--ink);
  font-weight: 600;
}

.nav-link:hover::after,
.nav-link.active::after {
  transform: scaleX(1);
}

.search {
  flex: 1;
  max-width: 360px;
  height: 42px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 6px 0 14px;
  background: #f3f4f7;
  border: 1px solid transparent;
  border-radius: 999px;
  transition: all 0.2s;
}

.search:focus-within {
  background: #fff;
  border-color: var(--brand);
  box-shadow: 0 0 0 4px rgba(255, 59, 48, 0.08);
}

.search-icon {
  color: var(--ink-3);
  font-size: 17px;
}

.search input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  background: transparent;
  font-size: 14px;
  color: var(--ink);
}

.search input::placeholder {
  color: var(--ink-3);
}

.search-btn {
  border: none;
  height: 32px;
  padding: 0 16px;
  border-radius: 999px;
  background: var(--ink);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.search-btn:hover {
  background: var(--brand);
}

.actions {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-left: auto;
  flex-shrink: 0;
}

.action-icon {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 14px;
  color: var(--ink-2);
  transition: color 0.2s;
}

.action-icon:hover {
  color: var(--brand);
}

.user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 999px;
  transition: background 0.2s;
}

.user:hover {
  background: #f3f4f7;
}

.user-name {
  font-size: 14px;
  max-width: 90px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.caret {
  color: var(--ink-3);
  font-size: 12px;
}

.auth {
  display: flex;
  align-items: center;
  gap: 14px;
}

.login-link {
  font-size: 14px;
  color: var(--ink-2);
}

.login-link:hover {
  color: var(--brand);
}

@media (max-width: 1080px) {
  .nav {
    display: none;
  }
  .search {
    max-width: 300px;
  }
}

@media (max-width: 860px) {
  .notice-right,
  .search,
  .action-icon span {
    display: none;
  }
  .header-inner {
    height: 66px;
    gap: 14px;
  }
  .brand-text em {
    display: none;
  }
}
</style>
