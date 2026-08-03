<script setup lang="ts">
import { computed, defineAsyncComponent, onMounted, ref, watch } from 'vue';
import { presentPlatformError } from '@muyun/platform-components';
import { createBackendHttpClient } from '../app/backendHttp';
import {
  createOpenApiAuthenticatedFetch,
  loadModuleOpenApi,
  openApiBackendBaseUrl,
  type ModuleOpenApiDocument,
} from '../app/moduleOpenApi';

const props = defineProps<{ moduleAlias: string }>();
const emit = defineEmits<{ back: [] }>();
const ApiReference = defineAsyncComponent(async () => {
  const [module] = await Promise.all([
    import('@scalar/api-reference'),
    import('@scalar/api-reference/style.css'),
  ]);
  return module.ApiReference;
});

const document = ref<ModuleOpenApiDocument>();
const loading = ref(false);
const error = ref<string>();
const documentText = computed(() => (document.value ? JSON.stringify(document.value, null, 2) : ''));
const schemaCount = computed(() => Object.keys(document.value?.components?.schemas ?? {}).length);
const scalarConfiguration = computed(() => {
  const backendBaseUrl = openApiBackendBaseUrl();
  return {
    content: document.value,
    title: document.value?.info.title ?? props.moduleAlias,
    baseServerURL: backendBaseUrl,
    servers: backendBaseUrl ? [{ url: backendBaseUrl }] : undefined,
    hideClientButton: false,
    customFetch: createOpenApiAuthenticatedFetch(),
    showDeveloperTools: 'never' as const,
    showToolbar: 'always' as const,
    theme: 'default' as const,
    darkMode: false,
    hideDarkModeToggle: true,
  };
});

onMounted(load);
watch(() => props.moduleAlias, load);

async function load() {
  loading.value = true;
  error.value = undefined;
  try {
    document.value = await loadModuleOpenApi(createBackendHttpClient(), props.moduleAlias);
  } catch (cause) {
    error.value = presentPlatformError(cause, { source: 'module-openapi-view', phase: 'load' }).message;
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <main class="module-openapi-view">
    <header class="module-openapi-view__header">
      <div>
        <p class="module-openapi-view__eyebrow">Module OpenAPI</p>
        <h1>{{ document?.info.title ?? moduleAlias }}</h1>
        <p class="module-openapi-view__alias">{{ moduleAlias }}</p>
      </div>
      <div class="module-openapi-view__actions">
        <button type="button" :disabled="loading" @click="load">刷新</button>
        <button type="button" @click="emit('back')">返回工作台</button>
      </div>
    </header>

    <p v-if="loading" class="module-openapi-view__message">正在读取模块 API 文档…</p>
    <section v-else-if="error" class="module-openapi-view__error" role="alert">
      <p>{{ error }}</p>
      <button type="button" @click="load">重试</button>
    </section>
    <template v-else-if="document">
      <p class="module-openapi-view__summary">
        OpenAPI {{ document.openapi }} · {{ Object.keys(document.paths).length }} 个路径 ·
        {{ schemaCount }} 个模型
      </p>
      <ApiReference class="module-openapi-view__reference" :configuration="scalarConfiguration" />
      <details class="module-openapi-view__raw">
        <summary>查看原始 JSON 文档</summary>
        <pre>{{ documentText }}</pre>
      </details>
    </template>
  </main>
</template>

<style scoped>
.module-openapi-view {
  max-width: 1440px;
  margin: 0 auto;
  padding: 24px;
  color: #1f2937;
}
.module-openapi-view__header,
.module-openapi-view__actions {
  display: flex;
  gap: 12px;
}
.module-openapi-view__header {
  align-items: start;
  justify-content: space-between;
  margin-bottom: 20px;
}
.module-openapi-view__actions {
  flex-wrap: wrap;
}
.module-openapi-view__eyebrow,
h1,
.module-openapi-view__alias,
.module-openapi-view__message,
.module-openapi-view__error p {
  margin: 0;
}
.module-openapi-view__eyebrow,
.module-openapi-view__alias {
  color: #64748b;
  font-size: 14px;
}
h1 {
  margin-top: 4px;
  font-size: 24px;
}
button {
  min-height: 34px;
  padding: 0 14px;
  border: 1px solid #2563eb;
  border-radius: 6px;
  background: #fff;
  color: #1d4ed8;
  cursor: pointer;
}
button:last-child {
  background: #2563eb;
  color: #fff;
}
button:disabled {
  cursor: wait;
  opacity: 0.6;
}
.module-openapi-view__summary,
.module-openapi-view__raw,
.module-openapi-view__error {
  margin-top: 16px;
  padding: 16px;
  border: 1px solid #dbe4ef;
  border-radius: 8px;
  background: #fff;
}
.module-openapi-view__summary {
  color: #475569;
}
.module-openapi-view__reference {
  display: block;
  min-height: 720px;
  margin-top: 16px;
  border: 1px solid #dbe4ef;
  border-radius: 8px;
  overflow: hidden;
}
.module-openapi-view__raw summary {
  cursor: pointer;
  font-weight: 600;
}
pre {
  overflow: auto;
  margin: 16px 0 0;
  padding: 16px;
  border-radius: 6px;
  background: #0f172a;
  color: #e2e8f0;
  font-size: 12px;
  line-height: 1.5;
}
.module-openapi-view__error {
  color: #b91c1c;
}
</style>
