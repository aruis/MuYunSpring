# MuYunSpring

MuYunSpring 是一个基于 Java 21 和 Spring Boot 的企业应用平台底座，核心目标是让静态 Java 业务模型和动态元数据模型复用同一套平台能力。

平台不是简单的动态 CRUD，也不是把低代码能力和 Java 业务能力拆成两套系统。MuYunSpring 以“动静一体”为主线，把数据访问、生命周期、租户、权限、审计、字段治理、页面交付和配置治理组织为可复用能力，让业务模块通过声明能力组合完成接入，减少重复 service 代码和模块专用接口。

## 平台定位

- 面向企业内部系统、业务中台和可配置业务应用的后端平台。
- 支持静态 Java 模块，也支持由应用、模块、元数据、字段、页面和动作配置驱动的动态模块。
- 平台能力优先形成标准模型、Ability、运行态门面和 Web 暴露面，而不是散落在具体业务代码中。
- 低代码页面交付复用动态运行态、权限、数据范围、动作、附件、导入导出、生成回写和配置治理链路。

## 核心优势

| 能力方向 | 说明 |
| --- | --- |
| 动静一体 | 静态模型和动态元数据进入同一套数据访问、生命周期和能力语义，避免平行实现。 |
| 能力组合 | CRUD、软删、树、排序、引用、父子聚合、缓存、启停、租户等能力以稳定接口和契约复用。 |
| 配置自举 | 平台可以管理自身应用、模块、元数据、字段、字典和菜单，并发布到动态运行态。 |
| 页面交付 | 动态模块可通过 UI 配置、查询模板、菜单入口、bootstrap、表单保存和页面交互完成低代码交付。 |
| 业务自动化 | 编码规则、导入导出、生单、回写、来源关系和贡献台账复用平台动作与动态记录链路。 |
| 治理闭环 | 配置包、健康检查、版本发布、回滚、迁移 dry-run、导入草稿和模板复用支撑平台化交付。 |

## 模块结构

```text
muyun-common    通用基础设施、异常、上下文和公共工具
muyun-ability   平台能力接口、默认实现、生命周期和数据访问契约
muyun-dynamic   动态元数据、动态记录运行态和动态模块发布
muyun-platform  平台配置、页面交付、自动化、工作流和治理能力
muyun-iam       租户、组织、用户、角色、权限和身份上下文
muyun-boot      Spring Boot 启动与装配入口
```

## 运行验证

```bash
./gradlew test
```

## 文档入口

- [开发原则](docs/DEVELOPMENT_PRINCIPLES.md)：项目长期取舍、推进方式和测试策略。
- [动静一体核心设计](docs/architecture/DYNAMIC_STATIC_UNIFIED_CORE.md)：静态模块与动态模块如何共享平台底座。
- [平台能力清单](docs/architecture/ABILITY_CATALOG.md)：业务接入时优先复用的能力索引。
- [命名与边界](docs/architecture/NAMING_AND_BOUNDARIES.md)：Gradle 子项目、Java 包、平台模块别名和动态边界。
- [平台配置与元数据自举](docs/architecture/PLATFORM_CONFIG_BOOTSTRAP.md)：应用、模块、元数据、字段、菜单和字典的配置闭环。
- [平台业务专题](docs/platform/README.md)：按业务专题整理的平台能力和 Web 接口交接入口。
- [技术债记录](docs/TECHNICAL_DEBT.md)：已确认但暂缓处理的平台级技术债。
- [Agent Guide](AGENTS.md)：面向协作 Agent 的仓库工作规则。
