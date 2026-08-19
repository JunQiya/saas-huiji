# 星河·会记 — 阿里云容器化上线部署

> 目标：2 个容器（后端 + 统一网关）+ 阿里云 RDS MySQL，单域名 `huiji.lxxno.cn` 路径分发，云效 Flow 自动构建镜像 → 推送 ACR → 部署，并限制每个容器的内存/CPU。

## 一、部署架构与资源限制

```
[SLB/Ingress] ──▶ huiji.lxxno.cn ──▶ gateway 容器(nginx 统一网关)
                                        ├─ /        → 官网介绍页(web/)
                                        ├─ /admin/  → 管理后台静态(history 回退)
                                        ├─ /h5/     → H5 会员端静态(history 回退)
                                        └─ /api/    → 反向代理
                                             └──▶ server 容器(Java 8081) ──▶ RDS MySQL
```

| 容器 | 镜像 | CPU limit | 内存 limit | 说明 |
|---|---|---|---|---|
| server | `server:tag` | 1 核 | **1Gi** | Spring Boot，JVM `-Xmx512m`（与 limit 配套，防 OOMKilled）|
| gateway | `gateway:tag` | 500m | **256Mi** | Nginx 统一网关：官网+admin+h5+`/api` 反代 |

内存/CPU 限制定义在 **两处**：
- 单机/本地：`deploy/docker-compose.yml` 的 `mem_limit` / `cpus`
- K8s/ASK：`deploy/k8s/*.yaml` 的 `resources.limits` / `resources.requests`
- 后端 JVM 堆上限在 `deploy/server.Dockerfile` 的 `JAVA_OPTS`（`-Xmx512m -XX:MaxRAMPercentage=75`），防止 JVM 按宿主内存自适应把容器撑爆被强制杀死。

## 二、阿里云资源准备（一次性）

1. **RDS MySQL 8.0**（杭州/上海等区域）
   - 创建实例 → 创建数据库 `huiji`（utf8mb4）→ 创建账号 `huiji_app`，仅授 `huiji` 库权限
   - 白名单加入 ASK/ECS 所在 VPC 网段（或本机 IP 用于导入数据）
   - 初始化数据：本机导出演示数据后导入，或 `server/deploy/huiji-db.sql`
2. **ACR 容器镜像服务**
   - 创建命名空间（如 `huiji`）→ 创建镜像仓库 `server` / `gateway`（公开/私有均可，ASK 拉取需凭证）
3. **ASK 容器服务（Serverless K8s）**
   - 创建集群（与 RDS 同 VPC）→ 启用 **ALB Ingress**
4. **域名与证书**
   - 备案域名：`huiji.lxxno.cn`（主站 /admin /h5 同一域名路径区分）
   - 申请 SSL 证书（阿里云免费证书），域名 CNAME/解析指向 ALB/SLB
5. **云效**（flow.aliyun.com）开通

## 三、本地构建镜像验证

```bash
# 后端(多阶段: maven 构建 + JRE 运行)
docker build -f deploy/server.Dockerfile -t huiji-server:1.0.0 .
# 管理后台
docker build -f deploy/admin.Dockerfile -t huiji-admin:1.0.0 .
# H5
docker build -f deploy/h5.Dockerfile -t huiji-h5:1.0.0 .

# 本地编排(含内存/CPU 限制, 自带 MySQL 供验证)
DB_URL='jdbc:mysql://localhost:13306/huiji?...' \
DB_USERNAME=root DB_PASSWORD=root123456 JWT_SECRET='<随机>' H5_DOMAIN='http://localhost:8080/h5' \
docker compose -f deploy/docker-compose.yml up -d --build
# 本地访问: http://localhost:8080 官网, /admin 后台, /h5 会员端
```

## 四、部署到 ASK

```bash
# 1. 命名空间 + 环境变量 Secret(RDS 连接串/JWT/域名等, 见 01-namespace.yaml 头部注释)
kubectl create ns huiji
kubectl -n huiji create secret generic huiji-env \
  --from-literal=DB_URL='jdbc:mysql://<RDS内网>:3306/huiji?useUnicode=true&characterEncoding=utf8&useSSL=true&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true' \
  --from-literal=DB_USERNAME='huiji_app' \
  --from-literal=DB_PASSWORD='<密码>' \
  --from-literal=JWT_SECRET='<随机48位以上>' \
  --from-literal=H5_DOMAIN='https://huiji.lxxno.cn/h5' \
  --from-literal=CORS_ORIGINS='https://huiji.lxxno.cn' \
  --from-literal=SMS_DEV_MODE='false'

# 2. 部署前把 yaml 中 <ACR_NAMESPACE> 与 <IMAGE_TAG> 替换为真实值
kubectl -n huiji apply -f deploy/k8s/01-namespace.yaml \
  -f deploy/k8s/02-server.yaml -f deploy/k8s/03-admin.yaml -f deploy/k8s/04-h5.yaml

# 3. Ingress(域名 + ALB), host 填 huiji.lxxno.cn
kubectl -n huiji apply -f deploy/k8s/05-ingress.yaml

# 4. 检查
kubectl -n huiji get pods,svc,ingress
kubectl -n huiji get deploy -o wide
```

> 注意：镜像从 ACR 拉取，ASK 需配置拉取凭证（`kubectl create secret docker-registry` 或 ACR 免密插件）。

## 五、自动部署：云效 Flow

完整流水线配置见 `deploy/flow.md`。核心链路：

```
代码提交(GitHub/Code) → 云效流水线触发
  ├─ 构建后端镜像  server:${commit}
  ├─ 构建 admin 镜像 admin:${commit}
  ├─ 构建 h5 镜像   h5:${commit}
  └─ 推送到 ACR → kubectl set image 滚动更新 ASK 中 Deployment
```

## 六、ECS + docker-compose 拉取部署（小规模/单机）

适合单台 ECS 跑全部服务，配合云效 Flow 构建镜像推 ACR，ECS 上执行 `pull + up`。

### 1. 云效 Flow 侧：构建并推送镜像到 ACR

同第五章，构建 3 个镜像（`deploy/server.Dockerfile` / `admin.Dockerfile` / `h5.Dockerfile`）并推送到
`registry.cn-hangzhou.aliyuncs.com/<ACR_NAMESPACE>/server|admin|h5`，Tag 建议固定为 **latest**（ECS pull 最省事）或版本号。

### 2. ECS 首次配置（一次性）

```bash
# 2.1 安装 Docker(阿里云源)
curl -fsSL https://get.docker.com | bash
systemctl enable --now docker

# 2.2 登录 ACR(私有仓库拉取凭证; 在 ACR 控制台「访问凭证」获取固定密码)
docker login --username=<ACR账号> registry.cn-hangzhou.aliyuncs.com

# 2.3 放置部署文件到 /opt/huiji
mkdir -p /opt/huiji && cd /opt/huiji
# 从仓库拉取或拷贝:
#   deploy/docker-compose.pull.yml  → /opt/huiji/docker-compose.pull.yml
#   deploy/.env.example            → /opt/huiji/.env  (然后修改)
cp /path/to/.env.example /opt/huiji/.env
vi /opt/huiji/.env   # 填 RDS 地址/账号/密码/JWT_SECRET/域名
chmod 600 /opt/huiji/.env
```

### 3. 部署（云效触发后手动执行，或挂 webhook/定时任务）

```bash
cd /opt/huiji && \
docker compose -f docker-compose.pull.yml pull && \
docker compose -f docker-compose.pull.yml up -d && \
docker image prune -f
```

- `pull`：拉取 ACR 上最新镜像
- `up -d`：按 `.env` 注入环境变量启动容器，保留 `mem_limit/cpus` 资源限制
- `docker image prune -f`：清理旧镜像释放磁盘

### 4. 端口与域名

compose 默认：gateway 映射 `80`（`.env` 里 `GATEWAY_PORT` 可改）。流量入口：
- **单域名 `huiji.lxxno.cn`**：DNS 解析到 ECS 公网 IP（或 SLB），`/` 官网、`/admin` 后台、`/h5` 会员端由网关 nginx 自动分发
- HTTPS：SLB 监听 443 挂证书转发到 ECS 80，或宿主 nginx 做 SSL 终结后反代到 80

### 5. 日常更新

云效 Flow 每次提交自动构建推 `latest` → 在 ECS 重跑上述 3 条命令即可热更新（滚动重建）。

> 对比：ASK/K8s 方案（第四、五章）适合需要自动伸缩/多副本的场景；ECS + compose 更简单直接。

## 七、Java 安全加固清单

> 后端为 Spring Boot 3.2 / Java 17 容器化部署，以下清单分「必须 / 强烈建议 / 可不加」。

### 🔴 必须（代码层已实现，部署时核对）

| 项 | 实现位置 |
|---|---|
| 接口角色鉴权 `@PreAllowed` 全量覆盖 | `server/.../security/RoleAspect.java` |
| JWT 强密钥、启动强校验、不落明文 | `JwtUtil.java` + 环境变量注入 |
| 登录失败限流(5 次锁 5 分钟)、验证码 60s 限流 | `AuthService` / `SmsCodeService` |
| 支付回调金额校验 + 幂等入账 | `WxPayController.notify` / `RechargeService` |
| 生产关闭演示开关 | `application-prod.yml`：`sms-dev-mode=false`、`mall-demo-pay=false`、`recharge-demo-pay=false` |
| 数据库最小权限账号 | `server/deploy/deploy-prod.sh`（`huiji_app` 仅授业务库）|
| **非 root 运行容器** | `server.Dockerfile`（`USER appuser:10001`）+ `k8s/02-server.yaml`（`runAsNonRoot`）|
| **HTTPS 强制** | SLB/ALB 挂证书，开启跳转 |

### 🟡 强烈建议（部署时配置）

- RDS 开 SSL，`DB_URL` 使用 `useSSL=true`
- 云效流水线：构建前跑 `mvn -B test`（单测）、构建后加 **镜像安全扫描**（云效「镜像扫描」/ Trivy）
- 日志不打印密码/密钥/token（已按要求编码，部署后抽查 `app.log`）
- RDS 白名单仅放行 ECS/ASK 所在 VPC 网段

### 🟢 可不加（明确不建议）

- **swagger / springdoc**：本项目无，**不要引入**（内部 API 无需文档，减少攻击面）
- **actuator**：非必需；若后续要监控，只暴露 `health` 且与公网隔离
- **WAF**：量小可先依赖 ALB/SLB 基础防护，需更高防护再开阿里云 WAF

### 内存/CPU 限制（已配置）

- compose：`mem_limit` / `cpus`（server 1Gi/1核、admin/h5 128Mi/0.25核）
- K8s：`resources.limits/requests` + 后端 `JVM -Xmx512m` 配套，防 OOMKilled

## 八、上线前检查清单

- [ ] `SMS_DEV_MODE=false`、`SMS_FIXED_CODE=` 清空（接入真实短信网关）
- [ ] `JWT_SECRET` 为强随机值（≥48 位），不落明文
- [ ] 修改默认管理员密码 `admin/123456`
- [ ] 微信支付/公众号凭据在后台重新配置（`WxAccount`）
- [ ] 资源限制确认生效：`kubectl -n huiji get pod -o yaml | grep -A3 resources`
- [ ] 后端日志无 `OOMKilled`：`kubectl -n huiji logs deploy/huiji-server --previous`
- [ ] 观察 JVM 内存：容器 1Gi limit 下堆 512m 留足余量
