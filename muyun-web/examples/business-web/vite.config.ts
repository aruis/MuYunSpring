import { fileURLToPath, URL } from 'node:url';
import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vite';

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
      '@muyun/vue-ui-antdv/styles.css': fileURLToPath(
        new URL('../../src/vue-ui-antdv/styles.css', import.meta.url),
      ),
      '@muyun/web-contracts': fileURLToPath(new URL('../../src/web-contracts/index.ts', import.meta.url)),
      '@muyun/web-core': fileURLToPath(new URL('../../src/web-core/index.ts', import.meta.url)),
      '@muyun/vue-ui-antdv': fileURLToPath(new URL('../../src/vue-ui-antdv/index.ts', import.meta.url)),
      '@muyun/dynamic-page-runtime': fileURLToPath(
        new URL('../../src/dynamic-page-runtime/index.ts', import.meta.url),
      ),
      '@muyun/platform-components': fileURLToPath(
        new URL('../../src/platform-components/index.ts', import.meta.url),
      ),
      '@muyun/platform-shell': fileURLToPath(new URL('../../src/platform-shell/index.ts', import.meta.url)),
    },
  },
  build: {
    sourcemap: false,
  },
});
