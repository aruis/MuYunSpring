import { existsSync, mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { execFileSync } from 'node:child_process';
import vue from '@vitejs/plugin-vue';
import { build } from 'vite';

const webRoot = dirname(dirname(fileURLToPath(import.meta.url)));
const repositoryRoot = dirname(webRoot);
const outputDirectory = join(repositoryRoot, 'build', 'consumer-npm');
const stagingDirectory = join(outputDirectory, 'staging', 'web-app');
const consumerEntry = join(webRoot, 'src', 'consumer', 'index.ts');
const rootPackage = JSON.parse(readFileSync(join(webRoot, 'package.json'), 'utf8'));

rmSync(outputDirectory, { recursive: true, force: true });
mkdirSync(stagingDirectory, { recursive: true });

writeFileSync(
  join(stagingDirectory, 'package.json'),
  `${JSON.stringify(
    {
      name: '@ximatai/muyun-web-app',
      version: rootPackage.version,
      description: 'MuYunSpring workbench and standard module runtime for business applications',
      type: 'module',
      files: ['dist'],
      main: './dist/index.js',
      types: './dist/types/consumer/index.d.ts',
      exports: {
        '.': { types: './dist/types/consumer/index.d.ts', import: './dist/index.js' },
        './style.css': './dist/index.css',
      },
      sideEffects: ['./dist/index.css'],
      peerDependencies: {
        vue: rootPackage.dependencies.vue,
        'ant-design-vue': rootPackage.dependencies['ant-design-vue'],
        '@ant-design/icons-vue': rootPackage.dependencies['@ant-design/icons-vue'],
      },
      license: 'Apache-2.0',
    },
    null,
    2,
  )}\n`,
);

if (!existsSync(join(webRoot, 'node_modules', 'vite'))) {
  throw new Error('请先在 muyun-web 执行 npm ci，再运行 pack:consumer。');
}

const aliases = {
  '@': join(webRoot, 'src'),
  '@muyun/vue-ui-antdv/styles.css': join(webRoot, 'src/vue-ui-antdv/styles.css'),
  '@muyun/web-contracts': join(webRoot, 'src/web-contracts/index.ts'),
  '@muyun/web-core': join(webRoot, 'src/web-core/index.ts'),
  '@muyun/vue-ui-antdv': join(webRoot, 'src/vue-ui-antdv/index.ts'),
  '@muyun/dynamic-page-runtime': join(webRoot, 'src/dynamic-page-runtime/index.ts'),
  '@muyun/platform-components': join(webRoot, 'src/platform-components/index.ts'),
  '@muyun/platform-workbench': join(webRoot, 'src/platform-workbench/index.ts'),
};

await build({
  configFile: false,
  plugins: [vue()],
  resolve: { alias: aliases },
  build: {
    emptyOutDir: true,
    outDir: join(stagingDirectory, 'dist'),
    lib: { entry: consumerEntry, formats: ['es'], fileName: 'index' },
    rollupOptions: {
      external: ['vue', 'ant-design-vue', '@ant-design/icons-vue'],
    },
  },
});

execFileSync(
  join(webRoot, 'node_modules', '.bin', 'vue-tsc'),
  [
    '--noEmit',
    'false',
    '--declaration',
    '--emitDeclarationOnly',
    '--rootDir',
    'src',
    '--outDir',
    join(stagingDirectory, 'dist', 'types'),
    '--declarationMap',
    'false',
  ],
  { cwd: webRoot, stdio: 'inherit' },
);
execFileSync('npm', ['pack', '--pack-destination', outputDirectory], {
  cwd: stagingDirectory,
  stdio: 'inherit',
});
