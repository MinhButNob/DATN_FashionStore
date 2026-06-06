# Giai đoạn 1: Đã nâng cấp lên Node.js bản 20 để tương thích với Vite 7
FROM node:20-alpine AS build-stage
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# Giai đoạn 2: Dùng Nginx làm Web Server phục vụ các file tĩnh
FROM nginx:stable-alpine AS production-stage
COPY --from=build-stage /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
