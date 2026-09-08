# ---------- Build stage ----------
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build -- --configuration production

# ---------- Runtime stage ----------
FROM nginx:1.27-alpine
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80

# busybox wget ships in nginx:alpine already — no extra package needed. 127.0.0.1, not
# localhost: this nginx config only binds IPv4 (0.0.0.0:80), but /etc/hosts resolves
# "localhost" to ::1 first — wget doesn't fall back to IPv4, so it reports a false "connection
# refused" against a server that's actually fine. Confirmed live.
HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=5 \
  CMD wget -qO- http://127.0.0.1:80/ >/dev/null || exit 1
