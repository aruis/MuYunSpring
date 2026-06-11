# 业务自动化 Web API

本文只列业务自动化相关 URL 和功能点，不替代 OpenAPI。

## 编码规则

详见 [code-rule/WEB_API.md](code-rule/WEB_API.md)。

- `/platform.code_rule`、`/platform/code/rule`：编码规则查询、查看、启停、排序、规则树、预览和运维动作。
- `/platform.code_ledger_entry`、`/platform/code/ledger-entry`：编码台账只读查询。
- `/platform.code_recycle_entry`、`/platform/code/recycle-entry`：编码回收池只读查询。
- `/platform.code_sequence_state`、`/platform/code/sequence-state`：编码序列状态只读查询。
- `/platform.code_issue_log`、`/platform/code/issue-log`：编码日志只读查询。
- `/{moduleAlias}/code/preview`：动态业务记录保存前预览可生成编码。

## 导入导出

详见 [data-exchange/WEB_API.md](data-exchange/WEB_API.md)。

- `/{moduleAlias}/exchange/template`：下载动态模块导入模板。
- `/{moduleAlias}/import/parse`：解析导入文件。
- `/{moduleAlias}/import/execute`：执行导入。
- `/{moduleAlias}/import/error-file/{token}`：下载导入错误文件。
- `/{moduleAlias}/export/data`：导出动态模块数据。

## 记录联动

详见 [record-linkage/WEB_API.md](record-linkage/WEB_API.md)。

- `/{moduleAlias}/actions`、`/{moduleAlias}/actions/{recordId}`：读取模块动作和记录动作，生单托管动作从这里暴露。
- `/{moduleAlias}/{actionCode}/{recordId}`：执行记录级托管动作，可承载生单动作。
- `/{moduleAlias}/references/{fieldName}/generate`：按引用字段触发生单。
- `/{moduleAlias}/generation/confirm`：确认生成草稿并保存目标记录。
- `/{moduleAlias}/insert`、`/{moduleAlias}/update/{id}`、`/{moduleAlias}/delete/{id}`：动态记录写入口；回写由这些写事件触发，`originContext` 也通过保存请求进入来源关系登记。

## 明确留给其他专题

- 页面查询、引用候选解析、附件、模块任务和配置治理 URL 不归入业务自动化专题。
- `/{moduleAlias}/{actionCode}` 和 `/{moduleAlias}/{actionCode}/batch` 是动态动作通用宿主，不作为记录联动专属接口列出。
