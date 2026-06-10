# M8 Temporary Governance

本文是 M8 与 neo 颗粒度对齐后的临时专项治理清单。它不替代 M8 稳定契约，也不重新扩大 M8 里程碑范围；只记录两类事项：

1. M8 页面交付闭环已经声明或已经进入运行主链路，但颗粒度还需要补齐的能力。
2. 原本属于 M9/M10，但如果当前实现 M8 收尾项时顺手补一个薄挂点、校验或文档，可以降低后续返工的事项。

本清单完成后应被回收到 `M8_STABLE_CONTRACT.md`、后续里程碑或技术债记录中，不长期保留为并行路线。

## 治理原则

1. 只吸收 neo 的业务颗粒和交付经验，不复制历史模型、表结构、字段袋和临时协议。
2. 保持 MuYun 的稳定身份：模块使用 `moduleAlias`，字段使用 `ModuleMetadataField.id`，页面入口使用菜单 bootstrap，运行态使用已发布 UI 配置和查询模板。
3. 所有页面能力必须回到动态运行态、动作权限、数据权限、审计、乐观锁、附件业务关系和事务边界，不建立绕过平台底座的页面内核。
4. M8 只补页面交付闭环的缺口；动态弹窗、复杂关联视图、局部信息编辑、模块任务和完整设计器仍按 M9/M10 承接。

## M8 必补项

| 编号 | 事项 | 目标 | 验收口径 | 边界 |
| --- | --- | --- | --- | --- |
| M8-G01 | UI 类型目录输出 | 让前端能从 bootstrap 或目录入口读取字段 UI 类型、属性 schema 和字段映射 | 已发布页面解析字段时，不只得到 `fieldUiTypeAlias`，还能得到对应属性、默认值和映射规则；非法 UI 类型继续 fail-fast | 不使用逗号串、`expendMap` 或 neo 字段袋 |
| M8-G02 | 默认 UI 配置脚手架 | 降低配置器创建 LIST/FORM/DETAIL/REFERENCE 配置的初始成本 | 提供显式服务或管理入口，可为 UI Set 生成 WEB/APP 默认配置和基础字段明细；结果仍需显式发布后才进入在线页面 | 不放进保存 hook 做隐藏副作用 |
| M8-G03 | `layoutJson` 最小语义校验 | 发布前拦住明显无效的页面布局结构 | 发布 UI 配置时至少校验 summary panel、引用候选、子表区块等 M8 已承诺结构；错误返回可定位到 layout path | 不提前做完整拖拽设计器 schema |
| M8-G04 | 快速查询 | 补齐列表页常用的 quick/fuzzy 查询体验 | 列表查询请求可携带快速查询值；后端只在已发布 LIST 配置声明的字段范围内编译为 Criteria，并继续叠加数据权限 | 不开放前端任意字段模糊搜索 |
| M8-G05 | 引用候选双上下文 | 让引用选择弹窗具备目标列表配置和来源表单校验 | 引用候选入口显式区分来源 `sourceUiConfigId` 与目标候选 `uiConfigId/queryTemplateId`；权限使用 `REFERENCE`，不复用普通 query 授权 | 不把引用生单重新塞回 resolve 的隐式副作用 |
| M8-G06 | 子表 UI 保存校验 | 将表单保存 wrapper 的 required/readOnly 语义覆盖到基础子表 | `children.{relationCode}` 提交时按已发布 UI 配置校验本次提交子行的必填；基础只读字段显式提交应拒绝 | 条件只读、动作级字段白名单进入 M9 |
| M8-G07 | 附件 fileserver 薄对接 | 明确 MuYunSpring 与文件服务的页面交付边界 | 提供 upload ticket、preview/download ticket 或 redirect 的后端入口；附件列表能得到前端展示所需 metadata 或快照 | MuYunSpring 不实现文件存储、预览、下载和物理删除策略 |
| M8-G08 | 保存与版本前端契约 | 把 wrapper、children、乐观锁和错误语义写成可交付口径 | OpenAPI 和 handoff 文档说明 `uiConfigId + record`、`children` 缺省/null/空数组/partial、update/delete version、409 `DYNAMIC_CONFLICT` | 不引入审批任务上下文；审批相关进入 M6/M9 |
| M8-G09 | 查重与附件错误码 | 让前端能稳定分支处理页面辅助错误 | 查重命中、附件票据失败、UI 校验失败、乐观锁冲突有稳定 code/status/message/traceId 语义 | 不回退到只靠中文 message 判断 |
| M8-G10 | 前端 handoff 索引 | 将 M8 后端契约整理为前端可消费文档 | 至少覆盖 bootstrap、query/summary/quick、save wrapper/children/version、reference、attachments、duplicate/error、OpenAPI 入口 | 不复制 neo 全量文档，只写 MuYun 稳定协议 |

## 顺手治理项

这些事项本质属于 M9/M10 或后续生产化。如果在实现 M8 必补项时改动面很小，可以补薄挂点或测试；如果会牵引新模型、新执行器或新运行态，不在 M8 做。

| 编号 | 事项 | 可顺手做的最小形态 | 正式归属 |
| --- | --- | --- | --- |
| M8-O01 | 关联视图区块 layout 预留 | `layoutJson` 校验允许识别关联视图区块类型，但运行态仍返回未实现或忽略 | M9 关联视图 |
| M8-O02 | 局部信息编辑动作挂点 | OpenAPI/动作结果保留字段范围和目标 UI 配置挂点，不实现独立保存动作 | M9 局部信息编辑 |
| M8-O03 | 动态弹窗结果挂点 | 保持动作结果中的 dialog 结构稳定，必要时补 schema 测试 | M9 动态弹窗 |
| M8-O04 | 模块任务完成项挂点 | 页面 bootstrap 可预留 task panel 区块类型，不做任务定义、检查和工作流联动 | M9 模块任务 |
| M8-O05 | 跨页列表导航扩展点 | 当前页导航保持不变；可在导航会话模型中预留 query snapshot 标识 | M9 列表导航增强 |
| M8-O06 | 标准 OpenAPI adapter 预留 | 当前 `DynamicOpenApiDocument` 保持稳定；命名和 schema 避免阻塞后续 OpenAPI 3.0 转换 | M10/后续 |
| M8-O07 | 配置版本治理预留 | 发布服务保持统一门面，便于后续引入版本表、差异比对和回滚 | M10 生产化治理 |
| M8-O08 | SQL 级投影和汇总预留 | query/summary API 不绑定内存实现细节，后续可下推 SQL 聚合和投影 | 后续性能治理 |

## 明确不做

1. 不引入 neo 式 `ModuleEnter` 作为 M8 必需模型。菜单入口已能承载当前页面打开场景；同模块多入口、组织/部门差异入口和命中条件进入后续结构化入口 scope。
2. 不复制 `metadataDetailId`、`uiIndex`、逗号组织/部门集合、历史 accessType 和旧权限 SQL 字段。
3. 不把 `REFERENCE` 作为菜单页面模式；引用候选属于字段交互和候选列表能力。
4. 不把 M3 view descriptor 扩展成第二套页面配置真相源；M8 UI 配置仍是页面交付真相源。
5. 不在 M8 建完整可视化设计器、动态弹窗执行器、关联视图运行引擎、局部信息编辑动作和模块任务系统。
6. 不把 MuYunSpring 做成文件服务；文件二进制、预览、下载、token、promote 和物理删除策略归文件服务，MuYunSpring 只维护业务关系和页面票据入口。

## 完成口径

1. 每个 M8 必补项至少有服务或 Web contract 测试覆盖；涉及真实查询、保存、附件、引用和发布边界时优先补真实路径 contract。
2. 文档只保留稳定协议、字段语义、错误语义和后续边界，不记录执行流水。
3. 完成本清单后，能合并进 `M8_STABLE_CONTRACT.md` 的内容应迁移过去；纯后续项转入 M9/M10 或技术债记录。
