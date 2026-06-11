# 动态运行态专题

本文梳理动态记录运行态的主要入口和链路。详细路线仍以 `README.md`、`docs/DEVELOPMENT_PRINCIPLES.md`、`docs/architecture/DYNAMIC_STATIC_UNIFIED_CORE.md` 和 `docs/architecture/NAMING_AND_BOUNDARIES.md` 为准。

## 能力定位

动态运行态负责把已发布的动态模块元数据变成可执行的记录服务。它不是另一套低代码内核，也不是任意脚本或插件系统；动态记录仍要进入平台统一的数据访问、能力语义、生命周期、权限、审计、租户和事务边界。

当前专题覆盖：

1. 动态记录 describe/descriptor 输出。
2. 动态主元数据 CRUD、query、view、tree、sort、enable/disable。
3. 动态引用解析基础运行线索。
4. 动态动作目录、动作可用性和动作执行基础。
5. 动态 OpenAPI 基础输出。

导入导出、附件、页面 bootstrap、模块任务、工作流、编码规则、生成回写等能力可能复用动态运行态入口，但归属各自专题，不在本文展开。

## 主要入口

代码入口：

| 入口 | 作用 |
| --- | --- |
| `DynamicRecordService` | 动态记录对外门面，按 `moduleAlias` 和 `entityAlias` 定位运行态服务，承接 describe、CRUD/query、引用、视图、关联视图和动作执行。 |
| `DynamicEntityOperations` | 单个动态实体的轻量操作门面，供调用方以实体维度执行记录操作。 |
| `DynamicRecordRuntime` / `DynamicModuleRegistry` | 保存已发布模块运行态快照，按模块别名提供模块定义和实体服务。 |
| `DynamicEntityService` | 单个动态实体的运行态服务，复用 CRUD、软删、树、排序、引用、缓存等平台能力。 |
| `DynamicRecordDao` | 动态表数据访问与 SQL 映射，不承接生命周期、权限或业务编排。 |
| `DynamicRecordWebController` | 动态模块 Web 入口，路径根为 `/{moduleAlias}`。 |
| `DynamicOpenApiGenerator` | 从动态 descriptor 生成基础 OpenAPI 文档模型。 |

运行时身份统一使用平台模块别名 `moduleAlias`，实体身份在运行态服务内部使用 `entityAlias`。Web 层面向前端主要暴露模块主元数据，不暴露 `entities/{entityAlias}` 路由体系。

## 核心运行链路

动态模块发布后，配置态会被编译成运行态模块定义，并进入动态运行时注册表：

```text
配置态模块/元数据/字段/关系/视图/动作
  -> 发布编译
  -> DynamicRecordRuntime
  -> DynamicRecordService
  -> DynamicEntityService
  -> DynamicRecordDao
  -> MuYunDatabase
```

`describe` 链路输出模块结构，包含模块别名、主实体别名、实体字段、能力、关系、引用、视图、关联视图和动作目录。调用方应通过 descriptor 理解当前模块可用能力，不应假设所有模块都具备树、排序、启停或引用。

CRUD/query 链路由 `DynamicRecordService` 进入实体服务，继续复用平台能力层。租户 scope、软删过滤、乐观锁、生命周期、缓存失效、数据权限和运行事件不应由 Web Controller 或业务调用方手写绕过。

引用链路由字段引用定义驱动。descriptor 先表达字段引用、目标模块/实体、标题和投影；运行态解析再按引用配置执行查询、标题翻译、投影输出和上下文校验。

动作链路先通过 descriptor 暴露动作目录，再按动作级别进入列表、记录或批量执行入口。标准动作复用现有平台能力；自定义动作通过 executor 路由，并在执行上下文中保留模块、实体、动作、记录、trace、租户和授权语义。

OpenAPI 链路以 descriptor 为输入，输出基础文档模型，用于联调和前端理解当前模块接口。它是动态模块当前运行态的描述，不替代配置治理，也不等同完整手写 OpenAPI 规范。

## 边界说明

1. 动态运行态只执行已发布配置，不负责配置态编辑、版本发布、回滚和迁移治理。
2. 动态 Web 根路径采用 `/{moduleAlias}`，为同 alias 的静态 Controller 接管预留路径一致性。
3. 主元数据是 Web 入口的默认记录对象；多实体、子实体和关联实体通过运行态服务、关系、引用和关联视图表达。
4. `actions` 路径只用于动作查询和可用性查询，不用于动作执行。
5. 动作执行路径使用动作编码本身，动作编码需要避开平台保留路径。
6. descriptor/OpenAPI 只描述当前模块可见和可用的运行态能力；权限、数据范围和动作可用性会影响输出或执行结果。
7. 动态运行态快照是性能和一致性边界，不是绕过模型、权限、审计、校验和数据访问契约的第二套内核。

## 关联专题

| 专题 | 关系 |
| --- | --- |
| 配置与发布 | 提供应用、模块、元数据、字段、关系、视图和动作配置，并发布到动态运行态。 |
| 页面交付 | 消费 descriptor、视图配置、查询配置、关联视图和动态 Web API。 |
| 身份与权限 | 为动态 CRUD、查询、动作、菜单和 descriptor 输出提供授权与数据范围裁剪。 |
| 工作流与任务 | 通过动态记录、动作执行和运行上下文挂接审批、待办和任务能力。 |
| 业务编排 | 编码规则、导入导出、生成回写等专题复用动态记录门面和动作上下文。 |
