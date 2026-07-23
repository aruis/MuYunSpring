<script setup lang="ts">
import {
  DateTimeText,
  RecordDetailFields,
  RecordFormFields,
  RecordMetaSection,
  type RecordDetailDisplayResolver,
  type RecordFormFieldDescriptor,
  type RecordFormFieldFallback,
  type RecordFormFieldValue,
  type RecordFormRecord,
} from '@muyun/platform-components';
import { UiButton, UiError, UiInput, UiSpin } from '@muyun/vue-ui-antdv';
import type { ResetPasswordResponse, UserAccount } from '@muyun/web-contracts';
import type { UserDetailMode } from './userDetailStateModel';

defineOptions({ name: 'UserDetailContent' });

defineProps<{
  mode: UserDetailMode;
  draft: Partial<UserAccount>;
  selectedUser?: UserAccount;
  loading: boolean;
  loadFailed: boolean;
  saving: boolean;
  tenantTitle: string;
  fields: Map<string, RecordFormFieldDescriptor>;
  fallback: Record<string, RecordFormFieldFallback>;
  fieldNames: string[];
  password: string;
  resetPasswordResult?: ResetPasswordResponse;
  displayOf?: RecordDetailDisplayResolver;
  disabledOf?: (fieldName: string) => boolean;
}>();

const emit = defineEmits<{
  retry: [];
  save: [];
  'update:field': [fieldName: string, value: RecordFormFieldValue];
  'update:password': [value: string];
}>();
</script>

<template>
  <UiSpin v-if="loading" class="user-detail-state" tip="加载用户详情" />
  <div v-else-if="loadFailed" class="user-detail-state">
    <UiError title="详情加载失败" message="无法加载用户详情，请重试" />
    <UiButton type="primary" icon-name="reload" @click="emit('retry')">重试</UiButton>
  </div>

  <template v-else-if="mode === 'view' || mode === 'create' || selectedUser">
    <RecordDetailFields
      v-if="mode === 'view'"
      :record="draft as RecordFormRecord"
      :fields="fields"
      :fallback="fallback"
      :display-of="displayOf"
    />
    <div v-if="mode === 'view' && resetPasswordResult?.temporaryPassword" class="user-password-reset-result">
      <span>临时密码</span>
      <UiInput :value="resetPasswordResult.temporaryPassword" disabled />
      <small v-if="resetPasswordResult.expiresAt">
        有效期至 <DateTimeText :value="resetPasswordResult.expiresAt" />
      </small>
    </div>

    <form v-if="mode !== 'view'" class="user-form" @submit.prevent="emit('save')">
      <label>
        <span class="user-form-label">当前租户</span>
        <UiInput :value="tenantTitle" disabled />
      </label>
      <RecordFormFields
        v-if="mode !== 'resetPassword'"
        :record="draft as RecordFormRecord"
        :field-names="fieldNames"
        :fields="fields"
        :fallback="fallback"
        :disabled="saving"
        :disabled-of="disabledOf"
        @update:field="(fieldName, value) => emit('update:field', fieldName, value)"
      />
      <label v-if="mode === 'create' || mode === 'resetPassword'">
        <span class="user-form-label">{{ mode === 'create' ? '初始密码' : '新密码' }}</span>
        <UiInput
          :value="password"
          type="password"
          :disabled="saving"
          placeholder="请输入密码"
          allow-clear
          @update:value="emit('update:password', $event)"
        />
      </label>
    </form>
    <RecordMetaSection v-if="mode !== 'create' && mode !== 'resetPassword'" :record="draft" />
  </template>
</template>

<style scoped>
.user-detail-state,
.user-form,
.user-password-reset-result {
  display: grid;
  gap: 12px;
}

.user-detail-state {
  place-items: center;
  min-height: 180px;
}

.user-form > label {
  display: grid;
  gap: 6px;
  color: var(--muyun-text-muted);
  font-size: 13px;
}

.user-form-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.user-password-reset-result {
  margin: 12px 0;
  padding: 12px;
  border: 1px solid var(--muyun-border);
  border-radius: 8px;
  background: var(--muyun-hover-subtle);
  color: var(--muyun-text-muted);
  font-size: 13px;
}

.user-password-reset-result small {
  color: var(--muyun-text-muted);
}
</style>
