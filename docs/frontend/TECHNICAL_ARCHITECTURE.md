# 前端技术架构

本文记录 MuYunSpring 前端的阶段性架构方向。它强调设计哲学、团队协作方式和可演进边界，不把尚未进入建设阶段的平台业务细节提前定死。

## 设计哲学

MuYun 前端不是单个业务后台，也不是 Ant Design Vue 的简单二次封装。它应逐步形成一组可被平台项目和业务项目共同消费的前端平台能力。

核心原则：

1. **平台语义优先**：业务页面优先使用 MuYun 组件、MuYun client 和 MuYun runtime，不直接依赖底层 UI 库或裸 HTTP URL。
2. **动静共用**：静态业务页面和动态页面运行器共享组件、字段语义、动作语义、错误处理和请求上下文。
3. **能力可上升**：业务项目中反复出现、语义稳定、跨项目可复用的能力，应上升为平台包；一次性业务特殊组件留在业务项目内。
4. **上游可闭源，下游可顺畅开发**：平台团队维护源码，业务团队通过私有 npm 包、类型声明、文档、示例和 playground 使用平台成果。
5. **边界服务于演进**：目录和包名先表达职责方向，不把未来接口细节一次性锁死。

## 技术路线

首期采用：

```text
Vue 3 + TypeScript + Vite
Ant Design Vue 作为首个 UI adapter
Vue Router
Pinia
TanStack Query for Vue
```

选择 Vue 的原因是中后台页面、复杂表单、业务组件组合和 descriptor 驱动页面的代码更贴近业务直觉；Ant Design Vue 只作为首个 adapter，不作为业务项目的直接开发语义。

## 分层骨架

当前正式前端骨架位于：

```text
muyun-web/
```

现阶段先使用单项目内分层，未来根据发布和协作需要拆为私有 npm 包：

```text
muyun-web/src/web-contracts       前端公共契约和平台语义类型
muyun-web/src/web-core            无 UI 平台能力，如 HTTP、错误、消息、上下文、Query 治理
muyun-web/src/vue-ui-antdv        Vue + Ant Design Vue UI adapter
muyun-web/src/dynamic-page-runtime        动态页面运行器
muyun-web/src/platform-components    跨业务可复用的平台业务组件
muyun-web/src/platform-shell      平台控制台壳
muyun-web/src/app                 当前应用装配、路由和 provider
muyun-web/src/views               当前应用页面入口
```

未来包名按前端职责命名，避免复用后端 Gradle 子项目名：

```text
@muyun/web-contracts
@muyun/web-core
@muyun/vue-ui-antdv
@muyun/dynamic-page-runtime
@muyun/platform-components
@muyun/platform-shell
```

## 能力分层

### web-contracts

承载前端公共类型和平台语义词汇。这里可以表达 `moduleAlias`、`metadataAlias`、`actionCode`、字段、动作、动态页面描述、保存 envelope 等概念，但具体字段会随着后端平台接口进入建设后再收口。

### web-core

承载无 UI 的平台能力，例如：

```text
HTTP client
请求上下文
异常归一
消息机制
当前用户和租户上下文
权限判断
Query client 默认配置
traceId 和前端异常追踪
```

这层应独立于 Vue UI adapter，避免业务项目重复写 axios 封装、错误提示、token 处理和缓存失效。

### vue-ui-antdv

承载 MuYun UI 组件到 Ant Design Vue 的适配。业务项目使用 `MuyunInput`、`MuyunSelect`、`MuyunForm`、`MuyunTable` 等平台组件，不直接使用 Ant Design Vue 组件。

基础组件可以保持轻量，但不能把 Ant Design Vue 的完整 props 原样暴露成 MuYun 公共 API。

### dynamic-page-runtime

承载动态页面运行器。它负责把后端 descriptor、动作、字段、列表、表单和页面上下文转换成 MuYun 组件组合。

dynamic-page-runtime 不直接依赖 Ant Design Vue；它依赖 MuYun 契约、web-core 能力和 MuYun 组件。

### platform-components

承载跨业务复用的平台业务组件。这里不是所有业务组件的收容所，只接纳语义稳定、跨项目复用、依赖平台标准能力的组件，例如引用选择、附件面板、导入导出、审批动作区、查询表格和子表。

项目私有组件仍留在业务项目内。

### platform-shell

承载平台控制台壳，包括布局、菜单、登录态入口、租户切换入口、全局错误出口和路由框架。它面向平台应用和业务应用提供统一承载形态。

## 业务项目接入示例

当前仓库内提供业务项目接入示例：

```text
muyun-web/examples/business-web
```

它模拟未来业务团队在独立仓库中通过私有 npm 包消费平台成果。当前为了本仓库构建验证，示例使用 Vite alias 指向本地源码；真实拆包后应替换为私有 npm registry 中的 `@muyun/*` 包。

业务项目应优先依赖：

```text
@muyun/web-contracts
@muyun/web-core
@muyun/vue-ui-antdv
@muyun/dynamic-page-runtime
@muyun/platform-components
@muyun/platform-shell
```

## 协作模式

平台团队负责：

```text
平台契约
无 UI 基础能力
UI adapter
动态页面运行器
跨业务平台组件
平台 shell
文档、示例、playground 和迁移说明
```

业务团队负责：

```text
业务页面
业务专属组件
业务路由组合
业务项目配置
对平台缺口的反馈和上升申请
```

上升为平台能力的判断标准：

1. 是否跨多个业务项目复用。
2. 是否依赖平台标准能力。
3. 是否能降低业务项目接入成本。
4. 是否有稳定语义和维护责任人。
5. 是否适合通过文档和类型声明给下游使用。

## 前后端语义

前端不复制后端模块边界，但共享平台业务语义。涉及双方共有的概念时，应优先沿用后端已经稳定的命名，例如：

```text
applicationAlias
moduleAlias
metadataAlias
relationAlias
actionCode
fieldName
record
values
children
attachments
originContext
uiConfigId
```

这些语义会在具体平台业务接口建设时进一步固化到 `web-contracts` 和 `web-core` 中。

## 边界约束

当前骨架保留一个最小工程约束：除 `src/vue-ui-antdv` 外，不允许源码直接 import `ant-design-vue`。

```bash
cd muyun-web
npm run check:boundaries
```

后续正式拆包时，该约束应升级为 ESLint/CI 规则。

当前同时提供轻量 ESLint 与 Prettier 配置，用于保障基础代码质量和格式一致。规则保持克制，后续只在确有协作收益时逐步收紧。

## 当前非目标

首期不建设：

1. 完整低代码设计器。
2. 多 UI 库并行 adapter。
3. 可视化流程设计器。
4. 完整插件市场。
5. 真实平台业务接口闭环。

当前重点是先固定技术路线、分层骨架、团队协作模型和可演进方向。

## 验证命令

```bash
cd muyun-web
npm install
npm run lint
npm run format:check
npm run build
npm run check:boundaries
npm run build:business-example
```

后端仍使用仓库默认验证：

```bash
./gradlew test
```
