# Quarkus Migration Plan

本文记录 Spring 到 Quarkus 迁移的稳定目标、阶段边界和验收口径。执行过程中的临时方案和待回收项记录在
`docs/technical-debt/QUARKUS_MIGRATION_TEMPORARY_DEBT.md`。

本文不记录探索过程、提交流水或短期调试信息。

## Migration Goals

1. 移除 Spring Framework、Spring MVC 和 Spring Boot 运行时强耦合。
2. 保持动静一体平台路线：静态 Java 模型和动态元数据模型继续复用同一套能力、数据访问、生命周期和治理语义。
3. 保留 Quarkus 可稳定承接的第三方能力；不因迁移而重写已有成熟依赖。
4. 优先让 Quarkus 主链路可编译、可启动、可测试，再逐批回收迁移期适配层。
5. 避免把 Spring 的隐式模型机械搬到 Quarkus。Web 路由、请求上下文、依赖注入、扫描契约和测试图应逐步显式化。

## Architecture Guardrails

1. 平台能力层优先于业务硬编码。迁移中发现重复适配或业务层补丁时，应优先判断是否需要补齐平台门面。
2. 静态链路保持轻量直观。Quarkus 适配不能把普通静态 service/controller 推向底层 CDI、JAX-RS 或运行时元数据细节。
3. 动态链路继续由元数据驱动。动态模块 Web/API 迁移不能退化为特定模块硬编码。
4. HTTP 边界以 JAX-RS resource 方法和明确的 request context 为准，不依赖 Spring MVC 的 annotation merge、servlet attribute 或 handler method 语义。
5. CDI 边界应收敛为明确 scope、qualifier、producer 和稳定平台接口；`ObjectProvider` 等 Spring 风格适配只作为迁移期工具。
6. 迁移期允许局部债务，但必须记录现状、影响和回收方向，并在相关代码批次完成后同步更新。

## Workstreams

### Web Route Explicitness

- 将 Spring MVC controller 迁为 Quarkus JAX-RS resource。
- 清理接口 default method、override 注解合并和重复 endpoint 声明带来的行为差异。
- 动态 Web catch-all route 与静态 exact alias resource 的优先级作为明确契约维护。
- 为关键动态/静态 Web 入口补真实 Quarkus HTTP contract 测试。

### Request Context Cleanup

- 逐步减少从 ThreadLocal 读取 HTTP 请求数据。
- 路径参数优先通过 `@PathParam` 或显式上下文参数传递。
- 当前用户、租户、trace、action context 收敛到 request-scoped context、Quarkus security context 或平台稳定门面。
- 保留已有平台上下文门面时，应确认清理生命周期、并发隔离和异步传播边界。

### CDI Dependency Boundary

- 将 Spring 注入习惯迁为 CDI 可分析的依赖图。
- 清理 ambiguous dependency、unused bean removal、代理构造限制和临时 `@Dependent` scope。
- 将确有价值的 provider/producer 固化为平台门面，移除只为迁移存在的适配层。

### Static Scanning Contract

- 明确静态模块、菜单、action、事件处理器等声明模型在 Quarkus 下的扫描规则。
- 不默认复刻 Spring merged annotation 和 AOP proxy 解析语义。
- 如平台需要组合注解能力，应建立统一 annotation resolver 或 Jandex-based scanner，并补契约测试。

### Database And Platform Integration

- 以 MuYunDatabase Quarkus 能力为底层数据访问主线。
- 保持标准 DAO、能力组合、事务边界和动态 schema/runtime 能力一致。
- 对真实数据访问、分页排序、建表、动态元数据解析等基础边界优先补 contract 或 Quarkus IT。

### Test Migration

- 普通单元/契约测试继续由 `./gradlew test` 承接。
- 历史 Spring IT 逐批迁到 `@QuarkusTest`、Quarkus test resource 和明确 test profile。
- 测试 profile 中的 exclude/producers 只能作为迁移期隔离手段，长期应收敛为稳定 fixture。
- 每迁移一批 IT，应同步缩小旧 Spring IT 编译排除范围。

### Temporary Debt Recovery

- 每个临时债务项必须包含现状、影响和回收方向。
- 完成相关代码回收后，同步更新或删除债务项。
- 技术债文档不替代测试；能由测试锁住的行为优先用测试表达。

## Current Phase

当前阶段是 Quarkus 主链路和 Web/DI/test 迁移收敛期。

阶段重点：

1. 保持 `./gradlew test` 可通过。
2. 保持 `:muyun-boot:quarkusBuild -x test` 可通过。
3. 继续恢复动态 Web 和静态 Web 的真实 Quarkus HTTP contract。
4. 逐批移除 Spring MVC、servlet、Spring context、Spring transaction test 等依赖。
5. 把已确认的迁移期临时方案登记到技术债文件，避免隐性长期化。

## Done Criteria

迁移完成应满足以下条件：

1. 生产代码不再依赖 Spring Framework、Spring MVC、Spring Boot 运行时 API。
2. Quarkus 应用构建、启动和核心 HTTP contract 稳定。
3. 动态模块和静态模块共享平台能力、权限、租户、审计、事务和生命周期语义。
4. 关键 Web 路由、ActionEndpoint、请求上下文、异常响应和数据访问行为有测试证据。
5. 历史 Spring IT 已迁移、删除或明确归档为非当前目标。
6. 迁移期技术债已清零，或只保留与 Quarkus 迁移无关的长期平台待办。

## Review Checklist

每个迁移批次合入前检查：

1. 是否引入新的 Spring 或 servlet 运行时依赖。
2. 是否把 HTTP 请求事实隐藏进 ThreadLocal、全局状态或业务 service。
3. 是否新增 CDI ambiguous dependency、过宽 scope 或只为绕过 Quarkus 构建期分析的适配。
4. 是否破坏动静一体能力复用，形成动态/静态两套平行实现。
5. 是否有目标测试覆盖新增或恢复的 Quarkus 契约。
6. 是否同步更新 `QUARKUS_MIGRATION_TEMPORARY_DEBT.md` 中相关债务状态。
