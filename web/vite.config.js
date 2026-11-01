import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// DD-P2-6: in prod the SPA is built into Spring Boot's static/ and served same-origin.
// In dev, proxy /api to the Boot app (default :8080) so fetch('/api/...') works unchanged.
// ARANG_API_TARGET lets `scripts/dev.sh` point the proxy at the dev backend (:8081).
const target = process.env.ARANG_API_TARGET || 'http://localhost:8080'

export default defineConfig({
  plugins: [react()],
  build: { outDir: 'dist' },
  server: {
    proxy: {
      '/api': { target, changeOrigin: true },
      '/tiles': { target, changeOrigin: true },
      '/media': { target, changeOrigin: true },
    },
  },
})
