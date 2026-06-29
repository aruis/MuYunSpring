# 职员管理静态 UI 接入临时治理清单

本文是 `iam.employee` 职员管理作为静态业务前后端打通样板的临时推进清单。它服务于 [UI 声明与读投影设计](../../../architecture/UI_DECLARATION_AND_READ_PROJECTION.md) 的第一阶段落地，不作为长期产品文档保留；当静态 UI 声明、Web bootstrap 和前端消费链路稳定后，应将稳定契约回收到架构文档、专题文档或测试中，并删除本文。

## 目标边界

- [x] 以 `iam.employee` 职员管理验证静态 UI 声明最小闭环。
- [x] 后端声明 `listView` 和 `formView`，前端优先消费 resolved UI descriptor，减少页面内硬编码列和字段。
- [x] 保持 service 不承载 UI schema，逐步废弃 `FormAbility` 作为表单 schema 来源。
- [x] 保持 SQL 不直接消费 UI 配置；当前 descriptor 不暴露物理列、表名、schema 名或 SQL 片段。
- [ ] 不改变当前低代码页面交付以动态表单为主的阶段定位。

## 不做范围

- [ ] 不重构机构树 scope 和 `departmentScope` 外部查询。
- [ ] 不动态化职员账号、职员任岗、业务代办和受托代办子面板。
- [ ] 不做复杂布局、表达式联动、动作态字段差异和多客户端差异。
- [ ] 不在第一阶段改造静态 DAO 投影 SQL。
- [ ] 不把动态 UI 配置整体重构为新链路，只补最小适配契约或 contract test。

## 执行规划

### 阶段 0：现状锁定

- [x] 固化 `EmployeeManagementView` 当前行为：机构树选择、职员列表、详情抽屉、新建、编辑、删除、启停。
- [x] 固化 `RecordQueryListPanel` 当前查询行为：query schema、quick search、条件查询、分页和默认排序。
- [x] 固化 `EmployeeWebController` 当前静态模块扫描结果：模块 alias、路由、标准动作和自定义 record actions。
- [x] 明确当前页面内硬编码 UI 事实：`employeeColumns`、表单字段、必填校验、按钮和状态切换。
- [x] 产出测试基线，确保后续迁移不会改变当前职员管理可见行为。

### 阶段 1：后端声明模型最小实现

- [x] 新增源无关 UI 定义模型：`ModuleUiDefinition`、`ViewDefinition`。
- [x] 新增字段和规则模型：`ViewFieldRef`、常量版 `UiRule<T>`。
- [x] 为 `StaticModuleDefinition` 增加 UI definition 承载位置，或先引入独立 `StaticModuleUiContributor`。
- [x] 为 `iam.employee` 写入默认 `listView` 和 `formView` 声明。
- [x] 不改 `EmployeeService` 的业务校验和 CRUD 行为。
- [x] 后端单测证明 `iam.employee` UI 声明可以被扫描或贡献出来。

### 阶段 2：后端编译与 Web 出口

- [x] 实现静态 UI 来源归一：`StaticModuleDefinition / contributor -> ModuleUiDefinition`。
- [x] 实现最小 resolved 编译：`ModuleUiDefinition -> ResolvedModuleUiDescriptor`。
- [ ] 实现完整 resolved 编译：`ModuleUiDefinition + service/model facts -> ResolvedModuleUiDescriptor + ResolvedModuleReadModel`。
- [x] resolved 编译器已接入静态模块实体字段事实，能校验 UI 字段是否存在。
- [x] resolved 编译器已生成最小内部 `ResolvedModuleReadModel`，只包含逻辑字段事实，不进入 Web 响应。
- [x] 增加静态模块 UI bootstrap 或扩展模块 context，返回 `ModuleUiDefinition` views。
- [x] 保留 `/iam.employee/query/schema` 当前查询能力。
- [x] 迁移期保留 `/iam.employee/form/schema`，但来源改为 UI definition form view。
- [x] 后端单测证明 descriptor 不包含物理列、表名、schema 名或 SQL 片段。

### 阶段 3：前端列表消费

- [x] 在 `web-contracts` 中定义 UI definition 类型。
- [x] 在 `web-core` 的 `ModuleContext` 中增加 UI bootstrap 读取能力。
- [x] `RecordQueryListPanel` 支持从 resolved UI descriptor list view 推导 columns，并以 UI definition 兜底。
- [x] `RecordQueryListPanel` 保留手写 `columns` 兼容路径。
- [x] `EmployeeManagementView` 改为使用 UI definition list view 生成职员列表列。
- [x] 前端测试证明 `EmployeeManagementView` 不再硬编码 `employeeColumns`。

### 阶段 4：前端表单对齐

- [x] `EmployeeManagementView` 继续保留当前手写表单结构。
- [x] 表单字段标题、必填和只读语义逐步从 UI definition form view 读取。
- [x] 保存前端校验只做用户体验提示，最终校验仍由 `EmployeeService` 兜底。
- [x] 部门 `RecordPicker`、机构上下文、启停开关和删除确认继续手写。
- [x] 前端测试证明表单字段标题、必填和只读语义与 UI definition form view 对齐。

### 阶段 5：读投影准备与动态最小契约

- [x] 增加 `RecordReadProjection` 内部模型，先用于字段校验和输出裁剪。
- [ ] 读投影叠加动作授权、数据范围、字段保护和字段级可读策略。
- [ ] `/iam.employee/query` 第一阶段可继续读取完整实体，不改静态 DAO SQL。
- [ ] 补动态配置到 `ModuleUiDefinition` 的最小 contract test，覆盖 `uiConfigId`、发布快照、字段 ID 与 `ViewFieldRef` 映射。
- [ ] 明确动态配置接入只验证归一契约，不做动态 UI 配置整体重构。

### 阶段 6：收口与清理

- [x] `EmployeeService` 不再实现或不再作为 `FormAbility` schema 来源。
- [ ] 删除前端职员列表列的硬编码兼容分支，前提是至少一个版本验证稳定。
- [ ] 将稳定契约回收到架构文档、页面交付专题或 contract test。
- [ ] 删除本文，或把剩余未完成项迁移到技术债记录。

## 后端定义

- [x] 定义 `ModuleUiDefinition`、`ViewDefinition`。
- [x] 定义 `ViewFieldRef`，支持静态主表字段短写法归一为 `relationCode=null + fieldName`。
- [x] 定义常量版 `UiRule<T>`，先支持 `visible`、`required`、`readOnly` 的常量规则。
- [x] 为 `StaticModuleDefinition` 增加 UI definition 承载位置，或提供独立 `StaticModuleUiContributor`。
- [x] 为 `iam.employee` 声明默认列表视图：`employeeNo`、`title`、`mobile`、`email`、`enabled`。
- [x] 为 `iam.employee` 声明默认表单视图：`organizationId`、`departmentId`、`employeeNo`、`title`、`gender`、`mobile`、`email`、`enabled`。
- [x] `iam.employee` 视图声明接驳 `EmployeeService` 已知事实，但不要求 `EmployeeService` 实现新的 UI ability。
- [x] 保留 `EmployeeService` 的业务保存校验：机构、部门、职员编号、职员姓名和唯一性校验仍由 service 负责。

## 编译与契约

- [ ] 编写静态 UI 编译器：`StaticModuleDefinition / contributor -> ModuleUiDefinition`。
- [x] 编写最小 resolved 编译器：`ModuleUiDefinition -> ResolvedModuleUiDescriptor`。
- [ ] 编写完整 resolved 编译器：`ModuleUiDefinition + service/model facts -> ResolvedModuleUiDescriptor + ResolvedModuleReadModel`。
- [x] 编译时校验视图字段存在于模型字段、平台标准字段或能力字段中。
- [x] `/form/schema` 兼容出口已复用静态模型事实，能合并 `@OptionField` 字典和选项标题字段。
- [ ] 编译时完整合并字段标题、字段类型、选项、引用、保护、能力字段等事实。
- [x] 对外 `ResolvedModuleUiDescriptor` 不包含物理列、表名、schema 名或 SQL 片段。
- [x] 后端内部 `ResolvedModuleReadModel` 只表达模块级字段读事实，不包含本次请求输出字段、物理列集合或后处理任务。
- [x] `RecordReadProjection` 作为 view/request 级计划，先承载 `ViewFieldRef` 形态的 `outputFields`、平台必需字段和 `postReadTransforms`。
- [ ] `RecordReadProjection` 后续在 SQL 投影阶段再承载后端白名单解析后的 `selectColumns`。
- [ ] 读投影必须叠加动作授权、数据范围、字段保护和字段级可读策略。
- [ ] 补动态配置到 `ModuleUiDefinition` 的最小 contract test，覆盖 `uiConfigId`、发布快照、字段 ID 与 `ViewFieldRef` 映射。

## Web 出口

- [x] 增加静态模块 UI bootstrap 或扩展现有模块 context，返回 `iam.employee` 的 resolved UI descriptor views，并迁移期保留 `ModuleUiDefinition`。
- [x] Web 响应不暴露 `sourceKind` 给前端运行器作为分支依据。
- [x] `/iam.employee/form/schema` 在迁移期可以兼容，但来源应改为 UI definition form view，而不是 `FormAbility`。
- [x] `/iam.employee/form/schema` 由 UI definition form view 声明字段顺序和 UI 语义，同时合并 `Employee` 静态模型上的选项事实。
- [x] `/iam.employee/query/schema` 暂时保留，继续服务查询条件、quick search 和默认排序。
- [x] `/iam.employee/query` 第一阶段可继续返回完整记录；读投影已具备独立字段校验和输出裁剪能力。
- [ ] 将 `RecordReadProjection` 输出裁剪接入 `/iam.employee/query` 响应，需先确认静态 CRUD Web 响应类型迁移策略。

## 前端消费

- [x] 在 `web-contracts` 中补充 UI definition 类型。
- [x] 在 `web-core` 中为 `ModuleContext` 增加 UI bootstrap 读取能力。
- [x] `RecordQueryListPanel` 支持从 resolved UI descriptor list view 推导 columns，并以 UI definition 兜底。
- [x] `RecordQueryListPanel` 保留手写 `columns` 入参作为兼容路径。
- [x] `EmployeeManagementView` 移除硬编码 `employeeColumns`，改为消费后端 resolved UI descriptor list view。
- [x] `EmployeeManagementView` 表单字段先保留当前手写结构，但字段标题、必填和只读语义逐步对齐 UI definition form view。
- [x] 机构树、部门 `RecordPicker`、保存动作、启停动作、删除确认继续保持当前手写业务编排。
- [x] 页面权限判断继续使用 `employeeContext.can(...)`，不因 descriptor 中有字段或动作就默认可执行。

## 测试与验收

- [x] 后端测试覆盖 `iam.employee` 静态 UI 声明被扫描或贡献到 `StaticModuleDefinition`。
- [x] 后端测试覆盖 `iam.employee` UI definition list view 字段顺序和字段标题。
- [x] 后端测试覆盖 `iam.employee` UI definition form view 必填字段和只读字段。
- [x] 后端测试覆盖 descriptor 不包含物理列或 SQL 信息。
- [ ] 后端测试覆盖动态配置最小样例可归一到同一套 `ModuleUiDefinition`。
- [x] 前端测试覆盖 `RecordQueryListPanel` 可由 UI definition list view 生成 columns。
- [x] 前端测试覆盖 `EmployeeManagementView` 不再硬编码职员列表列。
- [x] 前端测试覆盖手写 columns 兼容路径仍可用。
- [x] 前端测试覆盖显式 columns 场景不强依赖菜单级 runtime context，避免只有查询权限时列表无法加载。
- [x] 运行 `./gradlew test`。
- [x] 运行前端相关测试。

## 收口条件

- [x] `iam.employee` 能通过后端 UI 声明驱动列表列展示。
- [x] `iam.employee` 表单视图能通过 UI definition 表达字段顺序、标题、必填和只读语义。
- [x] `EmployeeService` 不再作为表单 schema 来源。
- [ ] 静态和动态都存在进入 `ModuleUiDefinition` 的最小契约证据。
- [ ] 读投影链路具备字段白名单、权限裁剪和输出裁剪的测试证据。
- [x] 当前已有字段白名单和输出裁剪测试证据；权限裁剪仍待接入。
- [ ] 稳定设计已回收到架构文档或专题文档，本文可删除。
