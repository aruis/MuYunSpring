# 导入导出 Web API

本文按 URL 简要列功能点，不展开完整请求和响应结构。

所有接口使用动态模块路径：

- `/{moduleAlias}` 必须是合法平台模块别名。
- 需要当前租户上下文，并校验租户有效。
- 模块主实体必须开启 `EXCHANGE` 能力。

## 模板

基础路径：

- `/{moduleAlias}/exchange`

接口：

- `POST /template`：下载导入模板，返回 xlsx 文件。请求可指定关闭引用下拉的字段和引用下拉数量上限。

## 导入

基础路径：

- `/{moduleAlias}/import`

接口：

- `POST /parse`：上传 xlsx 文件并解析，返回解析结果，不执行写入。
- `POST /execute`：上传执行命令和 xlsx 文件，按主表及子表匹配字段、重复策略执行导入。
- `POST /error-file/{token}`：下载导入错误文件。

导入执行结果包含新增数、更新数、跳过数、错误数、是否部分成功、消息、错误文件名和错误文件 token。

## 导出

基础路径：

- `/{moduleAlias}/export`

接口：

- `POST /data`：按查询条件导出动态模块数据，返回 xlsx 文件。

导出请求复用 `WebQueryRequest` 查询上下文，可携带分页、排序、查询模板、外部查询值、查询表单值和手工条件。
