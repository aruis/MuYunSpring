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

test('record explorer panel uses a single title contract', () => {
  const panelSource = readSource('src/platform-components/RecordExplorerPanel.vue');
  const layoutSource = readSource('src/platform-components/StaticManagementLayout.vue');

  assert.doesNotMatch(panelSource, /eyebrow/);
  assert.doesNotMatch(layoutSource, /groupTitle/);
});

test('record containers delegate chain errors to page feedback', () => {
  const treeSource = readSource('src/platform-components/TreeRecordExplorer.vue');
  const crudListSource = readSource('src/platform-components/CrudRecordListExplorer.vue');
  const layoutSource = readSource('src/platform-components/StaticManagementLayout.vue');

  assert.match(treeSource, /presentPlatformError/);
  assert.match(crudListSource, /presentPlatformError/);
  assert.match(treeSource, /tree\.value = \[\]/);
  assert.match(treeSource, /expandedKeys\.value = \[\]/);
  assert.match(treeSource, /emit\('loaded', \[\]\)/);
  assert.match(crudListSource, /records\.value = \[\]/);
  assert.match(crudListSource, /emit\('loaded', \[\]\)/);
  assert.doesNotMatch(treeSource, /loadError/);
  assert.doesNotMatch(crudListSource, /loadError/);
  assert.doesNotMatch(treeSource, /UiError/);
  assert.doesNotMatch(crudListSource, /UiError/);
  assert.doesNotMatch(layoutSource, /actionError/);
  assert.doesNotMatch(layoutSource, /message error/);
});

test('record explorer panel focuses and closes search from keyboard', () => {
  const panelSource = readSource('src/platform-components/RecordExplorerPanel.vue');
  const inputSource = readSource('src/vue-ui-antdv/components/UiInput.vue');

  assert.match(panelSource, /focusSearchInput/);
  assert.match(panelSource, /querySelector\('input'\)\?\.focus\(\)/);
  assert.match(panelSource, /@keydown\.esc="handleSearchEscape"/);
  assert.match(inputSource, /keydown: \[event: KeyboardEvent\]/);
});

test('tree explorer editor is explicit edit mode instead of selected record presence', () => {
  const positionViewSource = readSource('src/views/PositionManagementView.vue');
  const dictionaryViewSource = readSource('src/views/DictionaryManagementView.vue');

  assert.match(
    positionViewSource,
    /categoryEditorVisible = computed\(\(\) => categoryMode\.value !== 'view'\)/,
  );
  assert.match(
    dictionaryViewSource,
    /categoryEditorVisible = computed\(\(\) => categoryMode\.value !== 'view'\)/,
  );
  assert.doesNotMatch(positionViewSource, /categoryEditorVisible[\s\S]*Boolean\(selectedCategory/);
  assert.doesNotMatch(dictionaryViewSource, /categoryEditorVisible[\s\S]*Boolean\(selectedCategory/);
});

test('tree explorer state does not reopen category editor after delete or scope reset', () => {
  const positionStateSource = readSource('src/views/positionManagementState.ts');
  const dictionaryStateSource = readSource('src/views/dictionaryManagementState.ts');

  assert.doesNotMatch(
    positionStateSource,
    /categoryMode\.value = canCreateCategory\.value \? 'create-root' : 'view'/,
  );
  assert.doesNotMatch(
    dictionaryStateSource,
    /categoryMode\.value = canCreateCategory\.value[\s\S]*\? 'create-root' : 'view'/,
  );
  assert.doesNotMatch(
    dictionaryStateSource,
    /categoryMode\.value = selectedCategory\.value[\s\S]*\? 'view' : 'create-root'/,
  );
});

test('application scope switcher is a platform component for scoped management pages', () => {
  const indexSource = readSource('src/platform-components/index.ts');
  const switcherSource = readSource('src/platform-components/ApplicationScopeSwitcher.vue');
  const dictionaryViewSource = readSource('src/views/DictionaryManagementView.vue');

  assert.match(indexSource, /ApplicationScopeSwitcher/);
  assert.match(switcherSource, /defineOptions\(\{ name: 'ApplicationScopeSwitcher' \}\)/);
  assert.match(switcherSource, /UiDropdown/);
  assert.match(switcherSource, /:selected-key="String\(value \?\? ''\)"/);
  assert.match(switcherSource, /align="start"/);
  assert.match(dictionaryViewSource, /<ApplicationScopeSwitcher/);
  assert.doesNotMatch(dictionaryViewSource, /class="application-scope-select"/);
});

test('static crud state supports business-owned action errors before platform fallback', () => {
  const stateSource = readSource('src/platform-components/staticCrudManagementState.ts');
  const feedbackSource = readSource('src/platform-components/platformErrorFeedback.ts');

  assert.match(stateSource, /StaticCrudActionErrorHandler/);
  assert.match(stateSource, /presentPlatformError/);
  assert.match(stateSource, /presentPlatformMessage/);
  assert.match(stateSource, /matchesActionErrorHandler/);
  assert.doesNotMatch(stateSource, /presentActionError/);
  assert.match(feedbackSource, /handler\.code && error\.code === handler\.code/);
  assert.match(feedbackSource, /error\.details\?\.marker === handler\.marker/);
});

test('platform error feedback respects global error presentation slots', () => {
  const feedbackSource = readSource('src/platform-components/platformErrorFeedback.ts');

  assert.match(feedbackSource, /resolveGlobalErrorPresentation/);
  assert.match(feedbackSource, /toErrorUiContext/);
  assert.match(feedbackSource, /presentation\.slot === 'silent'/);
  assert.match(feedbackSource, /presentation\.slot === 'redirect-login'/);
});

function readSource(path: string) {
  return readFileSync(resolve(root, path), 'utf8');
}
