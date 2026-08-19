# =============================================================================
# 星河·会记 管理后台 (Vue3 + Vite) — 构建静态资源 + Nginx 托管
# 构建: docker build -f deploy/admin.Dockerfile -t huiji-admin:1.0.0 .
# =============================================================================
FROM node:20-alpine AS build
WORKDIR /build
COPY admin/package*.json ./
RUN npm install --registry=https://registry.npmmirror.com
COPY admin/ ./
RUN npm run build

# ---------- 运行阶段 ----------
FROM nginx:1.27-alpine
COPY deploy/nginx-admin.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist /usr/share/nginx/html
EXPOSE 80
