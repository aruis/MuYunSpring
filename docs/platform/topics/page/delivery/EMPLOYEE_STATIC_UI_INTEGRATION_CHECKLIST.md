# 职员管理静态 UI 接入收口记录

本文记录 `iam.employee` 作为静态业务前后端打通样板的阶段性收口结果。稳定路线已回收到 [UI 声明与读投影设计](../../../architecture/UI_DECLARATION_AND_READ_PROJECTION.md)，本文只保留样板现状、仍需专项治理的边界和删除条件。

## 已稳定契约

1. `EmployeeService` 不承载 UI schema，`FormAbility` 不再作为职员表单 schema 来源。
2. 前端正式运行协议为 `ResolvedModuleUiDescriptor`，runtime context 不再暴露或消费来源态 `uiDefinition`。
3. `iam.employee` 静态 UI 声明已覆盖默认列表视图和默认表单视图。
4. descriptor 不暴露物理列、表名、schema 名或 SQL 片段。
5. `/iam.employee/query` 响应按 resolved list view 和 `RecordReadProjection` 输出裁剪；SQL 仍暂时读取完整实体。
6. `StaticRecordReadProjectionService` 已作为静态模块读投影门面，负责投影编译和 Web 响应重建。
7. 动态发布快照已有最小归一证据：可转换为静态声明共用的 `ModuleUiDefinition` 主线。

## 前端样板现状

1. `RecordQueryListPanel` 可从 resolved list view 推导列表列，并提供标准 CRUD 顶部动作和行级动作。
2. `RecordFormFields` 已承接普通输入字段、`enabledStatus` 和 `recordPicker` 控件。
3. `executeStaticFormSave` 已承接保存动作的重复提交保护、权限提示、校验提示、loading、成功反馈和异常反馈。
4. `executeStaticRecordAction` 已承接启停和删除动作的重复提交保护、权限提示、确认后的 loading、成功反馈和异常反馈。
5. `EmployeeManagementView` 不再硬编码职员列表列、普通表单字段、启停表单控件、部门选择器、保存流程、启停流程和删除流程的通用动作样板。

## 仍属业务编排

以下内容继续留在职员业务页，不应为了“完全配置化”提前下沉：

1. 机构树选择、组织作用域和 `departmentScope` 外部查询值。
2. `recordPicker` 的候选来源、作用域、标题函数和刷新 key。
3. 保存 payload 归一化，例如组织 ID 注入、字符串 trim、空字符串归一为 `undefined`。
4. 启停动作的 enable / disable 分支选择。
5. 删除确认文案和删除后的页面状态同步。
6. 职员账号、职员任岗、业务代办和受托代办子面板。

## 后续专项

1. 字段级授权配置和角色授权存储模型。
2. `RecordReadProjection` 的 SQL 列投影阶段，包含后端白名单解析后的 `selectColumns`。
3. 动态发布快照接入共用 descriptor、读投影和前端运行器的真实 Web 链路。
4. 标准运行器继续扩展到更多静态样板：树/排序、引用/字典/枚举、普通 CRUD 和动态模块。
5. 动作后的页面状态同步如果在多个模块重复出现，再抽为更高层页面状态运行器；不要只为职员样板提前抽象。

## 验收证据

1. 后端测试覆盖静态 UI 声明扫描、descriptor 编译、runtime context 协议、读投影计划和 `/iam.employee/query` 输出裁剪。
2. 后端测试覆盖 `StaticRecordReadProjectionService`、字段保护输出策略、action 权限上下文和字段级可读策略。
3. 后端测试覆盖动态配置最小样例可归一到同一套 `ModuleUiDefinition`。
4. 前端测试覆盖 `RecordQueryListPanel`、`RecordFormFields`、标准 CRUD 动作、保存动作执行器、记录动作执行器和 `EmployeeManagementView` 接入契约。
5. 阶段验证命令：`npm test --prefix muyun-web`、`npm run build --prefix muyun-web`、`./gradlew test`、`git diff --check`。

## 删除条件

当后续专项治理入口全部迁移到长期架构文档、页面交付专题或独立 issue 后，本文可以删除。删除前确认：

1. [UI 声明与读投影设计](../../../architecture/UI_DECLARATION_AND_READ_PROJECTION.md) 已覆盖目标协议、分层边界、读投影和运行器路线。
2. 页面交付专题已覆盖动态发布快照接入共用 descriptor 和运行器的真实链路。
3. 职员样板相关契约均有测试覆盖，不依赖本文解释才能维护。
