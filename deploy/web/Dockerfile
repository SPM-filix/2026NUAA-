# 构建上下文必须是仓库根目录：docker build -f deploy/web/Dockerfile .
# 生产镜像必须关闭 Mock，否则前端会走本地 fake 数据，登录不经过后端
FROM node:22-alpine AS user-build
WORKDIR /app
ENV VITE_USE_MOCK=false
COPY user-web/package.json user-web/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY user-web/ ./
RUN npm run build

FROM node:22-alpine AS admin-build
WORKDIR /app
ENV VITE_USE_MOCK=false
COPY admin-web/package.json admin-web/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY admin-web/ ./
# admin-web/src/mock/db.js 通过 ../../../mocks 引用仓库根目录种子数据
COPY mocks /mocks
# 管理端挂在 /admin/ 路径下，资源与路由 base 同步
RUN npm run build -- --base=/admin/

FROM nginx:1.27-alpine
COPY deploy/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=user-build /app/dist /usr/share/nginx/html
COPY --from=admin-build /app/dist /usr/share/nginx/html/admin
EXPOSE 80
