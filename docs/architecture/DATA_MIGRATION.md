# 数据迁移

## 定位

数据迁移（data migration）解决"已上线业务数据的版本化、步骤式修补"——历史值转换、字段搬运、平台配置初始化、一次性数据修正。它和 schema 迁移是两个维度：

| | 数据迁移（本模块 `muyun-migration`） | schema 迁移 |
| --- | --- | --- |
| 操作对象 | 业务记录（通过 Service / Repository API） | 表结构（DDL） |
| 入口 | `AbstractMigration` + `MigrationBootstrap` | `StaticSchemaService` / `DynamicSchemaService` |
| 触发 | 应用启动时自动执行 | 配置发布 / 显式 `ensureTable` |

结构变更走 schema 迁移；动态记录的值修补优先走动态记录 API 或导入导出。只有需要"按版本号断点续传地跑一次性数据脚本"时，才用本模块。

## 模型

- `AbstractMigration`：业务子类化为 Spring `@Component`，声明 `getAlias()` 与 `getMigrateSteps()`。
- `MigrateStep(version, action)`：版本号必须为正且在单个迁移内唯一；`action` 是单步逻辑。
- `MigrationVersionStore`：版本读写接口，记录每个 alias 跑到第几版。

启动时 `MigrationBootstrap`（`ApplicationRunner`）在系统态下：建版本追踪表 → 按版本号排序并校验每个迁移 → 跳过已执行步骤 → 逐个执行未完成步骤，每步连同版本记录在独立事务内提交。失败抛出，停止后续步骤。

## 全局版本与多租户预留

第一版是**全局/平台级**迁移：每个 alias 一套版本序列，系统态执行，不按租户划分。

### 表结构

版本追踪表 `migration_record`：
- `alias` 列声明 `unique = true`，平台 `PlatformUniqueIndexes` 把它归一化为 `(tenant_id, alias)` 复合唯一索引——这是按租户唯一的标准模式。
- 当前 `tenant_id` 恒为 null（`MigrationRecordService.normalizeBeforeMutation` 强制），所以"按租户唯一"在全局阶段退化为"按 null 维度唯一"。

### NULL 语义陷阱与 partial unique index

SQL 标准下 `NULL != NULL`，所以 `(tenant_id, alias)` 复合索引在 `tenant_id IS NULL` 时**不强制 alias 全局唯一**——两条 `(NULL, 'foo')` 数据库不会拒绝。

为兜底全局阶段的 alias 唯一性，`MigrationBootstrap` 在 `ensureTable` 之后显式执行：

```sql
CREATE UNIQUE INDEX IF NOT EXISTS migration_record_alias_global_unique
ON migration_record (alias) WHERE tenant_id IS NULL
```

这个 partial unique index 只约束 `tenant_id IS NULL` 的行，与复合索引并存不冲突。`MigrationRecord.alias` 上声明的 `unique = true` 仍然保留——它是未来多租户阶段的契约表达，partial index 是当前阶段的运行时兜底，两者意图互补。

### 升级到按租户迁移

未来按租户独立迁移时，追加一个 sibling partial index：

```sql
CREATE UNIQUE INDEX IF NOT EXISTS migration_record_tenant_alias_unique
ON migration_record (tenant_id, alias) WHERE tenant_id IS NOT NULL
```

配合一个按 `TenantContext` 解析租户维度的 `MigrationVersionStore` 实现替换默认的 `GlobalMigrationVersionStore`。

**表结构、`AbstractMigration` / `MigrateStep` 签名、迁移类代码都不需要改动。** 这是刻意预留的接缝，不是当前能力。

## 用法

```java
@Component
class FooMigrate extends AbstractMigration {
    private final FooService fooService;

    FooMigrate(FooService fooService) {
        this.fooService = fooService;
    }

    @Override
    public String getAlias() {
        return "foo";
    }

    @Override
    public List<MigrateStep> getMigrateSteps() {
        return List.of(
            new MigrateStep(1, () -> fooService.bumpAllAgesBy(10)),
            new MigrateStep(2, () -> fooService.createSeed("seed", 30))
        );
    }
}
```

## 边界

- 迁移在启动时同步执行，会延长启动时间；大体积数据迁移应拆成多步。
- 每步独立事务；步骤之间不共享事务，失败的步骤不会回滚已提交的前序步骤。
- 框架只保证"步骤不重复执行"，不保证"步骤内逻辑幂等"——迁移作者需自行保证步骤内逻辑能容忍部分记录已被改过的情况。
- **步骤版本号一经发布不可删除、不可重排、不可改语义。** 执行器以"跳过 `version <= currentVersion` 的步骤"实现断点续传，并用 `currentVersion >= finalVersion` 做早退；若删除尾部步骤，`finalVersion` 减小会导致中间未应用的步骤被早退跳过。新增变更只能追加更大的版本号；若要废弃旧步骤，让 action 留空（`() -> { }`）而不是删除。
- **多个 `AbstractMigration` 之间不保证执行顺序。** `MigrationBootstrap` 通过 Spring 注入 `List<AbstractMigration>`，顺序由容器决定，未显式排序。若一个迁移依赖另一个迁移已跑过，应在依赖方的步骤内做 defensive 查询（如 `findByAlias`），不要假设先后；必要时可以把两者合并成一个 migration 的多个 step。

### 生产环境硬伤

以下限制在多实例 / 复杂回滚场景下会暴露，迁移作者与运维都需要知道：

- **步骤内对外副作用不可回滚。** 事务边界只覆盖数据库写入；步骤内调用的外部 API（HTTP、消息队列、文件 IO）一旦生效，事务回滚也无法撤销。**重试时步骤会再次执行**——若步骤本身不幂等（如不带 idempotency-key 的 HTTP 调用、自增 ID 写入），重试会造成数据重复或状态错乱。迁移作者需保证步骤对外副作用幂等。
- **不支持反向迁移（回滚）。** 版本表只向前追加；从 v1.2 回滚到 v1.0 时，`currentVersion > finalVersion` 让所有步骤早退跳过，但 v1.1/v1.2 已改的数据**留在新状态**——代码版本回滚后业务行为与数据状态不一致。生产回滚需手工 SQL 修补，或避免回滚跨越数据迁移版本。
- **运行态可观测性弱。** 启动时通过日志输出步骤号，没有进度端点、没有失败告警、没有 dry-run 预演。长时间迁移无法外部观测进度。见 TD-021。
