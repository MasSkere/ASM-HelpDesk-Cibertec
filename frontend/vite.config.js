import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// En desarrollo el frontend corre en 5173 y hace proxy de /api al backend 8080,
// evitando problemas de CORS. En producción (Nginx) se usa VITE_API_BASE_URL.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.VITE_PROXY_TARGET || 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
