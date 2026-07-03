# Quarkus Migration Temporary Debt

本文记录 Spring 到 Quarkus 迁移过程中引入或暂时保留的技术债。目标是让迁移先形成可编译、可运行的 Quarkus 主链路，再按清单逐项回收行为差异和临时适配层。

本文只记录需要后续处理的稳定债务，不记录探索过程和执行流水。

## Web Migration Boundary Decisions

以下决策用于约束后续 Web 迁移，避免把 Spring MVC 机械翻译成 JAX-RS 后形成新的长期耦合。

1. CRUD Web 入口短期保留继承式 controller/resource 形态，用于降低迁移面；长期只允许基类承载稳定、无业务分支的协议模板。子类存在个性化路径、参数或响应语义时，应拆成明确的 JAX-RS resource 方法，不能依赖 override 注解合并。
2. `ActionEndpoint` 不再复刻 Spring `HandlerMethod` 拦截模型。Quarkus 侧以资源方法注解和显式 endpoint context 为边界：过滤器只负责提取 HTTP 请求事实，动作授权、acting/delegation scope 和审计上下文由独立 resolver 组合，便于单测和 HTTP contract 测试分别覆盖。
3. 嵌套资源 scope 不以 ThreadLocal 路径变量作为长期模型。父级 scope 应通过显式 `@PathParam` 或资源方法参数传入 service/support；只有过渡期允许请求上下文适配器承接旧基类签名，且必须在测试恢复后删除。
4. 当前用户、租户、trace 等请求上下文优先收敛为 request-scoped 上下文对象或显式上下文参数。ThreadLocal 只作为现有平台上下文门面的边界实现，不能成为 Web 层解析 HTTP 请求数据的长期入口。
5. 统一异常响应以 JAX-RS `ExceptionMapper` 为正式入口，保持既有平台错误码、message、traceId 语义。Validation error 需要单独映射为稳定响应结构，不混入普通 `PlatformException` 的处理分支。

## Web Endpoint Semantics

### Nested scope path variables use a transitional request attribute

- 现状：`NestedCrudWebSupport` 的路径变量读取已从 Spring `HandlerMapping` 改为平台自有 request attribute；真实 Quarkus HTTP 请求如何填充该 attribute 尚未收口，`ModuleScopedRuleTreeWebSupport` 等其他路径 scope 仍待迁移。
- 影响：轻量 contract test 可以验证 scope 行为，但真实 HTTP 路由下的嵌套 CRUD、模块范围规则树等能力仍可能无法正确解析 scope。
- 回收方向：改为显式 `@PathParam` 传递，或提供 Quarkus 请求上下文适配器，并为嵌套路径补真实 HTTP 测试。

### Duplicate route handling uses temporary override removal

- 现状：为避免 Quarkus 构建期识别到重复 JAX-RS endpoint，部分 override 方法上的路由注解被移除，暂时依赖父类资源方法；正式方向见 Web 迁移边界决策第 1 条。
- 影响：子类 override 中的个性化路径、参数或响应语义存在遗漏风险。
- 回收方向：逐个核对继承式 Web controller，必要时拆成明确的 Quarkus resource 方法，确保每个路由只有一个声明来源。

### Dynamic Web route precedence and inherited default routes are not fully restored

- 现状：`DynamicRecordWebControllerIT` 已迁移为 Quarkus HTTP 测试，但当前 RESTEasy Reactive 路由下，静态 exact alias resource 仍会被动态模块 regex resource 抢占；部分接口 default method 声明的动态记录路由仍返回 404。
- 影响：动态模块 Web 入口与旧 Spring MVC 路由优先级、接口继承路由暴露语义不完全等价。
- 回收方向：把动态 Web 入口从接口 default route 迁到显式 JAX-RS resource 方法或专门路由层，明确静态模块 exact path 优先于动态 fallback，并补真实 HTTP contract 测试。

### Spring multi-path mappings were collapsed

- 现状：部分 Spring `@RequestMapping` 多路径数组迁移为 JAX-RS `@Path` 时只保留了主路径。
- 影响：兼容别名路径或历史路径可能不可用。
- 回收方向：明确哪些多路径是正式契约；对正式契约补充独立 JAX-RS 方法或统一路由层测试。

## Request Context

### Request data uses ThreadLocal adapters

- 现状：`BearerTokenCurrentUserProvider` 和 `DynamicWebRequest` 通过 JAX-RS filter 写入 ThreadLocal 来读取授权头和请求路径；正式方向见 Web 迁移边界决策第 4 条。
- 影响：虽然可用于迁移期解耦 Spring `RequestContextHolder`，但需要严格清理生命周期，异步执行或上下文传播场景存在风险。
- 回收方向：改为 `@RequestScoped` 上下文对象、JAX-RS `ContainerRequestContext` 适配器或 Quarkus 安全上下文，并补并发/请求隔离测试。

## Dependency Injection

### Broad `@Dependent` scopes are provisional

- 现状：部分核心 service 使用 `@Dependent` 规避 Quarkus 对普通作用域 bean 的代理构造限制。
- 影响：生命周期和实例复用行为可能不同于预期，长期也会掩盖真正需要无参构造、接口代理或 producer 管理的 bean。
- 回收方向：按服务职责收敛为 `@ApplicationScoped`、`@Singleton` 或更精确 scope；必要时调整基类构造方式，并补 CDI 注入 contract 测试。

### Spring-style provider adapters remain

- 现状：项目内新增 `ObjectProvider`、`ObjectProviders`，并通过 Quarkus producer 提供 `Optional`、`List`、`BaseDao` 等迁移适配。
- 影响：能降低迁移面，但长期会形成类 Spring 注入习惯，模糊平台正式门面。
- 回收方向：保留确有价值的稳定门面，移除只为迁移存在的 provider；关键 producer 补注入行为测试。

### Tenant verifier injection is disambiguated by concrete service

- 现状：多个租户域 service 都实现 `ActiveTenantVerifier`，Quarkus CDI 按接口注入会出现 ambiguous dependency。动态 Web controller 迁移期暂时注入 `TenantService` 作为租户有效性校验入口。
- 影响：Web 层对 IAM 具体服务存在额外耦合，和“请求上下文/租户校验用稳定门面承接”的长期方向不完全一致。
- 回收方向：引入专门 qualifier 或独立 `TenantVerifier` 门面 producer，只暴露唯一默认 bean；完成后把动态 Web controller 从 `TenantService` 收回到稳定接口。

## Static Scanning

### Spring merged annotation semantics are not preserved

- 现状：静态模块、菜单声明、运行时事件处理器扫描已从 Spring `ApplicationContext`/`AopUtils`/`AnnotationUtils` 迁到 CDI `BeanManager`，当前主要做直接类注解扫描。
- 影响：组合注解、继承注解、代理类解析等 Spring merged annotation 语义可能丢失。
- 回收方向：确认平台允许的声明模型；如需要组合注解能力，建立 Quarkus/Jandex 或统一 annotation resolver，并补扫描契约测试。

### Static module route alias validation is deferred

- 现状：静态模块扫描器不再读取 Spring MVC `@RequestMapping`，原有 web scope 与 `@PlatformStaticModule.alias` 一致性校验暂时移出 scanner。
- 影响：静态模块 alias 与 JAX-RS 路由不一致时，当前扫描测试不会直接失败。
- 回收方向：在 Quarkus Web 路由契约恢复后，以 JAX-RS `@Path`/资源方法元数据补回静态模块入口校验，或明确把模块 alias 与 URL 解耦为正式架构决策。

## HTTP Payloads

### Multipart and download APIs are transitional

- 现状：动态导入 multipart 已迁移到 RESTEasy Reactive `@RestForm`/`FileUpload`，但下载等部分路径仍保留 servlet `HttpServletResponse` 和 `jakarta.servlet-api` 临时依赖。
- 影响：HTTP 响应构造方式不统一，Quarkus native/build-time 优化和测试方式也会受影响。
- 回收方向：统一迁移到 JAX-RS `Response`、`StreamingOutput` 或 RESTEasy Reactive 推荐类型，移除 servlet 过渡依赖。

## Test Migration

### Legacy integration tests are temporarily excluded from test compilation

- 现状：普通 `./gradlew test` 已恢复；历史 `*IT.java` 仍位于 `src/test/java`，且大量使用 Spring Boot test context、Spring property injection 和 Spring transaction test API。迁移期间 `compileTestJava` 只放行已迁移的 Quarkus IT，未迁移的旧 Spring IT 继续被排除。
- 影响：`integrationTest` 已可承接已迁移的 Quarkus IT，但真实数据库/HTTP 集成回归保护仍不完整。
- 回收方向：逐个迁移 `*IT.java` 到 `@QuarkusTest`、Quarkus test resource、REST Assured/JAX-RS 客户端和 MuYunDatabase Quarkus repository 注入；每迁移一批后同步收缩构建排除白名单。

### Quarkus IT profile disables unused-bean removal

- 现状：首个 Quarkus IT profile 临时设置 `quarkus.arc.remove-unused-beans=false`，避免迁移期通过运行时类型查找的 Web filter 在测试启动时被 Arc 裁剪。
- 影响：该 profile 与生产构建优化策略不完全一致，只适合作为恢复集成测试迁移通道的临时措施。
- 回收方向：把运行时查找的 Web/filter/provider 收敛为明确 CDI 引用或标注 `@Unremovable`，确认生产和测试启动行为一致后移除该 profile 配置。

### Platform library module uses Quarkus plugin only for IT model generation

- 现状：`muyun-platform` 为运行 `@QuarkusTest` 集成测试临时应用 Quarkus Gradle 插件，但该模块仍是库模块，不是独立 Quarkus 应用；因此禁用了 platform 模块自身的 Quarkus 应用打包任务。
- 影响：platform 集成测试可以获得 Quarkus ApplicationModel 和 MuYunDatabase Quarkus 注入能力，但 `:muyun-platform:quarkusBuild` 不作为有效验证入口。
- 回收方向：将平台 Quarkus IT 迁到正式测试宿主模块，或为库模块建立独立 Quarkus test fixture/source set；届时移除 platform 模块的临时插件/任务禁用配置。

### Platform Quarkus IT profiles manually isolate service graphs

- 现状：部分平台 `@QuarkusTest` 通过 `quarkus.arc.exclude-types` 精确排除未进入当前测试目标的平台 bean，并在测试内手动组装服务图。
- 影响：可以避免迁移期一次性打开整个平台 CDI 图，但测试 profile 与正式应用注入图仍存在差异。
- 回收方向：平台服务 CDI 边界稳定后，减少测试 profile 中的 bean 排除项，改用正式 producer/module fixture 组装集成测试依赖。
