# 命名与边界

## 三类模块名称

| 名称 | 含义 | 示例 | 规则 |
| --- | --- | --- | --- |
| Gradle 子项目 | 构建和依赖边界 | `muyun-ability` | 只有存在真实代码、测试和稳定依赖边界时才创建 |
| Java 包 | 源码命名空间 | `net.ximatai.muyun.spring.ability` | 按代码职责组织 |
| 平台模块别名 | 运行时业务边界 | `platform.metadata` | 用于权限、审计、菜单、OpenAPI 等运行时语义 |

三者不能混用。一个 Gradle 子项目可以包含多个平台模块。

## Java 包根

统一使用：

```text
net.ximatai.muyun.spring
```

当前包和模块保持克制，不提前创建空目录或空子项目。新增边界必须能减少真实复杂度。

## 平台模块别名

平台模块别名格式：

```text
<applicationAlias>.<moduleName>
```

应用、模块、元数据和配置对象统一使用 alias 语义：

| 对象 | 语义字段 | 参数名 | 唯一范围 |
| --- | --- | --- | --- |
| 应用 | `alias` | `applicationAlias` | 全局 |
| 模块 | `alias` | `moduleAlias` | 全局，且必须以 `applicationAlias.` 开头 |
| 元数据 | `alias` | `metadataAlias` | 应用内 |
| 菜单 | `alias` | `menuAlias` | 应用内 |
| 数据字典 | `alias` | `dictionaryAlias` | 应用内 |

别名是稳定运行时标识，不是 URL，也不是 Java 包名。模块身份在业务字段、参数、DTO 和关系表列中统一叫 `moduleAlias` / `module_alias`，不使用 `moduleId` / `module_id` 表达同一件事。即使 `Module.id` 与 `Module.alias` 使用相同值，模块下属业务仍按 `moduleAlias` 命名。

元数据身份不等于物理表名。`Metadata.id` 是平台生成的稳定 ID；`metadataAlias` 是应用内业务别名；`schemaName + tableName` 才是物理表定位。

## 模型命名

`Model` 用于持久化领域对象契约：

```text
EntityContract
TreeCapable
SortCapable
TitledCapable
```

`Definition` 用于配置定义：

```text
ModuleDefinition
EntityDefinition
FieldDefinition
ActionDefinition
```

`Runtime` 或 `Instance` 用于运行态记录：

```text
WorkflowInstance
WorkflowTask
WriteBackExecution
```

## 动态边界

`dynamic` 表示元数据驱动的运行时执行。它不表示任意脚本、不表示插件系统，也不允许绕过 Java 服务、权限、审计、校验和建表治理。
