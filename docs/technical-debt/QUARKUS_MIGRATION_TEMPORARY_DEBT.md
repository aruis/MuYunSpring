# Quarkus Migration Temporary Debt

本文记录 Spring 到 Quarkus 迁移过程中引入或暂时保留的技术债。目标是让迁移先形成可编译、可运行的 Quarkus 主链路，再按清单逐项回收行为差异和临时适配层。

本文只记录需要后续处理的稳定债务，不记录探索过程和执行流水。

## Web Endpoint Semantics

### Action endpoint context is incomplete

- 现状：`ActionEndpointInterceptor` 已迁移为 JAX-RS 过滤器骨架，但原 Spring `HandlerMethod` 注解解析、动作上下文、授权上下文、acting/delegation scope 等语义尚未恢复。
- 影响：依赖 action endpoint 上下文的授权、审计或动作解析行为可能与 Spring 版本不一致。
- 回收方向：基于 Quarkus/Resteasy Reactive 的资源方法元数据、路径参数和过滤器上下文重建 `ActionEndpointContextResolver` 契约，并补动作端点 contract 测试。

### Nested scope path variables are disabled

- 现状：`NestedCrudWebSupport`、`ModuleScopedRuleTreeWebSupport` 的路径变量读取从 Spring `HandlerMapping` 迁出后，当前临时返回空 map。
- 影响：依赖父子资源路径变量的嵌套 CRUD、模块范围规则树等能力可能无法正确解析 scope。
- 回收方向：改为显式 `@PathParam` 传递，或提供 Quarkus 请求上下文适配器，并为嵌套路径补真实 HTTP 测试。

### Duplicate route handling uses temporary override removal

- 现状：为避免 Quarkus 构建期识别到重复 JAX-RS endpoint，部分 override 方法上的路由注解被移除，暂时依赖父类资源方法。
- 影响：子类 override 中的个性化路径、参数或响应语义存在遗漏风险。
- 回收方向：逐个核对继承式 Web controller，必要时拆成明确的 Quarkus resource 方法，确保每个路由只有一个声明来源。

### Spring multi-path mappings were collapsed

- 现状：部分 Spring `@RequestMapping` 多路径数组迁移为 JAX-RS `@Path` 时只保留了主路径。
- 影响：兼容别名路径或历史路径可能不可用。
- 回收方向：明确哪些多路径是正式契约；对正式契约补充独立 JAX-RS 方法或统一路由层测试。

### CORS is not restored

- 现状：Spring MVC CORS 配置已移除，尚未用 Quarkus `quarkus.http.cors.*` 或等价机制恢复。
- 影响：浏览器跨域访问策略可能与原配置不同。
- 回收方向：确认部署侧 CORS 契约，迁移到 Quarkus 配置并补配置说明或集成测试。

## Request Context

### Request data uses ThreadLocal adapters

- 现状：`BearerTokenCurrentUserProvider` 和 `DynamicWebRequest` 通过 JAX-RS filter 写入 ThreadLocal 来读取授权头和请求路径。
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

## Static Scanning

### Spring merged annotation semantics are not preserved

- 现状：静态模块、菜单声明、运行时事件处理器扫描已从 Spring `ApplicationContext`/`AopUtils`/`AnnotationUtils` 迁到 CDI `BeanManager`，当前主要做直接类注解扫描。
- 影响：组合注解、继承注解、代理类解析等 Spring merged annotation 语义可能丢失。
- 回收方向：确认平台允许的声明模型；如需要组合注解能力，建立 Quarkus/Jandex 或统一 annotation resolver，并补扫描契约测试。

## HTTP Payloads

### Multipart and download APIs are transitional

- 现状：动态导入 multipart 已迁移到 RESTEasy Reactive `@RestForm`/`FileUpload`，但下载等部分路径仍保留 servlet `HttpServletResponse` 和 `jakarta.servlet-api` 临时依赖。
- 影响：HTTP 响应构造方式不统一，Quarkus native/build-time 优化和测试方式也会受影响。
- 回收方向：统一迁移到 JAX-RS `Response`、`StreamingOutput` 或 RESTEasy Reactive 推荐类型，移除 servlet 过渡依赖。

## Test Migration

### Test suite still depends on Spring testing stack

- 现状：生产代码已能通过 Quarkus 编译和 `quarkusBuild`，但测试代码仍大量依赖 Spring Test、MockMvc、`@SpringBootTest`、Mockito/AssertJ 组合，`./gradlew test` 尚未恢复。
- 影响：迁移后的行为缺少自动回归保护。
- 回收方向：按平台契约优先级迁移测试：核心能力 contract 测试、Web resource 测试、动态运行态真实路径测试，再清理 Spring test 依赖。

### Legacy integration tests are temporarily excluded from test compilation

- 现状：历史 `*IT.java` 仍位于 `src/test/java`，且大量使用 Spring Boot test context、Spring property injection 和 Spring transaction test API。迁移期间 `compileTestJava` 临时排除了 `**/*IT.java`，避免阻塞普通单元测试恢复。
- 影响：`integrationTest` 任务在这些测试 Quarkus 化之前不能提供真实数据库/HTTP 集成回归保护。
- 回收方向：建立 Quarkus 风格集成测试源集或逐个迁移 `*IT.java` 到 `@QuarkusTest`、Quarkus test resource、REST Assured/JAX-RS 客户端和 MuYunDatabase Quarkus repository 注入。

### Legacy boot Spring tests are temporarily excluded from boot test compilation

- 现状：`muyun-boot` 中部分 configuration、MockMvc Web controller、Spring HandlerMethod/filter/exception-handler 测试仍依赖 Spring Test。迁移期间在 `muyun-boot/build.gradle.kts` 临时排除了这些测试文件，保留不依赖 Spring 的 boot 单元测试继续编译。
- 影响：Quarkus HTTP 路由、异常映射、过滤器、动作端点上下文和配置 producer 仍缺少等价测试保护。
- 回收方向：按业务入口重建 Quarkus 测试：configuration producer 用 CDI/Quarkus component test 或 `@QuarkusTest`，Web controller 用 REST Assured 或 JAX-RS 资源测试，filter/exception mapper 用 Quarkus HTTP 集成测试。
