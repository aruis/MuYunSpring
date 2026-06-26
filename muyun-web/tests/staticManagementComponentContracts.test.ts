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

test('dictionary item explorer uses tree explorer for tree-backed items', () => {
  const dictionaryViewSource = readSource('src/views/DictionaryManagementView.vue');

  assert.equal(matchCount(dictionaryViewSource, /<TreeRecordExplorer/g), 2);
  assert.doesNotMatch(dictionaryViewSource, /RecordListExplorer/);
  assert.match(dictionaryViewSource, /v-else-if="!canTreeItem"/);
  assert.match(dictionaryViewSource, /function itemTreeActionsOf/);
  assert.match(dictionaryViewSource, /startCreateChildItem\(record\)/);
});

test('dictionary item parent selector uses tree-aware record picker', () => {
  const dictionaryViewSource = readSource('src/views/DictionaryManagementView.vue');
  const pickerSource = readSource('src/platform-components/RecordPicker.vue');

  assert.match(dictionaryViewSource, /<RecordPicker[\s\S]*v-model:value="itemDraft\.parentId"/);
  assert.match(dictionaryViewSource, /:context="itemExplorerContext"/);
  assert.match(dictionaryViewSource, /:reload-key="itemReloadKey"/);
  assert.match(dictionaryViewSource, /parentRecordConstraints\(itemDraft\.id\)/);
  assert.doesNotMatch(dictionaryViewSource, /itemParentOptions/);
  assert.match(pickerSource, /reloadKey\?: number/);
  assert.match(pickerSource, /\(\) => props\.reloadKey/);
  assert.match(pickerSource, /\(\) => loadRecords\(\)/);
});

test('three-column management pages use the platform detail panel', () => {
  const indexSource = readSource('src/platform-components/index.ts');
  const panelSource = readSource('src/platform-components/RecordDetailPanel.vue');
  const layoutSource = readSource('src/platform-components/StaticManagementLayout.vue');
  const applicationViewSource = readSource('src/views/ApplicationManagementView.vue');
  const tenantViewSource = readSource('src/views/TenantManagementView.vue');
  const organizationViewSource = readSource('src/views/OrganizationManagementView.vue');
  const positionViewSource = readSource('src/views/PositionManagementView.vue');
  const dictionaryViewSource = readSource('src/views/DictionaryManagementView.vue');
  const departmentViewSource = readSource('src/views/DepartmentManagementView.vue');
  const dictionaryDetailSource = dictionaryViewSource.slice(
    dictionaryViewSource.indexOf('<RecordDetailPanel class="dictionary-column"'),
  );

  assert.match(indexSource, /RecordDetailPanel/);
  assert.match(panelSource, /defineOptions\(\{ name: 'RecordDetailPanel' \}\)/);
  assert.match(panelSource, /<slot name="status" \/>/);
  assert.match(panelSource, /<slot name="actions" \/>/);
  assert.match(layoutSource, /<RecordDetailPanel[\s\S]*:title="cardTitle"/);
  assert.match(layoutSource, /<slot name="card-status" \/>/);
  assert.doesNotMatch(layoutSource, /RecordStatusTag|card-header|title-line/);
  for (const source of [applicationViewSource, tenantViewSource, organizationViewSource]) {
    assert.match(source, /<template #card-status>/);
    assert.match(source, /<RecordStatusSwitch/);
    assert.doesNotMatch(source, /EnabledSelect|启用状态|toggle-enabled|show-status/);
  }
  assert.equal(matchCount(positionViewSource, /<RecordDetailPanel/g), 1);
  assert.equal(matchCount(dictionaryViewSource, /<RecordDetailPanel/g), 1);
  assert.equal(matchCount(departmentViewSource, /<RecordDetailPanel/g), 1);
  assert.doesNotMatch(positionViewSource, /detail-column|detail-title-group|detail-header-actions/);
  assert.doesNotMatch(dictionaryViewSource, /detail-column|detail-title-group|detail-header-actions/);
  assert.doesNotMatch(departmentViewSource, /detail-column|detail-title-group|detail-header-actions/);
  assert.doesNotMatch(dictionaryDetailSource, /EnabledSelect|启用状态/);
  assert.doesNotMatch(layoutSource, /actionMessage|message success|message\.success/);
  assert.doesNotMatch(positionViewSource, /message success|message\.success/);
  assert.doesNotMatch(dictionaryViewSource, /message success|message\.success/);
  assert.doesNotMatch(departmentViewSource, /message success|message\.success/);
});

test('department management uses organization as read-only scope and department as tree business', () => {
  const departmentViewSource = readSource('src/views/DepartmentManagementView.vue');
  const departmentStateSource = readSource('src/views/departmentManagementState.ts');

  assert.equal(matchCount(departmentViewSource, /<TreeRecordExplorer/g), 2);
  assert.match(departmentViewSource, /moduleAlias: 'iam\.organization'/);
  assert.match(departmentViewSource, /moduleAlias: 'iam\.department'/);
  assert.match(departmentViewSource, /createOrganizationScopedDepartmentContext/);
  assert.match(departmentViewSource, /path: '\/iam\.department\/tree'/);
  assert.match(departmentViewSource, /:actions-of="departmentTreeActionsOf"/);
  assert.match(departmentViewSource, /<RecordPicker[\s\S]*v-model:value="draft\.parentId"/);
  assert.match(departmentViewSource, /parentRecordConstraints\(draft\.id\)/);
  assert.doesNotMatch(departmentViewSource, /OrganizationManagementView/);
  assert.doesNotMatch(departmentViewSource, /EnabledSelect/);
  assert.doesNotMatch(departmentViewSource, /启用状态/);
  assert.match(departmentStateSource, /resetDepartmentsForOrganization/);
  assert.doesNotMatch(departmentStateSource, /已启用|已停用/);
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
  const uiFeedbackSource = readSource('src/vue-ui-antdv/feedback.ts');
  const staticCrudStateSource = readSource('src/platform-components/staticCrudManagementState.ts');
  const departmentStateSource = readSource('src/views/departmentManagementState.ts');
  const organizationStateSource = readSource('src/views/organizationManagementState.ts');
  const positionStateSource = readSource('src/views/positionManagementState.ts');
  const dictionaryStateSource = readSource('src/views/dictionaryManagementState.ts');

  assert.match(feedbackSource, /resolveGlobalErrorPresentation/);
  assert.match(feedbackSource, /toErrorUiContext/);
  assert.match(feedbackSource, /presentation\.slot === 'silent'/);
  assert.match(feedbackSource, /presentation\.slot === 'redirect-login'/);
  assert.match(feedbackSource, /tone\?: 'error' \| 'success'/);
  assert.match(feedbackSource, /showSuccessMessage\(message\)/);
  assert.match(uiFeedbackSource, /const id = `muyun-global-feedback-\$\{tone\}`/);
  assert.match(uiFeedbackSource, /className = `muyun-global-feedback \$\{tone\}`/);
  assert.match(uiFeedbackSource, /\.muyun-global-feedback\.success[\s\S]*right: 20px/);
  assert.match(uiFeedbackSource, /\.muyun-global-feedback\.error[\s\S]*left: 50%/);
  assert.match(staticCrudStateSource, /tone: 'success'/);
  assert.match(departmentStateSource, /tone: 'success'/);
  assert.match(organizationStateSource, /tone: 'success'/);
  assert.match(positionStateSource, /tone: 'success'/);
  assert.match(dictionaryStateSource, /tone: 'success'/);
});

function readSource(path: string) {
  return readFileSync(resolve(root, path), 'utf8');
}

function matchCount(source: string, pattern: RegExp) {
  return source.match(pattern)?.length ?? 0;
}
