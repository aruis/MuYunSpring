# 配置治理专题

## 能力定位

配置治理专题覆盖低代码模块从“能配能跑”进入“可治理、可迁移、可复用、可验收”的生产化能力。它的核心单元是低代码模块包 `LowCodeModulePackage`，不是单个 UI JSON、单张元数据表或整库配置。

本专题与平台配置专题分工明确：平台配置负责应用、模块、元数据、菜单、字典等基础配置；配置治理负责配置包、版本快照、发布回滚、配置包迁移、模板复用和健康门禁。

## 核心对象

| 对象 | 作用 | 关键边界 |
| --- | --- | --- |
| `LowCodeModulePackage` | 低代码生产化治理的模块级载体 | 以 `applicationAlias + moduleAlias` 作为稳定身份 |
| bundle 分层 | 按配置类型拆分包内容 | 元数据、页面、交互、入口、自动化分别归档，不混成一坨 JSON |
| `dependencyManifest` | 声明迁移和发布前需要满足的外部事实 | 覆盖模块、动作、字典、工作流、文件服务和外部依赖 |
| `publishManifest` | 记录包协议、来源版本、来源环境和导出信息 | 用于跨环境迁移和版本追踪，不替代版本表 |
| 健康报告 | 发布、导入和模板复用前的结构化门禁 | `FAIL` 阻断，`WARN` 可继续但必须保留诊断 |
| 配置版本 | 已发布模块包的不可变快照 | 当前版本只表达线上指针，历史版本保留发布事实 |
| 导入草稿 | dry-run 之后的最小导入执行承接 | 当前为内存 draft，不持久化、不做复杂合并 |
| 模板 | 从已发布版本生成的可复用样板包 | 模板实例化后生成 `MODULE_FULL` 包，再进入治理链路 |

## 模块包分层

`LowCodeModulePackage` 当前按以下 bundle 分层：

| 分层 | 内容 |
| --- | --- |
| `METADATA` | 元数据、字段、关系、引用和能力声明 |
| `PAGE` | 列表、表单、详情、查询和页面交付配置 |
| `INTERACTION` | 关联视图、动作区块、弹窗、局部编辑和模块任务 |
| `ENTRY` | 菜单入口、页面模式、客户端和默认上下文 |
| `AUTOMATION` | 编码、生成、回写、业务数据导入导出配置和工作流挂点 |

包模式固定为：

1. `MODULE_FULL`：完整模块包，必须包含 `METADATA`，可发布为配置版本。
2. `PAGE_ONLY`：页面迁移包，只允许 `PAGE/INTERACTION/ENTRY`，不直接发布为当前完整版本。
3. `TEMPLATE`：模板包，必须包含 `METADATA`，需实例化为 `MODULE_FULL` 后再进入发布链路。

## 健康门禁

健康检查由 `LowCodeModuleHealthService` 聚合 `LowCodeModuleHealthChecker`，输出 `LowCodeConfigHealthReport`。当前稳定门禁保持克制：

1. 包结构、包模式、bundle 内容和依赖声明基本形态。
2. bundle 顶层 `module/moduleAlias` 与包身份一致。
3. 依赖 manifest 的 resolver 诊断和缺失依赖诊断。

健康检查当前只承诺包级身份和依赖事实，不深度解析 UI、工作流或自动化配置语义。后续补强时应继续增加独立 checker，避免把治理逻辑堆成单体判断。

## 发布与回滚

`LowCodeModuleConfigPublishFacade` 发布 `MODULE_FULL` 包时先执行健康检查。`FAIL` 阻断发布，`PASS/WARN` 可生成配置版本。

发布版本保存：

1. `packageSnapshotText`：发布时完整包快照。
2. `packageHash`：快照 hash。
3. `summaryJson`：包模式、包含 bundle、健康状态和问题数。
4. `currentVersion`：当前在线版本指针。

回滚首期只切换当前版本，不改写底层 UI、查询、菜单配置，不自动执行数据迁移，也不接入工作流审批。已发布快照不可变，历史版本仍保持 `PUBLISHED` 事实。

## 迁移与导入

`LowCodeModulePackageExchangeService` 承接导出、解析和 dry-run：

1. 从当前在线版本或指定历史版本导出模块包 JSON。
2. 解析模块包 JSON 为 `LowCodeModulePackage`。
3. dry-run 复用健康检查，并输出冲突列表。
4. `MODULE_FULL` 指向已有模块时返回 `WARN`，表示后续需要显式发布新版本。
5. `PAGE_ONLY` 要求目标模块已有当前版本，否则阻断。
6. `TEMPLATE` 指向已有模块时阻断。

依赖按两类处理：

1. `MODULE/ACTION/DICTIONARY` 是平台默认可解析依赖；required 依赖缺 resolver 或缺失会阻断，optional 依赖只诊断告警。
2. `WORKFLOW/FILE_SERVICE/EXTERNAL` 当前为 manifest-only，缺 resolver 只返回 `WARN`；后续若提供显式 resolver，required 缺失仍应阻断。

`LowCodeModulePackageImportService` 当前只提供最小导入门面：`prepareDraft` 在 dry-run 不阻断时生成内存草稿，`publishDraft` 校验基线版本未变化后，只允许 `MODULE_FULL` 草稿交给发布门面生成配置版本。当前不持久化草稿、不批量写真实配置表、不做字段级 diff、审批流或合并策略。

## Web 暴露面

配置治理通过 `platform.low_code_governance` 静态模块入口开放。当前 Web 层只承接已有治理门面，覆盖配置包健康检查、发布、回滚、当前/历史版本导出、导入 dry-run、导入草稿和草稿发布；具体 URL 见 `WEB_API.md`。

模板服务当前还没有稳定模板仓库和管理模型，暂不开放独立 Web API。模板实例化结果仍可作为 `LowCodeModulePackage` 进入 dry-run 和发布链路。

## 模板复用

`LowCodeModuleTemplateService` 从已发布 `MODULE_FULL + METADATA` 版本创建模板。模板底包使用 `TEMPLATE` 模式，实例化时生成新的 `MODULE_FULL` 包。

首期实例化规则：

1. 请求必须提供合法 `applicationAlias` 和新 `moduleAlias`。
2. `applicationAlias/module/moduleAlias` 是保留参数，不能被模板参数覆盖。
3. 只替换 bundle 顶层 `module/moduleAlias`。
4. 标题和显式参数只写入 `METADATA` bundle。
5. 依赖 manifest 默认保持不变，不做隐式重写。
6. 实例化结果继续走 dry-run、健康检查和发布链路。

模板首期不做深层 JSON 重写、模板继承、模板市场、自动升级或跨版本参数迁移。

## 演示业务包边界

`sales.contract` 是平台演示业务包，不是具体客户业务系统。它用于反压验证治理链路能表达一个中等复杂模块包，并穿过发布、配置包导出、迁移 dry-run、回滚、模板实例化和依赖诊断链路。

演示包可以覆盖合同主子表、客户引用、状态字典、列表/表单/详情、查询、关联视图、局部编辑、模块任务、生成/回写、业务数据导入导出配置、权限动作声明和依赖 manifest。治理专题不在这里重做授权运行判定，也不引入业务 service、业务流水或专题数据模型。

## 边界说明

1. 本专题不负责应用、模块、元数据、菜单、字典的基础维护；这些归属配置专题。
2. 本专题不负责运行态 CRUD、动作执行、引用解析和 OpenAPI 消费；这些归属运行态专题。
3. 本专题不负责页面配置细节和前端 bootstrap；这些归属页面交付专题。
4. 本专题不承诺完整配置中心、审批流、字段级 diff、跨版本合并或真实业务系统落地。
5. 后续真实业务专题接入时，应复用模块包、健康门禁、版本、迁移和模板链路，但业务规则和业务流水要按业务边界单独承接。
