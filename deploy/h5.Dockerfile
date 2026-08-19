# =============================================================================
# 星河·会记 H5 会员端 (Vue3 + Vite + Vant) — 构建静态资源 + Nginx 托管
# 构建: docker build -f deploy/h5.Dockerfile -t huiji-h5:1.0.0 .
# =============================================================================
FROM node:20-alpine AS build
WORKDIR /build
COPY h5/package*.json ./
RUN npm install --registry=https://registry.npmmirror.com
COPY h5/ ./
RUN npm run build

# ---------- 运行阶段 ----------
FROM nginx:1.27-alpine
COPY deploy/nginx-h5.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist /usr/share/nginx/html
EXPOSE 80
