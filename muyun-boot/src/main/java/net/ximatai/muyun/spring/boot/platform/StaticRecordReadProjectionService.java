package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.CriteriaClause;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.CriteriaGroup;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.web.WebPageResponse;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContextHolder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class StaticRecordReadProjectionService {
    private final StaticModuleDefinitionCatalog staticModuleDefinitionCatalog;
    private final RelationProjectionQueryExecutor projectionQueryExecutor;
    private final RelationProjectionDatabaseTypeProvider databaseTypeProvider;

    public StaticRecordReadProjectionService(StaticModuleDefinitionCatalog staticModuleDefinitionCatalog) {
        this(staticModuleDefinitionCatalog, (RelationProjectionQueryExecutor) null, null);
    }

    @Autowired
    public StaticRecordReadProjectionService(StaticModuleDefinitionCatalog staticModuleDefinitionCatalog,
                                             ObjectProvider<RelationProjectionQueryExecutor> projectionQueryExecutor,
                                             ObjectProvider<RelationProjectionDatabaseTypeProvider> databaseTypeProvider) {
        this(staticModuleDefinitionCatalog,
                projectionQueryExecutor == null ? null : projectionQueryExecutor.getIfAvailable(),
                databaseTypeProvider == null ? null : databaseTypeProvider.getIfAvailable());
    }

    StaticRecordReadProjectionService(StaticModuleDefinitionCatalog staticModuleDefinitionCatalog,
                                      RelationProjectionQueryExecutor projectionQueryExecutor,
                                      RelationProjectionDatabaseTypeProvider databaseTypeProvider) {
        this.staticModuleDefinitionCatalog = staticModuleDefinitionCatalog;
        this.projectionQueryExecutor = projectionQueryExecutor;
        this.databaseTypeProvider = databaseTypeProvider == null
                ? new RelationProjectionDatabaseTypeProvider()
                : databaseTypeProvider;
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
        if (projectionQueryExecutor == null) {
            return false;
        }
        StaticModuleDefinition definition = staticModuleDefinitionCatalog.find(moduleAlias).orElse(null);
        if (definition == null || definition.projectionJoins().isEmpty()) {
            return false;
        }
        return defaultListProjection(moduleAlias, recordService)
                .filter(projection -> projection.postReadTransforms().isEmpty())
                .isPresent();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> Optional<WebPageResponse<T>> queryDefaultList(String moduleAlias,
                                                            Criteria criteria,
                                                            PageRequest pageRequest,
                                                            Object recordService,
                                                            Sort... sorts) {
        if (projectionQueryExecutor == null) {
            return Optional.empty();
        }
        StaticModuleDefinition definition = staticModuleDefinitionCatalog.find(moduleAlias).orElse(null);
        if (definition == null || definition.projectionJoins().isEmpty()) {
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
        if (!projection.postReadTransforms().isEmpty()) {
            return Optional.empty();
        }
        RelationProjectionSqlPlan plan = RelationProjectionQueryPlanner.plan(
                definition,
                projection,
                databaseTypeProvider.databaseType(),
                requiredMainFields(criteria, sorts)
        );
        if (!plan.hasRelationProjection()) {
            return Optional.empty();
        }
        PageResult<Map<String, Object>> page = projectionQueryExecutor.page(plan, criteria, pageRequest, sorts);
        WebPageResponse response = WebPageResponse.from(page);
        return Optional.of(response);
    }

    private java.util.Set<String> requiredMainFields(Criteria criteria, Sort... sorts) {
        java.util.LinkedHashSet<String> fields = new java.util.LinkedHashSet<>();
        if (criteria != null) {
            collectCriteriaFields(criteria.getRoot(), fields);
        }
        if (sorts != null) {
            java.util.Arrays.stream(sorts)
                    .filter(java.util.Objects::nonNull)
                    .map(Sort::getField)
                    .filter(field -> field != null && !field.isBlank())
                    .forEach(fields::add);
        }
        return java.util.Set.copyOf(fields);
    }

    private void collectCriteriaFields(CriteriaGroup group, java.util.Set<String> fields) {
        if (group == null) {
            return;
        }
        for (CriteriaGroup.Entry entry : group.getEntries()) {
            Object node = entry.getNode();
            if (node instanceof CriteriaClause clause) {
                String field = clause.getField();
                if (field != null && !field.isBlank()) {
                    fields.add(field);
                }
            } else if (node instanceof CriteriaGroup childGroup) {
                collectCriteriaFields(childGroup, fields);
            }
        }
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
