<script setup lang="ts">
import { computed, ref } from 'vue';
import { UiEmpty, UiIcon } from '@muyun/vue-ui-antdv';
import type { MenuNavigationTarget, MenuRecord, MenuTreeNode } from '@muyun/web-contracts';
import WorkbenchMenuTree from './WorkbenchMenuTree.vue';
import {
  buildWorkbenchMegaMenuModel,
  createWorkbenchMenuNodes,
  filterWorkbenchMenuNodes,
  findWorkbenchMenuNodeById,
  findWorkbenchMenuPath,
  firstDeepRootIdOf,
  type WorkbenchMenuNode,
} from './menuTreeModel';

defineOptions({ name: 'WorkbenchMenu' });

const props = withDefaults(
  defineProps<{
    menus: MenuTreeNode[];
    selectedMenuId?: string;
    tenantLabel?: string;
    searchPlaceholder?: string;
  }>(),
  {
    selectedMenuId: undefined,
    tenantLabel: '系统工作区',
    searchPlaceholder: '搜索菜单、模块或路由',
  },
);

const emit = defineEmits<{
  selectMenu: [menu: MenuRecord, target: MenuNavigationTarget];
  invalidMenu: [menu: MenuRecord];
}>();

const menuFilter = ref('');
const activeRootMenuId = ref<string>();
const activeDeepRootId = ref<string>();
const megaPanelTop = ref(8);

const menuNodes = computed(() => createWorkbenchMenuNodes(props.menus));
const filteredMenus = computed(() => filterWorkbenchMenuNodes(menuNodes.value, menuFilter.value));
const selectedMenuPath = computed(() =>
  props.selectedMenuId ? findWorkbenchMenuPath(menuNodes.value, props.selectedMenuId) : [],
);
const selectedRootMenuId = computed(() => selectedMenuPath.value[0]?.record.id);
const activeRootNode = computed(() =>
  activeRootMenuId.value ? findWorkbenchMenuNodeById(filteredMenus.value, activeRootMenuId.value) : undefined,
);
const megaMenuModel = computed(() =>
  activeRootNode.value
    ? buildWorkbenchMegaMenuModel(activeRootNode.value, activeDeepRootId.value)
    : undefined,
);
const activeDeepRootNode = computed(() => megaMenuModel.value?.activeDeepRoot);

function selectMenuNode(node: WorkbenchMenuNode) {
  if (node.target) {
    emit('selectMenu', node.record, node.target);
  } else {
    emit('invalidMenu', node.record);
  }
}

function handleDeepMenuSelect(menu: MenuRecord, target: MenuNavigationTarget) {
  emit('selectMenu', menu, target);
}

function openRootMenu(node: WorkbenchMenuNode, event?: MouseEvent | FocusEvent) {
  activeRootMenuId.value = node.record.id;
  activeDeepRootId.value = firstDeepRootIdOf(node);
  updateMegaPanelTop(event?.currentTarget);
}

function closeMegaMenu() {
  activeRootMenuId.value = undefined;
  activeDeepRootId.value = undefined;
}

function handleMenuKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    closeMegaMenu();
  }
}

function updateMegaPanelTop(target: EventTarget | null | undefined) {
  if (!(target instanceof HTMLElement)) {
    return;
  }
  const rect = target.getBoundingClientRect();
  const panelHeight = Math.min(window.innerHeight - 16, 620);
  const idealTop = rect.top;
  const maxTop = Math.max(8, window.innerHeight - panelHeight - 8);
  megaPanelTop.value = Math.round(Math.min(Math.max(idealTop, 8), maxTop));
}

function keepDeepRoot(node: WorkbenchMenuNode) {
  activeDeepRootId.value = node.hasChildren ? node.record.id : undefined;
}

function isSelectedRoot(node: WorkbenchMenuNode) {
  return selectedRootMenuId.value === node.record.id;
}
</script>

<template>
  <div class="workbench-menu" @mouseleave="closeMegaMenu" @keydown="handleMenuKeydown">
    <aside class="menu-sidebar">
      <div class="brand-area">
        <div class="brand-mark">
          <UiIcon name="app" />
        </div>
        <div class="brand-copy">
          <strong>MuYun</strong>
          <span>{{ tenantLabel }}</span>
        </div>
      </div>

      <div class="menu-search">
        <UiIcon name="search" />
        <input v-model="menuFilter" type="search" :placeholder="searchPlaceholder" aria-label="搜索菜单" />
      </div>

      <nav class="root-menu" aria-label="主导航">
        <div v-if="filteredMenus.length > 0" class="root-menu-list">
          <button
            v-for="node in filteredMenus"
            :key="node.record.id"
            class="root-menu-item"
            :class="{
              active: activeRootNode?.record.id === node.record.id,
              selected: isSelectedRoot(node),
              navigable: node.navigable,
            }"
            type="button"
            :aria-expanded="activeRootNode?.record.id === node.record.id"
            :aria-controls="activeRootNode?.record.id === node.record.id ? 'workbench-mega-panel' : undefined"
            @mouseenter="openRootMenu(node, $event)"
            @focus="openRootMenu(node, $event)"
            @click="node.navigable && selectMenuNode(node)"
          >
            <span>{{ node.record.title }}</span>
          </button>
        </div>
        <UiEmpty v-else description="暂无菜单" />
      </nav>

      <div class="sidebar-footer">
        <div class="status-dot" />
        <span>平台在线</span>
      </div>
    </aside>

    <section
      v-if="activeRootNode"
      id="workbench-mega-panel"
      class="mega-panel"
      :style="{ '--mega-panel-top': `${megaPanelTop}px` }"
    >
      <div class="mega-body" :class="{ 'has-deep': activeDeepRootNode }">
        <div class="mega-groups">
          <section v-for="group in megaMenuModel?.groups ?? []" :key="group.record.id" class="mega-group">
            <button
              class="mega-group-title"
              :class="{ navigable: group.navigable }"
              type="button"
              :disabled="!group.navigable"
              @click="selectMenuNode(group)"
            >
              <span>{{ group.record.title }}</span>
            </button>

            <div class="mega-entry-list">
              <button
                v-for="entry in group.children"
                :key="entry.record.id"
                class="mega-entry"
                :class="{
                  navigable: entry.navigable,
                  active: activeDeepRootNode?.record.id === entry.record.id,
                  branch: entry.hasChildren,
                }"
                type="button"
                :disabled="!entry.navigable && !entry.hasChildren"
                @mouseenter="keepDeepRoot(entry)"
                @focus="keepDeepRoot(entry)"
                @click="entry.navigable && selectMenuNode(entry)"
              >
                <span>{{ entry.record.title }}</span>
              </button>
            </div>
          </section>
        </div>

        <aside v-if="activeDeepRootNode" class="mega-deep-panel">
          <header>
            <span>深层导航</span>
            <strong>{{ activeDeepRootNode.record.title }}</strong>
          </header>
          <ul class="mega-deep-tree">
            <WorkbenchMenuTree :node="activeDeepRootNode" @select-menu="handleDeepMenuSelect" />
          </ul>
        </aside>
      </div>
    </section>
  </div>
</template>

<style scoped>
.workbench-menu {
  position: relative;
  z-index: 20;
  min-width: 0;
}

.menu-sidebar {
  position: sticky;
  top: 0;
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr) auto;
  gap: 10px;
  height: 100vh;
  min-width: 0;
  padding: 12px 10px;
  border-right: 1px solid #d8e1ea;
  background: #fbfcfe;
}

.brand-area {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  height: 40px;
  padding: 0 6px;
}

.brand-mark {
  display: inline-grid;
  width: 30px;
  height: 30px;
  place-items: center;
  border-radius: 7px;
  background: #172033;
  color: #fff;
}

.brand-copy {
  display: grid;
  min-width: 0;
}

.brand-copy strong {
  color: #172033;
  font-size: 15px;
  line-height: 1.1;
}

.brand-copy span {
  overflow: hidden;
  margin-top: 2px;
  color: #64748b;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.menu-search {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 34px;
  padding: 0 9px;
  border: 1px solid #d8e1ea;
  border-radius: 7px;
  background: #fff;
  color: #64748b;
}

.menu-search input {
  width: 100%;
  min-width: 0;
  border: 0;
  outline: 0;
  background: transparent;
  color: #172033;
  font-size: 12px;
}

.menu-search input::placeholder {
  color: #94a3b8;
}

.root-menu {
  position: relative;
  z-index: 2;
  min-height: 0;
  overflow: auto;
  padding: 2px 0;
}

.root-menu-list {
  display: grid;
  gap: 2px;
}

.root-menu-item,
.mega-group-title,
.mega-entry {
  width: 100%;
  border: 0;
  background: transparent;
  font: inherit;
  text-align: left;
}

.root-menu-item {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 34px;
  padding: 7px 8px;
  border: 1px solid transparent;
  border-radius: 6px;
  color: #334155;
  font-size: 13px;
  cursor: default;
}

.root-menu-item span,
.mega-entry span,
.mega-group-title span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.root-menu-item:hover,
.root-menu-item.active,
.root-menu-item.active.selected {
  background: #edf4f7;
  color: #0f766e;
}

.root-menu-item.selected {
  background: #e4f2ef;
  color: #0f766e;
  font-weight: 700;
}

.root-menu-item.active,
.root-menu-item.active.selected {
  z-index: 2;
  border-color: #d8e1ea;
  border-right-color: transparent;
  border-radius: 6px 0 0 6px;
  background: #fff;
  font-weight: 700;
}

.root-menu-item.active::after {
  position: absolute;
  top: 0;
  right: -12px;
  bottom: 0;
  width: 12px;
  background: #fff;
  content: '';
}

.root-menu-item.navigable {
  cursor: pointer;
}

.sidebar-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 34px;
  padding: 0 9px;
  border: 1px solid #d9e4ee;
  border-radius: 7px;
  background: #fff;
  color: #475569;
  font-size: 11px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #10b981;
  box-shadow: 0 0 0 4px rgb(16 185 129 / 12%);
}

.mega-panel {
  position: absolute;
  z-index: 1;
  top: var(--mega-panel-top);
  left: calc(100% - 11px);
  display: grid;
  grid-template-rows: minmax(0, 1fr);
  width: min(820px, calc(100vw - 286px));
  max-height: calc(100vh - 16px);
  border: 1px solid #d8e1ea;
  border-radius: 0 8px 8px 0;
  background: #fff;
  box-shadow: 0 24px 60px rgb(15 23 42 / 14%);
  clip-path: inset(0 -80px -80px 0);
  overflow: hidden;
}

.mega-deep-panel header span {
  color: #64748b;
  font-size: 11px;
}

.mega-deep-panel header strong {
  overflow: hidden;
  color: #172033;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mega-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  min-height: 0;
}

.mega-body.has-deep {
  grid-template-columns: minmax(0, 1fr) 280px;
}

.mega-groups {
  display: grid;
  grid-template-columns: repeat(3, minmax(150px, 1fr));
  align-content: start;
  gap: 16px 18px;
  min-width: 0;
  max-height: calc(100vh - 16px);
  padding: 14px;
  overflow: auto;
}

.mega-group {
  display: grid;
  align-content: start;
  gap: 6px;
  min-width: 0;
}

.mega-group-title {
  display: flex;
  align-items: center;
  min-height: 24px;
  padding: 2px 0;
  color: #475569;
  font-size: 12px;
  font-weight: 800;
  cursor: default;
}

.mega-group-title.navigable {
  color: #0f766e;
  cursor: pointer;
}

.mega-group-title.navigable:hover {
  text-decoration: underline;
}

.mega-entry-list {
  display: grid;
  gap: 2px;
}

.mega-entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 30px;
  padding: 5px 7px;
  border-radius: 6px;
  color: #64748b;
  font-size: 12px;
  cursor: default;
}

.mega-entry.navigable {
  color: #1e293b;
  cursor: pointer;
}

.mega-entry.branch {
  font-weight: 600;
}

.mega-entry:hover,
.mega-entry.active {
  background: #eef7f4;
  color: #0f766e;
}

.mega-deep-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-width: 0;
  border-left: 1px solid #e2e8f0;
  background: #fbfcfe;
}

.mega-deep-panel header {
  display: grid;
  gap: 2px;
  min-height: 46px;
  padding: 8px 12px;
  border-bottom: 1px solid #e2e8f0;
}

.mega-deep-tree {
  min-height: 0;
  margin: 0;
  padding: 8px;
  overflow: auto;
}

@media (max-width: 980px) {
  .menu-sidebar {
    position: relative;
    height: auto;
    min-height: 0;
    border-right: 0;
    border-bottom: 1px solid #d8e1ea;
  }

  .root-menu {
    max-height: 240px;
  }

  .mega-panel {
    position: relative;
    top: auto;
    left: auto;
    width: 100%;
    max-height: none;
    margin-top: 8px;
    border-left: 1px solid #d8e1ea;
    border-radius: 8px;
    box-shadow: 0 16px 34px rgb(15 23 42 / 10%);
  }

  .mega-body,
  .mega-body.has-deep {
    grid-template-columns: minmax(0, 1fr);
  }

  .mega-groups {
    grid-template-columns: minmax(0, 1fr);
    max-height: none;
  }

  .mega-deep-panel {
    border-top: 1px solid #e2e8f0;
    border-left: 0;
  }
}
</style>
