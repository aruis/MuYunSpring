# 编码规则 Web API

本文按 URL 简要列功能点，不展开完整请求和响应结构。

## 规则管理

基础路径：

- `/platform.code_rule`
- `/platform/code/rule`

接口：

- `POST /query`：查询编码规则列表，可按规则目标、模式、组织范围、启停状态、生效时间等字段过滤。
- `GET /view/{id}`：查看单条编码规则。
- `POST /enable/{id}`：启用编码规则。
- `POST /disable/{id}`：停用编码规则。
- `POST /sort/{id}`：调整规则排序。
- `GET /viewTree/{id}`：查看规则树。
- `POST /saveTree`：保存规则树。
- `POST /preview`：预览编码，可按 `ruleId` 或草稿规则、上下文、组织、时间和指定序列值生成预览结果。

## 运维动作

基础路径同规则管理。

- `POST /ops/view/{id}`：查看单条规则的运维快照。
- `POST /ops/queryByBizObject`：按业务对象查询规则运维快照。
- `POST /ops/sequenceState/locate`：定位规则、依据桶和期间桶对应的序列状态。
- `POST /ops/sequenceState/baseline`：设置序列基线。
- `POST /ops/recycleEntry/{id}/adjust`：调整回收池条目状态。
- `POST /ops/ledgerEntry/{id}/inspect`：检查编码台账条目。
- `POST /ops/ledgerEntry/{id}/release`：释放滞留台账占用。

## 只读事实表

编码台账：

- `/platform.code_ledger_entry`
- `/platform/code/ledger-entry`
- `POST /query`：查询编码占用事实。
- `GET /view/{id}`：查看台账条目。

编码回收池：

- `/platform.code_recycle_entry`
- `/platform/code/recycle-entry`
- `POST /query`：查询可回收编码。
- `GET /view/{id}`：查看回收池条目。

编码序列状态：

- `/platform.code_sequence_state`
- `/platform/code/sequence-state`
- `POST /query`：查询序列状态。
- `GET /view/{id}`：查看序列状态。

编码日志：

- `/platform.code_issue_log`
- `/platform/code/issue-log`
- `POST /query`：查询编码生成、占用、释放等执行日志。
- `GET /view/{id}`：查看编码日志条目。

## 动态记录预览

基础路径：

- `/{moduleAlias}`

接口：

- `POST /code/preview`：按当前动态记录值预览业务编码。该入口走 `CREATE` 动作语义，用于保存前展示候选编码，不表示已正式占用。
