# 动作结果与后效应治理

本文记录前端动作成功结果的消费边界。目标是让静态页面和后续动态页面共用同一套业务交互语义，避免页面直接把后端返回值翻译成分散的刷新、关闭、清空选择和成功提示代码。

## 稳定契约

动作成功结果按以下事实消费：

| 字段 | 含义 |
| --- | --- |
| `message` | 成功后的业务文案。前端可展示，但不把它解释成 UI 类型。 |
| `resultType` | 业务结果类型。前端当前只透传和测试，不做硬编码分支。 |
| `effects` | 动作后效应列表，表达业务交互后续动作。 |

`effects` 的元素必须是结构化对象：

```ts
{
  type: string;
  payload?: Record<string, unknown>;
}
```

前端不接受裸字符串 effect。`payload` 只能承载业务交互事实，不承载 toast、modal、placement、duration 等 UI 呈现语义。

## 标准 Effect Type

当前前端已稳定使用以下标准 effect type：

| type | 语义 |
| --- | --- |
| `refresh-list` | 刷新当前资源列表、树或查询结果。 |
| `refresh-detail` | 刷新当前记录详情。当前保留为标准语义，尚未大面积使用。 |
| `close-editor` | 关闭当前编辑态，回到查看态。 |
| `clear-selection` | 清空当前资源选择。 |
| `select-record` | 选择指定记录。当前保留为标准语义，尚未大面积使用。 |

这些是业务交互语义，不是 UI adapter 语义。

## 前端默认 Effects

在后端还没有统一返回 `effects` 前，前端允许为标准动作追加本地默认 effects：

| 场景 | 默认 effects |
| --- | --- |
| 保存成功 | `close-editor` + `refresh-list` |
| 启停成功 | `refresh-list` |
| 删除成功 | 通常是 `clear-selection` + `refresh-list` |

如果后端已经返回同类型 effect，前端本地默认 effect 按 type 去重，不重复执行。后端返回的同类型 effect 优先保留。

## 当前接入面

已接入标准 action result/effects 的前端链路：

| 范围 | 状态 |
| --- | --- |
| `staticFormActionFlow` | 统一解析动作结果，支持 effect handler。 |
| `staticCrudManagementState` | 平铺 CRUD 默认接入保存、启停、删除后效应。 |
| 密码管理 | 接入标准后效应。 |
| 组织管理 | 接入标准后效应。 |
| 岗位管理 | 分类和岗位两套资源分别接入标准后效应。 |
| 字典管理 | 类目和字典项两套资源分别接入标准后效应。 |

`web-contracts` 已允许 `WebRecordResponse` 和 `WebCountResponse` 承载 `message`、`resultType`、`effects`。`staticModuleClient` 会保留这些事实，不在 normalize 时丢弃。

## 保留在页面内的联动

以下逻辑暂不抽象成标准 effects：

1. 切换 scope、分类、类目、机构时导致的选择重置。
2. 删除岗位或字典项后进入“创建占位”的页面特有状态。
3. 保存岗位后如果分类变化，同步切换当前分类并临时保留已保存岗位。
4. 菜单、角色、用户、员工等尚未进入本轮治理的专属状态机。
5. 外部刷新按钮触发的 `reloadKey += 1`。

这些逻辑要么依赖页面上下文，要么是具体业务工作流，不应急着压成通用 action effect。

## 剩余扫描结论

当前剩余手写后效应主要分布在：

| 范围 | 结论 |
| --- | --- |
| 应用、租户、密码、组织、岗位、字典 | 已由标准 effects 覆盖动作成功后的主要刷新和编辑态收口。 |
| 部门管理 | 依赖机构 scope 和部门树上下文，暂保留页面状态机。 |
| 菜单管理 | 同时涉及菜单方案和菜单树，暂保留页面状态机。 |
| 用户、角色、员工、系统用户 | 涉及绑定、授权、详情加载和多区块联动，暂不纳入本轮 action effect 治理。 |

后续如果继续治理这些页面，应先判断页面是否能用标准 resource effect 表达；不能表达时，不应为了统一而牺牲业务直觉。

## 后续 Review 触发点

前端 action result/effects 已具备阶段闭环。进入前端整体架构 review 前，应先确认：

1. 是否需要继续迁部门、菜单、用户、角色、员工等专属状态机。
2. 是否需要为多资源页面约定 `payload.resource` 一类可选事实。
3. 是否准备把后端动作结果统一到正式 `ActionResult` 契约。

整体 review 开始前应先通知项目负责人，再从 `web-contracts`、`web-core`、`platform-components`、页面状态层和动态/静态运行器归一方向一起评估。
