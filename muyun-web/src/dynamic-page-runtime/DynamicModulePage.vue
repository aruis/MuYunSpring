<script setup lang="ts">
import { computed, ref } from 'vue';
import { MuyunActionBar, MuyunForm, MuyunTable } from '@muyun/vue-ui-antdv';
import type { MuyunActionContract, MuyunDynamicPageDescriptor, MuyunRecord } from '@muyun/web-contracts';

defineOptions({ name: 'DynamicModulePage' });

const props = defineProps<{
  descriptor: MuyunDynamicPageDescriptor;
}>();

const record = ref<MuyunRecord>({ ...props.descriptor.initialRecord });
const lastAction = ref<string>('none');

const saveEnvelope = computed(() => ({
  moduleAlias: props.descriptor.moduleAlias,
  values: record.value,
  children: {},
  originContext: {
    source: 'dynamic-runtime-skeleton',
  },
}));

function executeAction(action: MuyunActionContract) {
  lastAction.value = `${action.actionCode}:${action.refresh ?? 'none'}`;
}
</script>

<template>
  <section class="runtime-page">
    <header class="section-header">
      <div>
        <h2>{{ descriptor.title }}</h2>
        <p>{{ descriptor.moduleAlias }}</p>
      </div>
      <MuyunActionBar :actions="descriptor.actions" @execute="executeAction" />
    </header>

    <div class="runtime-grid">
      <article class="panel">
        <h3>动态表单</h3>
        <MuyunForm v-model="record" :contract="descriptor.form" submit-text="动态保存" />
      </article>
      <article class="panel">
        <h3>动态列表</h3>
        <MuyunTable :contract="descriptor.list" :rows="descriptor.records" />
      </article>
    </div>

    <article class="panel">
      <h3>运行态输出</h3>
      <p>
        last action: <code>{{ lastAction }}</code>
      </p>
      <pre>{{ JSON.stringify(saveEnvelope, null, 2) }}</pre>
    </article>
  </section>
</template>
