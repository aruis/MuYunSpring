import { existsSync, readdirSync, readFileSync, statSync } from 'node:fs';
import { join, relative } from 'node:path';

const root = new URL('..', import.meta.url).pathname;
const sourceRoots = ['src', 'examples/business-web/src'];
const allowedAntdvPrefix = 'src/vue-ui-antdv/';
const violations = [];
const packageViolations = [];
const antdvTemplatePattern = /<\/?a-[a-z0-9-]+[\s>]/i;

function walk(dir) {
  return readdirSync(dir).flatMap((name) => {
    const path = join(dir, name);
    if (statSync(path).isDirectory()) {
      return walk(path);
    }
    return [path];
  });
}

for (const sourceRoot of sourceRoots) {
  const absoluteRoot = join(root, sourceRoot);
  for (const file of walk(absoluteRoot)) {
    if (!/\.(ts|vue)$/.test(file)) {
      continue;
    }

    const projectPath = relative(root, file);
    const source = readFileSync(file, 'utf8');
    const usesAntdvPackage = source.includes('ant-design-vue');
    const usesAntdvTemplate = file.endsWith('.vue') && antdvTemplatePattern.test(source);

    if ((usesAntdvPackage || usesAntdvTemplate) && !projectPath.startsWith(allowedAntdvPrefix)) {
      violations.push(projectPath);
    }
  }
}

for (const packagePath of ['examples/business-web/package.json']) {
  const absolutePath = join(root, packagePath);
  if (!existsSync(absolutePath)) {
    continue;
  }

  const packageJson = JSON.parse(readFileSync(absolutePath, 'utf8'));
  const directDependencies = {
    ...(packageJson.dependencies ?? {}),
    ...(packageJson.devDependencies ?? {}),
    ...(packageJson.peerDependencies ?? {}),
  };

  for (const dependencyName of ['ant-design-vue', '@ant-design/icons-vue']) {
    if (directDependencies[dependencyName]) {
      packageViolations.push(`${packagePath}: ${dependencyName}`);
    }
  }
}

if (violations.length > 0 || packageViolations.length > 0) {
  if (violations.length > 0) {
    console.error('Ant Design Vue imports or template tags are only allowed under src/vue-ui-antdv:');
    for (const violation of violations) {
      console.error(`- ${violation}`);
    }
  }

  if (packageViolations.length > 0) {
    console.error('Business examples must not declare direct Ant Design Vue dependencies:');
    for (const violation of packageViolations) {
      console.error(`- ${violation}`);
    }
  }

  process.exit(1);
}

console.log('Boundary check passed.');
