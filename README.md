# MuYunSpring

[![CI](https://github.com/aruis/MuYunSpring/actions/workflows/ci.yml/badge.svg)](https://github.com/aruis/MuYunSpring/actions/workflows/ci.yml)

MuYunSpring 是一个基于 Java 21、Spring Boot 和 Vue 3 的企业应用平台底座。项目的核心路线是“动静一体”：静态 Java 业务模型和动态元数据模型复用同一套平台能力、数据访问、生命周期、权限、审计和页面交付链路。

它不是单纯的动态 CRUD，也不是把低代码平台和 Java 业务开发拆成两套系统。MuYunSpring 希望让稳定业务模块保持直观的 Java 接入体验，同时让可配置业务对象通过元数据获得相同的平台能力。

## 项目状态

MuYunSpring 目前处于平台底座快速演进阶段，重点是沉淀“动静一体”的后端能力契约、动态运行态、平台配置治理、身份权限、页面交付接口和前端工作台骨架。当前更适合作为企业应用平台底座、架构参考或二次开发起点，不是面向终端用户的完整业务套件。

当前成熟度大致如下：

| 范围 | 状态 |
| --- | --- |
| 后端 Ability、静态模型、动态记录运行态、schema 初始化 | 已形成主要契约，仍随平台边界演进 |
| 平台配置、IAM、页面交付接口、业务自动化、工作流任务、治理能力 | 已有阶段能力和测试支撑，具体边界以 `docs/` 和测试为准 |
| 前端 workbench、登录、菜单、页签、UI adapter、mock/后端模式 | 可运行的骨架和接入示例 |
| 前端真实动态业务页面闭环、低代码设计器、多 UI adapter、插件市场 | 当前非目标或后续触发建设 |

适合关注本项目的场景：

- 企业内部系统、业务中台和可配置业务应用的平台底座。
- 希望静态 Java 业务代码和动态配置对象共享同一套能力、权限、审计、租户和生命周期语义。
- 需要把编码规则、导入导出、页面交付、工作流任务等能力沉淀为可复用平台能力。
- 需要低代码配置能力，但不希望业务逻辑绕过服务层、权限和治理链路。

不适合的场景：

- 需要立即交付给业务用户使用的完整低代码产品。
- 需要完整可视化表单设计器、流程设计器、插件市场或多 UI adapter。
- 只需要一个轻量 CRUD generator，不需要平台治理、租户、审计和动静一体边界。

## 核心能力

| 方向 | 当前范围 | 阶段状态 |
| --- | --- | --- |
| 动静一体 | 静态模型和动态元数据共享 CRUD、软删、树、排序、引用、缓存、生命周期和租户作用域语义。 | 核心契约 |
| 动态运行态 | 支持应用、模块、元数据、字段、关系和动态记录运行态刷新，动态记录复用平台 Ability 链路。 | 核心契约 |
| 平台配置 | 应用、模块、元数据、字段、字典、菜单和 UI 配置可由平台自身管理并自举。 | 阶段能力 |
| 身份权限 | 租户、组织、部门、岗位、员工、用户、角色、动作授权、菜单可见性和当前用户上下文。 | 阶段能力 |
| 页面交付 | 菜单入口、页面 bootstrap、列表查询、表单保存、附件、查重、引用候选和页面偏好接口。 | 阶段契约 |
| 业务自动化 | 编码规则、导入导出、生单、回写、来源关系、贡献台账和执行诊断。 | 阶段能力 |
| 工作流任务 | 流程定义、版本、实例、任务、审批、历史、委托、待办工作台和插件挂点。 | 阶段能力 |
| 治理闭环 | 配置包、健康检查、版本归档、指针切换、迁移 dry-run、导入草稿和模板复用。 | 阶段能力 |
| 前端工作台 | Vue 3 + TypeScript + Vite，包含登录、菜单、页签、平台 UI adapter、mock/后端模式和业务接入示例。 | 可运行骨架 |

## 仓库结构

```text
muyun-common      通用基础设施、异常、上下文、租户和公共工具
muyun-ability     平台能力接口、默认实现、生命周期和数据访问契约
muyun-dynamic     动态元数据、动态记录运行态和动态运行态刷新
muyun-platform    平台配置、页面交付、自动化、工作流和治理能力
muyun-iam         租户、组织、用户、角色、权限和身份上下文
muyun-boot        Spring Boot 启动与装配入口
muyun-web         Vue 前端工作台、平台 UI adapter、动态页面骨架和业务示例
docs              架构原则、平台专题、前端路线和技术债记录
```

## 技术栈

后端：

- Java 21
- Spring Boot
- Gradle Kotlin DSL
- PostgreSQL
- JUnit 5 / Testcontainers

前端：

- Vue 3
- TypeScript
- Vite
- Ant Design Vue 作为首个 UI adapter
- Vue Router / Pinia / TanStack Query for Vue

## 快速开始

本地开发需要 Java 21、Docker Compose v2、Node.js LTS 和 npm。后端默认连接 PostgreSQL，仓库不内置嵌入式数据库。

| 服务 | 默认地址 |
| --- | --- |
| PostgreSQL | `127.0.0.1:54321` |
| 后端 API | `http://127.0.0.1:8080` |
| 前端工作台 | `http://127.0.0.1:5173/` |

> 安全说明：快速开始配置仅用于本地开发。不要在公网或共享环境使用默认数据库密码和 `admin/admin123`。生产或共享环境应显式设置 `muyun.initial-admin.initial-password`，并按 `production` 运行模式治理 schema migration。

一键启动本地开发栈：

```bash
./scripts/dev-local.sh
```

该脚本会启动 PostgreSQL，并并行启动后端和前端；默认启用演示初始化，便于本地验证租户、机构和角色基础数据。按 `Ctrl-C` 会停止脚本拉起的后端和前端进程，PostgreSQL 容器会继续保留。需要关闭演示初始化时：

```bash
MUYUN_DEMO_BOOTSTRAP_ENABLED=false ./scripts/dev-local.sh
```

也可以按下面步骤分开启动。

1. 启动 PostgreSQL：

```bash
docker compose up -d
```

Compose 会启动 `postgres:18.4-alpine`，本机端口为 `54321`，数据库名为 `muyun_spring`。

不使用 Compose 时，可以手动启动等价容器：

```bash
docker run --name muyun-spring-postgres \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=muyun_dev \
  -e POSTGRES_DB=muyun_spring \
  -p 54321:5432 \
  -v muyun-spring-postgres-data:/var/lib/postgresql \
  -d postgres:18.4-alpine
```

2. 启动后端：

```bash
./gradlew :muyun-boot:bootRun --args='--muyun.runtime.mode=development --spring.datasource.url=jdbc:postgresql://127.0.0.1:54321/muyun_spring --spring.datasource.username=postgres --spring.datasource.password=muyun_dev'
```

后端默认监听 `http://127.0.0.1:8080`。开发态会按当前 schema 策略初始化或拉齐平台表结构。

3. 启动前端：

```bash
cd muyun-web
npm ci
npm run dev:backend
```

前端默认监听 `http://127.0.0.1:5173/`。`dev:backend` 会读取 `muyun-web/.env.backend`，连接本地后端。

裸库首次启动会创建平台超级管理员。默认登录信息：

```text
用户名：admin
密码：admin123
租户：不填
```

初始密码可通过 `muyun.initial-admin.initial-password` 调整。平台超级管理员是系统用户，不归属默认租户。

本地演示环境如需同时创建一组租户业务治理数据，可显式启用演示初始化：

```properties
muyun.demo-bootstrap.enabled=true
muyun.demo-bootstrap.tenant-title=演示租户
muyun.demo-bootstrap.organization-title=戏码台
muyun.demo-bootstrap.department-title=综合管理部
muyun.demo-bootstrap.employee-title=演示租户管理员
muyun.demo-bootstrap.admin-username=demo_admin
muyun.demo-bootstrap.admin-initial-password=demo123
```

该能力默认关闭，只用于裸库开发和演示启动；生产环境不应把演示租户、组织、部门、职员、租户管理员账号和租户管理员账号角色授权视为平台 baseline 数据。演示租户管理员是普通租户用户，默认授予内置 `tenant.admin` 账号角色，默认登录时租户填写 `demo`，不要和系统超级管理员 `admin` 混用。

租户创建成功后，平台会自动准备租户级默认菜单方案和独立菜单数据；当前实现采用每租户复制菜单树，后续会收敛为系统菜单模板加租户覆盖差异。

首次登录后可进入平台工作台，查看菜单、平台配置、IAM 等静态管理入口。动态页面闭环仍以文档和测试契约为准。

## 运行验证

后端默认验证：

```bash
./gradlew test
```

后端集成测试单独运行：

```bash
./gradlew integrationTest
```

前端验证：

```bash
cd muyun-web
npm run check
```

只改文档时可以不运行测试，但提交说明应写明原因。涉及 Java 代码、构建配置或平台契约时，默认运行 `./gradlew test`。

## 文档导航

- [开发原则](docs/DEVELOPMENT_PRINCIPLES.md)：长期路线、推进方式和测试策略。
- [动静一体核心设计](docs/architecture/DYNAMIC_STATIC_UNIFIED_CORE.md)：静态模块与动态模块如何共享平台底座。
- [命名与边界](docs/architecture/NAMING_AND_BOUNDARIES.md)：Gradle 子项目、Java 包、平台模块别名和动态边界。
- [平台文档入口](docs/platform/README.md)：按业务专题整理的平台能力和 Web 接口交接入口。
- [前端技术架构](docs/frontend/TECHNICAL_ARCHITECTURE.md)：Vue 前端技术路线、组件契约、运行器边界和协作方式。
- [技术债记录](docs/TECHNICAL_DEBT.md)：已确认但暂缓处理的平台级问题。

更多专题能力和 Web 接口说明从 [平台文档入口](docs/platform/README.md) 继续阅读。

## 贡献

欢迎通过 Issue 和 Pull Request 参与。开始前请阅读 [CONTRIBUTING.md](CONTRIBUTING.md)，并尽量让改动沿用现有模块边界、命名和测试风格。

常规贡献建议：

- Issue 适合提交可复现 bug、文档缺口、平台边界问题和具体能力建议。
- 路线级调整、模块拆分、新框架或大范围公共契约变化，建议先通过 Issue 讨论。
- 从 `main` 拉出短生命周期分支。
- 一个 PR 聚焦一个完整能力、业务目标或修复目标。
- 对外可见行为、平台契约和边界修正应补测试或说明验证方式。
- PR 描述包含变更内容、验证结果和剩余风险。
- 当前仓库尚未建立专门的安全披露流程；请不要在公开 Issue 中直接贴出可利用漏洞细节。

## 许可证

MuYunSpring 使用 [Apache License 2.0](LICENSE)。
