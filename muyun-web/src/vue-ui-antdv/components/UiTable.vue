<script setup lang="ts">
import { computed } from 'vue';
import { resolveDictionaryOptions } from '../dictionaries';
import UiDataTable, { type UiDataTableColumn } from './UiDataTable.vue';
import type { RecordData, TableColumn, TableContract } from '@muyun/web-contracts';
import type { TablePaginationConfig, TableProps } from 'ant-design-vue';

defineOptions({ name: 'UiTable' });

const props = defineProps<{
  contract: TableContract;
  rows: RecordData[];
  loading?: boolean;
  pagination?: false | TablePaginationConfig;
  rowSelection?: TableProps['rowSelection'];
  size?: 'small' | 'middle' | 'large';
}>();

function renderCell(column: TableColumn, value: unknown) {
  if (!column.dictionaryAlias) {
    return String(value ?? '');
  }

  const option = resolveDictionaryOptions(column.dictionaryAlias).find((item) => item.value === value);
  return option?.label ?? String(value ?? '');
}

function contractColumnOf(column: UiDataTableColumn): TableColumn {
  return (
    props.contract.columns.find((item) => item.key === column.key) ?? {
      key: column.key,
      title: column.title,
      width: typeof column.width === 'number' ? column.width : undefined,
    }
  );
}

const columns = computed(() =>
  props.contract.columns.map<UiDataTableColumn>((column) => ({
    title: column.title,
    key: column.key,
    dataIndex: column.key,
    width: column.width,
  })),
);
</script>

<template>
  <UiDataTable
    :columns="columns"
    :rows="rows"
    :row-key="contract.rowKey ?? 'id'"
    :loading="loading"
    :pagination="pagination ?? { pageSize: 5, showSizeChanger: false }"
    :row-selection="rowSelection"
    :size="size ?? 'middle'"
  >
    <template #cell="{ column, value }">
      {{ renderCell(contractColumnOf(column), value) }}
    </template>
  </UiDataTable>
</template>
