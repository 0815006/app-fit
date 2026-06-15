# =============================================
# 本地 docker-compose 构建用 Dockerfile
# 上下文：web-fit-vue/
# =============================================
FROM node:22-alpine AS builder
WORKDIR /build
COPY package.json ./
RUN npm install --registry=https://registry.npmmirror.com
COPY . .
RUN npm run build

FROM nginx:alpine
# 用自定义 nginx 配置替换默认配置
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /build/dist /usr/share/nginx/html
EXPOSE 80