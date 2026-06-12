# 平台能力清单

本文是能力索引，不替代代码和测试。它用于让接手者快速判断：某个业务问题应优先复用哪项平台能力、需要什么模型前提、使用时有哪些边界。

能力实现主要位于 `muyun-ability`。静态业务通过模型契约、基类、注解和 ability 接口接入；动态业务通过元数据能力声明编译到同一套运行语义。

## 能力使用原则

1. 业务 Service 优先表达能力组合，不重复写 CRUD、租户、软删、树、排序、引用、子表等通用逻辑。
2. 静态链路优雅优先。动态链路由平台维护，静态业务会大量编写，能力设计要优先降低静态 Service 的样板代码。
3. 能力启用后字段语义必须稳定。动态侧开启能力时应使用平台标准字段，不允许运行态随意改名。
4. 能力文档只记录稳定边界；细节行为优先由 contract test 锁住。

## 基础链路

| 能力 | 核心解决问题 | 主要依赖 | 注意点 |
| --- | --- | --- | --- |
| `CrudAbility` | 统一插入、查询、分页、更新、删除、乐观锁、生命周期 hook 和平台内部链 | `EntityContract`、`BaseDao<T, String>` | 所有业务能力的主入口。平台内部链先于业务扩展 hook 执行，业务覆盖 `after*` 不应破坏平台能力。 |
| `AbstractAbilityService` | 给静态 Service 提供 `moduleAlias`、`modelClass`、`dao` 和常用校验辅助 | `CrudAbility`、`BaseDao` | 标准静态 Service 通常继承它；不要把普通非业务工具继续塞进这里。 |
| `StandardBusinessService` | 给普通静态业务收口保存前规范化、通用保存校验、插入校验和更新校验 hook | `AbstractAbilityService` | 适合不需要系统态或租户态写入门禁的普通业务，业务优先覆盖 `normalizeBeforeMutation`、`validateBeforeSave`、`validateBeforeInsert`、`validateBeforeUpdate`，避免重复覆盖多个 CRUD hook。 |
| `SystemStandardBusinessService` | 在系统态写入校验基础上收口系统配置保存 hook 模板 | `SystemManagedAbility` | 适合租户等明确要求系统态维护的配置；系统态业务不要直接用 `StandardBusinessService` 绕过系统上下文。 |
| `BaseDao` | 屏蔽静态 DAO、动态 DAO 和底层数据访问差异 | MuYunDatabase 默认实现 | 生命周期、权限、软删等不应下沉到 DAO；DAO 只负责数据访问。 |

## 数据状态与作用域

| 能力 | 核心解决问题 | 主要依赖 | 注意点 |
| --- | --- | --- | --- |
| `SoftDeleteAbility` | 统一软删除写入、默认过滤和忽略软删读取 | `EntityContract.deleted/deletedAt` | 默认读写隐藏已删除数据；确需读取已删除数据时使用明确的 RAW/ignore 入口。 |
| `EnableAbility` | 统一启用、停用、启用校验和启用条件构造 | `EnabledCapable.enabled` | 启停不是默认过滤条件；业务需要时显式调用 `enabledCriteria` 或 `requireEnabled`。 |
| `SystemManagedAbility` | 限制系统级配置只能在系统态维护 | `TenantContext.system(reason)` | 适合租户、应用、平台模块等系统态配置；写入前可做 `normalizeBeforeMutation`。 |
| `TenantActiveScopedAbility` | 限制租户内业务写入必须处于有效租户上下文 | `TenantContext.currentTenantId()`、`ActiveTenantVerifier` | 写入前会要求租户上下文并校验租户有效；适合组织、部门等租户内业务。 |
| `TenantActiveScopedService` | 收口租户内业务 Service 对 `ActiveTenantVerifier` 的样板依赖 | `AbstractAbilityService`、`TenantActiveScopedAbility` | 后续租户内静态 Service 优先继承它，而不是重复声明 verifier 字段和转发方法。 |
| `TenantStandardBusinessService` | 在租户有效性校验基础上收口租户内业务保存 hook 模板 | `TenantActiveScopedService` | 适合部门、职员等租户内标准业务，业务只补规范化和业务校验，不重复写租户校验链路。 |
| `GlobalScopedAbility` | 表达不受当前租户过滤影响的全局配置读取 | `SoftDeleteAbility` | 适合租户自身、平台全局配置等；不要用于普通租户业务绕过隔离。 |

## 结构能力

| 能力 | 核心解决问题 | 主要依赖 | 注意点 |
| --- | --- | --- | --- |
| `SortAbility` | 统一排序字段、完整 scope 重排、相邻移动 | `SortCapable.sortOrder` | `reorder` 要覆盖完整排序 scope，避免局部重排造成数据不一致；非树业务可用 `sortScopeByFields` / `validateSortScopeByFields` 声明排序 scope。 |
| `TreeAbility` | 统一树形父子关系、根节点、子节点、祖先后代、环保护 | `TreeCapable.parentId`，天然包含排序能力 | 树天然支持同父级排序；有业务 scope 的树应使用显式 scope 查询、`scopedTreeCriteria`、`validateTreeSortScopeByFields` 和 scoped `moveInTree`。 |

## 引用与聚合

| 能力 | 核心解决问题 | 主要依赖 | 注意点 |
| --- | --- | --- | --- |
| `ReferenceAbility` | 给被引用模型提供标题、选项、投影读取能力 | `EntityContract`、`TitledCapable` 或 `@TitleField` | 标题解析不应触发目标业务的完整变更逻辑；找不到标题字段应 fail-fast。 |
| `ReferencerAbility` | 给引用方声明引用依赖，支持标题回填、投影和缓存失效 | `@ReferenceTo` 等静态引用声明或动态引用配置 | 引用依赖是跨模型缓存失效和展示解析的基础，不应在业务 Service 里手写散落逻辑。 |
| `ChildAbility` | 给子表 Service 提供子记录选择、排序和软删兼容入口 | 子模型 `EntityContract` | 子表自身仍是标准实体能力组合，不应脱离 CRUD 链路。 |
| `ChildrenAbility` | 给父表 Service 提供父子聚合插入、替换、装配和父删联动 | `ChildRelation`、静态子关系注解或动态关系配置 | `null` 子列表表示不改子表，空列表表示清空；父子写链路应由调用方事务包裹。 |
| `CascadeDeleteChildAbility` | 标记子表支持父删级联批量删除 | `ChildAbility` | 当前是轻量语义入口，具体删除仍复用子表标准删除能力。 |

## 字段治理

| 能力 | 核心解决问题 | 主要依赖 | 注意点 |
| --- | --- | --- | --- |
| `FieldProtectionAbility` | 统一字段加密、签名校验和输出脱敏的运行语义 | 静态字段注解或动态字段保护元数据、`FieldCryptoProvider`、`FieldSigner` | 写入时临时转换为存储态并立即恢复业务对象；动态侧保护配置独立于字段基础表，签名伴生字段由平台生成。 |

## 缓存、事件与执行支撑

| 能力 | 核心解决问题 | 主要依赖 | 注意点 |
| --- | --- | --- | --- |
| `CacheAbility` | 统一按 ID 和全量列表缓存、写后失效、事务内绕过、对象副本隔离 | `CrudAbility`、`CacheRegistry`、`TenantContext` | 缓存命名空间包含服务、模块和 DAO；跨模型引用失效依赖 `ReferencerAbility`。 |
| `PlatformAbilityDispatcher` | 调度平台内部 after 链，避免业务 hook 覆盖破坏平台能力 | CRUD 生命周期 | 新能力如果需要挂入 CRUD 内部链，应优先考虑这里，而不是要求业务手动调用 `super`。 |
| `RuntimeEventPublisher` 等事件组件 | 提供 after-commit 运行事件发布和监听边界 | `TransactionScopeSupport`、事件 listener | 平台审计只记录必要上下文；工作流等专题应保留自己的流水。 |

## 选型提示

| 业务场景 | 推荐能力组合 |
| --- | --- |
| 系统态维护的全局配置，如租户、应用 | `SystemManagedAbility + GlobalScopedAbility + EnableAbility + SortAbility` |
| 租户内树形业务，如组织机构、部门 | `TenantStandardBusinessService + SoftDeleteAbility + EnableAbility + TreeAbility + ReferenceAbility` |
| 可被其他模型选择的基础资料 | `ReferenceAbility`，必要时叠加 `EnableAbility`、`SortAbility` |
| 引用了其他模型且需要标题/投影展示的业务 | `ReferencerAbility` + 静态引用注解或动态引用配置 |
| 主子表聚合保存和读取 | 父 Service 实现 `ChildrenAbility`，子 Service 实现 `ChildAbility` |
| 读多写少且需要减少重复查询的模型 | `CacheAbility`，并确认写后失效和引用依赖失效测试覆盖 |

## 后续维护规则

1. 新增能力接口、能力基类或重要能力支撑组件时，同步补一行清单。
2. 只有能力语义、依赖或接入方式稳定后才写入本文；探索过程和临时方案不要写进来。
3. 如果能力已废弃或合并，及时删除或改名，保持清单准确。
