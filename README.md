# 星河·会记 SaaS

> 面向美业、餐饮、门店零售的一站式 **多租户 SaaS 会员经营平台**。
> 以**会员储值 + 营销增长 + 门店数字化经营**为核心，一套系统覆盖收银、会员、储值、优惠券、营销活动、扫码点餐、线上商城、赢奖小游戏、微信公众号与微信支付等场景，帮助商户把「到店顾客」沉淀为「长期会员」，并用多套营销工具持续激活复购。

<p align="left">
  <img src="https://img.shields.io/badge/后端-Java%2017%20%2F%20Spring%20Boot%203.2-green" alt="backend">
  <img src="https://img.shields.io/badge/前端-Vue%203%20%2F%20TypeScript-blue" alt="frontend">
  <img src="https://img.shields.io/badge/数据-MySQL%20%2F%20H2-orange" alt="db">
  <img src="https://img.shields.io/badge/鉴权-JWT%20%2F%20RBAC-purple" alt="auth">
  <img src="https://img.shields.io/badge/微信-公众号%20%2F%20支付-brightgreen" alt="wechat">
</p>

**文档导航**：[后端启动说明](server/README.md) · [API 契约](docs/API.md) · [仓库地址](https://github.com/JunQiya/saas-huiji)

---

## 目录

- [1. 项目简介](#1-项目简介)
- [2. 功能全景](#2-功能全景)
- [3. 技术架构](#3-技术架构)
- [4. 技术栈](#4-技术栈)
- [5. 快速开始](#5-快速开始)
- [6. API 契约约定](#6-api-契约约定详见-docsapimd)
- [7. 安全与工程质量](#7-安全与工程质量)
- [8. 目录结构总览](#8-目录结构总览)
- [9. 部署要点（prod）](#9-部署要点prod)
- [10. 版本与迭代](#10-版本与迭代)

---

## 1. 项目简介

本项目是一个**前后端分离的企业级多租户 SaaS** 单体仓库（Monorepo），由三个子项目构成：

| 子项目 | 说明 | 端口 |
|---|---|---|
| `server/` | 后端服务（Java 17 + Spring Boot 3.2） | 8081 |
| `admin/` | 商户后台管理端（Vue 3 + Element Plus + ECharts） | 5173 |
| `h5/` | 会员移动端 H5（Vue 3 + Vant，适配微信内打开） | 5174 |

三个端共享同一套 `docs/API.md` 契约，遵循统一的响应格式、JWT 鉴权、多租户数据隔离与 RBAC 角色控制。

### 核心理念

- **多租户**：一套系统服务多个商户，数据按 `tenantId` 严格隔离，各租户拥有独立的门店、员工、会员与营销数据。
- **会员经营闭环**：开卡建档 → 储值充值与消费扣款 → 积分自动累积 → 等级自动升级 → 优惠券 / 营销活动触达 → 微信消息 / 短信召回 → 老客转介绍拉新，形成完整增长飞轮。
- **线上线下打通**：门店收银、桌台点餐、厨房工单与线上商城、扫码点餐、会员端同库同套，数据实时联动。
- **营销工具箱**：优惠券、营销活动（生日 / 沉睡 / 回购 / 人工）、赢奖小游戏（大转盘 / 刮刮乐 / 砸金蛋 / 摇一摇）、推荐裂变，一站配齐。

---

## 2. 功能全景

### 2.1 经营总览

| 模块 | 说明 |
|---|---|
| **数据看板** | 营业额与环比、会员数、订单量、客单价等 KPI，近 30 天趋势、会员增长、TOP10 服务项目、RFM 分层、24 小时下单分布，支持多门店切换 |
| **收银台 POS** | 按门店筛选商品与服务、购物车结算、会员储值优先扣款、优惠券核销、开单收银 |
| **商品服务** | 服务 / 商品目录、分类管理、按门店配置适用商品、封面图管理 |
| **订单流水** | 全渠道订单查询（收银 / 点餐 / 商城）、订单详情、状态流转 |
| **桌台管理** | 桌台开台 / 并台 / 换桌 / 结账，桌台状态实时可见 |
| **厨房工单** | 点餐自动生成后厨工单，支持轮询刷新、出菜状态管理 |

### 2.2 会员与储值

| 模块 | 说明 |
|---|---|
| **会员管理** | 开卡建档（姓名 / 手机 / 性别 / 生日）、标签、门店归属、积分与等级调整、消费明细、资金流水 |
| **等级体系** | 按累计消费自动升级，等级门槛由租户配置，支持自定义等级规则 |
| **储值钱包** | 充值（含赠送金额）、消费扣款、储值流水，金额以「分」为单位精确传输 |
| **推荐裂变** | 邀请码分享、上下级绑定、分销奖励记录、手机号脱敏展示 |
| **会员端（H5）** | 电子会员卡、余额 / 积分 / 等级可视化、我的券、领券中心、消费记录、附近门店 |

### 2.3 营销中心

| 模块 | 说明 |
|---|---|
| **优惠券** | 满减券、折扣券、体验券、生日券；支持限时 / 限天数有效期、总量与每人限领、按门店发放、扫码核销、发放与核销记录 |
| **营销活动** | 生日关怀、沉睡唤醒、回购刺激、人工群发四大类型；支持短信 / 微信 / App 内触达；启停、命中人数预览、触发 / 触达 / 转化统计 |
| **赢奖小游戏** | 大转盘、刮刮乐、砸金蛋、摇一摇四种玩法；后台配置奖品池 / 概率 / 每人次数，H5 端即开即玩 |
| **营销日历** | 以日历视图总览各活动排期，便于制定月度营销计划 |

### 2.4 线上商城

- **后台**：商城分类、商品管理、商城订单管理（发货 / 收货确认等）。
- **H5**：商城首页、商品详情、购物车、下单结算、商城订单中心、物流信息，支持微信支付。

### 2.5 扫码点餐

- 门店入口（扫描桌台二维码 / 门店列表选择）→ 菜单浏览（分类）→ 加购下单 → 提交后自动生成后厨工单。
- 免登录即可浏览菜单，下单时引导登录会员。

### 2.6 组织与运营

| 模块 | 说明 |
|---|---|
| **门店管理** | 多门店增删改查、启停开关、营业时间与联系电话 |
| **员工管理** | 员工账号（店长 / 员工 / 收银三种角色）、门店归属、重置密码、业绩统计、导入导出 |
| **代理商** | 代理商体系与分佣管理 |

### 2.7 系统与合规

| 模块 | 说明 |
|---|---|
| **微信公众号** | 公众号配置、OAuth 授权登录、JS-SDK、模板消息下发（防开放重定向） |
| **微信支付** | 微信支付下单、回调通知、订单查询（H5 端支付） |
| **消息中心** | 短信群发任务（真实扣减短信余额）、任务模板、发送记录 |
| **报表中心** | 业务报表、PDF / Excel / CSV 导出、定时报表任务 |
| **审计日志** | 操作审计 + 登录日志，全量留痕 |
| **系统设置** | 租户名称、品牌色（实时生效）、等级规则、充值规则、短信签名、功能开关、套餐信息 |
| **安全** | JWT 无状态鉴权、RBAC 角色校验、乐观锁防并发、跨租户越权校验、密码 BCrypt 加密 |

---

## 3. 技术架构

### 3.1 整体架构

```
┌─────────────────────┐      ┌──────────────────┐
│   admin 管理端       │      │   h5 会员端       │
│  Vue3 + ElementPlus │      │  Vue3 + Vant     │
│  ECharts 数据看板    │      │  微信内嵌 H5       │
│  Vite dev :5173     │      │  Vite dev :5174  │
└─────────┬───────────┘      └────────┬─────────┘
          │      HTTP / JSON (/api)     │
          └─────────────┬──────────────┘
                        ▼
                ┌────────────────┐
                │   huiji-server  │  Spring Boot :8081
                │  JWT 无状态鉴权   │
                │  RBAC 角色控制   │
                │  多租户数据隔离   │
                │  定时任务/异步    │
                └────────┬───────┘
                         ▼
           ┌─────────────┼──────────────┐
           ▼             ▼              ▼
        MySQL          H2 (test)   微信/短信/支付
      生产 / dev      零依赖内存库     外部服务
```

> `test` profile 使用 H2（MySQL 兼容模式），无需安装 MySQL 即可完整启动与联调；`dev` / `prod` 使用 MySQL。

### 3.2 后端分层设计（`server/src/main/java/com/huiji/`）

| 包 | 职责 | 关键类 |
|---|---|---|
| `common/` | 统一响应与异常体系 | `Result`、`PageData`、`ErrorCode`、`BizException`、`GlobalExceptionHandler` |
| `security/` | 鉴权与权限控制 | `JwtUtil`、`MemberTokenUtil`、`JwtAuthFilter`、`SecurityConfig`、`PreAllowed` + `RoleAspect`、`PlanLimitCheck` + `PlanLimitAspect`、`LoginUserHolder`、`MemberContext` |
| `entity/` | JPA 领域实体 | `BaseEntity`（统一主键 / tenantId / 软删除 / 时间戳 / `@Version` 乐观锁）+ 30 个实体 + 2 个 JPA Converter |
| `repository/` | 数据访问层 | 30 个 Spring Data JPA Repository |
| `dto/` | 请求 / 响应对象 | 15 个 DTO 按业务域拆分 |
| `service/` | 业务逻辑 | 23 个业务服务 + `AuditHelper` + `CsvHelper` |
| `controller/` | HTTP 入口 | 27 个控制器（含 H5 会员端、微信、微信支付） |
| `config/` | 定时任务与异步 | `SchedulingConfig`、`MessageSchedulerHook`、`ReportSchedulerHook`、`AsyncConfig` |
| `init/` | 演示数据初始化 | `DataInitializer`（test / dev profile 自动填充种子数据） |

> 服务层采用「`@Transactional` + Service 事务下沉」的模式；`@Async` 自调用场景已抽取独立 Hook Bean，避免事务失效。

### 3.3 数据模型（30 个实体）

- **组织**：`Tenant`（租户）、`TenantSetting`（租户配置）、`Store`（门店）、`User`（后台用户）、`Agent`（代理商）
- **会员**：`Member`（会员）、`MemberTag`（会员标签）、`WalletTransaction`（资金流水）、`Referral`（推荐关系）
- **交易**：`Order`、`OrderItem`、`OrderExtend`（订单扩展）、`Product`（商品 / 服务）、`Cart`（购物车）
- **营销**：`Coupon`（优惠券）、`CouponRecord`（券发放 / 核销记录）、`Campaign`（营销活动）、`CampaignLog`（活动日志）
- **点餐 / 后厨**：`DiningTable`（桌台）、`MenuCategory`（菜单分类）、`KitchenOrder`（厨房工单）
- **商城**：`MallCategory`（商城分类）、`WxAccount`（公众号配置）
- **游戏**：`Game`、`GamePrize`（奖品）、`GamePlay`（参与记录）
- **系统**：`AuditLog`（操作审计）、`LoginLog`（登录日志）、`MessageTask`（消息任务）、`ReportTask`（报表任务）

所有实体继承 `BaseEntity`：自增主键、`tenantId` 租户字段、`is_deleted` 软删除、`created_at / updated_at` 时间戳、`version` 乐观锁版本号。

---

## 4. 技术栈

### 4.1 后端（server/）

| 类别 | 选型 |
|---|---|
| 语言 / 框架 | Java 17、Spring Boot 3.2.12、Spring Web |
| 安全 | Spring Security、jjwt 0.12.x（JWT）、BCrypt |
| 数据 | Spring Data JPA（Hibernate）、MySQL Connector、H2（test 内存库） |
| 校验 | Jakarta Validation（Bean Validation） |
| 报表 | iTextPDF 5.5.13（PDF）、Apache POI 5.2.5（Excel）、CSV |
| 微信生态 | weixin-java-mp 4.6.0（公众号）、weixin-java-pay 4.6.0（微信支付） |
| 代码精简 | Lombok 1.18.38 |

### 4.2 管理端（admin/）

| 类别 | 选型 |
|---|---|
| 框架 | Vue 3.5（Composition API）、Vue Router 4、Pinia |
| UI | Element Plus 2.8（按需引入）、ECharts 5（按需引入，看板图表） |
| 请求 | Axios（拦截器统一处理 token / 401 / 错误提示） |
| 构建 | Vite 5 + TypeScript、unplugin-auto-import / unplugin-vue-components |
| 其他 | qrcode（优惠券二维码展示）、CSV 导出、`usePolling` 轮询 composable、深色模式（html.dark + localStorage 持久化） |

### 4.3 会员端（h5/）

| 类别 | 选型 |
|---|---|
| 框架 | Vue 3.5、Vue Router 4、Pinia |
| UI | Vant 4.9（按需引入） |
| 微信 | `wx-sdk` 封装（JS-SDK 调用）、OAuth 授权登录 |
| 构建 | Vite 5 + TypeScript、target es2015（兼容低端安卓 / 微信 WebView） |

---

## 5. 快速开始

### 5.1 环境要求

- **JDK 17（必需）**：后端要求 Java 17，Lombok 1.18.38 不兼容更高版本 JDK 的 javac。
  - 查看本机 JDK：`/usr/libexec/java_home -V`
  - 若默认 JDK 非 17，构建 / 启动前执行：`export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home`
- Maven 3.6+
- Node.js 18+（前端）
- MySQL 5.7 / 8.x（仅 `dev` / `prod` profile 需要；`test` profile 零外部依赖）

### 5.2 启动后端（零依赖：H2 内存库）

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
cd server
mvn spring-boot:run -Dspring-boot.run.profiles=test
```

- `test` profile：H2 内存库（MySQL 兼容模式）+ 自动种子数据，**无需安装 MySQL**，适合快速体验与联调。
- H2 控制台：`http://localhost:8081/h2-console`（JDBC URL `jdbc:h2:mem:huiji`，用户 `sa`，无密码）。

### 5.3 启动后端（dev：本地 MySQL）

```bash
# 1. 准备 MySQL
CREATE DATABASE huiji DEFAULT CHARSET utf8mb4;
# 2. 启动（默认 datasource：localhost:3306 / root / 123456）
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 5.4 启动管理端（admin，端口 5173）

```bash
cd admin
npm install
npm run dev
```

### 5.5 启动会员端（h5，端口 5174）

```bash
cd h5
npm install
npm run dev
```

> 两个前端均通过 Vite dev proxy 将 `/api` 转发到后端 `localhost:8081`，无需额外配置跨域。

### 5.6 默认演示账号

| 端 | 账号 | 密码 | 角色 |
|---|---|---|---|
| 后台 | `admin` | `123456` | TENANT_ADMIN（租户管理员） |
| 后台 | `wang mgr` | `123456` | STORE_MANAGER（店长） |
| 后台 | `li staff` | `123456` | STAFF（员工） |
| 后台 | `zhao cash` | `123456` | CASHIER（收银） |
| H5 | 手机号 `13800000001`（张伟） | 验证码 `8888` | 会员 |

内置演示数据：默认租户「星河·会记演示」、2 家门店、10 个示例会员、4 张优惠券、3 个营销活动、跨 60 天的资金流水与订单，启动后看板数据即可直接展示。

---

## 6. API 契约约定（详见 `docs/API.md`）

- **Base URL**：`/api`；除 `/api/auth/login`、`/api/h5/**`、微信相关公开接口外，请求头需带 `Authorization: Bearer <token>`。
- **统一响应**：
  - 成功：`{ "ok": true, "data": ... }`
  - 分页：`{ "ok": true, "data": { "list": [], "total": 0, "page": 1, "size": 20 } }`
  - 失败：`{ "ok": false, "message": "...", "code": "..." }`
  - 登录过期：`{ "ok": false, "code": "SESSION_EXPIRED", "message": "登录已过期" }`（HTTP 401）
- **金额**：一律以「分」为单位传输（整数），展示时除以 100。
- **时间**：ISO-8601 字符串；**分页**：`page` 从 1 起、`size` 默认 20。
- **角色**：`TENANT_ADMIN` / `STORE_MANAGER` / `STAFF` / `CASHIER`，服务端通过 `@PreAllowed` 注解做接口级校验。
- **鉴权双通道**：后台登录签发 `token`（JWT 内含 `tenantId`）；H5 会员登录签发独立 `memberToken`。
- **错误码**：`SESSION_EXPIRED`(401)、`FORBIDDEN`(403)、`NOT_FOUND`(404)、`VALIDATION`(422)、`CONFLICT`(409)、`BIZ_ERROR`(400)、`SERVER_ERROR`(500)。

### 主要 API 域

- **Auth**：登录 / 登出 / 个人信息 / 修改密码
- **Members**：会员 CRUD、储值、消费、标签、流水、持有的券
- **Coupons**：券管理、发放、核销（二维码）、发放记录
- **Campaigns**：活动 CRUD、启停、命中预览、转化统计
- **Stores / Employees**：门店与员工管理、员工业绩
- **Stats**：看板 6 组统计接口（overview / trend / growth / top-services / rfm / hour）
- **Orders / Products / Dining / Kitchen / Mall**：交易域全套接口
- **Games**：四种游戏的后台配置与 H5 参与接口
- **Referral**：推荐关系与奖励
- **Messages / Reports**：短信群发任务与报表导出
- **Audit**：操作审计、登录日志
- **Settings**：租户级设置与功能开关
- **H5**：登录、会员卡、余额、券、流水、门店、点餐、商城、游戏、推荐等会员端接口
- **Wx / WxPay**：公众号 OAuth、JS-SDK、模板消息、微信支付下单 / 回调 / 查询

---

## 7. 安全与工程质量

- **鉴权与授权**：Spring Security 无状态 JWT + `JwtAuthFilter`；401 / 403 统一 JSON 返回；后台用户与会员两套 token 体系、两套 Holder 上下文。
- **多租户隔离**：`tenantId` 贯穿全部实体与查询，登录后从 token 解析，防止跨租户数据越权。
- **RBAC**：`@PreAllowed` 注解 + AOP 切面校验 `TENANT_ADMIN / STORE_MANAGER / STAFF / CASHIER` 四级角色。
- **套餐限制**：`PlanLimitCheck` + AOP 对租户套餐配额做上限校验。
- **数据一致性**：`@Version` 乐观锁防并发丢失更新；软删除统一过滤；SQL 标识符自动加反引号规避 MySQL 保留字。
- **密钥管理**：JWT 密钥、数据库口令均支持环境变量注入，生产不落明文。
- **审计留痕**：登录日志与操作审计双日志，重要写操作自动记录操作人、动作、目标与详情。
- **错误语义**：业务失败一律按 200 + 业务码返回，避免与接口 404 / 500 混淆；补充 405、参数类型不匹配等异常处理器。
- **E2E 验证**：`.test-scripts/` 内置 Playwright 全页面回归脚本，曾对 admin / h5 全量页面跑通验证。

---

## 8. 目录结构总览

```
saas-huiji/
├── README.md            # 本文件（项目介绍）
├── docs/
│   └── API.md           # 前后端三方共享的 API 契约
├── server/              # 后端：Spring Boot 3.2（Java 17）
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/huiji/
│       │   ├── common/       # 统一响应 / 异常 / 错误码
│       │   ├── security/     # JWT / RBAC / 多租户 / 套餐限制
│       │   ├── entity/       # BaseEntity + 30 个 JPA 实体
│       │   ├── repository/   # 30 个 Repository
│       │   ├── dto/          # 15 个请求 / 响应 DTO
│       │   ├── service/      # 23 个业务服务
│       │   ├── controller/   # 27 个控制器
│       │   ├── config/       # 定时任务 / 异步配置
│       │   └── init/         # DataInitializer 演示数据
│       └── resources/
│           ├── application.yml       # 公共配置（prod 指向 MySQL）
│           ├── application-dev.yml   # dev：本地 MySQL + 种子数据
│           └── application-test.yml  # test：H2 内存库，零依赖
├── admin/               # 商户管理端：Vue 3 + Element Plus
│   └── src/
│       ├── api/         # Axios 封装 + 接口定义
│       ├── views/       # 22 个业务页面 + 登录 / 404
│       ├── components/  # KpiCard / ChartCard 等复用组件
│       ├── layouts/     # 主布局（侧边栏由路由 meta 自动生成）
│       ├── stores/      # Pinia 用户 / 会话状态
│       └── router/      # 路由 + 登录守卫
└── h5/                  # 会员端 H5：Vue 3 + Vant
    └── src/
        ├── api/         # H5 接口定义
        ├── views/       # 25 个页面（会员卡 / 券 / 商城 / 点餐 / 游戏 / 我的…）
        ├── components/  # NavBar / TabBar / 会员卡等复用组件
        ├── utils/       # 微信 SDK / 请求封装 / 格式化
        ├── constants/   # tab 配置与业务常量
        └── router/      # 路由 + 登录守卫
```

---

## 9. 部署要点（prod）

1. 准备 MySQL：`CREATE DATABASE huiji DEFAULT CHARSET utf8mb4;`，表结构由 Hibernate `ddl-auto: update` 自动维护。
2. 通过环境变量注入敏感配置：`DB_URL` / `DB_USERNAME` / `DB_PASSWORD` / `JWT_SECRET`（≥32 字符）。
3. 使用 `prod` profile 启动（关闭演示数据与短信验证码回显）：

   ```bash
   mvn -DskipTests package
   $JAVA_HOME/bin/java -jar target/huiji-server-1.0.0.jar --spring.profiles.active=prod
   ```

4. 前端构建产物分别部署：`admin/dist`、`h5/dist`，由 Nginx 等反向代理 `/api` 与微信回调路径至后端。
5. 如需对接真实短信 / 公众号 / 微信支付，在后台「系统设置」「微信公众号」中配置，并通过 `huiji.h5.sms-dev-mode: false` 关闭验证码回显。

---

## 10. 版本与迭代

项目当前处于 **MVP 完善阶段**（v0.1.0），代码注释与提交信息保持中文。主要迭代亮点（详见 `git log`）：

- **功能补齐**：线下扫码点餐、线上商城、赢奖小游戏（大转盘 / 刮刮乐 / 砸金蛋 / 摇一摇）、微信 OAuth / JS-SDK / 模板消息 / 微信支付、代理商体系、短信验证码（真实发码 + 限流 + dev 回显）。
- **性能优化**：Admin 按需引入 Element Plus / ECharts（主包 1MB → 399KB，echarts 1MB → 521KB）、H5 去除 Vant 全量引入。
- **工程化**：新增 `test` profile（H2 零依赖启动验证）、`usePolling` / `useGamePage` 等 composable 抽取、菜单与面包屑由路由 meta 自动生成。
- **稳定性修复**：修复 @Async 自调用绕过事务、tenant 主键回填致全站 500、跨租户越权、乐观锁并发丢失更新等严重问题。

### 注意事项

- 微信生态接口需在已认证的公众号 AppID 环境下验证；未配置真实公众号时，相关接口已做优雅降级（不报 500）。
- 生产环境请务必替换所有默认密钥、默认账号密码与演示配置（`JWT_SECRET`、数据库口令、`sms-code`、公众号密钥等）。
