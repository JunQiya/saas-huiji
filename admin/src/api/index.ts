import request from './request'
import type {
  LoginResult,
  LoginUser,
  PageData,
  Member,
  MemberProfile,
  Transaction,
  Coupon,
  CouponDisplay,
  CouponRecord,
  Campaign,
  CampaignPreview,
  Store,
  Employee,
  Performance,
  AuditLog,
  LoginLog,
  TenantSettings,
  OverviewStats,
  SummaryStats,
  TrendPoint,
  MemberGrowthPoint,
  TopService,
  RfmStats,
  HourPoint
} from '@/types'

// ============ Auth ============
export const authApi = {
  login: (data: { username: string; password: string }) =>
    request.post<any, LoginResult>('/auth/login', data),
  logout: () => request.post<any, null>('/auth/logout'),
  profile: () => request.get<any, LoginUser>('/auth/profile'),
  updatePassword: (data: { oldPassword: string; newPassword: string }) =>
    request.put<any, null>('/auth/password', data)
}

// ============ Stats ============
export const statsApi = {
  overview: () => request.get<any, OverviewStats>('/stats/overview'),
  summary: () => request.get<any, SummaryStats>('/stats/summary'),
  trend: (params: { range: '7d' | '30d' | '90d'; metric: 'revenue' | 'orders' | 'members' }) =>
    request.get<any, TrendPoint[]>('/stats/trend', { params }),
  memberGrowth: () => request.get<any, MemberGrowthPoint[]>('/stats/member-growth'),
  topServices: () => request.get<any, TopService[]>('/stats/top-services'),
  rfm: () => request.get<any, RfmStats>('/stats/rfm'),
  hour: () => request.get<any, HourPoint[]>('/stats/hour'),
  ordersToday: () => request.get<any, { count: number; amount: number; date: string }>('/stats/orders/today'),
  productsTop: (params: { limit?: number; start?: string; end?: string }) =>
    request.get<any, { productId: number; productName: string; quantity: number; subtotal: number }[]>('/stats/products/top', { params })
}

// ============ Members ============
export const membersApi = {
  list: (params: any) => request.get<any, PageData<Member>>('/members', { params }),
  detail: (id: number) => request.get<any, Member>(`/members/${id}`),
  create: (data: any) => request.post<any, Member>('/members', data),
  update: (id: number, data: any) => request.put<any, Member>(`/members/${id}`, data),
  remove: (id: number) => request.delete<any, null>(`/members/${id}`),
  transactions: (id: number, params: any) =>
    request.get<any, PageData<Transaction>>(`/members/${id}/transactions`, { params }),
  recharge: (id: number, data: { amount: number; gift: number; payMethod: string; remark?: string }) =>
    request.post<any, { balance: number }>(`/members/${id}/recharge`, data),
  consume: (id: number, data: any) => request.post<any, any>(`/members/${id}/consume`, data),
  setTags: (id: number, tags: string[]) => request.post<any, null>(`/members/${id}/tags`, { tags }),
  coupons: (id: number) => request.get<any, CouponRecord[]>(`/members/${id}/coupons`),
  batchTags: (data: { memberIds: number[]; tags: string[] }) =>
    request.post<any, null>('/members/batch/tags', data),
  batchLevel: (data: { memberIds: number[]; level: number }) =>
    request.post<any, null>('/members/batch/level', data),
  adjustPoints: (id: number, data: { delta: number; reason: string }) =>
    request.post<any, { points: number }>(`/members/${id}/points`, data),
  setLevel: (id: number, level: number) =>
    request.put<any, { level: number; levelName: string }>(`/members/${id}/level`, { level }),
  import: (form: FormData) => request.post<any, { success: number; failed: number; errors: string[] }>('/members/import', form),
  export: (params: any) => request.get<any, any>('/members/export', { params, responseType: 'blob' }),
  profile: (id: number) => request.get<any, MemberProfile>(`/members/${id}/profile`)
}

// ============ Wallet(全局储值流水) ============
export const walletApi = {
  transactions: (params: any) => request.get<any, any>('/transactions', { params })
}

// ============ Coupons ============
export const couponsApi = {
  list: (params: any) => request.get<any, Coupon[]>('/coupons', { params }),
  create: (data: any) => request.post<any, Coupon>('/coupons', data),
  import: (data: any[]) => request.post<any, { success: number; failed: number; errors: string[] }>('/coupons/import', data),
  update: (id: number, data: any) => request.put<any, Coupon>(`/coupons/${id}`, data),
  remove: (id: number) => request.delete<any, null>(`/coupons/${id}`),
  grant: (id: number, data: { memberIds: number[]; storeId?: number }) =>
    request.post<any, null>(`/coupons/${id}/grant`, data),
  stop: (id: number) => request.post<any, null>(`/coupons/${id}/stop`),
  records: (id: number, params: any) =>
    request.get<any, PageData<CouponRecord>>(`/coupons/${id}/records`, { params }),
  verify: (data: { code: string; storeId?: number }) =>
    request.post<any, CouponRecord>('/coupons/verify', data),
  display: (code: string) => request.get<any, CouponDisplay>(`/coupons/records/${code}/qrcode`)
}

// ============ Campaigns ============
export const campaignsApi = {
  list: (params: any) => request.get<any, Campaign[]>('/campaigns', { params }),
  create: (data: any) => request.post<any, Campaign>('/campaigns', data),
  update: (id: number, data: any) => request.put<any, Campaign>(`/campaigns/${id}`, data),
  remove: (id: number) => request.delete<any, null>(`/campaigns/${id}`),
  toggle: (id: number, enabled: boolean) =>
    request.post<any, null>(`/campaigns/${id}/toggle`, { enabled }),
  preview: (id: number) => request.post<any, CampaignPreview>(`/campaigns/${id}/preview`),
  stats: (id: number) =>
    request.get<any, { triggered: number; reached: number; converted: number }>(
      `/campaigns/${id}/stats`
    )
}

// ============ Stores ============
export const storesApi = {
  list: () => request.get<any, Store[]>('/stores'),
  create: (data: any) => request.post<any, Store>('/stores', data),
  update: (id: number, data: any) => request.put<any, Store>(`/stores/${id}`, data),
  remove: (id: number) => request.delete<any, null>(`/stores/${id}`)
}

// ============ Employees ============
export const employeesApi = {
  list: (params: any) => request.get<any, Employee[]>('/employees', { params }),
  create: (data: any) => request.post<any, Employee>('/employees', data),
  update: (id: number, data: any) => request.put<any, Employee>(`/employees/${id}`, data),
  resetPassword: (id: number, password: string) =>
    request.put<any, null>(`/employees/${id}/password`, { password }),
  remove: (id: number) => request.delete<any, null>(`/employees/${id}`),
  performance: (id: number) => request.get<any, Performance[]>(`/employees/${id}/performance`)
}

// ============ Audit ============
export const auditApi = {
  logs: (params: any) => request.get<any, PageData<AuditLog>>('/audit/logs', { params }),
  logins: (params: any) => request.get<any, PageData<LoginLog>>('/audit/logins', { params })
}

// ============ Settings ============
export const settingsApi = {
  get: () => request.get<any, TenantSettings>('/settings'),
  update: (data: TenantSettings) => request.put<any, null>('/settings', data),
  getFeatures: () => request.get<any, any>('/settings/features'),
  updateFeatures: (data: any) => request.put<any, any>('/settings/features', data)
}


// ============ 商品/服务 ============
export const productsApi = {
  list: (params: any) => request.get<any, PageData<any>>('/products', { params }),
  detail: (id: number) => request.get<any, any>(`/products/${id}`),
  create: (data: any) => request.post<any, any>('/products', data),
  update: (id: number, data: any) => request.put<any, any>(`/products/${id}`, data),
  remove: (id: number) => request.delete<any, null>(`/products/${id}`),
  active: (params?: any) => request.get<any, any[]>('/products/active', { params }),
  changeStatus: (id: number, status: 'ACTIVE' | 'DISABLED') =>
    request.put<any, any>(`/products/${id}/status`, { status }),
  stock: (id: number, mode: 'SET' | 'INC', value: number) =>
    request.put<any, any>(`/products/${id}/stock`, { mode, value }),
  updateStores: (id: number, storeIds: number[]) =>
    request.put<any, any>(`/products/${id}/stores`, { storeIds })
}

// ============ 订单 ============
export const ordersApi = {
  list: (params: any) => request.get<any, PageData<any>>('/orders', { params }),
  detail: (id: number) => request.get<any, any>(`/orders/${id}`),
  create: (data: any) => request.post<any, any>('/orders', data),
  pay: (id: number, data: any) => request.post<any, any>(`/orders/${id}/pay`, data),
  refund: (id: number, data: any) => request.post<any, any>(`/orders/${id}/refund`, data),
  void: (id: number, data: any) => request.post<any, any>(`/orders/${id}/void`, data)
}

// ============ Settings 扩展: 计费 / 多店 ============
export const settingsPlanApi = {
  get: () => request.get<any, any>('/settings/plan'),
  upgrade: (data: { plan: string; months: number }) => request.post<any, any>('/settings/plan/upgrade', data),
  currentStore: () => request.get<any, any>('/settings/store/current'),
  switchStore: (storeId: number) => request.post<any, any>('/settings/store/switch', { storeId })
}


// ============ 消息中心(X1) ============
export const messagesApi = {
  list: (params: any) => request.get<any, PageData<any>>('/messages', { params }),
  detail: (id: number) => request.get<any, any>(`/messages/${id}`),
  create: (data: any) => request.post<any, any>('/messages', data),
  cancel: (id: number) => request.post<any, null>(`/messages/${id}/cancel`),
  retry: (id: number) => request.post<any, any>(`/messages/${id}/retry`),
  stats: () => request.get<any, any>('/messages/stats')
}

// ============ 报表中心(X2) ============
export const reportsApi = {
  list: (params: any) => request.get<any, PageData<any>>('/reports', { params }),
  detail: (id: number) => request.get<any, any>(`/reports/${id}`),
  create: (data: any) => request.post<any, any>('/reports', data),
  update: (id: number, data: any) => request.put<any, any>(`/reports/${id}`, data),
  remove: (id: number) => request.delete<any, null>(`/reports/${id}`),
  toggle: (id: number, enabled: boolean) =>
    request.post<any, any>(`/reports/${id}/toggle`, { enabled }),
  run: (id: number) => request.post<any, any>(`/reports/${id}/run`),
  downloadUrl: (id: number, type: 'pdf' | 'xlsx') => `/reports/${id}/download?type=${type}`,
  stats: () => request.get<any, any>('/reports/stats')
}

// ============ 推荐裂变(X3 admin) ============
export const referralsApi = {
  listByReferrer: (memberId: number) => request.get<any, any[]>(`/referrals/list?memberId=${memberId}`),
  adminAll: (params: any) => request.get<any, PageData<any>>('/referrals/admin/all', { params }),
  adminStats: (memberId: number) => request.get<any, any>(`/referrals/admin/stats?memberId=${memberId}`),
  adminSummary: () => request.get<any, any>('/referrals/admin/summary'),
  adminBind: (memberId: number, code: string) => request.post<any, any>('/referrals/admin/bind', { memberId, code })
}

// ============ 微信公众号 ============
export const wxAccountApi = {
  get: () => request.get<any, any>('/wx/account'),
  save: (data: any) => request.put<any, any>('/wx/account', data),
  test: () => request.get<any, any>('/wx/account/test')
}

// ============ 代理商 ============
export const agentsApi = {
  list: () => request.get<any, any[]>('/agents'),
  create: (data: any) => request.post<any, any>('/agents', data),
  update: (id: number, data: any) => request.put<any, any>(`/agents/${id}`, data),
  remove: (id: number) => request.delete<any, null>(`/agents/${id}`),
  stats: (id: number) => request.get<any, any>(`/agents/${id}/stats`)
}

// ============ 门店点餐 ============
export const diningApi = {
  // 桌台
  tables: (storeId: number) => request.get<any, any[]>('/dining/tables', { params: { storeId } }),
  saveTable: (data: any) => request.post<any, any>('/dining/tables', data),
  removeTable: (id: number) => request.delete<any, null>(`/dining/tables/${id}`),
  occupyTable: (id: number) => request.post<any, any>(`/dining/tables/${id}/occupy`),
  freeTable: (id: number) => request.post<any, any>(`/dining/tables/${id}/free`),
  qrcode: (id: number) => request.post<any, any>(`/dining/tables/${id}/qrcode`),
  // 菜单分类
  categories: (storeId: number) => request.get<any, any[]>('/dining/categories', { params: { storeId } }),
  saveCategory: (data: any) => request.post<any, any>('/dining/categories', data),
  removeCategory: (id: number) => request.delete<any, null>(`/dining/categories/${id}`),
  bindProducts: (categoryId: number, productIds: number[]) =>
    request.post<any, any>(`/dining/categories/${categoryId}/products`, { productIds }),
  // 厨房工单
  kitchenOrders: (storeId: number, status?: string) =>
    request.get<any, any[]>('/dining/kitchen-orders', { params: { storeId, status } }),
  updateKitchenStatus: (id: number, status: string) =>
    request.post<any, any>(`/dining/kitchen-orders/${id}/status`, { status })
}

// ============ 赢奖小游戏 ============
export const gameApi = {
  list: (params?: any) => request.get<any, any[]>('/games', { params }),
  detail: (id: number) => request.get<any, any>(`/games/${id}`),
  save: (data: any) => request.post<any, any>('/games', data),
  toggleStatus: (id: number, status: string) => request.post<any, any>(`/games/${id}/status`, { status }),
  remove: (id: number) => request.delete<any, null>(`/games/${id}`),
  prizes: (gameId: number) => request.get<any, any[]>(`/games/${gameId}/prizes`),
  savePrize: (gameId: number, data: any) => request.post<any, any>(`/games/${gameId}/prizes`, data),
  removePrize: (prizeId: number) => request.delete<any, null>(`/games/prizes/${prizeId}`),
  stats: (gameId: number) => request.get<any, any>(`/games/${gameId}/stats`)
}

// ============ 线上商城 ============
export const mallApi = {
  categories: () => request.get<any, any[]>('/mall/categories'),
  saveCategory: (data: any) => request.post<any, any>('/mall/categories', data),
  removeCategory: (id: number) => request.delete<any, null>(`/mall/categories/${id}`),
  bindProducts: (categoryId: number, productIds: number[]) =>
    request.post<any, any>(`/mall/categories/${categoryId}/products`, { productIds }),
  orders: (params: any) => request.get<any, any>('/mall/orders', { params }),
  updateTracking: (orderId: number, data: any) =>
    request.put<any, any>(`/mall/orders/${orderId}/tracking`, data)
}
