# M8 Stable Contract

M8 的稳定边界是“已发布页面配置驱动动态页面运行”，而不是新增一套绕过动态模块运行态的页面系统。

## 主链路契约

1. 菜单入口通过 `/platform.menu/{menuId}/entry` 进入页面，返回面向前端的 bootstrap 契约；菜单只定位模块、页面模式、客户端类型、默认 UI 配置、默认查询模板和入口参数。
2. bootstrap 只消费已发布且启用的 UI 配置和查询模板；未发布配置只属于配置工作区，不进入在线页面。
3. 列表查询、汇总面板、动态导出和引用候选复用同一查询模板编译链路，额外手工条件只作为运行态 Criteria 叠加，不生成第二套查询协议。快速查询只在已发布 LIST 配置的可见主关系文本字段范围内编译为 `LIKE` 条件；显式指定字段时，字段必须在该范围内。
4. 查看、新增、更新、子表保存、附件关系维护、查重预检、引用解析和引用生单都继续走动态 Web 与动态保存链路，不直接写平台配置表，也不绕过动作权限、数据权限、字段保护、乐观锁和动态事件。
5. 页面保存 wrapper 使用 `uiConfigId + record`；`uiConfigId` 只作为 mutation metadata 触发表单 required/readOnly 校验。主关系字段校验当前记录；子关系字段只校验本次提交的 `children.{relationCode}` 行，未提交子表不校验，空数组表示提交了空子表。
6. `record.attachments` 只维护业务记录到 `fileId` 的关系。上传、预览和下载通过附件 access envelope 发放 ticket 或跳转信息；文件二进制、文件事实、预览、下载和物理删除策略归属文件服务。
7. 模块 OpenAPI 暴露页面交付所需入口，包括查询、汇总、保存 wrapper、附件维护、附件 access ticket、查重预检、引用解析、动态动作和稳定错误响应；实际可用性仍以后端授权和运行态校验为准。

## 前端 Handoff 索引

| 业务板块 | 入口/字段 | 稳定语义 |
| --- | --- | --- |
| 页面初始化 | `/platform.menu/{menuId}/entry` | 菜单入口返回模块、页面模式、客户端、默认 UI 配置、默认查询模板、动作和 OpenAPI 入口。 |
| 列表查询 | `POST /{moduleAlias}/query` | 支持 `uiConfigId` 列投影、`queryTemplateId + externalQueryValues`、手工 `conditions`、`quickSearch + quickSearchFields`、分页排序和可选导航会话。 |
| 汇总面板 | `POST /{moduleAlias}/query/summary` | 使用与列表一致的查询上下文，从已发布 LIST 配置的 `layoutJson.summaryPanel.items` 读取汇总项。 |
| 引用候选 | `POST /{moduleAlias}/references/{fieldName}/resolve` | `sourceUiConfigId` 校验来源 FORM/DETAIL 上下文，`uiConfigId` 校验目标 LIST/REFERENCE 上下文，`queryTemplateId` 可覆盖字段默认候选模板。 |
| 页面保存 | `POST /{moduleAlias}/insert`、`POST /{moduleAlias}/update/{id}` | wrapper 为 `uiConfigId + record`；`record.values` 写业务字段，`record.children` 写一级子表，`record.version` 参与乐观锁。 |
| 子表提交 | `record.children.{relationCode}` | 缺省或 `null` 表示不改该子表；空数组表示提交空子表；提交的子行按已发布 UI 配置校验 required/readOnly。 |
| 附件关系 | `record.attachments` 和 `/view/{id}/attachments/*` | 只维护业务附件关系和排序/备注；`fileId` 来自文件服务，不在 MuYunSpring 保存 MIME、大小、上传人等文件事实。 |
| 附件 ticket | `/view/{id}/attachments/upload-ticket`、`/{attachmentId}/preview-ticket`、`/{attachmentId}/download-ticket` | 返回 `RecordAttachmentAccess`，由后续 adapter 映射到文件服务 token、URL 或重定向信息。preview/download 先校验记录权限和附件归属。 |
| 查重预检 | `POST /{moduleAlias}/{actionCode}/duplicate/check` | 查重绑定动态 action 槽位和权限，不替代数据库唯一约束，返回匹配记录摘要供前端提示。 |
| 错误响应 | `DynamicWebError` | 通用错误 `DYNAMIC_BAD_REQUEST`，UI 保存校验 `DYNAMIC_UI_VALIDATION`，附件接入 `DYNAMIC_ATTACHMENT_ERROR`，查重接入 `DYNAMIC_DUPLICATE_CHECK_ERROR`，乐观锁 `DYNAMIC_CONFLICT`。 |

## M3 视图兼容基线

M3 运行态 view descriptor 继续作为动态模块结构描述的一部分，用于兼容既有调用方读取列表/表单字段、基础控件、只读和必填提示。M8 UI 配置是页面交付真相源，用于菜单入口、bootstrap、列表列、表单字段、保存校验、查询汇总和客户端差异。

后续迁移遵循以下规则：

1. 新页面优先使用 M8 UI Set/UI Config/Query Template，不再扩展 M3 view descriptor 表达复杂布局、发布状态、菜单入口或页面查询方案。
2. M3 view descriptor 不删除、不改语义；它保持结构级兼容输出，避免旧集成被迫迁移。
3. 当一个模块同时存在 M3 view 和 M8 UI 配置时，页面 bootstrap、动态列表投影、表单保存校验和汇总面板以已发布 M8 UI 配置为准。
4. 从 M3 view 迁移到 M8 UI 配置时，只迁移字段顺序、显示、标题、只读、必填和基础控件别名；布局、查询、菜单入口、发布状态和客户端差异在 M8 配置中重新声明。
5. 不建立从 M8 UI 配置反写 M3 view 的同步机制，避免两个真相源互相覆盖。

## 测试锚点

M8 主链路由以下 contract 测试共同锁定：

| 链路 | 测试 |
| --- | --- |
| 菜单入口到 bootstrap | `MenuEntryBootstrapContractTest` |
| 发布快照、UI 字段和查询模板 | `PlatformUiConfigurationServiceContractTest` |
| 动态查询、汇总、保存、附件、查重、引用候选、引用生单、乐观锁 | `DynamicRecordWebControllerTest` |
| OpenAPI 页面交付入口 | `DynamicOpenApiGeneratorTest` |
| 发布到动态运行态和 M3 descriptor 兼容 | `PlatformDynamicModulePublisherIT` |
