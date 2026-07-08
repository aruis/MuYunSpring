import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const root = resolve(import.meta.dirname, '..');

test('record list explorer exposes visible secondary identity text', () => {
  const itemSource = readSource('src/vue-ui-antdv/components/UiRecordExplorerItem.vue');
  const listSource = readSource('src/platform-components/RecordListExplorer.vue');
  const crudListSource = readSource('src/platform-components/CrudRecordListExplorer.vue');
  const treeSource = readSource('src/platform-components/TreeRecordExplorer.vue');
  const itemModelSource = readSource('src/platform-components/recordExplorerItemModel.ts');
  const treeTypesSource = readSource('src/vue-ui-antdv/types.ts');
  const uiTreeSource = readSource('src/vue-ui-antdv/components/UiTree.vue');

  assert.match(itemModelSource, /interface RecordExplorerItemDescriptor/);
  assert.match(itemModelSource, /title: string/);
  assert.match(itemModelSource, /secondary\?: string/);
  assert.match(itemModelSource, /tag\?: string/);
  assert.match(itemModelSource, /actions\?: UiRecordInlineAction\[\]/);
  assert.match(itemSource, /secondary\?: string/);
  assert.match(itemSource, /class="ui-record-explorer-item-secondary"/);
  assert.match(itemSource, /\.ui-record-explorer-item:focus-within \.ui-record-explorer-item-actions/);
  assert.match(
    listSource,
    /itemOf\?: \(record: RecordListExplorerRecord\) => RecordExplorerItemDescriptor \| undefined/,
  );
  assert.match(listSource, /function recordSecondary/);
  assert.match(listSource, /props\.codeOf \? props\.codeOf\(record\)/);
  assert.match(listSource, /:secondary="recordSecondary\(record\)"/);
  assert.match(
    crudListSource,
    /itemOf\?: \(record: CrudRecordListBase\) => RecordExplorerItemDescriptor \| undefined/,
  );
  assert.match(crudListSource, /actionsOf\?: \(record: CrudRecordListBase\) => UiRecordInlineAction\[\]/);
  assert.match(crudListSource, /action: \[action: UiRecordInlineAction, record: CrudRecordListBase\]/);
  assert.match(crudListSource, /props\.subtitleOf[\s\S]*\? props\.subtitleOf\(record\)/);
  assert.match(crudListSource, /:item-of="\(record\) => itemOf\?\.\(record as CrudRecordListBase\)"/);
  assert.match(
    crudListSource,
    /:actions-of="\(record\) => actionsOf\?\.\(record as CrudRecordListBase\) \?\? \[\]"/,
  );
  assert.match(
    crudListSource,
    /@action="\(action, record\) => handleAction\(action, record as CrudRecordListBase\)"/,
  );
  assert.match(treeTypesSource, /secondary\?: string/);
  assert.match(treeSource, /secondaryOf\?: \(record: TreeRecordBase\) => string \| undefined/);
  assert.match(
    treeSource,
    /itemOf\?: \(record: TreeRecordBase\) => RecordExplorerItemDescriptor \| undefined/,
  );
  assert.match(treeSource, /const item = props\.itemOf\?\.\(record\)/);
  assert.match(treeSource, /secondary: item\?\.secondary \?\? props\.secondaryOf\?\.\(record\)/);
  assert.match(uiTreeSource, /#title="\{ key, title, secondary, tag, muted, actions \}"/);
  assert.match(uiTreeSource, /:secondary="secondary"/);
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

test('record mode drawer owns detail mode branch switching', () => {
  const drawerSource = readSource('src/platform-components/RecordModeDrawer.vue');
  const indexSource = readSource('src/platform-components/index.ts');

  assert.match(indexSource, /export \{ default as RecordModeDrawer \}/);
  assert.match(drawerSource, /defineOptions\(\{ name: 'RecordModeDrawer' \}\)/);
  assert.match(drawerSource, /viewMode: 'view'/);
  assert.match(drawerSource, /formModes: \(\) => \['edit', 'create'\]/);
  assert.match(drawerSource, /const viewModeActive = computed\(\(\) => props\.mode === props\.viewMode\)/);
  assert.match(
    drawerSource,
    /const formModeActive = computed\(\(\) => props\.formModes\.includes\(props\.mode\)\)/,
  );
  assert.match(drawerSource, /props\.closeOnOutside \?\? viewModeActive\.value/);
  assert.match(drawerSource, /<template v-if="loading">/);
  assert.match(drawerSource, /<template v-else-if="loadFailed">/);
  assert.match(drawerSource, /<template v-else-if="viewModeActive">/);
  assert.match(drawerSource, /<template v-else-if="formModeActive">/);
  assert.doesNotMatch(drawerSource, /<template v-else>\s*<slot name="form"/);
  assert.match(drawerSource, /<slot name="view" \/>/);
  assert.match(drawerSource, /<slot name="form" \/>/);
});

test('record explorer panel focuses and closes search from keyboard', () => {
  const panelSource = readSource('src/platform-components/RecordExplorerPanel.vue');
  const inputSource = readSource('src/vue-ui-antdv/components/UiInput.vue');

  assert.match(panelSource, /focusSearchInput/);
  assert.match(panelSource, /querySelector\('input'\)\?\.focus\(\)/);
  assert.match(panelSource, /@mousedown\.prevent/);
  assert.match(panelSource, /@keydown\.esc="handleSearchEscape"/);
  assert.match(inputSource, /keydown: \[event: KeyboardEvent\]/);
});

test('menu management keeps scheme actions inline and delegates search to panel', () => {
  const menuViewSource = readSource('src/views/MenuManagementView.vue');
  const schemePanelStart = menuViewSource.indexOf('title="菜单方案"');
  const menuTreePanelStart = menuViewSource.indexOf('title="菜单树"');
  const schemePanelSource = menuViewSource.slice(schemePanelStart, menuTreePanelStart);
  const menuTreePanelSource = menuViewSource.slice(menuTreePanelStart);

  assert.match(menuViewSource, /function schemeActionsOf/);
  assert.match(menuViewSource, /function schemeItemOf/);
  assert.match(menuViewSource, /function handleSchemeInlineAction/);
  assert.match(schemePanelSource, /:item-of="schemeItemOf"/);
  assert.match(schemePanelSource, /@action="handleSchemeInlineAction"/);
  assert.match(schemePanelSource, /:filter-option="schemeFilterOption"/);
  assert.doesNotMatch(schemePanelSource, /title="编辑菜单方案"/);
  assert.doesNotMatch(schemePanelSource, /title="删除菜单方案"/);
  assert.match(menuTreePanelSource, /search-mode="none"/);
  assert.match(menuTreePanelSource, /search-trigger="external"/);
});

test('static management explorers use unified item descriptors', () => {
  const explorerViews = [
    'ApplicationManagementView.vue',
    'TenantManagementView.vue',
    'OrganizationManagementView.vue',
    'DepartmentManagementView.vue',
    'PositionManagementView.vue',
    'DictionaryManagementView.vue',
    'MenuManagementView.vue',
    'EmployeeManagementView.vue',
    'UserManagementView.vue',
    'RoleManagementView.vue',
  ];

  for (const fileName of explorerViews) {
    const source = readSource(`src/views/${fileName}`);
    assert.match(source, /RecordExplorerItemDescriptor/, fileName);
    assert.match(source, /:item-of=/, fileName);
  }

  const dictionarySource = readSource('src/views/DictionaryManagementView.vue');
  const menuSource = readSource('src/views/MenuManagementView.vue');
  const positionSource = readSource('src/views/PositionManagementView.vue');
  const departmentSource = readSource('src/views/DepartmentManagementView.vue');

  assert.doesNotMatch(dictionarySource, /:tag-of=|:actions-of=|:muted-of=/);
  assert.doesNotMatch(menuSource, /:tag-of=|:actions-of=/);
  assert.doesNotMatch(positionSource, /:actions-of=/);
  assert.doesNotMatch(departmentSource, /:actions-of=/);
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

test('menu entry low-code fields are only exposed for dynamic module entries', () => {
  const menuViewSource = readSource('src/views/MenuManagementView.vue');

  assert.match(menuViewSource, /<label v-if="isDynamicModuleEntry"[\s\S]*页面模式/);
  assert.match(menuViewSource, /<label v-if="isDynamicModuleEntry"[\s\S]*默认 UI 配置/);
  assert.match(menuViewSource, /<label v-if="isDynamicModuleEntry"[\s\S]*默认查询模板/);
  assert.match(menuViewSource, /<label v-if="isDynamicModuleEntry" class="full-row"[\s\S]*入口参数 JSON/);
  assert.doesNotMatch(menuViewSource, /<label v-if="hasModuleEntry" class="full-row"[\s\S]*入口参数 JSON/);
});

test('application scope switcher is a platform component for scoped management pages', () => {
  const indexSource = readSource('src/platform-components/index.ts');
  const switcherSource = readSource('src/platform-components/ApplicationScopeSwitcher.vue');
  const dictionaryViewSource = readSource('src/views/DictionaryManagementView.vue');

  assert.match(indexSource, /ApplicationScopeSwitcher/);
  assert.match(indexSource, /createStaticTreeResourceModuleContext/);
  assert.match(switcherSource, /defineOptions\(\{ name: 'ApplicationScopeSwitcher' \}\)/);
  assert.match(switcherSource, /UiDropdown/);
  assert.match(switcherSource, /:selected-key="String\(value \?\? ''\)"/);
  assert.match(switcherSource, /align="start"/);
  assert.match(dictionaryViewSource, /<ApplicationScopeSwitcher/);
  assert.match(dictionaryViewSource, /createStaticTreeResourceModuleContext/);
  assert.doesNotMatch(dictionaryViewSource, /function fallbackCategoryClient/);
  assert.doesNotMatch(dictionaryViewSource, /function fallbackItemClient/);
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

  assert.match(dictionaryViewSource, /:context="itemExplorerContext"/);
  assert.match(dictionaryViewSource, /:reload-key="itemReloadKey"/);
  assert.match(dictionaryViewSource, /parentRecordConstraints\(itemDraft\.value\.id\)/);
  assert.match(dictionaryViewSource, /itemFormPickerConfigs/);
  assert.match(dictionaryViewSource, /:picker-configs="itemFormPickerConfigs"/);
  assert.doesNotMatch(dictionaryViewSource, /itemParentOptions/);
  assert.doesNotMatch(dictionaryViewSource, /<RecordPicker[\s\S]*v-model:value="itemDraft\.parentId"/);
  assert.match(pickerSource, /reloadKey\?: number/);
  assert.match(pickerSource, /\(\) => props\.reloadKey/);
  assert.match(pickerSource, /\(\) => loadRecords\(\)/);
});

test('dictionary management uses record form fields for category and item forms', () => {
  const dictionaryViewSource = readSource('src/views/DictionaryManagementView.vue');

  assert.equal(matchCount(dictionaryViewSource, /<RecordFormFields/g), 2);
  assert.match(dictionaryViewSource, /onMounted\(loadDictionaryFormDefinitions\)/);
  assert.match(dictionaryViewSource, /resolveRecordFormFields\(runtimeContext\.uiDescriptor\)/);
  assert.match(
    dictionaryViewSource,
    /resolveRecordFormFields\(\s*runtimeContext\.uiDescriptor,\s*childResourceDefaultFormViewCode\(ITEM_RESOURCE\),?\s*\)/,
  );
  assert.match(dictionaryViewSource, /const ITEM_RESOURCE = 'item'/);
  assert.match(dictionaryViewSource, /childResourceDefaultFormViewCode/);
  assert.match(dictionaryViewSource, /categoryFormFieldDefinitions/);
  assert.match(dictionaryViewSource, /itemFormFieldDefinitions/);
  assert.match(dictionaryViewSource, /:field-names="itemFormFieldNames"/);
  assert.match(dictionaryViewSource, /:fields="itemFormFieldDefinitions"/);
  assert.match(dictionaryViewSource, /categoryKind: \{[\s\S]*controlType: 'select'/);
  assert.match(dictionaryViewSource, /enabled: \{[\s\S]*controlType: 'enabledStatus'/);
  assert.match(dictionaryViewSource, /itemFormFieldFallback/);
  assert.match(dictionaryViewSource, /parentId: \{[\s\S]*controlType: 'recordPicker'/);
  assert.match(dictionaryViewSource, /:disabled-of="itemFormFieldDisabled"/);
  assert.doesNotMatch(dictionaryViewSource, /<UiInput[\s\S]*v-model:value="categoryDraft\.alias"/);
  assert.doesNotMatch(dictionaryViewSource, /<UiSelect[\s\S]*v-model:value="categoryDraft\.categoryKind"/);
  assert.doesNotMatch(dictionaryViewSource, /<UiInput[\s\S]*v-model:value="itemDraft\.code"/);
});

test('position management uses child resource form descriptor for position form', () => {
  const positionViewSource = readSource('src/views/PositionManagementView.vue');

  assert.match(positionViewSource, /onMounted\(loadPositionFormDefinition\)/);
  assert.match(
    positionViewSource,
    /resolveRecordFormFields\(\s*runtimeContext\.uiDescriptor,\s*childResourceDefaultFormViewCode\(POSITION_RESOURCE\),?\s*\)/,
  );
  assert.match(positionViewSource, /const POSITION_RESOURCE = 'position'/);
  assert.match(positionViewSource, /childResourceDefaultFormViewCode/);
  assert.match(
    positionViewSource,
    /positionFormFieldDefinitions = ref\(resolveRecordFormFields\(undefined\)\)/,
  );
  assert.match(positionViewSource, /<RecordFormFields/);
  assert.match(positionViewSource, /:field-names="positionFormFieldNames"/);
  assert.match(positionViewSource, /:fields="positionFormFieldDefinitions"/);
  assert.match(positionViewSource, /:fallback="positionFormFieldFallback"/);
  assert.match(positionViewSource, /@update:field="updatePositionDraftField"/);
  assert.match(positionViewSource, /categoryId: \{[\s\S]*controlType: 'select'/);
  assert.match(positionViewSource, /options: categoryOptions\.value/);
  assert.doesNotMatch(positionViewSource, /<UiSelect[\s\S]*v-model:value="positionDraft\.categoryId"/);
  assert.doesNotMatch(positionViewSource, /<UiInput[\s\S]*v-model:value="positionDraft\.code"/);
  assert.doesNotMatch(positionViewSource, /<UiInput[\s\S]*v-model:value="positionDraft\.title"/);
  assert.doesNotMatch(positionViewSource, /<UiInput[\s\S]*v-model:value="positionDraft\.description"/);
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
  const menuViewSource = readSource('src/views/MenuManagementView.vue');
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
  assert.equal(matchCount(menuViewSource, /<RecordDetailPanel/g), 1);
  assert.match(positionViewSource, /v-if="positionMode !== 'view'"[\s\S]*:enabled="positionDraft\.enabled"/);
  assert.match(dictionaryViewSource, /v-if="itemMode !== 'view'"[\s\S]*:enabled="itemDraft\.enabled"/);
  assert.match(departmentViewSource, /<RecordFormFields[\s\S]*:record="draft as RecordFormRecord"/);
  assert.doesNotMatch(departmentViewSource, /:enabled="draft\.enabled"/);
  assert.doesNotMatch(positionViewSource, /v-if="positionMode === 'create'"/);
  assert.doesNotMatch(dictionaryViewSource, /v-if="itemMode === 'create'"/);
  assert.doesNotMatch(departmentViewSource, /v-if="mode\.startsWith\('create'\)"/);
  assert.match(menuViewSource, /<template #editor>[\s\S]*scheme-editor-panel/);
  assert.match(menuViewSource, /<RecordDetailPanel class="menu-detail-column"[\s\S]*:title="menuCardTitle"/);
  assert.doesNotMatch(menuViewSource, /<RecordDetailPanel[\s\S]*:title="schemeCardTitle"/);
  assert.match(
    positionViewSource,
    /:enabled="categoryDraft\.enabled"[\s\S]*@change="categoryDraft\.enabled = \$event"/,
  );
  assert.match(
    dictionaryViewSource,
    /:enabled="categoryDraft\.enabled"[\s\S]*@change="categoryDraft\.enabled = \$event"/,
  );
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
  assert.match(departmentViewSource, /createScopedTreeModuleContext/);
  assert.match(departmentViewSource, /treePath: '\/iam\.department\/tree'/);
  assert.match(departmentViewSource, /sortPath: '\/iam\.department\/sort'/);
  assert.match(departmentViewSource, /function departmentItemOf/);
  assert.match(departmentViewSource, /actions: departmentTreeActionsOf\(department\)/);
  assert.match(departmentViewSource, /:item-of="departmentItemOf"/);
  assert.match(departmentViewSource, /onMounted\(loadDepartmentFormDefinition\)/);
  assert.match(departmentViewSource, /resolveRecordFormFields\(runtimeContext\.uiDescriptor\)/);
  assert.match(departmentViewSource, /<RecordFormFields/);
  assert.match(departmentViewSource, /resolveRecordFormFieldState/);
  assert.match(departmentViewSource, /departmentFormPickerConfigs/);
  assert.match(departmentViewSource, /constraints: parentRecordConstraints\(draft\.value\.id\)/);
  assert.match(departmentViewSource, /:exclude-field-names="\['organizationId'\]"/);
  assert.match(departmentViewSource, /:picker-configs="departmentFormPickerConfigs"/);
  assert.match(departmentViewSource, /@update:field="updateDepartmentDraftField"/);
  assert.doesNotMatch(departmentViewSource, /departmentStandardFormFields/);
  assert.doesNotMatch(departmentViewSource, /Array\.from\(departmentFormFieldDefinitions\.value\.keys\(\)\)/);
  assert.doesNotMatch(departmentViewSource, /<RecordPicker\s[\s\S]*v-model:value="draft\.parentId"/);
  assert.doesNotMatch(departmentViewSource, /OrganizationManagementView/);
  assert.doesNotMatch(departmentViewSource, /EnabledSelect/);
  assert.doesNotMatch(departmentViewSource, /<RecordStatusSwitch\s[\s\S]{0,240}:enabled="draft\.enabled"/);
  assert.match(departmentStateSource, /resetDepartmentsForOrganization/);
  assert.match(departmentStateSource, /executeStaticFormSave<Department>/);
  assert.match(departmentStateSource, /executeStaticRecordAction/);
  assert.doesNotMatch(departmentStateSource, /已启用|已停用/);
});

test('employee management uses organization scope and platform query list panel', () => {
  const indexSource = readSource('src/platform-components/index.ts');
  const drawerSource = readSource('src/platform-components/RecordDetailDrawer.vue');
  const panelSource = readSource('src/platform-components/RecordQueryListPanel.vue');
  const formFieldsSource = readSource('src/platform-components/RecordFormFields.vue');
  const formFieldModelSource = readSource('src/platform-components/recordFormFieldModel.ts');
  const runtimeContextSource = readSource('src/web-core/module/runtimeContext.ts');
  const dropdownSource = readSource('src/vue-ui-antdv/components/UiDropdown.vue');
  const employeeViewSource = readSource('src/views/EmployeeManagementView.vue');
  const contractsSource = readSource('src/web-contracts/index.ts');

  assert.match(indexSource, /RecordQueryListPanel/);
  assert.match(indexSource, /RecordFormFields/);
  assert.match(indexSource, /resolveRecordFormFieldNames/);
  assert.match(indexSource, /resolveRecordFormFieldState/);
  assert.match(formFieldsSource, /RecordStatusSwitch/);
  assert.match(formFieldsSource, /RecordPicker/);
  assert.match(formFieldsSource, /pickerConfigs\?: Record<string, RecordFormFieldPickerConfig>/);
  assert.match(formFieldsSource, /fieldNames\?: string\[\]/);
  assert.match(formFieldsSource, /excludeFieldNames\?: string\[\]/);
  assert.match(formFieldsSource, /resolveRecordFormFieldNames/);
  assert.match(formFieldsSource, /resolveRecordFormFieldState/);
  assert.match(formFieldModelSource, /field\?\.uiType === 'enabledStatus'/);
  assert.match(formFieldModelSource, /field\?\.uiType === 'recordPicker'/);
  assert.match(formFieldModelSource, /fallback\?\.controlType \?\? 'input'/);
  assert.match(formFieldsSource, /booleanFieldValue/);
  assert.match(formFieldsSource, /field\.controlType === 'recordPicker' && field\.pickerConfig/);
  assert.match(formFieldsSource, /field\.controlType === 'select' && field\.options/);
  assert.match(
    formFieldsSource,
    /disabledOf\?: \(fieldName: string, field: RecordFormFieldState\) => boolean/,
  );
  assert.match(
    formFieldsSource,
    /'update:field': \[fieldName: string, value: string \| number \| boolean \| undefined\]/,
  );
  assert.match(panelSource, /defineOptions\(\{ name: 'RecordQueryListPanel' \}\)/);
  assert.match(panelSource, /querySchema\(\{\s*uiConfigId: props\.uiConfigId,\s*\}\)/);
  assert.match(panelSource, /emptyQuerySchema/);
  assert.match(panelSource, /isUnsupportedQuerySchemaError/);
  assert.match(panelSource, /query schema is not supported by/);
  assert.match(panelSource, /externalQueryValues/);
  assert.match(panelSource, /actions\?: RecordActionItem\[\]/);
  assert.match(panelSource, /standardCrudActions\?: boolean/);
  assert.match(panelSource, /standardCrudRowActions\?: boolean/);
  assert.match(panelSource, /function standardCrudRowActionsOf/);
  assert.match(panelSource, /rowActionsOf\?: \(record: QueryListRecord\) => RecordActionItem\[\]/);
  assert.match(panelSource, /type\?: 'text' \| 'enabledStatus'/);
  assert.match(panelSource, /interface QueryListRow/);
  assert.match(panelSource, /const rows = computed<QueryListRow/);
  assert.match(panelSource, /function resolveRow/);
  assert.match(panelSource, /<RecordActionBar/);
  assert.match(panelSource, /<RecordStatusTag/);
  assert.match(panelSource, /<UiDropdown/);
  assert.match(panelSource, /emit\('action', action, event\)/);
  assert.match(panelSource, /emit\('rowDblclick', row\.record, \$event\)/);
  assert.match(panelSource, /emit\('rowAction', action, row\.record/);
  assert.doesNotMatch(panelSource, /primaryRowAction\(record\)/);
  assert.match(panelSource, /allow-clear/);
  assert.match(panelSource, /conditionsDisabled/);
  assert.doesNotMatch(panelSource, />清除</);
  assert.match(dropdownSource, /scheduleCloseDropdown/);
  assert.match(dropdownSource, /clearCloseTimer/);
  assert.match(dropdownSource, /setTimeout/);
  assert.match(panelSource, /ready\?: boolean/);
  assert.match(panelSource, /waitingDescription\?: string/);
  assert.match(panelSource, /\(\) => props\.ready/);
  assert.match(panelSource, /runtimeViews = ref<ResolvedViewDescriptor\[\]>\(\[\]\)/);
  assert.match(panelSource, /runtimeViews\.value = await loadRuntimeViews\(\)/);
  assert.match(panelSource, /async function loadRuntimeViews/);
  assert.match(panelSource, /if \(props\.columns && props\.columns\.length > 0\)/);
  assert.match(panelSource, /descriptorLoadError = ref\(false\)/);
  assert.match(panelSource, /descriptorLoadError\.value = true/);
  assert.match(panelSource, /列表声明加载失败，请稍后重试/);
  assert.doesNotMatch(panelSource, /catch \{\s*return \[\];\s*\}/);
  assert.match(
    panelSource,
    /presentPlatformError\(cause, \{ source: 'record-query-list-panel', phase: 'load' \}\)/,
  );
  assert.match(panelSource, /tableColumns = computed<RecordQueryListColumn\[\]>/);
  assert.match(panelSource, /columnsFromRuntimeListView/);
  assert.match(panelSource, /field\.fieldRef\.fieldName/);
  assert.match(panelSource, /field\.uiType === 'enabledStatus'/);
  assert.match(panelSource, /record\[`\$\{fieldName\}Title`\]/);
  assert.match(panelSource, /return value \? '是' : '否'/);
  assert.match(panelSource, /emit\('loaded', \[\]\)/);
  assert.match(panelSource, /recordsRequestSeq/);
  assert.match(panelSource, /if \(!queryReady\.value\)/);
  assert.match(panelSource, /activeConditions\.value = \[\]/);
  assert.match(panelSource, /validateConditionDrafts/);
  assert.match(
    panelSource,
    /operator === 'BETWEEN'[\s\S]*valuesOfDraft\(field, operator, draft\)\.length !== 2/,
  );
  assert.match(panelSource, /quickSearchFields/);
  assert.match(panelSource, /conditions: activeConditions\.value/);
  assert.match(panelSource, /page: \{ pageNum: pageNum\.value, pageSize: pageSize\.value \}/);
  assert.match(employeeViewSource, /moduleAlias: 'iam\.organization'/);
  assert.match(employeeViewSource, /moduleAlias: 'iam\.employee'/);
  assert.match(employeeViewSource, /<TreeRecordExplorer/);
  assert.match(employeeViewSource, /<RecordQueryListPanel/);
  assert.match(employeeViewSource, /standard-crud-actions/);
  assert.match(employeeViewSource, /create-title="新建职员"/);
  assert.match(employeeViewSource, /@action="handleEmployeeListAction"/);
  assert.match(indexSource, /RecordDetailDrawer/);
  assert.match(indexSource, /RecordDetailFields/);
  assert.match(drawerSource, /closeOnOutside\?: boolean/);
  assert.match(drawerSource, /handleDocumentPointerDown/);
  assert.match(employeeViewSource, /<RecordDetailDrawer/);
  assert.match(employeeViewSource, /:close-on-outside="employeeDetailMode === 'view'"/);
  assert.match(employeeViewSource, /standard-crud-row-actions/);
  assert.doesNotMatch(employeeViewSource, /employeeRowActionsOf/);
  assert.match(employeeViewSource, /@row-action="handleEmployeeRowAction"/);
  assert.match(employeeViewSource, /@row-dblclick="handleEmployeeRowDblclick"/);
  assert.doesNotMatch(employeeViewSource, /employeeColumns/);
  assert.doesNotMatch(employeeViewSource, /:columns="employeeColumns"/);
  assert.doesNotMatch(employeeViewSource, /type: 'enabledStatus'/);
  assert.match(employeeViewSource, /onMounted\(loadEmployeeFormDefinition\)/);
  assert.match(employeeViewSource, /resolveRecordFormFields\(runtimeContext\.uiDescriptor\)/);
  assert.match(
    employeeViewSource,
    /employeeFormFieldDefinitions = ref\(resolveRecordFormFields\(undefined\)\)/,
  );
  assert.match(employeeViewSource, /<RecordFormFields/);
  assert.match(employeeViewSource, /<RecordDetailFields/);
  assert.match(employeeViewSource, /v-if="employeeDetailMode === 'view'"/);
  assert.match(employeeViewSource, /<form v-else class="employee-form"/);
  assert.match(employeeViewSource, /:display-of="employeeDetailDisplayValue"/);
  assert.match(employeeViewSource, /function employeeDetailDisplayValue/);
  assert.match(employeeViewSource, /resolveRecordFormFieldState/);
  assert.match(employeeViewSource, /:exclude-field-names="\['organizationId'\]"/);
  assert.doesNotMatch(employeeViewSource, /const employeeStandardFormFields = computed/);
  assert.doesNotMatch(employeeViewSource, /Array\.from\(employeeFormFieldDefinitions\.value\.keys\(\)\)/);
  assert.match(employeeViewSource, /placeholder: '请输入性别'/);
  assert.match(employeeViewSource, /gender: draft\.gender\?\.trim\(\) \|\| undefined/);
  assert.match(employeeViewSource, /gender: \{ label: '性别'/);
  assert.match(employeeViewSource, /departmentId: \{[\s\S]*controlType: 'recordPicker'/);
  assert.match(employeeViewSource, /enabled: \{[\s\S]*controlType: 'enabledStatus'/);
  assert.match(employeeViewSource, /employeeFormPickerConfigs/);
  assert.match(
    employeeViewSource,
    /context: scopedDepartmentContext\.value as unknown as ModuleContext<RecordPickerRecord>/,
  );
  assert.match(employeeViewSource, /:picker-configs="employeeFormPickerConfigs"/);
  assert.match(employeeViewSource, /@update:field="updateEmployeeDraftField"/);
  assert.match(employeeViewSource, /employeeDetailMode === 'view' && selectedEmployee/);
  assert.doesNotMatch(employeeViewSource, /employeeDetailMode !== 'view'[\s\S]*employeeDraft\.enabled/);
  assert.doesNotMatch(employeeViewSource, /<RecordPicker[\s\S]*v-model:value="employeeDraft\.departmentId"/);
  assert.doesNotMatch(employeeViewSource, /employeeFormLabel\('employeeNo'\)/);
  assert.doesNotMatch(employeeViewSource, /employeeFormRequired\('employeeNo'\)/);
  assert.match(employeeViewSource, /label: '所属部门'/);
  assert.doesNotMatch(employeeViewSource, /function employeeFormFieldDisabled/);
  assert.doesNotMatch(employeeViewSource, /<span>所属部门<\/span>/);
  assert.doesNotMatch(employeeViewSource, /<span>职员编号<\/span>/);
  assert.match(employeeViewSource, /const employeeFormDisabled = computed/);
  assert.match(employeeViewSource, /const loadingEmployeeDetail = ref\(false\)/);
  assert.match(employeeViewSource, /const employeeDetailLoadFailed = ref\(false\)/);
  assert.match(employeeViewSource, /const employeeDetailRequestSeq = ref\(0\)/);
  assert.match(employeeViewSource, /isEmployeeFormDisabled/);
  assert.match(employeeViewSource, /shouldShowEmployeeDetailContent/);
  assert.match(employeeViewSource, /shouldCloseEmployeeDetailOnCancel/);
  assert.match(employeeViewSource, /canSwitchEmployeeDetailContext/);
  assert.match(employeeViewSource, /function canLeaveEmployeeDetailContext\(\)/);
  assert.match(employeeViewSource, /canSwitchEmployeeDetailContext\(\{ saving: savingEmployee\.value \}\)/);
  assert.match(employeeViewSource, /if \(!canLeaveEmployeeDetailContext\(\)\) \{\s*return;\s*\}/);
  assert.match(employeeViewSource, /key: 'edit'[\s\S]*disabled: savingEmployee\.value/);
  assert.match(employeeViewSource, /key: 'delete'[\s\S]*disabled: savingEmployee\.value/);
  assert.match(employeeViewSource, /selectedEmployeeId: selectedEmployee\.value\?\.id/);
  assert.match(
    employeeViewSource,
    /const currentDetailId = String\(selectedEmployee\.value\?\.id \?\? employeeDraft\.value\.id \?\? ''\)/,
  );
  assert.match(employeeViewSource, /employeeDetailOpen\.value && currentDetailId !== nextKey/);
  assert.match(employeeViewSource, /employeeDetailOpen\.value = false/);
  assert.match(employeeViewSource, /const canSaveEmployee = computed/);
  assert.match(employeeViewSource, /if \(loadingEmployeeDetail\.value\) \{\s*return false;\s*\}/);
  assert.match(employeeViewSource, /const canToggleEmployee = computed/);
  assert.match(employeeViewSource, /loadingEmployeeDetail\.value \|\| !selectedEmployee\.value\?\.id/);
  assert.match(employeeViewSource, /function employeeToggleActionCode/);
  assert.match(
    employeeViewSource,
    /selectedEmployee\.value = undefined;\s*employeeDraft\.value = copyEmployee/,
  );
  assert.doesNotMatch(employeeViewSource, /selectedEmployee\.value = record as Employee/);
  assert.match(employeeViewSource, /const requestSeq = employeeDetailRequestSeq\.value \+ 1/);
  assert.match(employeeViewSource, /employeeDetailRequestSeq\.value = requestSeq/);
  assert.match(employeeViewSource, /shouldCommitEmployeeDetailRequest/);
  assert.match(employeeViewSource, /const canCommitRequest = \(\) =>/);
  assert.match(employeeViewSource, /if \(!canCommitRequest\(\)\)/);
  assert.match(employeeViewSource, /if \(canCommitRequest\(\)\) \{\s*employeeDetailLoadFailed\.value = true/);
  assert.match(employeeViewSource, /loadingEmployeeDetail\.value = false/);
  assert.match(employeeViewSource, /async function loadEmployeeDetailDepartment\([\s\S]*requestSeq/);
  assert.match(employeeViewSource, /function canCommitEmployeeDetailSideEffect/);
  assert.match(employeeViewSource, /activeRequestSeq: employeeDetailRequestSeq\.value/);
  assert.match(employeeViewSource, /selectedEmployeeKey: selectedEmployeeKey\.value/);
  assert.match(employeeViewSource, /if \(canCommitEmployeeDetailSideEffect\(employeeId, requestSeq\)\)/);
  assert.match(employeeViewSource, /function cancelEmployeeDetail\(\)/);
  assert.match(
    employeeViewSource,
    /function cancelEmployeeDetail[\s\S]*shouldCloseEmployeeDetailOnCancel[\s\S]*closeEmployeeDetail\(\)/,
  );
  assert.match(
    employeeViewSource,
    /function cancelEmployeeDetail[\s\S]*employeeDraft\.value = copyEmployee\(selectedEmployee\.value!\)[\s\S]*employeeDetailMode\.value = 'view'/,
  );
  assert.match(
    employeeViewSource,
    /if \(action\.key === 'cancel'\) \{\s*cancelEmployeeDetail\(\);\s*return;\s*\}/,
  );
  assert.match(
    employeeViewSource,
    /function handleEmployeeDetailAction[\s\S]*if \(!canLeaveEmployeeDetailContext\(\)\) \{\s*return;\s*\}[\s\S]*if \(action\.key === 'edit'\)/,
  );
  assert.match(employeeViewSource, /if \(!selectedEmployee\.value \|\| loadingEmployeeDetail\.value\)/);
  assert.match(employeeViewSource, /function retryEmployeeDetail/);
  assert.match(employeeViewSource, /<UiSpin v-if="loadingEmployeeDetail"/);
  assert.match(employeeViewSource, /v-else-if="employeeDetailLoadFailed"/);
  assert.match(employeeViewSource, /<UiError title="详情加载失败"/);
  assert.match(employeeViewSource, /@click="retryEmployeeDetail"/);
  assert.match(employeeViewSource, /v-else-if="showEmployeeDetailContent"/);
  assert.match(employeeViewSource, /executeStaticFormSave<Employee>/);
  assert.match(employeeViewSource, /executeStaticRecordAction/);
  assert.match(
    employeeViewSource,
    /validateContext: \(\) => \(selectedOrganizationId\.value \? undefined : '请先选择机构'\)/,
  );
  assert.match(employeeViewSource, /canSave: \(\) => canSaveEmployee\.value/);
  assert.match(employeeViewSource, /validateRecord: validateEmployeeDraft/);
  assert.match(employeeViewSource, /function validateEmployeeDraft\(draft: Employee\)/);
  assert.match(employeeViewSource, /validateEmployeeRequiredFormFields/);
  assert.match(employeeViewSource, /const employeeRequiredFormFieldNames = \[/);
  assert.doesNotMatch(employeeViewSource, /draft\.departmentId && draft\.employeeNo && draft\.title/);
  assert.match(
    employeeViewSource,
    /onSaved: \(\{ record \}\) => \{\s*const requestSeq = commitEmployeeDetailRecord\(record\)[\s\S]*void loadEmployeeDetailDepartment\(record, requestSeq\)/,
  );
  assert.match(employeeViewSource, /当前用户无权保存职员/);
  assert.match(employeeViewSource, /当前用户无权变更职员启停状态/);
  assert.match(employeeViewSource, /canExecute: \(\) => canToggleEmployee\.value/);
  assert.match(employeeViewSource, /employeeContext\.crud\.enable\(employee\.id!\)/);
  assert.match(employeeViewSource, /employeeContext\.crud\.disable\(employee\.id!\)/);
  assert.match(
    employeeViewSource,
    /const refreshed = await employeeContext\.crud\.view\(employee\.id!\);\s*const requestSeq = commitEmployeeDetailRecord\(refreshed\);\s*await loadEmployeeDetailDepartment\(refreshed, requestSeq\)/,
  );
  assert.match(employeeViewSource, /confirm: \(target\) =>[\s\S]*title: '删除职员'/);
  assert.match(employeeViewSource, /content: `确认删除职员/);
  assert.match(employeeViewSource, /employeeContext\.crud\.delete\(String\(target\.id\)\)/);
  assert.match(employeeViewSource, /:disabled="savingEmployee \|\| !canToggleEmployee"/);
  assert.doesNotMatch(employeeViewSource, /:disabled="employeeFormFieldDisabled\('employeeNo'\)"/);
  assert.doesNotMatch(
    employeeViewSource,
    /render: \(record\) => \(record\.enabled === false \? '停用' : '启用'\)/,
  );
  assert.match(employeeViewSource, /employeeContext\.crud\.insert/);
  assert.match(employeeViewSource, /employeeContext\.crud\.update/);
  assert.match(employeeViewSource, /employeeContext\.crud\.delete/);
  assert.doesNotMatch(employeeViewSource, /presentPlatformMessage\(result\.message \?\? '操作成功'/);
  assert.match(employeeViewSource, /employeeReloadKey\.value \+= 1/);
  assert.match(indexSource, /createScopedTreeModuleContext/);
  assert.match(employeeViewSource, /createScopedTreeModuleContext/);
  assert.match(employeeViewSource, /treePath: '\/iam\.department\/tree'/);
  assert.match(employeeViewSource, /sortPath: '\/iam\.department\/sort'/);
  assert.doesNotMatch(employeeViewSource, /createOrganizationScopedDepartmentContext/);
  assert.match(employeeViewSource, /organizationReloadKey/);
  assert.match(employeeViewSource, /@refresh="refreshOrganizations"/);
  assert.match(employeeViewSource, /:reload-key="organizationReloadKey"/);
  assert.match(employeeViewSource, /:ready="Boolean\(selectedOrganization\?\.id\)"/);
  assert.match(employeeViewSource, /departmentScope/);
  assert.match(contractsSource, /export interface QuerySchema/);
  assert.match(contractsSource, /export interface ModuleUiDefinition/);
  assert.match(contractsSource, /export interface ViewDefinition/);
  assert.match(contractsSource, /export interface ViewFieldDefinition/);
  assert.match(contractsSource, /gender\?: string/);
  assert.match(contractsSource, /schemaVersion: string/);
  assert.doesNotMatch(runtimeContextSource, /uiDefinition\?:/);
  assert.doesNotMatch(panelSource, /uiDefinition/);
  assert.doesNotMatch(employeeViewSource, /uiDefinition/);
  assert.match(contractsSource, /externalQueryValues\?: Record<string, unknown>/);
});

test('role management keeps basic scope management separate from binding and authorization', () => {
  const roleViewSource = readSource('src/views/RoleManagementView.vue');
  const routesSource = readSource('src/app/businessRoutes.ts');
  const contractsSource = readSource('src/web-contracts/index.ts');
  const panelSource = readSource('src/platform-components/RecordQueryListPanel.vue');

  assert.match(routesSource, /moduleAlias: 'iam\.role'/);
  assert.match(routesSource, /route: '\/iam\/roles'/);
  assert.match(roleViewSource, /defineOptions\(\{ name: 'RoleManagementView' \}\)/);
  assert.match(roleViewSource, /moduleAlias: 'iam\.tenant'/);
  assert.match(roleViewSource, /moduleAlias: 'iam\.organization'/);
  assert.match(roleViewSource, /moduleAlias: 'iam\.role'/);
  assert.match(roleViewSource, /role-management-page/);
  assert.match(roleViewSource, /<CrudRecordListExplorer/);
  assert.match(roleViewSource, /<TreeRecordExplorer/);
  assert.match(roleViewSource, /<RecordQueryListPanel/);
  assert.match(roleViewSource, /selectedScope/);
  assert.match(roleViewSource, /canBrowseTenants/);
  assert.match(roleViewSource, /currentUserTenant/);
  assert.match(roleViewSource, /initializeTenantUserScope/);
  assert.match(roleViewSource, /secondary="当前租户"/);
  assert.match(roleViewSource, /v-if="!canBrowseTenants && currentUserTenant"/);
  assert.match(roleViewSource, /selectPlatformScope/);
  assert.match(roleViewSource, /title: '平台角色'/);
  assert.match(roleViewSource, /selectTenantRootScope/);
  assert.match(roleViewSource, /selectOrganizationScope/);
  assert.match(roleViewSource, /fieldName: 'ownerScopeType'/);
  assert.match(roleViewSource, /fieldName: 'ownerScopeId'/);
  assert.match(roleViewSource, /values: \[scope\.kind\]/);
  assert.match(roleViewSource, /values: \[scope\.id\]/);
  assert.match(roleViewSource, /createScopedTreeModuleContext/);
  assert.match(roleViewSource, /treePath: '\/iam\.organization\/tree'/);
  assert.match(roleViewSource, /scopeFieldName: 'tenantId'/);
  assert.match(roleViewSource, /onMounted\(loadRoleFormDefinition\)/);
  assert.match(roleViewSource, /resolveRecordFormFields\(runtimeContext\.uiDescriptor\)/);
  assert.match(roleViewSource, /roleFormFieldDefinitions = ref\(resolveRecordFormFields\(undefined\)\)/);
  assert.match(roleViewSource, /:fields="roleFormFieldDefinitions"/);
  assert.match(roleViewSource, /:fallback="roleFormFieldFallback"/);
  assert.match(roleViewSource, /tenantId: scopeTenantId\(scope\)/);
  assert.match(roleViewSource, /function scopeTenantId\(scope: RoleScope \| undefined\)/);
  assert.match(roleViewSource, /scope\?\.kind === 'platform'/);
  assert.match(roleViewSource, /scope\?\.tenant\?\.id \?\? scope\?\.id/);
  assert.match(roleViewSource, /assignmentType: \{[\s\S]*controlType: 'select'/);
  assert.match(roleViewSource, /roleKind: \{[\s\S]*controlType: 'select'/);
  assert.match(roleViewSource, /sharePolicy: \{[\s\S]*options: sharePolicyOptions/);
  assert.match(roleViewSource, /roleDraft\.value\.roleKind === 'group'/);
  assert.match(roleViewSource, /fieldName === 'ownerScopeType' \|\| fieldName === 'ownerScopeId'/);
  assert.match(roleViewSource, /roleDetailMode\.value === 'edit' && \['assignmentType', 'roleKind'\]/);
  assert.match(roleViewSource, /selectedRole\.value\?\.systemManaged/);
  assert.match(roleViewSource, /target as Role\)\.systemManaged !== true/);
  assert.match(roleViewSource, /const roleListColumns = computed<RecordQueryListColumn\[\]>/);
  assert.match(roleViewSource, /:columns="roleListColumns"/);
  assert.match(roleViewSource, /function assignmentTypeTitle/);
  assert.match(roleViewSource, /function roleKindTitle/);
  assert.match(roleViewSource, /function sharePolicyTitle/);
  assert.match(roleViewSource, /commitRoleDetailRecord\(fullRecord, mode\)/);
  assert.match(roleViewSource, /nextMode === 'edit' && record\.systemManaged !== true \? 'edit' : 'view'/);
  assert.match(roleViewSource, /standard-crud-actions/);
  assert.match(roleViewSource, /standard-crud-row-actions/);
  assert.doesNotMatch(roleViewSource, /account-grants/);
  assert.doesNotMatch(roleViewSource, /employment-grants/);
  assert.doesNotMatch(roleViewSource, /permissionMatrix/);
  assert.doesNotMatch(roleViewSource, /rolePermissions/);
  assert.match(panelSource, /record\[`\$\{fieldName\}Title`\]/);
  assert.match(contractsSource, /export type RoleAssignmentType = 'account' \| 'employment'/);
  assert.match(contractsSource, /export type RoleOwnerScopeType = 'platform' \| 'tenant' \| 'organization'/);
  assert.match(
    contractsSource,
    /export type RoleSharePolicy = 'private' \| 'ownerAndChildren' \| 'tenant' \| 'platform'/,
  );
});

test('user management keeps account basics separate from employment binding and role authorization', () => {
  const userViewSource = readSource('src/views/UserManagementView.vue');
  const routesSource = readSource('src/app/businessRoutes.ts');
  const contractsSource = readSource('src/web-contracts/index.ts');
  const inputSource = readSource('src/vue-ui-antdv/components/UiInput.vue');
  const iconSource = readSource('src/vue-ui-antdv/components/UiIcon.vue');

  assert.match(routesSource, /moduleAlias: 'iam\.user'/);
  assert.match(routesSource, /route: '\/iam\/users'/);
  assert.match(userViewSource, /defineOptions\(\{ name: 'UserManagementView' \}\)/);
  assert.match(userViewSource, /moduleAlias: 'iam\.tenant'/);
  assert.match(userViewSource, /moduleAlias: 'iam\.user'/);
  assert.match(userViewSource, /user-management-page/);
  assert.match(userViewSource, /<CrudRecordListExplorer/);
  assert.match(userViewSource, /<RecordQueryListPanel/);
  assert.match(userViewSource, /standard-crud-actions/);
  assert.match(userViewSource, /standard-crud-row-actions/);
  assert.match(userViewSource, /canBrowseTenants/);
  assert.match(userViewSource, /currentUserTenant/);
  assert.match(userViewSource, /initializeTenantUserScope/);
  assert.match(userViewSource, /fieldName: 'tenantId'/);
  assert.match(userViewSource, /createScopedUserModuleContext/);
  assert.match(userViewSource, /onMounted\(loadUserFormDefinition\)/);
  assert.match(userViewSource, /resolveRecordFormFields\(runtimeContext\.uiDescriptor\)/);
  assert.match(userViewSource, /userFormFieldDefinitions = ref\(resolveRecordFormFields\(undefined\)\)/);
  assert.match(userViewSource, /<RecordFormFields/);
  assert.match(userViewSource, /:fields="userFormFieldDefinitions"/);
  assert.match(userViewSource, /:fallback="userFormFieldFallback"/);
  assert.match(userViewSource, /username: \{ label: '账号'/);
  assert.match(userViewSource, /enabled: \{ label: '允许登录'/);
  assert.match(userViewSource, /key: 'resetPassword'[\s\S]*actionCode: 'changePassword'/);
  assert.match(userViewSource, /key: 'resetGeneratedPassword'[\s\S]*actionCode: 'resetPassword'/);
  assert.match(userViewSource, /title: '修改密码'/);
  assert.match(userViewSource, /title: '重置密码'/);
  assert.match(userViewSource, /temporaryPassword/);
  assert.match(
    userViewSource,
    /userDetailMode\.value === 'resetPassword'[\s\S]*const userId = selectedUser\.value\?\.id[\s\S]*userContext\.can\('changePassword', userId\)/,
  );
  assert.match(userViewSource, /:record-id="selectedUser\?\.id"/);
  assert.match(userViewSource, /path: `\/iam\.user\/changePassword\/\$\{encodeURIComponent\(user\.id!\)\}`/);
  assert.match(userViewSource, /path: `\/iam\.user\/resetPassword\/\$\{encodeURIComponent\(user\.id!\)\}`/);
  assert.match(userViewSource, /type="password"/);
  assert.match(inputSource, /type\?: 'text' \| 'password'/);
  assert.match(iconSource, /LockOutlined/);
  assert.match(contractsSource, /export interface UserAccount extends StandardEnabledSortableEntity/);
  assert.match(
    contractsSource,
    /export type UserPasswordStatus = 'normal' \| 'initial' \| 'resetRequired' \| 'expired'/,
  );
  assert.match(contractsSource, /passwordStatusTitle\?: string/);
  assert.match(contractsSource, /export interface ResetPasswordResponse/);
  assert.match(contractsSource, /username\?: string/);
  assert.match(contractsSource, /password\?: string/);
  assert.doesNotMatch(contractsSource, /passwordHash/);
  assert.doesNotMatch(userViewSource, /iam\.employee_account/);
  assert.doesNotMatch(userViewSource, /iam\.role_assignment/);
  assert.doesNotMatch(userViewSource, /moduleAlias: 'iam\.organization'/);
  assert.doesNotMatch(userViewSource, /fieldName: 'organizationId'/);
  assert.doesNotMatch(userViewSource, /系统账号/);
  assert.doesNotMatch(userViewSource, /operator: 'NULL'/);
  assert.doesNotMatch(userViewSource, /permissionMatrix/);
  assert.doesNotMatch(userViewSource, /sessionList|sessionAudit|revokeSession/);
});

test('system user management is a separate root account entry', () => {
  const systemUserViewSource = readSource('src/views/SystemUserManagementView.vue');
  const userViewSource = readSource('src/views/UserManagementView.vue');
  const routesSource = readSource('src/app/businessRoutes.ts');

  assert.match(routesSource, /moduleAlias: 'iam\.system_user'/);
  assert.match(routesSource, /route: '\/iam\/system-users'/);
  assert.match(systemUserViewSource, /defineOptions\(\{ name: 'SystemUserManagementView' \}\)/);
  assert.match(systemUserViewSource, /moduleAlias: 'iam\.user'/);
  assert.match(systemUserViewSource, /system-user-management-page/);
  assert.match(systemUserViewSource, /height: calc\(100vh - 116px\)/);
  assert.match(systemUserViewSource, /overflow: hidden/);
  assert.match(systemUserViewSource, /<RecordQueryListPanel/);
  assert.match(systemUserViewSource, /<RecordModeDrawer/);
  assert.match(systemUserViewSource, /:mode="detailMode"/);
  assert.match(systemUserViewSource, /:form-modes="\['edit', 'resetPassword'\]"/);
  assert.match(systemUserViewSource, /<template #view>/);
  assert.match(systemUserViewSource, /<template #form>/);
  assert.match(systemUserViewSource, /<RecordDetailFields/);
  assert.match(systemUserViewSource, /<RecordFormFields/);
  assert.match(systemUserViewSource, /<RecordStatusSwitch/);
  assert.match(systemUserViewSource, /<RecordActionBar/);
  assert.match(systemUserViewSource, /fieldName: 'tenantId'/);
  assert.match(systemUserViewSource, /operator: 'NULL'/);
  assert.match(systemUserViewSource, /title="系统账号"/);
  assert.match(systemUserViewSource, /function rowActionsOf/);
  assert.match(systemUserViewSource, /actionCode: 'view'/);
  assert.match(systemUserViewSource, /actionCode: 'update'/);
  assert.match(systemUserViewSource, /actionCode: 'changePassword'/);
  assert.match(systemUserViewSource, /actionCode: 'resetPassword'/);
  assert.match(systemUserViewSource, /:record-id="selectedUser\?\.id"/);
  assert.match(systemUserViewSource, /title: '修改密码'/);
  assert.match(systemUserViewSource, /title: '重置密码'/);
  assert.match(systemUserViewSource, /temporaryPassword/);
  assert.match(
    systemUserViewSource,
    /path: `\/iam\.user\/changePassword\/\$\{encodeURIComponent\(user\.id!\)\}`/,
  );
  assert.match(
    systemUserViewSource,
    /path: `\/iam\.user\/resetPassword\/\$\{encodeURIComponent\(user\.id!\)\}`/,
  );
  assert.match(systemUserViewSource, /tenantId: undefined/);
  assert.match(systemUserViewSource, /enabled: \{ label: '允许登录'/);
  assert.match(systemUserViewSource, /systemUserFormFieldDisabled/);
  assert.doesNotMatch(systemUserViewSource, /<CrudRecordListExplorer/);
  assert.doesNotMatch(systemUserViewSource, /<TreeRecordExplorer/);
  assert.doesNotMatch(systemUserViewSource, /standard-crud-actions/);
  assert.doesNotMatch(systemUserViewSource, /standard-crud-row-actions/);
  assert.doesNotMatch(systemUserViewSource, /actionCode: 'create'/);
  assert.doesNotMatch(systemUserViewSource, /actionCode: 'delete'/);
  assert.doesNotMatch(userViewSource, /iam\.system_user/);
});

test('password management is a dedicated security settings page', () => {
  const passwordViewSource = readSource('src/views/PasswordManagementView.vue');
  const routesSource = readSource('src/app/businessRoutes.ts');
  const startupSource = readSource('src/app/appWorkbenchStartup.ts');
  const contractsSource = readSource('src/web-contracts/index.ts');

  assert.match(routesSource, /moduleAlias: 'iam\.password_policy_rule'/);
  assert.match(routesSource, /route: '\/platform\/security\/passwords'/);
  assert.match(startupSource, /businessModuleRoutes/);
  assert.match(startupSource, /businessRoutePrefixes/);
  assert.match(passwordViewSource, /defineOptions\(\{ name: 'PasswordManagementView' \}\)/);
  assert.match(passwordViewSource, /moduleAlias: 'iam\.password_policy_rule'/);
  assert.match(passwordViewSource, /<StaticManagementLayout/);
  assert.match(passwordViewSource, /<CrudRecordListExplorer/);
  assert.match(passwordViewSource, /<RecordActionBar/);
  assert.match(passwordViewSource, /<RecordStatusSwitch/);
  assert.match(passwordViewSource, /密码试算/);
  assert.match(passwordViewSource, /new RegExp\(rule\.pattern/);
  assert.match(passwordViewSource, /scopeType: 'global'/);
  assert.doesNotMatch(passwordViewSource, /ruleCode/);
  assert.doesNotMatch(passwordViewSource, /规则编码/);
  assert.match(passwordViewSource, /pattern/);
  assert.match(passwordViewSource, /message/);
  assert.match(passwordViewSource, /description/);
  assert.match(contractsSource, /export type PasswordPolicyScopeType = 'global' \| 'tenant'/);
  assert.match(contractsSource, /export interface PasswordPolicyRule extends StandardEnabledSortableEntity/);
  assert.doesNotMatch(contractsSource, /ruleCode/);
});

test('workbench exposes own password change through auth boundary', () => {
  const appSource = readSource('src/App.vue');
  const workbenchSource = readSource('src/platform-workbench/Workbench.vue');
  const dialogSource = readSource('src/app/ChangeOwnPasswordDialog.vue');
  const authClientSource = readSource('src/web-core/clients.ts');

  assert.match(workbenchSource, /key: 'changePassword'/);
  assert.match(workbenchSource, /title: '修改密码'/);
  assert.match(appSource, /command === 'changePassword'[\s\S]*openChangeOwnPasswordDialog\(\)/);
  assert.match(appSource, /authClient\.changeOwnPassword/);
  assert.match(appSource, /effectiveAuthToken/);
  assert.match(appSource, /currentPassword: currentPassword\.value/);
  assert.match(appSource, /newPassword: newPassword\.value/);
  assert.match(dialogSource, /defineOptions\(\{ name: 'ChangeOwnPasswordDialog' \}\)/);
  assert.match(dialogSource, /autocomplete="current-password"/);
  assert.match(dialogSource, /autocomplete="new-password"/);
  assert.match(authClientSource, /path: '\/iam\.auth\/changeOwnPassword'/);
  assert.doesNotMatch(appSource, /iam\.user\/changePassword/);
  assert.doesNotMatch(appSource, /iam\.user\/resetPassword/);
});

test('dynamic module host uses shared descriptor driven list and form runners', () => {
  const hostSource = readSource('src/platform-workbench/hosts/DynamicModuleHost.vue');

  assert.match(hostSource, /useModuleContext<QueryListRecord>/);
  assert.match(hostSource, /<RecordQueryListPanel/);
  assert.match(hostSource, /<RecordFormFields/);
  assert.match(hostSource, /resolveRecordFormFields\(runtimeContext\.uiDescriptor, view\?\.viewCode\)/);
  assert.match(hostSource, /isListPage/);
  assert.match(hostSource, /listUiConfigId/);
  assert.match(hostSource, /:ui-config-id="listUiConfigId"/);
  assert.match(hostSource, /:query-template-id="descriptor\.target\.defaultQueryTemplateId"/);
  assert.match(hostSource, /动态\$\{pageMode\.value\}入口暂未接入运行器/);
  assert.doesNotMatch(hostSource, /等待接入页面 bootstrap 与列表查询/);
});

test('record query list panel forwards dynamic ui config and query template ids', () => {
  const panelSource = readSource('src/platform-components/RecordQueryListPanel.vue');

  assert.match(panelSource, /uiConfigId\?: string/);
  assert.match(panelSource, /queryTemplateId\?: string/);
  assert.match(panelSource, /querySchema\(\{\s*uiConfigId: props\.uiConfigId,\s*\}\)/);
  assert.match(panelSource, /request\.uiConfigId = props\.uiConfigId/);
  assert.match(panelSource, /request\.queryTemplateId = props\.queryTemplateId/);
});

test('platform error feedback respects global error presentation slots', () => {
  const feedbackSource = readSource('src/platform-components/platformErrorFeedback.ts');
  const uiFeedbackSource = readSource('src/vue-ui-antdv/feedback.ts');
  const staticCrudStateSource = readSource('src/platform-components/staticCrudManagementState.ts');
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
