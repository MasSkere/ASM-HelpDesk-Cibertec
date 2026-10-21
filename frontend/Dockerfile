# =============================================================================
#  Dockerfile multi-stage — Frontend ASM-HelpDesk-Cibertec (React + Vite)
# -----------------------------------------------------------------------------
#  Contexto de build: ./frontend   (donde vive el package.json)
#  Uso:
#    docker build -t asm-helpdesk-frontend ./frontend
# =============================================================================

# ------------------------- Etapa 1: BUILD con Node ---------------------------
FROM node:20-alpine AS build
WORKDIR /app

# 1) Dependencias (capa cacheable)
COPY package.json package-lock.json* ./
RUN npm ci

# 2) Código y build de producción
COPY . .
RUN npm run build

# ---------------------- Etapa 2: RUNTIME con Nginx ---------------------------
FROM nginx:1.27-alpine

# Configuración con fallback SPA y proxy /api -> backend
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Solo los archivos estáticos compilados
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
