<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { Workbench, WorkbenchOutlet } from '@muyun/platform-workbench';
import { presentPlatformError, presentPlatformMessage } from '@muyun/platform-components';
import { configureModuleContext, createAuthClient, provideModuleContextConfig } from '@muyun/web-core';
import type { MenuNavigationTarget, MenuRecord, WorkbenchStartupState } from '@muyun/web-contracts';
import {
  clearAuthToken,
  effectiveAuthToken,
  isAuthenticationRequiredError,
  saveAuthToken,
} from './app/authSession';
import { provideCurrentUserContext } from './app/currentUserContext';
import { loadAppWorkbenchStartupState, usesMockStartup } from './app/appWorkbenchStartup';
import { createBackendHttpClient } from './app/backendHttp';
import { businessModuleRoutes, businessRoutePrefixes, isStaticBusinessRoutePage } from './app/businessRoutes';
import ChangeOwnPasswordDialog from './app/ChangeOwnPasswordDialog.vue';
import LoginView from './app/LoginView.vue';
import StaticBusinessRouteOutlet from './app/StaticBusinessRouteOutlet.vue';
import {
  activeTabUrlOf,
  closeMenuTab,
  menuTargetUrl,
  openMenuTab,
  restoreWorkbenchStartupStateFromUrl,
} from './app/workbenchStartup';

const startup = ref<WorkbenchStartupState>();
const currentUser = computed(() => startup.value?.session.currentUser);
const loading = ref(true);
const error = ref<string>();
const activeTabKey = ref<string>();
const loginRequired = ref(false);
const loginLoading = ref(false);
const logoutLoading = ref(false);
const changePasswordOpen = ref(false);
const changePasswordSaving = ref(false);
const changePasswordError = ref<string>();
const currentPassword = ref('');
const newPassword = ref('');
const confirmPassword = ref('');
const businessRouteResolveOptions = { businessRoutePrefixes, businessModuleRoutes };

configureModuleContext({ httpFactory: createBackendHttpClient });
provideModuleContextConfig({ httpFactory: createBackendHttpClient });
provideCurrentUserContext(currentUser);

const authClient = createAuthClient(createBackendHttpClient({ withAuth: false }));

onMounted(async () => {
  if (!usesMockStartup() && !effectiveAuthToken(import.meta.env.VITE_MUYUN_AUTH_TOKEN)) {
    loginRequired.value = true;
    loading.value = false;
    return;
  }
  await loadWorkbench();
});

async function loadWorkbench() {
  loading.value = true;
  error.value = undefined;
  try {
    const startupState = await loadAppWorkbenchStartupState();
    const state = restoreWorkbenchStartupStateFromUrl(
      startupState,
      currentBrowserPath(),
      businessRouteResolveOptions,
    );
    startup.value = state;
    activeTabKey.value = state.activeTabKey;
    loginRequired.value = false;
    syncBrowserUrl(state);
  } catch (cause) {
    if (requiresLogin(cause)) {
      clearAuthToken();
      loginRequired.value = true;
    }
    error.value = cause instanceof Error ? cause.message : 'Workbench startup failed';
  } finally {
    loading.value = false;
  }
}

async function handleAuthenticated(token: string) {
  saveAuthToken(token);
  loginRequired.value = false;
  loginLoading.value = true;
  try {
    await loadWorkbench();
  } finally {
    loginLoading.value = false;
  }
}

async function handleUserCommand(command: string) {
  if (command === 'changePassword') {
    openChangeOwnPasswordDialog();
    return;
  }
  if (command === 'logout') {
    await handleLogout();
  }
}

function openChangeOwnPasswordDialog() {
  currentPassword.value = '';
  newPassword.value = '';
  confirmPassword.value = '';
  changePasswordError.value = undefined;
  changePasswordOpen.value = true;
}

function closeChangeOwnPasswordDialog() {
  if (changePasswordSaving.value) {
    return;
  }
  changePasswordOpen.value = false;
  changePasswordError.value = undefined;
}

async function submitChangeOwnPassword() {
  if (changePasswordSaving.value) {
    return;
  }
  const validationError = validateChangeOwnPassword();
  if (validationError) {
    changePasswordError.value = validationError;
    return;
  }
  const token = effectiveAuthToken(import.meta.env.VITE_MUYUN_AUTH_TOKEN);
  if (!token) {
    changePasswordError.value = '登录已失效，请重新登录';
    return;
  }
  changePasswordSaving.value = true;
  changePasswordError.value = undefined;
  try {
    await authClient.changeOwnPassword(
      {
        currentPassword: currentPassword.value,
        newPassword: newPassword.value,
      },
      token,
    );
    changePasswordOpen.value = false;
    currentPassword.value = '';
    newPassword.value = '';
    confirmPassword.value = '';
    presentPlatformMessage('密码已修改', { source: 'change-own-password', tone: 'success' });
  } catch (cause) {
    const error = presentPlatformError(cause, { source: 'change-own-password-dialog', phase: 'action' });
    changePasswordError.value = error.message;
  } finally {
    changePasswordSaving.value = false;
  }
}

function validateChangeOwnPassword() {
  if (!currentPassword.value.trim()) {
    return '请输入当前密码';
  }
  if (!newPassword.value.trim()) {
    return '请输入新密码';
  }
  if (newPassword.value !== confirmPassword.value) {
    return '两次输入的新密码不一致';
  }
  if (currentPassword.value === newPassword.value) {
    return '新密码不能与当前密码相同';
  }
  return undefined;
}

async function handleLogout() {
  if (logoutLoading.value) {
    return;
  }
  logoutLoading.value = true;
  const token = effectiveAuthToken(import.meta.env.VITE_MUYUN_AUTH_TOKEN);
  try {
    await authClient.logout(token);
  } catch {
    // Local logout should still be possible if the token is already expired or the backend is unavailable.
  } finally {
    clearAuthToken();
    startup.value = undefined;
    activeTabKey.value = undefined;
    error.value = undefined;
    loginRequired.value = true;
    loading.value = false;
    logoutLoading.value = false;
    if (currentBrowserPath() !== '/') {
      window.history.replaceState(window.history.state, '', '/');
    }
  }
}

function handleSelectMenu(menu: MenuRecord, target: MenuNavigationTarget) {
  if (target.openMode === 'window') {
    openWindow(menuTargetUrl(menu, target));
    return;
  }

  const current = startup.value;
  if (!current) {
    return;
  }

  const result = openMenuTab(current.tabs ?? [], menu, target, businessRouteResolveOptions);
  startup.value = {
    ...current,
    tabs: result.tabs,
    activeTabKey: result.activeTabKey,
  };
  activeTabKey.value = result.activeTabKey;
  syncBrowserUrl(startup.value);
}

function openWindow(url: string) {
  window.open(url, '_blank', 'noopener,noreferrer');
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

function syncBrowserUrl(state: WorkbenchStartupState) {
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
</script>

<template>
  <LoginView
    v-if="loginRequired"
    :auth-client="authClient"
    :loading="loginLoading"
    :error="error"
    @authenticated="handleAuthenticated"
  />
  <Workbench
    v-else
    v-model:active-tab-key="activeTabKey"
    :startup="startup"
    :loading="loading"
    :error="error"
    @select-menu="handleSelectMenu"
    @change-tab="handleChangeTab"
    @close-tab="handleCloseTab"
    @user-command="handleUserCommand"
  >
    <template #default="{ pageDescriptor }">
      <StaticBusinessRouteOutlet
        v-if="isStaticBusinessRoutePage(pageDescriptor)"
        :descriptor="pageDescriptor"
      />
      <WorkbenchOutlet v-else :descriptor="pageDescriptor" />
    </template>
  </Workbench>
  <ChangeOwnPasswordDialog
    v-model:current-password="currentPassword"
    v-model:new-password="newPassword"
    v-model:confirm-password="confirmPassword"
    :open="changePasswordOpen"
    :saving="changePasswordSaving"
    :error="changePasswordError"
    @close="closeChangeOwnPasswordDialog"
    @submit="submitChangeOwnPassword"
  />
</template>
