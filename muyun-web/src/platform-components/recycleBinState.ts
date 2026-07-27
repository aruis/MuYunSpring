import { computed, ref, watch, type Ref } from 'vue';
import type {
  PurgeReport,
  RecycleBinItem,
  RestoreReport,
  WebListResponse,
  WebPageRequest,
} from '@muyun/web-contracts';
import type { ModuleContext } from '@muyun/web-core';
import { presentPlatformError, presentPlatformSuccess } from './platformErrorFeedback';

export interface RecycleBinStateOptions<TRecord> {
  context: ModuleContext<TRecord>;
  recordTitle?: (record: TRecord) => string;
  /** 外部刷新信号，变化时重新加载列表 */
  reloadKey?: Ref<number>;
}

export function useRecycleBinState<TRecord>(options: RecycleBinStateOptions<TRecord>) {
  const { context } = options;
  const items = ref<RecycleBinItem<TRecord>[]>([]);
  const loading = ref(false);
  const acting = ref(false);
  const actingOperationId = ref<string>();

  if (options.reloadKey) {
    watch(options.reloadKey, () => void load());
  }

  const isEmpty = computed(() => !loading.value && items.value.length === 0);

  function recordTitleOf(item: RecycleBinItem<TRecord>): string {
    if (options.recordTitle) {
      return options.recordTitle(item.record);
    }
    const record = item.record as Record<string, unknown>;
    return String(record.title ?? record.alias ?? record.id ?? '未命名记录');
  }

  async function load() {
    loading.value = true;
    try {
      const page: WebPageRequest = { pageNum: 1, pageSize: 200 };
      const response = await context.http.request<WebListResponse<RecycleBinItem<TRecord>>>({
        method: 'POST',
        path: `/${context.moduleAlias}/recycle-bin/query`,
        body: page,
      });
      items.value = response.records;
    } catch (cause) {
      presentPlatformError(cause, { source: 'recycle-bin', phase: 'load' });
    } finally {
      loading.value = false;
    }
  }

  async function restore(item: RecycleBinItem<TRecord>): Promise<RestoreReport | undefined> {
    if (acting.value) return undefined;
    acting.value = true;
    actingOperationId.value = item.sourceDeleteOperationId;
    try {
      const report = await context.http.request<RestoreReport>({
        method: 'POST',
        path: `/${context.moduleAlias}/recycle-bin/${encodeURIComponent(item.sourceDeleteOperationId)}/restore`,
      });
      presentRestoreResult(report, recordTitleOf(item));
      await load();
      return report;
    } catch (cause) {
      presentPlatformError(cause, { source: 'recycle-bin', phase: 'action' });
      return undefined;
    } finally {
      acting.value = false;
      actingOperationId.value = undefined;
    }
  }

  async function purge(item: RecycleBinItem<TRecord>): Promise<PurgeReport | undefined> {
    if (acting.value) return undefined;
    acting.value = true;
    actingOperationId.value = item.sourceDeleteOperationId;
    try {
      const report = await context.http.request<PurgeReport>({
        method: 'POST',
        path: `/${context.moduleAlias}/recycle-bin/${encodeURIComponent(item.sourceDeleteOperationId)}/purge`,
      });
      presentPurgeResult(report, recordTitleOf(item));
      await load();
      return report;
    } catch (cause) {
      presentPlatformError(cause, { source: 'recycle-bin', phase: 'action' });
      return undefined;
    } finally {
      acting.value = false;
      actingOperationId.value = undefined;
    }
  }

  return {
    items,
    loading,
    acting,
    actingOperationId,
    isEmpty,
    recordTitleOf,
    load,
    restore,
    purge,
  };
}

function presentRestoreResult(report: RestoreReport, title: string) {
  const restored = report.entries.filter((entry) => entry.status === 'RESTORED').length;
  const skipped = report.entries.filter((entry) => entry.status === 'SKIPPED').length;
  const failed = report.entries.filter((entry) => entry.status === 'FAILED').length;
  if (failed > 0) {
    presentPlatformSuccess(`「${title}」恢复完成：成功 ${restored}，跳过 ${skipped}，失败 ${failed}`, {
      source: 'recycle-bin',
      phase: 'action',
    });
  } else {
    presentPlatformSuccess(`「${title}」已恢复`, { source: 'recycle-bin', phase: 'action' });
  }
}

function presentPurgeResult(report: PurgeReport, title: string) {
  const purged = report.entries.filter((entry) => entry.status === 'PURGED').length;
  const skipped = report.entries.filter((entry) => entry.status === 'SKIPPED').length;
  const failed = report.entries.filter((entry) => entry.status === 'FAILED').length;
  if (failed > 0) {
    presentPlatformSuccess(`「${title}」彻底删除完成：成功 ${purged}，跳过 ${skipped}，失败 ${failed}`, {
      source: 'recycle-bin',
      phase: 'action',
    });
  } else {
    presentPlatformSuccess(`「${title}」已彻底删除`, { source: 'recycle-bin', phase: 'action' });
  }
}
