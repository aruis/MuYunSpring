<script setup lang="ts">
import { computed, ref } from 'vue';
import {
  CrudRecordListExplorer,
  ModuleActionButton,
  RecordActionBar,
  RecordDetailPanel,
  RecordExplorerPanel,
  RecordMetaSection,
  RecordPicker,
  RecordStatusSwitch,
  TreeRecordExplorer,
  createScopedResourceTreeModuleContext,
  parentRecordConstraints,
  type CrudRecordListBase,
  type RecordActionItem,
  type TreeRecordBase,
} from '@muyun/platform-components';
import type { MenuRecord, MenuScheme, Option, PlatformModule } from '@muyun/web-contracts';
import { useModuleContext, type ModuleContext } from '@muyun/web-core';
import { confirmAction, UiEmpty, UiInput, UiSelect, type UiRecordInlineAction } from '@muyun/vue-ui-antdv';
import { useCurrentUserContext } from '../app/currentUserContext';
import { createMenuManagementState, menuTitleOf, schemeTitleOf } from './menuManagementState';

defineOptions({ name: 'MenuManagementView' });

const schemeContext = useModuleContext<MenuScheme>({ moduleAlias: 'platform.menu_scheme' });
const menuBaseContext = useModuleContext<MenuRecord>({ moduleAlias: 'platform.menu' });
const moduleContext = useModuleContext<PlatformModule>({ moduleAlias: 'platform.module' });
const currentUser = useCurrentUserContext();
const schemeSearchKeyword = ref('');
const menuSearchKeyword = ref('');
const {
  schemeReloadKey,
  menuReloadKey,
  selectedScheme,
  selectedMenu,
  schemeDraft,
  menuDraft,
  schemeMode,
  menuMode,
  savingScheme,
  savingMenu,
  selectedSchemeId,
  schemeReadonly,
  menuReadonly,
  canToggleScheme,
  canToggleMenu,
  schemeCardTitle,
  menuCardTitle,
  handleSchemesLoaded,
  selectScheme,
  handleMenusLoaded,
  selectMenu,
  startCreateScheme,
  startEditScheme,
  cancelSchemeEdit,
  startCreateRootMenu,
  startCreateChildMenu,
  startEditMenu,
  cancelMenuEdit,
  saveScheme,
  saveMenu,
  toggleMenuEnabled,
  removeSelectedScheme,
  removeSelectedMenu,
} = createMenuManagementState(schemeContext, () => menuContext.value, confirmAction, {
  currentUser: () => currentUser?.value,
});

const menuContext = computed<ModuleContext<MenuRecord>>(() =>
  createScopedResourceTreeModuleContext<MenuRecord, MenuRecord>(menuBaseContext, {
    resourcePath: selectedSchemeId.value
      ? `/platform.menu-scheme/${encodeURIComponent(selectedSchemeId.value)}/menus`
      : undefined,
    emptyQueryScopeName: 'platform.menu.empty',
  }),
);
const schemeActions = computed<RecordActionItem[]>(() => {
  if (schemeMode.value !== 'view') {
    return [
      { key: 'cancel', title: '取消', disabled: savingScheme.value },
      {
        key: 'save',
        actionCode: schemeMode.value === 'create' ? 'create' : 'update',
        title: savingScheme.value ? '保存中' : '保存',
        loading: savingScheme.value,
        primary: true,
      },
    ];
  }
  return [
    { key: 'edit', actionCode: 'update', title: '编辑', disabled: !selectedScheme.value },
    {
      key: 'delete',
      actionCode: 'delete',
      title: '删除',
      disabled: !selectedScheme.value,
      loading: savingScheme.value,
      danger: true,
    },
  ];
});
const menuActions = computed<RecordActionItem[]>(() => {
  if (menuMode.value !== 'view') {
    return [
      { key: 'cancel', title: '取消', disabled: savingMenu.value },
      {
        key: 'save',
        actionCode: menuMode.value === 'edit' ? 'update' : 'create',
        title: savingMenu.value ? '保存中' : '保存',
        loading: savingMenu.value,
        primary: true,
      },
    ];
  }
  return [
    { key: 'edit', actionCode: 'update', title: '编辑', disabled: !selectedMenu.value },
    {
      key: 'create-child',
      actionCode: 'create',
      title: '新建下级',
      disabled: !selectedMenu.value,
    },
    {
      key: 'delete',
      actionCode: 'delete',
      title: '删除',
      disabled: !selectedMenu.value,
      loading: savingMenu.value,
      danger: true,
    },
  ];
});
const scopeTypeOptions: Option[] = [
  { label: '系统', value: 'system' },
  { label: '租户', value: 'tenant' },
  { label: '机构', value: 'organization' },
];
const menuTypeOptions: Option[] = [
  { label: '分组', value: 'group' },
  { label: '模块', value: 'module' },
  { label: '路由', value: 'route' },
  { label: '外链', value: 'link' },
];
const openModeOptions: Option[] = [
  { label: '页签', value: 'tab' },
  { label: '新窗口', value: 'window' },
];
const pageModeOptions: Option[] = [
  { label: '列表', value: 'LIST' },
  { label: '表单', value: 'FORM' },
  { label: '详情', value: 'DETAIL' },
];
const menuParentPickerContext = computed(() => menuContext.value);
const menuParentPickerConstraints = computed(() => parentRecordConstraints(menuDraft.value.id));
const menuFormDisabled = computed(() => menuReadonly.value || savingMenu.value);
const schemeIdentityReadonly = computed(() => schemeMode.value !== 'create' || savingScheme.value);
const menuType = computed(() => menuDraft.value.menuType ?? 'group');
const schemeEditorVisible = computed(() => schemeMode.value !== 'view');

function schemeSubtitle(record: CrudRecordListBase) {
  const scheme = record as MenuScheme;
  return [scopeTypeTitle(scheme.scopeType), scheme.alias ?? scheme.id].filter(Boolean).join(' / ');
}

function handleSchemeLoaded(records: CrudRecordListBase[]) {
  handleSchemesLoaded(records as MenuScheme[]);
}

function handleSchemeSelect(record: CrudRecordListBase) {
  selectScheme(record as MenuScheme);
}

function handleMenuLoaded(records: TreeRecordBase[]) {
  handleMenusLoaded(records as MenuRecord[]);
}

function handleMenuSelect(record: TreeRecordBase) {
  selectMenu(record as MenuRecord);
}

function menuTagOf(record: TreeRecordBase) {
  const menu = record as MenuRecord;
  if (menu.enabled === false) {
    return '停用';
  }
  return menuTypeTitle(menu.menuType);
}

function menuFilterOption(record: TreeRecordBase, keyword: string) {
  const menu = record as MenuRecord;
  return [menu.title, menu.id, menu.moduleAlias, menu.route, menu.externalUrl]
    .filter(Boolean)
    .some((value) => String(value).toLowerCase().includes(keyword));
}

function menuTreeActionsOf(record: TreeRecordBase): UiRecordInlineAction[] {
  const menu = record as MenuRecord;
  const actions: UiRecordInlineAction[] = [];
  if (menu.id && menuContext.value.can('create') === true) {
    actions.push({ key: 'create-child', title: '新增下级', iconName: 'plus' });
  }
  if (menu.id && menuContext.value.can('update') === true) {
    actions.push({ key: 'edit', title: '编辑菜单', iconName: 'edit' });
  }
  if (menu.id && menuContext.value.can('delete') === true) {
    actions.push({ key: 'delete', title: '删除菜单', iconName: 'delete', danger: true });
  }
  return actions;
}

function handleMenuTreeAction(action: UiRecordInlineAction, record: TreeRecordBase) {
  const menu = record as MenuRecord;
  selectMenu(menu);
  if (action.key === 'create-child') {
    startCreateChildMenu(menu);
    return;
  }
  if (action.key === 'edit') {
    startEditMenu();
    return;
  }
  if (action.key === 'delete') {
    void removeSelectedMenu();
  }
}

function handleSchemeAction(action: RecordActionItem) {
  if (action.key === 'edit') {
    startEditScheme();
    return;
  }
  if (action.key === 'delete') {
    void removeSelectedScheme();
    return;
  }
  if (action.key === 'cancel') {
    cancelSchemeEdit();
    return;
  }
  if (action.key === 'save') {
    void saveScheme();
  }
}

function handleMenuAction(action: RecordActionItem) {
  if (action.key === 'edit') {
    startEditMenu();
    return;
  }
  if (action.key === 'create-child') {
    startCreateChildMenu();
    return;
  }
  if (action.key === 'delete') {
    void removeSelectedMenu();
    return;
  }
  if (action.key === 'cancel') {
    cancelMenuEdit();
    return;
  }
  if (action.key === 'save') {
    void saveMenu();
  }
}

function moduleTitle(record: PlatformModule) {
  return record.title ?? record.alias ?? record.id ?? '模块';
}

function moduleDescription(record: PlatformModule) {
  return [record.applicationAlias, record.id].filter(Boolean).join(' / ');
}

function scopeTypeTitle(value: MenuScheme['scopeType']) {
  return scopeTypeOptions.find((item) => item.value === value)?.label ?? '租户';
}

function menuTypeTitle(value: MenuRecord['menuType']) {
  return menuTypeOptions.find((item) => item.value === value)?.label ?? '分组';
}
</script>

<template>
  <section class="menu-management-page">
    <div class="menu-management-grid">
      <RecordExplorerPanel
        title="菜单方案"
        refresh-title="刷新菜单方案"
        :search-keyword="schemeSearchKeyword"
        search-placeholder="搜索方案名称、alias 或 ID"
        @update:search-keyword="schemeSearchKeyword = $event"
        @refresh="schemeReloadKey += 1"
      >
        <template #actions>
          <ModuleActionButton
            class="record-panel-create-button"
            :context="schemeContext"
            action-code="update"
            title="编辑菜单方案"
            icon-only
            :disabled="!selectedScheme || savingScheme"
            @click="startEditScheme"
          />
          <ModuleActionButton
            class="record-panel-create-button"
            :context="schemeContext"
            action-code="delete"
            title="删除菜单方案"
            icon-only
            danger
            :disabled="!selectedScheme || savingScheme"
            @click="removeSelectedScheme"
          />
          <ModuleActionButton
            class="record-panel-create-button"
            :context="schemeContext"
            action-code="create"
            title="新建菜单方案"
            icon-only
            @click="startCreateScheme"
          />
        </template>
        <CrudRecordListExplorer
          :context="schemeContext"
          :selected-id="selectedScheme?.id"
          :reload-key="schemeReloadKey"
          :keyword="schemeSearchKeyword"
          empty-description="暂无菜单方案"
          loading-tip="加载菜单方案"
          fallback-title="未命名方案"
          :title-of="(record) => schemeTitleOf(record as MenuScheme)"
          :subtitle-of="schemeSubtitle"
          @select="handleSchemeSelect"
          @loaded="handleSchemeLoaded"
        />
        <template #editor>
          <Transition name="scheme-editor-drawer">
            <section v-if="schemeEditorVisible" class="scheme-editor-panel">
              <header class="scheme-editor-header">
                <h3>{{ schemeCardTitle }}</h3>
                <RecordActionBar
                  :context="schemeContext"
                  :actions="schemeActions"
                  size="compact"
                  @action="handleSchemeAction"
                />
              </header>

              <form class="scheme-form" @submit.prevent="saveScheme">
                <label>
                  <span>方案 alias</span>
                  <UiInput v-model:value="schemeDraft.alias" :disabled="schemeIdentityReadonly" />
                </label>
                <label>
                  <span>方案名称</span>
                  <UiInput v-model:value="schemeDraft.title" :disabled="schemeReadonly || savingScheme" />
                </label>
                <label>
                  <span>scope 类型</span>
                  <UiSelect
                    v-model:value="schemeDraft.scopeType"
                    :options="scopeTypeOptions"
                    :disabled="schemeIdentityReadonly"
                    :allow-clear="false"
                  />
                </label>
                <label>
                  <span>scope ID</span>
                  <UiInput v-model:value="schemeDraft.scopeId" :disabled="schemeIdentityReadonly" />
                </label>
              </form>

              <section v-if="schemeMode === 'edit' && selectedScheme?.id" class="scheme-status-panel">
                <RecordStatusSwitch
                  :enabled="schemeDraft.enabled"
                  :disabled="savingScheme || !canToggleScheme"
                  @change="schemeDraft.enabled = $event"
                />
              </section>
            </section>
          </Transition>
        </template>
      </RecordExplorerPanel>

      <RecordExplorerPanel
        title="菜单树"
        refresh-title="刷新菜单树"
        :search-keyword="menuSearchKeyword"
        search-placeholder="搜索菜单、模块或路由"
        @update:search-keyword="menuSearchKeyword = $event"
        @refresh="menuReloadKey += 1"
      >
        <template #actions>
          <ModuleActionButton
            class="record-panel-create-button"
            :context="menuContext"
            action-code="create"
            title="新建根菜单"
            icon-only
            :disabled="!selectedSchemeId"
            @click="startCreateRootMenu"
          />
        </template>
        <TreeRecordExplorer
          v-if="selectedSchemeId"
          :context="menuContext"
          :selected-id="selectedMenu?.id"
          :reload-key="menuReloadKey"
          :keyword="menuSearchKeyword"
          search-trigger="external"
          empty-description="暂无菜单"
          loading-tip="加载菜单树"
          fallback-title="未命名菜单"
          :title-of="(record) => menuTitleOf(record as MenuRecord)"
          :tag-of="menuTagOf"
          :filter-option="menuFilterOption"
          :actions-of="menuTreeActionsOf"
          @select="handleMenuSelect"
          @action="handleMenuTreeAction"
          @loaded="handleMenuLoaded"
        />
        <UiEmpty v-else description="请先选择菜单方案" />
      </RecordExplorerPanel>

      <RecordDetailPanel class="menu-detail-column" :title="menuCardTitle">
        <template #status>
          <RecordStatusSwitch
            v-if="menuMode !== 'view'"
            :enabled="menuDraft.enabled"
            :show-label="false"
            @change="menuDraft.enabled = $event"
          />
          <RecordStatusSwitch
            v-else-if="selectedMenu"
            :enabled="selectedMenu.enabled"
            :disabled="savingMenu || !canToggleMenu"
            :loading="savingMenu"
            :show-label="false"
            @change="toggleMenuEnabled"
          />
        </template>
        <template #actions>
          <RecordActionBar :context="menuContext" :actions="menuActions" @action="handleMenuAction" />
        </template>

        <UiEmpty v-if="!selectedMenu && menuMode === 'view'" description="请选择或新建菜单" />
        <form v-else class="static-record-form" @submit.prevent="saveMenu">
          <label>
            <span>菜单 ID</span>
            <UiInput
              v-model:value="menuDraft.id"
              :disabled="menuMode !== 'create-root' && menuMode !== 'create-child'"
            />
          </label>
          <label>
            <span>菜单名称</span>
            <UiInput v-model:value="menuDraft.title" :disabled="menuFormDisabled" />
          </label>
          <label>
            <span>上级菜单</span>
            <RecordPicker
              v-model:value="menuDraft.parentId"
              :context="menuParentPickerContext"
              :reload-key="menuReloadKey"
              placeholder="根菜单留空"
              :disabled="menuFormDisabled"
              :constraints="menuParentPickerConstraints"
              :title-of="(record) => menuTitleOf(record as MenuRecord)"
            />
          </label>
          <label>
            <span>菜单类型</span>
            <UiSelect
              v-model:value="menuDraft.menuType"
              :options="menuTypeOptions"
              :disabled="menuFormDisabled"
              :allow-clear="false"
            />
          </label>
          <label v-if="menuType !== 'group'">
            <span>打开方式</span>
            <UiSelect
              v-model:value="menuDraft.openMode"
              :options="openModeOptions"
              :disabled="menuFormDisabled"
              :allow-clear="false"
            />
          </label>
          <label v-if="menuType !== 'group'">
            <span>模块入口</span>
            <RecordPicker
              v-model:value="menuDraft.moduleAlias"
              :context="moduleContext"
              mode="list"
              placeholder="选择模块入口"
              :disabled="menuFormDisabled"
              :title-of="(record) => moduleTitle(record as PlatformModule)"
              :description-of="(record) => moduleDescription(record as PlatformModule)"
            />
          </label>
          <label v-if="menuType === 'module'">
            <span>页面模式</span>
            <UiSelect
              v-model:value="menuDraft.pageMode"
              :options="pageModeOptions"
              placeholder="默认列表"
              :disabled="menuFormDisabled"
            />
          </label>
          <label v-if="menuType === 'module'">
            <span>默认 UI 配置</span>
            <UiInput v-model:value="menuDraft.defaultUiConfigId" :disabled="menuFormDisabled" />
          </label>
          <label v-if="menuType === 'module'">
            <span>默认查询模板</span>
            <UiInput v-model:value="menuDraft.defaultQueryTemplateId" :disabled="menuFormDisabled" />
          </label>
          <label v-if="menuType === 'module' || menuType === 'route'" class="full-row">
            <span>入口参数 JSON</span>
            <UiInput v-model:value="menuDraft.entryParamsJson" :disabled="menuFormDisabled" />
          </label>
        </form>

        <RecordMetaSection v-if="selectedMenu || menuMode !== 'view'" :record="menuDraft" show-sort-order />
      </RecordDetailPanel>
    </div>
  </section>
</template>

<style scoped>
.menu-management-page {
  display: grid;
  gap: 12px;
  min-height: calc(100vh - 116px);
}

.menu-management-grid {
  display: grid;
  grid-template-columns: minmax(240px, 280px) minmax(280px, 340px) minmax(440px, 1fr);
  gap: 12px;
  min-height: 0;
}

.menu-management-grid > *,
.menu-detail-column {
  min-width: 0;
}

.menu-management-grid > :deep(.record-explorer-panel) {
  border: 1px solid var(--muyun-border);
  border-radius: 8px;
  background: var(--muyun-surface);
}

.menu-management-grid :deep(.record-panel-create-button) {
  width: 28px;
  height: 28px;
  padding: 0;
  border-radius: 999px;
}

.menu-detail-column {
  display: grid;
  align-content: start;
  min-height: 0;
  border: 1px solid var(--muyun-border);
  border-radius: 8px;
  background: var(--muyun-surface);
}

.static-record-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(180px, 1fr));
  gap: 14px;
}

.static-record-form label {
  display: grid;
  gap: 6px;
  color: var(--muyun-text-body);
  font-size: 13px;
}

.static-record-form .full-row {
  grid-column: 1 / -1;
}

.scheme-editor-panel {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 3;
  display: grid;
  align-content: start;
  gap: 10px;
  max-height: min(460px, 68%);
  min-height: 0;
  padding: 12px;
  border: 1px solid var(--muyun-border);
  border-top: 1px solid var(--muyun-border-subtle);
  border-radius: 8px 8px 0 0;
  background: var(--muyun-surface);
  box-shadow:
    0 -1px 0 rgb(15 23 42 / 4%),
    0 -12px 28px rgb(15 23 42 / 12%);
  overflow: auto;
}

.scheme-editor-drawer-enter-active,
.scheme-editor-drawer-leave-active {
  transition:
    transform 0.18s ease,
    opacity 0.18s ease;
}

.scheme-editor-drawer-enter-from,
.scheme-editor-drawer-leave-to {
  opacity: 0;
  transform: translateY(100%);
}

.scheme-editor-drawer-enter-to,
.scheme-editor-drawer-leave-from {
  opacity: 1;
  transform: translateY(0);
}

.scheme-editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-width: 0;
}

.scheme-editor-header h3 {
  margin: 0;
  overflow: hidden;
  color: var(--muyun-text);
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.scheme-editor-header :deep(.record-action-bar) {
  flex: 0 0 auto;
}

.scheme-form {
  display: grid;
  gap: 12px;
}

.scheme-form label {
  display: grid;
  gap: 6px;
  color: var(--muyun-text-body);
  font-size: 13px;
}

.scheme-status-panel {
  padding-top: 10px;
  border-top: 1px solid var(--muyun-border-subtle);
}

@media (max-width: 1200px) {
  .menu-management-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 800px) {
  .menu-management-grid {
    grid-template-columns: 1fr;
  }

  .static-record-form {
    grid-template-columns: 1fr;
  }
}
</style>
