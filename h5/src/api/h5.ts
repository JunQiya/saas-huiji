import { request, type PageData } from '@/utils/request'

// 会员卡面信息（GET /api/h5/profile）
export interface MemberProfile {
  id: number | string
  name: string
  phone: string
  level: number
  levelName: string
  balance: number
  points: number
  gender?: string
  birthday?: string
  avatar?: string
  consumeCount?: number
  totalAmount?: number
  lastConsumeAt?: string
  createdAt?: string
}

// 登录响应（POST /api/h5/login）
export interface LoginResult {
  memberToken: string
  member: MemberProfile
}

// 券记录
export interface CouponRecord {
  id: number | string
  memberId?: number | string
  memberName?: string
  couponName: string
  code: string
  status: 'UNUSED' | 'USED' | 'EXPIRED'
  type?: 'FULL_CUT' | 'PERCENT' | 'EXPERIENCE' | 'BIRTHDAY'
  faceValue?: number
  threshold?: number
  grantedAt?: string
  usedAt?: string
  expireAt?: string
}

// 可领取的券
export interface AvailableCoupon {
  id: number | string
  name: string
  type: 'FULL_CUT' | 'PERCENT' | 'EXPERIENCE' | 'BIRTHDAY'
  faceValue: number
  threshold: number
  validType: 'DAYS' | 'RANGE'
  validDays?: number
  validStart?: string
  validEnd?: string
  total?: number
  remain?: number
  perLimit?: number
  scope?: string
  claimed?: boolean
}

// 流水记录
export interface TransactionRecord {
  id: number | string
  type: 'RECHARGE' | 'CONSUME' | 'REFUND' | 'GIFT' | string
  amount: number
  balanceAfter?: number
  storeId?: number | string
  storeName?: string
  remark?: string
  createdAt: string
}

// 门店
export interface Store {
  id: number | string
  name: string
  address: string
  phone: string
  businessHours?: string
  status?: string
  distance?: number
  longitude?: number
  latitude?: number
}

// 提取列表数据：兼容数组和分页结构
function pickList<T>(data: T[] | PageData<T>): T[] {
  if (Array.isArray(data)) return data
  return data?.list || []
}

// H5 API
export const h5Api = {
  login(phone: string, code: string) {
    return request.post<LoginResult>('/api/h5/login', { phone, code })
  },
  profile() {
    return request.get<MemberProfile>('/api/h5/profile')
  },
  balance() {
    return request.get<{ balance: number; recent: TransactionRecord[] }>('/api/h5/balance')
  },
  async myCoupons(status: 'UNUSED' | 'USED' | 'EXPIRED'): Promise<CouponRecord[]> {
    const data = await request.get<CouponRecord[] | PageData<CouponRecord>>('/api/h5/coupons', {
      status
    })
    return pickList(data)
  },
  async availableCoupons(): Promise<AvailableCoupon[]> {
    const data = await request.get<AvailableCoupon[] | PageData<AvailableCoupon>>(
      '/api/h5/coupons/available'
    )
    return pickList(data)
  },
  claimCoupon(id: number | string) {
    return request.post<null>(`/api/h5/coupons/${id}/claim`)
  },
  async transactions(params?: {
    type?: string
    page?: number
    size?: number
  }): Promise<TransactionRecord[]> {
    const data = await request.get<TransactionRecord[] | PageData<TransactionRecord>>(
      '/api/h5/transactions',
      params
    )
    return pickList(data)
  },
  async stores(): Promise<Store[]> {
    const data = await request.get<Store[] | PageData<Store>>('/api/h5/stores')
    return pickList(data)
  },
  async myOrders(status: string | undefined, page: number = 1, size: number = 20): Promise<OrderInfo[] | PageData<OrderInfo>> {
    return request.get<OrderInfo[] | PageData<OrderInfo>>('/api/h5/orders', { status, page, size })
  },
  orderDetail(id: number | string) {
    return request.get<OrderInfo>(`/api/h5/orders/${id}`)
  },
  async activeProducts(category?: string): Promise<Product[]> {
    const data = await request.get<Product[] | PageData<Product>>('/api/h5/products/active', { category })
    return pickList(data)
  }
}


// 订单
export interface OrderItem {
  id: number | string
  productId: number | string
  productName: string
  unitPrice: number
  quantity: number
  subtotal: number
}
export interface OrderInfo {
  id: number | string
  orderNo: string
  storeId?: number | string
  memberId?: number | string
  totalAmount: number
  discountAmount: number
  paidAmount: number
  payMethod?: string
  status: 'PENDING' | 'PAID' | 'REFUNDED' | 'VOID'
  remark?: string
  paidAt?: string
  refundedAt?: string
  refundReason?: string
  createdAt: string
  items?: OrderItem[]
}

export interface Product {
  id: number | string
  name: string
  category: 'SERVICE' | 'GOODS'
  cover?: string
  price: number
  costPrice?: number
  stock?: number
  status?: string
  description?: string
}


// ============ 推荐裂变(X3 H5) ============
export const referralApi = {
  me: () => request.get<any>('/api/h5/referral/me'),
  list: () => request.get<any>('/api/h5/referral/list'),
  bind: (code: string) => request.post<any>('/api/h5/referral/bind', { code })
}


// ============ 微信 ============
// JS-SDK 签名响应
export interface WxJsSdkSignature {
  appId: string
  timestamp: string
  nonceStr: string
  signature: string
}

// 微信支付下单响应（调起微信内置支付所需参数）
export interface WxPayParams {
  timeStamp: string
  nonceStr: string
  package: string
  signType: string
  paySign: string
}

// 微信支付状态查询结果
export interface WxPayStatusResult {
  status: 'PENDING' | 'PAID' | 'REFUNDED' | 'FAILED'
  paidAt?: string
  transactionId?: string
}

export const wxApi = {
  // 获取 JS-SDK 签名（需要 member token）
  jssdk: (url: string) => request.get<WxJsSdkSignature>('/api/wx/jssdk', { url }),
  // 微信支付下单
  pay: (orderId: number) => request.post<WxPayParams>(`/api/wxpay/order/${orderId}`),
  // 查询支付状态
  queryPay: (orderId: number) => request.get<WxPayStatusResult>(`/api/wxpay/query/${orderId}`)
}

// ============ 门店点餐 ============
export const diningApi = {
  // 桌台信息（公开）
  table: (tableId: number | string) => request.get<any>(`/api/h5/dining/table/${tableId}`),
  // 菜单（公开，按分类分组）
  menu: (storeId: number | string) => request.get<any[]>('/api/h5/dining/menu', { storeId }),
  // 提交点餐（需 member token）
  order: (data: {
    tableId: number | string
    storeId: number | string
    orderType: 'DINE_IN' | 'TAKEOUT'
    items: { productId: number | string; quantity: number; remark?: string }[]
    remark?: string
  }) => request.post<any>('/api/h5/dining/order', data),
  // 我的点餐订单（需 member token）
  myOrders: () => request.get<any[]>('/api/h5/dining/my-orders')
}

// ============ 线上商城 ============
export const mallApi = {
  // 商城分类（公开）
  categories: () => request.get<any[]>('/api/h5/mall/categories'),
  // 商城商品列表（公开）
  products: (params: any) => request.get<any>('/api/h5/mall/products', params),
  // 商品详情（公开）
  productDetail: (id: number | string) => request.get<any>(`/api/h5/mall/products/${id}`),
  // 购物车列表（需 member token）
  cart: () => request.get<any[]>('/api/h5/mall/cart'),
  // 加购
  addToCart: (data: { productId: number | string; quantity: number }) =>
    request.post<any>('/api/h5/mall/cart', data),
  // 更新数量 / 选中状态
  updateCart: (cartId: number | string, data: { quantity?: number; selected?: boolean }) =>
    request.put<any>(`/api/h5/mall/cart/${cartId}`, data),
  // 移除购物车项（按商品 ID）
  removeFromCart: (productId: number | string) =>
    request.delete<null>(`/api/h5/mall/cart/${productId}`),
  // 清空购物车
  clearCart: () => request.post<null>('/api/h5/mall/cart/clear'),
  // 购物车汇总
  cartSummary: () => request.get<any>('/api/h5/mall/cart/summary'),
  // 结算下单
  checkout: (data: any) => request.post<any>('/api/h5/mall/checkout', data),
  // 我的商城订单
  myOrders: (params: any) => request.get<any>('/api/h5/mall/my-orders', params),
  // 商城订单详情
  orderDetail: (id: number | string) => request.get<any>(`/api/h5/mall/orders/${id}`)
}
