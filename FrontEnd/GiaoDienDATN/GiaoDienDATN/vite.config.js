import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import path from "path";

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3000,
    open: true,
    hmr: {
      overlay: true,
    },
    // Enable CORS but remove restrictive headers
    cors: true,
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
      "vue": "vue/dist/vue.esm-bundler.js",
    },
  },
  // Optimize dependency loading
  optimizeDeps: {
    include: [
      'oh-my-live2d'
    ],
  },
  // Handle static assets
  assetsInclude: [
    '**/*.moc3',
    '**/*.model3.json',
    '**/*.motion3.json',
    '**/*.physics3.json',
    '**/*.pose3.json',
    '**/*.cdi3.json'
  ],
  // Build configuration
  build: {
    rollupOptions: {
      output: {
        // Separate chunks for better loading
        manualChunks: {
          'live2d': ['oh-my-live2d'],
        },
      },
    },
  },
});
