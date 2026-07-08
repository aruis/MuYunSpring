# 关联投影查询治理

本文记录 SQL 级关联投影查询的治理路线。它约束静态模块和动态模块后续如何共享读投影能力，避免把“关联字段展示”退化为业务层硬编码、查询后临时补值或动态运行态巨型服务。

关联投影用于解决列表、详情、导出、引用候选等读场景中“主记录可见，同时带出关联对象摘要字段”的问题。例如用户列表展示已绑定职员的工号、姓名、组织和部门。

## 目标

1. 静态 Java 模型和动态元数据模型最终复用同一套关联投影规划、SQL 组装、字段输出和权限边界。
2. 业务实体保持领域语义清晰，不因为列表展示需要把关联对象字段塞回主实体。
3. 关联投影由 resolved UI、查询模板、导出模板或显式读上下文驱动，只读取当前场景真正需要的字段。
4. SQL join 只承载适合列表分页的 `1:1` 和 `N:1` 关系；`1:N` 关系走聚合投影、子查询视图或关联列表，不直接放大主表行。
5. 查询、排序、字段保护、租户、软删和数据权限都必须经过平台统一契约，不允许绕过 Ability、权限和治理边界。

## 设计口径

当前已以 `RelationProjectionJoinDefinition` 作为来源无关的关联投影定义雏形。静态侧先通过 Java contributor 接入；动态侧未来应把元数据引用、模块关系和字典标题编译到同一种定义，而不是另起动态专用 planner。

当前运行态已沉淀 `RelationProjectionReadService` 作为读投影门面，统一编排必需字段收集、SQL plan 构建、分页执行和响应字段边界。静态列表入口只负责把静态模块编译成 `RecordReadProjection` 后调用该门面；动态侧未来不应复制静态 service 的编排，而应把动态元数据编译到同一组 definition/projection 后复用该门面。

推荐长期抽象：

```text
静态模块声明 / 动态元数据引用 / 字典标题 / 选择器标题
  -> RelationProjectionDefinition
  -> RecordReadProjection
  -> ProjectionQueryPlanner
  -> ProjectionQueryExecutor
```

静态侧只贡献 Java 声明，动态侧只把元数据编译成相同定义。运行时不应区分“静态 join planner”和“动态 join planner”两套内核。

## 可借鉴能力

参考动态表单成熟实现时，值得吸收的是机制，不是服务形态。

应吸收：

1. UI 和配置驱动投影字段选择。
2. 引用标题字段和 plus field 的稳定别名策略。
3. 先生成投影子查询，再在外层执行条件、排序和分页。
4. 递归引用时的深度限制、循环检测和自引用处理。
5. 强制投影字段机制，确保查询条件、排序和内部能力需要的字段不会被裁剪掉。

不宜照搬：

1. 把 UI 配置、动态服务状态、字典标题、影子表、引用 join 和查询 SQL 缓存都揉进一个巨型 service。
2. 让动态运行态成为唯一能力入口，导致静态链路需要绕进动态模型。
3. 用字符串 SQL 拼接替代平台统一的字段、标识符、权限和输出契约。

MuYunSpring 应保持“能力层平台化、静态链路优雅、动态链路编译接入”的方向。

## 短期补强

短期目标是把当前 SQL 投影能力收住边界，避免能力扩散后形成安全和分页风险。

1. SQL 投影只返回 `RecordReadProjection.outputFields + id + 必需内部字段`，不默认投影主表全部字段。
2. 增加字段输出保护测试，证明 SQL Map 输出路径不会绕过脱敏、隐藏和字段读策略。
3. `postReadTransforms` 非空时继续回退实体查询，直到 SQL 输出路径有统一字段保护执行器。
4. 关联定义通过 `cardinality` 约束列表 SQL join 默认只允许 `ONE_TO_ONE` 和 `MANY_TO_ONE`。
5. relation 字段默认只可展示；是否可过滤、可排序必须由投影定义或查询字段契约显式声明。
6. 为用户到职员的 `bound_employee` 补真实链路测试，覆盖租户过滤、软删中间表、软删职员、未绑定账号、分页和条件查询。
7. 数据库类型不应写死在执行器内；短期 PostgreSQL 可作为默认 provider，后续按真实数据源或运行态配置接入。

## 中期平台化

中期目标是把静态侧验证过的能力沉淀为动静共享的平台查询能力。

1. 继续把 `RelationProjection*` 从静态列表专用能力推进为来源无关的读投影平台能力，运行态入口以 `RelationProjectionReadService` 为准。
2. 静态 `RelationProjectionJoinContributor` 保持轻量业务声明，动态侧后续编译为相同 relation 定义。
3. 动态引用字段、模块关系和字典标题也编译为同一种 `RelationProjectionDefinition`。
4. `RecordReadProjectionPlanner` 统一决定输出字段、内部读取字段、强制投影字段和 post-read transform。
5. `ProjectionQueryPlanner` 统一负责 select、from、join、where 外层包装、排序、分页和参数绑定。
6. 引入 join depth、join count 和 cycle path 校验，禁止无界递归展开。
7. projection plan 可以缓存，但必须按静态模块定义版本、动态元数据运行态版本、UI 配置版本或查询模板版本失效。
8. relation 字段的筛选和排序纳入 `QueryDescriptor` 或后续 `ProjectionQueryDescriptor`，避免“投影了就能查”的隐式扩权。
9. 数据权限策略进入 relation 定义，区分“源记录可见即可展示有限摘要”和“目标模块也必须满足数据权限”。
10. 标准租户、软删和启停等 join 过滤应由平台根据 relation 目标实体能力生成，业务 contributor 只声明关系路径和业务过滤事实。

## 长期统一

长期目标是形成完整的读投影图，而不是只处理静态列表 join。

1. 列表、详情、导出、引用候选、选择器和关联视图共享同一套读投影规划，只通过 view、action 和 context 区分场景。
2. 动态 UI 配置和静态 UI 声明都先编译成 `ResolvedModuleUiDescriptor`，再进入统一读投影链路。
3. 动态引用字段、静态 relation、字典 title、对象选择器 title 和 plus field 都作为 projection graph 的节点。
4. 自引用和多层引用支持显式深度上限，并在响应诊断中说明被裁剪的路径。
5. `1:N` 关系提供聚合投影、计数投影、摘要子查询或独立关联列表，不直接参与主列表分页 join。
6. 字段保护、租户、软删、数据权限、审计和缓存失效都以 projection graph 为锚点统一治理。
7. 平台可为常见关系形态提供声明 DSL，业务模块只声明业务事实，不接触底层 SQL 细节。

## 当前边界

1. 当前能力先服务静态用户列表展示绑定职员信息，动态侧尚未接入同一 planner。
2. 当前 SQL 关联投影只适合 `1:1` 或 `N:1` 摘要展示，不承诺一对多展开。
3. 字段别名策略仍处于早期阶段。多个 relation 投影同名字段时，应优先使用稳定 relation 前缀或路径别名，避免响应字段冲突。
4. SQL 路径返回 Map 时必须谨慎处理字段保护。没有统一保护执行器前，遇到需要 post-read transform 的场景应回退实体查询。
5. 当前文档记录治理方向，不表示所有列出的长期能力已经实现。
