import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      // AI 相关接口 → Python FastAPI 8000
      '/api/ai': {
        target: 'http://localhost:8000',
        changeOrigin: true,
      },
      // 上传文件静态资源 → Java 8080（Spring 静态资源映射 /uploads/** → file:./uploads/**）
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // Swagger / OpenAPI 文档 → Java 8080
      '/v3': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/swagger-ui.html': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/swagger-resources': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // 其余所有 /api → Java Spring Boot 8080
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
