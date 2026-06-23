<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { Workbench, WorkbenchOutlet } from '@muyun/platform-workbench';
import {
  configureModuleContext,
  createAuthClient,
  ModuleContextProvider,
  provideModuleContextConfig,
} from '@muyun/web-core';
import type {
  MenuNavigationTarget,
  MenuRecord,
  PageDescriptor,
  WorkbenchStartupState,
} from '@muyun/web-contracts';
import {
  clearAuthToken,
  effectiveAuthToken,
  isAuthenticationRequiredError,
  saveAuthToken,
} from './app/authSession';
import { loadAppWorkbenchStartupState, usesMockStartup } from './app/appWorkbenchStartup';
import { createBackendHttpClient } from './app/backendHttp';
import LoginView from './app/LoginView.vue';
import OrganizationManagementView from './views/OrganizationManagementView.vue';
import {
  activeTabUrlOf,
  closeMenuTab,
  menuTargetUrl,
  openMenuTab,
  restoreWorkbenchStartupStateFromUrl,
} from './app/workbenchStartup';

const startup = ref<WorkbenchStartupState>();
const loading = ref(true);
const error = ref<string>();
const activeTabKey = ref<string>();
const loginRequired = ref(false);
const loginLoading = ref(false);
const logoutLoading = ref(false);

configureModuleContext({ httpFactory: createBackendHttpClient });
provideModuleContextConfig({ httpFactory: createBackendHttpClient });

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
    const state = restoreWorkbenchStartupStateFromUrl(startupState, currentBrowserPath(), {
      businessRoutePrefixes: ['/iam'],
    });
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
  if (command === 'logout') {
    await handleLogout();
  }
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
  if (target.openMode === 'WINDOW') {
    openWindow(menuTargetUrl(menu, target));
    return;
  }

  const current = startup.value;
  if (!current) {
    return;
  }

  const result = openMenuTab(current.tabs ?? [], menu, target, { businessRoutePrefixes: ['/iam'] });
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

function isOrganizationRoute(pageDescriptor?: PageDescriptor) {
  return (
    pageDescriptor?.pageType === 'business-route' && pageDescriptor.target.route === '/iam/organizations'
  );
}

function moduleAliasOf(pageDescriptor?: PageDescriptor) {
  if (pageDescriptor?.pageType !== 'business-route') {
    return undefined;
  }
  return (
    pageDescriptor.target.moduleAlias ??
    (isOrganizationRoute(pageDescriptor) ? 'iam.organization' : undefined)
  );
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
      <ModuleContextProvider
        v-if="isOrganizationRoute(pageDescriptor)"
        :module-alias="moduleAliasOf(pageDescriptor)"
      >
        <OrganizationManagementView />
      </ModuleContextProvider>
      <WorkbenchOutlet v-else :descriptor="pageDescriptor" />
    </template>
  </Workbench>
</template>
