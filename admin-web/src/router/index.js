import { createRouter, createWebHistory } from 'vue-router'
import AdminLayout from '@/layouts/AdminLayout.vue'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/Login.vue'),
    meta: { title: '登录', standalone: true }
  },
  {
    path: '/',
    component: AdminLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/dashboard/Dashboard.vue'),
        meta: { title: '数据看板', perm: 'dashboard:view', icon: 'Odometer' }
      },
      {
        path: 'products',
        name: 'products',
        component: () => import('@/views/product/ProductList.vue'),
        meta: { title: '商品管理', perm: 'product:list', icon: 'Goods' }
      },
      {
        path: 'categories',
        name: 'categories',
        component: () => import('@/views/category/CategoryList.vue'),
        meta: { title: '分类管理', perm: 'category:manage', icon: 'Grid' }
      },
      {
        path: 'seckill',
        name: 'seckill',
        component: () => import('@/views/seckill/ActivityList.vue'),
        meta: { title: '秒杀活动', perm: 'seckill:list', icon: 'Timer' }
      },
      {
        path: 'orders',
        name: 'orders',
        component: () => import('@/views/order/OrderList.vue'),
        meta: { title: '订单管理', perm: 'order:list', icon: 'Tickets' }
      },
      {
        path: 'refunds',
        name: 'refunds',
        component: () => import('@/views/refund/RefundList.vue'),
        meta: { title: '退款审核', perm: 'refund:list', icon: 'Money' }
      },
      {
        path: 'users',
        name: 'users',
        component: () => import('@/views/user/UserList.vue'),
        meta: { title: '用户管理', perm: 'user:list', icon: 'User' }
      },
      {
        path: 'roles',
        name: 'roles',
        component: () => import('@/views/role/RoleList.vue'),
        meta: { title: '角色权限', perm: 'role:manage', icon: 'Key' }
      },
      {
        path: 'logs',
        name: 'logs',
        component: () => import('@/views/log/LogList.vue'),
        meta: { title: '操作日志', perm: 'log:list', icon: 'Document' }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · 秒杀集市管理后台` : '秒杀集市管理后台'
  if (to.meta.perm) {
    const perms = JSON.parse(localStorage.getItem('admin_perms') || '[]')
    // 权限还没加载（刚登录完直接刷新）时先放行，由布局里的 /me 拉取后菜单再收敛
    if (perms.length && !perms.includes('*') && !perms.includes(to.meta.perm)) {
      return { path: '/dashboard' }
    }
  }
  if (!to.meta.standalone && !localStorage.getItem('admin_token')) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
