<script setup lang="ts">
import { computed, ref } from 'vue';
import {
  RecordExplorerPanel,
  RecordQueryListPanel,
  TreeRecordExplorer,
  type QueryListRecord,
  type RecordQueryListColumn,
} from '@muyun/platform-components';
import type { Employee, Organization } from '@muyun/web-contracts';
import { useModuleContext, type ModuleContext } from '@muyun/web-core';

defineOptions({ name: 'EmployeeManagementView' });

const organizationContext = useModuleContext<Organization>({ moduleAlias: 'iam.organization' });
const employeeContext = useModuleContext<Employee>({ moduleAlias: 'iam.employee' });
const organizationSearchKeyword = ref('');
const organizationReloadKey = ref(0);
const selectedOrganization = ref<Organization>();
const selectedEmployeeKey = ref<string>();

const employeeListContext = computed(() => employeeContext as unknown as ModuleContext<QueryListRecord>);
const employeeExternalQueryValues = computed<Record<string, unknown> | undefined>(() => {
  const organizationId = selectedOrganization.value?.id;
  if (!organizationId) {
    return undefined;
  }
  return {
    departmentScope: {
      organizationId,
      includeChildren: true,
    },
  };
});

const employeeColumns: RecordQueryListColumn[] = [
  { key: 'employeeNo', title: '职员编号', width: '150px' },
  { key: 'title', title: '职员姓名', width: '150px' },
  { key: 'mobile', title: '手机号', width: '150px' },
  { key: 'email', title: '邮箱' },
  {
    key: 'enabled',
    title: '状态',
    width: '90px',
    align: 'center',
    render: (record) => (record.enabled === false ? '停用' : '启用'),
  },
];

function handleOrganizationsLoaded(records: Organization[]) {
  if (!selectedOrganization.value && records.length > 0) {
    selectedOrganization.value = records[0];
  }
}

function selectOrganization(record: Organization) {
  selectedOrganization.value = record;
  selectedEmployeeKey.value = undefined;
}

function refreshOrganizations() {
  organizationReloadKey.value += 1;
}

function selectEmployee(record: QueryListRecord) {
  selectedEmployeeKey.value = String(record.id ?? '');
}
</script>

<template>
  <section class="employee-management-page">
    <RecordExplorerPanel
      class="employee-scope-panel"
      title="机构树"
      refresh-title="刷新机构树"
      :search-keyword="organizationSearchKeyword"
      search-placeholder="搜索机构名称、编码或 ID"
      @refresh="refreshOrganizations"
      @update:search-keyword="organizationSearchKeyword = $event"
    >
      <TreeRecordExplorer
        :context="organizationContext"
        :selected-id="selectedOrganization?.id"
        :reload-key="organizationReloadKey"
        :keyword="organizationSearchKeyword"
        search-mode="none"
        search-trigger="external"
        empty-description="暂无机构"
        loading-tip="加载机构树"
        fallback-title="未命名机构"
        @loaded="handleOrganizationsLoaded"
        @select="selectOrganization"
      />
    </RecordExplorerPanel>

    <RecordQueryListPanel
      class="employee-list-panel"
      :context="employeeListContext"
      title="职员列表"
      :columns="employeeColumns"
      :selected-key="selectedEmployeeKey"
      :ready="Boolean(selectedOrganization?.id)"
      :external-query-values="employeeExternalQueryValues"
      quick-search-placeholder="搜索编号、姓名、手机号或邮箱"
      empty-description="当前机构暂无职员"
      waiting-description="请选择机构"
      @select="selectEmployee"
    />
  </section>
</template>

<style scoped>
.employee-management-page {
  display: grid;
  grid-template-columns: minmax(260px, 320px) minmax(0, 1fr);
  gap: 12px;
  min-height: calc(100vh - 116px);
}

.employee-scope-panel,
.employee-list-panel {
  min-width: 0;
  min-height: 0;
}

@media (max-width: 900px) {
  .employee-management-page {
    grid-template-columns: 1fr;
  }
}
</style>
