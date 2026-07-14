# 动作结果与数据变更治理

本文记录前端动作成功结果的消费边界。目标是让静态页面和后续动态页面共用同一套业务事实语义，同时避免后端把刷新、关闭编辑器、清空选择等 UI 行为通知给前端。

## 稳定契约

动作成功结果按以下事实消费。当前前端已经具备解析和保留这些事实的能力；后端统一产出 `changes` 仍属于后续契约建设，不表示所有现有后端接口已经完整返回该字段。

| 字段         | 含义                                                   |
| ------------ | ------------------------------------------------------ |
| `message`    | 成功后的业务文案。前端可展示，但不把它解释成 UI 类型。 |
| `resultType` | 业务结果类型。前端当前只透传和测试，不做硬编码分支。   |
| `changes`    | 数据变更事实，表达哪些模块、记录或集合发生了变化。     |

`changes` 的元素必须是结构化对象：

```ts
{
  type: string;
  moduleAlias: string;
  recordId?: string;
  resourceKey?: string;
  scope?: string;
  [key: string]: unknown;
}
```

`changes` 只承载业务数据事实，不承载 toast、modal、placement、duration、关闭抽屉、刷新组件等 UI 呈现或交互语义。`resourceKey` 和 `scope` 用于表达同一页面内的业务资源或作用域，例如分类、明细项、职员、账号等，不等同于前端组件名称。

## 标准 Change Type

当前前端契约保留以下标准数据变更类型：

| type                 | 语义                                           |
| -------------------- | ---------------------------------------------- |
| `record-created`     | 指定模块下新增了一条记录。                     |
| `record-updated`     | 指定模块下某条记录发生更新。                   |
| `record-deleted`     | 指定模块下某条记录被删除。                     |
| `collection-changed` | 指定模块或资源集合发生变化，但不限定单条记录。 |

这些是业务数据事实，不是 UI adapter 语义。后续 SSE、WebSocket 或跨标签页广播应优先复用同一套数据变更事实模型。

## 前端本地 Reactions

刷新列表、刷新详情、关闭编辑器、清空选择和选择记录属于前端本地 reaction。它们可以由当前页面根据 `changes`、当前选中记录、编辑态、路由和业务上下文决定是否执行，也可以由页面动作流程追加默认 reaction。

当前前端本地仍保留以下 reaction type：

| type              | 语义                             |
| ----------------- | -------------------------------- |
| `refresh-list`    | 刷新当前资源列表、树或查询结果。 |
| `refresh-detail`  | 刷新当前记录详情。               |
| `close-editor`    | 关闭当前编辑态，回到查看态。     |
| `clear-selection` | 清空当前资源选择。               |
| `select-record`   | 选择指定记录。                   |

在后端返回 `changes` 后，页面可以自行映射为本地 reaction。例如当前页面正在展示 `iam.employee` 列表，收到 `iam.employee` 的 `record-updated` 后，可以选择刷新列表；如果正在编辑且存在未保存草稿，也可以选择提示用户而不是立即刷新。

## 分层边界

动作结果能力按以下层次组织：

| 层级                  | 职责                                                           |
| --------------------- | -------------------------------------------------------------- |
| `web-contracts`       | 定义 `WebActionResult`、`WebDataChange` 和标准 change type。   |
| `web-core`            | 解析动作结果、合并数据变更事实、按模块/记录/资源/作用域去重。  |
| `platform-components` | 绑定 Vue 页面状态 handler、前端本地 reactions 和成功提示适配。 |

业务页面应优先消费 `platform-components` 提供的门面；只有非 UI 的底层测试、客户端或运行器需要直接使用 `web-core`。

## 当前接入面

已接入标准 action result 和前端本地 reactions 的链路：

| 范围                        | 状态                                                   |
| --------------------------- | ------------------------------------------------------ |
| `staticFormActionFlow`      | 统一解析动作结果，支持本地 reaction handler。          |
| `staticCrudManagementState` | 平铺 CRUD 默认接入保存、启停、删除后的本地 reactions。 |
| 密码管理                    | 接入本地 reactions。                                   |
| 组织管理                    | 接入本地 reactions。                                   |
| 岗位管理                    | 分类和岗位两套资源分别接入本地 reactions。             |
| 字典管理                    | 类目和字典项两套资源分别接入本地 reactions。           |
| 动态运行器                  | 兼容读取旧 `refresh` 字段，并编译为本地 reactions。    |

`web-contracts` 已允许统一 `WebActionResult` 承载 `message`、`resultType`、`changes`；前端对旧顶层 `CountResult` 仍兼容保留这些事实。`staticModuleClient` 会保留后端已经返回的动作事实，不在 normalize 时丢弃。后端统一 `ActionResult` / `DataChange` 输出需要在后续后端契约治理中补齐。

## 保留在页面内的联动

以下逻辑暂不抽象成标准数据变更事实：

1. 切换 scope、分类、类目、机构时导致的选择重置。
2. 删除岗位或字典项后进入“创建占位”的页面特有状态。
3. 保存岗位后如果分类变化，同步切换当前分类并临时保留已保存岗位。
4. 菜单、角色、用户、员工等尚未进入本轮治理的专属状态机。
5. 外部刷新按钮触发的 `reloadKey += 1`。

这些逻辑要么依赖页面上下文，要么是具体业务工作流，不应急着压成后端动作结果契约。

## 后续 Review 触发点

进入前端整体架构 review 前，应先确认：

1. 是否需要继续迁部门、菜单、用户、角色、员工等专属状态机。
2. 多资源页面是否已足够使用 `changes.resourceKey/scope` 表达业务作用域，还是需要补充更具体的业务事实。
3. 后端动作结果是否准备统一到正式 `ActionResult` / `DataChange` 契约。
4. 是否需要把本地 reactions 从动作成功门面继续扩展到实时数据变更订阅。

整体 review 开始前应先通知项目负责人，再从 `web-contracts`、`web-core`、`platform-components`、页面状态层和动态/静态运行器归一方向一起评估。
