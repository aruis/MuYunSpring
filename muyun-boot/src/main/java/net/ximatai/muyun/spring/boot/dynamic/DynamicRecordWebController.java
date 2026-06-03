package net.ximatai.muyun.spring.boot.dynamic;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.OptimisticLockException;
import net.ximatai.muyun.spring.boot.web.ActionWeb;
import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.ReferenceWeb;
import net.ximatai.muyun.spring.boot.web.TreeSortWebRequest;
import net.ximatai.muyun.spring.boot.web.TreeWeb;
import net.ximatai.muyun.spring.boot.web.WebCountResponse;
import net.ximatai.muyun.spring.boot.web.WebQueryCondition;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.common.web.PlatformWebPathRules;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicModuleDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicActionDescriptor;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;
import net.ximatai.muyun.spring.dynamic.metadata.EntityCapability;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinitionException;
import net.ximatai.muyun.spring.dynamic.openapi.DynamicOpenApiDocument;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionException;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionRequest;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionAvailability;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicEntityOperations;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicQueryCondition;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicReferenceMatchMode;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicReferenceResolveMode;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicReferenceResolveRequest;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicReferenceResolveResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@RestController
@RequestMapping("/{moduleAlias:[a-z][a-z0-9_]*(?:\\.[a-z][a-z0-9_]*)+}")
public class DynamicRecordWebController implements
        CrudWeb<DynamicRecord, DynamicEntityOperations>,
        EnableWeb<DynamicRecord, DynamicEntityOperations>,
        TreeWeb<DynamicRecord, DynamicEntityOperations>,
        ActionWeb<DynamicEntityOperations,
                DynamicWebActionRequest,
                DynamicActionDescriptor,
                DynamicWebActionAvailabilityResponse,
                DynamicWebActionExecutionResponse>,
        ReferenceWeb<DynamicEntityOperations,
                DynamicWebReferenceRequest,
                DynamicReferenceResolveResponse> {
    private static final Set<String> INTERNAL_RESULT_ACTIONS = Set.of("queryCriteria", "enabledCriteria");
    private final DynamicRecordService recordService;
    private final ActiveTenantVerifier activeTenantVerifier;

    public DynamicRecordWebController(DynamicRecordService recordService,
                                      ActiveTenantVerifier activeTenantVerifier) {
        this.recordService = recordService;
        this.activeTenantVerifier = activeTenantVerifier;
    }

    @Override
    public DynamicEntityOperations service() {
        return recordService.mainEntity(DynamicWebRequest.moduleAlias());
    }

    @Override
    public <T> T webScope(Supplier<T> action) {
        return tenantScope(DynamicWebRequest.moduleAlias(), action);
    }

    @Override
    public Criteria queryCriteria(WebQueryRequest request) {
        if (request == null || request.conditions().isEmpty()) {
            return Criteria.of();
        }
        return service().queryCriteria(DynamicWebQueryMapper.queryConditions(request.conditions()));
    }

    @Override
    public Sort[] querySorts(WebQueryRequest request) {
        if (request == null || request.sorts().isEmpty()) {
            return new Sort[0];
        }
        return DynamicWebQueryMapper.sorts(request.sorts());
    }

    @Override
    @PostMapping("/sort/{id}")
    public WebCountResponse sort(@PathVariable String id,
                                 @RequestBody(required = false) TreeSortWebRequest request) {
        return webScope(() -> {
            TreeSortWebRequest normalized = request == null ? new TreeSortWebRequest(null, null, null) : request;
            DynamicEntityOperations operations = service();
            Set<String> capabilities = operations.describe().capabilities();
            if (capabilities.contains(EntityCapability.TREE.name())) {
                requireSortInput(normalized);
                operations.moveInTree(id, normalized.previousId(), normalized.nextId(), normalized.parentId());
                return new WebCountResponse(1);
            }
            if (!capabilities.contains(EntityCapability.SORT.name())) {
                throw new PlatformException("dynamic entity does not support capability: SORT");
            }
            if (normalized.parentId() != null && !normalized.parentId().isBlank()) {
                throw new IllegalArgumentException("sort parentId requires TREE capability");
            }
            if (normalized.previousId() != null && !normalized.previousId().isBlank()) {
                operations.moveAfter(id, normalized.previousId());
                return new WebCountResponse(1);
            }
            if (normalized.nextId() != null && !normalized.nextId().isBlank()) {
                operations.moveBefore(id, normalized.nextId());
                return new WebCountResponse(1);
            }
            throw new IllegalArgumentException("sort requires previousId or nextId");
        });
    }

    @GetMapping("/describe")
    public DynamicModuleDescriptor describeModule(@PathVariable String moduleAlias) {
        return tenantScope(moduleAlias, () -> recordService.describe(moduleAlias));
    }

    @GetMapping("/openapi")
    public DynamicOpenApiDocument openApi(@PathVariable String moduleAlias) {
        return tenantScope(moduleAlias, () -> recordService.openApi(moduleAlias));
    }

    @Override
    public List<DynamicActionDescriptor> listActions() {
        return recordService.actions(DynamicWebRequest.moduleAlias());
    }

    @Override
    public List<DynamicWebActionAvailabilityResponse> listRecordActions(String recordId) {
        String moduleAlias = DynamicWebRequest.moduleAlias();
        String entityAlias = mainEntityAlias(moduleAlias);
        DynamicRecord record = recordService.select(moduleAlias, entityAlias, recordId);
        if (record == null) {
            throw new IllegalArgumentException("dynamic record does not exist: " + recordId);
        }
        return recordService.actions(moduleAlias).stream()
                .filter(DynamicRecordWebController::isRecordAction)
                .map(action -> DynamicWebActionAvailabilityResponse.from(action,
                        recordService.actionAvailability(moduleAlias, action.code(), record)))
                .toList();
    }

    @Override
    public DynamicWebActionExecutionResponse executeListAction(String actionCode, DynamicWebActionRequest request) {
        String moduleAlias = DynamicWebRequest.moduleAlias();
        requireActionLevel(moduleAlias, actionCode, Set.of(EntityActionLevel.LIST, EntityActionLevel.ANY),
                "dynamic action does not support list path: ");
        return executeAction(moduleAlias, actionCode, null, request);
    }

    @Override
    public DynamicWebActionExecutionResponse executeBatchAction(String actionCode, DynamicWebActionRequest request) {
        String moduleAlias = DynamicWebRequest.moduleAlias();
        DynamicWebActionRequest normalized = request == null ? DynamicWebActionRequest.empty() : request;
        if (normalized.ids().isEmpty()) {
            throw new IllegalArgumentException("batch action requires ids");
        }
        requireActionLevel(moduleAlias, actionCode, Set.of(EntityActionLevel.BATCH, EntityActionLevel.ANY),
                "dynamic action does not support batch path: ");
        return executeAction(moduleAlias, actionCode, null, normalized);
    }

    @Override
    public DynamicWebActionExecutionResponse executeRecordAction(String actionCode,
                                                                 String recordId,
                                                                 DynamicWebActionRequest request) {
        String moduleAlias = DynamicWebRequest.moduleAlias();
        requireActionLevel(moduleAlias, actionCode, Set.of(EntityActionLevel.RECORD, EntityActionLevel.ANY),
                "dynamic action does not support record path: ");
        return executeAction(moduleAlias, actionCode, recordId, request);
    }

    @Override
    @PostMapping("/references/{fieldName}/resolve")
    public DynamicReferenceResolveResponse reference(@PathVariable String fieldName,
                                                     @RequestBody(required = false) DynamicWebReferenceRequest request) {
        return ReferenceWeb.super.reference(fieldName, request);
    }

    private DynamicWebActionExecutionResponse executeAction(String moduleAlias,
                                                            String actionCode,
                                                            String pathRecordId,
                                                            DynamicWebActionRequest request) {
        String entityAlias = mainEntityAlias(moduleAlias);
        return DynamicWebActionExecutionResponse.from(recordService.executeAction(
                moduleAlias, actionCode, actionRequest(moduleAlias, entityAlias, pathRecordId, request)));
    }

    @Override
    public DynamicReferenceResolveResponse resolveReference(String fieldName, DynamicWebReferenceRequest request) {
        String moduleAlias = DynamicWebRequest.moduleAlias();
        String entityAlias = mainEntityAlias(moduleAlias);
        DynamicWebReferenceRequest normalized = request == null ? DynamicWebReferenceRequest.empty() : request;
        return recordService.resolveFieldReference(moduleAlias, entityAlias, fieldName, new DynamicReferenceResolveRequest(
                normalized.mode(),
                normalized.matchMode(),
                normalized.fuzzy(),
                normalized.values(),
                criteria(moduleAlias, entityAlias, normalized.conditions()),
                DynamicWebQueryMapper.page(normalized.page()),
                normalized.includeProjections()
        ));
    }

    @ExceptionHandler({IllegalArgumentException.class, ModuleDefinitionException.class, PlatformException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public DynamicWebError handleBadRequest(RuntimeException exception) {
        return DynamicWebError.badRequest(exception.getMessage());
    }

    @ExceptionHandler(HttpMessageConversionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public DynamicWebError handleMessageConversion(HttpMessageConversionException exception) {
        return DynamicWebError.badRequest(rootMessage(exception));
    }

    @ExceptionHandler(DynamicActionExecutionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public DynamicWebActionError handleActionFailure(DynamicActionExecutionException exception) {
        return DynamicWebActionError.from(exception);
    }

    @ExceptionHandler(OptimisticLockException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public DynamicWebError handleOptimisticLock(OptimisticLockException exception) {
        return DynamicWebError.conflict(exception.getMessage());
    }

    private DynamicRecord record(String moduleAlias, String entityAlias, DynamicRecordPayload payload) {
        DynamicRecord record = recordService.newRecord(moduleAlias, entityAlias);
        DynamicRecordPayload normalized = payload == null ? DynamicRecordPayload.empty() : payload;
        if (normalized.id() != null && !normalized.id().isBlank()) {
            record.setId(normalized.id());
        }
        if (normalized.version() != null) {
            record.setVersion(normalized.version());
        }
        normalized.values().forEach(record::setValue);
        return record;
    }

    private String mainEntityAlias(String moduleAlias) {
        return recordService.mainEntityAlias(moduleAlias);
    }

    private void requireSortInput(TreeSortWebRequest request) {
        if ((request.previousId() == null || request.previousId().isBlank())
                && (request.nextId() == null || request.nextId().isBlank())
                && (request.parentId() == null || request.parentId().isBlank())) {
            throw new IllegalArgumentException("tree sort requires previousId, nextId, or parentId");
        }
    }

    private <T> T tenantScope(String moduleAlias, Supplier<T> action) {
        String tenantId = TenantContext.currentTenantId()
                .orElseThrow(() -> new PlatformException(moduleAlias + " requires tenant context"));
        activeTenantVerifier.verifyActiveTenant(tenantId);
        return action.get();
    }

    private Criteria criteria(String moduleAlias, String entityAlias, List<WebQueryCondition> conditions) {
        List<DynamicQueryCondition> queryConditions = DynamicWebQueryMapper.queryConditions(conditions);
        if (queryConditions.isEmpty()) {
            return Criteria.of();
        }
        return recordService.queryCriteria(moduleAlias, entityAlias, queryConditions);
    }

    private DynamicActionExecutionRequest actionRequest(String moduleAlias,
                                                        String entityAlias,
                                                        String pathRecordId,
                                                        DynamicWebActionRequest request) {
        DynamicWebActionRequest normalized = request == null ? DynamicWebActionRequest.empty() : request;
        String recordId = resolveActionRecordId(pathRecordId, normalized.recordId());
        DynamicActionExecutionRequest actionRequest = DynamicActionExecutionRequest.empty()
                .withRecordId(recordId)
                .withIds(normalized.ids())
                .withOrderedIds(normalized.orderedIds())
                .withBeforeId(normalized.beforeId())
                .withAfterId(normalized.afterId())
                .withParentId(normalized.parentId())
                .withFieldNames(normalized.fieldNames())
                .withQueryConditions(DynamicWebQueryMapper.queryConditions(normalized.conditions()))
                .withPayload(normalized.payload());
        if (normalized.record() != null && entityAlias != null) {
            actionRequest = actionRequest.withRecord(actionRecord(moduleAlias, entityAlias, recordId, normalized.record()));
        }
        if (!normalized.conditions().isEmpty() && entityAlias != null) {
            actionRequest = actionRequest.withCriteria(criteria(moduleAlias, entityAlias, normalized.conditions()));
        }
        if (normalized.page() != null) {
            actionRequest = actionRequest.withPageRequest(DynamicWebQueryMapper.page(normalized.page()));
        }
        if (!normalized.sorts().isEmpty()) {
            actionRequest = actionRequest.withSorts(List.of(DynamicWebQueryMapper.sorts(normalized.sorts())));
        }
        return actionRequest;
    }

    private void requireActionLevel(String moduleAlias,
                                    String actionCode,
                                    Set<EntityActionLevel> allowed,
                                    String messagePrefix) {
        rejectInternalResultAction(actionCode);
        rejectReservedActionPath(actionCode);
        EntityActionLevel level = recordService.action(moduleAlias, actionCode).actionLevel();
        if (!allowed.contains(level)) {
            throw new IllegalArgumentException(messagePrefix + actionCode);
        }
    }

    private void rejectReservedActionPath(String actionCode) {
        if (PlatformWebPathRules.isReservedWebActionCode(actionCode)) {
            throw new IllegalArgumentException("dynamic action path is reserved: " + actionCode);
        }
    }

    private String resolveActionRecordId(String pathRecordId, String bodyRecordId) {
        if (pathRecordId == null || pathRecordId.isBlank()) {
            return bodyRecordId;
        }
        if (bodyRecordId != null && !bodyRecordId.isBlank() && !pathRecordId.equals(bodyRecordId)) {
            throw new IllegalArgumentException("action path recordId must match request recordId");
        }
        return pathRecordId;
    }

    private DynamicRecord actionRecord(String moduleAlias,
                                       String entityAlias,
                                       String recordId,
                                       DynamicRecordPayload payload) {
        DynamicRecord record = record(moduleAlias, entityAlias, payload);
        if (recordId == null || recordId.isBlank()) {
            return record;
        }
        if (record.getId() != null && !record.getId().isBlank() && !recordId.equals(record.getId())) {
            throw new IllegalArgumentException("action request recordId must match record.id");
        }
        record.setId(recordId);
        return record;
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage();
    }

    private void rejectInternalResultAction(String actionCode) {
        if (INTERNAL_RESULT_ACTIONS.contains(actionCode)) {
            throw new IllegalArgumentException("dynamic web action is not exposed: " + actionCode);
        }
    }

    private static boolean isRecordAction(DynamicActionDescriptor action) {
        return action.actionLevel() == EntityActionLevel.RECORD || action.actionLevel() == EntityActionLevel.ANY;
    }
}
