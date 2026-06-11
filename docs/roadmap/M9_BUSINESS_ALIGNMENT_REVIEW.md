# M9 Business Alignment Review

本文按 M9 业务面与外部成熟系统做颗粒度对齐复盘。结论只用于校准 MuYun 后续规划，不复制历史表名、包结构、字段袋或临时协议。

## 总体结论

MuYun M9 第一批能力已经覆盖外部成熟系统里最关键的交互主链路：关联视图查询、局部信息编辑动作、动态弹窗动作、模块任务完成项 check。当前差距主要不在“能不能跑”，而在“配置态模型、托管来源、运行态结果协议和诊断颗粒”。

需要补齐的方向可以分成两类：

1. M10 前置补强：不改变 M9 已收口基础契约，但应在进入大规模业务专题前补齐，否则前端和业务配置会继续靠约定拼装。
2. M10 治理/业务专题：涉及配置版本、设计器体验、审批/工作流消费、生成/回写事实联动和业务专题落地，不回灌 M9 基础契约。

## 对齐矩阵

| 业务板块 | 成熟系统颗粒 | MuYun 当前状态 | 缺口判断 | 建议归属 |
| --- | --- | --- | --- | --- |
| 模块上下游关系 | `relationOverview` 返回 upstream/downstream，服务关联视图 path 配置 | 动态元数据已有引用、关系和 association view descriptor，但没有平台级关系总览接口 | 缺设计器辅助能力，不阻塞运行态 | M10 配置器体验 |
| 关联视图配置 | `ModuleRelatedView` 持久化配置，含 `targetModuleAlias/path/queryTemplateId/linkListUiSetId/linkFormUiSetId/viewMode/isTop/rootQueryMapping` | 通过动态模块 descriptor + UI `associationView` block 暴露，支持按 relation/reference 查询目标列表 | 缺独立设计态模型、多跳 path、置顶、FORM 唯一命中、目标 UI Set 和 rootQueryMapping | M10 前置增强，优先级高 |
| 关联视图查询映射 | `rootQueryMapping` 支持 AND/OR 树，叶子值来自来源字段、系统变量、常量；公式来源属于后续预留 | M8 查询模板支持复杂条件树；M9 关联视图仅支持固定关联关系 + 请求 criteria | 缺“来源记录字段 -> 目标查询条件”的声明模型 | M10 前置增强，优先级高 |
| 关联视图运行态 | 先执行 path/queryTemplate/mapping，再向前端返回锁定目标 id 的 query context | MuYun 后端直接返回目标分页结果 | MuYun 更直接，但不利于前端复用目标列表配置和诊断 target id 集合 | M10 视图诊断/前端体验 |
| 局部信息编辑配置 | `ModuleInfoEdit` 独立模型，绑定 FORM UI Set，含宽高、启用、系统级；同步托管 `info_edit_*` action | `muyun.localEdit` executor + `localEdit` block + action 自身 executorKey | 缺独立信息编辑配置模型、byAction 反查、弹窗尺寸和托管 action 同步 | M10 前置增强，中高 |
| 局部编辑保存 | 专用动作保存入口不复用标准 edit 权限，绑定表单限制字段，不允许附件夹带 | 动态 action 主链路执行 localEdit，按发布 UI 配置限制主表字段并要求 version | 核心安全语义已对齐；URL 形态不同但符合 MuYun 动作统一路线 | 已覆盖，保留当前路线 |
| 动态弹窗入口 | 独立 init/submit 双阶段入口、handler registry、标准 init/submit DTO | MuYun 使用 DIALOG action 初始化，`submitActionCode/submitPath` 回到动态 action 主链路 | MuYun 更统一，但缺专门的 handler registry 和 init/submit 双阶段类型体系 | M10 视业务复杂度增强 |
| 弹窗上下文 | 有 `sourceType`、`recordId`、`selectedIds`、`uiConfigId`、当前 query，并归一为 `targetIds` | MuYun action request 已有 recordId/ids/criteria/page/sorts/payload 等，但 DIALOG 结果契约未明确 source/query 语义 | 缺稳定“触发来源 + 当前列表查询上下文”协议 | M10 前置补强，中 |
| 弹窗 UI 协议 | init 返回 `dialog/view/buttons/meta`，view 可含 config/dataSource/formulaRules/conditionRules/saveValidationRules | MuYun DIALOG 返回 `DynamicActionDialog`，主要是 dialogKey/title/actionCode/submitActionCode/submitPath/recordId/refreshOnSuccess/redirectTo | 缺按钮、多意图提交、字段/规则配置和详情展示协议 | M10 业务驱动增强 |
| 弹窗刷新跳转 | submit 返回 `refresh.list/detail/redirectToDetail/redirectRecordId/redirectModuleAlias` | MuYun 有 `refreshOnSuccess` 和 `redirectTo` | 缺列表/详情/跨模块详情跳转的结构化刷新策略 | M10 前置补强，高 |
| 模块任务定义 | 有 `ModuleTask` 持久化模型，含 taskCode/taskType/originType/originId/isManaged/syncStatus/isSystem/sort | MuYun `taskPanel` 是 UI block，无独立任务定义表 | 当前只适合页面轻量完成项，不足以承载托管任务和跨流程消费 | M10 前置增强，高 |
| 任务引导 | `ModuleTaskGuide` 支持 GENERATE_RULE、MODULE_INFO_EDIT、OPEN_FORM、OPEN_LIST、FOCUS_FIELD | MuYun task block 只有 `diagnosticPath` | 缺可执行引导模型，前端只能跳诊断路径 | M10 前置增强，高 |
| 任务判定 | `ModuleTaskCheck` 支持 FORMULA、QUERY_EXISTS、RELATED_QUERY_EXISTS、GENERATED_QUERY_EXISTS、expectedCount、审批状态过滤 | MuYun 支持 ASSOCIATION_VIEW、QUERY_TEMPLATE、MANUAL UNKNOWN，返回 matchedCount | 已有最小可查验链路；缺公式、生成关系、审批状态、expectedCount、多 check AND | M10 前置增强，高 |
| 任务评估结果 | 返回顶层 `passed`、任务 `COMPLETED/INCOMPLETE/NO_CHECK`、checks 明细、guides；无 check 不视为通过 | MuYun 返回 `COMPLETE/PENDING/UNKNOWN`、matchedCount、message | MuYun 结果较薄，`UNKNOWN` 类似 NO_CHECK 但缺 passed 汇总和 check 明细 | M10 前置补强，中高 |
| 托管来源同步 | 由 GenerateRule、ModuleInfoEdit 自动维护托管任务/guide/check，并保护托管子项 | MuYun 未做托管任务同步 | 缺生成/信息编辑与任务完成项的自动治理 | M10 前置增强，高 |
| 审批/工作流消费 | 文档定义 MODULE_TASK 节点消费任务，但任务本身不存待办状态 | MuYun 明确不把模块任务变成审批待办 | 方向一致；具体消费链路未实现 | 后续工作流专题 |

## 明确需要补充的事项

### P1：弹窗刷新策略结构化

MuYun 现在的 `refreshOnSuccess/redirectTo` 只能表达粗粒度刷新。结构化 `refresh.list/detail/redirectToDetail/redirectRecordId/redirectModuleAlias` 更适合真实业务动作，例如提交后刷新当前详情、刷新列表、跳到新生成记录详情、跨模块跳转。

建议在 M10 前置补强中补齐，不必复制历史系统的独立 `/dialog/init|submit` 路线，可以扩展 MuYun 的 `DynamicActionDialog` 或 action result body：

```text
refresh = {
  list: boolean,
  detail: boolean,
  redirectToDetail: boolean,
  redirectRecordId: string,
  redirectModuleAlias: string
}
```

### P1：模块任务从 UI block 升级为可治理定义

`taskPanel` 适合页面第一版展示，但无法承载成熟业务系统已验证的业务颗粒：taskCode、originType、originId、isManaged、syncStatus、guides、checks、expectedCount、托管保护。后续若要让生成、局部编辑、审批节点或业务阶段消费任务，必须有独立平台定义。

建议 M10 前置战役建立 MuYun 风格的任务定义模型。不要直接复制历史表结构，但应吸收以下语义：

1. 任务定义与运行待办分离。
2. guide 与 check 分离。
3. 无 check 返回 NO_CHECK/UNKNOWN，不视为系统通过。
4. 来源托管任务可见、可停用、可补充配置，但托管核心字段受保护。

### P1：关联视图设计态能力

MuYun 当前 association view 更接近运行态 descriptor 和详情区块，不具备成熟配置器颗粒。缺少多跳 path、rootQueryMapping、置顶、FORM 唯一命中和目标 UI Set，会影响真实设计器使用。

建议 M10 前置战役补平台级关联视图定义或在动态 metadata 中补等价能力，重点吸收：

1. 模块上下游关系总览。
2. 关联 path，多跳和首步 inbound 规则。
3. rootQueryMapping，来源字段/系统变量/常量到目标查询条件；公式来源留作后续能力，不作为首批设计依据。
4. LIST/FORM 展示模式和目标 UI 配置。

### P1：任务判定接入生成关系与 expectedCount

MuYun M7 已有生成来源影响关系，M9 任务 check 还没有用上。`GENERATED_QUERY_EXISTS + generatedImpactScope` 这类判定正好对应 MuYun 的生成关系事实。

建议优先补：

1. `GENERATED_RELATION` 或等价 checkType。
2. `expectedCount`，当前 matchedCount 只能让前端自行判断。
3. 多 check AND 汇总和顶层 `passed`。

## 可以延后但应记录的事项

1. 动态弹窗多按钮提交、FORM/DETAIL view config、formulaRules/conditionRules/saveValidationRules：业务复杂度上来后再做，避免把 M9 弹窗变成任意页面引擎。
2. ModuleInfoEdit 独立配置模型和 byAction 反查：MuYun 当前 action + UI block 能跑，但设计器体验不足；进入真实配置器时补。
3. 关联视图运行态返回 target id 锁定 query context：MuYun 直接返回分页结果更简单，若前端要复用目标模块列表页能力，再引入。
4. 审批/工作流 MODULE_TASK 节点：方向可以吸收，但应归入后续工作流专题，不塞回 M9。
5. 真实 Web/IT 越权记录拒绝回归：当前已有 controller 单测覆盖 `requireRecordActionScope` 调用，后续补完整链路更稳。

## 不建议照搬的事项

1. 动态弹窗独立 `/dialog/init|submit` Controller 不一定适合 MuYun。MuYun 已建立动态 action 主链路，继续让 DIALOG action 回到 action runtime 更符合长期边界。
2. `ModuleInfoEdit` 的 `info_edit_*` URL 形态不必照搬。MuYun 的 `executeAction` 入口已经能承载动作授权和局部保存。
3. 任务设计不要提前接入审批待办字段。成熟系统已明确区分模块业务任务与审批待办，MuYun 应继续保持这个边界。
4. rootQueryMapping 可以吸收语义，但应复用 MuYun M8 `Criteria` / `QueryTemplate` 树和字段解析门面，不新增一套平行查询语言。

## 建议的后续战役划分

如果继续推进业务颗粒对齐，建议拆成四场，不要混在 M10 大治理里一次性铺开：

1. **M10 前置补强战役**：结构化刷新策略、任务评估结果汇总、expectedCount、完整越权 Web/IT 回归。目标是在不重开 M9 基础契约的前提下，把第一批交互能力补到“前端可稳定联调”。
2. **关联视图设计态战役**：模块上下游关系总览、关联视图定义、path、rootQueryMapping、目标 UI 配置和 FORM/LIST 模式。
3. **模块任务定义战役**：任务/guide/check 独立模型，生成关系 check、公式 check、托管来源同步和托管保护。
4. **弹窗与信息编辑体验战役**：弹窗多按钮、FORM/DETAIL view schema、规则节点、ModuleInfoEdit 独立配置和 byAction 运行态反查。只在真实业务动作需要时推进。

这四场都归入 M10 前置能力或 M10 早期治理，不再回灌 M9 基础契约。
