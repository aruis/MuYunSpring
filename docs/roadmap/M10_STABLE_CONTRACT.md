# M10 稳定契约

M10 的稳定边界是低代码配置从“能配能跑”进入“可治理、可迁移、可复用、可验收”。当前阶段只承诺模块级配置包、健康门禁、版本快照、迁移预检、最小导入草稿、模板实例化和演示专题验收，不承诺完整配置中心、审批流、字段级 diff、跨版本合并或真实业务系统落地。

## 核心单元

低代码生产化治理的核心单元是 `LowCodeModulePackage`，以 `applicationAlias + moduleAlias` 作为稳定身份。包内按 bundle 分层：

1. `METADATA`：元数据、字段、关系、引用和能力声明。
2. `PAGE`：列表、表单、详情、查询和页面交付配置。
3. `INTERACTION`：关联视图、动作区块、弹窗、局部编辑和模块任务。
4. `ENTRY`：菜单入口、页面模式、客户端和默认上下文。
5. `AUTOMATION`：编码、生成、回写、导入导出和工作流挂点。
6. `dependencyManifest`：模块、动作、字典、工作流、文件服务和外部依赖声明。
7. `publishManifest`：包协议、来源版本、来源环境、导出人和导出时间。

包模式固定为：

1. `MODULE_FULL`：完整模块包，必须包含 `METADATA`，可发布为配置版本。
2. `PAGE_ONLY`：页面迁移包，只允许 `PAGE/INTERACTION/ENTRY`，不直接发布为当前完整版本。
3. `TEMPLATE`：模板包，必须包含 `METADATA`，需实例化为 `MODULE_FULL` 后再进入发布链路。

## 健康门禁

`LowCodeModuleHealthService` 聚合 `LowCodeModuleHealthChecker`，输出 `LowCodeConfigHealthReport`。`FAIL` 阻断发布和导入草稿，`WARN` 允许继续但必须保留诊断。

当前稳定 checker：

1. `LowCodeModulePackageHealthChecker`：校验包结构、包模式、bundle 内容和依赖声明基本形态。
2. `LowCodeModuleBundleIdentityHealthChecker`：校验 bundle 顶层 `module/moduleAlias` 与包身份一致。
3. `LowCodeModuleDependencyHealthChecker`：复用依赖诊断，输出缺 resolver 或缺依赖问题。

健康检查只承诺包级身份和依赖事实，不深度解析 UI、工作流、自动化配置语义。

## 版本治理

`LowCodeModuleConfigPublishFacade` 发布 `MODULE_FULL` 包时生成 `LowCodeModuleConfigVersion`：

1. `packageSnapshotText` 保存发布时完整包快照。
2. `packageHash` 保存快照 hash。
3. `summaryJson` 保存包模式、包含 bundle、健康状态和问题数。
4. `currentVersion` 表达当前在线版本。
5. 回滚只切换当前版本，不改写底层 UI、查询、菜单或数据结构。

已发布快照不可变；历史版本仍保持 `PUBLISHED` 事实。

## 迁移与导入

`LowCodeModulePackageExchangeService` 提供导出、解析和 dry-run：

1. 可导出当前版本或指定历史版本包 JSON。
2. 可解析包 JSON 为 `LowCodeModulePackage`。
3. dry-run 复用健康检查，并输出冲突列表。
4. `MODULE_FULL` 指向已有模块时返回 `WARN`，表示后续需要显式发布新版本。
5. `PAGE_ONLY` 要求目标模块已有当前版本，否则阻断。
6. `TEMPLATE` 指向已有模块时阻断。

依赖分两类：

1. `MODULE/ACTION/DICTIONARY` 是平台默认可解析依赖，缺 resolver 或 required 依赖缺失会阻断。
2. `WORKFLOW/FILE_SERVICE/EXTERNAL` 当前为 manifest-only，缺 resolver 只返回 `WARN`；若后续提供显式 resolver，则 required 缺失会阻断。

`LowCodeModulePackageImportService` 是最小导入门面：

1. `prepareDraft` 在 dry-run 不阻断时生成内存 draft，记录包、健康/冲突结果和当前基线版本。
2. `publishDraft` 只允许 `MODULE_FULL` draft，且发布前校验当前版本仍等于基线版本。
3. `PAGE_ONLY/TEMPLATE` 只到 prepare/dry-run 层，不直接发布。
4. 当前不持久化草稿、不批量写真实配置表、不做字段级 diff、审批流或合并策略。

## 模板复用

`LowCodeModuleTemplateService` 从已发布 `MODULE_FULL + METADATA` 版本创建模板。实例化时：

1. 请求必须提供合法 `applicationAlias` 和新 `moduleAlias`。
2. `applicationAlias/module/moduleAlias` 是保留参数，不能被模板参数覆盖。
3. 只替换 bundle 顶层 `module/moduleAlias`。
4. 标题和显式参数只写入 `METADATA` bundle。
5. 依赖 manifest 默认保持不变，不做隐式重写。
6. 实例化结果是 `MODULE_FULL` 包，可继续 dry-run、健康检查和发布。

模板首期不做深层 JSON 重写、模板继承、模板市场、自动升级或跨版本参数迁移。

## 演示验收

`sales.contract` 是平台演示业务包，不是具体客户业务系统。它用于验证 M10 治理闭环能表达一个中等复杂模块包，并穿过发布、导出、迁移 dry-run、回滚、模板实例化和依赖诊断链路。

演示包可以包含主子表、引用、列表/表单/详情、查询、关联视图、局部编辑、模块任务、生成/回写、导入导出、权限动作声明和依赖 manifest。M10 不在这里重做 M5 授权运行判定，也不引入业务 service、业务流水或专题数据模型。

## 测试锚点

稳定契约由以下测试锁定：

1. `LowCodeModulePackageValidatorTest`
2. `LowCodeModuleHealthServiceTest`
3. `LowCodeModuleConfigPublishFacadeTest`
4. `LowCodeModulePackageExchangeServiceTest`
5. `LowCodeModulePackageImportServiceTest`
6. `LowCodeModuleTemplateServiceTest`
7. `M10LowCodeDemoBusinessAcceptanceTest`

默认验证命令仍是：

```bash
./gradlew test
```

