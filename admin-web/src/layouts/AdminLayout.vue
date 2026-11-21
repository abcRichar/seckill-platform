<template>
  <div class="admin">
    <aside class="sidebar" :class="{ collapsed }">
      <div class="logo">
        <span class="logo-mark"><el-icon><Lightning /></el-icon></span>
        <div v-show="!collapsed" class="logo-text">
          <strong>秒杀集市</strong>
          <em>ADMIN CONSOLE</em>
        </div>
      </div>

      <div v-show="!collapsed" class="menu-label">运营管理</div>
      <el-menu
        :default-active="route.path"
        router
        class="menu"
        background-color="transparent"
        text-color="#94a3b8"
        active-text-color="#ffffff"
      >
        <el-menu-item v-for="item in visibleMenus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-foot" v-show="!collapsed">
        <div class="foot-card">
          <span>⚡</span>
          <div>
            <strong>高并发秒杀</strong>
            <em>Redis · Lua · Stream</em>
          </div>
        </div>
        <p>v1.0.0 · 学习演示</p>
      </div>
    </aside>

    <div class="main" :class="{ wide: collapsed }">
      <header class="topbar">
        <div class="left">
          <button class="collapse-btn" @click="collapsed = !collapsed">
            <el-icon><Fold v-if="!collapsed" /><Expand v-else /></el-icon>
          </button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="right">
          <a href="http://localhost:5180" target="_blank" class="shop-link">
            <el-icon><Shop /></el-icon>
            查看商城
          </a>
          <span class="divider"></span>
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="user">
              <el-avatar :size="32" :src="adminStore.user?.avatar">
                {{ adminStore.nickname.slice(0, 1) }}
              </el-avatar>
              <div class="user-info">
                <strong>{{ adminStore.nickname }}</strong>
                <em>{{ adminStore.isSuper ? '超级管理员' : '运营人员' }}</em>
              </div>
              <el-icon class="caret"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="mall" @click="openMall">
                  <el-icon><Shop /></el-icon> 前台商城
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAdminStore } from '@/stores/admin'

const route = useRoute()
const router = useRouter()
const adminStore = useAdminStore()
const collapsed = ref(false)

const allMenus = [
  { path: '/dashboard', title: '数据看板', icon: 'Odometer', perm: 'dashboard:view' },
  { path: '/products', title: '商品管理', icon: 'Goods', perm: 'product:list' },
  { path: '/categories', title: '分类管理', icon: 'Grid', perm: 'category:manage' },
  { path: '/seckill', title: '秒杀活动', icon: 'Timer', perm: 'seckill:list' },
  { path: '/orders', title: '订单管理', icon: 'Tickets', perm: 'order:list' },
  { path: '/refunds', title: '退款审核', icon: 'Money', perm: 'refund:list' },
  { path: '/users', title: '用户管理', icon: 'User', perm: 'user:list' },
  { path: '/roles', title: '角色权限', icon: 'Key', perm: 'role:manage' },
  { path: '/logs', title: '操作日志', icon: 'Document', perm: 'log:list' }
]

const visibleMenus = computed(() => allMenus.filter((item) => adminStore.hasPerm(item.perm)))

function handleCommand(command) {
  if (command === 'logout') {
    adminStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } else if (command === 'mall') {
    window.open('http://localhost:5180', '_blank')
  }
}

onMounted(() => {
  adminStore.loadMe()
})
</script>

<style scoped>
.admin {
  min-height: 100vh;
  background: var(--bg);
}

.sidebar {
  position: fixed;
  inset: 0 auto 0 0;
  width: 232px;
  background:
    radial-gradient(360px 260px at 20% 0%, rgba(37, 99, 235, 0.2), transparent 60%),
    linear-gradient(180deg, var(--sidebar), #080d18);
  color: #94a3b8;
  display: flex;
  flex-direction: column;
  transition: width 0.25s;
  z-index: 20;
  overflow: hidden;
}

.sidebar.collapsed {
  width: 78px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 72px;
  padding: 0 20px;
  flex-shrink: 0;
}

.logo-mark {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  background: linear-gradient(145deg, #2563eb, #60a5fa);
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 20px;
  flex-shrink: 0;
  box-shadow: 0 8px 20px rgba(37, 99, 235, 0.35);
}

.logo-text {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
  white-space: nowrap;
}

.logo-text strong {
  color: #fff;
  font-size: 16px;
  letter-spacing: 1px;
}

.logo-text em {
  font-style: normal;
  font-size: 8px;
  letter-spacing: 2px;
  color: #546274;
  margin-top: 4px;
}

.menu-label {
  padding: 12px 26px 8px;
  font-size: 10px;
  letter-spacing: 2px;
  color: #475569;
}

.menu {
  border-right: none;
  padding: 0 12px;
  flex: 1;
  overflow-y: auto;
}

.menu :deep(.el-menu-item) {
  height: 44px;
  line-height: 44px;
  border-radius: 10px;
  margin-bottom: 4px;
  font-size: 13.5px;
}

.menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.05);
  color: #e2e8f0;
}

.menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: #fff;
  font-weight: 600;
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.28);
}

.menu :deep(.el-icon) {
  font-size: 17px;
}

.sidebar.collapsed .menu {
  padding: 0 10px;
}

.sidebar.collapsed .menu :deep(.el-menu-item) {
  justify-content: center;
  padding: 0 !important;
}

.sidebar-foot {
  padding: 16px 16px 20px;
}

.foot-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border-radius: 12px;
  background: rgba(37, 99, 235, 0.12);
  border: 1px solid rgba(59, 130, 246, 0.18);
}

.foot-card span {
  font-size: 20px;
}

.foot-card strong {
  display: block;
  color: #cbd5e1;
  font-size: 12px;
}

.foot-card em {
  display: block;
  font-style: normal;
  font-size: 10px;
  color: #64748b;
  margin-top: 3px;
}

.sidebar-foot p {
  font-size: 10px;
  color: #3f4b5f;
  text-align: center;
  margin: 14px 0 0;
  letter-spacing: 0.5px;
}

.main {
  margin-left: 232px;
  transition: margin-left 0.25s;
  min-height: 100vh;
}

.main.wide {
  margin-left: 78px;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 15;
  height: 64px;
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--line);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  width: 32px;
  height: 32px;
  border: 1px solid var(--line);
  background: #fff;
  border-radius: 8px;
  color: var(--ink-2);
  cursor: pointer;
  display: grid;
  place-items: center;
  transition: all 0.2s;
}

.collapse-btn:hover {
  color: var(--brand);
  border-color: #bfdbfe;
}

.el-breadcrumb {
  font-size: 13px;
}

.right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.shop-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--ink-2);
  transition: color 0.2s;
}

.shop-link:hover {
  color: var(--brand);
}

.divider {
  width: 1px;
  height: 22px;
  background: var(--line);
}

.user {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 4px 6px;
  border-radius: 10px;
  transition: background 0.2s;
}

.user:hover {
  background: #f1f5f9;
}

.user-info {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.user-info strong {
  font-size: 13px;
  font-weight: 600;
}

.user-info em {
  font-style: normal;
  font-size: 10px;
  color: var(--ink-3);
  margin-top: 2px;
}

.caret {
  font-size: 11px;
  color: var(--ink-3);
}

.content {
  padding: 0;
}

@media (max-width: 900px) {
  .sidebar {
    width: 78px;
  }
  .sidebar .logo-text,
  .sidebar .menu-label,
  .sidebar .sidebar-foot {
    display: none !important;
  }
  .sidebar .menu {
    padding: 0 10px;
  }
  .sidebar .menu :deep(.el-menu-item) {
    justify-content: center;
    padding: 0 !important;
  }
  .sidebar .menu :deep(.el-menu-item span) {
    display: none;
  }
  .main {
    margin-left: 78px;
  }
  .user-info,
  .shop-link {
    display: none;
  }
}
</style>
