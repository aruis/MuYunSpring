<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import {
  CrudRecordListExplorer,
  ModuleActionButton,
  RecordActionBar,
  RecordDetailFields,
  RecordMetaSection,
  RecordStatusSwitch,
  StaticManagementLayout,
  createStaticTreeResourceModuleContext,
  presentPlatformError,
  useFlatCrudManagementState,
  type CrudRecordListBase,
  type RecordActionItem,
  type RecordExplorerItemDescriptor,
  type RecordFormRecord,
} from '@muyun/platform-components';
import type { PlatformModuleAction } from '@muyun/web-contracts';
import { createStaticResourceTreeClient, useModuleContext } from '@muyun/web-core';
import { confirmAction, UiCheckbox, UiEmpty, UiInput, UiSelect, UiTextArea } from '@muyun/vue-ui-antdv';

defineOptions({ name: 'ModuleActionManagementView' });

const props = defineProps<{
  moduleAlias: string;
  moduleTitle?: string;
  moduleKind?: 'static' | 'dynamic';
  title?: string;
}>();
type ExecutorDefinition = {
  executorKey: string;
  title: string;
  description?: string;
  supportedLevels: Array<'LIST' | 'RECORD' | 'BATCH' | 'ANY'>;
};
const baseContext = useModuleContext<PlatformModuleAction>({ moduleAlias: 'platform.module_action' });
const actionContext = createStaticTreeResourceModuleContext(baseContext, {
  client: createStaticResourceTreeClient<PlatformModuleAction>(
    baseContext.http,
    `/platform.module/${encodeURIComponent(props.moduleAlias)}/actions`,
  ),
});
const searchKeyword = ref('');
const executorDefinitions = ref<ExecutorDefinition[]>([]);
const management = useFlatCrudManagementState({
  context: actionContext,
  confirmAction,
  emptyDraft: () => emptyActionDraft(props.moduleAlias),
  normalizeDraft: (record) => normalizeActionDraft(record, props.moduleAlias),
  copyRecord: (record) => ({ ...record }),
  titleOf: actionTitleOf,
  fallbackTitle: '模块动作',
  createTitle: '新建动作',
  requiredMessage: '动作编码不能为空',
  isValid: (record) => Boolean(record.actionCode?.trim()),
  recordName: '动作',
  deleteTitle: '删除动作',
  saveDeniedMessage: '当前用户无权保存模块动作',
  createDeniedMessage: '当前用户无权新建模块动作',
  enableDeniedMessage: '当前用户无权变更模块动作启停状态',
  deleteDeniedMessage: () => '当前用户无权删除模块动作',
  canDeleteRecord: (record) => record.systemManaged !== true,
  canEnableRecord: (record) => record.systemManaged !== true,
});
const { selected, draft, mode, reloadKey, saving, cardTitle, canCreate, canEnable } = management;
const canCreateManualAction = computed(
  () => props.moduleKind === 'dynamic' && canCreate.value && executorDefinitions.value.length > 0,
);
const executorOptions = computed(() =>
  executorDefinitions.value.map((executor) => ({
    value: executor.executorKey,
    label: executor.description ? `${executor.title} · ${executor.description}` : executor.title,
  })),
);
const actionLevelOptions = computed(() => {
  const executor = executorDefinitions.value.find((item) => item.executorKey === draft.value.executorKey);
  const levels = executor?.supportedLevels ?? [];
  return levels.map((value) => ({
    value,
    label: { LIST: '列表', RECORD: '单条记录', BATCH: '批量', ANY: '任意' }[value],
  }));
});

onMounted(() => void loadExecutorDefinitions());

const readonly = computed(() => management.readonly.value);
const formDisabled = computed(() => readonly.value || management.saving.value);
const cardActions = computed<RecordActionItem[]>(() => {
  if (management.mode.value !== 'view') {
    return [
      { key: 'cancel', title: '取消', disabled: management.saving.value },
      {
        key: 'save',
        actionCode: management.mode.value === 'create' ? 'create' : 'update',
        title: management.saving.value ? '保存中' : '保存',
        primary: true,
        loading: management.saving.value,
      },
    ];
  }
  const selected = management.selected.value;
  return [
    {
      key: 'edit',
      actionCode: 'update',
      title: '编辑',
      disabled: !selected || !management.canUpdate.value,
    },
    {
      key: 'delete',
      actionCode: 'delete',
      title: '删除',
      danger: true,
      loading: management.saving.value,
      disabled: !selected || selected.systemManaged === true || !management.canDelete.value,
    },
    ...(selected?.systemManaged === true && hasPermissionGovernanceOverride(selected)
      ? [{ key: 'restore-permission-governance', actionCode: 'update', title: '恢复代码声明' }]
      : []),
  ];
});

function actionItemOf(record: CrudRecordListBase): RecordExplorerItemDescriptor {
  const action = record as PlatformModuleAction;
  return {
    title: actionTitleOf(action),
    secondary: action.actionCode,
    tag: action.systemManaged === true ? '平台托管' : action.category,
    muted: action.enabled === false,
  };
}

function handleAction(action: RecordActionItem) {
  if (action.key === 'edit') management.startEdit();
  if (action.key === 'delete') void management.removeSelected();
  if (action.key === 'restore-permission-governance') void clearPermissionGovernance();
  if (action.key === 'cancel') management.cancelEdit();
  if (action.key === 'save') void management.save();
}

async function clearPermissionGovernance() {
  if (!selected.value?.id) return;
  try {
    management.saving.value = true;
    await baseContext.http.request<void>({
      path: `/platform.module/${encodeURIComponent(props.moduleAlias)}/actions/${encodeURIComponent(selected.value.id)}/permission-governance`,
      method: 'DELETE',
      body: { version: selected.value.version },
    });
    management.cancelEdit();
    reloadKey.value += 1;
  } catch (cause) {
    presentPlatformError(cause, { source: 'module-action-management', phase: 'action' });
  } finally {
    management.saving.value = false;
  }
}

function hasPermissionGovernanceOverride(action: PlatformModuleAction) {
  return [
    action.permissionActionCodeOverride,
    action.accessModeOverride,
    action.actionAuthOverride,
    action.dataAuthOverride,
    action.defaultGrantPolicyOverride,
  ].some((value) => value != null);
}

function updateDraft(field: keyof PlatformModuleAction, value: unknown) {
  management.draft.value = {
    ...management.draft.value,
    [field]: value ?? undefined,
  } as PlatformModuleAction;
}

async function loadExecutorDefinitions() {
  try {
    executorDefinitions.value = await baseContext.http.request<ExecutorDefinition[]>({
      path: '/platform.module/action-executors',
    });
  } catch (cause) {
    executorDefinitions.value = [];
    presentPlatformError(cause, { source: 'module-action-management', phase: 'load' });
  }
}

function startCreate() {
  management.startCreate();
  const executor = executorDefinitions.value[0];
  if (!executor) return;
  management.draft.value = {
    ...management.draft.value,
    executorKey: executor.executorKey,
    actionLevel: executor.supportedLevels[0] ?? 'ANY',
  };
}

function updateExecutor(executorKey: unknown) {
  const value = typeof executorKey === 'string' ? executorKey : undefined;
  const executor = executorDefinitions.value.find((item) => item.executorKey === value);
  updateDraft('executorKey', value);
  if (executor?.supportedLevels.length) {
    updateDraft('actionLevel', executor.supportedLevels[0]);
  }
}

function emptyActionDraft(moduleAlias: string): PlatformModuleAction {
  return {
    moduleAlias,
    actionCode: '',
    title: '',
    category: 'CUSTOM',
    actionLevel: 'ANY',
    accessMode: 'AUTH_REQUIRED',
    actionAuth: true,
    dataAuth: false,
    defaultGrantPolicy: 'NONE',
    executorType: 'SERVICE',
    enabled: true,
    systemManaged: false,
  };
}

function normalizeActionDraft(record: PlatformModuleAction, moduleAlias: string): PlatformModuleAction {
  return {
    ...record,
    moduleAlias,
    actionCode: trimRequired(record.actionCode),
    title: trimOptional(record.title) ?? trimRequired(record.actionCode),
    entityAlias: trimOptional(record.entityAlias),
    permissionActionCode: trimOptional(record.permissionActionCode),
    availableExpression: trimOptional(record.availableExpression),
    unavailableMessage: trimOptional(record.unavailableMessage),
    executorKey: trimOptional(record.executorKey),
  };
}

function trimRequired(value: string | undefined) {
  return value?.trim() ?? '';
}

function trimOptional(value: string | undefined) {
  const normalized = value?.trim();
  return normalized ? normalized : undefined;
}

function actionTitleOf(action: PlatformModuleAction) {
  return action.title?.trim() || action.actionCode?.trim() || action.id || '未命名动作';
}
</script>

<template>
  <StaticManagementLayout
    explorer-title="模块动作"
    refresh-title="刷新动作"
    :mode="mode"
    :detail-title="cardTitle"
    :explorer-search-keyword="searchKeyword"
    explorer-search-placeholder="搜索动作名称或编码"
    @update:explorer-search-keyword="searchKeyword = $event"
    @refresh="reloadKey += 1"
  >
    <template #explorer-actions>
      <ModuleActionButton
        class="record-panel-create-button"
        :context="actionContext"
        action-code="create"
        title="新建动作"
        icon-only
        :disabled="!canCreateManualAction || saving"
        @click="startCreate"
      />
    </template>
    <template #explorer>
      <CrudRecordListExplorer
        :context="actionContext"
        :selected-id="selected?.id"
        :reload-key="reloadKey"
        :keyword="searchKeyword"
        empty-description="当前模块暂无动作"
        loading-tip="加载模块动作"
        fallback-title="未命名动作"
        :item-of="actionItemOf"
        @loaded="management.handleListLoaded($event as PlatformModuleAction[])"
        @select="management.handleSelect($event as PlatformModuleAction)"
      />
    </template>

    <template #detail-status>
      <RecordStatusSwitch
        v-if="mode === 'view' && selected"
        :enabled="selected.enabled"
        :disabled="saving || selected.systemManaged === true || !canEnable"
        :loading="saving"
        :show-label="false"
        @change="management.toggleEnabled"
      />
    </template>
    <template #detail-actions>
      <RecordActionBar :context="actionContext" :actions="cardActions" @action="handleAction" />
    </template>

    <UiEmpty
      v-if="!selected && mode === 'view'"
      :description="moduleKind === 'static' ? '静态模块动作由 Java 声明自动注册' : '当前模块暂无动作'"
    />
    <template v-else-if="mode === 'view' && selected">
      <p v-if="selected.systemManaged" class="managed-notice">
        执行逻辑、动作编码和业务语义由代码声明维护。可编辑权限治理覆盖；保存后立即影响授权、运行时动作与
        OpenAPI 可见性。
      </p>
      <RecordDetailFields
        :record="draft as RecordFormRecord"
        :field-names="[
          'actionCode',
          'title',
          'category',
          'actionLevel',
          'accessMode',
          'permissionActionCode',
          'entityAlias',
          'executorType',
          'executorKey',
          'actionAuth',
          'dataAuth',
          'defaultGrantPolicy',
          'availableExpression',
          'unavailableMessage',
          'sourceType',
          'bindingAlias',
        ]"
        :fallback="{
          actionCode: { label: '动作编码' },
          title: { label: '动作名称' },
          category: { label: '动作类别' },
          actionLevel: { label: '执行层级' },
          accessMode: { label: '访问方式' },
          permissionActionCode: { label: '权限动作编码' },
          entityAlias: { label: '目标实体' },
          executorType: { label: '执行器类型' },
          executorKey: { label: '执行器标识' },
          actionAuth: { label: '动作授权' },
          dataAuth: { label: '数据授权' },
          defaultGrantPolicy: { label: '默认授予策略' },
          availableExpression: { label: '可用条件' },
          unavailableMessage: { label: '不可用提示' },
          sourceType: { label: '来源类型' },
          bindingAlias: { label: '绑定标识' },
        }"
      />
    </template>
    <form v-else class="static-record-form" @submit.prevent="management.save">
      <p class="form-hint wide-field">
        {{
          selected?.systemManaged
            ? '仅调整权限治理覆盖。访问方式、动作授权、数据授权和默认授予策略可以比代码声明更严格或更宽松。'
            : '手工动作只能绑定已部署的二开执行器；平台会通过通用动作接口承接权限、审计和运行态刷新。'
        }}
      </p>
      <label
        ><span>动作编码</span
        ><UiInput
          :value="draft.actionCode"
          :disabled="formDisabled || selected?.systemManaged === true"
          @update:value="updateDraft('actionCode', $event)"
      /></label>
      <label
        ><span>动作名称</span
        ><UiInput
          :value="draft.title"
          :disabled="formDisabled || selected?.systemManaged === true"
          @update:value="updateDraft('title', $event)"
      /></label>
      <label
        ><span>执行层级</span
        ><UiSelect
          :value="draft.actionLevel"
          :disabled="formDisabled || selected?.systemManaged === true"
          :options="actionLevelOptions"
          @update:value="updateDraft('actionLevel', $event)"
      /></label>
      <label
        ><span>访问方式</span
        ><UiSelect
          :value="selected?.systemManaged ? (draft.accessModeOverride ?? draft.accessMode) : draft.accessMode"
          :disabled="formDisabled"
          :options="[
            { label: '需要授权', value: 'AUTH_REQUIRED' },
            { label: '登录可用', value: 'LOGIN_REQUIRED' },
            { label: '匿名可用', value: 'ANONYMOUS_ALLOWED' },
          ]"
          @update:value="updateDraft(selected?.systemManaged ? 'accessModeOverride' : 'accessMode', $event)"
      /></label>
      <label
        ><span>权限动作编码</span
        ><UiInput
          :value="
            selected?.systemManaged
              ? (draft.permissionActionCodeOverride ?? draft.permissionActionCode)
              : draft.permissionActionCode
          "
          :disabled="formDisabled"
          placeholder="留空则使用动作编码"
          @update:value="
            updateDraft(
              selected?.systemManaged ? 'permissionActionCodeOverride' : 'permissionActionCode',
              $event,
            )
          "
      /></label>
      <label
        ><span>目标实体 alias</span
        ><UiInput
          :value="draft.entityAlias"
          :disabled="formDisabled || selected?.systemManaged === true"
          @update:value="updateDraft('entityAlias', $event)"
      /></label>
      <label
        ><span>二开执行器</span
        ><UiSelect
          :value="draft.executorKey"
          :disabled="formDisabled || selected?.systemManaged === true"
          :options="executorOptions"
          @update:value="updateExecutor($event)"
      /></label>
      <label><span>执行器类型</span><UiInput value="服务（二开）" disabled /></label>
      <label class="checkbox-field"
        ><UiCheckbox
          :checked="
            selected?.systemManaged
              ? (draft.actionAuthOverride ?? draft.actionAuth) !== false
              : draft.actionAuth !== false
          "
          :disabled="formDisabled"
          @update:checked="updateDraft(selected?.systemManaged ? 'actionAuthOverride' : 'actionAuth', $event)"
          >启用动作授权</UiCheckbox
        ></label
      >
      <label class="checkbox-field"
        ><UiCheckbox
          :checked="
            selected?.systemManaged
              ? (draft.dataAuthOverride ?? draft.dataAuth) === true
              : draft.dataAuth === true
          "
          :disabled="formDisabled"
          @update:checked="updateDraft(selected?.systemManaged ? 'dataAuthOverride' : 'dataAuth', $event)"
          >启用数据授权</UiCheckbox
        ></label
      >
      <label
        ><span>默认授予策略</span
        ><UiSelect
          :value="
            selected?.systemManaged
              ? (draft.defaultGrantPolicyOverride ?? draft.defaultGrantPolicy)
              : draft.defaultGrantPolicy
          "
          :disabled="formDisabled"
          :options="[
            { label: '不默认授予', value: 'NONE' },
            { label: '所有登录用户', value: 'ANY_LOGIN_USER' },
            { label: '记录所有者', value: 'OWNER' },
            { label: '办理人', value: 'ASSIGNEE' },
            { label: '成员', value: 'MEMBER' },
          ]"
          @update:value="
            updateDraft(selected?.systemManaged ? 'defaultGrantPolicyOverride' : 'defaultGrantPolicy', $event)
          "
      /></label>
      <label class="wide-field"
        ><span>可用条件表达式</span
        ><UiTextArea
          :value="draft.availableExpression"
          :disabled="formDisabled || selected?.systemManaged === true"
          @update:value="updateDraft('availableExpression', $event)"
      /></label>
      <label class="wide-field"
        ><span>不可用提示</span
        ><UiTextArea
          :value="draft.unavailableMessage"
          :disabled="formDisabled || selected?.systemManaged === true"
          @update:value="updateDraft('unavailableMessage', $event)"
      /></label>
    </form>
    <RecordMetaSection v-if="selected || mode !== 'view'" :record="draft" show-sort-order />
  </StaticManagementLayout>
</template>

<style scoped>
.managed-notice {
  margin: 0 0 14px;
  padding: 9px 10px;
  border: 1px solid var(--muyun-border);
  border-radius: 6px;
  background: var(--muyun-hover-subtle);
  color: var(--muyun-text-muted);
  font-size: 13px;
}
.checkbox-field {
  align-self: end;
  padding-bottom: 8px;
}
.wide-field {
  grid-column: 1 / -1;
}
.form-hint {
  margin: 0;
  color: var(--muyun-text-muted);
  font-size: 13px;
}
</style>
