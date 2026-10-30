import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  // GitHub Pages serves this repository from a subdirectory.
  // Render and local development serve it from the domain root.
  base: process.env.GITHUB_ACTIONS ? '/Social-Flow/' : '/',
});
