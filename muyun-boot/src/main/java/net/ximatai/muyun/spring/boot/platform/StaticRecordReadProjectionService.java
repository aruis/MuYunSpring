package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.boot.web.WebPageResponse;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContextHolder;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class StaticRecordReadProjectionService {
    private final StaticModuleDefinitionCatalog staticModuleDefinitionCatalog;

    public StaticRecordReadProjectionService(StaticModuleDefinitionCatalog staticModuleDefinitionCatalog) {
        this.staticModuleDefinitionCatalog = staticModuleDefinitionCatalog;
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
