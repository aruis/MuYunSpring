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
| `code` | 引用路径片段。为空时从字段名推导，例如 `employeeId -> employee`。 |
| `target` | 目标静态 service 类型。目标类型必须暴露 `public static String MODULE_ALIAS`。 |
| `targetModuleAlias` | 目标模块别名。保留给静态引用动态模块或无 service 类型的场景。 |
| `targetField` | 目标字段。当前静态运行态只支持 `id`。 |

`target` 和 `targetModuleAlias` 必须二选一。当前不支持非主键引用；需要唯一键引用时，应先补清楚索引、唯一性、字段类型和运行态校验契约。

引用关系描述的是模型事实，不描述 UI 要展示哪些字段，也不描述 SQL join。普通业务模块不应手写 join SQL。
同一模块内引用 `code` 必须唯一，避免引用路径解析出现隐式歧义。

## 读投影声明

当前模块对外可带出的关联字段声明在 service 上：

```java
class UserAccountService implements ModuleReadProjectionContributor {
    @Override
    public List<ModuleReadProjection> moduleReadProjections() {
        return List.of(
                ModuleReadProjection.filterable("employee_account.employee.employeeNo", "employeeNo"),
                ModuleReadProjection.of("employee_account.employee.title", "employeeTitle")
        );
    }
}
```

`path` 使用引用路径加目标字段：

```text
employee_account.employee.employeeNo
```

`outputField` 是当前模块对外暴露的稳定字段名。UI、查询接口和前端只消费 `outputField`，不直接消费跨模块路径。
同一模块内 `outputField` 必须唯一，并且不能覆盖主实体字段或平台标准字段。

把“带出哪些关联字段”放在 service 上，是为了让后续其它模块引用当前模块时，可以复用当前模块已经定义好的读投影能力。例如后续 `C -> A` 时，`C` 有机会消费 `A` service 暴露的关联摘要，而不是重新理解 `A -> B` 的内部 join 细节。

## 查询能力

`ModuleReadProjection` 的默认语义是：

| 声明方式 | 可展示 | 可排序 | 可过滤 |
| --- | --- | --- | --- |
| `ModuleReadProjection.of(path, outputField)` | 是 | 是 | 否 |
| `ModuleReadProjection.sortableOnly(path, outputField)` | 是 | 是 | 否 |
| `ModuleReadProjection.filterable(path, outputField)` | 是 | 是 | 是 |

过滤必须显式开启。展示字段不会因为已经被 select 出来就自动获得过滤能力。

当前用户模块的边界是：

| 输出字段 | 路径 | 能力 |
| --- | --- | --- |
| `employeeNo` | `employee_account.employee.employeeNo` | 展示、排序、过滤 |
| `employeeTitle` | `employee_account.employee.title` | 展示、排序 |

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

UI 不直接写 `employee_account.employee.title`，也不声明 SQL join。跨模块路径是后端 service 和平台 planner 的内部契约。

## SQL 运行时边界

静态引用路径由平台解析成 join plan。当前支持：

1. 直接引用：当前模块字段引用目标模块主记录。
2. 反向桥接引用：候选模块引用当前模块，路径片段命中候选模块主实体别名。
3. 递归路径：例如 `employee_account.employee`。

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
