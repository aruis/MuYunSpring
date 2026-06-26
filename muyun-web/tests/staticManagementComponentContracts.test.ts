import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const root = resolve(import.meta.dirname, '..');

test('record list explorer exposes visible secondary identity text', () => {
  const itemSource = readSource('src/vue-ui-antdv/components/UiRecordExplorerItem.vue');
  const listSource = readSource('src/platform-components/RecordListExplorer.vue');

  assert.match(itemSource, /secondary\?: string/);
  assert.match(itemSource, /class="ui-record-explorer-item-secondary"/);
  assert.match(listSource, /function recordSecondary/);
  assert.match(listSource, /:secondary="recordSecondary\(record\)"/);
});

test('static management layout forwards group title to explorer eyebrow', () => {
  const source = readSource('src/platform-components/StaticManagementLayout.vue');

  assert.match(source, /groupTitle: string/);
  assert.match(source, /:eyebrow="groupTitle"/);
});

function readSource(path: string) {
  return readFileSync(resolve(root, path), 'utf8');
}
