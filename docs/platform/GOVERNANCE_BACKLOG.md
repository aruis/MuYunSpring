# 平台治理台账

本文档用于记录平台能力复盘后尚未治理完成的事项。它是治理过程台账，不替代业务专题文档。

治理项完成并形成稳定能力后，再回流到对应专题的 `OVERVIEW.md` 和 `WEB_API.md`。专题文档只描述已稳定的能力、边界和接口线索，不承载过程性待办。

## 使用原则

1. 只记录已经经过专题复盘确认的缺口或待判断项。
2. 优先描述能力缺口和 Web 暴露面缺口，不展开 Java 底层设计。
3. 状态使用 `待治理`、`治理中`、`已完成`、`暂不吸收`。
4. 优先级使用 `P0`、`P1`、`P2`：
   - `P0`：平台能力已有明显基础，但缺少必要 Web 暴露面或交接入口。
   - `P1`：对平台完整性、接手效率或配置体验有明显价值，但不阻断主链路。
   - `P2`：可延后评估，或需要更多真实业务场景证明。

## 总览

| 优先级 | 专题 | 治理项 | 当前判断 | 状态 |
| --- | --- | --- | --- | --- |
| P0 | 配置与元数据 | UI 配置管理 Web 暴露面 | 已补齐 UI 配置集、UI 配置、字段配置和发布入口。 | 已完成 |
| P0 | 配置与元数据 | 查询模板管理 Web 暴露面 | 已补齐查询模板、查询项和发布入口。 | 已完成 |
| P1 | 配置与元数据 | 字段引用、字段保护、校验规则等配置入口 | 已补齐字段引用、保护、引用过滤、引用回填和公式规则配置入口。 | 已完成 |
| P1 | 身份权限 | 角色授权批量矩阵操作 | 已补齐批量授权、批量撤销和矩阵回显入口。 | 已完成 |
| P1 | 身份权限 | 角色菜单授权视图 | 已补齐角色菜单矩阵视图，保存仍复用模块 `menu` 动作授权。 | 已完成 |
| P1 | 身份权限 | 用户选择器类查询接口 | 已补齐按角色、组织、关键字过滤的用户选择器入口。 | 已完成 |
| P1 | 业务自动化 | 选中导出 | 已补齐按选中 ID 导出的显式入口，并复用动态导出链路。 | 已完成 |
| P1 | 业务自动化 | 用户导出模板 | 用户级导出字段、排序等个人化配置先由页面偏好承载，不新增模板模型。 | 暂不吸收 |
| P1 | 业务自动化 | 通用用户偏好 | 已确认通过页面偏好接口承载当前用户页面与导出个人化配置。 | 已完成 |
| P1 | 治理 | 配置发布、包、导入导出 Web 暴露面 | 已补齐配置包健康检查、发布、回滚、导出、导入预检和导入草稿发布入口。 | 已完成 |
| P1 | 配置与元数据 | 菜单方案、菜单维护 Web 暴露面 | 已补齐菜单方案和方案内菜单树维护入口。 | 已完成 |
| P1 | 配置与元数据 | 字段 UI 类型属性和字段映射配置入口 | 已补齐字段 UI 类型下的属性和字段映射维护入口。 | 已完成 |
| P1 | 记录联动 | 生单、回写配置 Web 暴露面 | 已补齐按模块聚合的生单和回写规则树配置入口。 | 已完成 |
| P1 | 工作流任务 | 工作流定义与发布配置 Web 暴露面 | 已补齐模块聚合的定义、版本和发布配置入口。 | 已完成 |
| P2 | 业务自动化 | 序列状态行级调整 | 已补齐序列状态行级基线调整入口，并复用编码运维服务。 | 已完成 |
| P2 | 页面交互 | 公式即时计算 | 已补齐 `/{moduleAlias}/formula/preview`，作为动态运行态试算入口，不落库并返回变更字段和结构化诊断。 | 已完成 |
| P2 | 配置与元数据 | 元数据视图、视图字段配置入口 | 已按模块元数据关系聚合开放元数据视图和视图字段维护入口。 | 已完成 |
| P2 | 治理 | 模板复用管理入口 | 已补齐无状态模板派生和实例化入口；不引入模板仓库和持久化管理模型。 | 已完成 |
| P2 | 运行态 | 标准批量删除动作贡献 | 已新增标准 `batchDelete` 批量动作，复用 `/{moduleAlias}/{actionCode}/batch` 宿主和 `delete` 权限。 | 已完成 |
| P2 | 页面交互 | 动态弹窗独立接口 | 当前可优先由动作模型承载；除非动作模型表达不足，否则不单独扩展。 | 暂不吸收 |
| P2 | 配置与元数据 | 排序方案、打印模板 | neo 中存在近似能力，但 MuYun 当前主线不明确依赖。 | 暂不吸收 |

## 已完成治理项

| 专题 | 治理项 | 完成说明 | 回流目标 |
| --- | --- | --- | --- |
| 配置与元数据 | 平台配置基础 Web 暴露面 | 已补齐应用、模块、元数据、字段、模块动作、模块元数据关系、模块字段、字段类型和字段 UI 类型等基础入口。 | `configuration/WEB_API.md` |
| 配置与元数据 | UI 配置管理 Web 暴露面 | 已补齐模块 UI 配置集、配置集下 UI 配置、UI 配置字段和发布/取消发布入口。 | `configuration/WEB_API.md`、`page/delivery/WEB_API.md` |
| 配置与元数据 | 查询模板管理 Web 暴露面 | 已补齐模块查询模板、模板下查询项和发布/取消发布入口。 | `configuration/WEB_API.md`、`runtime/WEB_API.md` |
| 配置与元数据 | 字段行为配置 Web 暴露面 | 已补齐字段引用、字段保护、模块字段引用过滤、引用回填和公式规则配置入口。 | `configuration/WEB_API.md` |
| 配置与元数据 | 嵌套资源标准 Web 支撑 | 已抽出嵌套 CRUD、排序、启停支撑，避免配置接口退化成各自手写业务代码。 | `configuration/OVERVIEW.md` |
| 配置与元数据 | 菜单方案、菜单维护 Web 暴露面 | 已补齐 `/platform.menu_scheme` 和 `/platform.menu-scheme/{schemeId}/menus`，菜单消费入口继续保持 `/platform.menu/mine` 和 `/platform.menu/{menuId}/entry`。 | `configuration/WEB_API.md`、`page/delivery/WEB_API.md` |
| 配置与元数据 | 字段 UI 类型属性和字段映射配置入口 | 已补齐 `/platform.field_ui_type/{fieldUiTypeAlias}/attributes` 和 `/platform.field_ui_type/{fieldUiTypeAlias}/field-mappings`，页面 bootstrap 继续消费 resolved 字段 UI 类型配置。 | `configuration/WEB_API.md`、`page/delivery/WEB_API.md` |
| 运行态与权限 | 静态模块注解优先解析 | 已修正动作端点上下文解析，保证平台配置接口优先使用自身静态模块语义。 | `runtime/OVERVIEW.md`、`identity-permission/OVERVIEW.md` |
| 身份权限 | 当前用户上下文接口 | 已补齐 `GET /iam.auth/context`，用于前端和接手方识别当前用户身份。 | `identity-permission/WEB_API.md` |
| 身份权限 | 权限配置与授权工作台 | 已补齐角色批量授权/撤销、角色菜单矩阵和用户选择器入口；菜单授权继续复用模块 `menu` 动作。 | `identity-permission/WEB_API.md` |
| 业务自动化 | 选中导出 | 已补齐 `POST /{moduleAlias}/export/selected`，按选中 ID 导出并可叠加查询上下文。 | `business-automation/data-exchange/WEB_API.md` |
| 业务自动化 | 序列状态行级调整 | 已补齐 `POST /platform.code_sequence_state/adjust/{id}`，从序列状态行直接调整基线，并复用编码运维日志和下一值预览。 | `business-automation/code-rule/WEB_API.md` |
| 页面交付 | 通用用户偏好 | 已确认 `/platform.page-preference/{moduleAlias}` 可承载当前用户页面偏好和个人化导出配置。 | `page/WEB_API.md` |
| 治理 | 配置发布、包、导入导出 Web 暴露面 | 已补齐配置治理静态模块入口，开放配置包健康检查、发布、回滚、导出、导入 dry-run、导入草稿和草稿发布接口。 | `governance/WEB_API.md` |
| 治理 | 模板复用工具入口 | 已补齐从历史版本派生模板和实例化模板包入口；模板对象由客户端承接，实例化结果继续进入 dry-run 和发布链路。 | `governance/WEB_API.md` |

## 二次 neo 校准记录

本轮按配置与治理、运行态与页面交付、业务自动化三条线对照 neo 体系复盘。结论如下：

1. MuYun 已覆盖动态 CRUD、动作、引用、OpenAPI、页面 bootstrap、页面偏好、导入导出、编码规则运维、工作流运行态和生单/回写运行态主链路。
2. neo 的大一统 `ModuleWeb`、独立动态弹窗接口、专用生单 URL、专用批量删除 URL、直链附件访问等形态不吸收；MuYun 继续保持按平台对象聚合、动作主链路和 access envelope/ticket 边界。
3. 新确认的 P1 缺口集中在“配置态交接面”：菜单维护、字段 UI 类型属性/映射、生单/回写规则配置、工作流定义与发布配置。
4. P2 缺口主要是需要进一步判断边界的体验和模型能力：公式即时计算、元数据视图、模板复用管理、标准批量删除动作贡献。

## 待治理详情

### 配置与元数据

#### UI 配置管理 Web 暴露面

- 优先级：P0
- 状态：已完成
- 当前判断：`PlatformUiSetService`、`PlatformUiConfigService`、`PlatformUiConfigFieldService` 已通过平台配置 URL 暴露。
- 治理结果：提供 UI 配置集、配置项、字段配置的查询和维护入口，并通过专门发布入口控制发布状态。
- 回流目标：`configuration/WEB_API.md`、`page/delivery/WEB_API.md`

#### 查询模板管理 Web 暴露面

- 优先级：P0
- 状态：已完成
- 当前判断：`PlatformQueryTemplateService`、`PlatformQueryItemService` 已通过平台配置 URL 暴露。
- 治理结果：提供查询模板、查询项的轻量 CRUD、排序、启停和发布入口；已复用标准嵌套 Web 支撑。
- 回流目标：`configuration/WEB_API.md`、`runtime/WEB_API.md`

#### 字段引用、字段保护、校验规则等配置入口

- 优先级：P1
- 状态：已完成
- 当前判断：MuYun 已有字段引用、字段保护、模块字段引用过滤、引用回填和公式规则等稳定服务。
- 治理结果：补齐对应配置 Web 入口，并按元数据字段、模块关系和模块字段归属聚合；未引入新的字段行为模型。
- 回流目标：`configuration/OVERVIEW.md`、`configuration/WEB_API.md`

#### 菜单方案、菜单维护 Web 暴露面

- 优先级：P1
- 状态：已完成
- 当前判断：`MenuSchemeService`、`MenuService` 已存在，菜单消费入口、菜单 entry 和菜单授权视图也已存在，但缺少配置维护入口。
- 治理结果：补齐菜单方案维护入口 `/platform.menu_scheme`，以及方案内菜单树维护入口 `/platform.menu-scheme/{schemeId}/menus`；维护入口按平台静态模块和方案聚合，不照搬 neo 大一统模块维护接口。
- 回流目标：`configuration/WEB_API.md`、`page/delivery/WEB_API.md`

#### 字段 UI 类型属性和字段映射配置入口

- 优先级：P1
- 状态：已完成
- 当前判断：字段类型和字段 UI 类型目录已开放；`PlatformFieldUiTypeAttributeService`、`PlatformFieldUiTypeFieldMappingService` 已存在但缺少 Web 暴露面。
- 治理结果：按字段 UI 类型 alias 聚合属性和字段映射配置入口，新增和更新时以后端 URL 中的 `fieldUiTypeAlias` 为准，支撑页面 bootstrap 和字段渲染配置交接。
- 回流目标：`configuration/WEB_API.md`、`page/delivery/WEB_API.md`

#### 元数据视图、视图字段配置入口

- 优先级：P2
- 状态：已完成
- 当前判断：`MetadataViewService`、`MetadataViewFieldService` 已存在，并被模块定义编译链路引用；但它可能与 UI 配置集、UI 配置字段存在边界重叠。
- 治理结果：元数据视图是模块定义发布前的视图真相源，已按 `/platform.module/{moduleAlias}/metadata-relations/{relationId}/views` 和其下 `/{viewId}/fields` 补齐轻量维护入口；页面布局、区块、端侧差异仍归 UI 配置专题。
- 回流目标：`configuration/WEB_API.md`

### 身份权限

#### 角色授权批量矩阵操作

- 优先级：P1
- 状态：已完成
- 当前判断：当前已有角色、动作授权、数据权限等基础能力，单点授权和矩阵回显可用。
- 治理结果：补充按角色批量授权和批量撤销接口，矩阵回显继续使用 `/iam.role/permissionMatrix/{roleId}`。
- 回流目标：`identity-permission/WEB_API.md`

#### 角色菜单授权视图

- 优先级：P1
- 状态：已完成
- 当前判断：当前用户菜单和菜单剪枝能力已存在，菜单可见性由模块 `menu` 动作授权决定。
- 治理结果：补充角色菜单矩阵视图；不新增角色-菜单表，保存继续走标准模块动作授权。
- 回流目标：`identity-permission/WEB_API.md`

#### 用户选择器类查询接口

- 优先级：P1
- 状态：已完成
- 当前判断：用户 CRUD 已有基础，配置和授权场景需要轻量选择器。
- 治理结果：补充用户选择器查询，支持角色、组织、关键字和启用状态过滤，不替代用户管理 CRUD。
- 回流目标：`identity-permission/WEB_API.md`

### 业务自动化

#### 选中导出

- 优先级：P1
- 状态：已完成
- 当前判断：导出主链路已有，选中导出应作为同一导出能力的显式入口。
- 治理结果：补充 `POST /{moduleAlias}/export/selected`，按选中 ID 导出并可叠加查询上下文，继续复用现有导出执行链路。
- 回流目标：`business-automation/data-exchange/WEB_API.md`

#### 用户导出模板

- 优先级：P1
- 状态：暂不吸收
- 当前判断：当前只有个人化导出字段、排序等体验诉求，尚不足以抽象系统级导出模板模型。
- 治理结果：用户级导出配置先由页面偏好承载；只有出现可共享、可发布的系统模板场景时再单独治理。
- 回流目标：`business-automation/data-exchange/OVERVIEW.md`

#### 通用用户偏好

- 优先级：P1
- 状态：已完成
- 当前判断：当前偏好主要服务页面交付和当前用户体验，不改变平台配置真相源。
- 治理结果：复用 `/platform.page-preference/{moduleAlias}` 读取和保存当前用户偏好，可承载列表列宽、排序、筛选和个人化导出配置。
- 回流目标：`page/OVERVIEW.md`、`page/WEB_API.md`

#### 序列状态行级调整

- 优先级：P2
- 状态：已完成
- 当前判断：编码规则、预览、取号、台账、回收池已覆盖主链路；状态行级调整属于运维增强。
- 治理结果：补齐 `POST /platform.code_sequence_state/adjust/{id}`，从状态行发起基线调整，底层复用 `CodeOpsActionService` 的规则校验、治理日志和下一值预览。
- 回流目标：`business-automation/code-rule/WEB_API.md`

#### 生单、回写配置 Web 暴露面

- 优先级：P1
- 状态：已完成
- 当前判断：生单、回写运行态已由动作和写事件触发链路覆盖；`RecordGenerationRuleService`、`RecordWriteBackRuleService` 已存在，但配置维护 Web 面不足。
- 治理方向：按 MuYun 平台风格补齐规则配置入口，优先考虑模块聚合的独立静态模块或嵌套资源，不照搬 neo `ModuleWeb` 中的规则维护方法。
- 治理结果：补齐 `/platform.module/{moduleAlias}/generation-rules` 和 `/platform.module/{moduleAlias}/write-back-rules`，以规则树为配置交接单位，支持查询、查看、保存、启停、排序和删除；路径模块分别锁定生单 `sourceModuleAlias` 与回写 `triggerModuleAlias`。
- 回流目标：`business-automation/record-linkage/WEB_API.md`

### 工作流任务

#### 工作流定义与发布配置 Web 暴露面

- 优先级：P1
- 状态：已完成
- 当前判断：工作流运行态、管理端、委托和工作台入口已覆盖；`WorkflowDefinitionService`、`WorkflowVersionService`、`WorkflowPublishFacade` 已存在，但定义、版本、发布配置入口不足。
- 治理方向：补齐工作流定义、版本和发布配置的 Web 面，保持配置态和运行态分离。
- 治理结果：补齐 `/platform.module/{moduleAlias}/workflow-definitions` 和 `/platform.module/{moduleAlias}/workflow-definitions/{definitionId}/versions`，普通维护只面向草稿；发布、停用和归档统一走 `WorkflowPublishFacade`，保持模块动作贡献和定义状态同步。
- 回流目标：`workflow-task/WEB_API.md`

### 页面交互

#### 公式即时计算

- 优先级：P2
- 状态：已完成
- 当前判断：MuYun 已有公式规则配置、保存校验和运行态公式能力，但缺少面向页面 onchange/试算的只读 Web 入口。
- 治理结果：补齐 `/{moduleAlias}/formula/preview`，复用动态公式运行态，基于页面当前记录 payload 试算，不落库、不读取已有记录原值，返回试算记录、变更字段和结构化诊断。字段可见和可编辑范围仍由页面 bootstrap、动作配置和保存链路兜底。
- 回流目标：`page/interaction/WEB_API.md`、`runtime/WEB_API.md`

### 治理

#### 配置发布、包、导入导出 Web 暴露面

- 优先级：P1
- 状态：已完成
- 当前判断：治理专题已记录配置包、健康检查、版本发布、回滚、迁移 dry-run 等能力线索，已有服务门面可承接 Web 暴露。
- 治理结果：补齐 `platform.low_code_governance` 静态模块入口，开放配置包健康检查、发布、回滚、导出、导入 dry-run、导入草稿和草稿发布接口；模板复用入口按无状态工具单独治理。
- 回流目标：`governance/OVERVIEW.md`、`governance/WEB_API.md`

#### 模板复用管理入口

- 优先级：P2
- 状态：已完成
- 当前判断：`LowCodeModuleTemplateService` 已提供从版本生成模板和实例化包的服务能力，但缺少稳定模板仓库、持久化管理模型和 Web API。
- 治理结果：不建设模板仓库、模板市场、模板版本和持久化管理模型；补齐治理模块下的无状态工具入口，从已发布版本派生 `LowCodeModuleTemplate`，再由客户端提交模板和实例化参数生成新的 `MODULE_FULL` 包。实例化结果继续走导入预检和发布链路。
- 回流目标：`governance/OVERVIEW.md`、`governance/WEB_API.md`

### 运行态

#### 标准批量删除动作贡献

- 优先级：P2
- 状态：已完成
- 当前判断：MuYun 已有通用批量动作宿主 `/{moduleAlias}/{actionCode}/batch`，但是否需要平台默认贡献标准批量删除动作仍未明确。
- 治理结果：不新增 `/deleteBatch` 平行 URL；动态 CRUD 模块贡献标准 `batchDelete` 批量动作，通过 `/{moduleAlias}/batchDelete/batch` 执行，复用软删、动作目录、`delete` 权限、数据范围和运行事件链路。
- 回流目标：`runtime/WEB_API.md`

### 暂不吸收

#### 动态弹窗独立接口

- 当前判断：若动作模型和页面交互模型能够承载动态弹窗，则不单独扩展一套接口。
- 后续触发条件：动作模型无法描述弹窗参数、回填结果或权限语义。

#### 排序方案、打印模板

- 当前判断：neo 中有近似能力，但 MuYun 当前专题主链路不明确依赖。
- 后续触发条件：出现稳定业务场景，并能明确归属到配置、页面或数据交换专题。
