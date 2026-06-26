# 静态管理页组件边界

本文记录静态管理页的组件拆分口径，用于指导后续平台配置、身份权限和稳定业务模块页面建设。

## 核心原则

静态管理页优先复用平台组件，但不为了统一形状强行封装。组件边界按职责拆分：

1. 页面负责业务编排、表单字段、动作含义和跨区联动。
2. `RecordExplorerPanel` 负责 explorer 外壳，包括标题、刷新、搜索入口、动作区和内容槽。
3. `RecordListExplorer` 负责纯平铺列表展示，只消费已加载记录。
4. `CrudRecordListExplorer` 负责标准 CRUD 平铺列表的数据加载适配，内部复用 `RecordListExplorer`。
5. `TreeRecordExplorer` 负责标准树能力的数据加载和树展示。
6. 业务语义封装只有在出现真实复用场景后再沉淀，例如未来的机构树选择或组织范围浏览。

## 组件层级

### RecordExplorerPanel

`RecordExplorerPanel` 是 explorer 的外壳组件，不访问业务数据。

它适合承载：

```text
标题
刷新按钮
搜索输入框
右上角动作
内容 slot
轻量 editor slot
```

它不应承载表单保存、记录加载、权限解释或业务状态机。

管理页 explorer 标题使用单行标题，不显示业务分组 eyebrow。业务分组属于页面导航或详情区语义，不进入 explorer header。

右上角动作区应保持克制：

1. 常规管理页只放一个主新增动作，使用 icon-only 圆形按钮。
2. 同一动作区不放多个相同图标的 icon-only 按钮。
3. 如果存在“新建根节点 / 新建下级”等多个创建语义，侧栏只保留默认主动作，其他动作放到详情动作区或后续菜单组件。
4. 动作语义不能只依赖 `title` tooltip 区分。

### RecordListExplorer

`RecordListExplorer` 是纯列表 body。

它只负责：

```text
records 展示
keyword 本地过滤
selected 高亮
停用或自定义 tag
inline action
select/action 事件
```

如果父页面已经持有记录集合、loading、错误和选择联动，应直接使用 `RecordListExplorer`。岗位管理的岗位列表属于这种场景，因为它依赖左侧分类和贡献动作权限。

列表项视觉由 `UiRecordExplorerItem` 固定。业务页面不应通过外层容器继承来改变 explorer item 的字号、行高、hover 或 selected 样式。树和列表使用同一 item 视觉契约。

### CrudRecordListExplorer

`CrudRecordListExplorer` 是标准 CRUD 平铺列表适配器。

它负责：

```text
等待 module runtime ready
调用 context.abilities.crud().query
维护 loading/error
响应 reloadKey
emit loaded/select
内部复用 RecordListExplorer
```

它适合应用、租户等独立平铺 CRUD 管理页。它不应继续增长为业务动作容器、表单状态容器或复杂查询面板。

如果某个页面的列表数据依赖其他区域选择、需要特殊权限组合、需要父页面统一控制加载时机，应把加载状态放在页面 state 中，直接使用 `RecordListExplorer`。

### TreeRecordExplorer

`TreeRecordExplorer` 是标准树能力适配器。

它负责：

```text
等待 module runtime ready
调用 context.abilities.tree().tree
维护 loading/error
响应 reloadKey
维护展开状态
树过滤
emit loaded/select/action
```

当管理页已经有 `RecordExplorerPanel` 时，搜索入口应放在 panel，树组件通过 `keyword` 消费搜索词，并设置 `searchMode="none"` 关闭内置搜索行。

树节点也使用 `UiRecordExplorerItem` 的视觉契约。不要在业务页单独覆盖 Ant Tree 字号来制造页面差异。

业务语义树组件不提前沉淀。比如机构树只有在多个业务页面真实复用时，再基于 `TreeRecordExplorer` 轻封装 `OrganizationTree` 或 `OrganizationSelectTree`。

## 常见组合

两栏平铺管理页：

```text
StaticManagementLayout
  -> RecordExplorerPanel
    -> CrudRecordListExplorer
  -> detail card
```

两栏树管理页：

```text
StaticManagementLayout
  -> RecordExplorerPanel
    -> TreeRecordExplorer
  -> detail card
```

三栏主子管理页：

```text
RecordExplorerPanel
  -> TreeRecordExplorer
RecordExplorerPanel
  -> RecordListExplorer
detail area
```

## 当前页面口径

| 页面 | Explorer 组合 | 状态口径 |
| --- | --- | --- |
| 应用管理 | `StaticManagementLayout -> CrudRecordListExplorer -> RecordListExplorer` | 平铺 CRUD 状态复用 `useFlatCrudManagementState`。 |
| 租户管理 | `StaticManagementLayout -> CrudRecordListExplorer -> RecordListExplorer` | 平铺 CRUD 状态复用 `useFlatCrudManagementState`，页面保留平台租户保护规则。 |
| 组织管理 | `StaticManagementLayout -> TreeRecordExplorer` | 页面直接依赖树能力，避免在主业务页套业务语义树封装。 |
| 岗位管理 | `RecordExplorerPanel -> TreeRecordExplorer` 和 `RecordExplorerPanel -> RecordListExplorer` | 分类树和岗位列表由页面统一编排，岗位列表加载依赖选中分类。 |

## 新页面判断

新增静态管理页时按顺序判断：

1. 是否是独立平铺 CRUD 列表。是则优先使用 `CrudRecordListExplorer`。
2. 是否是标准树模型。是则优先使用 `TreeRecordExplorer`。
3. 列表数据是否依赖其他区域选择或复杂权限组合。是则页面 state 自己加载，body 使用 `RecordListExplorer`。
4. 是否需要业务语义封装。只有跨页面真实复用后再沉淀，不为单一页面提前封装。
5. 是否需要新的平台组件。只有它能降低重复、稳定边界并减少接入成本时再新增。

## 状态机约束

管理页进入 create/edit 模式时，要明确取消后的返回锚点。

1. 编辑已有记录时，取消回到当前选中记录。
2. 新建子记录时，取消回到触发新建时的父级或列表上下文。
3. 新建根记录时，如果进入前已有选中记录，取消应回到原选中记录。
4. 不要用“当前 selected 是否为空”隐式决定取消行为，除非该页面已经明确没有返回对象。
5. 复杂主子页面应为 create/edit/cancel 补状态测试。
