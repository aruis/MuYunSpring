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
| UI 配置集 | `PlatformUiSetService` | `/platform.module/{moduleAlias}/ui-sets` |
| UI 配置 | `PlatformUiConfigService` | `/platform.ui-set/{uiSetId}/configs` |
| UI 字段配置 | `PlatformUiConfigFieldService` | `/platform.ui-config/{uiConfigId}/fields` |
| 查询模板 | `PlatformQueryTemplateService` | `/platform.module/{moduleAlias}/query-templates` |
| 查询项 | `PlatformQueryItemService` | `/platform.query-template/{queryTemplateId}/items` |
| 页面配置发布 | `PlatformPageConfigPublishService` | `/platform.page_config_publish` |
| 字段引用配置 | `MetadataFieldReferenceConfigService` | `/platform.metadata/{metadataId}/fields/{fieldId}/reference-configs` |
| 字段保护配置 | `MetadataFieldProtectionConfigService` | `/platform.metadata/{metadataId}/fields/{fieldId}/protection-configs` |
| 元数据视图 | `MetadataViewService`、`MetadataViewFieldService` | 当前未暴露独立 Web Controller |
| 模块-元数据关系 | `ModuleMetadataRelationService` | `/platform.module/{moduleAlias}/metadata-relations` |
| 模块字段配置 | `ModuleMetadataFieldService` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/fields` |
| 模块字段引用过滤 | `ModuleMetadataFieldFilterService` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/fields/{fieldId}/filters` |
| 模块字段引用回填 | `ModuleMetadataFieldAffectService` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/fields/{fieldId}/affects` |
| 模块公式规则 | `ModuleMetadataFormulaRuleService` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/formula-rules` |
| 数据字典类目 | `DictionaryCategoryService` | 当前未暴露独立 Web Controller |
| 数据字典项目 | `DictionaryItemService` | 当前未暴露独立 Web Controller |
| 菜单方案 | `MenuSchemeService` | 当前未暴露独立 Web Controller |
| 菜单维护 | `MenuService` | 仅暴露当前用户菜单消费入口；基础维护当前未暴露独立 Web Controller |

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
| 模块字段 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/fields/query` | 查询关系下的模块字段配置 |
| 模块字段 | `POST` | `/platform.module/{moduleAlias}/metadata-relations/{relationId}/fields/ensure` | 按元数据字段同步生成模块字段配置 |
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

## 发布后的消费入口

动态模块发布后，运行态 Web 入口使用业务根路径 `/{moduleAlias}`。这里是配置发布后的消费面，不是配置维护面；完整接口清单归属运行态专题。

| 方法 | URL | 功能点 |
| --- | --- | --- |
| `GET` | `/{moduleAlias}/describe` | 读取动态模块运行态描述 |

页面配置和查询模板发布通过 `/platform.page_config_publish` 完成；动态模块定义发布和配置包治理仍归属配置治理专题。

## 关联专题入口

| 专题 | 说明 |
| --- | --- |
| 运行态 | `/{moduleAlias}` 下的查询、保存、动作、引用和 OpenAPI |
| 页面交付 | 菜单 entry bootstrap、页面偏好、查询模板、表单保存和附件关系 |
| 配置治理 | 配置包、健康检查、版本发布、回滚、导入 dry-run 和模板复用 |

## 命名提醒

1. URL 中的模块身份统一使用 `moduleAlias`。
2. 配置对象字段、DTO 和关系列不使用 `moduleId` 表达模块身份。
3. 元数据业务别名使用 `metadataAlias`；物理表名不作为元数据身份。
