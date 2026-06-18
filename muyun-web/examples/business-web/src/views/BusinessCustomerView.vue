<script setup lang="ts">
import { ref } from 'vue';
import { MuyunActionBar, MuyunForm, MuyunTable } from '@muyun/vue-ui-antdv';
import type {
  MuyunActionContract,
  MuyunFormContract,
  MuyunRecord,
  MuyunTableContract,
} from '@muyun/web-contracts';

const formContract: MuyunFormContract = {
  fields: [
    { name: 'customerName', label: '客户名称', kind: 'input', required: true },
    { name: 'industry', label: '行业', kind: 'dictionary-select', dictionaryAlias: 'crm.industry' },
    { name: 'level', label: '客户等级', kind: 'dictionary-select', dictionaryAlias: 'crm.customer.level' },
  ],
};

const tableContract: MuyunTableContract = {
  rowKey: 'id',
  columns: [
    { key: 'customerName', title: '客户名称' },
    { key: 'industry', title: '行业', dictionaryAlias: 'crm.industry' },
    { key: 'level', title: '等级', dictionaryAlias: 'crm.customer.level' },
  ],
};

const actions: MuyunActionContract[] = [
  { actionCode: 'save', title: '保存', level: 'primary', refresh: 'record' },
  { actionCode: 'submit', title: '提交审批', refresh: 'all' },
];

const record = ref<MuyunRecord>({
  customerName: '业务项目客户',
  industry: 'software',
  level: 'key',
});

const rows = ref<MuyunRecord[]>([
  { id: 'biz-001', customerName: '业务项目客户', industry: 'software', level: 'key' },
]);
</script>

<template>
  <section class="business-grid">
    <article class="business-card">
      <h2>业务表单</h2>
      <MuyunForm v-model="record" :contract="formContract" submit-text="业务保存" />
      <MuyunActionBar :actions="actions" />
    </article>

    <article class="business-card">
      <h2>业务列表</h2>
      <MuyunTable :contract="tableContract" :rows="rows" />
    </article>
  </section>
</template>
