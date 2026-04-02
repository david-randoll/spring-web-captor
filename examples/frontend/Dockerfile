# Build stage
FROM node:23-alpine3.20 AS builder
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
ARG VITE_BACKEND_URL=http://localhost:11006
ENV VITE_BACKEND_URL=$VITE_BACKEND_URL
RUN npm run build

# https://github.com/lipanski/docker-static-website/tree/master
FROM lipanski/docker-static-website:latest
WORKDIR /var/www/html
COPY --from=builder /app/dist ./
EXPOSE 11007
CMD ["/busybox-httpd", "-f", "-v", "-p", "11007"]