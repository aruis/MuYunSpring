<script setup lang="ts">
import { UiButton, UiInput } from '@muyun/vue-ui-antdv';

defineOptions({ name: 'ChangeOwnPasswordDialog' });

withDefaults(
  defineProps<{
    open?: boolean;
    currentPassword: string;
    newPassword: string;
    confirmPassword: string;
    saving?: boolean;
    error?: string;
  }>(),
  {
    open: false,
    saving: false,
    error: undefined,
  },
);

const emit = defineEmits<{
  close: [];
  submit: [];
  'update:currentPassword': [value: string];
  'update:newPassword': [value: string];
  'update:confirmPassword': [value: string];
}>();
</script>

<template>
  <div v-if="open" class="change-own-password-mask" role="presentation">
    <form class="change-own-password-dialog" role="dialog" aria-modal="true" @submit.prevent="emit('submit')">
      <header>
        <h2>修改密码</h2>
        <button type="button" aria-label="关闭" @click="emit('close')">×</button>
      </header>

      <div class="change-own-password-fields">
        <label>
          <span>当前密码</span>
          <UiInput
            :value="currentPassword"
            type="password"
            autocomplete="current-password"
            :disabled="saving"
            placeholder="请输入当前密码"
            @update:value="emit('update:currentPassword', $event)"
          />
        </label>
        <label>
          <span>新密码</span>
          <UiInput
            :value="newPassword"
            type="password"
            autocomplete="new-password"
            :disabled="saving"
            placeholder="请输入新密码"
            @update:value="emit('update:newPassword', $event)"
          />
        </label>
        <label>
          <span>确认新密码</span>
          <UiInput
            :value="confirmPassword"
            type="password"
            autocomplete="new-password"
            :disabled="saving"
            placeholder="请再次输入新密码"
            @update:value="emit('update:confirmPassword', $event)"
          />
        </label>
      </div>

      <p v-if="error" class="change-own-password-error">{{ error }}</p>

      <footer>
        <UiButton type="default" :disabled="saving" @click="emit('close')">取消</UiButton>
        <UiButton html-type="submit" type="primary" :loading="saving">保存</UiButton>
      </footer>
    </form>
  </div>
</template>

<style scoped>
.change-own-password-mask {
  position: fixed;
  inset: 0;
  z-index: 1200;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.32);
}

.change-own-password-dialog {
  display: grid;
  gap: 16px;
  width: min(420px, 100%);
  padding: 20px;
  border: 1px solid #dbe5f0;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.2);
}

.change-own-password-dialog header,
.change-own-password-dialog footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.change-own-password-dialog h2 {
  margin: 0;
  color: #1f2933;
  font-size: 16px;
}

.change-own-password-dialog header button {
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #64748b;
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
}

.change-own-password-fields {
  display: grid;
  gap: 12px;
}

.change-own-password-fields label {
  display: grid;
  gap: 6px;
  color: #334155;
  font-size: 13px;
}

.change-own-password-error {
  margin: 0;
  color: #b42318;
  font-size: 13px;
}

.change-own-password-dialog footer {
  justify-content: flex-end;
}
</style>
