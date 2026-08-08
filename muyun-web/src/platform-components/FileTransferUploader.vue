<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { UiButton } from '@muyun/vue-ui-antdv';
import {
  performBrowserFileTransferUpload,
  type FileTransferUploadAccess,
  type FileTransferUploadReceipt,
  type FileTransferUploadTask,
} from './fileTransferUpload';
import { presentPlatformError, presentPlatformSuccess } from './platformErrorFeedback';

defineOptions({ name: 'FileTransferUploader' });

type UploadState = 'ready' | 'requesting' | 'uploading' | 'confirming' | 'completed' | 'failed' | 'cancelled';

interface UploadItem {
  id: number;
  file: File;
  state: UploadState;
  progress: number;
  error?: string;
  task?: FileTransferUploadTask;
}

const props = withDefaults(
  defineProps<{
    /** Business API callback: authorize this exact file and return a short-lived upload target. */
    requestUploadAccess: (file: File) => Promise<FileTransferUploadAccess>;
    /** Business API callback: persist/confirm the uploaded storage result. */
    confirmUpload: (receipt: FileTransferUploadReceipt) => Promise<unknown>;
    accept?: string;
    multiple?: boolean;
    disabled?: boolean;
    maxFiles?: number;
    autoUpload?: boolean;
    uploadText?: string;
    disabledHint?: string;
    completionHint?: string;
  }>(),
  {
    accept: undefined,
    multiple: false,
    disabled: false,
    maxFiles: undefined,
    autoUpload: true,
    uploadText: '选择文件上传',
    disabledHint: undefined,
    completionHint: undefined,
  },
);

const emit = defineEmits<{
  completed: [receipt: FileTransferUploadReceipt, result: unknown];
  failed: [file: File, error: unknown];
  changed: [files: readonly File[]];
}>();

const input = ref<HTMLInputElement>();
const items = ref<UploadItem[]>([]);
const dragging = ref(false);
let nextId = 1;

const active = computed(() =>
  items.value.some((item) => ['requesting', 'uploading', 'confirming'].includes(item.state)),
);

function chooseFiles() {
  if (props.disabled || active.value) return;
  input.value?.click();
}

function selectFiles(event: Event) {
  addFiles(Array.from((event.target as HTMLInputElement).files ?? []));
  // Selecting the same file again must still produce a change event.
  (event.target as HTMLInputElement).value = '';
}

function addFiles(selected: readonly File[]) {
  if (props.disabled || active.value) return;
  const capacity =
    props.maxFiles === undefined ? selected.length : Math.max(0, props.maxFiles - items.value.length);
  const accepted = selected.slice(0, props.multiple ? capacity : Math.min(1, capacity));
  if (accepted.length) {
    // `upload()` mutates the item throughout its lifecycle. Keep the very same
    // reactive instance both in the rendered list and in that async workflow;
    // otherwise Vue cannot observe mutations made through the pre-insertion raw
    // object and the UI can remain stuck at its first state.
    const additions = accepted.map((file) =>
      reactive<UploadItem>({ id: nextId++, file, state: 'ready', progress: 0 }),
    );
    items.value.push(...additions);
    emit(
      'changed',
      items.value.map((item) => item.file),
    );
    if (props.autoUpload) {
      additions.forEach((item) => void upload(item));
    }
  }
}

function dragOver(event: DragEvent) {
  event.preventDefault();
  if (!props.disabled && !active.value) dragging.value = true;
}

function dragLeave(event: DragEvent) {
  // Moving over a child of the drop zone also emits dragleave; only reset when leaving it.
  const dropZone = event.currentTarget as HTMLElement;
  if (!dropZone.contains(event.relatedTarget as Node | null)) dragging.value = false;
}

function dropFiles(event: DragEvent) {
  event.preventDefault();
  dragging.value = false;
  addFiles(Array.from(event.dataTransfer?.files ?? []));
}

function handleDropZoneKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault();
    chooseFiles();
  }
}

async function upload(item: UploadItem) {
  if (props.disabled) {
    return;
  }
  item.error = undefined;
  try {
    const { receipt, result } = await performBrowserFileTransferUpload(
      item.file,
      props.requestUploadAccess,
      props.confirmUpload,
      {
        stateChanged: (state) => {
          item.state = state;
        },
        progressChanged: (percent) => {
          item.progress = percent;
        },
        taskCreated: (task) => {
          item.task = task;
        },
        taskFinished: () => {
          item.task = undefined;
        },
      },
    );
    emit('completed', receipt, result);
    presentPlatformSuccess(`“${item.file.name}”上传成功。${props.completionHint ?? ''}`, {
      source: 'file-transfer',
      tone: 'success',
    });
  } catch (error) {
    item.task = undefined;
    item.state = error instanceof Error && error.message === '上传已取消。' ? 'cancelled' : 'failed';
    item.error = error instanceof Error ? error.message : '文件上传失败。';
    emit('failed', item.file, error);
    if (item.state === 'failed') {
      presentPlatformError(error, { source: 'file-transfer', phase: 'action' });
    }
  }
}

function cancel(item: UploadItem) {
  item.task?.cancel();
}

function remove(item: UploadItem) {
  if (['requesting', 'uploading', 'confirming'].includes(item.state)) {
    cancel(item);
    return;
  }
  items.value = items.value.filter((candidate) => candidate.id !== item.id);
  emit(
    'changed',
    items.value.map((candidate) => candidate.file),
  );
}

function retry(item: UploadItem) {
  if (item.state === 'failed' || item.state === 'cancelled') {
    item.progress = 0;
    void upload(item);
  }
}

function stateText(item: UploadItem) {
  const labels: Record<UploadState, string> = {
    ready: '等待上传',
    // The upload ticket is an implementation detail.  Keep the user-facing
    // status focused on the file's progress rather than the authorization step.
    requesting: '上传准备中…',
    uploading: `正在上传 ${item.progress}%`,
    confirming: '校验文件中…',
    completed: '上传完成',
    failed: item.error ?? '上传失败',
    cancelled: '已取消',
  };
  return labels[item.state];
}
</script>

<template>
  <div class="file-transfer-uploader">
    <input
      ref="input"
      class="file-transfer-uploader__input"
      type="file"
      :accept="accept"
      :multiple="multiple"
      @change="selectFiles"
    />
    <div
      class="file-transfer-uploader__drop-zone"
      :class="{ 'is-dragging': dragging, 'is-disabled': disabled || active }"
      :tabindex="disabled || active ? -1 : 0"
      role="button"
      :aria-disabled="disabled || active"
      @click="chooseFiles"
      @keydown="handleDropZoneKeydown"
      @dragenter.prevent="dragOver"
      @dragover="dragOver"
      @dragleave="dragLeave"
      @drop="dropFiles"
    >
      <span class="file-transfer-uploader__drop-zone-icon">+</span>
      <span class="file-transfer-uploader__drop-zone-title">{{ uploadText }}</span>
      <span class="file-transfer-uploader__drop-zone-hint">{{
        disabled ? (disabledHint ?? '当前不可上传') : '点击选择，或将文件拖拽到此处'
      }}</span>
    </div>
    <div v-if="items.length" class="file-transfer-uploader__list" aria-live="polite">
      <div v-for="item in items" :key="item.id" class="file-transfer-uploader__item">
        <div class="file-transfer-uploader__name" :title="item.file.name">{{ item.file.name }}</div>
        <div class="file-transfer-uploader__state" :class="`is-${item.state}`">{{ stateText(item) }}</div>
        <div class="file-transfer-uploader__actions">
          <UiButton v-if="item.state === 'ready'" type="link" @click="upload(item)"> 上传 </UiButton>
          <UiButton
            v-else-if="item.state === 'failed' || item.state === 'cancelled'"
            type="link"
            @click="retry(item)"
          >
            重试
          </UiButton>
          <UiButton
            v-if="['requesting', 'uploading'].includes(item.state)"
            type="link"
            danger
            @click="cancel(item)"
          >
            取消
          </UiButton>
          <UiButton v-else-if="!['confirming'].includes(item.state)" type="link" danger @click="remove(item)">
            移除
          </UiButton>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.file-transfer-uploader {
  display: grid;
  gap: 8px;
}
.file-transfer-uploader__input {
  display: none;
}
.file-transfer-uploader__drop-zone {
  display: grid;
  justify-items: center;
  gap: 4px;
  min-height: 112px;
  padding: 20px;
  color: var(--ant-color-text-secondary);
  background: var(--ant-color-fill-quaternary);
  border: 1px dashed var(--ant-color-border-secondary);
  border-radius: 8px;
  cursor: pointer;
  outline: none;
  transition:
    border-color 160ms ease,
    background-color 160ms ease,
    box-shadow 160ms ease;
}
.file-transfer-uploader__drop-zone:hover,
.file-transfer-uploader__drop-zone:focus-visible,
.file-transfer-uploader__drop-zone.is-dragging {
  background: var(--ant-color-primary-bg);
  border-color: var(--ant-color-primary);
  box-shadow: 0 0 0 3px var(--ant-color-primary-bg);
}
.file-transfer-uploader__drop-zone.is-disabled {
  cursor: not-allowed;
  opacity: 0.65;
}
.file-transfer-uploader__drop-zone-icon {
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  color: var(--ant-color-primary);
  font-size: 22px;
  font-weight: 300;
  line-height: 1;
  background: var(--ant-color-primary-bg);
  border-radius: 50%;
}
.file-transfer-uploader__drop-zone-title {
  color: var(--ant-color-text);
  font-size: 14px;
}
.file-transfer-uploader__drop-zone-hint {
  font-size: 12px;
}
.file-transfer-uploader__list {
  display: grid;
  gap: 6px;
}
.file-transfer-uploader__item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 12px;
  padding: 8px 10px;
  border: 1px solid var(--ant-color-border-secondary);
  border-radius: 6px;
}
.file-transfer-uploader__name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.file-transfer-uploader__state {
  color: var(--ant-color-text-secondary);
  font-size: 12px;
}
.file-transfer-uploader__state.is-failed {
  color: var(--ant-color-error);
}
.file-transfer-uploader__state.is-completed {
  color: var(--ant-color-success);
}
.file-transfer-uploader__actions {
  display: flex;
  gap: 4px;
}
</style>
