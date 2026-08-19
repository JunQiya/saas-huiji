# 云效 Flow 自动部署 — 完整操作步骤（ECS + ACR + Compose）

> 目标：代码推送到 `main` → 云效自动构建 2 个镜像（server / gateway）推 ACR → SSH 到 ECS 执行 `pull + up` 滚动更新。
> 全程在阿里云控制台完成，以下按顺序照做。

---

## 第 0 步：前置准备（一次性，半小时内）

### 0.1 代码仓库
代码推送到一个 Git 仓库（GitHub / 云效 Codeup / 自建 Git 均可）。仓库根目录需包含 `deploy/` 全部文件。

### 0.2 ACR 镜像仓库
1. 阿里云控制台 → 搜索 **容器镜像服务 ACR** → 开通
2. 创建**命名空间**：`huiji`
3. 在命名空间下创建 **3 个镜像仓库**：`server`、`admin`、`h5`（类型选「公开」最省事；私有需配拉取凭证）
4. 记录镜像地址：`registry.cn-hangzhou.aliyuncs.com/lxxno/huiji-server`（地域按你选的，下面以杭州 `cn-hangzhou` 为例）

### 0.3 ECS 准备
```bash
# 安装 Docker
curl -fsSL https://get.docker.com | bash
systemctl enable --now docker

# 登录 ACR(仅私有仓库需要; 在 ACR 控制台「访问凭证」创建固定密码)
docker login --username=<阿里云账号全名> registry.cn-hangzhou.aliyuncs.com

# 部署目录
mkdir -p /opt/huiji && cd /opt/huiji
# 把 deploy/docker-compose.pull.yml 放到这里(改名 docker-compose.pull.yml)
# 把 deploy/.env.example 复制为 .env 并修改
vi .env && chmod 600 .env
```
`.env` 必填项：`ACR_NAMESPACE=huiji`、`DB_URL`（RDS 内网）、`DB_USERNAME/PASSWORD`、`JWT_SECRET`、`H5_DOMAIN`、`CORS_ORIGINS`、`SMS_DEV_MODE=false`。

### 0.4 安全组
ECS 安全组放行：`80`(网关)、`22`(SSH，仅你的 IP)；SLB 场景另行配置。

---

## 第 1 步：开通云效并进入流水线

1. 访问 **flow.aliyun.com**，用阿里云账号登录（未开通过会自动引导创建企业，随便填企业名）
2. 创建/进入一个**项目**（云效 → 项目协作 → 新建项目 → 选「DevOps」模板）
3. 左侧菜单选 **流水线 Flow**（或顶部切换「流水线」）

---

## 第 2 步：绑定代码仓库

1. 流水线页面 → 右上角 **新建流水线**
2. 选择模板：搜索 **空模板**，点「创建」（或选「Docker 镜像构建」类模板再改造）
3. 流水线名称：`huiji-deploy`
4. 在「代码源」区点 **添加代码源**：
   - **代码仓库类型**：GitHub（需先授权）/ 云效 Codeup / 通用 Git
   - **仓库**：选择你的仓库
   - **默认分支**：`main`
   - **触发方式**：勾选 **Webhook 触发**（每次 push 自动构建）

---

## 第 3 步：配置「镜像构建并推送至 ACR 个人版」构建阶段

流水线默认有一个「阶段」（可改名 `构建`）。在阶段内添加 **2 个「镜像构建并推送至ACR」任务**（后端 + 统一网关，可并行）。

### 3.1 先建 ACR 个人版「服务连接」（一次）

1. 云效 Flow 页面 → 右上角 **设置** → **服务连接** → **新建服务连接**
2. 类型选 **「阿里云容器镜像服务（ACR）」**
3. 填写：
   - 连接名称：如 `huiji-acr`
   - **服务类型：个人版**（个人版 vs 企业版二选一，这里选个人版）
   - **地域 Region**：选你的镜像所在地域，如 `华东1（杭州）cn-hangzhou`
   - 点「验证并保存」，用当前登录的阿里云账号授权即可
4. 若提示需要 RAM 授权，按引导给云效授 ACR 推送权限（AliyunContainerRegistryFullAccess 或最小权限）

### 3.2 添加构建任务（每个镜像填一次）

在「构建」阶段点 **+ 添加任务** → 搜索并选择 **「镜像构建并推送至ACR」**，按如下填写：

| 参数 | 任务1(后端) | 任务2(统一网关) |
|---|---|---|
| 步骤 | 镜像构建并推送至ACR | 镜像构建并推送至ACR |
| **服务连接** | `huiji-acr`（个人版） | 同上 |
| **镜像仓库地址** | `registry.cn-hangzhou.aliyuncs.com/lxxno/huiji-server` | `.../lxxno/huiji-gateway` |
| **镜像版本 Tag** | `latest` | `latest` |
| **Dockerfile 路径** | `deploy/server.Dockerfile` | `deploy/gateway.Dockerfile` |
| **构建上下文** | 仓库根目录 `.` | `.` |

> 说明：
> - 「镜像仓库地址」也可通过下拉直接选服务连接下已有的 ACR 命名空间+仓库（`huiji`/`server`、`huiji`/`gateway`）
> - Tag 用 `latest` 最省事；需要回滚可改成变量 `${DATETIME}`，但 ECS 拉取侧要同步改 `.env` 的 `IMAGE_TAG`
> - server 镜像多阶段构建（内嵌 `mvn package`），**无需额外 Java 构建步骤**；gateway 镜像内嵌 admin+h5 的 `npm run build`，**无需额外 Node 构建步骤**
> - 两个任务放同一阶段内并勾选「并行」，构建更快

### 3.3 验证步骤配置正确

运行一次流水线后，去 **ACR 控制台 → 镜像仓库 → lxxno/huiji-server、lxxno/huiji-gateway → 镜像版本**，应能看到刚推送的 `latest`（或对应 Tag）。

---

## 第 4 步：配置「部署」阶段（SSH 到 ECS 拉取运行）

1. 点 **+ 添加阶段** → 命名 `部署`
2. 添加任务 → 步骤选「**主机部署**」
3. 首次需配置「**主机组**」：
   - 添加主机 → 填 ECS **公网 IP**、SSH 端口 `22`、登录账号 `root`、密码或密钥
4. 「主机部署」的 **部署脚本** 填：
   ```bash
   cd /opt/huiji && \
   docker compose -f docker-compose.pull.yml pull && \
   docker compose -f docker-compose.pull.yml up -d && \
   docker image prune -f
   ```
5. 保存

> 若不想让云效 SSH 到 ECS（安全原因），可以跳过本阶段，改为 ECS 上 `crontab` 每 5 分钟自动执行这 3 条命令（拉取到新 `latest` 自动重建）。

---

## 第 5 步：首次运行与验证

1. 流水线页点 **运行**
2. 观察三个阶段状态：
   - `构建`：3 个 Docker 构建任务（首次较慢，需拉 maven/node 基础镜像）
   - `部署`：SSH 到 ECS 执行 pull+up
3. 验证：
   ```bash
   # ECS 上
   docker ps                     # 应看到 huiji-server / huiji-gateway
   curl -s http://localhost:8081/api/h5/stores | head   # 后端健康(容器内网)
   curl -s http://localhost/ | head                     # 网关: 官网
   curl -s http://localhost/admin/ | head               # 网关: 管理后台
   curl -s http://localhost/h5/ | head                  # 网关: H5
   ```
4. 浏览器访问：`http://ECS公网IP/`（官网）、`/admin`（后台）、`/h5`（会员端）；域名解析生效后访问 `https://huiji.lxxno.cn/` 等

---

## 第 6 步：日常更新流程

之后每次 `git push` 到 `main`：
1. Webhook 自动触发流水线
2. 构建 3 镜像 → 推 ACR `latest`
3. 主机部署 SSH 到 ECS → `pull && up -d` 滚动重建
4. 观察云效运行记录，绿勾即成功

---

## 常见问题

| 问题 | 处理 |
|---|---|
| 拉镜像 401/denied | 私有仓库：ECS `docker login` 重新登录，或改用「公开」仓库 |
| Docker 构建失败 | 检查 Dockerfile 路径是否 `deploy/xxx.Dockerfile`、上下文是否 `.` |
| ECS pull 后没更新 | `.env` 里 `IMAGE_TAG` 改为 `latest`；确认云效推的就是 `latest` |
| 主机部署连不上 | 安全组放行 `22`、主机组用公网 IP、账号 `root` + 正确密码/密钥 |
| 内存/CPU 限制 | 已在 `docker-compose.pull.yml` 配置（server 1Gi/1核，admin/h5 128Mi/0.25核），`docker inspect` 可查 |
| 后端启动失败 | `docker logs huiji-server` 看日志；多为 `.env` 中 `DB_URL`/`JWT_SECRET` 配置问题 |
