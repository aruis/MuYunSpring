# 命名与边界

## 三类模块名称

| 名称 | 含义 | 示例 | 规则 |
| --- | --- | --- | --- |
| Gradle 子项目 | 构建和依赖边界 | `muyun-ability` | 只有存在真实代码、测试和稳定依赖边界时才创建 |
| Java 包 | 源码命名空间 | `net.ximatai.muyun.spring.ability` | 按代码职责组织 |
| 平台模块别名 | 运行时业务边界 | `iam.organization` | 用于权限、审计、菜单、OpenAPI 等运行时语义 |

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
<namespace>.<name>
```

建议命名空间：

| 命名空间 | 范围 |
| --- | --- |
| `iam.*` | 用户、组织、部门、角色、身份等 |
| `nav.*` | 菜单、导航 |
| `platform.*` | 元数据、模块、动作、字典、附件、审计等 |
| `workflow.*` | 工作流配置和运行时 |
| `rule.*` | 编码、生成、回写等规则 |
| `exchange.*` | 导入导出 |

别名是稳定运行时标识，不是 URL，也不是 Java 包名。`admin.*` 不作为平台模块命名空间；管理端可以管理多个命名空间，但不拥有它们的业务语义。

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
