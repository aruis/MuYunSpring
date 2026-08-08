# Vitest 标准测试体系迁移

## 目标

MuYunSpring 前端统一使用 Vitest 作为测试框架，并将 jsdom 和 Vue Test Utils 纳入标准组件测试
基础设施。基础 TypeScript 逻辑、Vue 组件和前端集成测试使用同一套测试发现、断言、Mock 和
执行入口，不再维护 Node Test Runner 与 Vitest 双轨体系。

本次迁移改变测试运行器、断言方式和测试基础设施，不改变生产代码行为、前端公开 npm API，
也不否定现有纯逻辑测试的价值。

## 当前基线

当前 `muyun-web` 测试具有以下特征：

- 共有 50 个集中放置在 `muyun-web/tests/` 的 `.test.ts` 文件，约 435 个测试用例。
- 测试使用 `node:test` 和 `node:assert`，通过 Node TypeScript 类型擦除与自定义 loader 执行。
- `package.json` 手工枚举默认测试文件，只有 47 个文件进入默认测试命令。
- `businessRoutes.test.ts`、`staticFormActionFlow.test.ts` 和 `treeRecordModel.test.ts` 当前未进入
  默认 `npm test`。
- 现有测试主要覆盖状态模型、descriptor、导航、工作视图会话、数据转换和平台基础逻辑。
- 当前没有标准的 Vue SFC 挂载、DOM 交互或 jsdom 测试层。

现有测试资产应整体保留并完成等价迁移。迁移不以重写业务断言或扩大生产功能范围为目标。

## 已确认决策

1. Vitest 是唯一测试框架，迁移完成后不再使用 `node:test`、`node:assert` 或 Node Test Runner
   命令。
2. 所有基础 TypeScript 测试也使用 Vitest，不保留另一套纯逻辑测试框架。
3. 基础 TypeScript 测试默认运行在 Vitest 的 `node` environment；这里的 Node.js 是 Vitest
   运行时，不表示继续使用 Node Test Runner。
4. jsdom 作为标准测试环境集成，但只用于 Vue 组件和依赖 DOM 的前端测试，不作为全局默认环境。
5. Vue 组件测试统一使用 Vitest、jsdom 和 Vue Test Utils。
6. 不保留 Node Test 与 Vitest 双轨过渡状态；一次迁移必须覆盖全部现有测试和默认测试命令。
7. 首轮不安装覆盖率 provider，不生成覆盖率报告，也不设置覆盖率门禁。覆盖率作为后续独立决策。

## 相对现有方式的能力提升

引入 Vitest 的目的不是单纯替换测试语法，而是让前端测试直接进入 Vue 与 Vite 的标准工具链，
补齐现有方式无法自然覆盖的组件和浏览器语义。

| 方向            | 当前 Node Test 方式                        | Vitest 标准体系                                         |
| --------------- | ------------------------------------------ | ------------------------------------------------------- |
| TypeScript 执行 | 依赖 Node 类型擦除和自定义 loader          | 复用 Vite 转换与模块解析能力                            |
| Vue SFC         | 不能直接加载 `.vue`                        | 可直接测试 Vue 单文件组件                               |
| Vite alias      | 需要自定义 loader 适配                     | 复用 Vite alias 和插件配置                              |
| DOM 环境        | 没有标准 DOM 测试层                        | 按需使用 jsdom                                          |
| Vue 组件交互    | 当前无法挂载和操作组件                     | 通过 Vue Test Utils 测试 props、slots、emits 和输入交互 |
| Mock 能力       | 以手工 fake 和 Node Mock 为主              | 统一使用 `vi.fn`、`vi.mock`、`vi.spyOn` 和 timer        |
| 断言体验        | `node:assert` 偏底层                       | `expect` 提供更直观的语义和失败信息                     |
| 测试发现        | `package.json` 手工枚举，当前遗漏 3 个文件 | 按规则自动发现全部测试                                  |
| Watch           | 缺少与 Vite 模块图统一的开发体验           | 按依赖关系重跑受影响测试                                |
| 测试隔离        | 依赖当前执行脚本和 loader                  | 通过 Vitest Projects 区分基础测试与组件测试             |
| 前端扩展        | 继续增加 loader 和辅助脚本                 | 可继续扩展 Snapshot、Browser Mode 和测试 UI             |
| 团队一致性      | Node Test 写法与主流 Vue 测试生态不同      | 与 Vite、Vue Test Utils 和 Vue 社区工具链一致           |

现有状态模型、descriptor、导航和数据转换测试仍是平台质量保障的主体。Vitest 负责统一它们的
运行方式，同时补齐 Vue 组件、DOM、Vite 模块和前端集成测试能力；不应为了展示新框架能力而把
稳定的纯逻辑测试改写成组件测试。

## 成本与能力边界

- Vitest、jsdom 和 Vue Test Utils 会增加开发依赖、安装体积与配置维护成本。
- Vitest 仍以 Node.js 作为基础运行时；退出的是 Node Test Runner，而不是 Node.js 运行环境。
- jsdom 模拟常用浏览器 API，但不提供真实布局、绘制、完整焦点行为或跨浏览器差异。
- Drawer、Modal、焦点、iframe、尺寸布局和浏览器兼容性等真实行为，后续仍应使用 Vitest
  Browser Mode 或 Playwright 验证。
- Snapshot 只作为可读性良好且变更意图明确的补充手段，不替代业务行为断言。
- 本阶段不建设覆盖率、端到端测试或真实浏览器测试体系。

## 测试目录

测试继续集中在 `muyun-web/tests/`，不移动到生产源码旁。测试子目录严格镜像主要被测对象的
`src/` 目录：

```text
muyun-web/src/web-core/                   muyun-web/tests/web-core/
muyun-web/src/web-contracts/              muyun-web/tests/web-contracts/
muyun-web/src/vue-ui-antdv/               muyun-web/tests/vue-ui-antdv/
muyun-web/src/platform-components/        muyun-web/tests/platform-components/
muyun-web/src/platform-workbench/         muyun-web/tests/platform-workbench/
muyun-web/src/platform-admin-runtime/     muyun-web/tests/platform-admin-runtime/
muyun-web/src/dynamic-page-runtime/       muyun-web/tests/dynamic-page-runtime/
muyun-web/src/app/                        muyun-web/tests/app/
muyun-web/src/views/                      muyun-web/tests/views/
```

测试基础设施使用两个辅助目录：

```text
muyun-web/tests/setup/       jsdom 和组件测试初始化
muyun-web/tests/fixtures/    跨测试复用的稳定测试数据
```

现有测试按其主要被测生产模块迁移到对应目录。一个测试跨越多个层时，归入拥有入口编排职责的
模块；不创建 `common`、`misc` 或含义不清的公共测试目录。测试 fixture 只有在被多个测试文件
稳定复用时才进入 `fixtures/`，单个测试的构造数据继续留在测试文件内。

## 测试分组与环境

使用 Vitest Projects 或等价的明确分组配置两个测试项目：

| 测试项目    | 文件范围                           | Environment | 用途                                                             |
| ----------- | ---------------------------------- | ----------- | ---------------------------------------------------------------- |
| `unit`      | 普通 `*.test.ts`，排除组件测试命名 | `node`      | 状态模型、契约、client、导航、数据转换和其他基础 TypeScript 逻辑 |
| `component` | `*.component.test.ts`              | `jsdom`     | Vue SFC、DOM、props、slots、emits、provide/inject 和用户交互     |

`unit` 不加载 jsdom setup，避免纯逻辑测试隐式依赖浏览器全局对象。`component` 通过统一 setup
安装必要的 Vue 组件测试清理和项目级测试约定。只有确需组件挂载或 DOM 的测试才使用
`.component.test.ts` 命名。

## 配置和命令

实施阶段应完成以下测试基础设施收口：

1. 增加 Vitest、jsdom 和 Vue Test Utils 开发依赖。
2. 增加独立 Vitest 配置并复用现有 Vite Vue 插件与 alias，避免复制两套模块解析规则。
3. 删除仅服务 Node Test TypeScript 执行的自定义 loader 和注册脚本。
4. 由 Vitest 按文件规则自动发现测试，删除 `package.json` 中的测试文件枚举。
5. 保留 Node 版本要求的工程门禁，但不再把 Node Test Runner 作为测试入口。
6. 统一提供以下命令语义：

```text
npm test                非 watch 模式执行全部 Vitest 项目
npm run test:watch      watch 模式执行相关测试
npm run test:unit       只执行 unit 项目
npm run test:component  只执行 component 项目
```

现有 `npm run check` 应继续调用统一的 `npm test`，不额外维护旧测试兼容入口。本阶段不增加
`test:coverage` 命令。

## 测试代码迁移规则

所有测试显式从 `vitest` 导入所需 API，不启用全局测试 API，保持依赖来源清晰：

```ts
import { describe, expect, it, vi } from "vitest";
```

常用断言按以下方式迁移：

| Node Test / Assert                   | Vitest                                                       |
| ------------------------------------ | ------------------------------------------------------------ |
| `test(name, fn)`                     | `it(name, fn)` 或 `test(name, fn)`                           |
| `assert.equal(actual, expected)`     | `expect(actual).toBe(expected)`                              |
| `assert.deepEqual(actual, expected)` | `expect(actual).toEqual(expected)`                           |
| `assert.ok(value)`                   | `expect(value).toBeTruthy()`；有明确语义时使用更精确 matcher |
| `assert.match(value, pattern)`       | `expect(value).toMatch(pattern)`                             |
| `assert.throws(fn)`                  | `expect(fn).toThrow()`                                       |
| `assert.rejects(promise)`            | `await expect(promise).rejects...`                           |
| 手工调用记录                         | `vi.fn()` 与 `toHaveBeenCalledWith()`                        |
| 手工替换模块依赖                     | `vi.mock()` 或 `vi.spyOn()`，优先保留显式依赖注入            |

迁移断言时保持原测试语义，不以更宽松的 matcher 换取通过。现有 fake 如果能清楚表达平台契约，
不要求机械替换为 `vi.fn()`；Vitest Mock 用于减少重复记录代码或验证交互事实，不替代领域测试
构造器。

## 实施顺序

迁移按一个完整目标推进，但可以分阶段验证：

1. 建立 Vitest 配置、两个测试项目、jsdom setup 和统一命令。
2. 将现有测试移动到镜像目录，完成 `node:test`、`node:assert` 和断言语法迁移。
3. 确认全部 50 个现有测试文件由自动发现执行，修复原默认命令遗漏。
4. 删除旧 loader、旧测试命令和其他仅服务 Node Test Runner 的基础设施。
5. 增加至少一个代表性 Vue 组件测试，验证 SFC、alias、jsdom 和 Vue Test Utils 链路。
6. 执行单元、组件、全部前端检查和生产构建，确认迁移未改变生产交付。

任何阶段都不以保留双轨作为最终状态。若迁移尚未完成，合并前必须继续收口到单一 Vitest
入口，而不是长期保留兼容命令。

## 验收标准

迁移完成必须同时满足：

1. 现有约 435 个测试用例完成等价迁移并通过。
2. 50 个现有测试文件全部被自动发现，包括原先未进入默认命令的 3 个文件。
3. 基础 TypeScript 测试运行于 Vitest `unit` 项目且不加载 jsdom。
4. 至少一个 Vue 组件挂载与交互测试验证 `.vue` 转换、jsdom 和 Vue Test Utils 配置有效。
5. 测试代码中不存在 `node:test`、`node:assert` 或旧测试 loader 引用。
6. `npm test`、`npm run test:unit` 和 `npm run test:component` 均可独立通过。
7. 前端 lint、格式检查、类型检查和生产构建继续通过。
8. 生产代码行为和 `@ximatai/muyun-web-app` 公开 API 不发生变化。
9. 未引入覆盖率依赖、覆盖率命令或覆盖率门禁。

## 完成后的文档处理

迁移验收后，将“Vitest 是唯一测试框架”“测试目录镜像源码”“jsdom 按需启用”等稳定契约
回收到[前端技术架构](../TECHNICAL_ARCHITECTURE.md)的验证与协作章节。本文件只保留仍对消费者或
维护者有价值的迁移边界；纯执行步骤失去价值后应删除，避免与实际配置形成两套事实来源。
