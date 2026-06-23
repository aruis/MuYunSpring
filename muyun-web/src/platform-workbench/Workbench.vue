<script setup lang="ts">
import { computed } from 'vue';
import { UiDropdown, UiError, UiIcon, UiSpin, UiTabs } from '@muyun/vue-ui-antdv';
import type {
  MenuNavigationTarget,
  MenuRecord,
  MenuTab,
  PageDescriptor,
  WorkbenchStartupState,
} from '@muyun/web-contracts';
import type { UiDropdownItem, UiTabItem } from '@muyun/vue-ui-antdv';
import WorkbenchMenu from './WorkbenchMenu.vue';
import { resolvePageDescriptor } from './menuNavigation';

defineOptions({ name: 'Workbench' });

const props = withDefaults(
  defineProps<{
    startup?: WorkbenchStartupState;
    loading?: boolean;
    error?: string;
    activeTabKey?: string;
  }>(),
  {
    loading: false,
    error: undefined,
    startup: undefined,
    activeTabKey: undefined,
  },
);

const emit = defineEmits<{
  selectMenu: [menu: MenuRecord, target: MenuNavigationTarget];
  invalidMenu: [menu: MenuRecord];
  changeTab: [key: string];
  closeTab: [key: string];
  'update:activeTabKey': [key: string];
  userCommand: [key: string];
}>();

const tabs = computed<UiTabItem[]>(() => (props.startup?.tabs ?? []).map(toTabItem));
const activeTabKey = computed(
  () => props.activeTabKey ?? props.startup?.activeTabKey ?? tabs.value[0]?.key ?? '',
);
const activeTab = computed(() => (props.startup?.tabs ?? []).find((tab) => tab.key === activeTabKey.value));
const activePageDescriptor = computed(() => {
  const tab = activeTab.value;
  return (
    tab?.pageDescriptor ?? (tab?.target ? resolvePageDescriptor(tab.target, { title: tab.title }) : undefined)
  );
});
const currentUser = computed(() => props.startup?.session.currentUser);
const userDisplayName = computed(() => currentUser.value?.username ?? currentUser.value?.userId ?? '未登录');
const userInitial = computed(() => userDisplayName.value.trim().slice(0, 1).toUpperCase() || 'M');
const tenantLabel = computed(() => currentUser.value?.tenantId ?? '系统工作区');
const activePageTypeLabel = computed(() => pageTypeLabelOf(activePageDescriptor.value?.pageType));
const activeTargetLabel = computed(() => targetLabelOf(activePageDescriptor.value));
const userMenuItems: UiDropdownItem[] = [
  { key: 'profile', title: '个人信息' },
  { key: 'settings', title: '偏好设置' },
  { key: 'logout', title: '退出登录', danger: true },
];

function toTabItem(tab: MenuTab): UiTabItem {
  return {
    key: tab.key,
    title: tab.title,
    closable: tab.closable,
  };
}

function handleTabChange(key: string) {
  emit('update:activeTabKey', key);
  emit('changeTab', key);
}

function handleUserCommand(key: string) {
  emit('userCommand', key);
}

function handleSelectMenu(menu: MenuRecord, target: MenuNavigationTarget) {
  emit('selectMenu', menu, target);
}

function pageTypeLabelOf(pageType: string | undefined) {
  if (pageType === 'dynamic-module') {
    return '动态模块';
  }
  if (pageType === 'business-route') {
    return '业务页面';
  }
  if (pageType === 'platform-route') {
    return '平台页面';
  }
  if (pageType === 'remote-url') {
    return '在线页面';
  }
  if (pageType === 'external-link') {
    return '外部链接';
  }
  return '工作区';
}

function targetLabelOf(descriptor: PageDescriptor | undefined) {
  if (!descriptor) {
    return '未选择入口';
  }
  if (descriptor.pageType === 'dynamic-module') {
    return descriptor.target.moduleAlias;
  }
  if (descriptor.pageType === 'platform-route' || descriptor.pageType === 'business-route') {
    return descriptor.target.route ?? descriptor.target.routeName ?? descriptor.target.pageKey ?? 'workspace';
  }
  return descriptor.target.url;
}
</script>

<template>
  <main class="workbench">
    <WorkbenchMenu
      :menus="startup?.menus ?? []"
      :selected-menu-id="activeTab?.target?.menuId"
      :tenant-label="tenantLabel"
      @select-menu="handleSelectMenu"
      @invalid-menu="emit('invalidMenu', $event)"
    />

    <section class="app-main">
      <header class="app-topbar">
        <div class="topbar-title">
          <h1>{{ activeTab?.title ?? '控制台' }}</h1>
          <span>{{ activePageTypeLabel }} / {{ activeTargetLabel }}</span>
        </div>

        <div class="topbar-actions" aria-label="全局工具">
          <button class="icon-button wide" type="button" aria-label="搜索">
            <UiIcon name="search" />
            <span>搜索</span>
          </button>
          <button class="icon-button" type="button" aria-label="刷新">
            <UiIcon name="reload" />
          </button>
          <button class="icon-button" type="button" aria-label="通知">
            <UiIcon name="notification" />
          </button>
          <button class="icon-button" type="button" aria-label="设置">
            <UiIcon name="settings" />
          </button>
        </div>

        <UiDropdown v-slot="{ toggle }" :items="userMenuItems" @select="handleUserCommand">
          <button class="user-button" type="button" @click.stop="toggle">
            <span class="avatar">{{ userInitial }}</span>
            <span class="user-meta">
              <strong>{{ userDisplayName }}</strong>
              <small>{{ currentUser?.system ? '系统管理员' : '业务用户' }}</small>
            </span>
            <UiIcon class="user-caret" name="down" />
          </button>
        </UiDropdown>
      </header>

      <div class="tab-strip">
        <UiTabs
          v-if="tabs.length > 0"
          :tabs="tabs"
          :active-key="activeTabKey"
          @update:active-key="handleTabChange"
          @close="emit('closeTab', $event)"
        />
        <div v-else class="empty-tabs">暂无打开页面</div>
      </div>

      <section class="app-content">
        <UiSpin v-if="loading" />
        <UiError v-else-if="error" :message="error" />
        <slot
          v-else
          :active-tab="activeTab"
          :target="activeTab?.target"
          :page-descriptor="activePageDescriptor"
        />
      </section>
    </section>
  </main>
</template>

<style scoped>
.workbench {
  display: grid;
  grid-template-columns: 252px minmax(0, 1fr);
  min-height: 100vh;
  background: #f5f7fa;
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
  gap: 12px;
  min-height: 54px;
  padding: 8px 16px;
  border-bottom: 1px solid #dde5ef;
  background: #fff;
}

.app-topbar h1,
.workbench-eyebrow {
  margin: 0;
}

.topbar-title {
  display: grid;
  min-width: 0;
}

.app-topbar h1 {
  color: #1f2933;
  font-size: 16px;
  line-height: 1.2;
}

.topbar-title span {
  overflow: hidden;
  max-width: 560px;
  margin-top: 3px;
  color: #64748b;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.topbar-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
  margin-left: auto;
}

.icon-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 32px;
  height: 32px;
  border: 1px solid #d6e0ec;
  border-radius: 7px;
  background: #fff;
  color: #334155;
  cursor: pointer;
  transition:
    border-color 160ms ease,
    color 160ms ease,
    box-shadow 160ms ease,
    transform 160ms ease;
}

.icon-button.wide {
  width: auto;
  min-width: 112px;
  padding: 0 10px;
  justify-content: flex-start;
  color: #64748b;
  font-size: 12px;
}

.icon-button:hover {
  border-color: #9cc8c2;
  color: #0f766e;
  box-shadow: 0 8px 18px rgb(15 23 42 / 8%);
  transform: translateY(-1px);
}

.user-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 154px;
  height: 32px;
  padding: 3px 7px 3px 3px;
  border: 1px solid #d6e0ec;
  border-radius: 7px;
  background: #fff;
  color: #1f2933;
  cursor: pointer;
}

.avatar {
  display: inline-grid;
  width: 24px;
  height: 24px;
  place-items: center;
  border-radius: 6px;
  background: #172033;
  color: #fff;
  font-size: 11px;
  font-weight: 800;
}

.user-meta {
  display: grid;
  min-width: 0;
  text-align: left;
}

.user-meta strong,
.user-meta small {
  overflow: hidden;
  max-width: 104px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-meta strong {
  color: #172033;
  font-size: 12px;
}

.user-meta small {
  color: #64748b;
  font-size: 11px;
}

.user-caret {
  margin-left: auto;
  color: #64748b;
  font-size: 11px;
}

.tab-strip {
  min-width: 0;
  padding: 0 12px;
  border-bottom: 1px solid #dde5ef;
  background: #f8fafc;
}

.tab-strip :deep(.ant-tabs) {
  margin: 0;
}

.tab-strip :deep(.ant-tabs-nav) {
  margin: 0;
}

.tab-strip :deep(.ant-tabs-nav::before) {
  display: none;
}

.tab-strip :deep(.ant-tabs-tab) {
  margin: 6px 4px 6px 0 !important;
  padding: 5px 10px !important;
  border: 1px solid #d8e1ea !important;
  border-radius: 6px !important;
  background: #fff !important;
  color: #475569;
  font-size: 12px;
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease;
}

.tab-strip :deep(.ant-tabs-tab-active) {
  border-color: #9cc8c2 !important;
  box-shadow: 0 8px 18px rgb(15 23 42 / 7%);
}

.tab-strip :deep(.ant-tabs-tab-active .ant-tabs-tab-btn) {
  color: #0f766e !important;
  font-weight: 700;
}

.tab-strip :deep(.ant-tabs-nav-add) {
  display: none;
}

.empty-tabs {
  padding: 9px 4px;
  color: #64748b;
  font-size: 12px;
}

.app-content {
  min-width: 0;
  padding: 14px;
  overflow: auto;
}

@media (max-width: 980px) {
  .workbench {
    grid-template-columns: 1fr;
  }

  .app-topbar {
    display: grid;
    grid-template-columns: 1fr;
  }

  .topbar-actions {
    justify-content: flex-start;
    margin-left: 0;
  }

  .user-button {
    width: 100%;
  }
}
</style>
