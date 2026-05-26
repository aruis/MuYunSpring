# 技术债记录

本文只记录已经确认但本阶段暂缓处理的平台级技术债。执行过程、临时复核和普通 TODO 不放在这里。

## M1 暂缓项

| 编号 | 问题 | 风险 | 回收条件 |
| --- | --- | --- | --- |
| TD-001 | `ChildrenAbility` 依赖 `afterInsert/afterUpdate/afterDelete/afterSelect` 默认 hook 组合；业务服务覆盖 hook 时需要手动调用 super | 父子聚合可能因覆写纪律不足而静默失效 | 进入生命周期边界重构时，拆出平台内部 hook 链和业务扩展 hook |
| TD-002 | 聚合装配出的 child 当前按 RAW 查询读取，不执行 child service 完整 `afterSelect` / nested relation 语义 | 单独读取子实体和父聚合带出子实体的语义可能不一致 | 设计带深度控制的 RAW/HYDRATED 聚合读取策略 |
| TD-003 | `DynamicRecord` 无条件实现 `TreeCapable`、`TitledCapable`、`EnabledCapable`，能力事实由元数据二次判断 | marker interface 在动态侧更像适配器入口，外部泛型可能误读 | 引入动态能力适配层，或限制动态 marker 只在运行态内部使用 |
| TD-004 | `enabled` 字段边界未最终确定 | 接手方不清楚它是树附属字段、可启用能力字段，还是未来 UI/权限状态字段 | 设计可启用能力或从平台能力字段中移除 |
| TD-005 | `ReferencerAbility` 和引用依赖字段命名仍带有 namespace/source/target 混淆 | 引用缓存失效和服务定位可能各用一套字符串约定 | 改成显式目标模块/目标实体命名或引入目标引用标识 value object |
| TD-006 | `CacheAbility` 使用进程级静态 Map，无容量、卸载和 runtime 清理边界 | 动态运行态频繁发布后可能积累旧 namespace 缓存 | 设计运行态缓存管理器或发布/卸载清理入口 |
| TD-007 | `muyun-module` 的 `runtime` 包同时包含运行态、发布、注册、建表和映射职责 | 包结构继续扩大后会降低可读性 | 动态运行态进入 M3 前拆出 `schema`、`publish`、`runtime` 等边界 |
