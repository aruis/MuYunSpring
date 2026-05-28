# M1 代码量压缩验证

状态：阶段结论，后续标准业务接入时继续复核

本文验证 M1 是否仍在逼近“业务能力不缺口，同时压缩业务代码量”的目标。统计口径为当前仓库 Java 源码行数，不把测试代码算入业务实现；行数来自 `wc -l`，对象范围见下表。

## 结论

M1 已经把基础 CRUD、软删、树、排序、引用、父子聚合、缓存、生命周期和租户 scope 下沉到平台能力层。组织样例显示静态业务服务已呈现收敛趋势：服务只负责声明能力组合和提供 DAO，不再重复实现平台流程。

动态侧的代码量没有消失，而是集中在运行态基础设施里。这个方向是可接受的：动态模块不为每个业务实体新增 service/dao/model，业务差异由元数据和能力声明表达。

## 当前数据

| 对象 | 文件 | 行数 | 说明 |
| --- | --- | ---: | --- |
| 组织模型 | `Organization.java` | 18 | 继承标准树模型，只声明业务字段 `code` |
| 组织 DAO | `OrganizationDao.java` | 8 | 继承 `BaseDao`，不写 CRUD SQL |
| 组织服务 | `OrganizationService.java` | 34 | 组合 CRUD、软删、树、引用能力 |
| CRUD 能力 | `CrudAbility.java` | 206 | 插入、查询、更新、删除、分页、计数、生命周期和租户 scope |
| 软删能力 | `SoftDeleteAbility.java` | 81 | 软删过滤、软删写入、忽略软删读取、软删更新边界 |
| 排序能力 | `SortAbility.java` | 95 | 列表排序、相邻移动、批量重排 |
| 树能力 | `TreeAbility.java` | 102 | 子级、祖先、后代、树位置校验 |
| 引用能力 | `ReferenceAbility.java` | 58 | 标题解析和引用选项 |
| 动态实体服务 | `DynamicEntityService.java` | 356 | 动态记录接入同一组 Ability，并处理元数据能力开关 |
| 动态 DAO | `DynamicRecordDao.java` | 288 | 动态表 SQL 映射和 Criteria 编译接入 |
| 动态门面 | `DynamicRecordService.java` | 111 | 稳定运行态 API |

## 压缩效果

1. 静态组织业务没有重复写 CRUD、软删、树查询、排序、引用选项、租户过滤和建表规则。
2. `OrganizationService` 的业务代码主要是能力组合，新增同类静态业务时，预期只需要模型字段、DAO 接口、能力组合和必要 hook。
3. 动态实体不按业务实体生成 service/dao/model 三件套；同一套 `DynamicEntityService` 和 `DynamicRecordDao` 承载多个动态实体。
4. 静态建表入口和动态建表入口都已收口到平台字段、租户唯一约束和真实库 contract。

## 重复逻辑盘点

组织样例当前重复实现的平台流程数量为 0：没有手写 insert、update、delete、page、count、soft delete、children、reorder、title、referenceOptions、tenant scope 或 ensureTable 逻辑。

这个结论只覆盖 M1 的组织样例和动态运行态样例。M2 接入平台配置业务时，需要继续用同一口径复核，避免新业务重新把平台流程写回 service 层。

## 仍需关注

1. 动态运行态基础设施较重，后续新增能力时要避免继续堆大 `DynamicEntityService`。
2. 父子聚合仍依赖默认 hook 组合纪律，后续需要平台内部 hook 链来降低误用成本。
3. 缓存仍是 M1 验证形态，不是最终治理形态。
4. M2 开始接入平台配置业务时，要持续复查业务服务是否仍保持“能力组合为主，hook 为辅”。
