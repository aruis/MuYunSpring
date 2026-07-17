<script setup lang="ts">
import { computed } from 'vue';
import { Table as ATable } from 'ant-design-vue';
import { resolveDictionaryOptions } from '../dictionaries';
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

const columns = computed(() =>
  props.contract.columns.map((column) => ({
    title: column.title,
    dataIndex: column.key,
    key: column.key,
    width: column.width,
    customRender: ({ text }: { text: unknown }) => renderCell(column, text),
  })),
);
</script>

<template>
  <ATable
    :columns="columns"
    :data-source="rows"
    :row-key="contract.rowKey ?? 'id'"
    :loading="loading"
    :pagination="pagination ?? { pageSize: 5, showSizeChanger: false }"
    :row-selection="rowSelection"
    :size="size ?? 'middle'"
  />
</template>
