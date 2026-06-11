# M9 Stable Contract

M9 的稳定边界是“低代码页面之外的业务交互可配置、可授权、可检查”，而不是新增一套绕过动态模块动作、权限和数据范围的前端编排系统。

## 收口范围

M9 第一批能力包括关联视图、局部信息编辑、动态弹窗动作和模块任务完成项。当前四个能力块达到可收口边界：

1. 关联视图提供动态详情记录到目标记录集合的查询入口，并复用 M8 查询请求协议。
2. 局部信息编辑通过托管动态 action 承载，按发布 UI 配置限制字段范围，继续使用动态乐观锁更新。
3. 动态弹窗只作为动作参数补录入口，初始化和提交都回到动态 action 主链路。
4. 模块任务完成项由详情 UI 配置声明，后端 check 复用关联视图或查询模板判定，不引入审批待办状态。

M9 当前仍只承诺动态低代码交互闭环。静态业务可以复用动作、权限、数据范围、审计和底层能力，但不要求同步建设静态表单或静态页面配置。

## 主链路契约

1. 页面入口仍通过 `/platform.menu/{menuId}/entry` bootstrap；M9 只扩展 `resolvedConfig` 中的交互区块，不替代 M8 菜单、UI 配置、查询模板和动态 Web 主链路。
2. 详情关联区块通过 `layoutJson.blocks[].type=associationView` 声明，bootstrap 下发 `associationBlocks`。关联查询入口为 `POST /{moduleAlias}/view/{id}/associations/{viewCode}/query`，请求体沿用 M8 `WebQueryRequest`，目标记录输出按目标模块 LIST 语义脱敏。
3. 局部信息编辑动作使用内置 executor key `muyun.localEdit`。动作自身负责动作授权和记录数据范围校验；执行器只允许提交已发布 `localEdit` UI 配置中的主关系字段，并要求 `record.version` 参与乐观锁。
4. 动态弹窗动作使用 `EntityActionExecutorType.DIALOG`。`executorKey` 可用 `dialogKey#submitActionCode` 绑定后续业务动作；初始化结果只返回弹窗元信息和提交入口，不直接执行业务副作用。
5. 动作区块通过 `layoutJson.blocks[].type=dialog|action|localEdit` 声明，bootstrap 下发 `actionBlocks`。boot 层会按当前用户可见动作裁剪 action block，前端仍需按后端返回的 action availability 渲染记录级可用性。
6. 模块任务区块通过 `layoutJson.blocks[].type=taskPanel` 声明，bootstrap 下发 `taskBlocks`。任务检查入口为 `POST /{moduleAlias}/view/{id}/tasks/check`，先校验当前租户、动作授权和当前记录 `VIEW` 数据范围，再执行完成项判定。
7. 模块任务完成判定首期支持 `ASSOCIATION_VIEW` 和 `QUERY_TEMPLATE`。查询模板判定可通过 `externalRecordIdKey` 显式注入当前记录 ID；`MANUAL` 任务只返回 `UNKNOWN`，不在平台保存办理人、办理状态、委托、加签或审批待办语义。
8. M9 不允许新增前端私有 handler、脚本执行器或模块专用控制器绕过动态 action。所有业务动作必须能回到平台动作上下文、权限判定、数据范围、动态记录运行态和审计链路。

## 前端 Handoff 索引

| 业务板块 | 入口/字段 | 稳定语义 |
| --- | --- | --- |
| 关联区块 | `resolvedConfig.associationBlocks` | 来自已发布默认详情 UI 配置的 `associationView` block，包含 `viewCode`、标题、目标 UI 配置、默认查询模板和查询路径。 |
| 关联查询 | `POST /{moduleAlias}/view/{id}/associations/{viewCode}/query` | 查询当前记录的关联目标列表，复用列表查询的分页、排序、查询模板、外部值和复杂 `criteria`。 |
| 局部编辑按钮 | `resolvedConfig.actionBlocks[type=localEdit]` | 前端按 action block 渲染按钮；点击后仍走动态 action 执行入口，不直接调用标准 update。 |
| 局部编辑提交 | `POST /{moduleAlias}/{actionCode}/execute` | payload 中提交 `record.id/version/values` 和 `uiConfigId`；后端按发布 localEdit 配置校验字段范围。 |
| 弹窗按钮 | `resolvedConfig.actionBlocks[type=dialog]` | 前端按 DIALOG action 初始化弹窗；弹窗只收集后续动作所需参数。 |
| 弹窗初始化 | `POST /{moduleAlias}/{dialogActionCode}/execute` | 返回 `dialogKey/actionCode/submitActionCode/submitPath/recordId/refreshOnSuccess/redirectTo` 等弹窗契约字段。 |
| 弹窗提交 | `submitPath` | 提交仍走动态 action 主链路，目标 action 不能再是 DIALOG，避免弹窗嵌套动作链失控。 |
| 通用动作区块 | `resolvedConfig.actionBlocks[type=action]` | 用于普通动作按钮编排，只表达页面位置和 actionCode，不表达业务执行逻辑。 |
| 模块任务区块 | `resolvedConfig.taskBlocks` | 来自 `taskPanel` block，包含 `key/title/checkType/associationViewCode/queryTemplateId/externalRecordIdKey/diagnosticPath`。 |
| 模块任务检查 | `POST /{moduleAlias}/view/{id}/tasks/check` | 返回每个任务的 `COMPLETE/PENDING/UNKNOWN`、命中数量和诊断路径；不返回不可见记录信息。 |

## 配置边界

1. `layoutJson.blocks` 是页面交互区块声明入口，不是任意组件注册表。M9 只识别 `associationView`、`dialog`、`action`、`localEdit` 和 `taskPanel`。
2. 发布 UI 配置时会校验已知 block 的必要字段、动作类型、关联视图可查询性和任务检查引用的已发布查询模板。
3. bootstrap 只解析当前默认 UI 配置中面向当前客户端的区块。未发布配置、其他客户端配置和不可见 action 不进入在线响应。
4. `taskPanel` 的完成判定不新增条件语言；复杂条件通过 M8 查询模板和查询项树表达，关系存在性通过关联视图表达，后续生成关系、审批状态和工作流上下文也应作为平台事实接入。
5. `diagnosticPath` 是前端跳转或辅助定位提示，不是安全入口。真实访问仍以后端接口权限和数据范围为准。

## 权限与数据范围

1. 关联查询使用 `QUERY` 动作权限，目标数据仍按目标模块查询链路和数据范围输出。
2. 局部编辑、弹窗提交和普通动作执行使用动态 action 自身的授权、记录级数据范围和 action availability。
3. 模块任务检查使用 `VIEW` 动作权限，并在执行 check 前校验当前记录对当前用户可见；不可见记录不能通过任务状态、关联数量或查询模板命中数被探测。
4. bootstrap 的 action block 裁剪只解决“动作是否对当前用户可见”；记录级可用性仍由 action availability 或执行入口兜底。

## 测试锚点

M9 主链路由以下测试共同锁定：

| 链路 | 测试 |
| --- | --- |
| 关联区块、动作区块和任务区块 bootstrap | `MenuEntryBootstrapContractTest`、`DynamicPageBootstrapWebControllerTest` |
| UI 配置发布校验 | `PlatformUiConfigurationServiceContractTest` |
| 关联视图查询入口 | `DynamicRecordWebControllerTest`、`DynamicRecordServiceTest` |
| 局部信息编辑动作 | `DynamicLocalEditActionExecutorTest`、`DynamicRecordWebControllerTest` |
| 动态弹窗动作 | `DynamicRecordServiceTest`、`DynamicRecordWebControllerTest`、`DynamicOpenApiGeneratorTest` |
| 模块任务检查 | `PlatformModuleTaskCheckServiceTest`、`DynamicModuleTaskWebControllerTest` |

默认收口验证命令是：

```bash
./gradlew test
```

## 后续增强池

以下能力已经明确为后续项，不阻塞 M9 收口：

1. 模块任务完成判定接入生成关系事实、审批状态、工作流上下文和公式结果。
2. 任务检查补充真实 Web/IT 越权记录拒绝回归，覆盖拦截器、当前用户和数据范围策略完整链路。
3. 关联区块支持更丰富的 display mode、空态、跳转策略和目标页导航上下文。
4. 动态弹窗支持更完整的字段 UI 配置、默认值回填和提交后局部刷新策略，但不能演化成任意页面引擎。
5. action block 与 record action availability 的前端状态刷新协议可以继续增强，包括执行后刷新区块、刷新关联列表、刷新任务状态和条件跳转。
6. 发布校验与区块解析后续可进一步共用规范化模型，减少 `layoutJson` 字段扩展时的漂移风险。
7. M10 统一承接配置版本、发布审批、回滚、迁移、低代码样板模块和真实业务专题落地。
