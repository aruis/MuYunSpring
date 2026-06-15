# 数据迁移重设计方案（草案）

> 本文档是**方案 review 载体**，不是稳定文档。方向定稿后，内容合并回 `DATA_MIGRATION.md` 并删除本文档。记录讨论过程违反 `AGENTS.md` 文档边界，故不长期保留。

## 背景

PR #2 的初版数据迁移框架被作者判定为"代码逻辑语义的迁移"，不是"业务语义的迁移校准"，与 module 体系脱节。三个具体脱节点：

1. `AbstractMigration.getAlias()` 返回自由字符串（`"foo"`），不经 `PlatformNameRules` 校验，与 `PlatformModule` 无任何关系，不进权限/审计/运行口径。
2. 与 governance 治理链路平行：governance 已声明覆盖"可迁移"（`docs/platform/topics/governance/OVERVIEW.md`），有健康门禁、dry-run、版本快照；数据迁移另起了一套无门禁、无预演的链路。
3. `MigrationBootstrap`（`@Order(100)`）与 `StaticModuleDefinitionRegistrar`（无 `@Order`）无契约关系，仅"恰好都在启动时跑"。

作者已拍板两个方向：

- **Q1：身份收敛成 `moduleAlias`，挂 module 业务维度。**
- **Q2（方案 B）：独立链路，身份挂 module；不复用 governance 配置态治理链路，数据迁移自建运行态门禁与 dry-run，与 governance 平行而非嵌套。**

理由：governance 是**配置态**治理（`LowCodeModulePackage` / bundle / 发布指针），数据迁移是**运行态**校准（按 `moduleAlias` 跑一次性数据脚本），承载对象与门禁语义不同。强行合并复用度有限，且会污染 governance 已稳定的配置包边界。

本草案聚焦剩余的唯一开放问题：**收敛后，Migration 的声明形态如何挂到 module 体系。**

## 现状关键事实

| 对象 | 关键事实 |
| --- | --- |
| `moduleAlias` | = `PlatformModule.id`，全局唯一业务身份。`PlatformNameRules.requireModuleAliasInApplication` 强制 `moduleAlias = <applicationAlias>.<segment>`，长度上限 128。 |
| `StaticModuleDefinition` | record，组件：`applicationAlias` / `moduleAlias` / `title` / `parentModuleAlias` / `Set<EntityCapability> capabilities` / `List<StaticModuleActionDefinition> actions`。构造器对 `moduleAlias` 做 `requireModuleAliasInApplication` 校验。 |
| `StaticModuleDefinitionRegistrar` | `ApplicationRunner`（无 `@Order`），系统态遍历所有 `StaticModuleDefinition`，upsert `PlatformModule` + `PlatformModuleAction` 行。 |
| `StaticModuleDefinitionScanner` | 从带 `@PlatformStaticModule` 的 bean 反射出 `StaticModuleDefinition`；actions 从 bean 实现的 marker 接口（`CrudWeb`/`TreeWeb`/...）和 `@ActionEndpoint` 方法推导。 |
| `AbstractMigration` | `@Component`，两个抽象方法 `getAlias()`（自由 String）/ `getMigrateSteps()`。无生命周期钩子，无健康检查 hook。 |
| `MigrationBootstrap` | `@Component @Order(100) ApplicationRunner`，系统态 ensureTable + 跑所有 `AbstractMigration`。 |
| governance 链路 | `LowCodeModuleHealthChecker` SPI / `LowCodeConfigHealthReport` / `LowCodePackageDryRunResult` / `LowCodeModuleConfigPublishFacade` 全部围绕 `LowCodeModulePackage`（**仅动态模块**）。静态模块注册路径**今天本就没有治理门禁**。 |
| `PlatformUniqueIndexes` | 把任何非 id/tenant_id 的 unique 列改写成 `(tenant_id, alias)` 复合索引；`tenant_id IS NULL` 时退化，需 partial unique index 兜底。 |

## 重设计的两个核心约束

无论选 A 还是 B，以下两条都成立：

**约束 1（Q1 已定）：迁移身份必须是合法 `moduleAlias`。**
`AbstractMigration.getAlias()` 的返回值必须通过 `PlatformNameRules.requireModuleAliasInApplication` 校验，即 `<applicationAlias>.<segment>` 形态（例如 `iam.organization`，不再是 `foo`）。`migration_record.alias` 列语义从"自由标识"变为"所属 module 的业务身份"。

**约束 2（Q2 方案 B 已定）：独立运行态门禁与 dry-run，与 governance 平行。**
不复用 governance 的健康检查器 SPI（它绑定 `LowCodeModuleHealthContext`/`LowCodeModulePackage`）；数据迁移自建一套运行态语义的门禁（校验目标字段/引用/租户作用域）和 dry-run（执行校验但不写数据）。

## 开放问题：Migration 声明形态

下面两种形态都满足约束 1、2，区别在"迁移与 module 的关联是字符串约定还是结构契约"。

### 形态 A：独立 `@Component` + `moduleAlias` 校验（轻量）

`AbstractMigration` 仍是独立 `@Component`，但 `getAlias()` 强制走 `moduleAlias` 校验；`MigrationBootstrap` 执行前校验对应 `PlatformModule` 已注册。

```java
public abstract class AbstractMigration {
    /** moduleAlias, must pass PlatformNameRules.requireModuleAliasInApplication. */
    public abstract String getAlias();           // 例如 "iam.organization"
    public abstract String getApplicationAlias(); // 例如 "iam"
    public abstract List<MigrateStep> getMigrateSteps();
}
```

`MigrationBootstrap` 在跑某个 migration 前，`moduleService.select(getAlias()) == null` 则记 WARN 并跳过（或 FAIL，见后文）。

- **利：**
  - 改动面最小：只动 `AbstractMigration` 签名、`MigrationBootstrap` 加 module 校验、`migration_record.alias` 语义文档化。`StaticModuleDefinition` / Registrar / Scanner 完全不动。
  - 迁移类与 module 声明解耦：一个 module 可以不挂任何迁移；一个迁移挂到哪个 module 只由 `getAlias()` 决定，不要求 module 侧改任何代码。
  - 风险局部化：失败影响范围限于 `muyun-migration`。
- **弊：**
  - 关联仍是字符串约定：`getAlias()` 返回值和 `PlatformModule.id` 之间没有编译期约束，只有运行期校验。typo（`iam.orginization`）要等到 Bootstrap 跑才发现。
  - "迁移属于 module"这件事在代码结构上不可见——module 声明和迁移声明是两个独立文件，读者得跨文件拼关系。
  - 不直接呼应作者"跟 module 体系融合"的措辞——本质是"校验对齐"而非"结构融合"。

### 形态 B：迁移收进 `StaticModuleDefinition` 声明链（结构融合）

把迁移作为 `StaticModuleDefinition` 的并列组件（与 `actions` 并列），由 Scanner 反射、Registrar 统一注册。`StaticModuleDefinitionScanner` 新增：检测 bean 上带 `@MigrationStep` 的方法（或 bean 实现 `MigrationCapable` 接口），反射成 `List<MigrateStepDefinition>` 挂到 `StaticModuleDefinition`。

```java
public record StaticModuleDefinition(
    String applicationAlias,
    String moduleAlias,
    String title,
    String parentModuleAlias,
    Set<EntityCapability> capabilities,
    List<StaticModuleActionDefinition> actions,
    List<StaticModuleStepDefinition> migrateSteps   // 新增
) { ... }
```

`MigrationBootstrap` 不再注入 `List<AbstractMigration>`，改为注入 `StaticModuleDefinitionRegistrar` 产物（或共享的 `List<StaticModuleDefinition>`），按 `moduleAlias` 取 `migrateSteps` 跑。

迁移的声明形态收敛成"静态 module 的 Controller 同时是一个迁移载体"：

```java
@PlatformStaticModule(application = "iam", alias = "iam.organization")
@RestController
class OrganizationController extends WebSupport<OrganizationService>
        implements CrudWeb<...>, MigrationCapable {   // 新接口

    @Override
    public List<MigrateStepDefinition> migrateSteps() {
        return List.of(
            step(1, () -> service.bumpAllAgesBy(10)),
            step(2, () -> service.createSeed("seed", 30))
        );
    }
}
```

- **利：**
  - 结构契约：迁移与 module 在同一个声明点，编译期就绑定（实现 `MigrationCapable` 的 bean 必然挂在一个 `@PlatformStaticModule` 下），关系不再靠字符串对齐。
  - 真正"融合"：呼应作者原话。迁移声明成为 module 声明的一等组成部分，跟 actions 同构。
  - 统一注册序：迁移随 `StaticModuleDefinitionRegistrar` 跑，module 注册和迁移注册由同一条 `ApplicationRunner` 链驱动，`@Order` 问题自然消解（迁移执行仍可单独 `@Order`）。
  - 为动态侧预留对称形态：未来动态 module 的数据校准可以挂在动态配置包上，与静态侧同构。
- **弊：**
  - 平台基础设施变更：`StaticModuleDefinition` 是平台核心 record，扩组件要同步改 Scanner / Registrar / 所有构造点 / 测试。影响面横跨 `muyun-boot`。
  - 强耦合：迁移执行依赖 module 上下文（需要 `PlatformModuleService` 已就绪、module 已 upsert）。如果迁移要在 module 注册"之前"或"独立于 module"运行，这条路就别扭。
  - "迁移必须挂在一个静态 module 上"成为硬约束。平台自举 seed 数据、跨 module 的全局校准（如果将来有）不好挂——但这些用例当前未明确，作者方向是"module 业务维度"，可接受。
  - 工作量大、回归风险高：`StaticModuleDefinition` 改动会牵动现有所有静态 module 的声明链。

### 两者对比

| 维度 | 形态 A（独立 + 校验） | 形态 B（收进声明链） |
| --- | --- | --- |
| module 关联强度 | 运行期字符串校验 | 编译期结构契约 |
| 改动面 | `muyun-migration` 内 | 跨 `muyun-boot` 平台核心 |
| 呼应作者"融合" | 弱（对齐而非融合） | 强（结构融合） |
| 风险 | 低、局部 | 高、横跨平台核心 |
| 未来动态侧对称性 | 需另设动态迁移载体 | 静/动态可同构 |
| 迁移独立于 module 运行 | 支持 | 不支持（强约束） |

## 运行态门禁与 dry-run（两种形态共用，独立链路）

无论 A 还是 B，数据迁移的自建门禁与 dry-run 落在同一套设施上（不复用 governance）：

- **运行态健康检查**：执行前校验 `getMigrateSteps()` 引用的目标（字段是否存在、引用关系是否成立、租户作用域是否匹配）。失败策略（FAIL 阻断 vs WARN 跳过）作为配置项。
- **dry-run**：执行校验逻辑但不写数据；返回每个 step 的校验结果，类似 `LowCodePackageDryRunResult` 但针对运行态数据校准语义。

**注意：这部分是 TD-021 的能力补齐，不在本次"身份收敛"重构的最小集内。** 本次重构最小集是：alias 收敛 + 形态选型。门禁/dry-run 作为后续能力，建议作为独立 PR 跟进，避免本次重构无限扩大。

## 待决问题（请作者拍板）

1. **形态 A 还是形态 B？** 我的倾向是 **B**——理由是作者明确要的是"融合"不是"校验对齐"，A 只是"把错误抽象层的 alias 校验对了"，没真正解决"脱节"；B 才是把迁移变成 module 声明的一等组件。但 B 是平台核心改动，成本和回归风险显著高于 A，需要作者确认是否愿意承受。
2. **本次重构的最小集边界。** 建议本次只做"alias 收敛 + 形态落地 + 现有测试改写"。运行态门禁、dry-run、多租户、回滚这些 TD 项各自独立推进，不塞进本次。
3. **`muyun-migration` 子项目是否保留。** 形态 B 下，迁移代码天然可以并进 `muyun-boot`（与 Scanner/Registrar 同包）或 `muyun-platform`。保留独立子项目仅当仍有独立演进理由（如门禁/dry-run 体积大、未来要被业务服务单独依赖）。倾向保留，但待作者确认。

## 不会做的事（明确不做）

- 不改 governance 链路：`LowCodeModuleHealthChecker` SPI / `LowCodeConfigHealthReport` / `LowCodeModuleConfigPublishFacade` 不动。
- 不在本草案里设计多租户迁移、反向回滚、运行态可观测性的完整方案——它们是 TD-020/021/022，独立推进。
- 不为平台自举 seed 数据（如果将来需要）在本草案里设计载体——当前未明确，作者方向是 module 业务维度。
