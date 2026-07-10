import type { Ref } from 'vue';
import type { StaticRecordMutationResult } from '@muyun/web-core';
import { presentPlatformError, presentPlatformMessage, presentPlatformSuccess } from './platformErrorFeedback';

export type StaticFormSaveMode = 'create' | 'edit';

export interface StaticFormSaveOptions<TRecord> {
  loading: Ref<boolean>;
  mode: StaticFormSaveMode;
  source?: string;
  validateContext?: () => string | undefined;
  canSave: () => boolean;
  deniedMessage: string;
  createRecord: () => TRecord;
  validateRecord?: (record: TRecord) => string | undefined;
  save: (record: TRecord, mode: StaticFormSaveMode) => Promise<StaticRecordMutationResult<TRecord>>;
  onSaved: (result: StaticRecordMutationResult<TRecord>) => void;
  successMessage?: string;
}

export interface StaticRecordActionOptions<TRecord, TResult = unknown> {
  loading: Ref<boolean>;
  source?: string;
  record: () => TRecord | undefined;
  canExecute: (record: TRecord) => boolean;
  deniedMessage: string;
  confirm?: (record: TRecord) => Promise<boolean>;
  execute: (record: TRecord) => Promise<TResult>;
  onExecuted: (result: TResult, record: TRecord) => void | Promise<void>;
  successMessage?: string;
}

export async function executeStaticFormSave<TRecord>(options: StaticFormSaveOptions<TRecord>) {
  if (options.loading.value) {
    return undefined;
  }
  const source = options.source ?? 'static-form-action';
  const contextMessage = options.validateContext?.();
  if (contextMessage) {
    presentPlatformMessage(contextMessage, { source, phase: 'validation' });
    return undefined;
  }
  if (!options.canSave()) {
    presentPlatformMessage(options.deniedMessage, { source, phase: 'authorization' });
    return undefined;
  }
  const record = options.createRecord();
  const validationMessage = options.validateRecord?.(record);
  if (validationMessage) {
    presentPlatformMessage(validationMessage, { source, phase: 'validation' });
    return undefined;
  }

  options.loading.value = true;
  try {
    const result = await options.save(record, options.mode);
    options.onSaved(result);
    presentPlatformSuccess(result.message ?? options.successMessage ?? '操作成功', {
      source,
      phase: 'action',
    });
    return result;
  } catch (cause) {
    presentPlatformError(cause, { source, phase: 'action' });
    return undefined;
  } finally {
    options.loading.value = false;
  }
}

export async function executeStaticRecordAction<TRecord, TResult = unknown>(
  options: StaticRecordActionOptions<TRecord, TResult>,
) {
  if (options.loading.value) {
    return undefined;
  }
  const record = options.record();
  if (!record) {
    return undefined;
  }
  const source = options.source ?? 'static-record-action';
  if (!options.canExecute(record)) {
    presentPlatformMessage(options.deniedMessage, { source, phase: 'authorization' });
    return undefined;
  }
  const confirmed = options.confirm ? await options.confirm(record) : true;
  if (!confirmed) {
    return undefined;
  }

  options.loading.value = true;
  try {
    const result = await options.execute(record);
    await options.onExecuted(result, record);
    presentPlatformSuccess(actionResultMessage(result) ?? options.successMessage ?? '操作成功', {
      source,
      phase: 'action',
    });
    return result;
  } catch (cause) {
    presentPlatformError(cause, { source, phase: 'action' });
    return undefined;
  } finally {
    options.loading.value = false;
  }
}

function actionResultMessage(result: unknown): string | undefined {
  if (!result || typeof result !== 'object' || !('message' in result)) {
    return undefined;
  }
  const message = (result as { message?: unknown }).message;
  return typeof message === 'string' && message.trim() ? message : undefined;
}
