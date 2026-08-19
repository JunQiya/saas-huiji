# =============================================================================
# 星河·会记 统一网关(官网 + 管理后台 + H5 + API 反代) — 单域名路径分发
#   构建 admin 与 h5 静态资源 → 与官网一起放进 nginx, 由 nginx-gateway.conf 分发
# 构建: docker build -f deploy/gateway.Dockerfile -t huiji-gateway:1.0.0 .
# 基础镜像走 DaoCloud 加速(docker.io 国内直拉超时); 若有内网 ACR 镜像可自行替换
# =============================================================================
FROM docker.m.daocloud.io/library/node:20-alpine AS admin-build
WORKDIR /build
COPY admin/package*.json ./
RUN npm install --registry=https://registry.npmmirror.com
COPY admin/ ./
RUN npm run build

FROM docker.m.daocloud.io/library/node:20-alpine AS h5-build
WORKDIR /build
COPY h5/package*.json ./
RUN npm install --registry=https://registry.npmmirror.com
COPY h5/ ./
RUN npm run build

# ---------- 运行阶段: 统一 nginx 网关 ----------
FROM docker.m.daocloud.io/library/nginx:1.27-alpine
# 官网(根路径)
COPY web/ /usr/share/nginx/html/
# 管理后台(/admin/) 与 H5(/h5/)
COPY --from=admin-build /build/dist /usr/share/nginx/html/admin/
COPY --from=h5-build /build/dist /usr/share/nginx/html/h5/
COPY deploy/nginx-gateway.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
