<script setup lang="ts">
import { ref } from 'vue';
import { UiInput } from '@ximatai/muyun-web-app';

interface CustomerRecord {
  id?: string;
  customerName: string;
  industry: string;
  level: string;
}

const record = ref<CustomerRecord>({
  customerName: '业务项目客户',
  industry: 'software',
  level: 'key',
});

const rows = ref<CustomerRecord[]>([
  { id: 'biz-001', customerName: '业务项目客户', industry: 'software', level: 'key' },
]);

const generatedAt = new Intl.DateTimeFormat('zh-CN', {
  dateStyle: 'medium',
  timeStyle: 'short',
  timeZone: 'UTC',
}).format(new Date('2026-08-06T08:00:00Z'));
</script>

<template>
  <section class="business-grid">
    <article class="business-card">
      <h2>业务表单</h2>
      <dl class="customer-details">
        <dt>客户名称</dt>
        <dd><UiInput v-model:value="record.customerName" /></dd>
        <dt>行业</dt>
        <dd>{{ record.industry }}</dd>
        <dt>客户等级</dt>
        <dd>{{ record.level }}</dd>
      </dl>
    </article>

    <article class="business-card">
      <h2>业务列表</h2>
      <p class="generated-at">构建时间：{{ generatedAt }}</p>
      <table>
        <thead>
          <tr>
            <th>客户名称</th>
            <th>行业</th>
            <th>等级</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="String(row.id)">
            <td>{{ row.customerName }}</td>
            <td>{{ row.industry }}</td>
            <td>{{ row.level }}</td>
          </tr>
        </tbody>
      </table>
    </article>
  </section>
</template>
