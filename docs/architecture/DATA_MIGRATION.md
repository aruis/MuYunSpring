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

第一版是**全局/平台级**迁移：每个 alias 一套版本序列，系统态执行，不按租户划分。版本追踪表 `migration_record` 的 `alias` 字段声明唯一，平台归一化为 `(tenant_id, alias)` 复合唯一索引；当前 `tenant_id` 恒为 null，等价于 alias 全局唯一。

升级到按租户独立迁移时，只需：

1. 提供一个新的 `MigrationVersionStore` 实现（按 `TenantContext` 解析租户维度），替换默认的 `GlobalMigrationVersionStore`。
2. 版本追踪 Service 从全局作用域改为按租户作用域。

**表结构、索引、`AbstractMigration` / `MigrateStep` 签名、迁移类代码都不需要改动。** 这是刻意预留的接缝，不是当前能力。

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
