# MuYun 前端组件契约

本文记录 MuYun 前端组件治理原则。它用于防止业务项目把底层 UI 库 API 当成平台 API，也用于指导哪些组件应沉淀为平台能力。具体组件 props 会随着平台业务接口建设逐步收口。

静态管理页的 explorer、列表和树组件边界见 [静态管理页组件边界](STATIC_MANAGEMENT_PAGE_COMPONENTS.md)。

## 基本原则

1. MuYun 组件表达平台语义，不表达 Ant Design Vue 语义。
2. 静态页面和动态页面运行器优先复用同一批组件。
3. 组件暴露窄口径能力，第三方 UI 事件和值类型在 adapter 内归一。
4. 基础组件可以轻量，平台业务组件必须收口业务语义。
5. 业务项目中的稳定复用能力可以上升到平台包，但项目私有组件不强行平台化。

## 组件分类

### 基础 UI 组件

基础 UI 组件提供统一外观、交互和受控值语义，例如：

```text
UiInput
UiSelect
UiDatePicker
UiModal
UiTable
UiForm
```

这类组件可以比较薄，但不能退化成 Ant Design Vue props 的完整透传。

### 平台语义组件

平台语义组件承接 MuYun 平台能力，例如：

```text
DictionarySelect
ReferenceSelect
ActionBar
AttachmentPanel
UserSelect
OrganizationSelect
```

这类组件通常会组合 UI、web-core client、权限、错误态和平台上下文。它们应优先沉淀在 `platform-components` 或 `dynamic-page-runtime` 中，而不是散落到业务项目。

### 页面级运行组件

页面级运行组件负责把平台契约组合成可用页面能力，例如：

```text
DynamicForm
QueryTable
ChildTable
```

这些组件是动态页面运行器和静态业务页面之间的重要交汇点。它们应优先复用 `web-contracts` 和 `web-core`，并由平台团队维护。

## 业务组件上升标准

业务组件进入平台包前，应满足尽量多的条件：

1. 在多个业务项目或多个模块中重复出现。
2. 依赖平台标准语义，例如模块、动作、引用、字典、附件、权限或审计。
3. 能通过稳定 props、事件和文档交给下游团队使用。
4. 维护责任属于平台团队或有明确共同维护机制。
5. 抽象后能明显减少业务项目代码量和风格分叉。

不满足这些条件的组件，可以留在业务项目中。

## 与底层 UI 库的关系

业务项目不直接使用 Ant Design Vue。需要底层能力时，优先判断：

1. 是否已有 MuYun 组件可以表达。
2. 是否应该增强已有 MuYun 组件。
3. 是否应该新增平台组件。
4. 是否只是项目私有 UI，适合留在业务项目。

只有确认为项目私有且不影响平台风格的场景，才考虑在业务项目内局部处理，并应避免污染平台公共契约。

## 与 web-core 的关系

组件不应各自实现 HTTP、错误提示、消息机制、权限判断和缓存失效。涉及这些能力时，应优先通过 `web-core` 提供的 client、上下文和工具接入。

例如后续动态记录、引用候选、字典解析、动作执行等能力，应从平台 client 进入，而不是在组件里拼裸 URL。

## 当前阶段

当前骨架只提供最小组件集合，目的是固定方向和示例：

```text
UiInput
UiSelect
DictionarySelect
ReferenceSelect
UiForm
UiTable
ActionBar
```

这些组件不是最终形态。真实平台业务进入建设后，应根据后端能力和前端使用反馈继续调整契约。
