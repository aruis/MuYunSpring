<script setup lang="ts">
import { ref } from 'vue';
import type { AuthClient } from '@muyun/web-core';
import { normalizeInitialValue, resolveLoginTenantDefaults } from './loginTenant';

defineOptions({ name: 'LoginView' });

const props = defineProps<{
  authClient: AuthClient;
  loading?: boolean;
  error?: string;
}>();

const emit = defineEmits<{
  authenticated: [token: string];
}>();

const loginTenantDefaults = resolveLoginTenantDefaults(import.meta.env.VITE_MUYUN_LOGIN_TENANT_ID);
const tenantId = ref(loginTenantDefaults.tenantId);
const tenantLocked = loginTenantDefaults.tenantLocked;
const username = ref(normalizeInitialValue(import.meta.env.VITE_MUYUN_LOGIN_USERNAME));
const password = ref(normalizeInitialValue(import.meta.env.VITE_MUYUN_LOGIN_PASSWORD));
const submitting = ref(false);
const formError = ref<string>();

async function submit() {
  formError.value = undefined;
  submitting.value = true;
  try {
    const result = await props.authClient.login({
      tenantId: tenantId.value,
      username: username.value,
      password: password.value,
    });
    emit('authenticated', result.token);
  } catch (cause) {
    formError.value = cause instanceof Error ? cause.message : 'Login failed';
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-panel">
      <header>
        <p>MuYun Platform</p>
        <h1>平台登录</h1>
      </header>

      <p v-if="formError || error" class="login-error">
        {{ formError || error }}
      </p>

      <form class="login-form" @submit.prevent="submit">
        <p v-if="tenantLocked" class="login-context">租户：{{ tenantId }}</p>
        <label v-else>
          <span>租户 ID</span>
          <input v-model="tenantId" autocomplete="organization" placeholder="留空进入系统工作区" />
        </label>
        <label>
          <span>用户名</span>
          <input v-model="username" autocomplete="username" required />
        </label>
        <label>
          <span>密码</span>
          <input v-model="password" type="password" autocomplete="current-password" required />
        </label>
        <button type="submit" :disabled="submitting || loading">
          {{ submitting || loading ? '登录中' : '登录' }}
        </button>
      </form>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  display: grid;
  min-height: 100vh;
  place-items: center;
  padding: 24px;
  background: #eef3f8;
}

.login-panel {
  display: grid;
  width: min(100%, 380px);
  gap: 18px;
  padding: 28px;
  border: 1px solid #d8e2ee;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 16px 48px rgb(25 39 52 / 0.12);
}

header p {
  margin: 0 0 8px;
  color: #526579;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
  text-transform: uppercase;
}

header h1 {
  margin: 0;
  color: #172331;
  font-size: 22px;
  line-height: 1.25;
}

.login-error {
  margin: 0;
  padding: 10px 12px;
  border: 1px solid #ffccc7;
  border-radius: 6px;
  color: #a8071a;
  background: #fff2f0;
  font-size: 13px;
}

.login-form {
  display: grid;
  gap: 14px;
}

.login-context {
  margin: 0;
  padding: 10px 12px;
  border: 1px solid #d8e2ee;
  border-radius: 6px;
  color: #334155;
  background: #f8fafc;
  font-size: 13px;
  font-weight: 600;
}

label {
  display: grid;
  gap: 6px;
  color: #334155;
  font-size: 13px;
  font-weight: 600;
}

input {
  width: 100%;
  min-height: 38px;
  padding: 8px 10px;
  border: 1px solid #cbd6e2;
  border-radius: 6px;
  color: #172331;
  background: #fff;
}

input:focus {
  border-color: #1677ff;
  outline: 2px solid rgb(22 119 255 / 0.16);
}

button {
  min-height: 40px;
  border: 0;
  border-radius: 6px;
  color: #fff;
  background: #1677ff;
  cursor: pointer;
}

button:disabled {
  background: #8bbcff;
  cursor: not-allowed;
}
</style>
