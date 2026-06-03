# 动静一体核心设计

## 目标

静态模块和动态模块是同一个平台的两种接入方式：

| 类型 | 定义来源 | 典型形态 | 必须共享的能力 |
| --- | --- | --- | --- |
| 静态模块 | Java 类、注解、DAO、Service | 内嵌平台业务、稳定领域业务 | CRUD、树、排序、引用、生命周期、建表、权限、审计 |
| 动态模块 | 运行态元数据 | 可配置业务对象 | CRUD、树、排序、引用、生命周期、建表、权限、审计 |

两者可以有不同的声明方式，但不应有两套数据操作、两套生命周期或两套平台能力。

## 能力目录

平台能力使用同一个目录表达，但按声明方式分层：

详细能力索引见 [平台能力清单](ABILITY_CATALOG.md)。本文只保留动静一体语义分层。

| 类型 | 含义 | 当前能力 |
| --- | --- | --- |
| 基线能力 | 平台实体天然具备，不需要业务或动态元数据逐项声明 | CRUD、软删除、生命周期、缓存 |
| 字段声明能力 | 会要求模型提供标准字段，并改变运行时 API 或行为 | 树、排序、引用 |
| 独立定义能力 | 由关系、引用依赖等独立配置声明，不适合塞进单个字段 | 父子聚合、引用依赖 |

静态接入可以通过接口、基类、注解或后续扫描结果表达能力；动态接入通过 `EntityDefinition`、字段定义、关系定义和引用定义表达能力。进入运行态后，它们都应落到同一套 ability 语义。

## 当前核心层

M1 先建设以下底座：

1. `EntityContract`：统一基础字段和生命周期字段。
2. `BaseDao`：统一静态模型的数据访问入口，默认基于 MuYunDatabase。
3. `CrudAbility`：统一插入、查询、更新、硬删除、分页、计数、平台内部链和业务扩展 hook。
4. `SoftDeleteAbility`：统一软删除过滤、软删除写入和忽略软删读取。
5. `SortAbility`：统一排序字段、列表排序和相邻移动。
6. `TreeAbility`：统一父子关系、祖先、后代、环保护和树位置校验；树天然具备同级排序语义。
7. `ReferenceAbility`：统一标题解析和引用选项读取，保留 RAW 读取入口。
8. `ChildAbility` / `ChildrenAbility`：统一父子聚合的子表插入、更新替换、自动装配和父删联动；父子写链路必须由调用方事务包裹。
9. `CacheAbility`：统一按 ID 和全量列表的本地缓存、写后失效和缓存对象副本隔离。
10. `ReferencerAbility`：声明当前模型引用了哪些来源模型，为后续引用依赖失效提供稳定入口。

动态模块进入 M1 后，应复用同一套语义，而不是另起一套动态 CRUD。`EntityCapability` 是能力目录，不只是动态开关；其中基线能力会自动归一到实体定义上，字段声明能力和独立定义能力仍由具体模型配置触发。

## 动态运行态接入

动态模块没有静态 Java Service，但运行态仍应落到同一条能力链路：

```text
DynamicRecordService
  -> DynamicEntityService implements CrudAbility<DynamicRecord>, SoftDeleteAbility<DynamicRecord>, TreeAbility<DynamicRecord>, ReferenceAbility<DynamicRecord>, CacheAbility<DynamicRecord>
  -> DynamicRecordDao implements BaseDao<DynamicRecord, String>
  -> MuYunDatabase
```

`DynamicRecordService` 是动态记录对外门面，负责按模块别名和实体编码定位运行态服务。
`DynamicEntityService` 是单个动态实体的运行态服务，承接 CRUD、软删除、树、排序、引用、父子聚合和引用依赖采集等平台能力，并按元数据能力开关或关系配置决定哪些入口可用。动态父子聚合按元数据关系配置接入同一套 `ChildRelation`，不另起一套动态子表逻辑。
`DynamicRecordDao` 只负责动态表 SQL 映射和数据访问，不承接生命周期、权限或业务编排。

生命周期分为平台内部链和业务扩展 hook。CRUD 标准入口先调度平台内部链，再调用业务 hook；父子聚合等平台能力挂在内部链上，业务覆盖 `afterInsert`、`afterUpdate`、`afterDelete`、`afterSelect` 时不需要手动调用 `super` 来维持平台能力正确性。业务 `after*` hook 是平台能力完成后的扩展点，不用于观察或拦截平台内部链执行前的 RAW 对象状态。

乐观锁由 Ability 层统一表达：更新和带实体删除以当前记录 `version` 作为 expected version，写入时递增到下一版本；冲突时抛出 `OptimisticLockException`。动态 DAO 和静态 Repository 都通过 MuYunDatabase 条件写入口执行 `id + version` 约束写入；静态删除走条件删除，动态软删走条件更新，避免在业务层手写并发控制。

缓存能力先作为显式能力挂载：服务实现 `CacheAbility` 后，标准 `select(id)` 可复用缓存，写链路在 `afterChanged` 之后由 CRUD 内部统一失效。静态服务默认缓存命名空间包含服务类、模块别名和 DAO 实例；动态运行态缓存命名空间在同一 `DynamicRecordRuntime` 内按模块和实体稳定，在不同运行态之间隔离。缓存对象必须通过 `copyForCache` 进出，避免调用方修改返回对象污染缓存内容。跨模型引用缓存失效已有本地进程内闭环：`ReferencerAbility` 采集引用依赖，目标记录变更时清理引用方缓存；跨节点治理后续再升级。

父子聚合与缓存的关系采用“缓存父记录、按次装配子记录”的策略。父记录命中缓存后仍会重新按关系读取 children，不把装配后的 children 写回父缓存；这样子表变更不需要反向清理父缓存，也避免父缓存被聚合状态污染。聚合装配出的 child 是否继续执行 child service 的完整 `afterSelect` 语义，需要单独设计递归边界，不能在无深度控制的情况下隐式递归。

## 模型定义边界

静态模型和动态模型可以有不同声明入口，但进入平台底座前应编译成统一定义。

1. 静态模型由 Java 类、注解、DAO 和 Ability 组合声明；业务开发者不应为了接入平台再手写 `ModuleDefinition`。
2. 动态模型没有 Java 类，才直接使用 `ModuleDefinition`、`EntityDefinition`、`FieldDefinition` 表达配置态。
3. `Definition` 是动态配置和平台内部编译结果，不是静态业务接入的额外负担。
4. 字段定义只表达字段事实、物理列、类型、约束和运行态必须知道的轻量字段行为。字典绑定可以作为编译后的运行态事实进入 `FieldDefinition`，但字典类目、项目维护和启用校验仍属于平台字典能力；默认值、过滤、影响等复杂字段行为应进入后续独立配置。
5. 模块运行时标识统一使用点分平台模块别名，例如 `iam.organization`。

## 建表路线

平台需要支持两条建表路径：

1. 静态模型：`StaticEntityTableMapper` 根据 Java 模型和注解编译成 `TableWrapper`，再由 `StaticSchemaService` 创建或校验表结构。
2. 动态模型：`DynamicTableMapper` 根据运行态元数据编译成 `TableWrapper`，再由 `DynamicSchemaService` 创建或更新表结构。

建表是平台责任，不是业务 service 责任。表名、字段名等 SQL 标识符必须走白名单校验。破坏性 DDL 必须有明确治理模式，不能作为普通保存动作的副作用。

静态和动态两条路径的共同交汇点是 MuYunDatabase 的 `TableWrapper`。平台标准字段由 `PlatformTableValidator` 统一校验，避免两条路径演化出不同的基础字段、主键、租户列或生命周期列。

静态 DAO 继承的 `ensureTable()` 保留为 MuYunDatabase 提供的开箱入口，适合单个 repository 自检或轻量场景。
平台级初始化、批量拉齐、dry-run、strict migration 和后续审计治理，应统一从 `StaticSchemaService` 进入。
业务 Service 不应为了保存普通业务数据而手工触发表结构变更。

## 能力挂载原则

后续工作流、编码规则、生成规则、回写、导入导出、附件、字典、权限、审计等能力，都应优先考虑能否同时挂载到静态模块和动态模块。

如果某个能力短期只能挂到一侧，应记录为阶段限制，不能把它包装成最终形态。

能力字段属于平台契约，不属于业务配置自由项。静态模型通过 Java 字段和注解声明能力字段；动态模型一旦开启字段声明能力，必须使用同一组标准字段名、列名和类型。例如树能力统一使用 `parentId` / `parent_id`，并自动包含排序能力的 `sortOrder` / `sort_order`；启停能力统一使用 `enabled` / `enabled`，但不属于基线字段，也不参与平台默认过滤。`tenantId` / `tenant_id` 属于所有平台实体的基础字段，不由业务元数据重复声明。后续工作流等需要标准字段的能力也按同一原则处理；软删除、生命周期和缓存属于基线能力，不要求在动态元数据中重复声明。

租户过滤属于默认 Ability 作用域，不属于业务 DAO 的手写条件。运行时存在当前租户时，插入会补齐 `tenantId`，默认查询、分页、计数、按 ID 读取、更新和删除都应在同一租户作用域内执行；无租户上下文时按系统态处理。

## 运行时边界

动态运行时可以缓存或编译模块快照，但快照只是性能优化和一致性边界，不应成为绕过模型、权限、审计、校验和数据访问契约的第二套内核。
