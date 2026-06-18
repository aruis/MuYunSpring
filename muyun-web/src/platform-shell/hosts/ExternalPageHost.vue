<script setup lang="ts">
import type { ExternalLinkPageDescriptor, RemoteUrlPageDescriptor } from '@muyun/web-contracts';
import { computed, onMounted, watch } from 'vue';

defineOptions({ name: 'ExternalPageHost' });

const openedNewWindowUrls = new Set<string>();

const props = defineProps<{
  descriptor: ExternalLinkPageDescriptor | RemoteUrlPageDescriptor;
}>();

const title = computed(() => props.descriptor.title ?? props.descriptor.target.url);
const isNewWindow = computed(() => props.descriptor.openMode === 'new-window');

onMounted(openExternalLinkIfNeeded);
watch(() => props.descriptor.target.url, openExternalLinkIfNeeded);

function openExternalLinkIfNeeded() {
  const url = props.descriptor.target.url;
  if (isNewWindow.value && !openedNewWindowUrls.has(url)) {
    openedNewWindowUrls.add(url);
    window.open(url, '_blank', 'noopener,noreferrer');
  }
}
</script>

<template>
  <iframe
    v-if="descriptor.openMode === 'iframe'"
    class="external-frame"
    :src="descriptor.target.url"
    :title="title"
  />
  <section v-else class="page-host">
    <p class="eyebrow">{{ descriptor.openMode }}</p>
    <h2>{{ title }}</h2>
    <a class="external-link" :href="descriptor.target.url" target="_blank" rel="noopener noreferrer">
      打开页面
    </a>
  </section>
</template>

<style scoped>
.external-frame {
  display: block;
  width: 100%;
  min-height: 520px;
  border: 1px solid #dbe4ef;
  border-radius: 8px;
  background: #fff;
}

.page-host {
  min-height: 240px;
  padding: 24px;
  border: 1px solid #dbe4ef;
  border-radius: 8px;
  background: #fff;
}

.eyebrow {
  margin: 0 0 8px;
  color: #6b7788;
  font-size: 12px;
}

h2 {
  margin: 0;
  color: #1f2933;
  font-size: 18px;
}

.external-link {
  display: inline-flex;
  margin-top: 16px;
  color: #1677ff;
  text-decoration: none;
}
</style>
