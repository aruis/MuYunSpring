# M8 Stable Contract

M8 的稳定边界是“已发布页面配置驱动动态页面运行”，而不是新增一套绕过动态模块运行态的页面系统。

## 主链路契约

1. 菜单入口通过 `/platform.menu/{menuId}/entry` 进入页面，返回面向前端的 bootstrap 契约；菜单只定位模块、页面模式、客户端类型、默认 UI 配置、默认查询模板和入口参数。
2. bootstrap 只消费已发布且启用的 UI 配置和查询模板；未发布配置只属于配置工作区，不进入在线页面。
3. 列表查询、汇总面板、动态导出和引用候选复用同一查询模板编译链路，额外手工条件只作为运行态 Criteria 叠加，不生成第二套查询协议。
4. 查看、新增、更新、子表保存、附件关系维护、查重预检、引用解析和引用生单都继续走动态 Web 与动态保存链路，不直接写平台配置表，也不绕过动作权限、数据权限、字段保护、乐观锁和动态事件。
5. 页面保存 wrapper 使用 `uiConfigId + record`；`uiConfigId` 只作为 mutation metadata 触发表单 required/readOnly 校验，`record.attachments` 只维护业务记录到 `fileId` 的关系，文件事实归属 `MuYunFileServer`。
6. 模块 OpenAPI 暴露页面交付所需入口，包括查询、汇总、保存 wrapper、附件维护、查重预检、引用解析、动态动作和稳定错误响应；实际可用性仍以后端授权和运行态校验为准。

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
