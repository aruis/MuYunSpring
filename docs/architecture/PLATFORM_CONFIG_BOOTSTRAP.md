# 平台配置与元数据自举

## 目标

M2 的目标是让平台先能管理自身配置态：应用、模块、元数据、菜单和数据字典。它不是完整管理后台，也不展开用户、组织、角色、工作流等后续业务。

这一阶段要证明：

1. 平台配置业务本身可以复用 M1/M1.5 的能力体系。
2. 动态模块不再只靠代码或测试构造元数据，而能从持久化配置发布到运行态。
3. 模块、元数据、菜单、字典使用统一命名和关系边界，避免后续业务接入时重新发明规则。

## 标识规则

| 对象 | 字段 | 规则 |
| --- | --- | --- |
| 应用 | `applicationAlias` | 全局唯一，创建后不允许手动修改 |
| 模块 | `moduleAlias` | 全局唯一，必须满足 `applicationAlias.xxx`，创建后不允许手动修改 |
| 元数据 | `metadataAlias` | 在应用下唯一，不能作为物理表身份 |
| 菜单方案 | `menuSchemeAlias` | 在同一租户/scope 内唯一；菜单节点不设计 alias/code |
| 数据字典类目 | `dictionaryCategoryAlias` | 在应用下唯一；字段绑定字典类目 |
| 数据字典项目 | `dictionaryItemCode` | 在字典类目下唯一；业务数据默认存项目 code |

`Module.id` 可以等于 `moduleAlias`，但业务字段、参数、DTO 和关系表列统一使用 `moduleAlias` / `module_alias`，不使用 `moduleId` / `module_id` 表达模块身份。

`applicationAlias` 使用单段小写标识；`moduleAlias` 使用至少两段点分小写标识，并与动态运行态模块别名校验保持一致。

元数据 `id` 是平台稳定 ID，由平台生成；物理表定位至少包含 `schemaName + tableName`。表名是存储属性，不作为元数据身份。默认场景下 `schemaName` 和 `tableName` 可由平台生成，发布建表后不允许作为普通配置随意修改。

## 核心模型

### Application

应用是模块、元数据和数据字典类目的顶层归属。菜单不归属应用。应用不是树，但需要支持排序，便于管理端按业务顺序展示。

建议字段：

| 字段 | 含义 |
| --- | --- |
| `id` | 与 `applicationAlias` 保持一致或由模型适配为同一值 |
| `alias` | 应用别名，对外参数使用 `applicationAlias` |
| `title` | 应用名称 |
| `enabled` | 是否启用 |
| `sortOrder` | 排序 |

### Module

模块是运行入口、菜单挂载、动作权限和动态运行时定位的业务边界。

建议字段：

| 字段 | 含义 |
| --- | --- |
| `id` | 与 `moduleAlias` 保持一致 |
| `applicationAlias` | 所属应用 |
| `alias` | 完整模块别名，对外参数使用 `moduleAlias` |
| `title` | 模块名称 |
| `parentId` | 父模块；根模块使用平台树能力的根节点值 |
| `moduleKind` | `STATIC` / `DYNAMIC` |
| `enabled` | 是否启用 |
| `sortOrder` | 排序 |

模块 alias 创建后不允许手动修改。模块按“应用 -> 多级模块”组织成树，排序范围限定在同一应用和同一父模块下。`moduleKind` 表达当前模块接入方式：静态模块可以没有元数据；动态模块必须有一个主元数据。

### Metadata

元数据是应用内业务实体定义，不等同于模块，也不等同于物理表。

建议字段：

| 字段 | 含义 |
| --- | --- |
| `id` | 平台稳定 ID，自动生成 |
| `applicationAlias` | 所属应用 |
| `alias` | 元数据别名，对外参数使用 `metadataAlias` |
| `title` | 元数据名称 |
| `schemaName` | 物理 schema，可默认 |
| `tableName` | 物理表名，可默认生成 |
| `enabled` | 是否启用 |

`metadataAlias` 在同一应用下唯一。`schemaName + tableName` 是物理表定位，不参与元数据语义身份。

元数据字段独立维护，最小字段包括 `metadataId`、`fieldName`、`columnName`、`fieldType`、必填、唯一、索引、标题字段、排序字段、长度和精度。`fieldName` 对齐动态运行态字段名规则，`columnName` 对齐 SQL 标识符规则；同一元数据内字段名、列名、标题字段和排序字段都必须保持唯一。

### ModuleMetadataRelation

模块与元数据通过关系绑定。关系表达“某个模块如何使用某个元数据”，而不是把元数据强绑定到单一模块。

建议字段：

| 字段 | 含义 |
| --- | --- |
| `moduleAlias` | 当前运行模块 |
| `metadataId` | 被使用的元数据 |
| `relationRole` | `MAIN` / `CHILD` |
| `parentMetadataId` | 父级元数据，可为空 |
| `foreignKey` | 子表指向父表的字段 |
| `relationAlias` | 关系别名，在模块下唯一 |
| `autoPopulate` | 读取主记录时是否装配子记录 |
| `cascadeDelete` | 删除父记录时是否联动删除子记录 |
| `sortOrder` | 关系排序 |

约束：

1. 一个模块最多一个 `MAIN` 关系。
2. 静态模块可以没有元数据关系。
3. 动态模块必须有一个 `MAIN` 关系。
4. 关系模型允许父、子、孙结构；非 `MAIN` 关系的 `parentMetadataId` 必须已经作为同一模块的关系存在，不能只引用孤立元数据。
5. A 应用或模块下定义的元数据在 B 模块体现时，对外运行口径统一是 B 的 `moduleAlias`。

横向关联、引用选择、关联视图等“关联元数据”场景不是 `relationRole`，后续应作为引用/视图/动作等独立能力表达，避免和主子结构混用。

## 跨模块体现

当一个模块使用另一个模块或应用内已有元数据时：

1. 菜单、入口、动作、查询、权限、运行时 URL 和审计口径都使用当前模块的 `moduleAlias`。
2. 元数据结构、物理表和字段定义来自被绑定的 `metadataId`。
3. 对外 API 不暴露“当前记录其实来自另一个模块”的额外切换语义。

这条规则可以降低运行态心智负担：模块负责呈现和运行入口，元数据负责结构和存储。

## 菜单

菜单是导航入口体系，不归属应用。应用仍然只是模块的归类属性；菜单节点可挂载模块，但不理解应用。

菜单按方案组织：

1. `MenuScheme`：菜单方案，支持系统、租户和机构三个 scope。M2 实现系统/租户口径，机构方案先保留模型边界，等机构体系进入后再启用业务闭环。
2. `Menu`：某个方案下的菜单节点。一个菜单只属于一个方案；同一个模块如需出现在多个方案或多个位置，创建多个菜单节点分别挂同一个 `moduleAlias`。

系统方案只在系统态维护和读取，不作为租户菜单 fallback 模板。`MenuScheme.alias/scope/tenantId` 创建后不可变，`Menu.schemeId` 创建后不可变；如需跨方案调整，应新建菜单节点或后续提供明确的整树迁移能力。

`MenuScheme` 建议字段：

| 字段 | 含义 |
| --- | --- |
| `tenantId` | 租户 ID；系统方案为空 |
| `alias` | 方案别名，在同一 scope 内唯一 |
| `scopeType` | `SYSTEM` / `TENANT` / `ORGANIZATION` |
| `scopeId` | scope 标识；租户方案默认等于 `tenantId`，系统方案固定为 `system` |
| `title` | 方案名称 |
| `enabled` | 是否启用 |
| `sortOrder` | 排序 |

`Menu` 建议字段：

| 字段 | 含义 |
| --- | --- |
| `tenantId` | 从所属方案继承，用于租户隔离 |
| `schemeId` | 所属菜单方案 |
| `parentId` | 父菜单；根菜单使用平台树根节点值 |
| `title` | 菜单名称 |
| `menuType` | `GROUP` / `MODULE` / `ROUTE` / `LINK` |
| `moduleAlias` | 模块菜单挂载的模块 |
| `route` | 路由菜单的路由 |
| `externalUrl` | 外链菜单的 URL |
| `enabled` | 是否启用 |
| `sortOrder` | 排序 |

M2 只要求菜单方案、菜单树、排序、启停和模块挂载。权限以后只对已解析的菜单树做剪枝，不进入当前菜单模型。

## 数据字典

数据字典为元数据字段、表单显示、查询条件、导入导出和规则判断提供稳定枚举来源。

数据字典拆成：

1. `DictionaryCategory`：字典类目，应用内唯一 `dictionaryCategoryAlias`，支持树。类目可作为目录，也可作为可绑定字典。
2. `DictionaryItem`：字典项目，归属具体字典类目，支持树、排序、启停和标题。

业务字段绑定字典类目，不绑定单个字典项目。业务数据默认存字典项目 `code`，不存内部项目 ID；展示和校验时通过 `applicationAlias + dictionaryCategoryAlias + code` 解析字典项目。同一类目内项目 `code` 必须唯一，即使项目本身是树，也不能只按同父级唯一，否则业务数据只存 code 时会产生解析歧义。

M2 只建设字典类目和字典项目的基础维护能力；字段如何引用字典进入元数据字段行为配置，不塞进最小元数据字段模型。

## M2 验收闭环

M2 至少需要跑通一条配置到运行时闭环：

```text
Application
  -> Module
  -> Metadata + Field
  -> ModuleMetadataRelation
  -> Publish
  -> DynamicRecordService.entity(moduleAlias, metadataAlias)
  -> CRUD / query
```

验收重点不是 UI 完整度，而是模型边界、发布边界和运行口径稳定。
