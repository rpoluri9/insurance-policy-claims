import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Dev server on :5173, preview on :4173 — both are in the backend's CORS
// allow-list (see src/main/resources/application.properties).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
  },
  preview: {
    port: 4173,
  },
})
