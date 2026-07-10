# 静态模块引用与读投影契约

本文定义静态模块通过引用关系带出关联模块字段的稳定契约。它服务于动静一体路线：静态 Java 模型先用注解和 service 声明能力，动态元数据后续应编译到同一套引用图和读投影运行时，而不是另起一套查询内核。

## 适用范围

该契约用于列表、选择器、详情、导出等读场景中，从当前模块按 `N:1` 或 `1:1` 引用路径读取关联对象摘要字段。

当前已验证的静态样本包括：

```text
iam.user <- iam.employee_account -> iam.employee
iam.employee -> iam.organization
```

用户模块通过职员账号绑定表带出职员工号和职员姓名，验证了反向桥接引用和递归引用路径。
职员模块通过所属机构带出机构名称，验证了普通多对一直接引用路径。

## 引用声明

静态引用关系声明在 Java 模型字段上：

```java
@ModuleReference(target = EmployeeService.class)
private String employeeId;
```

字段含义：

| 字段 | 含义 |
| --- | --- |
| `code` | 引用短码，主要用于展示、兼容和动态侧保留。静态读投影不应依赖它作为主契约。 |
| `target` | 目标静态 service 类型。目标类型必须暴露 `public static String MODULE_ALIAS`。 |
| `targetModuleAlias` | 目标模块别名。保留给静态引用动态模块或无 service 类型的场景。 |
| `targetField` | 目标字段。当前静态运行态只支持 `id`。 |

`target` 和 `targetModuleAlias` 必须二选一。当前不支持非主键引用；需要唯一键引用时，应先补清楚索引、唯一性、字段类型和运行态校验契约。

引用关系描述的是模型事实，不描述 UI 要展示哪些字段，也不描述 SQL join。普通业务模块不应手写 join SQL。
同一模块内引用 `code` 必须唯一，避免兼容路径解析出现隐式歧义。静态 service 推荐使用字段引用链，
以模型字段本身作为引用路径抓手。

## 读投影声明

当前模块对外可带出的关联字段声明在 service 上：

```java
class UserAccountService implements ModuleReadProjectionContributor {
    @Override
    public List<ModuleReadProjection> moduleReadProjections() {
        return List.of(
                ModuleReadProjection.filterable(
                        ModuleReferencePath.inverseOne(EmployeeAccount::getUserId)
                                .then(EmployeeAccount::getEmployeeId)
                                .select(Employee::getEmployeeNo),
                        "employeeNo"),
                ModuleReadProjection.of(
                        ModuleReferencePath.inverseOne(EmployeeAccount::getUserId)
                                .then(EmployeeAccount::getEmployeeId)
                                .select(Employee::getTitle),
                        "employeeTitle")
        );
    }
}
```

静态链路优先使用 Java getter method reference 描述引用路径：

```java
ModuleReferencePath.from(Employee::getOrganizationId)
        .select(Organization::getTitle)
```

`from(...)` 表示从当前模块主模型的引用字段出发，`then(...)` 表示沿上一跳目标模型继续走一个引用字段，
`inverseOne(...)` 表示通过一个候选桥接模型的字段反向唯一命中当前模块。每一跳都必须落到真实 Java 字段，
并且该字段必须声明 `@ModuleReference`。列表读投影会生成分页 SQL join，反向桥接必须显式使用
`inverseOne(...)` / `thenInverseOne(...)` 声明一对一唯一关系；未声明唯一性的 `inverse(...)` / `thenInverse(...)`
视为不安全路径，不能进入分页 join。

`outputField` 是当前模块对外暴露的稳定字段名。UI、查询接口和前端只消费 `outputField`，不直接消费跨模块路径。
同一模块内 `outputField` 必须唯一，并且不能覆盖主实体字段或平台标准字段。

把“带出哪些关联字段”放在当前 service 上，是为了稳定当前模块自己的读 API。若出现 `A -> B -> C`，
且 A 需要带出 C 的字段，A service 应显式声明 `A.bId -> B.cId -> C.field` 的字段引用链；
不应通过消费 B service 的 `outputField` 间接形成投影依赖。

字符串 `path` API 仅作为动态元数据、兼容迁移或临时逃生口保留。普通静态 service 不推荐新增字符串路径声明。

## 查询能力

`ModuleReadProjection` 的默认语义是：

| 声明方式 | 可展示 | 可排序 | 可过滤 |
| --- | --- | --- | --- |
| `ModuleReadProjection.of(referencePath, outputField)` | 是 | 是 | 否 |
| `ModuleReadProjection.sortableOnly(referencePath, outputField)` | 是 | 是 | 否 |
| `ModuleReadProjection.filterable(referencePath, outputField)` | 是 | 是 | 是 |
| `ModuleReadProjection.filterableOnly(referencePath, outputField)` | 是 | 否 | 是 |
| `ModuleReadProjection.exists(referencePath, outputField)` | 是 | 否 | 是 |

过滤必须显式开启。展示字段不会因为已经被 select 出来就自动获得过滤能力。
`exists(...)` 用于“引用链是否命中”的布尔派生字段，例如职员是否已经绑定账号；SQL planner 会按引用链
生成 left join，并以目标记录主键是否非空作为输出值。

当前用户模块的边界是：

| 输出字段 | 路径 | 能力 |
| --- | --- | --- |
| `employeeNo` | `EmployeeAccount.userId(inverse) -> EmployeeAccount.employeeId -> Employee.employeeNo` | 展示、排序、过滤 |
| `employeeTitle` | `EmployeeAccount.userId(inverse) -> EmployeeAccount.employeeId -> Employee.title` | 展示、排序 |

当前职员模块的账号边界是：

| 输出字段 | 路径 | 能力 |
| --- | --- | --- |
| `username` | `EmployeeAccount.employeeId(inverse) -> EmployeeAccount.userId -> UserAccount.username` | 展示、过滤 |
| `accountBound` | `EmployeeAccount.employeeId(inverse) -> EmployeeAccount.id` | 展示、过滤 |

SQL plan 内部按语义拆分字段集合：

| 字段集合 | 含义 |
| --- | --- |
| `responseFields` | 响应给前端的字段。 |
| `queryableFields` | 可进入查询条件的字段。 |
| `sortableFields` | 可进入排序的字段。 |

## UI 消费边界

静态 UI 只声明当前模块字段或 service 已暴露的读投影输出字段：

```java
.listView(list -> list
        .field("username")
        .field("employeeNo")
        .field("employeeTitle"))
```

UI 不直接写跨模块路径，也不声明 SQL join。跨模块字段引用链是后端 service 和平台 planner 的内部契约。

## SQL 运行时边界

静态引用路径由平台解析成 join plan。当前支持：

1. 直接引用：当前模块字段引用目标模块主记录。
2. 反向桥接引用：候选模块的具体字段引用当前模块。
3. 递归路径：例如 `EmployeeAccount.userId(inverse) -> EmployeeAccount.employeeId`。

运行时 join 会自动附加租户等值和软删过滤。当前只面向适合分页列表的 `N:1` 或 `1:1` 摘要读取，不承诺一对多展开。

`RelationProjectionJoinContributor` 已标记为兼容逃生口。普通静态模块应优先使用：

```text
@ModuleReference + ModuleReadProjectionContributor
```

只有无法被引用图表达的特殊旧场景，才使用手写 join contributor。

## 当前限制

1. 动态模块尚未接入该静态引用图运行时。
2. `targetField` 当前只支持 `id`。
3. 暂未实现引用路径深度上限、循环诊断和 plan 缓存。
4. 当前数据权限仍以源模块列表读取为主，目标模块数据权限如何叠加需要单独治理。
5. 字段保护和 post-read transform 非空时仍需要谨慎处理 SQL Map 输出路径。

## 已验证样本

职员带出组织名称使用普通多对一直接引用：

```text
iam.employee -> iam.organization
```

该样本证明当前机制不是用户-职员桥接表特化。当前只暴露 `organizationTitle` 展示和排序；是否允许按组织名称过滤，需要结合组织选择器、组织权限和查询性能再决定。
