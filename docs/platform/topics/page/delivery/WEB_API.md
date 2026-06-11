# 页面交付 Web API

本文只按当前 `DynamicPageBootstrapWebController`、`DynamicRecordWebController` 和 `PlatformPagePreferenceWebController` 能确认的 URL 梳理页面交付接口。

## 页面初始化

| 方法 | URL | 功能 |
| --- | --- | --- |
| `GET` | `/platform.menu/{menuId}/entry` | 按菜单节点读取页面 bootstrap；`clientType` 默认 `WEB`。 |

bootstrap 返回模块入口、客户端类型、权限裁剪后的动态 descriptor、主实体别名、resolved 页面配置、字段 UI 类型定义、字段 UI 类型属性和字段映射，以及 `/{moduleAlias}/openapi` 文档入口。

页面配置本身由配置专题维护：菜单方案、菜单树、字段 UI 类型配置、UI 配置集、UI 配置、UI 字段配置、查询模板和查询项的配置 URL 见 `configuration/WEB_API.md`。页面交付只消费已发布配置和当前用户可见菜单。

## 列表查询与汇总

| 方法 | URL | 功能 |
| --- | --- | --- |
| `POST` | `/{moduleAlias}/query` | 按动态查询请求分页查询主元数据记录；可结合 `uiConfigId` 做列表投影。 |
| `POST` | `/{moduleAlias}/query/summary` | 按同一查询上下文计算汇总项；汇总配置来自已发布 LIST UI 配置。 |

查询请求可使用 `uiConfigId`、`queryTemplateId`、`externalQueryValues`、`queryForm`、`criteria`、兼容 `conditions`、`quickSearch`、分页和排序。

## 表单保存

| 方法 | URL | 功能 |
| --- | --- | --- |
| `GET` | `/{moduleAlias}/view/{id}` | 查看记录详情。 |
| `POST` | `/{moduleAlias}/insert` | 新增记录；可携带 `uiConfigId + record` wrapper 触发表单校验。 |
| `POST` | `/{moduleAlias}/update/{id}` | 更新记录；可携带 `uiConfigId + record` wrapper 触发表单校验和乐观锁。 |
| `POST` | `/{moduleAlias}/delete/{id}` | 删除记录，动态侧按平台软删语义执行。 |

页面保存仍走动态记录保存链路，不直接写配置表，也不绕过动作权限、数据权限、字段保护和动态事件。

## 列表排序

| 方法 | URL | 功能 |
| --- | --- | --- |
| `POST` | `/{moduleAlias}/sort/{id}` | 调整当前记录列表排序；TREE 能力启用时同一路径也承载树内移动。 |

该入口来自 `DynamicRecordWebController` 的动态排序实现，只有主元数据具备 SORT 或 TREE 能力时可用。

## 附件关系

| 方法 | URL | 功能 |
| --- | --- | --- |
| `POST` | `/{moduleAlias}/view/{id}/attachments/query` | 查询记录附件关系。 |
| `POST` | `/{moduleAlias}/view/{id}/attachments/add` | 新增业务附件关系。 |
| `POST` | `/{moduleAlias}/view/{id}/attachments/update/{attachmentId}` | 更新附件关系的排序、备注等关系属性。 |
| `POST` | `/{moduleAlias}/view/{id}/attachments/delete/{attachmentId}` | 删除业务附件关系。 |
| `POST` | `/{moduleAlias}/view/{id}/attachments/upload-ticket` | 获取上传 access envelope。 |
| `POST` | `/{moduleAlias}/view/{id}/attachments/{attachmentId}/preview-ticket` | 获取预览 access envelope。 |
| `POST` | `/{moduleAlias}/view/{id}/attachments/{attachmentId}/download-ticket` | 获取下载 access envelope。 |

preview/download 会先校验记录权限和附件归属。文件二进制、文件事实和物理删除策略不属于这些接口。

## 查重与引用

| 方法 | URL | 功能 |
| --- | --- | --- |
| `POST` | `/{moduleAlias}/{actionCode}/duplicate/check` | 按动态 action 槽位执行查重预检，返回匹配记录摘要。 |
| `POST` | `/{moduleAlias}/references/{fieldName}/resolve` | 解析引用候选、标题和投影；可结合来源和目标 UI/查询上下文。 |

查重不替代数据库唯一约束。引用生成和草稿确认归属业务自动化的记录联动专题。

## 导航与偏好

| 方法 | URL | 功能 |
| --- | --- | --- |
| `GET` | `/{moduleAlias}/navigation/{sessionId}/{recordId}` | 按列表导航会话返回上一条、当前和下一条记录。 |
| `GET` | `/platform.page-preference/{moduleAlias}` | 读取当前用户页面偏好；支持 `clientType` 和 `pageKey`。 |
| `POST` | `/platform.page-preference/{moduleAlias}` | 保存当前用户页面偏好；请求需提供非空 `preferenceJson`。 |

页面偏好只影响当前用户体验，不改变平台配置和发布快照。

## 文档入口

| 方法 | URL | 功能 |
| --- | --- | --- |
| `GET` | `/{moduleAlias}/describe` | 返回动态模块 descriptor。 |
| `GET` | `/{moduleAlias}/openapi` | 返回动态模块 OpenAPI 基础文档模型。 |

## 非页面交付归属

| URL | 归属 |
| --- | --- |
| `/{moduleAlias}/references/{fieldName}/generate` | 记录联动。 |
| `/{moduleAlias}/generation/confirm` | 记录联动。 |
| `/{moduleAlias}/code/preview` | 编码规则。 |
| `/{moduleAlias}/exchange/template`、`/{moduleAlias}/import/*`、`/{moduleAlias}/export/*` | 数据交换。 |
