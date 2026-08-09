// 底部 Tab 栏配置（首页 / 商城 / 订单 / 我的）
export interface TabItem {
  path: string
  label: string
  icon: string
  activeIcon?: string
}

export const TAB_ITEMS: TabItem[] = [
  { path: '/', label: '首页', icon: 'wap-home-o' },
  { path: '/mall', label: '商城', icon: 'gift-card-o' },
  { path: '/my-orders', label: '订单', icon: 'orders-o' },
  { path: '/profile', label: '我的', icon: 'user-o' }
]
