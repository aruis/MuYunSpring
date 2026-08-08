<script setup lang="ts">
import type { ModuleContext } from '@muyun/web-core';
import FileTransferUploader from './FileTransferUploader.vue';
import {
  unwrapResponsePayload,
  type FileTransferUploadAccess,
  type FileTransferUploadReceipt,
} from './fileTransferUpload';

defineOptions({ name: 'RecordFormFileTransfer' });

const props = defineProps<{
  context?: ModuleContext<unknown>;
  record: Record<string, unknown>;
  disabled?: boolean;
  disabledHint?: string;
}>();
const emit = defineEmits<{ uploaded: [fileId: string] }>();

async function requestUploadAccess(): Promise<FileTransferUploadAccess> {
  if (!props.context) throw new Error('文件上传字段需要模块上下文。');
  const response = await props.context.http.request<unknown>({
    method: 'POST',
    path: `/${props.context.moduleAlias}/file-transfer/upload-ticket`,
    body: props.record,
  });
  const access = unwrapResponsePayload(response) as { url?: string; formFields?: Record<string, string> };
  if (!access?.url) throw new Error('上传凭证未返回上传地址。');
  return { uploadUrl: access.url, formFields: access.formFields };
}

async function confirmUpload(receipt: FileTransferUploadReceipt) {
  const payload = receipt.payload as { items?: Array<{ id?: string }> };
  const fileId = payload?.items?.[0]?.id;
  if (!fileId) throw new Error('文件服务未返回文件标识。');
  emit('uploaded', fileId);
}
</script>

<template>
  <FileTransferUploader
    :disabled="disabled || !context"
    :disabled-hint="disabledHint"
    :request-upload-access="requestUploadAccess"
    :confirm-upload="confirmUpload"
    completion-hint="请保存文件信息。"
    upload-text="选择文件"
  />
</template>
