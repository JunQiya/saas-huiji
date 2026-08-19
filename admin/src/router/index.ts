import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

// 路由配置：侧边栏由静态路由生成
// 路由 meta: title 菜单名, icon 图标名, group 侧边栏分组, hidden 是否隐藏
export const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { hidden: true }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
{
        path: 'pos',
        name: 'Pos',
        component: () => import('@/views/Pos.vue'),
        meta: { title: '收银台', icon: 'Money', group: '经营总览' }
      },
      {
        path: 'products',
        name: 'Products',
        component: () => import('@/views/Products.vue'),
        meta: { title: '商品服务', icon: 'Goods', group: '经营总览' }
      },
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('@/views/Orders.vue'),
        meta: { title: '订单流水', icon: 'List', group: '经营总览' }
      },
      {
        path: 'dining-tables',
        name: 'DiningTables',
        component: () => import('@/views/DiningTables.vue'),
        meta: { title: '桌台管理', icon: 'Grid', group: '经营总览' }
      },
      {
        path: 'kitchen-orders',
        name: 'KitchenOrders',
        component: () => import('@/views/KitchenOrders.vue'),
        meta: { title: '厨房工单', icon: 'Food', group: '经营总览' }
      },
      {
        path: 'mall-admin',
        name: 'MallAdmin',
        component: () => import('@/views/MallAdmin.vue'),
        meta: { title: '线上商城', icon: 'Shop', group: '经营总览' }
      },
      {
        path: 'marketing-calendar',
        name: 'MarketingCalendar',
        component: () => import('@/views/MarketingCalendar.vue'),
        meta: { title: '营销日历', icon: 'Calendar', group: '营销中心' }
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '数据看板', icon: 'DataLine', group: '经营总览' }
      },
      {
        path: 'members',
        name: 'Members',
        component: () => import('@/views/Members.vue'),
        meta: { title: '会员管理', icon: 'User', group: '会员与储值' }
      },
      {
        path: 'wallet',
        name: 'Wallet',
        component: () => import('@/views/Wallet.vue'),
        meta: { title: '储值流水', icon: 'Wallet', group: '会员与储值' }
      },
      {
        path: 'coupons',
        name: 'Coupons',
        component: () => import('@/views/Coupons.vue'),
        meta: { title: '优惠券', icon: 'Ticket', group: '营销中心' }
      },
      {
        path: 'campaigns',
        name: 'Campaigns',
        component: () => import('@/views/Campaigns.vue'),
        meta: { title: '营销活动', icon: 'Promotion', group: '营销中心' }
      },
      {
        path: 'games',
        name: 'Games',
        component: () => import('@/views/Games.vue'),
        meta: { title: '赢奖小游戏', icon: 'Trophy', group: '营销中心' }
      },
      {
        path: 'stores',
        name: 'Stores',
        component: () => import('@/views/Stores.vue'),
        meta: { title: '门店管理', icon: 'Shop', group: '组织与运营' }
      },
      {
        path: 'employees',
        name: 'Employees',
        component: () => import('@/views/Employees.vue'),
        meta: { title: '员工管理', icon: 'Avatar', group: '组织与运营' }
      },
      {
        path: 'agents',
        name: 'Agents',
        component: () => import('@/views/Agents.vue'),
        meta: { title: '代理商', icon: 'Connection', group: '组织与运营' }
      },
      {
        path: 'audit',
        name: 'Audit',
        component: () => import('@/views/Audit.vue'),
        meta: { title: '审计日志', icon: 'Document', group: '系统' }
      },
      {
        path: 'messages',
        name: 'Messages',
        component: () => import('@/views/Messages.vue'),
        meta: { title: '消息中心', icon: 'ChatLineRound', group: '系统' }
      },
      {
        path: 'reports',
        name: 'Reports',
        component: () => import('@/views/Reports.vue'),
        meta: { title: '报表中心', icon: 'Document', group: '系统' }
      },
      {
        path: 'referrals',
        name: 'Referrals',
        component: () => import('@/views/Referrals.vue'),
        meta: { title: '推荐裂变', icon: 'Share', group: '会员与储值' }
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/Settings.vue'),
        meta: { title: '系统设置', icon: 'Setting', group: '系统' }
      },
      {
        path: 'wx-account',
        name: 'WxAccount',
        component: () => import('@/views/WxAccount.vue'),
        meta: { title: '微信公众号', icon: 'ChatDotRound', group: '系统' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { hidden: true }
  }
]

const router = createRouter({
  history: createWebHistory('/admin/'),
  routes
})

// 路由守卫：未登录跳 /login
router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()
  if (to.path === '/login') {
    if (userStore.isLogin) {
      next('/dashboard')
    } else {
      next()
    }
    return
  }
  if (!userStore.isLogin) {
    const redirect = to.fullPath
    next({ path: '/login', query: redirect !== '/' ? { redirect } : undefined })
    return
  }
  next()
})

export default router
