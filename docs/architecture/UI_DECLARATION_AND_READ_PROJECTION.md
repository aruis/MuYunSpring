# UI 声明与读投影设计

本文定义静态模块声明 UI、动态配置后续对齐、Web 返回和数据读取投影的总体边界。

本文是当前阶段的设计草案，不是不可变规范。后续实际推进中，如果实现验证、动态表单接入或前端运行器协作暴露出更好的方案，可以随时提出讨论，也可以在边界更清晰、收益明确时直接改进本文和实现。调整时应继续守住动静一体、service 不承载 UI schema、Web 不暴露物理列、SQL 不直接消费 UI 配置这些核心边界。

当前项目尚未上线，动态表单从 UI 配置到底层执行的链路也尚未形成稳定产品事实。因此 UI 声明设计优先从静态链路切入：先让 Java 静态模块用低成本、边界清晰的方式声明页面意图，再让动态 UI 配置编译到同一套运行结果，最终达成动静一体。

静态 UI 声明是为了沉淀统一 descriptor、读模型和投影契约，不改变当前低代码页面交付以动态表单为主的阶段定位。

## 目标

1. 静态 Service 不再承载表单 schema 声明，避免业务行为、平台能力和页面配置混在同一个 service 类型上。
2. 静态 UI 声明可以接驳 service 已知事实，例如模块别名、模型类型、Ability 组合和字段注解。
3. 静态声明和动态配置最终编译成同一套 resolved descriptor，前端不感知来源差异。
4. UI 配置可以影响列表等读场景的字段投影，但不能直接生成 SQL。
5. SQL 只消费后端内部读计划，所有物理列、字段能力、权限、租户和数据范围仍由平台后端校验和编译。

## 核心分层

UI 链路按三个阶段组织：

```text
Definition -> Descriptor -> Plan
```

| 阶段 | 含义 | 消费方 |
| --- | --- | --- |
| `Definition` | 源码声明或配置态事实，例如静态模块 UI 声明、后续动态 UI 配置 | 平台编译器 |
| `Descriptor` | 编译后的对外 resolved 结果，例如模块、字段、视图和动作的前端协议 | Web 和前端运行器 |
| `Plan` | 后端内部执行计划，例如读投影、查询计划和后处理任务 | DAO、SQL mapper 和输出转换 |

这三个阶段不能互相替代。`Definition` 长期不直接给前端，`Descriptor` 不携带物理列，`Plan` 不暴露给前端。迁移期 Web 可以同时返回 `uiDefinition` 和 `uiDescriptor`，但前端应优先消费 `uiDescriptor`，`uiDefinition` 只作为兼容兜底。

`Definition` 阶段内部再区分来源定义和源无关 UI 定义。静态 DSL 和动态配置都不直接编译到对外 descriptor，而是先归一到同一套 `ModuleUiDefinition`。

```text
StaticModuleDefinition
  -> ModuleUiDefinition
  -> ResolvedModuleUiDescriptor
  -> RecordReadProjection / QueryPlan
  -> DAO / SQL mapper

动态 UI 配置
  -> ModuleUiDefinition
  -> ResolvedModuleUiDescriptor
  -> RecordReadProjection / QueryPlan
  -> DAO / SQL mapper
```

`ModuleUiDefinition` 是静态和动态共同的 UI 声明目标。静态 `StaticModuleDefinition.forService(...)` 是构造它的一种源码 DSL；动态 UI 配置发布后也应先转换为它，再进入统一编译链路。

## 静态 UI 声明

静态 UI 声明归属模块定义，不归属 model，也不归属 service。

```text
Model                 字段事实、字段注解和稳定数据契约
Service               业务能力、业务行为、生命周期和运行事实
StaticModuleDefinition 模块身份、动作、页面和视图声明
```

推荐形态：

```java
StaticModuleDefinition.forService(EmployeeService.class)
        .formView(form -> form
                .title("职员档案")
                .field("organizationId", field -> field.required())
                .field("departmentId", field -> field.required())
                .field("employeeNo", field -> field.required())
                .field("title", field -> field.label("职员姓名").required())
                .field("gender")
                .field("mobile")
                .field("email"))
        .listView(list -> list
                .field("employeeNo")
                .field("title")
                .field("organizationId")
                .field("departmentId")
                .field("enabled"))
        .build();
```

这里 `forService(EmployeeService.class)` 是静态 UI 声明接驳 service 的入口。它允许编译器从 service bean 或 service 类型读取模块运行事实，但不要求 service 实现 UI ability。

不推荐继续使用以下形态：

```java
class EmployeeService implements FormAbility<Employee> {
}
```

`FormAbility` 会把 UI schema 暴露能力挂到 service 公共能力面上，长期会让 service 同时承载业务行为、数据能力、页面配置和字段展示。

## 声明内容

第一阶段只支持主表的扁平视图声明，先覆盖表单和列表。模型上应保留多视图和动态字段接入所需的稳定锚点，静态 DSL 可以提供最短写法，由编译器补齐默认值。

视图身份至少包含：

| 字段 | 含义 |
| --- | --- |
| `viewCode` | 模块内视图编码，例如 `default_list`、`default_form` |
| `viewKind` | 视图类型，例如 `LIST`、`FORM`、`DETAIL` |
| `clientType` | 客户端类型，第一阶段可默认为 Web |

第一阶段 `clientType` 只作为未来锚点，不参与编译分支。同一套 `ResolvedModuleUiDescriptor` 协议不能因为客户端不同而生成两套语义模型。

字段 UI 声明只表达页面意图：

| 字段 | 含义 |
| --- | --- |
| `fieldRef` | 字段引用，静态主表可简写为业务字段名 |
| `label` | 页面标题覆盖 |
| `visible` | 当前视图是否展示，第一阶段只支持常量规则 |
| `required` | 当前视图的输入必填语义，第一阶段只支持常量规则 |
| `readOnly` | 当前视图的输入只读语义，第一阶段只支持常量规则 |
| `uiType` | 平台 UI 类型提示，不绑定具体前端组件库 |
| `width` / `align` / `fixed` | 列表展示的轻量提示 |

`fieldRef` 是源无关字段锚点，后续可承载动态字段和子关系定位：

```text
ViewFieldRef
  relationCode/null
  fieldName
  fieldId optional
```

静态主表声明可继续使用 `.field("title")` 这类短写法；编译器将其归一为 `relationCode=null, fieldName=title`。

`visible`、`required` 和 `readOnly` 在定义模型上应按规则对象表达：

```text
UiRule<T>
```

第一阶段只实现 `constant(true/false)`。后续动作态、权限态、表达式联动和客户端差异可以扩展规则来源，但不能把规则直接下沉为 SQL 或绕过保存校验。

字段 UI 声明不表达以下事实：

1. 数据库列名、schema 名和表名。
2. 字段真实类型和 Java 类型。
3. 字典、枚举、引用、单位、金额、字段保护和公式等字段能力。
4. 租户、权限、数据范围和动作授权。
5. 查询条件、排序 SQL、join 或任意 SQL 片段。
6. 复杂布局、联动、表达式可见性、子表和客户端差异。

这些事实应从 model、注解、Ability、动态元数据、字段配置或后续页面配置专题中编译得到。静态 DSL 和动态配置都应先归一为 `ModuleUiDefinition`，再与字段事实合并。

## Service 接入

静态 UI 编译器通过 service 接驳运行事实：

```text
StaticModuleDefinition
  + service bean / service class
  + modelClass from CrudAbility
  + moduleAlias from service
  + Ability interfaces
  + Java model annotations
  + view definitions
  -> ModuleUiDefinition
  -> ResolvedModuleUiDescriptor
```

可接入的 service 事实包括：

1. `moduleAlias`。
2. `modelClass`。
3. `CrudAbility`、`TreeAbility`、`SortAbility`、`ReferenceAbility` 等能力组合。
4. 模型字段、平台标准字段和能力字段。
5. `@OptionField`、引用、字段保护、单位、金额等字段声明。
6. 后续静态模块动作贡献和权限动作事实。

service 不应因为 UI 声明新增公共 ability。确需拆分声明文件时，可以引入模块级 contributor：

```java
interface StaticModuleUiContributor {
    void contribute(ModuleUiDefinitionBuilder builder);
}
```

contributor 仍然贡献模块 UI 定义，不改变 service 的运行能力面。

## 编译与校验

静态 UI 声明在应用启动时注册并编译。请求时返回缓存后的 resolved descriptor，不重复解释源码 DSL。

编译分两步：

```text
来源定义 -> ModuleUiDefinition -> ResolvedModuleUiDescriptor
```

第一步只做来源归一：静态 DSL、动态 UI 配置或后续导入包配置都输出同一套 `ModuleUiDefinition`。第二步合并 service、model、元数据和字段能力事实，生成前端 descriptor 与模块级读模型。

编译职责：

1. 校验 `StaticModuleDefinition` 引用的 service 存在。
2. 校验视图字段存在于模型字段、平台标准字段或能力字段中。
3. 合并字段类型、标题、选项、引用、单位、金额、保护、虚拟字段等字段事实。
4. 校验 UI 声明与字段事实冲突，例如虚拟字段不应声明为普通可输入字段。
5. 生成前端可消费的 resolved 视图结构。
6. 生成后端读计划需要的模块级 `ResolvedModuleReadModel`。当前最小实现只包含逻辑字段事实，后续再合并字段类型、选项、保护、引用和存储形态等事实。
7. 不把物理列写入对外 descriptor。

`required` 和 `readOnly` 需要区分来源：

| 来源 | 含义 |
| --- | --- |
| 数据契约 | 模型、字段定义或平台能力要求的底线约束 |
| UI 声明 | 当前视图或当前页面场景下的输入语义 |

UI 可以加强输入要求，但不能绕过底层数据契约。隐藏或只读的必填字段必须由默认值、平台托管字段或业务链路填充，否则编译或保存校验应失败。

## Web 返回

Web 主协议应逐步收敛到模块或页面 bootstrap，而不是继续扩展独立 `/form/schema`。

返回对象是对外 resolved descriptor，例如：

```json
{
  "moduleAlias": "iam.employee",
  "mainEntity": {
    "fields": []
  },
  "views": {
    "list": {},
    "form": {}
  },
  "actions": [],
  "endpoints": {}
}
```

前端只消费 resolved 结果，不关心模块来自静态声明还是动态配置。

对外 descriptor 与后端读模型分开。对外 descriptor 只表达前端需要的模块、字段、视图、动作和端点；后端 `ResolvedModuleReadModel` 是模块级、缓存级的已解析字段事实和能力事实，可包含投影规划需要的字段读模型、字段角色、存储形态和后处理线索，但不包含本次请求的输出字段、物理列集合或后处理任务，也不进入 Web 响应。

旧的 `/form/schema` 可在迁移期保留，但来源应改为 resolved form view，而不是 service 上的 `FormAbility`。兼容出口不能只搬运 UI 声明字段，还必须合并静态模型事实，例如字段选项、选项标题输出字段、基础类型和模型字段存在性校验。当前端切换到 bootstrap 后，`/form/schema` 可以废弃。

## 动态配置对齐

动态链路不作为当前静态 UI 声明设计的包袱。后续动态配置应按同一套目标形态反向对齐：

```text
动态元数据 + 动态 UI 配置
  -> ModuleUiDefinition
  -> ResolvedModuleUiDescriptor
  -> Web bootstrap
  -> 前端运行器
```

静态和动态的差异只保留在来源定义层：

| 类型 | 来源定义 | 归一目标 | Descriptor |
| --- | --- | --- |
| 静态模块 | Java model、Ability、StaticModuleDefinition | `ModuleUiDefinition` | `ResolvedModuleUiDescriptor` |
| 动态模块 | Metadata、FieldDefinition、动态 UI 配置 | `ModuleUiDefinition` | `ResolvedModuleUiDescriptor` |

前端运行器、Web bootstrap、读投影和输出转换不应因为静态或动态来源不同而分裂成两套协议。

动态 UI 配置不应直接等同于页面布局。布局、控件参数、交互和发布快照可以作为动态来源事实存在，但进入运行态前应先转换为平台通用的 `ModuleUiDefinition`，再与动态字段事实合并。这样动态侧可以有配置管理和发布治理，运行态仍保持与静态相同的 descriptor 和读投影链路。

## 读投影与 SQL

UI 配置可以影响读投影，但不能直接生成 SQL。

正确链路：

```text
Web request(viewCode / uiConfigId)
  -> ResolvedListViewDescriptor
  -> ResolvedModuleReadModel
  -> ActionAuthorization / DataScope / FieldReadPolicy
  -> RecordReadProjectionPlanner
  -> QueryCompiler
  -> DAO / SQL mapper
```

错误链路：

```text
UI 配置 -> SQL columns / SQL fragment
```

`RecordReadProjection` 是后端内部对象。当前最小实现先保持 `ViewFieldRef` 形态的逻辑字段计划，不承载物理列；后续接入 SQL 投影时再补 `selectColumns`。`outputFields` 不应退化为裸字段名，否则会丢失关系锚点，后续无法优雅支持引用字段、关联表字段和动态字段 ID 映射。它最终可包含：

| 内容 | 作用 |
| --- | --- |
| `outputFields` | 本次响应需要输出的字段引用 |
| `selectColumns` | 经字段事实白名单解析后的物理列 |
| `requiredPlatformColumns` | 平台运行需要自动补齐的列 |
| `postReadTransforms` | 字典标题、引用标题、脱敏、虚拟值等后处理 |
| `virtualFields` | 可输出但无物理列或不直接读列的字段 |
| `referenceTitleFields` | 引用标题或投影输出字段 |
| `protectedFields` | 输出前需要脱敏或签名验证的字段 |

`ResolvedModuleReadModel` 是模块级事实，`RecordReadProjection` 是 view/request 级执行计划。只有 `RecordReadProjection` 可以包含本次请求的 `outputFields`、`selectColumns` 和 `postReadTransforms`。

例如列表只声明输出：

```text
employeeNo, title, organizationId
```

实际 SQL 投影可能需要：

```text
id, version, tenant_id, employee_no, title, organization_id
```

如果 `organizationId` 需要标题输出，读计划应安排后处理任务，而不是允许 UI 配置声明 join 或 SQL。

读投影规则：

1. UI 决定需要输出哪些字段。
2. 字段事实决定字段能否读取、如何读取、是否有物理列。
3. SQL 只能使用后端白名单列名。
4. `id`、`version`、`tenantId` 和能力字段等平台必需字段自动补齐。
5. 虚拟字段、引用标题、字典标题、脱敏字段走后处理。
6. 展示、查询和排序分开校验；可展示不代表可查询或可排序。
7. 权限、租户、数据范围和动作授权不受 UI 配置控制。
8. descriptor 中出现字段不代表当前用户、当前动作或当前数据范围允许读取；读投影必须在认证上下文中叠加动作授权、数据范围、字段保护和字段级可读策略后再生成输出字段。

静态 DAO 如果短期没有投影查询能力，第一阶段可以先使用读投影做字段校验和输出裁剪，SQL 仍读取完整实体。后续再为 `BaseDao` 或 MuYunDatabase 增加投影读取入口。动态 SQL mapper 可以更早接入列投影。

## 第一阶段落地范围

第一阶段只做最小闭环：

1. 定义 `ModuleUiDefinition`、`ViewDefinition`、`ListViewDefinition`、`FormViewDefinition` 和字段 UI 声明模型。
2. 将视图声明挂到 `StaticModuleDefinition.forService(...)`。
3. 定义 `ViewFieldRef` 和常量版 `UiRule<T>`，让静态短写法可以归一到源无关模型。
4. 编写静态 UI 编译器，先输出 `ModuleUiDefinition`，再合并 service、model 和字段事实。
5. 先输出最小 `ResolvedModuleUiDescriptor`，随后补齐后端内部 `ResolvedModuleReadModel`。
6. Web bootstrap 返回 resolved views。
7. 增加 `RecordReadProjection` 内部模型，先用于列表字段校验和输出裁剪。
8. 补一个动态配置到 `ModuleUiDefinition` 的最小适配契约或 contract test 样例，至少覆盖 `uiConfigId`、发布快照、字段 ID 与 `ViewFieldRef` 的映射。
9. 废弃 service 层 `FormAbility` 作为 UI schema 来源。

第一阶段暂不做：

1. 复杂布局和栅格。
2. 表达式联动。
3. 子表表单。
4. 动作态字段差异。
5. 多客户端差异。
6. 静态 DAO 投影 SQL 的完整改造。
7. 动态 UI 配置的大规模重构。

## 命名建议

命名遵循项目既有边界：

| 名称 | 用途 |
| --- | --- |
| `Definition` | 源码或配置态声明，例如 `FormViewDefinition` |
| `ModuleUiDefinition` | 静态 DSL 和动态 UI 配置归一后的源无关 UI 定义 |
| `Descriptor` | 编译后的对外输出，例如 `ResolvedModuleUiDescriptor` |
| `ResolvedModuleReadModel` | 模块级后端内部读模型，不暴露给前端，不包含本次请求输出计划 |
| `Plan` / `Projection` | 后端内部执行计划，例如 `RecordReadProjection` |
| `moduleAlias` | 运行时模块身份，不使用 `scopeName` 表达同一件事 |
| `viewCode` | 模块内视图编码，用于区分 `list`、`form`、`detail` 等视图 |

`FormAbility` 不再作为目标形态。UI 声明不是 service ability，而是模块交付定义。
