# ---- build stage ----
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json ./
RUN npm install --no-audit --no-fund
COPY . .
# Leave empty: the browser then talks to the gateway on the same host it loaded the page from, port 9090.
ARG VITE_API_BASE=
ARG VITE_GATEWAY_PORT=9090
ENV VITE_API_BASE=$VITE_API_BASE
ENV VITE_GATEWAY_PORT=$VITE_GATEWAY_PORT
RUN npm run build

# ---- runtime stage: unprivileged nginx (listens on 9090, no root needed) ----
FROM nginxinc/nginx-unprivileged:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 9090
