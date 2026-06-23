<script setup lang="ts">
import { computed, ref } from 'vue';
import { OrganizationTree } from '@muyun/platform-components';
import type { Organization } from '@muyun/web-contracts';
import { normalizeError, useModuleContext } from '@muyun/web-core';
import { confirmAction } from '@muyun/vue-ui-antdv';

defineOptions({ name: 'OrganizationManagementView' });

type CardMode = 'view' | 'edit' | 'create';

const organizationContext = useModuleContext<Organization>();
const selected = ref<Organization>();
const draft = ref<Organization>(emptyDraft());
const mode = ref<CardMode>('view');
const reloadKey = ref(0);
const saving = ref(false);
const actionError = ref<string>();
const actionMessage = ref<string>();

const cardTitle = computed(() => {
  if (mode.value === 'create') {
    return draft.value.parentId ? '新建下级机构' : '新建根机构';
  }
  return selected.value?.title ?? '机构详情';
});
const readonly = computed(() => mode.value === 'view');
const canMutate = computed(() => Boolean(selected.value?.id));

function handleTreeLoaded(records: Organization[]) {
  if (selected.value?.id && records.some((item) => item.id === selected.value?.id)) {
    return;
  }
  const first = records[0];
  selected.value = first;
  draft.value = first ? copyRecord(first) : emptyDraft();
  mode.value = first ? 'view' : 'create';
}

function handleSelect(organization: Organization) {
  selected.value = organization;
  draft.value = copyRecord(organization);
  mode.value = 'view';
  clearFeedback();
}

function startCreateRoot() {
  selected.value = undefined;
  draft.value = emptyDraft();
  mode.value = 'create';
  clearFeedback();
}

function startCreateChild() {
  draft.value = emptyDraft(selected.value?.id);
  mode.value = 'create';
  clearFeedback();
}

function startEdit() {
  if (!selected.value) {
    return;
  }
  draft.value = copyRecord(selected.value);
  mode.value = 'edit';
  clearFeedback();
}

function cancelEdit() {
  draft.value = selected.value ? copyRecord(selected.value) : emptyDraft();
  mode.value = selected.value ? 'view' : 'create';
  clearFeedback();
}

async function save() {
  if (mode.value === 'view') {
    return;
  }
  clearFeedback();
  const validDraft = normalizedDraft(draft.value);
  if (!validDraft.title || !validDraft.code) {
    actionError.value = '机构名称和机构编码不能为空';
    return;
  }

  saving.value = true;
  try {
    const saved =
      mode.value === 'create'
        ? await organizationContext.crud.insert(validDraft)
        : await organizationContext.crud.update(requiredId(validDraft), validDraft);
    selected.value = saved;
    draft.value = copyRecord(saved);
    mode.value = 'view';
    actionMessage.value = '已保存';
    reloadKey.value += 1;
  } catch (cause) {
    actionError.value = normalizeError(cause).message;
  } finally {
    saving.value = false;
  }
}

async function toggleEnabled() {
  if (!selected.value?.id) {
    return;
  }
  clearFeedback();
  saving.value = true;
  try {
    if (selected.value.enabled === false) {
      await organizationContext.crud.enable(selected.value.id);
    } else {
      await organizationContext.crud.disable(selected.value.id);
    }
    const refreshed = await organizationContext.crud.view(selected.value.id);
    selected.value = refreshed;
    draft.value = copyRecord(refreshed);
    actionMessage.value = refreshed.enabled === false ? '已停用' : '已启用';
    reloadKey.value += 1;
  } catch (cause) {
    actionError.value = normalizeError(cause).message;
  } finally {
    saving.value = false;
  }
}

async function removeSelected() {
  if (!selected.value?.id) {
    return;
  }
  const confirmed = await confirmAction({
    title: '删除机构',
    content: `确认删除机构「${selected.value.title ?? selected.value.code ?? selected.value.id}」？`,
    okText: '删除',
    danger: true,
  });
  if (!confirmed) {
    return;
  }
  clearFeedback();
  saving.value = true;
  try {
    await organizationContext.crud.delete(selected.value.id);
    selected.value = undefined;
    draft.value = emptyDraft();
    mode.value = 'create';
    actionMessage.value = '已删除';
    reloadKey.value += 1;
  } catch (cause) {
    actionError.value = normalizeError(cause).message;
  } finally {
    saving.value = false;
  }
}

function copyRecord(record: Organization): Organization {
  return { ...record };
}

function emptyDraft(parentId?: string): Organization {
  return {
    parentId,
    enabled: true,
    title: '',
    code: '',
  };
}

function normalizedDraft(record: Organization): Organization {
  return {
    ...record,
    title: record.title?.trim(),
    code: record.code?.trim(),
    parentId: record.parentId?.trim() || undefined,
  };
}

function requiredId(record: Organization) {
  if (!record.id) {
    throw new Error('机构 ID 不能为空');
  }
  return record.id;
}

function clearFeedback() {
  actionError.value = undefined;
  actionMessage.value = undefined;
}
</script>

<template>
  <section class="organization-page">
    <aside class="organization-sidebar">
      <div class="sidebar-header">
        <div>
          <p>组织管理</p>
          <h2>机构树</h2>
        </div>
        <button type="button" class="icon-button" title="刷新机构树" @click="reloadKey += 1">刷新</button>
      </div>
      <div class="sidebar-actions">
        <button type="button" @click="startCreateRoot">新建根机构</button>
        <button type="button" :disabled="!selected" @click="startCreateChild">新建下级</button>
      </div>
      <OrganizationTree
        :context="organizationContext"
        :selected-id="selected?.id"
        :reload-key="reloadKey"
        @select="handleSelect"
        @loaded="handleTreeLoaded"
      />
    </aside>

    <main class="organization-card">
      <header class="card-header">
        <div>
          <p>{{ mode === 'view' ? '查看' : mode === 'edit' ? '编辑' : '新建' }}</p>
          <h2>{{ cardTitle }}</h2>
        </div>
        <div class="card-actions">
          <button v-if="mode === 'view'" type="button" :disabled="!canMutate" @click="startEdit">编辑</button>
          <button v-if="mode === 'view'" type="button" :disabled="!canMutate" @click="startCreateChild">
            新建下级
          </button>
          <button
            v-if="mode === 'view'"
            type="button"
            :disabled="!canMutate || saving"
            @click="toggleEnabled"
          >
            {{ selected?.enabled === false ? '启用' : '停用' }}
          </button>
          <button
            v-if="mode === 'view'"
            type="button"
            class="danger"
            :disabled="!canMutate || saving"
            @click="removeSelected"
          >
            删除
          </button>
          <button v-if="mode !== 'view'" type="button" :disabled="saving" @click="cancelEdit">取消</button>
          <button v-if="mode !== 'view'" type="button" class="primary" :disabled="saving" @click="save">
            {{ saving ? '保存中' : '保存' }}
          </button>
        </div>
      </header>

      <div v-if="actionError" class="message error">{{ actionError }}</div>
      <div v-else-if="actionMessage" class="message success">{{ actionMessage }}</div>

      <form class="record-form" @submit.prevent="save">
        <label>
          <span>机构名称</span>
          <input v-model="draft.title" :readonly="readonly" required />
        </label>
        <label>
          <span>机构编码</span>
          <input v-model="draft.code" :readonly="readonly" required />
        </label>
        <label>
          <span>上级机构 ID</span>
          <input v-model="draft.parentId" :readonly="readonly" placeholder="根机构留空" />
        </label>
        <label>
          <span>启用状态</span>
          <select v-model="draft.enabled" :disabled="readonly">
            <option :value="true">启用</option>
            <option :value="false">停用</option>
          </select>
        </label>
      </form>

      <section class="record-meta">
        <h3>系统信息</h3>
        <dl>
          <div>
            <dt>ID</dt>
            <dd>{{ draft.id ?? '-' }}</dd>
          </div>
          <div>
            <dt>版本</dt>
            <dd>{{ draft.version ?? '-' }}</dd>
          </div>
          <div>
            <dt>创建时间</dt>
            <dd>{{ draft.createdAt ?? '-' }}</dd>
          </div>
          <div>
            <dt>更新时间</dt>
            <dd>{{ draft.updatedAt ?? '-' }}</dd>
          </div>
        </dl>
      </section>
    </main>
  </section>
</template>

<style scoped>
.organization-page {
  display: grid;
  grid-template-columns: minmax(260px, 320px) minmax(0, 1fr);
  gap: 12px;
  min-height: calc(100vh - 116px);
}

.organization-sidebar,
.organization-card {
  min-width: 0;
  border: 1px solid #d8e1ea;
  border-radius: 8px;
  background: #fff;
}

.organization-sidebar {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  gap: 10px;
  padding: 12px;
  overflow: hidden;
}

.sidebar-header,
.card-header,
.card-actions,
.sidebar-actions {
  display: flex;
  align-items: center;
}

.sidebar-header,
.card-header {
  justify-content: space-between;
  gap: 12px;
}

.sidebar-header p,
.card-header p,
h2,
h3 {
  margin: 0;
}

.sidebar-header p,
.card-header p {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

h2 {
  color: #172033;
  font-size: 16px;
}

.sidebar-actions,
.card-actions {
  gap: 8px;
  flex-wrap: wrap;
}

button {
  height: 32px;
  padding: 0 10px;
  border: 1px solid #cfd9e5;
  border-radius: 6px;
  background: #fff;
  color: #243447;
  cursor: pointer;
}

button:disabled {
  color: #9aa7b5;
  cursor: not-allowed;
}

button.primary {
  border-color: #0f766e;
  background: #0f766e;
  color: #fff;
}

button.danger {
  border-color: #f3c6c6;
  color: #b42318;
}

.organization-card {
  display: grid;
  align-content: start;
  gap: 14px;
  padding: 16px;
}

.record-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(220px, 1fr));
  gap: 14px;
}

label {
  display: grid;
  gap: 6px;
  color: #465569;
  font-size: 13px;
}

input,
select {
  width: 100%;
  min-width: 0;
  height: 34px;
  padding: 0 10px;
  border: 1px solid #cfd9e5;
  border-radius: 6px;
  background: #fff;
  color: #172033;
}

input:read-only,
select:disabled {
  background: #f8fafc;
  color: #475569;
}

.message {
  padding: 9px 10px;
  border-radius: 6px;
  font-size: 13px;
}

.message.error {
  border: 1px solid #f4b8b8;
  background: #fff5f5;
  color: #b42318;
}

.message.success {
  border: 1px solid #a9d7c8;
  background: #f1fbf7;
  color: #0f6b57;
}

.record-meta {
  display: grid;
  gap: 10px;
  padding-top: 4px;
}

.record-meta h3 {
  color: #334155;
  font-size: 14px;
}

dl {
  display: grid;
  grid-template-columns: repeat(2, minmax(220px, 1fr));
  gap: 10px 16px;
  margin: 0;
}

dl div {
  min-width: 0;
}

dt {
  color: #64748b;
  font-size: 12px;
}

dd {
  overflow: hidden;
  margin: 3px 0 0;
  color: #243447;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 900px) {
  .organization-page {
    grid-template-columns: 1fr;
  }

  .record-form,
  dl {
    grid-template-columns: 1fr;
  }
}
</style>
