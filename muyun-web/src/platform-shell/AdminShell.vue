<script setup lang="ts">
import { computed } from 'vue';
import { UiDropdown, UiEmpty, UiError, UiMenu, UiSpin, UiTabs } from '@muyun/vue-ui-antdv';
import type {
  MenuNavigationTarget,
  MenuRecord,
  MenuTab,
  MenuTreeNode,
  ShellStartupState,
} from '@muyun/web-contracts';
import type { UiDropdownItem, UiMenuItem, UiTabItem } from '@muyun/vue-ui-antdv';
import { getMenuNavigationTarget } from './menuNavigation';

defineOptions({ name: 'AdminShell' });

const props = withDefaults(
  defineProps<{
    startup?: ShellStartupState;
    loading?: boolean;
    error?: string;
    activeTabKey?: string;
    openMenuKeys?: string[];
  }>(),
  {
    loading: false,
    error: undefined,
    startup: undefined,
    activeTabKey: undefined,
    openMenuKeys: () => [],
  },
);

const emit = defineEmits<{
  selectMenu: [menu: MenuRecord, target: MenuNavigationTarget];
  invalidMenu: [menu: MenuRecord];
  changeTab: [key: string];
  closeTab: [key: string];
  'update:activeTabKey': [key: string];
  'update:openMenuKeys': [keys: string[]];
  userCommand: [key: string];
}>();

const menuItems = computed(() => (props.startup?.menus ?? []).map(toMenuItem));
const menuRecords = computed(() => mapMenuRecords(props.startup?.menus ?? []));
const tabs = computed<UiTabItem[]>(() => (props.startup?.tabs ?? []).map(toTabItem));
const activeTabKey = computed(
  () => props.activeTabKey ?? props.startup?.activeTabKey ?? tabs.value[0]?.key ?? '',
);
const selectedMenuKey = computed(() => activeTab.value?.target.menuId);
const activeTab = computed(() => (props.startup?.tabs ?? []).find((tab) => tab.key === activeTabKey.value));

const userMenuItems: UiDropdownItem[] = [
  { key: 'profile', title: '个人信息' },
  { key: 'logout', title: '退出登录', danger: true },
];

function toMenuItem(node: MenuTreeNode): UiMenuItem {
  const hasChildren = node.children.length > 0;
  return {
    key: node.record.id,
    title: node.record.title,
    disabled: node.record.enabled === false || (!hasChildren && !getMenuNavigationTarget(node.record)),
    children: node.children.map(toMenuItem),
  };
}

function toTabItem(tab: MenuTab): UiTabItem {
  return {
    key: tab.key,
    title: tab.title,
    closable: tab.closable,
  };
}

function mapMenuRecords(nodes: MenuTreeNode[]) {
  const records = new Map<string, MenuRecord>();
  const visit = (node: MenuTreeNode) => {
    records.set(node.record.id, node.record);
    node.children.forEach(visit);
  };
  nodes.forEach(visit);
  return records;
}

function handleMenuSelect(menuId: string) {
  const menu = menuRecords.value.get(menuId);
  const target = menu ? getMenuNavigationTarget(menu) : undefined;
  if (menu && target) {
    emit('selectMenu', menu, target);
  } else if (menu) {
    emit('invalidMenu', menu);
  }
}

function handleTabChange(key: string) {
  emit('update:activeTabKey', key);
  emit('changeTab', key);
}
</script>

<template>
  <main class="app-shell">
    <aside class="app-sidebar">
      <div class="brand">MuYun</div>
      <UiMenu
        v-if="menuItems.length > 0"
        :items="menuItems"
        :selected-key="selectedMenuKey"
        :open-keys="openMenuKeys"
        @select="handleMenuSelect"
        @update:open-keys="emit('update:openMenuKeys', $event)"
      />
      <UiEmpty v-else description="暂无菜单" />
    </aside>

    <section class="app-main">
      <header class="app-topbar">
        <div>
          <p class="shell-eyebrow">Workspace</p>
          <h1>{{ activeTab?.title ?? '控制台' }}</h1>
        </div>
        <UiDropdown :items="userMenuItems" @select="emit('userCommand', $event)">
          <button class="user-button" type="button">
            {{ startup?.session.currentUser.username ?? startup?.session.currentUser.userId ?? '未登录' }}
          </button>
        </UiDropdown>
      </header>

      <UiTabs
        v-if="tabs.length > 0"
        :tabs="tabs"
        :active-key="activeTabKey"
        @update:active-key="handleTabChange"
        @close="emit('closeTab', $event)"
      />

      <section class="app-content">
        <UiSpin v-if="loading" />
        <UiError v-else-if="error" :message="error" />
        <slot v-else :active-tab="activeTab" :target="activeTab?.target" />
      </section>
    </section>
  </main>
</template>

<style scoped>
.app-shell {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  min-height: 100vh;
  background: #f4f7fb;
}

.app-sidebar {
  min-width: 0;
  padding: 20px 12px;
  border-right: 1px solid #dde5ef;
  background: #102033;
}

.brand {
  padding: 4px 12px 20px;
  color: #f8fafc;
  font-size: 20px;
  font-weight: 700;
}

.app-main {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  min-width: 0;
}

.app-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 72px;
  padding: 14px 24px;
  border-bottom: 1px solid #dde5ef;
  background: #fff;
}

.app-topbar h1,
.shell-eyebrow {
  margin: 0;
}

.app-topbar h1 {
  color: #1f2933;
  font-size: 18px;
}

.shell-eyebrow {
  color: #6b7788;
  font-size: 12px;
}

.user-button {
  padding: 8px 12px;
  border: 1px solid #d6e0ec;
  border-radius: 6px;
  background: #fff;
  color: #1f2933;
  cursor: pointer;
}

.app-content {
  min-width: 0;
  padding: 24px;
  overflow: auto;
}

@media (max-width: 980px) {
  .app-shell {
    grid-template-columns: 1fr;
  }

  .app-sidebar {
    border-right: 0;
  }
}
</style>
