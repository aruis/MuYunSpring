<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { AdminShell, PageHostOutlet } from '@muyun/platform-shell';
import { createAuthClient, createHttpClient } from '@muyun/web-core';
import type { MenuNavigationTarget, MenuRecord, ShellStartupState } from '@muyun/web-contracts';
import { clearAuthToken, effectiveAuthToken, isAuthenticationRequiredError, saveAuthToken } from './app/authSession';
import { loadAppShellStartupState, usesMockStartup } from './app/appShellStartup';
import LoginView from './app/LoginView.vue';
import {
  activeTabUrlOf,
  closeMenuTab,
  initialOpenMenuKeys,
  openMenuTab,
  restoreShellStartupStateFromUrl,
} from './app/shellStartup';

const startup = ref<ShellStartupState>();
const loading = ref(true);
const error = ref<string>();
const activeTabKey = ref<string>();
const openMenuKeys = ref<string[]>([]);
const loginRequired = ref(false);
const loginLoading = ref(false);

const authClient = createAuthClient(
  createHttpClient({
    baseUrl: import.meta.env.VITE_MUYUN_API_BASE_URL,
    credentials: credentialsOf(import.meta.env.VITE_MUYUN_CREDENTIALS),
  }),
);

onMounted(async () => {
  if (!usesMockStartup() && !effectiveAuthToken(import.meta.env.VITE_MUYUN_AUTH_TOKEN)) {
    loginRequired.value = true;
    loading.value = false;
    return;
  }
  await loadShell();
});

async function loadShell() {
  loading.value = true;
  error.value = undefined;
  try {
    const startupState = await loadAppShellStartupState();
    const state = restoreShellStartupStateFromUrl(startupState, currentBrowserPath());
    startup.value = state;
    activeTabKey.value = state.activeTabKey;
    openMenuKeys.value = initialOpenMenuKeys(state);
    loginRequired.value = false;
    syncBrowserUrl(state);
  } catch (cause) {
    if (requiresLogin(cause)) {
      clearAuthToken();
      loginRequired.value = true;
    }
    error.value = cause instanceof Error ? cause.message : 'Shell startup failed';
  } finally {
    loading.value = false;
  }
}

async function handleAuthenticated(token: string) {
  saveAuthToken(token);
  loginRequired.value = false;
  loginLoading.value = true;
  try {
    await loadShell();
  } finally {
    loginLoading.value = false;
  }
}

function handleSelectMenu(menu: MenuRecord, target: MenuNavigationTarget) {
  const current = startup.value;
  if (!current) {
    return;
  }

  const result = openMenuTab(current.tabs ?? [], menu, target);
  startup.value = {
    ...current,
    tabs: result.tabs,
    activeTabKey: result.activeTabKey,
  };
  activeTabKey.value = result.activeTabKey;
  syncBrowserUrl(startup.value);
}

function handleCloseTab(key: string) {
  const current = startup.value;
  if (!current) {
    return;
  }

  const result = closeMenuTab(current.tabs ?? [], activeTabKey.value, key);
  startup.value = {
    ...current,
    tabs: result.tabs,
    activeTabKey: result.activeTabKey,
  };
  activeTabKey.value = result.activeTabKey;
  syncBrowserUrl(startup.value);
}

function handleChangeTab(key: string) {
  activeTabKey.value = key;
  const current = startup.value;
  if (!current) {
    return;
  }

  startup.value = {
    ...current,
    activeTabKey: key,
  };
  syncBrowserUrl(startup.value);
}

function currentBrowserPath() {
  return `${window.location.pathname}${window.location.search}`;
}

function syncBrowserUrl(state: ShellStartupState) {
  const url = activeTabUrlOf(state) ?? '/';
  if (url === currentBrowserPath()) {
    return;
  }

  window.history.replaceState(window.history.state, '', url);
}

function requiresLogin(cause: unknown) {
  if (usesMockStartup()) {
    return false;
  }
  return isAuthenticationRequiredError(cause);
}

function credentialsOf(value: string | undefined) {
  return value === 'include' || value === 'omit' || value === 'same-origin' ? value : undefined;
}
</script>

<template>
  <LoginView
    v-if="loginRequired"
    :auth-client="authClient"
    :loading="loginLoading"
    :error="error"
    @authenticated="handleAuthenticated"
  />
  <AdminShell
    v-else
    v-model:active-tab-key="activeTabKey"
    v-model:open-menu-keys="openMenuKeys"
    :startup="startup"
    :loading="loading"
    :error="error"
    @select-menu="handleSelectMenu"
    @change-tab="handleChangeTab"
    @close-tab="handleCloseTab"
  >
    <template #default="{ pageDescriptor }">
      <PageHostOutlet :descriptor="pageDescriptor" />
    </template>
  </AdminShell>
</template>
