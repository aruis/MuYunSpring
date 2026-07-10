package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.query.QueryAbility;
import net.ximatai.muyun.spring.ability.query.QueryCompiler;
import net.ximatai.muyun.spring.ability.query.QueryDescriptor;
import net.ximatai.muyun.spring.ability.query.QueryDescriptors;
import net.ximatai.muyun.spring.ability.query.QueryField;
import net.ximatai.muyun.spring.ability.query.QueryOperator;
import net.ximatai.muyun.spring.ability.query.QueryRequest;
import net.ximatai.muyun.spring.ability.query.QuerySchema;
import net.ximatai.muyun.spring.ability.query.QueryValueType;
import net.ximatai.muyun.spring.ability.reference.ModuleReadProjection;
import net.ximatai.muyun.spring.boot.web.WebPageResponse;
import net.ximatai.muyun.spring.common.option.OptionSourceRegistry;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContextHolder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class StaticRecordReadProjectionService {
    private final StaticModuleDefinitionCatalog staticModuleDefinitionCatalog;
    private final RelationProjectionReadService relationProjectionReadService;
    private final OptionSourceRegistry optionSourceRegistry;

    public StaticRecordReadProjectionService(StaticModuleDefinitionCatalog staticModuleDefinitionCatalog) {
        this(staticModuleDefinitionCatalog, (RelationProjectionReadService) null, null);
    }

    @Autowired
    public StaticRecordReadProjectionService(StaticModuleDefinitionCatalog staticModuleDefinitionCatalog,
                                             ObjectProvider<RelationProjectionReadService> relationProjectionReadService,
                                             ObjectProvider<OptionSourceRegistry> optionSourceRegistry) {
        this(staticModuleDefinitionCatalog,
                relationProjectionReadService == null ? null : relationProjectionReadService.getIfAvailable(),
                optionSourceRegistry == null ? null : optionSourceRegistry.getIfAvailable());
    }

    StaticRecordReadProjectionService(StaticModuleDefinitionCatalog staticModuleDefinitionCatalog,
                                      RelationProjectionQueryExecutor projectionQueryExecutor,
                                      RelationProjectionDatabaseTypeProvider databaseTypeProvider) {
        this(staticModuleDefinitionCatalog, projectionQueryExecutor, databaseTypeProvider, null);
    }

    StaticRecordReadProjectionService(StaticModuleDefinitionCatalog staticModuleDefinitionCatalog,
                                      RelationProjectionQueryExecutor projectionQueryExecutor,
                                      RelationProjectionDatabaseTypeProvider databaseTypeProvider,
                                      OptionSourceRegistry optionSourceRegistry) {
        this(staticModuleDefinitionCatalog,
                new RelationProjectionReadService(projectionQueryExecutor, databaseTypeProvider),
                optionSourceRegistry);
    }

    private StaticRecordReadProjectionService(StaticModuleDefinitionCatalog staticModuleDefinitionCatalog,
                                              RelationProjectionReadService relationProjectionReadService,
                                              OptionSourceRegistry optionSourceRegistry) {
        this.staticModuleDefinitionCatalog = staticModuleDefinitionCatalog;
        this.relationProjectionReadService = relationProjectionReadService == null
                ? new RelationProjectionReadService()
                : relationProjectionReadService;
        this.optionSourceRegistry = optionSourceRegistry;
    }

    public <T> WebPageResponse<T> projectDefaultList(String moduleAlias,
                                                     WebPageResponse<T> response,
                                                     Object recordService) {
        RecordReadProjection projection = defaultListProjection(moduleAlias, recordService).orElse(null);
        if (projection == null) {
            return response;
        }
        return projectResponse(response, projection);
    }

    public boolean supportsDefaultListQuery(String moduleAlias, Object recordService) {
        StaticModuleDefinition definition = staticModuleDefinitionCatalog.find(moduleAlias).orElse(null);
        if (definition == null) {
            return false;
        }
        return defaultListProjection(moduleAlias, recordService)
                .filter(projection -> relationProjectionReadService.supportsListQuery(definition, projection))
                .isPresent();
    }

    public boolean hasModuleDefinition(String moduleAlias) {
        if (moduleAlias == null) {
            return false;
        }
        Optional<StaticModuleDefinition> definition = staticModuleDefinitionCatalog.find(moduleAlias);
        return definition != null && definition.isPresent();
    }

    public Criteria queryCriteria(String moduleAlias, Object recordService, QueryRequest request) {
        QueryDescriptor descriptor = projectionAwareQueryDescriptor(moduleAlias, recordService);
        return new QueryCompiler(descriptor).criteria(request);
    }

    public Sort[] querySorts(String moduleAlias, Object recordService, QueryRequest request) {
        QueryDescriptor descriptor = projectionAwareQueryDescriptor(moduleAlias, recordService);
        return new QueryCompiler(descriptor).sorts(request);
    }

    public QuerySchema querySchema(String moduleAlias, Object recordService) {
        return QuerySchema.from(projectionAwareQueryDescriptor(moduleAlias, recordService), modelClass(recordService));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> Optional<WebPageResponse<T>> queryDefaultList(String moduleAlias,
                                                            Criteria criteria,
                                                            PageRequest pageRequest,
                                                            Object recordService,
                                                            Sort... sorts) {
        StaticModuleDefinition definition = staticModuleDefinitionCatalog.find(moduleAlias).orElse(null);
        if (definition == null) {
            return Optional.empty();
        }
        ModuleUiCompilationResult compilation = ModuleUiDescriptorCompiler.compileModule(definition);
        if (compilation == null || compilation.uiDescriptor() == null || compilation.readModel() == null) {
            return Optional.empty();
        }
        RecordReadProjection projection = RecordReadProjectionPlanner.defaultList(
                compilation.uiDescriptor(),
                compilation.readModel(),
                recordService,
                ActionExecutionContextHolder.current().orElse(null)
        );
        PageResult<Map<String, Object>> page = relationProjectionReadService.queryList(
                staticModuleDefinitionCatalog.definitions(),
                definition,
                projection,
                criteria,
                pageRequest,
                sorts
        ).orElse(null);
        if (page == null) {
            return Optional.empty();
        }
        List<Map<String, Object>> records = RecordReadProjectionOptionTitleProjector.project(
                modelClass(recordService),
                projection,
                page.getRecords(),
                optionSourceRegistry
        );
        WebPageResponse response = new WebPageResponse(
                records,
                page.getTotal(),
                page.getPageNum(),
                page.getPageSize(),
                page.getPages(),
                page.isTotalKnown(),
                null
        );
        return Optional.of(response);
    }

    private Class<?> modelClass(Object recordService) {
        if (recordService instanceof CrudAbility<?> crudAbility) {
            return crudAbility.modelClass();
        }
        return null;
    }

    private QueryDescriptor projectionAwareQueryDescriptor(String moduleAlias, Object recordService) {
        QueryDescriptor base = recordService instanceof QueryAbility<?> queryAbility
                ? queryAbility.queryDescriptor()
                : QueryDescriptor.builder(moduleAlias).build();
        QueryDescriptor.Builder builder = QueryDescriptor.builder(base.scopeName());
        base.fields().forEach(builder::field);
        for (String key : base.externalCriteriaKeys()) {
            builder.externalCriteria(key, base.externalCriteriaResolver(key));
        }
        for (Sort sort : base.defaultSorts()) {
            builder.defaultSort(sort);
        }
        staticModuleDefinitionCatalog.find(moduleAlias)
                .stream()
                .flatMap(definition -> definition.readProjections().stream())
                .filter(projection -> projection.filterable() || projection.sortable())
                .forEach(projection -> builder.field(queryField(recordService, projection)));
        return builder.build();
    }

    private QueryField queryField(Object recordService, StaticModuleReadProjectionDefinition projection) {
        QueryField field = projection.projectionType() == ModuleReadProjection.ProjectionType.EXISTS
                ? QueryField.of(projection.outputField(), QueryValueType.BOOLEAN, QueryOperator.EQ)
                : QueryDescriptors.field(modelClass(recordService), projection.outputField());
        if (!projection.filterable()) {
            return new QueryField(
                    field.fieldName(),
                    field.title(),
                    field.valueType(),
                    Set.of(),
                    null,
                    projection.sortable(),
                    false,
                    field.optionBinding(),
                    field.selectionMode(),
                    field.optionTitleField()
            );
        }
        if (projection.sortable()) {
            field = field.withSortable();
        }
        return field;
    }

    private Optional<RecordReadProjection> defaultListProjection(String moduleAlias, Object recordService) {
        if (moduleAlias == null || moduleAlias.isBlank()) {
            return Optional.empty();
        }
        return staticModuleDefinitionCatalog.find(moduleAlias)
                .map(ModuleUiDescriptorCompiler::compileModule)
                .filter(compilation -> compilation.uiDescriptor() != null && compilation.readModel() != null)
                .map(compilation -> RecordReadProjectionPlanner.defaultList(
                        compilation.uiDescriptor(),
                        compilation.readModel(),
                        recordService,
                        ActionExecutionContextHolder.current().orElse(null)
                ));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> WebPageResponse<T> projectResponse(WebPageResponse<T> response,
                                                   RecordReadProjection projection) {
        List<Map<String, Object>> records = RecordReadProjectionProjector.project(response.records(), projection);
        return new WebPageResponse(
                records,
                response.total(),
                response.pageNum(),
                response.pageSize(),
                response.pages(),
                response.totalKnown(),
                response.navigation()
        );
    }
}
