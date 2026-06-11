# 配置治理 Web API

本文只按当前代码中能确认的 URL/接口状态梳理配置治理入口。已核实 `muyun-boot`、`muyun-platform`、`muyun-dynamic`、`muyun-iam` 下的 Controller 和 Spring mapping；当前未看到低代码配置治理的独立 Web Controller，治理能力先由服务门面承接，不提前编造 URL。

## 治理服务门面

| 能力 | 当前服务线索 | Web API |
| --- | --- | --- |
| 包校验与健康检查 | `LowCodeModulePackageValidator`、`LowCodeModuleHealthService` | 当前未暴露独立 Web Controller，先由服务门面承接 |
| 配置版本发布与回滚 | `LowCodeModuleConfigPublishFacade` | 当前未暴露独立 Web Controller，先由服务门面承接 |
| 配置包交换与迁移预检 | `LowCodeModulePackageExchangeService` | 当前未暴露独立 Web Controller，先由服务门面承接 |
| 导入草稿 | `LowCodeModulePackageImportService` | 当前未暴露独立 Web Controller，先由服务门面承接 |
| 模板复用 | `LowCodeModuleTemplateService` | 当前未暴露独立 Web Controller，先由服务门面承接 |

当前代码中存在 `/{moduleAlias}/import`、`/{moduleAlias}/export`、`/{moduleAlias}/exchange/template` 等动态业务数据导入导出和交换模板入口，但这些归属运行态/业务数据交换，不是 `LowCodeModulePackage`、配置版本、配置包 dry-run、导入草稿或模板复用的配置治理 URL。

## 建议的 URL 归属口径

后续如果补 Web Controller，建议围绕治理对象建独立平台入口，不复用动态运行态 `/{moduleAlias}` 业务根路径：

| 接口维度 | 建议功能点 |
| --- | --- |
| 模块包健康检查 | 对上传或指定版本的模块包执行健康检查，返回 `LowCodeConfigHealthReport` |
| 配置版本发布 | 发布 `MODULE_FULL` 包，生成不可变版本快照并切换当前版本 |
| 配置版本回滚 | 将模块当前版本指针切换到指定历史版本 |
| 配置包导出 | 按当前版本或历史版本导出模块包 JSON |
| 配置包导入 dry-run | 解析上传包，执行健康检查和冲突诊断，不写真实配置 |
| 导入草稿 | 在 dry-run 不阻断时准备草稿，并在基线版本未变化时发布 |
| 模板复用 | 从已发布版本创建模板，按新 `applicationAlias/moduleAlias` 实例化模块包 |

上述只是后续 Controller 设计口径，不代表当前已有 URL。

## 与配置专题的接口边界

| 归属 | 接口对象 |
| --- | --- |
| 配置专题 | 应用、模块、元数据、字段、模块-元数据关系、菜单、字典的基础维护和发布到运行态 |
| 配置治理专题 | 模块包、健康门禁、配置版本、回滚、导出、导入 dry-run、导入草稿和模板复用 |
| 运行态专题 | 发布后的 `/{moduleAlias}` 描述、查询、保存、动作、引用和 OpenAPI |
| 页面交付专题 | 页面 bootstrap、列表/表单配置、查询模板、页面偏好和附件页面关系 |

## 命名提醒

1. 治理对象的模块身份统一使用 `moduleAlias`。
2. 模块包身份必须同时满足 `applicationAlias + moduleAlias`。
3. `PAGE_ONLY` 包不能夹带元数据或自动化配置。
4. Web 层补齐前，文档只记录服务门面能力，不把服务方法臆造为 URL。
