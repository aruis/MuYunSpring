# 平台配置 Web API

本文只按当前代码中能确认的 URL 梳理配置专题相关 Web 入口。配置维护面以平台模块别名和模块聚合为主：应用、模块、元数据是独立配置根；模块动作、模块-元数据关系、模块字段配置挂在 `/platform.module/{moduleAlias}` 下。

## 配置维护入口

| 对象 | 当前服务线索 | Web API |
| --- | --- | --- |
| 应用 | `ApplicationService` | `/platform.application` |
| 模块 | `PlatformModuleService` | `/platform.module` |
| 模块动作 | `PlatformModuleActionService` | `/platform.module/{moduleAlias}/actions` |
| 元数据 | `MetadataService` | `/platform.metadata` |
| 元数据字段 | `MetadataFieldService` | `/platform.metadata/{metadataId}/fields` |
| 字段类型 | `PlatformFieldTypeService` | `/platform.field_type` |
| 字段 UI 类型 | `PlatformFieldUiTypeService` | `/platform.field_ui_type` |
| 字段 UI 类型属性 | `PlatformFieldUiTypeAttributeService` | `/platform.field_ui_type/{fieldUiTypeAlias}/attributes` |
| 字段 UI 类型字段映射 | `PlatformFieldUiTypeFieldMappingService` | `/platform.field_ui_type/{fieldUiTypeAlias}/field-mappings` |
| UI 配置集 | `PlatformUiSetService` | `/platform.module/{moduleAlias}/ui-sets` |
| UI 配置 | `PlatformUiConfigService` | `/platform.ui-set/{uiSetId}/configs` |
| UI 字段配置 | `PlatformUiConfigFieldService` | `/platform.ui-config/{uiConfigId}/fields` |
| 查询模板 | `PlatformQueryTemplateService` | `/platform.module/{moduleAlias}/query-templates` |
| 查询项 | `PlatformQueryItemService` | `/platform.query-template/{queryTemplateId}/items` |
| 动态运行态刷新 | `PlatformDynamicRuntimeRefreshService` | `/platform.module/{moduleAlias}/runtime` |
| 页面配置发布 | `PlatformPageConfigPublishService` | `/platform.page_config_publish` |
| 字段引用配置 | `MetadataFieldReferenceConfigService` | `/platform.metadata/{metadataId}/fields/{fieldId}/reference-configs` |
| 字段保护配置 | `MetadataFieldProtectionConfigService` | `/platform.metadata/{metadataId}/fields/{fieldId}/protection-configs` |
| 模块-元数据关系 | `ModuleMetadataRelationService` | `/platform.module/{moduleAlias}/metadata-relations` |
| 元数据视图 | `MetadataViewService` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/views` |
| 元数据视图字段 | `MetadataViewFieldService` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/views/{viewId}/fields` |
| 模块字段配置 | `ModuleMetadataFieldService` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/fields` |
| 模块字段引用过滤 | `ModuleMetadataFieldFilterService` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/fields/{fieldId}/filters` |
| 模块字段引用回填 | `ModuleMetadataFieldAffectService` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/fields/{fieldId}/affects` |
| 模块公式规则 | `ModuleMetadataFormulaRuleService` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/formula-rules` |
| 数据字典类目 | `DictionaryCategoryService` | `/platform.application/{applicationAlias}/dictionary-categories` |
| 数据字典项目 | `DictionaryItemService` | `/platform.application/{applicationAlias}/dictionary-categories/{categoryAlias}/items` |
| 计量单位分类 | `MeasureUnitCategoryService` | `/platform.measure_unit/categories` |
| 计量单位 | `MeasureUnitService` | `/platform.measure_unit/categories/{categoryAlias}/units` |
| 计量单位换算规则 | `MeasureUnitConversionRuleService` | `/platform.measure_unit/conversion-rules` |
| 币种 | `CurrencyService` | `/platform.currencies` |
| 租户币种设置 | `TenantCurrencySettingService` | `/platform.tenant-currency-settings` |
| 汇率类型 | `ExchangeRateTypeService` | `/platform.exchange-rate-types` |
| 汇率 | `ExchangeRateService` | `/platform.exchange-rates` |
| 菜单方案 | `MenuSchemeService` | `/platform.menu_scheme` |
| 菜单维护 | `MenuService` | `/platform.menu-scheme/{schemeId}/menus` |

## 标准维护接口

应用、模块、元数据、字段类型和字段 UI 类型使用平台标准维护风格。模块树按应用聚合，不提供无应用边界的全局树。

| 对象 | 方法 | URL | 功能点 |
| --- | --- | --- | --- |
| 应用 | `POST` | `/platform.application/query` | 查询应用列表，支持安全字段过滤和排序 |
| 应用 | `GET` | `/platform.application/view/{id}` | 查看应用 |
| 应用 | `POST` | `/platform.application/insert` | 新增应用 |
| 应用 | `POST` | `/platform.application/update/{id}` | 更新应用 |
| 应用 | `POST` | `/platform.application/delete/{id}` | 删除应用 |
| 应用 | `POST` | `/platform.application/enable/{id}`、`/disable/{id}` | 启用或停用应用 |
| 应用 | `POST` | `/platform.application/sort/{id}` | 调整应用排序 |
| 模块 | `POST` | `/platform.module/query` | 查询模块列表，支持按应用、父级、模块类型等字段过滤 |
| 模块 | `GET` | `/platform.module/view/{id}` | 查看模块；`id` 即 `moduleAlias` |
| 模块 | `POST` | `/platform.module/insert` | 新增模块 |
| 模块 | `POST` | `/platform.module/update/{id}` | 更新模块 |
| 模块 | `POST` | `/platform.module/delete/{id}` | 删除模块 |
| 模块 | `POST` | `/platform.module/enable/{id}`、`/disable/{id}` | 启用或停用模块 |
| 模块 | `POST` | `/platform.module/sort/{id}` | 在应用内调整模块树位置 |
| 模块 | `GET` | `/platform.module/tree/{applicationAlias}` | 获取指定应用下的模块树 |
| 模块 | `GET` | `/platform.module/tree/{applicationAlias}/{parentId}` | 获取指定父模块下的子树或扁平列表 |
| 模块 | `POST` | `/platform.module/{moduleAlias}/runtime/preview-refresh` | 预览把当前模块配置编译并同步到动态运行态的 schema 变更；dry-run，不更新运行态 registry |
| 模块 | `POST` | `/platform.module/{moduleAlias}/runtime/refresh` | 把当前模块配置编译为 `ModuleDefinition`，必要时执行 schema ensure，并刷新 `DynamicRecordRuntime` registry |
| 元数据 | `POST` | `/platform.metadata/query` | 查询元数据列表，支持按应用、别名、物理表等字段过滤 |
| 元数据 | `GET` | `/platform.metadata/view/{id}` | 查看元数据 |
| 元数据 | `POST` | `/platform.metadata/insert` | 新增元数据 |
| 元数据 | `POST` | `/platform.metadata/update/{id}` | 更新元数据 |
| 元数据 | `POST` | `/platform.metadata/delete/{id}` | 删除元数据 |
| 元数据 | `POST` | `/platform.metadata/enable/{id}`、`/disable/{id}` | 启用或停用元数据 |
| 元数据 | `POST` | `/platform.metadata/sort/{id}` | 在应用内调整元数据排序 |
| 字段类型 | `POST` | `/platform.field_type/query` | 查询字段类型目录 |
| 字段类型 | `GET` | `/platform.field_type/view/{id}` | 查看字段类型 |
| 字段类型 | `POST` | `/platform.field_type/insert` | 新增字段类型 |
| 字段类型 | `POST` | `/platform.field_type/update/{id}` | 更新字段类型 |
| 字段类型 | `POST` | `/platform.field_type/delete/{id}` | 删除字段类型 |
| 字段类型 | `POST` | `/platform.field_type/enable/{id}`、`/disable/{id}` | 启用或停用字段类型 |
| 字段类型 | `POST` | `/platform.field_type/sort/{id}` | 调整字段类型排序 |
| 字段 UI 类型 | `POST` | `/platform.field_ui_type/query` | 查询字段 UI 类型目录 |
| 字段 UI 类型 | `GET` | `/platform.field_ui_type/view/{id}` | 查看字段 UI 类型 |
| 字段 UI 类型 | `POST` | `/platform.field_ui_type/insert` | 新增字段 UI 类型 |
| 字段 UI 类型 | `POST` | `/platform.field_ui_type/update/{id}` | 更新字段 UI 类型 |
| 字段 UI 类型 | `POST` | `/platform.field_ui_type/delete/{id}` | 删除字段 UI 类型 |
| 字段 UI 类型 | `POST` | `/platform.field_ui_type/enable/{id}`、`/disable/{id}` | 启用或停用字段 UI 类型 |
| 字段 UI 类型 | `POST` | `/platform.field_ui_type/sort/{id}` | 调整字段 UI 类型排序 |

## 字段 UI 类型配置

字段 UI 类型属性和字段映射挂在字段 UI 类型 alias 下。请求体中的 `fieldUiTypeAlias` 以后端 URL 为准，避免跨 UI 类型维护。

| 对象 | 方法 | URL | 功能点 |
| --- | --- | --- | --- |
| UI 类型属性 | `POST` | `/platform.field_ui_type/{fieldUiTypeAlias}/attributes/query` | 查询 UI 类型属性 |
| UI 类型属性 | `GET` | `/platform.field_ui_type/{fieldUiTypeAlias}/attributes/view/{id}` | 查看 UI 类型属性，并校验归属 |
| UI 类型属性 | `POST` | `/platform.field_ui_type/{fieldUiTypeAlias}/attributes/insert` | 新增 UI 类型属性；后端以 URL 中的 `fieldUiTypeAlias` 为准 |
| UI 类型属性 | `POST` | `/platform.field_ui_type/{fieldUiTypeAlias}/attributes/update/{id}` | 更新 UI 类型属性，并保持归属不跨 UI 类型 |
| UI 类型属性 | `POST` | `/platform.field_ui_type/{fieldUiTypeAlias}/attributes/delete/{id}` | 删除 UI 类型属性 |
| UI 类型属性 | `POST` | `/platform.field_ui_type/{fieldUiTypeAlias}/attributes/sort/{id}` | 在同一 UI 类型内调整属性顺序 |
| UI 类型字段映射 | `POST` | `/platform.field_ui_type/{fieldUiTypeAlias}/field-mappings/query` | 查询 UI 类型字段映射 |
| UI 类型字段映射 | `GET` | `/platform.field_ui_type/{fieldUiTypeAlias}/field-mappings/view/{id}` | 查看 UI 类型字段映射，并校验归属 |
| UI 类型字段映射 | `POST` | `/platform.field_ui_type/{fieldUiTypeAlias}/field-mappings/insert` | 新增 UI 类型字段映射；后端以 URL 中的 `fieldUiTypeAlias` 为准 |
| UI 类型字段映射 | `POST` | `/platform.field_ui_type/{fieldUiTypeAlias}/field-mappings/update/{id}` | 更新 UI 类型字段映射，并保持归属不跨 UI 类型 |
| UI 类型字段映射 | `POST` | `/platform.field_ui_type/{fieldUiTypeAlias}/field-mappings/delete/{id}` | 删除 UI 类型字段映射 |
| UI 类型字段映射 | `POST` | `/platform.field_ui_type/{fieldUiTypeAlias}/field-mappings/sort/{id}` | 在同一 UI 类型内调整字段映射顺序 |

## 数据字典

数据字典类目挂在应用下，字典项目挂在类目下。请求体中的 `applicationAlias` 和 `categoryAlias` 以后端 URL 为准，避免跨应用或跨类目维护。

| 对象 | 方法 | URL | 功能点 |
| --- | --- | --- | --- |
| 字典类目 | `POST` | `/platform.application/{applicationAlias}/dictionary-categories/query` | 查询应用下的字典类目 |
| 字典类目 | `GET` | `/platform.application/{applicationAlias}/dictionary-categories/tree` | 获取应用下的字典类目树，可用 `flat=true` 返回扁平列表 |
| 字典类目 | `GET` | `/platform.application/{applicationAlias}/dictionary-categories/tree/{id}` | 获取指定类目下的子树 |
| 字典类目 | `GET` | `/platform.application/{applicationAlias}/dictionary-categories/view/{id}` | 查看字典类目，并校验类目属于该应用 |
| 字典类目 | `POST` | `/platform.application/{applicationAlias}/dictionary-categories/insert` | 新增字典类目；后端以 URL 中的 `applicationAlias` 为准 |
| 字典类目 | `POST` | `/platform.application/{applicationAlias}/dictionary-categories/update/{id}` | 更新字典类目，并保持应用归属不跨应用 |
| 字典类目 | `POST` | `/platform.application/{applicationAlias}/dictionary-categories/delete/{id}` | 删除字典类目 |
| 字典类目 | `POST` | `/platform.application/{applicationAlias}/dictionary-categories/enable/{id}`、`/disable/{id}` | 启用或停用字典类目 |
| 字典类目 | `POST` | `/platform.application/{applicationAlias}/dictionary-categories/sort/{id}` | 在同一应用和父类目范围内调整类目顺序 |
| 字典项目 | `POST` | `/platform.application/{applicationAlias}/dictionary-categories/{categoryAlias}/items/query` | 查询类目下的字典项目 |
| 字典项目 | `GET` | `/platform.application/{applicationAlias}/dictionary-categories/{categoryAlias}/items/tree` | 获取类目下的项目树，可用 `flat=true` 返回扁平列表 |
| 字典项目 | `GET` | `/platform.application/{applicationAlias}/dictionary-categories/{categoryAlias}/items/tree/{id}` | 获取指定项目下的子树 |
| 字典项目 | `GET` | `/platform.application/{applicationAlias}/dictionary-categories/{categoryAlias}/items/view/{id}` | 查看字典项目，并校验项目属于该类目 |
| 字典项目 | `POST` | `/platform.application/{applicationAlias}/dictionary-categories/{categoryAlias}/items/insert` | 新增字典项目；后端以 URL 中的应用和类目为准 |
| 字典项目 | `POST` | `/platform.application/{applicationAlias}/dictionary-categories/{categoryAlias}/items/update/{id}` | 更新字典项目，并保持类目归属不跨类目 |
| 字典项目 | `POST` | `/platform.application/{applicationAlias}/dictionary-categories/{categoryAlias}/items/delete/{id}` | 删除字典项目 |
| 字典项目 | `POST` | `/platform.application/{applicationAlias}/dictionary-categories/{categoryAlias}/items/enable/{id}`、`/disable/{id}` | 启用或停用字典项目 |
| 字典项目 | `POST` | `/platform.application/{applicationAlias}/dictionary-categories/{categoryAlias}/items/sort/{id}` | 在同一类目和父项目范围内调整项目顺序 |

## 计量单位

计量单位主数据以共享单位库为主：`tenantId` 为空表示平台全局公开，`tenantId` 有值表示租户公开。共享入口固定把 `applicationAlias` 绑定为 `platform`，由平台配置管理员代管全局和租户公开单位；应用路径继续保留为兼容入口和应用视角消费入口，仍承担计量单位静态模块定义注册。运行态字段消费按“租户公开优先、平台全局公开兜底”解析，共享单位库优先于历史应用路径配置，应用路径只作为兼容兜底。

单位目录换算入口只处理同分类线性换算；跨分类或依赖模块、记录上下文的硬换算由计量单位换算规则入口承接。

| 对象 | 方法 | URL | 功能点 |
| --- | --- | --- | --- |
| 计量单位分类 | `POST` | `/platform.measure_unit/categories/query` | 查询共享计量单位分类，可按 `tenantId` 区分平台全局和租户公开 |
| 计量单位分类 | `GET` | `/platform.measure_unit/categories/options` | 获取共享计量单位分类候选，可用 `enabledOnly=false` 返回包含停用项的列表 |
| 计量单位分类 | `GET` | `/platform.measure_unit/categories/view/{id}` | 查看共享计量单位分类 |
| 计量单位分类 | `POST` | `/platform.measure_unit/categories/insert` | 新增共享计量单位分类；后端强制 `applicationAlias=platform` |
| 计量单位分类 | `POST` | `/platform.measure_unit/categories/update/{id}` | 更新共享计量单位分类，分类身份不可随意改动 |
| 计量单位分类 | `POST` | `/platform.measure_unit/categories/delete/{id}` | 删除共享计量单位分类 |
| 计量单位分类 | `POST` | `/platform.measure_unit/categories/enable/{id}`、`/disable/{id}` | 启用或停用共享计量单位分类 |
| 计量单位分类 | `POST` | `/platform.measure_unit/categories/sort/{id}` | 在同一租户和共享单位库内调整分类顺序 |
| 计量单位 | `POST` | `/platform.measure_unit/categories/{categoryAlias}/units/query` | 查询共享分类下的计量单位 |
| 计量单位 | `GET` | `/platform.measure_unit/categories/{categoryAlias}/units/options` | 获取共享分类下的单位候选，可用 `enabledOnly=false` 返回包含停用项的列表 |
| 计量单位 | `GET` | `/platform.measure_unit/categories/{categoryAlias}/units/view/{id}` | 查看共享计量单位 |
| 计量单位 | `POST` | `/platform.measure_unit/categories/{categoryAlias}/units/insert` | 新增共享计量单位；后端强制 `applicationAlias=platform` 和 URL 中的 `categoryAlias` |
| 计量单位 | `POST` | `/platform.measure_unit/categories/{categoryAlias}/units/update/{id}` | 更新共享计量单位，单位身份不可随意改动 |
| 计量单位 | `POST` | `/platform.measure_unit/categories/{categoryAlias}/units/delete/{id}` | 删除共享计量单位 |
| 计量单位 | `POST` | `/platform.measure_unit/categories/{categoryAlias}/units/enable/{id}`、`/disable/{id}` | 启用或停用共享计量单位 |
| 计量单位 | `POST` | `/platform.measure_unit/categories/{categoryAlias}/units/sort/{id}` | 在同一租户和分类内调整单位顺序 |
| 计量单位 | `POST` | `/platform.measure_unit/categories/{categoryAlias}/units/convert` | 按共享分类线性换算参数执行单位换算 |
| 换算规则 | `POST` | `/platform.measure_unit/conversion-rules/query` | 查询共享业务硬换算规则，可按 `tenantId` 区分平台全局和租户公开 |
| 换算规则 | `GET` | `/platform.measure_unit/conversion-rules/view/{id}` | 查看共享换算规则 |
| 换算规则 | `POST` | `/platform.measure_unit/conversion-rules/insert` | 新增共享换算规则；后端强制 `applicationAlias=platform` |
| 换算规则 | `POST` | `/platform.measure_unit/conversion-rules/update/{id}` | 更新共享换算规则，规则身份不可随意改动 |
| 换算规则 | `POST` | `/platform.measure_unit/conversion-rules/delete/{id}` | 删除共享换算规则 |
| 换算规则 | `POST` | `/platform.measure_unit/conversion-rules/enable/{id}`、`/disable/{id}` | 启用或停用共享换算规则 |
| 换算规则 | `POST` | `/platform.measure_unit/conversion-rules/sort/{id}` | 在同一租户和规则作用域内调整规则顺序 |
| 换算规则 | `POST` | `/platform.measure_unit/conversion-rules/convert` | 按共享单位库、模块和记录上下文预览业务硬换算 |
| 兼容分类 | `POST` | `/platform.application/{applicationAlias}/measure-unit-categories/query` | 查询应用路径下的计量单位分类 |
| 兼容分类 | `GET` | `/platform.application/{applicationAlias}/measure-unit-categories/options` | 获取应用视角可见的计量单位分类候选，包含共享单位库兜底 |
| 计量单位分类 | `GET` | `/platform.application/{applicationAlias}/measure-unit-categories/view/{id}` | 查看计量单位分类，并校验分类属于该应用 |
| 计量单位分类 | `POST` | `/platform.application/{applicationAlias}/measure-unit-categories/insert` | 新增计量单位分类；后端以 URL 中的 `applicationAlias` 为准 |
| 计量单位分类 | `POST` | `/platform.application/{applicationAlias}/measure-unit-categories/update/{id}` | 更新计量单位分类，并保持应用归属不跨应用 |
| 计量单位分类 | `POST` | `/platform.application/{applicationAlias}/measure-unit-categories/delete/{id}` | 删除计量单位分类 |
| 计量单位分类 | `POST` | `/platform.application/{applicationAlias}/measure-unit-categories/enable/{id}`、`/disable/{id}` | 启用或停用计量单位分类 |
| 计量单位分类 | `POST` | `/platform.application/{applicationAlias}/measure-unit-categories/sort/{id}` | 在同一应用内调整分类顺序 |
| 兼容单位 | `POST` | `/platform.application/{applicationAlias}/measure-unit-categories/{categoryAlias}/units/query` | 查询应用路径分类下的计量单位 |
| 兼容单位 | `GET` | `/platform.application/{applicationAlias}/measure-unit-categories/{categoryAlias}/units/options` | 获取应用视角可见单位候选，包含共享单位库兜底 |
| 计量单位 | `GET` | `/platform.application/{applicationAlias}/measure-unit-categories/{categoryAlias}/units/view/{id}` | 查看计量单位，并校验单位属于该分类 |
| 计量单位 | `POST` | `/platform.application/{applicationAlias}/measure-unit-categories/{categoryAlias}/units/insert` | 新增计量单位；后端以 URL 中的应用和分类为准 |
| 计量单位 | `POST` | `/platform.application/{applicationAlias}/measure-unit-categories/{categoryAlias}/units/update/{id}` | 更新计量单位，并保持分类归属不跨分类 |
| 计量单位 | `POST` | `/platform.application/{applicationAlias}/measure-unit-categories/{categoryAlias}/units/delete/{id}` | 删除计量单位 |
| 计量单位 | `POST` | `/platform.application/{applicationAlias}/measure-unit-categories/{categoryAlias}/units/enable/{id}`、`/disable/{id}` | 启用或停用计量单位 |
| 计量单位 | `POST` | `/platform.application/{applicationAlias}/measure-unit-categories/{categoryAlias}/units/sort/{id}` | 在同一分类内调整单位顺序 |
| 计量单位 | `POST` | `/platform.application/{applicationAlias}/measure-unit-categories/{categoryAlias}/units/convert` | 按同分类线性换算参数执行单位换算 |
| 换算规则 | `POST` | `/platform.application/{applicationAlias}/measure-unit-conversion-rules/query` | 查询应用下的业务硬换算规则 |
| 换算规则 | `GET` | `/platform.application/{applicationAlias}/measure-unit-conversion-rules/view/{id}` | 查看换算规则，并校验规则属于该应用 |
| 换算规则 | `POST` | `/platform.application/{applicationAlias}/measure-unit-conversion-rules/insert` | 新增换算规则；后端以 URL 中的 `applicationAlias` 为准 |
| 换算规则 | `POST` | `/platform.application/{applicationAlias}/measure-unit-conversion-rules/update/{id}` | 更新换算规则，并保持应用归属不跨应用 |
| 换算规则 | `POST` | `/platform.application/{applicationAlias}/measure-unit-conversion-rules/delete/{id}` | 删除换算规则 |
| 换算规则 | `POST` | `/platform.application/{applicationAlias}/measure-unit-conversion-rules/enable/{id}`、`/disable/{id}` | 启用或停用换算规则 |
| 换算规则 | `POST` | `/platform.application/{applicationAlias}/measure-unit-conversion-rules/sort/{id}` | 在同一规则作用域内调整规则顺序 |
| 换算规则 | `POST` | `/platform.application/{applicationAlias}/measure-unit-conversion-rules/convert` | 按应用、模块和记录上下文预览业务硬换算 |

计量单位验收优先使用换算预览接口，不需要先发布动态模块。

同分类线性换算请求示例：

```json
{
  "value": 2.5,
  "fromUnitCode": "kg",
  "toUnitCode": "g"
}
```

业务硬换算请求示例：

```json
{
  "applicationAlias": "sales",
  "moduleAlias": "sales.order",
  "contextObjectType": "sku_id",
  "contextObjectId": "sku-1",
  "value": 2,
  "fromCategoryAlias": "package",
  "fromUnitCode": "pallet",
  "toCategoryAlias": "package",
  "toUnitCode": "bottle"
}
```

共享换算预览入口使用 `applicationAlias` 表示消费方应用，用于校验 `moduleAlias` 和查找兼容应用路径规则；不传时默认按 `platform`。返回 `convertedValue=1152` 表示当前规则链可把 `2托` 换算为 `1152瓶`。跨分类硬换算使用同一入口，例如 `roll:roll -> length:m` 返回 `convertedValue=60` 可验收 `2卷=60米`。

## 币种与汇率

币种与汇率是平台金额口径的基础维护面。当前阶段提供币种目录、租户本位币、汇率类型、汇率维护、基础折算和模块金额字段准备动作；金额字段运行态可通过 `FieldDefinition.money` 接入动态记录保存归一。配置包健康检查和导入导出金额列联动属于后续能力。

| 对象 | 方法 | URL | 功能点 |
| --- | --- | --- | --- |
| 币种 | `POST` | `/platform.currencies/query` | 查询币种目录，可按当前租户看到租户配置和平台全局兜底配置 |
| 币种 | `GET` | `/platform.currencies/options` | 获取可见币种候选，可用 `enabledOnly=false` 返回包含停用项的列表 |
| 币种 | `GET` | `/platform.currencies/view/{id}` | 查看币种 |
| 币种 | `POST` | `/platform.currencies/insert` | 新增币种；业务 code 使用 ISO 4217 alpha-3 |
| 币种 | `POST` | `/platform.currencies/update/{id}` | 更新币种，币种 code 和 numericCode 不允许随意改动 |
| 币种 | `POST` | `/platform.currencies/delete/{id}` | 删除币种 |
| 币种 | `POST` | `/platform.currencies/enable/{id}`、`/disable/{id}` | 启用或停用币种 |
| 币种 | `POST` | `/platform.currencies/sort/{id}` | 在同一租户 scope 内调整币种顺序 |
| 租户币种设置 | `POST` | `/platform.tenant-currency-settings/query` | 查询当前租户币种设置 |
| 租户币种设置 | `GET` | `/platform.tenant-currency-settings/view/{id}` | 查看当前租户币种设置 |
| 租户币种设置 | `POST` | `/platform.tenant-currency-settings/insert` | 设置当前租户本位币 |
| 租户币种设置 | `POST` | `/platform.tenant-currency-settings/update/{id}` | 更新当前租户本位币 |
| 汇率类型 | `POST` | `/platform.exchange-rate-types/query` | 查询汇率类型 |
| 汇率类型 | `GET` | `/platform.exchange-rate-types/options` | 获取汇率类型候选 |
| 汇率类型 | `GET` | `/platform.exchange-rate-types/view/{id}` | 查看汇率类型 |
| 汇率类型 | `POST` | `/platform.exchange-rate-types/insert` | 新增汇率类型 |
| 汇率类型 | `POST` | `/platform.exchange-rate-types/update/{id}` | 更新汇率类型，类型 code 不允许随意改动 |
| 汇率类型 | `POST` | `/platform.exchange-rate-types/delete/{id}` | 删除汇率类型 |
| 汇率类型 | `POST` | `/platform.exchange-rate-types/enable/{id}`、`/disable/{id}` | 启用或停用汇率类型 |
| 汇率类型 | `POST` | `/platform.exchange-rate-types/sort/{id}` | 在同一租户 scope 内调整汇率类型顺序 |
| 汇率 | `POST` | `/platform.exchange-rates/query` | 查询汇率，可按币种对、汇率类型和生效日期过滤 |
| 汇率 | `GET` | `/platform.exchange-rates/view/{id}` | 查看汇率 |
| 汇率 | `POST` | `/platform.exchange-rates/insert` | 新增汇率 |
| 汇率 | `POST` | `/platform.exchange-rates/update/{id}` | 更新汇率，币种对、汇率类型和生效日期不允许随意改动 |
| 汇率 | `POST` | `/platform.exchange-rates/delete/{id}` | 删除汇率 |
| 汇率 | `POST` | `/platform.exchange-rates/enable/{id}`、`/disable/{id}` | 启用或停用汇率 |
| 汇率 | `POST` | `/platform.exchange-rates/sort/{id}` | 在同一币种对和汇率类型内调整汇率顺序 |
| 汇率 | `POST` | `/platform.exchange-rates/convert` | 按金额、源币种、目标币种、汇率类型和业务日期执行基础折算 |

汇率折算请求示例：

```json
{
  "amount": 12.34,
  "fromCurrencyCode": "USD",
  "toCurrencyCode": "CNY",
  "rateTypeCode": "SPOT",
  "rateDate": "2026-02-16"
}
```

## 菜单配置

菜单配置维护面分为菜单方案和方案内菜单树。菜单消费入口仍是 `/platform.menu/mine` 和 `/platform.menu/{menuId}/entry`，不替代配置维护接口。

| 对象 | 方法 | URL | 功能点 |
| --- | --- | --- | --- |
| 菜单方案 | `POST` | `/platform.menu_scheme/query` | 查询菜单方案，支持按别名、作用域、启停状态等字段过滤 |
| 菜单方案 | `GET` | `/platform.menu_scheme/view/{id}` | 查看菜单方案 |
| 菜单方案 | `POST` | `/platform.menu_scheme/insert` | 新增菜单方案 |
| 菜单方案 | `POST` | `/platform.menu_scheme/update/{id}` | 更新菜单方案；方案身份字段不可随意改动 |
| 菜单方案 | `POST` | `/platform.menu_scheme/delete/{id}` | 删除菜单方案 |
| 菜单方案 | `POST` | `/platform.menu_scheme/enable/{id}`、`/disable/{id}` | 启用或停用菜单方案 |
| 菜单方案 | `POST` | `/platform.menu_scheme/sort/{id}` | 在同一作用域下调整菜单方案顺序 |
| 菜单节点 | `POST` | `/platform.menu-scheme/{schemeId}/menus/query` | 查询指定方案下的菜单节点 |
| 菜单节点 | `GET` | `/platform.menu-scheme/{schemeId}/menus/tree` | 获取指定方案下的菜单树，可用 `flat=true` 返回扁平列表 |
| 菜单节点 | `GET` | `/platform.menu-scheme/{schemeId}/menus/tree/{id}` | 获取指定菜单节点下的子树 |
| 菜单节点 | `GET` | `/platform.menu-scheme/{schemeId}/menus/view/{id}` | 查看菜单节点，并校验节点属于该方案 |
| 菜单节点 | `POST` | `/platform.menu-scheme/{schemeId}/menus/insert` | 新增菜单节点；后端以 URL 中的 `schemeId` 为准 |
| 菜单节点 | `POST` | `/platform.menu-scheme/{schemeId}/menus/update/{id}` | 更新菜单节点，并保持方案归属不跨方案 |
| 菜单节点 | `POST` | `/platform.menu-scheme/{schemeId}/menus/delete/{id}` | 删除菜单节点 |
| 菜单节点 | `POST` | `/platform.menu-scheme/{schemeId}/menus/enable/{id}`、`/disable/{id}` | 启用或停用菜单节点 |
| 菜单节点 | `POST` | `/platform.menu-scheme/{schemeId}/menus/sort/{id}` | 在同一方案和父节点范围内调整菜单顺序 |

## 元数据字段

| 方法 | URL | 功能点 |
| --- | --- | --- |
| `POST` | `/platform.metadata/{metadataId}/fields/query` | 查询指定元数据下的字段 |
| `GET` | `/platform.metadata/{metadataId}/fields/view/{id}` | 查看字段，并校验字段属于该元数据 |
| `POST` | `/platform.metadata/{metadataId}/fields/insert` | 新增字段；后端以 URL 中的 `metadataId` 为准 |
| `POST` | `/platform.metadata/{metadataId}/fields/update/{id}` | 更新字段，并保持字段归属不跨元数据 |
| `POST` | `/platform.metadata/{metadataId}/fields/delete/{id}` | 删除字段 |
| `POST` | `/platform.metadata/{metadataId}/fields/enable/{id}`、`/disable/{id}` | 启用或停用字段 |
| `POST` | `/platform.metadata/{metadataId}/fields/sort/{id}` | 在同一元数据下调整字段顺序 |

字段行为配置挂在具体字段下。URL 中的 `metadataId` 和 `fieldId` 是归属边界，新增或更新时以后端路径为准。

| 对象 | 方法 | URL | 功能点 |
| --- | --- | --- | --- |
| 字段引用配置 | `POST` | `/platform.metadata/{metadataId}/fields/{fieldId}/reference-configs/query` | 查询字段引用配置 |
| 字段引用配置 | `GET` | `/platform.metadata/{metadataId}/fields/{fieldId}/reference-configs/view/{id}` | 查看字段引用配置 |
| 字段引用配置 | `POST` | `/platform.metadata/{metadataId}/fields/{fieldId}/reference-configs/insert` | 新增字段引用配置；后端以 URL 中的 `fieldId` 为准 |
| 字段引用配置 | `POST` | `/platform.metadata/{metadataId}/fields/{fieldId}/reference-configs/update/{id}` | 更新字段引用配置 |
| 字段引用配置 | `POST` | `/platform.metadata/{metadataId}/fields/{fieldId}/reference-configs/delete/{id}` | 删除字段引用配置 |
| 字段保护配置 | `POST` | `/platform.metadata/{metadataId}/fields/{fieldId}/protection-configs/query` | 查询字段保护配置 |
| 字段保护配置 | `GET` | `/platform.metadata/{metadataId}/fields/{fieldId}/protection-configs/view/{id}` | 查看字段保护配置 |
| 字段保护配置 | `POST` | `/platform.metadata/{metadataId}/fields/{fieldId}/protection-configs/insert` | 新增字段保护配置；后端以 URL 中的 `fieldId` 为准 |
| 字段保护配置 | `POST` | `/platform.metadata/{metadataId}/fields/{fieldId}/protection-configs/update/{id}` | 更新字段保护配置 |
| 字段保护配置 | `POST` | `/platform.metadata/{metadataId}/fields/{fieldId}/protection-configs/delete/{id}` | 删除字段保护配置 |

## 模块聚合配置

模块聚合接口只处理天然归属模块的配置。请求体里即使传入 `moduleAlias` 或 `relationId`，后端也以 URL 路径为准，并校验存量记录不能跨模块操作。

模块字段配置可声明计量单位消费契约。主数值字段通过 `unitCategoryAlias` 进入单位能力；`unitMode=FIXED` 时使用 `fixedUnitCode`，`unitMode=SELECTABLE` 时必须绑定同元数据、同 owner 的伴生单位字段 `unitFieldId`。`baseValueFieldId` 必须绑定同 owner 的影子标准值字段，`baseUnitCategoryAlias` 和 `baseUnitCode` 是归一基准单位，未配置基准分类时默认等于 `unitCategoryAlias`；`unitConversionMode` 表达线性目录换算或业务规则换算，`conversionScopeFieldId` 用于后续记录上下文换算。

模块字段配置提供单位字段准备动作。该动作以模块字段配置记录 `id` 为入口，自动准备可选单位伴生字段和标准值影子字段，同步当前 relation 下的伴生/影子模块字段配置，并回填主字段模块配置上的单位消费契约。固定单位模式不强制创建伴生单位字段；标准值影子字段始终会被准备。

模块字段配置也可声明金额消费契约。主金额字段通过 `moneyCurrencyMode` 进入金额能力；`FIXED` 时使用 `moneyFixedCurrencyCode`，`SELECTABLE` 时绑定同元数据、同 owner 的币种伴生字段 `moneyCurrencyFieldId`。`moneyBaseAmountFieldId` 是动态保存时写入的本位金额影子字段；`moneyBaseCurrencyCode` 可固定本位币，未配置时运行态按租户本位币设置解析；`moneyRateTypeCode` 必填；`moneyRateDateFieldId` 可绑定业务日期字段；`moneyExchangeRateFieldId` 可选，用于保存本次折算汇率。

模块字段配置提供金额字段准备动作。该动作以模块字段配置记录 `id` 为入口，自动准备可选币种伴生字段、本位金额影子字段和可选汇率影子字段，同步当前 relation 下的伴生/影子模块字段配置，并回填主字段模块配置上的金额消费契约。固定币种模式不创建币种伴生字段；本位金额影子字段始终会被准备。

`measure-unit/prepare` 和 `money/prepare` 是保存辅助动作，不是发布、预览或可逆开关。它们会真实创建或复用元数据伴生/影子字段，并保存当前模块字段配置；新建或更新的元数据字段会按元数据保存即生效规则触发 schema ensure。管理端如果已经自行维护好伴生/影子字段，也可以直接走标准 `fields/update/{id}` 保存绑定关系，避免把 prepare 当成额外的发布状态。

管理端可以按两种方式配置计量单位字段：

1. 使用 `fields/{id}/measure-unit/prepare`，由平台按主数值字段自动创建或复用单位伴生字段、标准值影子字段，并回填当前模块字段配置。
2. 使用 `fields/update/{id}`，保存已存在的 `unitFieldId`、`baseValueFieldId`、`conversionScopeFieldId` 等绑定关系。标准更新不是局部 patch，管理端应先读取 `view/{id}`，合并计量单位字段后提交完整模块字段配置。

`measure-unit/prepare` 请求体为 `ModuleMetadataMeasureUnitPrepareCommand`：

| 字段 | 说明 |
| --- | --- |
| `unitCategoryAlias` | 必填，主业务值使用的单位分类 alias |
| `unitMode` | `SELECTABLE` 或 `FIXED`，为空时默认 `SELECTABLE` |
| `fixedUnitCode` | 固定单位模式必填 |
| `defaultUnitCode` | 可选单位模式的默认单位；固定单位模式未传时默认等于 `fixedUnitCode` |
| `unitFieldName` | 可选单位模式下准备的伴生字段名，默认 `<ownerFieldName>Unit` |
| `baseValueFieldName` | 准备的标准值影子字段名，默认 `<ownerFieldName>Base` |
| `baseUnitCategoryAlias` | 标准值单位分类，空时默认等于 `unitCategoryAlias` |
| `baseUnitCode` | 必填，标准值使用的基准单位 code |
| `unitConversionMode` | `LINEAR` 或 `BUSINESS_RULE`，为空时默认 `LINEAR` |
| `conversionScopeFieldId` | 可选，业务硬换算的记录上下文字段 |
| `unitRequired` | 可选，单位是否必填；为空时按后端默认值处理 |
| `unitFieldTypeAlias` | 伴生单位字段类型，默认 `string` |
| `baseValueFieldTypeAlias` | 标准值影子字段类型，默认等于主数值字段类型 |

返回体为 `ModuleMetadataMeasureUnitPrepareResult`：

| 字段 | 说明 |
| --- | --- |
| `moduleField` | 已回填计量单位契约的模块字段配置 |
| `unitField` | 可选单位模式下创建或复用的单位伴生元数据字段；固定单位模式为空 |
| `baseValueField` | 创建或复用的标准值影子元数据字段 |

管理端可以按两种方式配置金额字段：

1. 使用 `fields/{id}/money/prepare`，由平台按主金额字段自动创建或复用币种伴生字段、本位金额影子字段和可选汇率影子字段，并回填当前模块字段配置。
2. 使用 `fields/update/{id}`，保存已存在的 `moneyCurrencyFieldId`、`moneyBaseAmountFieldId`、`moneyRateDateFieldId`、`moneyExchangeRateFieldId` 等绑定关系。标准更新不是局部 patch，管理端应先读取 `view/{id}`，合并金额字段后提交完整模块字段配置。

`money/prepare` 请求体为 `ModuleMetadataMoneyPrepareCommand`：

| 字段 | 说明 |
| --- | --- |
| `currencyMode` | `SELECTABLE` 或 `FIXED`，为空时默认 `SELECTABLE` |
| `fixedCurrencyCode` | 固定币种模式必填，ISO 4217 alpha-3 |
| `defaultCurrencyCode` | 可选币种模式的默认币种；固定币种模式未传时默认等于 `fixedCurrencyCode` |
| `currencyFieldName` | 可选币种模式下准备的币种伴生字段名，默认 `<ownerFieldName>Currency` |
| `baseAmountFieldName` | 准备的本位金额影子字段名，默认 `<ownerFieldName>Base` |
| `baseCurrencyCode` | 可选，固定本位币；空时运行态按租户本位币设置解析 |
| `rateTypeCode` | 必填，汇率类型 code |
| `rateDateFieldId` | 可选，同元数据下的业务日期字段，支持 `DATE`、`TIMESTAMP`、`ZONED_TIMESTAMP` |
| `exchangeRateFieldName` | 可选，准备的汇率影子字段名；传入时会创建汇率字段 |
| `createExchangeRateField` | 可选，`true` 时按默认字段名创建汇率字段 |
| `currencyRequired` | 可选，币种是否必填，默认 `true` |
| `currencyFieldTypeAlias` | 可选，币种字段类型，默认 `string` |
| `baseAmountFieldTypeAlias` | 可选，本位金额字段类型，默认等于主金额字段类型 |
| `exchangeRateFieldTypeAlias` | 可选，汇率字段类型，默认 `decimal` |

返回体为 `ModuleMetadataMoneyPrepareResult`：

| 字段 | 说明 |
| --- | --- |
| `moduleField` | 已回填金额契约的模块字段配置 |
| `currencyField` | 可选币种模式下创建或复用的币种伴生元数据字段；固定币种模式为空 |
| `baseAmountField` | 创建或复用的本位金额影子元数据字段 |
| `exchangeRateField` | 创建或复用的汇率影子元数据字段；未请求汇率字段时为空 |

典型请求：

```json
{
  "unitCategoryAlias": "package",
  "unitMode": "SELECTABLE",
  "defaultUnitCode": "box",
  "unitFieldName": "quantityUnit",
  "baseValueFieldName": "quantityBase",
  "baseUnitCategoryAlias": "package",
  "baseUnitCode": "bottle",
  "unitConversionMode": "BUSINESS_RULE",
  "unitRequired": true
}
```

| 对象 | 方法 | URL | 功能点 |
| --- | --- | --- | --- |
| 模块动作 | `POST` | `/platform.module/{moduleAlias}/actions/query` | 查询模块动作 |
| 模块动作 | `GET` | `/platform.module/{moduleAlias}/actions/view/{id}` | 查看模块动作 |
| 模块动作 | `POST` | `/platform.module/{moduleAlias}/actions/insert` | 新增模块动作 |
| 模块动作 | `POST` | `/platform.module/{moduleAlias}/actions/update/{id}` | 更新模块动作 |
| 模块动作 | `POST` | `/platform.module/{moduleAlias}/actions/delete/{id}` | 删除模块动作 |
| 模块动作 | `POST` | `/platform.module/{moduleAlias}/actions/enable/{id}`、`/disable/{id}` | 启用或停用模块动作 |
| 模块动作 | `POST` | `/platform.module/{moduleAlias}/actions/sort/{id}` | 在模块内调整动作顺序 |
| 元数据关系 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/query` | 查询模块绑定的元数据关系 |
| 元数据关系 | `GET` | `/platform.module/{moduleAlias}/metadata-relations/view/{id}` | 查看模块元数据关系 |
| 元数据关系 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/insert` | 新增模块元数据关系 |
| 元数据关系 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/update/{id}` | 更新模块元数据关系 |
| 元数据关系 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/delete/{id}` | 删除模块元数据关系 |
| 元数据关系 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/sort/{id}` | 在模块内调整关系顺序 |
| 元数据视图 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/views/query` | 查询关系下的元数据视图 |
| 元数据视图 | `GET` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/views/view/{id}` | 查看元数据视图 |
| 元数据视图 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/views/insert` | 新增元数据视图；后端以 URL 中的 `relationId` 为准 |
| 元数据视图 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/views/update/{id}` | 更新元数据视图，并保持视图不跨关系；关系本身不跨模块 |
| 元数据视图 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/views/delete/{id}` | 删除元数据视图 |
| 元数据视图 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/views/enable/{id}`、`/disable/{id}` | 启用或停用元数据视图 |
| 元数据视图 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/views/sort/{id}` | 在关系内调整视图顺序 |
| 元数据视图字段 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/views/{viewId}/fields/query` | 查询视图字段 |
| 元数据视图字段 | `GET` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/views/{viewId}/fields/view/{id}` | 查看视图字段 |
| 元数据视图字段 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/views/{viewId}/fields/insert` | 新增视图字段；后端以 URL 中的 `viewId` 为准 |
| 元数据视图字段 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/views/{viewId}/fields/update/{id}` | 更新视图字段，并保持视图归属不跨关系 |
| 元数据视图字段 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/views/{viewId}/fields/delete/{id}` | 删除视图字段 |
| 元数据视图字段 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/views/{viewId}/fields/enable/{id}`、`/disable/{id}` | 启用或停用视图字段 |
| 元数据视图字段 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/views/{viewId}/fields/sort/{id}` | 在视图内调整字段顺序 |
| 模块字段 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/fields/query` | 查询关系下的模块字段配置 |
| 模块字段 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/fields/ensure` | 按元数据字段同步生成模块字段配置 |
| 模块字段 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/fields/{id}/measure-unit/prepare` | 为主数值字段准备单位伴生字段、标准值影子字段并回填计量单位配置 |
| 模块字段 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/fields/{id}/money/prepare` | 为主金额字段准备币种伴生字段、本位金额影子字段、可选汇率影子字段并回填金额配置 |
| 模块字段 | `GET` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/fields/view/{id}` | 查看模块字段配置 |
| 模块字段 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/fields/insert` | 新增模块字段配置 |
| 模块字段 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/fields/update/{id}` | 更新模块字段配置 |
| 模块字段 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/fields/delete/{id}` | 删除模块字段配置 |
| 模块字段 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/fields/sort/{id}` | 在关系内调整字段配置顺序 |
| 字段引用过滤 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/fields/{fieldId}/filters/query` | 查询模块字段引用过滤配置 |
| 字段引用过滤 | `GET` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/fields/{fieldId}/filters/view/{id}` | 查看模块字段引用过滤配置 |
| 字段引用过滤 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/fields/{fieldId}/filters/insert` | 新增模块字段引用过滤配置；后端以 URL 中的 `fieldId` 为准 |
| 字段引用过滤 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/fields/{fieldId}/filters/update/{id}` | 更新模块字段引用过滤配置 |
| 字段引用过滤 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/fields/{fieldId}/filters/delete/{id}` | 删除模块字段引用过滤配置 |
| 字段引用过滤 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/fields/{fieldId}/filters/sort/{id}` | 在字段内调整引用过滤顺序 |
| 字段引用回填 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/fields/{fieldId}/affects/query` | 查询模块字段引用回填配置 |
| 字段引用回填 | `GET` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/fields/{fieldId}/affects/view/{id}` | 查看模块字段引用回填配置 |
| 字段引用回填 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/fields/{fieldId}/affects/insert` | 新增模块字段引用回填配置；后端以 URL 中的 `fieldId` 为准 |
| 字段引用回填 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/fields/{fieldId}/affects/update/{id}` | 更新模块字段引用回填配置 |
| 字段引用回填 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/fields/{fieldId}/affects/delete/{id}` | 删除模块字段引用回填配置 |
| 字段引用回填 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/fields/{fieldId}/affects/sort/{id}` | 在字段内调整引用回填顺序 |
| 模块公式规则 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/formula-rules/query` | 查询关系下的公式规则 |
| 模块公式规则 | `GET` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/formula-rules/view/{id}` | 查看公式规则 |
| 模块公式规则 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/formula-rules/insert` | 新增公式规则；后端以 URL 中的 `relationId` 为准 |
| 模块公式规则 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/formula-rules/update/{id}` | 更新公式规则 |
| 模块公式规则 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/formula-rules/delete/{id}` | 删除公式规则 |
| 模块公式规则 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/formula-rules/enable/{id}`、`/disable/{id}` | 启用或停用公式规则 |
| 模块公式规则 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/formula-rules/sort/{id}` | 在关系内调整公式规则顺序 |

## 页面配置与查询模板

页面配置维护面仍按配置归属聚合。UI 配置集和查询模板挂在模块下；具体 UI 配置挂在 UI 配置集下；UI 字段配置挂在 UI 配置下；查询项挂在查询模板下。请求体中的归属字段以后端 URL 为准。

| 对象 | 方法 | URL | 功能点 |
| --- | --- | --- | --- |
| UI 配置集 | `POST` | `/platform.module/{moduleAlias}/ui-sets/query` | 查询模块下的 UI 配置集 |
| UI 配置集 | `GET` | `/platform.module/{moduleAlias}/ui-sets/view/{id}` | 查看 UI 配置集 |
| UI 配置集 | `POST` | `/platform.module/{moduleAlias}/ui-sets/insert` | 新增 UI 配置集；后端以 URL 中的 `moduleAlias` 为准 |
| UI 配置集 | `POST` | `/platform.module/{moduleAlias}/ui-sets/update/{id}` | 更新 UI 配置集 |
| UI 配置集 | `POST` | `/platform.module/{moduleAlias}/ui-sets/delete/{id}` | 删除 UI 配置集 |
| UI 配置集 | `POST` | `/platform.module/{moduleAlias}/ui-sets/enable/{id}`、`/disable/{id}` | 启用或停用 UI 配置集 |
| UI 配置集 | `POST` | `/platform.module/{moduleAlias}/ui-sets/sort/{id}` | 在模块内调整 UI 配置集排序 |
| UI 配置 | `POST` | `/platform.ui-set/{uiSetId}/configs/query` | 查询配置集下的 UI 配置 |
| UI 配置 | `GET` | `/platform.ui-set/{uiSetId}/configs/view/{id}` | 查看 UI 配置 |
| UI 配置 | `POST` | `/platform.ui-set/{uiSetId}/configs/insert` | 新增 UI 配置；后端以 URL 中的 `uiSetId` 为准 |
| UI 配置 | `POST` | `/platform.ui-set/{uiSetId}/configs/update/{id}` | 更新 UI 配置；已发布配置需先取消发布 |
| UI 配置 | `POST` | `/platform.ui-set/{uiSetId}/configs/delete/{id}` | 删除 UI 配置；已发布配置需先取消发布 |
| UI 配置 | `POST` | `/platform.ui-set/{uiSetId}/configs/enable/{id}`、`/disable/{id}` | 启用或停用 UI 配置 |
| UI 配置 | `POST` | `/platform.ui-set/{uiSetId}/configs/sort/{id}` | 在配置集内调整 UI 配置排序 |
| UI 字段配置 | `POST` | `/platform.ui-config/{uiConfigId}/fields/query` | 查询 UI 配置下的字段配置 |
| UI 字段配置 | `GET` | `/platform.ui-config/{uiConfigId}/fields/view/{id}` | 查看字段配置 |
| UI 字段配置 | `POST` | `/platform.ui-config/{uiConfigId}/fields/insert` | 新增字段配置；后端以 URL 中的 `uiConfigId` 为准 |
| UI 字段配置 | `POST` | `/platform.ui-config/{uiConfigId}/fields/update/{id}` | 更新字段配置；已发布 UI 配置不可直接编辑字段 |
| UI 字段配置 | `POST` | `/platform.ui-config/{uiConfigId}/fields/delete/{id}` | 删除字段配置 |
| UI 字段配置 | `POST` | `/platform.ui-config/{uiConfigId}/fields/enable/{id}`、`/disable/{id}` | 启用或停用字段配置 |
| UI 字段配置 | `POST` | `/platform.ui-config/{uiConfigId}/fields/sort/{id}` | 在 UI 配置内调整字段顺序 |
| 查询模板 | `POST` | `/platform.module/{moduleAlias}/query-templates/query` | 查询模块下的查询模板 |
| 查询模板 | `GET` | `/platform.module/{moduleAlias}/query-templates/view/{id}` | 查看查询模板 |
| 查询模板 | `POST` | `/platform.module/{moduleAlias}/query-templates/insert` | 新增查询模板；后端以 URL 中的 `moduleAlias` 为准 |
| 查询模板 | `POST` | `/platform.module/{moduleAlias}/query-templates/update/{id}` | 更新查询模板；已发布模板需先取消发布 |
| 查询模板 | `POST` | `/platform.module/{moduleAlias}/query-templates/delete/{id}` | 删除查询模板；已发布模板需先取消发布 |
| 查询模板 | `POST` | `/platform.module/{moduleAlias}/query-templates/enable/{id}`、`/disable/{id}` | 启用或停用查询模板 |
| 查询模板 | `POST` | `/platform.module/{moduleAlias}/query-templates/sort/{id}` | 在模块内调整查询模板排序 |
| 查询项 | `POST` | `/platform.query-template/{queryTemplateId}/items/query` | 查询模板下的查询项 |
| 查询项 | `GET` | `/platform.query-template/{queryTemplateId}/items/view/{id}` | 查看查询项 |
| 查询项 | `POST` | `/platform.query-template/{queryTemplateId}/items/insert` | 新增查询项；后端以 URL 中的 `queryTemplateId` 为准 |
| 查询项 | `POST` | `/platform.query-template/{queryTemplateId}/items/update/{id}` | 更新查询项；已发布模板不可直接编辑查询项 |
| 查询项 | `POST` | `/platform.query-template/{queryTemplateId}/items/delete/{id}` | 删除查询项 |
| 查询项 | `POST` | `/platform.query-template/{queryTemplateId}/items/enable/{id}`、`/disable/{id}` | 启用或停用查询项 |
| 查询项 | `POST` | `/platform.query-template/{queryTemplateId}/items/sort/{id}` | 在同一查询组内调整查询项排序 |
| 页面配置发布 | `POST` | `/platform.page_config_publish/ui-configs/{id}/publish` | 校验并发布 UI 配置 |
| 页面配置发布 | `POST` | `/platform.page_config_publish/ui-configs/{id}/unpublish` | 取消发布 UI 配置 |
| 页面配置发布 | `POST` | `/platform.page_config_publish/query-templates/{id}/publish` | 校验并发布查询模板 |
| 页面配置发布 | `POST` | `/platform.page_config_publish/query-templates/{id}/unpublish` | 取消发布查询模板 |

## 相关消费入口

| 方法 | URL | 功能点 |
| --- | --- | --- |
| `GET` | `/platform.menu/mine` | 返回当前用户可见菜单树；后端按当前用户推理菜单方案，权限专题负责剪枝 |
| `GET` | `/platform.menu/{menuId}/entry` | 读取菜单节点对应的动态页面入口，可携带 `clientType`；页面 bootstrap 细节归属页面交付专题 |

上述两个 URL 是已存在的菜单消费/页面入口：`MenuWebController` 提供 `/mine`，`DynamicPageBootstrapWebController` 提供 `/{menuId}/entry`。它们不等同于菜单方案或菜单节点的配置维护接口。

## 刷新后的消费入口

动态模块刷新到运行态后，运行态 Web 入口使用业务根路径 `/{moduleAlias}`。这里是配置刷新后的消费面，不是配置维护面；完整接口清单归属运行态专题。

| 方法 | URL | 功能点 |
| --- | --- | --- |
| `GET` | `/{moduleAlias}/describe` | 读取动态模块运行态描述 |

动态运行态刷新通过 `/platform.module/{moduleAlias}/runtime/refresh` 完成，返回 `DynamicModulePublishResult`。它表达“当前配置同步到运行态”，不是配置包定稿、归档或跨环境迁移版本；配置包版本归档、指针切换、导入 dry-run 仍归属配置治理专题。影响 `ModuleDefinition` 编译结果的配置保存后会自动刷新受影响动态模块，事务提交后执行，无事务时立即执行：模块-元数据关系、模块字段消费配置、引用过滤/带出、公式规则、元数据视图、模块动作，以及元数据字段变化后引用该 metadata 的所有动态模块。页面配置和查询模板发布通过 `/platform.page_config_publish` 完成，保留“用户可见生效/取消生效”的发布语义，UI/query 普通保存不触发 runtime refresh。

## 关联专题入口

| 专题 | 说明 |
| --- | --- |
| 运行态 | `/{moduleAlias}` 下的查询、保存、动作、引用和 OpenAPI |
| 页面交付 | 菜单 entry bootstrap、页面偏好、查询模板、表单保存和附件关系 |
| 配置治理 | 配置包、健康检查、版本归档、指针切换、导入 dry-run 和模板复用 |

## 命名提醒

1. URL 中的模块身份统一使用 `moduleAlias`。
2. 配置对象字段、DTO 和关系列不使用 `moduleId` 表达模块身份。
3. 元数据业务别名使用 `metadataAlias`；物理表名不作为元数据身份。
