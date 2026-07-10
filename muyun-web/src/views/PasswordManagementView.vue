<script setup lang="ts">
import { computed, ref } from 'vue';
import {
  CrudRecordListExplorer,
  ModuleActionButton,
  RecordActionBar,
  RecordMetaSection,
  RecordStatusSwitch,
  StaticManagementLayout,
  createPlatformActionResultEffectHandlers,
  handlePlatformActionSuccess,
  platformActionResultEffects,
  presentPlatformError,
  presentPlatformMessage,
  type CrudRecordListBase,
  type PlatformActionResultEffect,
  type RecordActionItem,
  type RecordExplorerItemDescriptor,
  withPlatformActionResultEffects,
} from '@muyun/platform-components';
import type { PasswordPolicyRule } from '@muyun/web-contracts';
import { useModuleContext } from '@muyun/web-core';
import { confirmAction, UiInput } from '@muyun/vue-ui-antdv';

defineOptions({ name: 'PasswordManagementView' });

type PasswordRuleMode = 'view' | 'create' | 'edit';
type PasswordRuleCheck = {
  key: string;
  title: string;
  message: string;
  passed: boolean;
  error?: string;
};

const ruleContext = useModuleContext<PasswordPolicyRule>({ moduleAlias: 'iam.password_policy_rule' });
const searchKeyword = ref('');
const reloadKey = ref(0);
const selected = ref<PasswordPolicyRule>();
const draft = ref<PasswordPolicyRule>(emptyRuleDraft());
const mode = ref<PasswordRuleMode>('view');
const saving = ref(false);
const testPassword = ref('');

const readonly = computed(() => mode.value === 'view');
const cardTitle = computed(() => {
  if (mode.value === 'create') {
    return '新建密码规则';
  }
  return selected.value ? ruleTitle(selected.value) : '密码规则';
});
const canCreate = computed(() => ruleContext.can('create') === true);
const canUpdate = computed(() => Boolean(selected.value?.id) && ruleContext.can('update') === true);
const canDelete = computed(() => Boolean(selected.value?.id) && ruleContext.can('delete') === true);
const canEnable = computed(() => {
  if (!selected.value?.id) {
    return false;
  }
  return ruleContext.can(selected.value.enabled === false ? 'enable' : 'disable') === true;
});
const passwordActionEffectHandlers = createPlatformActionResultEffectHandlers({
  refreshList: () => {
    reloadKey.value += 1;
  },
  closeEditor: () => {
    mode.value = 'view';
  },
  clearSelection: () => {
    selected.value = undefined;
    draft.value = emptyRuleDraft();
    mode.value = canCreate.value ? 'create' : 'view';
  },
});
const cardActions = computed<RecordActionItem[]>(() => {
  if (mode.value !== 'view') {
    return [
      { key: 'cancel', title: '取消', iconName: 'close', disabled: saving.value },
      {
        key: 'save',
        actionCode: mode.value === 'create' ? 'create' : 'update',
        title: saving.value ? '保存中' : '保存',
        iconName: 'save',
        loading: saving.value,
        primary: true,
      },
    ];
  }
  return [
    { key: 'edit', actionCode: 'update', title: '编辑', iconName: 'edit', disabled: !selected.value },
    {
      key: 'delete',
      actionCode: 'delete',
      title: '删除',
      iconName: 'delete',
      disabled: !selected.value,
      loading: saving.value,
      danger: true,
    },
  ];
});
const activeRules = computed(() =>
  [selected.value, mode.value === 'view' ? undefined : draft.value]
    .filter((rule): rule is PasswordPolicyRule => Boolean(rule?.pattern))
    .filter((rule) => rule.enabled !== false),
);
const passwordChecks = computed<PasswordRuleCheck[]>(() =>
  activeRules.value.map((rule) => checkPasswordRule(rule, testPassword.value)),
);
const passwordCheckSummary = computed(() => {
  if (!testPassword.value) {
    return '输入密码后查看当前规则的校验结果';
  }
  if (passwordChecks.value.length === 0) {
    return '当前没有启用的可试算规则';
  }
  return passwordChecks.value.every((item) => item.passed) ? '全部通过' : '存在未通过规则';
});

function handleLoaded(records: CrudRecordListBase[]) {
  const rules = records as PasswordPolicyRule[];
  const matched = selected.value?.id ? rules.find((item) => item.id === selected.value?.id) : undefined;
  if (matched) {
    selected.value = matched;
    if (mode.value === 'view') {
      draft.value = copyRule(matched);
    }
    return;
  }
  const first = rules[0];
  selected.value = first;
  draft.value = first ? copyRule(first) : emptyRuleDraft();
  mode.value = first || !canCreate.value ? 'view' : 'create';
}

function handleSelect(record: CrudRecordListBase) {
  selected.value = record as PasswordPolicyRule;
  draft.value = copyRule(record as PasswordPolicyRule);
  mode.value = 'view';
}

function startCreate() {
  if (!canCreate.value) {
    presentPlatformMessage('当前用户无权新建密码规则', {
      source: 'password-management',
      phase: 'authorization',
    });
    return;
  }
  selected.value = undefined;
  draft.value = emptyRuleDraft();
  mode.value = 'create';
}

function startEdit() {
  if (!selected.value) {
    return;
  }
  draft.value = copyRule(selected.value);
  mode.value = 'edit';
}

function cancelEdit() {
  draft.value = selected.value ? copyRule(selected.value) : emptyRuleDraft();
  mode.value = selected.value ? 'view' : 'create';
}

async function save() {
  if (saving.value || mode.value === 'view') {
    return;
  }
  if (mode.value === 'create' && !canCreate.value) {
    presentPlatformMessage('当前用户无权新建密码规则', {
      source: 'password-management',
      phase: 'authorization',
    });
    return;
  }
  if (mode.value === 'edit' && !canUpdate.value) {
    presentPlatformMessage('当前用户无权编辑密码规则', {
      source: 'password-management',
      phase: 'authorization',
    });
    return;
  }
  const payload = normalizeRuleDraft(draft.value);
  const validationError = validateRule(payload);
  if (validationError) {
    presentPlatformMessage(validationError, { source: 'password-management', phase: 'validation' });
    return;
  }
  saving.value = true;
  try {
    await ruleContext.runtime.ready;
    const result =
      mode.value === 'create'
        ? await ruleContext.abilities.crud().insert(payload)
        : await ruleContext.abilities.crud().update(payload.id!, payload);
    selected.value = result.record;
    draft.value = copyRule(result.record);
    await handlePasswordActionSuccess(result, [
      platformActionResultEffects.closeEditor(),
      platformActionResultEffects.refreshList(),
    ]);
  } catch (cause) {
    presentPlatformError(cause, { source: 'password-management', phase: 'action' });
  } finally {
    saving.value = false;
  }
}

async function toggleEnabled(enabled: boolean) {
  if (mode.value !== 'view') {
    draft.value.enabled = enabled;
    return;
  }
  if (!selected.value?.id || !canEnable.value || saving.value) {
    return;
  }
  saving.value = true;
  try {
    await ruleContext.runtime.ready;
    const result =
      selected.value.enabled === false
        ? await ruleContext.abilities.enable().enable(selected.value.id)
        : await ruleContext.abilities.enable().disable(selected.value.id);
    const refreshed = await ruleContext.abilities.crud().view(selected.value.id);
    selected.value = refreshed;
    draft.value = copyRule(refreshed);
    await handlePasswordActionSuccess(result, [platformActionResultEffects.refreshList()]);
  } catch (cause) {
    presentPlatformError(cause, { source: 'password-management', phase: 'action' });
  } finally {
    saving.value = false;
  }
}

async function removeSelected() {
  if (!selected.value?.id || saving.value) {
    return;
  }
  if (!canDelete.value) {
    presentPlatformMessage('当前用户无权删除密码规则', {
      source: 'password-management',
      phase: 'authorization',
    });
    return;
  }
  const confirmed = await confirmAction({
    title: '删除密码规则',
    content: `确认删除密码规则「${ruleTitle(selected.value)}」？`,
    okText: '删除',
    danger: true,
  });
  if (!confirmed) {
    return;
  }
  saving.value = true;
  try {
    await ruleContext.runtime.ready;
    const result = await ruleContext.abilities.crud().delete(selected.value.id);
    await handlePasswordActionSuccess(result, [
      platformActionResultEffects.clearSelection(),
      platformActionResultEffects.refreshList(),
    ]);
  } catch (cause) {
    presentPlatformError(cause, { source: 'password-management', phase: 'action' });
  } finally {
    saving.value = false;
  }
}

function handleCardAction(action: RecordActionItem) {
  if (action.key === 'edit') {
    startEdit();
    return;
  }
  if (action.key === 'delete') {
    void removeSelected();
    return;
  }
  if (action.key === 'cancel') {
    cancelEdit();
    return;
  }
  if (action.key === 'save') {
    void save();
  }
}

function handlePasswordActionSuccess(result: unknown, defaultEffects: PlatformActionResultEffect[]) {
  return handlePlatformActionSuccess(withPlatformActionResultEffects(result, defaultEffects), {
    source: 'password-management',
    effectHandlers: passwordActionEffectHandlers,
  });
}

function ruleItemOf(record: CrudRecordListBase): RecordExplorerItemDescriptor {
  const rule = record as PasswordPolicyRule;
  return {
    title: ruleTitle(rule),
    secondary: rule.pattern ?? rule.id,
    tag: rule.enabled === false ? '停用' : undefined,
    muted: rule.enabled === false,
  };
}

function matchesRule(record: CrudRecordListBase, keyword: string) {
  const rule = record as PasswordPolicyRule;
  return [rule.title, rule.pattern, rule.message, rule.description, rule.id]
    .filter(Boolean)
    .some((value) => String(value).toLowerCase().includes(keyword));
}

function copyRule(rule: PasswordPolicyRule): PasswordPolicyRule {
  return { ...rule };
}

function emptyRuleDraft(): PasswordPolicyRule {
  return {
    scopeType: 'global',
    title: '',
    pattern: '',
    message: '',
    description: '',
    enabled: true,
    sortOrder: 100,
  };
}

function normalizeRuleDraft(rule: PasswordPolicyRule): PasswordPolicyRule {
  return {
    ...rule,
    scopeType: 'global',
    scopeId: undefined,
    scopeKey: undefined,
    title: rule.title?.trim(),
    pattern: rule.pattern?.trim(),
    message: rule.message?.trim(),
    description: blankToUndefined(rule.description),
    enabled: rule.enabled !== false,
    sortOrder: Number(rule.sortOrder ?? 100),
  };
}

function validateRule(rule: PasswordPolicyRule) {
  if (!rule.title) {
    return '请输入规则名称';
  }
  if (!rule.pattern) {
    return '请输入正则表达式';
  }
  if (!rule.message) {
    return '请输入失败提示';
  }
  try {
    new RegExp(rule.pattern);
  } catch {
    return '正则表达式不合法';
  }
  return undefined;
}

function checkPasswordRule(rule: PasswordPolicyRule, password: string): PasswordRuleCheck {
  try {
    const passed = new RegExp(rule.pattern ?? '').test(password);
    return {
      key: rule.id ?? rule.title ?? rule.pattern ?? '',
      title: ruleTitle(rule),
      message: rule.message ?? '未配置失败提示',
      passed,
    };
  } catch {
    return {
      key: rule.id ?? rule.title ?? rule.pattern ?? '',
      title: ruleTitle(rule),
      message: rule.message ?? '未配置失败提示',
      passed: false,
      error: '正则表达式不合法',
    };
  }
}

function ruleTitle(rule: Partial<PasswordPolicyRule>) {
  return rule.title || rule.id || '未命名规则';
}

function blankToUndefined(value: string | undefined) {
  const normalized = value?.trim();
  return normalized ? normalized : undefined;
}
</script>

<template>
  <StaticManagementLayout
    v-model:sidebar-search-keyword="searchKeyword"
    sidebar-title="密码规则"
    refresh-title="刷新密码规则"
    sidebar-search-placeholder="搜索规则名称、正则或提示"
    :mode="mode"
    :card-title="cardTitle"
    @refresh="reloadKey += 1"
  >
    <template #sidebar-actions>
      <ModuleActionButton
        class="record-panel-create-button"
        :context="ruleContext"
        action-code="create"
        title="新建规则"
        icon-only
        @click="startCreate"
      />
    </template>

    <template #explorer>
      <CrudRecordListExplorer
        :context="ruleContext"
        :selected-id="selected?.id"
        :reload-key="reloadKey"
        :keyword="searchKeyword"
        empty-description="暂无密码规则"
        loading-tip="加载密码规则"
        fallback-title="未命名规则"
        :item-of="ruleItemOf"
        :filter-option="matchesRule"
        @select="handleSelect"
        @loaded="handleLoaded"
      />
    </template>

    <template #card-actions>
      <RecordActionBar :context="ruleContext" :actions="cardActions" @action="handleCardAction" />
    </template>

    <template #card-status>
      <RecordStatusSwitch
        v-if="mode !== 'view'"
        :enabled="draft.enabled"
        :show-label="false"
        @change="toggleEnabled"
      />
      <RecordStatusSwitch
        v-else-if="selected"
        :enabled="selected.enabled"
        :disabled="saving || !canEnable"
        :loading="saving"
        :show-label="false"
        @change="toggleEnabled"
      />
    </template>

    <div class="password-management-grid">
      <form class="static-record-form password-rule-form" @submit.prevent="save">
        <label>
          <span>规则名称</span>
          <UiInput v-model:value="draft.title" :disabled="readonly" />
        </label>
        <label class="wide">
          <span>正则表达式</span>
          <UiInput v-model:value="draft.pattern" :disabled="readonly" />
        </label>
        <label class="wide">
          <span>失败提示</span>
          <UiInput v-model:value="draft.message" :disabled="readonly" />
        </label>
        <label>
          <span>排序号</span>
          <UiInput v-model:value="draft.sortOrder" :disabled="readonly" type="number" />
        </label>
        <label>
          <span>作用范围</span>
          <UiInput value="全局" disabled />
        </label>
        <label class="wide">
          <span>说明</span>
          <textarea v-model="draft.description" :disabled="readonly" rows="4" />
        </label>
      </form>

      <section class="password-test-panel">
        <div class="panel-heading">
          <h3>密码试算</h3>
          <span :class="{ failed: passwordChecks.some((item) => !item.passed) }">
            {{ passwordCheckSummary }}
          </span>
        </div>
        <UiInput v-model:value="testPassword" type="password" placeholder="输入测试密码" />
        <ul class="password-check-list">
          <li
            v-for="item in passwordChecks"
            :key="item.key"
            :class="{ passed: item.passed, failed: !item.passed }"
          >
            <strong>{{ item.title }}</strong>
            <span>{{ item.error ?? (item.passed ? '通过' : item.message) }}</span>
          </li>
        </ul>
      </section>
    </div>

    <RecordMetaSection :record="draft" show-sort-order />
  </StaticManagementLayout>
</template>

<style scoped>
.password-management-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(280px, 360px);
  gap: 16px;
  align-items: start;
}

.password-rule-form .wide {
  grid-column: 1 / -1;
}

.password-rule-form textarea {
  width: 100%;
  min-height: 92px;
  resize: vertical;
}

.password-test-panel {
  display: grid;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--muyun-border);
  border-radius: 8px;
  background: var(--muyun-hover-subtle);
}

.panel-heading {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.panel-heading h3 {
  margin: 0;
  color: var(--muyun-text);
  font-size: 15px;
}

.panel-heading span {
  color: var(--muyun-success-text);
  font-size: 12px;
  font-weight: 700;
}

.panel-heading span.failed {
  color: var(--muyun-danger-text);
}

.password-check-list {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.password-check-list li {
  display: grid;
  gap: 3px;
  padding: 9px 10px;
  border: 1px solid var(--muyun-border);
  border-radius: 6px;
  background: var(--muyun-surface);
}

.password-check-list li.passed {
  border-color: rgba(16, 185, 129, 0.35);
}

.password-check-list li.failed {
  border-color: rgba(239, 68, 68, 0.35);
}

.password-check-list strong {
  color: var(--muyun-text-body);
  font-size: 13px;
}

.password-check-list span {
  color: var(--muyun-text-muted);
  font-size: 12px;
}

@media (max-width: 1000px) {
  .password-management-grid {
    grid-template-columns: 1fr;
  }
}
</style>
