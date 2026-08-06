import { existsSync, readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { execFileSync } from 'node:child_process';

const webRoot = dirname(dirname(fileURLToPath(import.meta.url)));
const repositoryRoot = dirname(webRoot);
const version = JSON.parse(readFileSync(join(webRoot, 'package.json'), 'utf8')).version;
const tarball = join(repositoryRoot, 'build', 'consumer-npm', `ximatai-muyun-web-app-${version}.tgz`);
const exampleRoot = join(webRoot, 'examples', 'business-web');

if (!existsSync(tarball)) {
  throw new Error(`缺少消费者 tarball：${tarball}`);
}

execFileSync('node', [join(webRoot, 'scripts', 'verify-consumer-declarations.mjs')], {
  cwd: webRoot,
  stdio: 'inherit',
});
execFileSync('npm', ['ci'], { cwd: exampleRoot, stdio: 'inherit' });
execFileSync('npm', ['install', '--no-save', '--package-lock=false', tarball], {
  cwd: exampleRoot,
  stdio: 'inherit',
});
execFileSync('npm', ['run', 'build'], { cwd: exampleRoot, stdio: 'inherit' });
