<script setup lang="ts">
import { computed, ref } from 'vue';
import { ActionBar, UiForm, UiTable } from '@muyun/vue-ui-antdv';
import type { ActionContract, DynamicPageDescriptor, RecordData } from '@muyun/web-contracts';
import { resolveDynamicActionReactions } from './actionReactions';

defineOptions({ name: 'DynamicModulePage' });

const props = defineProps<{
  descriptor: DynamicPageDescriptor;
}>();

const record = ref<RecordData>({ ...props.descriptor.initialRecord });
const lastAction = ref<string>('none');

const saveEnvelope = computed(() => ({
  moduleAlias: props.descriptor.moduleAlias,
  values: record.value,
  children: {},
  originContext: {
    source: 'dynamic-runtime-skeleton',
  },
}));

function executeAction(action: ActionContract) {
  const reactions = resolveDynamicActionReactions(action, {
    moduleAlias: props.descriptor.moduleAlias,
    recordId: typeof record.value.id === 'string' ? record.value.id : undefined,
  });
  lastAction.value = `${action.actionCode}:${reactions.map((reaction) => reaction.type).join(',') || 'none'}`;
}
</script>

<template>
  <section class="runtime-page">
    <header class="section-header">
      <div>
        <h2>{{ descriptor.title }}</h2>
        <p>{{ descriptor.moduleAlias }}</p>
      </div>
      <ActionBar :actions="descriptor.actions" @execute="executeAction" />
    </header>

    <div class="runtime-grid">
      <article class="panel">
        <h3>动态表单</h3>
        <UiForm v-model="record" :contract="descriptor.form" submit-text="动态保存" />
      </article>
      <article class="panel">
        <h3>动态列表</h3>
        <UiTable :contract="descriptor.list" :rows="descriptor.records" />
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
