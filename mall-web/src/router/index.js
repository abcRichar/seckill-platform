import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'

const routes = [
  {
    path: '/',
    component: MainLayout,
    children: [
      { path: '', name: 'home', component: () => import('@/views/Home.vue'), meta: { title: '首页' } },
      {
        path: 'products',
        name: 'products',
        component: () => import('@/views/ProductList.vue'),
        meta: { title: '全部商品' }
      },
      {
        path: 'product/:id',
        name: 'product-detail',
        component: () => import('@/views/ProductDetail.vue'),
        meta: { title: '商品详情' }
      },
      {
        path: 'seckill/:id',
        name: 'seckill-detail',
        component: () => import('@/views/SeckillDetail.vue'),
        meta: { title: '秒杀详情' }
      },
      {
        path: 'orders',
        name: 'orders',
        component: () => import('@/views/Orders.vue'),
        meta: { title: '我的订单', requiresAuth: true }
      },
      {
        path: 'order/:id',
        name: 'order-detail',
        component: () => import('@/views/OrderDetail.vue'),
        meta: { title: '订单详情', requiresAuth: true }
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人中心', requiresAuth: true }
      }
    ]
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', standalone: true }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册', standalone: true }
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · 秒杀集市` : '秒杀集市'
  if (to.meta.requiresAuth && !localStorage.getItem('mall_token')) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
