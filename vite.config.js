import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      // 开发时将 /api 请求代理到后端，避免 CORS 跨域问题
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // 若后端接口已有 /api 前缀则不需要 rewrite
      },
    },
  },
})

