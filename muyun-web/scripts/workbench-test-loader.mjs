import { existsSync } from 'node:fs';
import { extname } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const root = fileURLToPath(new URL('..', import.meta.url));
const aliases = new Map([
  ['@muyun/platform-workbench', pathToFileURL(`${root}/src/platform-workbench/index.ts`).href],
  ['@muyun/web-contracts', pathToFileURL(`${root}/src/web-contracts/index.ts`).href],
  ['@muyun/web-core', pathToFileURL(`${root}/src/web-core/index.ts`).href],
]);

export async function resolve(specifier, context, nextResolve) {
  const aliasUrl = aliases.get(specifier);
  if (aliasUrl) {
    return { url: aliasUrl, shortCircuit: true };
  }

  if (specifier.startsWith('.') && !extname(specifier)) {
    const tsUrl = new URL(`${specifier}.ts`, context.parentURL);
    if (existsSync(fileURLToPath(tsUrl))) {
      return { url: tsUrl.href, shortCircuit: true };
    }
  }

  return nextResolve(specifier, context);
}

export async function load(url, context, nextLoad) {
  if (url.endsWith('.vue')) {
    return {
      format: 'module',
      shortCircuit: true,
      source: 'export default {};',
    };
  }

  return nextLoad(url, context);
}
