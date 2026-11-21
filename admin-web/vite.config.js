import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5181,
    strictPort: true,
    proxy: {
      '/api': 'http://localhost:9090',
      '/uploads': 'http://localhost:9090'
    }
  }
})
