const required = [22, 23, 0];
const actual = process.versions.node.split('.').map(Number);

const versionComparison =
  required.map((part, index) => actual[index] - part).find((difference) => difference !== 0) ?? 0;
const supported = versionComparison >= 0;

if (!supported) {
  console.error(
    `Node.js ${required.join('.')} or later is required for the frontend workbench tests; current version is ${process.versions.node}. Run \`nvm use\` in muyun-web and retry.`,
  );
  process.exit(1);
}
