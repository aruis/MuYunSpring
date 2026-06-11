# 记录联动 Web API

本文按 URL 简要列功能点，不展开完整请求和响应结构。

所有接口使用动态模块路径：

- `/{moduleAlias}` 必须是合法平台模块别名。
- 需要当前租户上下文，并校验租户有效。

## 动作目录与生单动作

基础路径：

- `/{moduleAlias}`

接口：

- `GET /actions`：读取当前模块可用动作。生单规则启用后贡献的托管动作从这里进入动作目录。
- `GET /actions/{recordId}`：读取某条记录可用的记录级动作，并返回可用性。
- `POST /{actionCode}/{recordId}`：执行记录级动作。生单托管动作通过该入口执行，返回目标草稿而不直接保存目标记录。

`actionCode` 来自动作目录，不应由调用方猜测。`POST /{actionCode}` 和 `POST /{actionCode}/batch` 是动态动作通用宿主，不属于记录联动专题的专属接口。

## 引用字段生单

基础路径：

- `/{moduleAlias}`

接口：

- `POST /references/{fieldName}/generate`：按引用字段和选中的来源记录触发生单，返回生成结果。

引用字段生单用于前端从字段语义出发触发生成，调用方不需要理解底层生成规则 actionCode。

## 生成草稿确认

基础路径：

- `/{moduleAlias}`

接口：

- `POST /generation/confirm`：确认生成草稿，保存目标记录并登记来源影响关系。

请求中的 `targetModuleAlias` 必须与路径模块一致。草稿 record 可携带一级子表 `children` envelope。`originContext` 只作为保存 mutation metadata 使用。

## 动态记录保存与回写触发

基础路径：

- `/{moduleAlias}`

接口：

- `POST /insert`：新增动态记录。可携带 `originContext`，保存成功后可登记来源影响关系；保存事件也可触发回写。
- `POST /update/{id}`：更新动态记录，产生 `before/after` 保存事件并触发回写。
- `POST /delete/{id}`：删除动态记录，产生删除事件并触发回写。

回写没有单独的用户动作入口。它由动态记录写事件同步执行，失败时按规则阻断来源事务，并通过诊断台账和字段影响记录支撑排查。

## 明确不归属本专题

- `POST /references/{fieldName}/resolve`：引用候选解析属于动态页面/引用基础能力，不作为记录联动专属接口。
- 页面查询、附件、模块任务和配置治理 URL 不在业务自动化记录联动专题内列出。
