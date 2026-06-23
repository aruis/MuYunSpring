<script setup lang="ts">
import type { DynamicModulePageDescriptor } from '@muyun/web-contracts';

defineOptions({ name: 'DynamicModuleHost' });

defineProps<{
  descriptor: DynamicModulePageDescriptor;
}>();
</script>

<template>
  <section class="module-workspace">
    <header class="module-header">
      <div>
        <h2>{{ descriptor.title ?? descriptor.target.moduleAlias }}</h2>
        <p>{{ descriptor.target.moduleAlias }}</p>
      </div>
      <div class="module-actions">
        <button type="button">查询</button>
        <button type="button">新建</button>
      </div>
    </header>

    <div class="module-body">
      <section class="record-list" aria-label="动态记录列表">
        <div class="list-toolbar">
          <span>全部记录</span>
          <strong>{{ descriptor.target.pageMode ?? 'LIST' }}</strong>
        </div>
        <div class="query-row">
          <span>快速筛选</span>
          <span>排序</span>
          <span>列设置</span>
        </div>
        <div class="record-row active">
          <div>
            <strong>{{ descriptor.target.moduleAlias }}</strong>
            <span>等待接入页面 bootstrap 与列表查询</span>
          </div>
          <small>当前</small>
        </div>
        <div class="record-row">
          <div>
            <strong>UI 配置</strong>
            <span>{{ descriptor.target.defaultUiConfigId ?? '默认配置' }}</span>
          </div>
          <small>配置</small>
        </div>
        <div class="record-row">
          <div>
            <strong>查询模板</strong>
            <span>{{ descriptor.target.defaultQueryTemplateId ?? '默认模板' }}</span>
          </div>
          <small>模板</small>
        </div>
      </section>

      <aside class="record-detail" aria-label="动态记录上下文">
        <div class="detail-heading">
          <span class="host-badge">动态模块</span>
          <h3>{{ descriptor.title ?? descriptor.target.moduleAlias }}</h3>
        </div>
        <dl class="host-facts">
          <div>
            <dt>页面模式</dt>
            <dd>{{ descriptor.target.pageMode ?? 'LIST' }}</dd>
          </div>
          <div>
            <dt>UI 配置</dt>
            <dd>{{ descriptor.target.defaultUiConfigId ?? '默认' }}</dd>
          </div>
          <div>
            <dt>查询模板</dt>
            <dd>{{ descriptor.target.defaultQueryTemplateId ?? '默认' }}</dd>
          </div>
        </dl>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.module-workspace {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: calc(100vh - 116px);
  border: 1px solid #d8e1ea;
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
}

.module-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
  min-height: 52px;
  padding: 10px 14px;
  border-bottom: 1px solid #e2e8f0;
}

.module-header div:first-child {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.module-header h2,
.detail-heading h3 {
  margin: 0;
  color: #172033;
}

.module-header h2 {
  overflow: hidden;
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.module-header p {
  overflow: hidden;
  margin: 0;
  color: #64748b;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.module-actions {
  display: flex;
  gap: 6px;
}

.module-actions button {
  height: 30px;
  padding: 0 10px;
  border: 1px solid #d8e1ea;
  border-radius: 6px;
  background: #fff;
  color: #334155;
  cursor: pointer;
}

.module-actions button:last-child {
  border-color: #0f766e;
  background: #0f766e;
  color: #fff;
}

.module-body {
  display: grid;
  grid-template-columns: minmax(420px, 1fr) 320px;
  min-height: 0;
}

.record-list {
  min-width: 0;
  border-right: 1px solid #e2e8f0;
}

.list-toolbar,
.query-row,
.record-row {
  display: flex;
  align-items: center;
}

.list-toolbar {
  justify-content: space-between;
  height: 42px;
  padding: 0 14px;
  border-bottom: 1px solid #e2e8f0;
  color: #334155;
  font-size: 13px;
}

.list-toolbar strong {
  color: #64748b;
  font-size: 11px;
}

.query-row {
  gap: 8px;
  height: 38px;
  padding: 0 14px;
  border-bottom: 1px solid #edf2f7;
  color: #64748b;
  font-size: 12px;
}

.query-row span {
  padding: 4px 8px;
  border: 1px solid #d8e1ea;
  border-radius: 999px;
  background: #f8fafc;
}

.record-row {
  justify-content: space-between;
  gap: 12px;
  min-height: 54px;
  padding: 9px 14px;
  border-bottom: 1px solid #edf2f7;
}

.record-row.active {
  background: #f1f8f6;
  box-shadow: inset 3px 0 0 #0f766e;
}

.record-row div {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.record-row strong,
.record-row span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.record-row strong {
  color: #172033;
  font-size: 13px;
}

.record-row span,
.record-row small {
  color: #64748b;
  font-size: 12px;
}

.record-detail {
  display: grid;
  align-content: start;
  gap: 12px;
  min-width: 0;
  padding: 14px;
  background: #fbfcfe;
}

.detail-heading {
  display: grid;
  gap: 8px;
}

.detail-heading h3 {
  font-size: 15px;
}

.host-badge {
  width: fit-content;
  padding: 4px 8px;
  border-radius: 999px;
  background: #e4f2ef;
  color: #0f766e;
  font-size: 12px;
  font-weight: 700;
}

h2 {
  margin: 0;
  color: #1f2933;
  font-size: 22px;
}

p {
  margin: 0;
  color: #64748b;
}

.host-facts {
  display: grid;
  gap: 8px;
  margin: 0;
}

.host-facts div {
  min-width: 0;
  padding: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #fff;
}

dt {
  color: #64748b;
  font-size: 12px;
}

dd {
  overflow: hidden;
  margin: 6px 0 0;
  color: #172033;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 760px) {
  .module-body {
    grid-template-columns: 1fr;
  }

  .record-list {
    border-right: 0;
  }
}
</style>
