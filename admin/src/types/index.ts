// 统一响应与领域类型定义

// 统一响应
export interface ApiResult<T = any> {
  ok: boolean
  data?: T
  message?: string
  code?: string
}

// 分页响应
export interface PageData<T = any> {
  list: T[]
  total: number
  page: number
  size: number
}

// 角色
export type Role = 'TENANT_ADMIN' | 'STORE_MANAGER' | 'STAFF' | 'CASHIER'

// 登录返回 user
export interface LoginUser {
  id: number
  username: string
  name: string
  role: Role
  storeId?: number | null
}
export interface LoginResult {
  token: string
  expiresIn: number
  user: LoginUser
}

// 会员
export interface Member {
  id: number
  name: string
  phone: string
  gender?: string
  birthday?: string
  level?: number
  levelName?: string
  balance?: number // 分
  points?: number
  tags?: string[]
  storeIds?: number[]
  consumeCount?: number
  totalAmount?: number // 分
  lastConsumeAt?: string
  createdAt?: string
}

// 资金流水
export interface Transaction {
  id: number
  memberId: number
  memberName?: string
  type: string // RECHARGE/CONSUME/GIFT/REFUND
  amount: number // 分（正负）
  balanceAfter?: number
  storeId?: number
  storeName?: string
  remark?: string
  createdAt: string
}

// 优惠券
export type CouponType = 'FULL_CUT' | 'PERCENT' | 'EXPERIENCE' | 'BIRTHDAY'
export type CouponValidType = 'DAYS' | 'RANGE'
export type CouponRecordStatus = 'UNUSED' | 'USED' | 'EXPIRED'
export interface Coupon {
  id: number
  name: string
  type: CouponType
  faceValue?: number
  threshold?: number
  validType: CouponValidType
  validDays?: number
  validStart?: string
  validEnd?: string
  total?: number
  granted?: number
  used?: number
  perLimit?: number
  scope?: string
  status?: string
  createdAt?: string
}
export interface CouponRecord {
  id: number
  memberId: number
  memberName: string
  couponName: string
  code: string
  status: CouponRecordStatus
  grantedAt: string
  usedAt?: string
  expireAt?: string
}

// 营销活动
export type CampaignType = 'BIRTHDAY' | 'DORMANT' | 'REPURCHASE' | 'MANUAL'
export type CampaignChannel = 'SMS' | 'WECHAT' | 'IN_APP'
export interface Campaign {
  id: number
  name: string
  type: CampaignType
  trigger?: string
  audience?: string
  channel?: CampaignChannel
  content?: string
  startAt?: string
  endAt?: string
  enabled?: boolean
  stats?: { triggered: number; reached: number; converted: number }
  createdAt?: string
}

// 门店
export interface Store {
  id: number
  name: string
  address?: string
  phone?: string
  businessHours?: string
  status?: string
}

// 员工
export interface Employee {
  id: number
  username: string
  name: string
  phone?: string
  role: Role
  storeIds?: number[]
  status?: string
  createdAt?: string
}
export interface Performance {
  month: string
  amount: number
  count: number
}

// 审计
export interface AuditLog {
  id: number
  operatorId: number
  operatorName: string
  action: string
  target?: string
  detail?: string
  ip?: string
  createdAt: string
}
export interface LoginLog {
  id: number
  userId?: number
  username: string
  ip?: string
  location?: string
  browser?: string
  os?: string
  status?: string
  message?: string
  createdAt: string
}

// 设置
export interface LevelRule {
  level: number
  name: string
  threshold: number // 分
}
export interface RechargeRule {
  amount: number // 分
  gift: number // 分
}
export interface TenantSettings {
  tenantName: string
  brandColor: string
  levelRules: LevelRule[]
  smsSign: string
  rechargeRules: RechargeRule[]
}

// 统计
export interface OverviewStats {
  revenue: number
  revenueDelta: number
  memberCount: number
  memberDelta: number
  orderCount: number
  orderDelta: number
  avgPrice: number
  avgPriceDelta: number
}
export interface TrendPoint {
  date: string
  value: number
}
export interface MemberGrowthPoint {
  date: string
  newCount: number
  activeCount: number
}
export interface TopService {
  name: string
  count: number
  amount: number
}
export interface RfmStats {
  high: number
  mid: number
  low: number
  dormant: number
}
export interface HourPoint {
  hour: number
  count: number
}

// 经营摘要
export interface SummaryStats {
  todayRevenue: number
  todayDelta: number
  weekRevenue: number
  weekDelta: number
  monthRevenue: number
  monthDelta: number
  todayOrders: number
  weekOrders: number
  monthOrders: number
  newMembersToday: number
  newMembersWeek: number
  newMembersMonth: number
  consumeMembersToday: number
}

// 会员画像
export interface MemberProfile {
  consumeScore: number      // 0-100
  activeScore: number
  lifecycle: string
  trend30d: { date: string; amount: number }[]
  tags: string[]
  nextActionHint: string
}

// 活动 SOP 预览
export interface SopStep {
  type: 'trigger' | 'filter' | 'action'
  text: string
}
export interface CampaignPreview {
  audience: { count: number; breakdown: { key: string; count: number }[] }
  channels: string[]
  estimatedCost: number
  estimatedReach: number
  sop: SopStep[]
}

// 核销码展示信息
export interface CouponDisplay {
  code: string
  couponName: string
  memberName: string
  status: string
  expireAt: string
}
