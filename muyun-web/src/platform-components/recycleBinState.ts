import { computed, ref, toValue, watch, type MaybeRefOrGetter, type Ref } from 'vue';
import type {
  PurgeReport,
  RecycleBinItem,
  RestoreReport,
  WebPageResponse,
  WebQueryRequest,
} from '@muyun/web-contracts';
import type { ModuleContext } from '@muyun/web-core';
import { presentPlatformError, presentPlatformSuccess } from './platformErrorFeedback';

export interface RecycleBinStateOptions<TRecord> {
  context: MaybeRefOrGetter<ModuleContext<TRecord>>;
  recordTitle?: (record: TRecord) => string;
  /** 外部刷新信号，变化时重新加载列表 */
  reloadKey?: Ref<number>;
}

export function useRecycleBinState<TRecord>(options: RecycleBinStateOptions<TRecord>) {
  const items = ref<RecycleBinItem<TRecord>[]>([]);
  const loading = ref(false);
  const acting = ref(false);
  const actingOperationId = ref<string>();
  const total = ref(0);
  const summaryTotal = ref<number>();
  const pageNum = ref(1);
  const pageSize = ref(20);
  let lastRequest: WebQueryRequest = defaultQueryRequest();
  let loadRequestSeq = 0;
  let summaryRequestSeq = 0;

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

  async function load(request: WebQueryRequest = lastRequest): Promise<boolean> {
    const requestSeq = ++loadRequestSeq;
    const context = toValue(options.context);
    loading.value = true;
    lastRequest = request;
    try {
      const response = await context.http.request<WebPageResponse<RecycleBinItem<TRecord>>>({
        method: 'POST',
        path: `/${context.moduleAlias}/recycle-bin/query`,
        body: request,
      });
      if (requestSeq !== loadRequestSeq) return false;
      items.value = response.records;
      total.value = response.total;
      summaryTotal.value = response.total;
      pageNum.value = response.pageNum;
      pageSize.value = response.pageSize;
      return true;
    } catch (cause) {
      if (requestSeq !== loadRequestSeq) return false;
      items.value = [];
      total.value = 0;
      presentPlatformError(cause, { source: 'recycle-bin', phase: 'load' });
      return false;
    } finally {
      if (requestSeq === loadRequestSeq) loading.value = false;
    }
  }

  async function refreshSummary(): Promise<number | undefined> {
    const requestSeq = ++summaryRequestSeq;
    const context = toValue(options.context);
    try {
      const response = await context.http.request<WebPageResponse<RecycleBinItem<TRecord>>>({
        method: 'POST',
        path: `/${context.moduleAlias}/recycle-bin/query`,
        body: { page: { pageNum: 1, pageSize: 1 }, conditions: [], sorts: [] },
      });
      if (requestSeq !== summaryRequestSeq) return undefined;
      summaryTotal.value = response.total;
      return response.total;
    } catch {
      if (requestSeq === summaryRequestSeq) summaryTotal.value = undefined;
      return undefined;
    }
  }

  async function restore(item: RecycleBinItem<TRecord>, reload = true): Promise<RestoreReport | undefined> {
    if (acting.value || !item.restorable || !item.sourceDeleteOperationId) return undefined;
    acting.value = true;
    actingOperationId.value = item.sourceDeleteOperationId;
    const context = toValue(options.context);
    try {
      const report = await context.http.request<RestoreReport>({
        method: 'POST',
        path: `/${context.moduleAlias}/recycle-bin/${encodeURIComponent(item.sourceDeleteOperationId)}/restore`,
      });
      presentRestoreResult(report, recordTitleOf(item));
      if (reload) await load();
      return report;
    } catch (cause) {
      presentPlatformError(cause, { source: 'recycle-bin', phase: 'action' });
      return undefined;
    } finally {
      acting.value = false;
      actingOperationId.value = undefined;
    }
  }

  async function purge(item: RecycleBinItem<TRecord>, reload = true): Promise<PurgeReport | undefined> {
    if (acting.value || !item.purgeable || !item.sourceDeleteOperationId) return undefined;
    acting.value = true;
    actingOperationId.value = item.sourceDeleteOperationId;
    const context = toValue(options.context);
    try {
      const report = await context.http.request<PurgeReport>({
        method: 'POST',
        path: `/${context.moduleAlias}/recycle-bin/${encodeURIComponent(item.sourceDeleteOperationId)}/purge`,
      });
      presentPurgeResult(report, recordTitleOf(item));
      if (reload) await load();
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
    total,
    summaryTotal,
    pageNum,
    pageSize,
    isEmpty,
    recordTitleOf,
    load,
    refreshSummary,
    restore,
    purge,
  };
}

function defaultQueryRequest(): WebQueryRequest {
  return {
    page: { pageNum: 1, pageSize: 200 },
    conditions: [],
    sorts: [],
  };
}

function presentRestoreResult(report: RestoreReport, title: string) {
  const restored = report.entries.filter((entry) => entry.status === 'RESTORED').length;
  const skipped = report.entries.filter((entry) => entry.status === 'SKIPPED').length;
  const failed = report.entries.filter((entry) => entry.status === 'FAILED').length;
  if (restored === 0) {
    presentPlatformSuccess(`「${title}」未恢复：跳过 ${skipped}，失败 ${failed}`, {
      source: 'recycle-bin',
      phase: 'action',
    });
  } else if (failed > 0 || skipped > 0) {
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
  if (purged === 0) {
    presentPlatformSuccess(`「${title}」未彻底删除：跳过 ${skipped}，失败 ${failed}`, {
      source: 'recycle-bin',
      phase: 'action',
    });
  } else if (failed > 0 || skipped > 0) {
    presentPlatformSuccess(`「${title}」彻底删除完成：成功 ${purged}，跳过 ${skipped}，失败 ${failed}`, {
      source: 'recycle-bin',
      phase: 'action',
    });
  } else {
    presentPlatformSuccess(`「${title}」已彻底删除`, { source: 'recycle-bin', phase: 'action' });
  }
}
