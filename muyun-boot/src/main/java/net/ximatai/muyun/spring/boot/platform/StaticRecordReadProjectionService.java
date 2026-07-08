package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
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
    private final RelationProjectionReadService relationProjectionReadService;

    public StaticRecordReadProjectionService(StaticModuleDefinitionCatalog staticModuleDefinitionCatalog) {
        this(staticModuleDefinitionCatalog, (RelationProjectionReadService) null);
    }

    @Autowired
    public StaticRecordReadProjectionService(StaticModuleDefinitionCatalog staticModuleDefinitionCatalog,
                                             ObjectProvider<RelationProjectionReadService> relationProjectionReadService) {
        this(staticModuleDefinitionCatalog,
                relationProjectionReadService == null ? null : relationProjectionReadService.getIfAvailable());
    }

    StaticRecordReadProjectionService(StaticModuleDefinitionCatalog staticModuleDefinitionCatalog,
                                      RelationProjectionQueryExecutor projectionQueryExecutor,
                                      RelationProjectionDatabaseTypeProvider databaseTypeProvider) {
        this(staticModuleDefinitionCatalog,
                new RelationProjectionReadService(projectionQueryExecutor, databaseTypeProvider));
    }

    private StaticRecordReadProjectionService(StaticModuleDefinitionCatalog staticModuleDefinitionCatalog,
                                              RelationProjectionReadService relationProjectionReadService) {
        this.staticModuleDefinitionCatalog = staticModuleDefinitionCatalog;
        this.relationProjectionReadService = relationProjectionReadService == null
                ? new RelationProjectionReadService()
                : relationProjectionReadService;
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
                definition,
                projection,
                criteria,
                pageRequest,
                sorts
        ).orElse(null);
        if (page == null) {
            return Optional.empty();
        }
        WebPageResponse response = WebPageResponse.from(page);
        return Optional.of(response);
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
