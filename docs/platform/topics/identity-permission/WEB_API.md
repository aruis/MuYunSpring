# 身份与权限 Web API

本文按当前 Controller 中已存在的 URL 梳理身份与权限相关 Web 入口，只列功能点，不写完整 OpenAPI。

受保护的静态管理入口通过 Web mixin 或动作端点进入当前用户、租户、动作权限和数据权限上下文。登录、登出和当前用户菜单入口按各自 Controller 语义处理。

## 登录与当前身份

| 方法 | URL | 功能 |
| --- | --- | --- |
| `POST` | `/iam.auth/login` | 用户登录。请求包含 `tenantId`、`username`、`password`；返回 Bearer token、签发时间和当前用户信息。 |
| `POST` | `/iam.auth/logout` | 当前 Bearer token 登出。token 从 `Authorization: Bearer ...` 读取。 |
| `GET` | `/iam.auth/context` | 返回当前请求解析出的用户上下文，用于前端会话恢复和启动态确认。 |

后续请求通过 `Authorization: Bearer <token>` 解析当前用户。解析成功后，Web Filter 会写入 `CurrentUserContext`；租户用户同步写入 `TenantContext`，系统用户进入系统态。

## 通用管理接口

下列模块复用通用 CRUD、启停、排序或树接口：

| 模块 | 根路径 | 能力 |
| --- | --- | --- |
| 租户 | `/iam.tenant` | CRUD、启停、排序、系统态访问 |
| 组织机构 | `/iam.organization` | CRUD、启停、树、树内排序 |
| 部门 | `/iam.department` | CRUD、启停、树、树内排序 |
| 职员 | `/iam.employee` | CRUD、启停、排序 |
| 岗位 | `/iam.position` | CRUD、启停、排序 |
| 用户 | `/iam.user` | CRUD、启停、排序 |
| 角色 | `/iam.role` | CRUD、启停、排序 |

当前身份权限专题没有 Controller 继承 `ReadOnlyWeb`。`ReadOnlyWeb` 的 `query`、`view` 映射用于其他专题的只读对象，不在本专题 URL 清单内。

通用接口：

| 方法 | URL | 功能 |
| --- | --- | --- |
| `POST` | `/{moduleAlias}/query` | 分页查询。支持的条件以对应 Web 层和服务能力为准。 |
| `GET` | `/{moduleAlias}/view/{id}` | 查看单条记录。 |
| `POST` | `/{moduleAlias}/insert` | 新增记录。 |
| `POST` | `/{moduleAlias}/update/{id}` | 更新记录。 |
| `POST` | `/{moduleAlias}/delete/{id}` | 删除记录。 |
| `POST` | `/{moduleAlias}/enable/{id}` | 启用记录。 |
| `POST` | `/{moduleAlias}/disable/{id}` | 停用记录。 |
| `POST` | `/{moduleAlias}/sort/{id}` | 同级排序；树模块也使用该路径做树内移动。 |

树模块额外接口：

| 方法 | URL | 功能 |
| --- | --- | --- |
| `GET` | `/{moduleAlias}/tree` | 读取根节点树；`flat=true` 时返回扁平列表。 |
| `GET` | `/{moduleAlias}/tree/{id}` | 读取指定节点子树；支持 `flat` 和 `includeSelf` 参数。 |

## 租户

根路径：`/iam.tenant`

| 方法 | URL | 功能 |
| --- | --- | --- |
| `POST` | `/iam.tenant/query` | 查询租户。 |
| `GET` | `/iam.tenant/view/{id}` | 查看租户。 |
| `POST` | `/iam.tenant/insert` | 新增租户；租户 alias 当前等同于记录 ID。 |
| `POST` | `/iam.tenant/update/{id}` | 更新租户。 |
| `POST` | `/iam.tenant/delete/{id}` | 删除租户。 |
| `POST` | `/iam.tenant/enable/{id}` | 启用租户。 |
| `POST` | `/iam.tenant/disable/{id}` | 停用租户。 |
| `POST` | `/iam.tenant/sort/{id}` | 调整租户排序。 |

## 组织机构

根路径：`/iam.organization`

| 方法 | URL | 功能 |
| --- | --- | --- |
| `POST` | `/iam.organization/query` | 查询机构。 |
| `GET` | `/iam.organization/view/{id}` | 查看机构。 |
| `POST` | `/iam.organization/insert` | 新增机构。 |
| `POST` | `/iam.organization/update/{id}` | 更新机构。 |
| `POST` | `/iam.organization/delete/{id}` | 删除机构。 |
| `POST` | `/iam.organization/enable/{id}` | 启用机构。 |
| `POST` | `/iam.organization/disable/{id}` | 停用机构。 |
| `GET` | `/iam.organization/tree` | 读取机构树。 |
| `GET` | `/iam.organization/tree/{id}` | 读取指定机构子树。 |
| `POST` | `/iam.organization/sort/{id}` | 调整机构树位置或同级顺序。 |

## 部门

根路径：`/iam.department`

| 方法 | URL | 功能 |
| --- | --- | --- |
| `POST` | `/iam.department/query` | 查询部门。 |
| `GET` | `/iam.department/view/{id}` | 查看部门。 |
| `POST` | `/iam.department/insert` | 新增部门；必须指定所属机构。 |
| `POST` | `/iam.department/update/{id}` | 更新部门。 |
| `POST` | `/iam.department/delete/{id}` | 删除部门。 |
| `POST` | `/iam.department/enable/{id}` | 启用部门。 |
| `POST` | `/iam.department/disable/{id}` | 停用部门。 |
| `GET` | `/iam.department/tree` | 读取部门树；查询条件可按机构过滤。 |
| `GET` | `/iam.department/tree/{id}` | 读取指定部门子树。 |
| `POST` | `/iam.department/sort/{id}` | 调整部门树位置或同级顺序。 |

## 职员

根路径：`/iam.employee`

| 方法 | URL | 功能 |
| --- | --- | --- |
| `POST` | `/iam.employee/query` | 查询职员。 |
| `GET` | `/iam.employee/view/{id}` | 查看职员。 |
| `POST` | `/iam.employee/insert` | 新增职员；必须指定所属机构和部门。 |
| `POST` | `/iam.employee/update/{id}` | 更新职员基础信息。 |
| `POST` | `/iam.employee/delete/{id}` | 删除职员。 |
| `POST` | `/iam.employee/enable/{id}` | 启用职员。 |
| `POST` | `/iam.employee/disable/{id}` | 停用职员。 |
| `POST` | `/iam.employee/sort/{id}` | 调整职员在部门内的排序。 |

## 岗位

根路径：`/iam.position`

| 方法 | URL | 功能 |
| --- | --- | --- |
| `POST` | `/iam.position/query` | 查询岗位标准项。 |
| `GET` | `/iam.position/view/{id}` | 查看岗位标准项。 |
| `POST` | `/iam.position/insert` | 新增岗位标准项。 |
| `POST` | `/iam.position/update/{id}` | 更新岗位标准项。 |
| `POST` | `/iam.position/delete/{id}` | 删除岗位标准项。 |
| `POST` | `/iam.position/enable/{id}` | 启用岗位标准项。 |
| `POST` | `/iam.position/disable/{id}` | 停用岗位标准项。 |
| `POST` | `/iam.position/sort/{id}` | 调整岗位标准项排序。 |

## 用户

根路径：`/iam.user`

| 方法 | URL | 功能 |
| --- | --- | --- |
| `POST` | `/iam.user/query` | 查询用户。 |
| `GET` | `/iam.user/view/{id}` | 查看用户。 |
| `POST` | `/iam.user/insert` | 新增用户；写入密码后服务端保存密码哈希。 |
| `POST` | `/iam.user/update/{id}` | 更新用户基础信息。 |
| `POST` | `/iam.user/delete/{id}` | 删除用户。 |
| `POST` | `/iam.user/enable/{id}` | 启用用户。 |
| `POST` | `/iam.user/disable/{id}` | 停用用户。 |
| `POST` | `/iam.user/sort/{id}` | 调整用户排序。 |
| `POST` | `/iam.user/changePassword/{id}` | 修改用户密码；成功后撤销该用户现有 session。 |
| `POST` | `/iam.user/selector/query` | 用户选择器查询；支持按角色、组织、关键字和启用状态过滤，返回轻量用户项。 |

## 角色、角色绑定与授权

根路径：`/iam.role`

| 方法 | URL | 功能 |
| --- | --- | --- |
| `POST` | `/iam.role/query` | 查询角色。 |
| `GET` | `/iam.role/view/{id}` | 查看角色。 |
| `POST` | `/iam.role/insert` | 新增角色。 |
| `POST` | `/iam.role/update/{id}` | 更新角色。 |
| `POST` | `/iam.role/delete/{id}` | 删除角色。 |
| `POST` | `/iam.role/enable/{id}` | 启用角色。 |
| `POST` | `/iam.role/disable/{id}` | 停用角色。 |
| `POST` | `/iam.role/sort/{id}` | 调整角色排序。 |
| `POST` | `/iam.role/users/{roleId}/bind` | 为角色批量绑定用户。 |
| `POST` | `/iam.role/users/{roleId}/unbind` | 为角色批量解绑用户。 |
| `GET` | `/iam.role/users/{roleId}` | 查询角色已绑定用户 ID。 |
| `POST` | `/iam.role/grant/{roleId}` | 授予角色某个 `moduleAlias + actionCode`，可携带数据权限策略、租户范围策略和引用依赖参数。 |
| `POST` | `/iam.role/grant/{roleId}/batch` | 批量授予角色多个模块动作；每项请求体复用单动作授权字段。 |
| `POST` | `/iam.role/wildcard-data-scope/{roleId}/grant` | 为数据权限通配角色授予通配数据范围动作。 |
| `POST` | `/iam.role/revoke/{roleId}` | 撤销角色某个模块动作授权。 |
| `POST` | `/iam.role/revoke/{roleId}/batch` | 批量撤销角色多个模块动作授权。 |
| `POST` | `/iam.role/permissionMatrix/{roleId}` | 按模块列表返回角色授权矩阵，用于回显可授权动作和已授权状态。 |
| `GET` | `/iam.role/menuMatrix/{roleId}/{schemeId}` | 按菜单方案返回菜单树和角色对模块菜单的授权状态。 |

授权请求中常见字段：

| 字段 | 说明 |
| --- | --- |
| `moduleAlias` | 平台模块别名，如 `iam.user`。 |
| `actionCode` | 动作编码。标准动作和配置动作最终都归到权限动作。 |
| `dataScopePolicy` | 数据范围策略，当前 JSON 使用 enum 名称，如 `ALL`、`OWNER`、`ORGANIZATION_AND_CHILDREN`。 |
| `tenantScopePolicy` | 租户范围策略，当前 JSON 使用 enum 名称，如 `CURRENT_TENANT`、`ALL_TENANTS`。 |
| `scopeCondition` | 自定义条件保留字段；当前不开放可执行自定义条件授权。 |
| `referenceFieldId` | 引用依赖数据权限使用的引用字段。 |
| `referenceActionCode` | 引用依赖数据权限使用的目标动作。 |

## 菜单剪枝

菜单维护本身属于平台菜单能力。身份权限相关的当前用户入口为：

| 方法 | URL | 功能 |
| --- | --- | --- |
| `GET` | `/platform.menu/mine` | 返回当前用户可见菜单树。后端按当前用户推理菜单方案，并按模块 `menu` 动作做剪枝。 |

该接口不接收前端传入的菜单方案参数；剪枝只影响返回结果，不修改菜单配置。

角色菜单授权视图使用 `/iam.role/menuMatrix/{roleId}/{schemeId}`。它不引入单独的角色-菜单权限模型：模块菜单是否可见仍由角色对目标模块的 `menu` 动作授权决定。保存菜单授权时，可使用 `/iam.role/grant/{roleId}`、`/iam.role/grant/{roleId}/batch`、`/iam.role/revoke/{roleId}` 或 `/iam.role/revoke/{roleId}/batch` 维护 `menu` 动作。

## 动态与静态动作接入线索

身份权限专题的 Web API 不单独定义动态模块 URL。动态模块仍使用动态 Web 入口，静态模块仍使用各自 Controller 入口；二者通过动作上下文和授权服务共享同一套判断：

1. 静态 Web 标准接口通过 `@ActionEndpoint` 标记查询、查看、新增、更新、删除、启停、排序、树等动作。
2. 动态 Web 标准 CRUD、动作执行、动作列表、记录动作可用性、descriptor 和 OpenAPI 复用 `moduleAlias + actionCode` 授权口径。
3. 需要数据权限的入口会把数据范围合入查询条件或校验目标记录范围。

## 留给其他专题的 URL

以下 URL 会复用当前用户、租户、动作权限或数据权限上下文，但不由身份权限专题维护接口清单：

| URL 形态 | 归属 | 说明 |
| --- | --- | --- |
| `/{moduleAlias}/query`、`/{moduleAlias}/view/{id}`、`/{moduleAlias}/insert`、`/{moduleAlias}/update/{id}`、`/{moduleAlias}/delete/{id}`、`/{moduleAlias}/enable/{id}`、`/{moduleAlias}/disable/{id}`、`/{moduleAlias}/sort/{id}`、`/{moduleAlias}/tree`、`/{moduleAlias}/tree/{id}` | 动态运行态 | 动态模块标准 Web 入口；权限专题只提供授权和数据范围裁剪。 |
| `/{moduleAlias}/actions`、`/{moduleAlias}/actions/{recordId}`、`/{moduleAlias}/{actionCode}`、`/{moduleAlias}/{actionCode}/{recordId}`、`/{moduleAlias}/{actionCode}/batch` | 动态运行态 | 动态动作目录、记录动作可用性和动作执行入口；按 `moduleAlias + actionCode` 进入权限判断。 |
| `/platform.menu/{menuId}/entry` | 页面交付 | 菜单节点页面 bootstrap；会返回权限裁剪后的 descriptor 和页面配置，但不是菜单剪枝接口。 |
| `/workflow/runtime/**`、`/workflow/runtime/admin/**` | 工作流与任务 | 工作流实例、任务、提交审批和管理动作入口；复用当前用户上下文，接口归工作流专题。 |
