<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { AdminShell, PageHostOutlet } from '@muyun/platform-shell';
import type { MenuNavigationTarget, MenuRecord, ShellStartupState } from '@muyun/web-contracts';
import { loadAppShellStartupState } from './app/appShellStartup';
import { closeMenuTab, initialOpenMenuKeys, openMenuTab } from './app/shellStartup';

const startup = ref<ShellStartupState>();
const loading = ref(true);
const error = ref<string>();
const activeTabKey = ref<string>();
const openMenuKeys = ref<string[]>([]);

onMounted(async () => {
  try {
    const state = await loadAppShellStartupState();
    startup.value = state;
    activeTabKey.value = state.activeTabKey;
    openMenuKeys.value = initialOpenMenuKeys(state);
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'Shell startup failed';
  } finally {
    loading.value = false;
  }
});

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
}
</script>

<template>
  <AdminShell
    v-model:active-tab-key="activeTabKey"
    v-model:open-menu-keys="openMenuKeys"
    :startup="startup"
    :loading="loading"
    :error="error"
    @select-menu="handleSelectMenu"
    @close-tab="handleCloseTab"
  >
    <template #default="{ pageDescriptor }">
      <PageHostOutlet :descriptor="pageDescriptor" />
    </template>
  </AdminShell>
</template>
