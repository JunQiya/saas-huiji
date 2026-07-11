import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useMemberStore } from '@/stores/member'

// 路由表
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/wx-login',
    name: 'WxLogin',
    component: () => import('@/views/WxLogin.vue'),
    meta: { public: true, title: '登录中' }
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { title: '会员卡' }
  },
  {
    path: '/coupons',
    name: 'MyCoupons',
    component: () => import('@/views/MyCoupons.vue'),
    meta: { title: '我的券' }
  },
  {
    path: '/coupon-center',
    name: 'CouponCenter',
    component: () => import('@/views/CouponCenter.vue'),
    meta: { title: '领券中心' }
  },
  {
    path: '/transactions',
    name: 'Transactions',
    component: () => import('@/views/Transactions.vue'),
    meta: { title: '消费记录' }
  },
  {
    path: '/stores',
    name: 'Stores',
    component: () => import('@/views/Stores.vue'),
    meta: { title: '附近门店' }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue'),
    meta: { title: '我的' }
  },
  {
    path: '/mall',
    name: 'Mall',
    component: () => import('@/views/Mall.vue'),
    meta: { title: '线上商城', public: true }
  },
  {
    path: '/mall/product/:id',
    name: 'MallProduct',
    component: () => import('@/views/MallProduct.vue'),
    meta: { title: '商品详情', public: true }
  },
  {
    path: '/mall/cart',
    name: 'MallCart',
    component: () => import('@/views/MallCart.vue'),
    meta: { title: '购物车' }
  },
  {
    path: '/mall/checkout',
    name: 'MallCheckout',
    component: () => import('@/views/MallCheckout.vue'),
    meta: { title: '确认订单' }
  },
  {
    path: '/mall/orders',
    name: 'MallOrders',
    component: () => import('@/views/MallOrders.vue'),
    meta: { title: '商城订单' }
  },
  {
    path: '/my-orders',
    name: 'MyOrders',
    component: () => import('@/views/MyOrders.vue'),
    meta: { title: '我的订单' }
  },
  {
    path: '/order/:id',
    name: 'OrderDetail',
    component: () => import('@/views/OrderDetail.vue'),
    meta: { title: '订单详情' }
  },
  {
    path: '/promotion/:id',
    name: 'Promotion',
    component: () => import('@/views/Promotion.vue'),
    meta: { title: '活动详情' }
  },
  {
    path: '/about',
    name: 'About',
    component: () => import('@/views/About.vue'),
    meta: { title: '关于我们', public: true }
  },
  {
    path: '/help',
    name: 'Help',
    component: () => import('@/views/Help.vue'),
    meta: { title: '帮助中心', public: true }
  },
  {
    path: '/referral',
    name: 'Referral',
    component: () => import('@/views/Referral.vue'),
    meta: { title: '邀请有礼' }
  },
  {
    path: '/dining',
    name: 'Dining',
    component: () => import('@/views/Dining.vue'),
    meta: { title: '扫码点餐', public: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

// 全局守卫：未登录跳 /login（白名单除外）
router.beforeEach((to) => {
  const member = useMemberStore()
  if (!to.meta.public && !member.isLogin) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && member.isLogin) {
    return { path: '/home' }
  }
  return true
})

// 标题
router.afterEach((to) => {
  const title = (to.meta.title as string) || '星河·会记'
  document.title = `${title} · 星河会记`
})

export default router
