import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    // When the Spring Boot backend exists, /api calls are proxied to it.
    proxy: { '/api': 'http://localhost:8080' },
  },
})
