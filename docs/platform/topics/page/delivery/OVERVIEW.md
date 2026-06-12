# 页面交付概览

页面交付把动态模块从后端可交付推进到前端可按配置运行：菜单能进入页面，bootstrap 能下发页面上下文，列表、表单、查询、汇总、附件、查重、引用候选和页面偏好能走稳定后端契约。

## 能力定位

页面交付不建设另一套动态表单内核。它消费已发布 UI 配置和查询模板，并通过动态 Web 入口执行数据读写。

核心能力：

1. 菜单入口定位模块、页面模式、客户端类型、默认 UI 配置、默认查询模板和入口参数。
2. bootstrap 返回模块 descriptor、主元数据、resolved 页面配置、动作目录和 OpenAPI 入口。
3. 列表查询、汇总和引用候选复用同一查询模板与 Criteria 编译链路。
4. 表单保存使用 `uiConfigId + record` wrapper，按已发布 UI 配置执行 required/readOnly 校验。
5. 子表保存沿用动态记录 `children` 语义：缺省或 `null` 表示不改，空数组表示提交空子表。
6. 附件只维护业务记录与 `fileId` 的关系，上传、预览、下载通过 access envelope 对接文件服务。
7. 查重预检绑定动态 action 槽位和权限，不替代数据库唯一约束。
8. 页面偏好属于当前用户体验配置，不改变平台 UI 配置真相源。

## 主链路

```text
/platform.menu/{menuId}/entry
  -> PlatformPageBootstrap
  -> resolvedConfig
  -> /{moduleAlias}/query
  -> /{moduleAlias}/insert 或 /update/{id}
  -> 动态记录服务、权限、数据范围、事件和审计
```

bootstrap 只消费在线发布快照。未发布配置、其他客户端配置和不可见动作不应进入在线响应。

## 查询语义

列表查询以 `WebQueryRequest` 为入口，支持：

| 输入 | 作用 |
| --- | --- |
| `uiConfigId` | 控制列表列投影和页面字段范围。 |
| `queryTemplateId` | 使用已发布查询模板。 |
| `externalQueryValues` | 为查询模板中的外部值占位提供运行时值。 |
| `queryForm` | 按已发布 LIST UI 的可见主表字段提交表单值。 |
| `criteria` | 表达任意层级 `AND/OR` 分组嵌套。 |
| `conditions` | 兼容顶层扁平 `AND` 条件。 |
| `quickSearch` | 在已发布 LIST 配置的可见主关系文本字段范围内编译为 `LIKE`。 |
| `page` / `sorts` | 分页和排序。 |

运行时合并规则为：

```text
queryTemplateId/externalQueryValues
AND queryForm
AND conditions
AND criteria
AND quickSearch
```

汇总面板和引用候选复用这条查询语义，避免形成第二套查询协议。

## 保存语义

页面保存请求可以直接提交动态记录，也可以提交 `uiConfigId + record` wrapper。带 `uiConfigId` 时：

1. UI 配置必须来自已发布快照。
2. 主关系字段校验当前记录。
3. 子关系字段只校验本次提交的 `children.{relationCode}` 行。
4. 未提交子表不校验；空数组表示提交了空子表。
5. `record.version` 参与乐观锁。
6. 字段保护、动作权限、数据范围和动态事件仍由动态保存链路处理。

动态记录请求保留字段包括 `id`、`version`、`values`、`children`、`attachments`、`originContext`、`uiConfigId` 和 `record`，不应作为业务字段名使用。

## 边界说明

1. 当前不扩展静态表单 UI；静态链路只共享底层平台能力。
2. 页面配置以 `moduleAlias` 和元数据字段稳定 ID 为锚点，不引入平行字段身份。
3. 菜单入口不负责权限本身；菜单剪枝、动作授权和数据范围由身份权限专题负责。
4. 附件接口不保存 MIME、大小、上传人等文件事实。
5. 用户偏好不改变已发布 UI 配置，只影响当前用户的页面体验。
6. 关联视图、局部编辑、动态弹窗和模块任务归属页面交互专题。
