# 星河·会记 — API 契约（v1）

统一约定见本文件，前后端三方（Spring Boot / Vue3 Admin / H5）均须严格遵循。

## 1. 通用约定

- Base URL: `/api`
- 鉴权: 除 `/api/auth/login`、`/api/h5/**` 公开接口外，请求头需带 `Authorization: Bearer <token>`
- 多租户: 登录后 token 内含 `tenantId`，后端自动隔离数据
- 统一响应：
  - 成功: `{ "ok": true, "data": ... }` 或 `{ "ok": true, "data": { "list": [], "total": 0, "page": 1, "size": 20 } }`
  - 失败: `{ "ok": false, "message": "...", "code": "..." }`
  - 401: `{ "ok": false, "code": "SESSION_EXPIRED", "message": "登录已过期" }`
- 分页参数: `page`(从1) / `size`(默认20)
- 时间: ISO-8601 字符串
- 货币: 金额一律以「分」为单位传输（整数），展示时除 100

## 2. 鉴权 / Auth

| Method | Path | 入参 | 出参 data |
|---|---|---|---|
| POST | `/api/auth/login` | `{ username, password }` | `{ token, expiresIn, user: {id, username, name, role, storeId} }` |
| POST | `/api/auth/logout` | — | `null` |
| GET | `/api/auth/profile` | — | `{ id, username, name, role, storeId, tenantId }` |
| PUT | `/api/auth/password` | `{ oldPassword, newPassword }` | `null` |

角色枚举 `role`: `TENANT_ADMIN`(租户管理员) / `STORE_MANAGER`(店长) / `STAFF`(员工) / `CASHIER`(收银)

## 3. 会员 / Members

| Method | Path | 说明 |
|---|---|---|
| GET | `/api/members` | 查询，参数 `keyword`(姓名/手机号) `level` `tag` `storeIds` `page` `size` |
| POST | `/api/members` | 新增 `{ name, phone, gender, birthday, storeIds, tags, remark }` |
| GET | `/api/members/{id}` | 详情，含等级/余额/标签 |
| PUT | `/api/members/{id}` | 编辑 |
| DELETE | `/api/members/{id}` | 软删除 |
| GET | `/api/members/{id}/transactions` | 资金流水，参数 `page size type` |
| POST | `/api/members/{id}/recharge` | 储值 `{ amount(分), gift(分), payMethod, remark }` 返回新余额 |
| POST | `/api/members/{id}/consume` | 消费扣款 `{ amount, storeId, items, remark }` 优先扣储值 |
| POST | `/api/members/{id}/tags` | `{ tags: [] }` |
| GET | `/api/members/{id}/coupons` | 该会员持有的券 |

会员对象: `{ id, name, phone, gender, birthday, level, levelName, balance, points, tags[], storeIds[], consumeCount, totalAmount, lastConsumeAt, createdAt }`

等级规则: 按累计消费 `totalAmount` 自动升级，阈值由租户配置（见 Settings）。

## 4. 优惠券 / Coupons

| Method | Path | 说明 |
|---|---|---|
| GET | `/api/coupons` | 列表，参数 `status` `type` |
| POST | `/api/coupons` | `{ name, type(FULL_CUT/PERCENT/EXPERIENCE/BIRTHDAY), faceValue, threshold, validType(DAYS/RANGE), validDays, validStart, validEnd, total, perLimit, scope }` |
| PUT | `/api/coupons/{id}` | 编辑（已发放不可改规则） |
| DELETE | `/api/coupons/{id}` | 删除（已发放仅停用） |
| POST | `/api/coupons/{id}/grant` | 发放 `{ memberIds: [], storeId? }` |
| POST | `/api/coupons/{id}/stop` | 停用 |
| GET | `/api/coupons/{id}/records` | 发放/核销记录 |
| POST | `/api/coupons/verify` | 核销 `{ code, storeId }` |

券记录对象: `{ id, memberId, memberName, couponName, code, status(UNUSED/USED/EXPIRED), grantedAt, usedAt, expireAt }`

## 5. 营销活动 / Campaigns

| Method | Path | 说明 |
|---|---|---|
| GET | `/api/campaigns` | 列表，参数 `status` |
| POST | `/api/campaigns` | `{ name, type(BIRTHDAY/DORMANT/REPURCHASE/MANUAL), trigger, audience, channel(SMS/WECHAT/IN_APP), content, startAt, endAt, enabled }` |
| PUT | `/api/campaigns/{id}` | 编辑 |
| DELETE | `/api/campaigns/{id}` | 删除 |
| POST | `/api/campaigns/{id}/toggle` | 启停 `{ enabled }` |
| POST | `/api/campaigns/{id}/preview` | 预览命中人数 |
| GET | `/api/campaigns/{id}/stats` | 触发/触达/转化统计 |

## 6. 门店 / Stores

| Method | Path | 说明 |
|---|---|---|
| GET | `/api/stores` | 当前租户门店列表 |
| POST | `/api/stores` | `{ name, address, phone, businessHours, status }` |
| PUT | `/api/stores/{id}` | 编辑 |
| DELETE | `/api/stores/{id}` | 删除（有会员/员工禁用） |

## 7. 员工 / Employees

| Method | Path | 说明 |
|---|---|---|
| GET | `/api/employees` | 参数 `storeId` `role` |
| POST | `/api/employees` | `{ username, password, name, phone, role, storeIds }` |
| PUT | `/api/employees/{id}` | 编辑（不含密码） |
| PUT | `/api/employees/{id}/password` | 重置密码 |
| DELETE | `/api/employees/{id}` | 禁用 |
| GET | `/api/employees/{id}/performance` | 业绩 `{ month, amount, count }` |

## 8. 数据看板 / Stats

| Method | Path | 出参 data |
|---|---|---|
| GET | `/api/stats/overview` | `{ revenue, revenueDelta, memberCount, memberDelta, orderCount, orderDelta, avgPrice, avgPriceDelta }` 含近 30 天对比 |
| GET | `/api/stats/trend` | 参数 `range(7d/30d/90d)` `metric(revenue/orders/members)` → `[{ date, value }]` |
| GET | `/api/stats/member-growth` | `[{ date, newCount, activeCount }]` |
| GET | `/api/stats/top-services` | `[{ name, count, amount }]` 前 10 |
| GET | `/api/stats/rfm` | `{ high, mid, low, dormant }` 分层占比与人数 |
| GET | `/api/stats/hour` | 24 小时下单分布 `[{ hour, count }]` |

## 9. 审计 / Audit

| Method | Path | 说明 |
|---|---|---|
| GET | `/api/audit/logs` | 参数 `page size operator action start end` |
| GET | `/api/audit/logins` | 登录日志 |

审计对象: `{ id, operatorId, operatorName, action, target, detail, ip, createdAt }`

## 10. 设置 / Settings（租户级）

| Method | Path | 说明 |
|---|---|---|
| GET | `/api/settings` | `{ tenantName, brandColor, levelRules[], smsSign, rechargeRules[] }` |
| PUT | `/api/settings` | 更新 |

## 11. H5 会员端 / H5（公开，凭会员 token）

H5 端使用独立 `memberToken`，登录方式：手机号 + 验证码（MVP 用固定验证码 8888）。

| Method | Path | 说明 |
|---|---|---|
| POST | `/api/h5/login` | `{ phone, code }` → `{ memberToken, member }` |
| GET | `/api/h5/profile` | 会员卡面（含等级/余额/积分） |
| GET | `/api/h5/balance` | 储值余额 + 近 5 笔流水 |
| GET | `/api/h5/coupons` | 我的券 `status` |
| GET | `/api/h5/coupons/available` | 可领取的券 |
| POST | `/api/h5/coupons/{id}/claim` | 领券 |
| GET | `/api/h5/transactions` | 消费记录 |
| GET | `/api/h5/stores` | 附近门店 |

## 12. 错误码

| code | HTTP | 含义 |
|---|---|---|
| `SESSION_EXPIRED` | 401 | 登录过期 |
| `FORBIDDEN` | 403 | 无权限 |
| `NOT_FOUND` | 404 | 资源不存在 |
| `VALIDATION` | 422 | 参数校验失败 |
| `CONFLICT` | 409 | 冲突（如手机号重复） |
| `BIZ_ERROR` | 400 | 业务异常 |
| `SERVER_ERROR` | 500 | 服务异常 |
