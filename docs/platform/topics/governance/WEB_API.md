# 配置治理 Web API

本文按当前已开放的治理 URL 梳理接口线索。配置治理入口围绕 `LowCodeModulePackage`、健康门禁、配置版本和导入导出组织，不复用动态运行态 `/{moduleAlias}` 业务数据接口。

## 平台入口

| URL 前缀 | 说明 |
| --- | --- |
| `/platform.low_code_governance` | 平台静态模块入口，进入平台动作和权限体系 |
| `/platform/low-code-governance` | 同一治理能力的路径风格别名 |

## 配置包与版本

| 方法 | URL | 功能点 |
| --- | --- | --- |
| `POST` | `/platform.low_code_governance/packages/health` | 对提交的模块配置包执行健康检查，返回 `LowCodeConfigHealthReport` |
| `POST` | `/platform.low_code_governance/packages/publish` | 发布 `MODULE_FULL` 配置包，生成不可变版本快照并切换当前版本 |
| `POST` | `/platform.low_code_governance/modules/{moduleAlias}/versions/{versionId}/rollback` | 将指定模块当前版本指针切换到历史版本 |
| `GET` | `/platform.low_code_governance/modules/{moduleAlias}/package` | 导出模块当前配置包 |
| `GET` | `/platform.low_code_governance/versions/{versionId}/package` | 导出指定历史版本配置包 |

## 导入迁移

| 方法 | URL | 功能点 |
| --- | --- | --- |
| `POST` | `/platform.low_code_governance/imports/dry-run` | 对提交的配置包执行导入预检，返回健康结果和冲突诊断，不写真实配置 |
| `POST` | `/platform.low_code_governance/imports/drafts` | 在预检不阻断时准备导入草稿，记录基线版本信息 |
| `POST` | `/platform.low_code_governance/imports/drafts/publish` | 发布导入草稿；若草稿基线版本已变化则由服务层拒绝 |

## 模板复用

| 方法 | URL | 功能点 |
| --- | --- | --- |
| `POST` | `/platform.low_code_governance/templates/from-version` | 基于已发布版本创建 `LowCodeModuleTemplate`；来源必须是包含元数据的完整模块包 |
| `POST` | `/platform.low_code_governance/templates/instantiate` | 使用客户端提交的模板和实例化参数生成新的 `MODULE_FULL` 配置包 |

## 当前边界

1. 当前开放的是已有治理门面的薄 Web 层，不在 Controller 中重写发布、回滚、冲突判断或健康检查逻辑。
2. 动态业务数据导入导出仍归属 `/{moduleAlias}/import`、`/{moduleAlias}/export`、`/{moduleAlias}/exchange/template` 等运行态接口，不等同于配置包治理。
3. 导入草稿当前是最小执行承接，不是服务端持久化草稿仓库；发布草稿时由客户端回传 draft，服务层仍会校验发布条件和基线版本。
4. 模板复用当前是无状态工具入口，不是模板仓库；服务端不保存模板、不提供模板列表、模板发布、模板版本或模板市场。
5. 治理对象的模块身份统一使用 `moduleAlias`；模块包身份同时包含 `applicationAlias + moduleAlias`。
6. `PAGE_ONLY` 包不能夹带元数据或自动化配置，是否可导入由预检结果说明。
