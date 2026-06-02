package net.ximatai.muyun.spring.boot.dynamic;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.OptimisticLockException;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicModuleDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicActionDescriptor;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinitionException;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionException;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionRequest;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionAvailability;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicQueryCondition;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicReferenceMatchMode;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicReferenceResolveMode;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicReferenceResolveRequest;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicReferenceResolveResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/{moduleAlias:[a-z][a-z0-9_]*(?:\\.[a-z][a-z0-9_]*)+}")
public class DynamicRecordWebController {
    private static final int MAX_PAGE_SIZE = 500;
    private static final Set<String> INTERNAL_RESULT_ACTIONS = Set.of("queryCriteria", "enabledCriteria");
    private static final Set<String> RESERVED_ACTION_PATHS = Set.of(
            "actions", "delete", "describe", "entities", "insert", "query", "references", "update", "view"
    );
    private final DynamicRecordService recordService;

    public DynamicRecordWebController(DynamicRecordService recordService) {
        this.recordService = recordService;
    }

    @PostMapping("/describe")
    public DynamicModuleDescriptor describeModule(@PathVariable String moduleAlias) {
        return recordService.describe(moduleAlias);
    }

    @PostMapping("/query")
    public DynamicPageResponse queryMainEntity(@PathVariable String moduleAlias,
                                               @RequestBody(required = false) DynamicQueryRequest request) {
        return pageRecords(moduleAlias, mainEntityAlias(moduleAlias), request);
    }

    @PostMapping("/view/{recordId}")
    public DynamicRecordResponse selectMainEntity(@PathVariable String moduleAlias,
                                                  @PathVariable String recordId) {
        return selectRecord(moduleAlias, mainEntityAlias(moduleAlias), recordId);
    }

    @PostMapping("/insert")
    @ResponseStatus(HttpStatus.CREATED)
    public RecordIdResponse createMainEntity(@PathVariable String moduleAlias,
                                             @RequestBody(required = false) DynamicRecordPayload payload) {
        String entityAlias = mainEntityAlias(moduleAlias);
        return createRecord(moduleAlias, entityAlias, payload);
    }

    @PostMapping("/update/{recordId}")
    public CountResponse updateMainEntity(@PathVariable String moduleAlias,
                                          @PathVariable String recordId,
                                          @RequestBody(required = false) DynamicRecordPayload payload) {
        return updateRecord(moduleAlias, mainEntityAlias(moduleAlias), recordId, payload);
    }

    @PostMapping("/delete/{recordId}")
    public CountResponse deleteMainEntity(@PathVariable String moduleAlias,
                                          @PathVariable String recordId) {
        return deleteRecord(moduleAlias, mainEntityAlias(moduleAlias), recordId);
    }

    @PostMapping("/actions")
    public List<DynamicActionDescriptor> mainEntityActions(@PathVariable String moduleAlias) {
        return recordService.actions(moduleAlias);
    }

    @PostMapping("/actions/{recordId}")
    public List<DynamicWebActionAvailabilityResponse> mainEntityRecordActions(@PathVariable String moduleAlias,
                                                                              @PathVariable String recordId) {
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

    @PostMapping("/{actionCode}")
    public DynamicWebActionExecutionResponse executeListAction(@PathVariable String moduleAlias,
                                                               @PathVariable String actionCode,
                                                               @RequestBody(required = false) DynamicWebActionRequest request) {
        requireActionLevel(moduleAlias, actionCode, Set.of(EntityActionLevel.LIST, EntityActionLevel.ANY),
                "dynamic action does not support list path: ");
        return executeAction(moduleAlias, actionCode, null, request);
    }

    @PostMapping("/{actionCode}/batch")
    public DynamicWebActionExecutionResponse executeBatchAction(@PathVariable String moduleAlias,
                                                                @PathVariable String actionCode,
                                                                @RequestBody(required = false) DynamicWebActionRequest request) {
        DynamicWebActionRequest normalized = request == null ? DynamicWebActionRequest.empty() : request;
        if (normalized.ids().isEmpty()) {
            throw new IllegalArgumentException("batch action requires ids");
        }
        requireActionLevel(moduleAlias, actionCode, Set.of(EntityActionLevel.BATCH, EntityActionLevel.ANY),
                "dynamic action does not support batch path: ");
        return executeAction(moduleAlias, actionCode, null, normalized);
    }

    @PostMapping("/{actionCode}/{recordId}")
    public DynamicWebActionExecutionResponse executeRecordAction(@PathVariable String moduleAlias,
                                                                 @PathVariable String actionCode,
                                                                 @PathVariable String recordId,
                                                                 @RequestBody(required = false) DynamicWebActionRequest request) {
        requireActionLevel(moduleAlias, actionCode, Set.of(EntityActionLevel.RECORD, EntityActionLevel.ANY),
                "dynamic action does not support record path: ");
        return executeAction(moduleAlias, actionCode, recordId, request);
    }

    private DynamicWebActionExecutionResponse executeAction(String moduleAlias,
                                                            String actionCode,
                                                            String pathRecordId,
                                                            DynamicWebActionRequest request) {
        String entityAlias = mainEntityAlias(moduleAlias);
        return DynamicWebActionExecutionResponse.from(recordService.executeAction(
                moduleAlias, actionCode, actionRequest(moduleAlias, entityAlias, pathRecordId, request)));
    }

    @PostMapping("/references/{fieldName}/resolve")
    public DynamicReferenceResolveResponse resolveReference(@PathVariable String moduleAlias,
                                                            @PathVariable String fieldName,
                                                            @RequestBody(required = false) DynamicWebReferenceRequest request) {
        String entityAlias = mainEntityAlias(moduleAlias);
        DynamicWebReferenceRequest normalized = request == null ? DynamicWebReferenceRequest.empty() : request;
        return recordService.resolveFieldReference(moduleAlias, entityAlias, fieldName, new DynamicReferenceResolveRequest(
                normalized.mode(),
                normalized.matchMode(),
                normalized.fuzzy(),
                normalized.values(),
                criteria(moduleAlias, entityAlias, normalized.conditions()),
                page(normalized.page()),
                normalized.includeProjections()
        ));
    }

    @ExceptionHandler({IllegalArgumentException.class, ModuleDefinitionException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public DynamicWebError handleBadRequest(RuntimeException exception) {
        return DynamicWebError.badRequest(exception.getMessage());
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

    private RecordIdResponse createRecord(String moduleAlias, String entityAlias, DynamicRecordPayload payload) {
        return new RecordIdResponse(recordService.create(moduleAlias, entityAlias,
                record(moduleAlias, entityAlias, payload)));
    }

    private DynamicRecordResponse selectRecord(String moduleAlias, String entityAlias, String recordId) {
        return DynamicRecordResponse.from(recordService.select(moduleAlias, entityAlias, recordId));
    }

    private CountResponse updateRecord(String moduleAlias,
                                       String entityAlias,
                                       String recordId,
                                       DynamicRecordPayload payload) {
        DynamicRecord record = record(moduleAlias, entityAlias, payload);
        record.setId(recordId);
        return new CountResponse(recordService.update(moduleAlias, entityAlias, record));
    }

    private CountResponse deleteRecord(String moduleAlias, String entityAlias, String recordId) {
        return new CountResponse(recordService.delete(moduleAlias, entityAlias, recordId));
    }

    private DynamicPageResponse pageRecords(String moduleAlias, String entityAlias, DynamicQueryRequest request) {
        DynamicQueryRequest normalized = request == null ? DynamicQueryRequest.empty() : request;
        return DynamicPageResponse.from(recordService.page(moduleAlias, entityAlias,
                criteria(moduleAlias, entityAlias, normalized.conditions()),
                page(normalized.page()),
                sorts(normalized.sorts())));
    }

    private Criteria criteria(String moduleAlias, String entityAlias, Collection<DynamicWebQueryCondition> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return Criteria.of();
        }
        return recordService.queryCriteria(moduleAlias, entityAlias, conditions.stream()
                .map(condition -> new DynamicQueryCondition(
                        condition.fieldName(),
                        condition.operator(),
                        condition.values()
                ))
                .toList());
    }

    private PageRequest page(DynamicWebPageRequest request) {
        DynamicWebPageRequest normalized = request == null ? DynamicWebPageRequest.DEFAULT : request;
        if (normalized.pageSize() > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("dynamic pageSize must not exceed " + MAX_PAGE_SIZE);
        }
        return PageRequest.of(normalized.pageNum(), normalized.pageSize());
    }

    private Sort[] sorts(List<DynamicWebSort> sorts) {
        if (sorts == null || sorts.isEmpty()) {
            return new Sort[0];
        }
        return sorts.stream()
                .map(sort -> sort.desc() ? Sort.desc(sort.field()) : Sort.asc(sort.field()))
                .toArray(Sort[]::new);
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
                .withQueryConditions(queryConditions(normalized.conditions()))
                .withPayload(normalized.payload());
        if (normalized.record() != null && entityAlias != null) {
            actionRequest = actionRequest.withRecord(actionRecord(moduleAlias, entityAlias, recordId, normalized.record()));
        }
        if (!normalized.conditions().isEmpty() && entityAlias != null) {
            actionRequest = actionRequest.withCriteria(criteria(moduleAlias, entityAlias, normalized.conditions()));
        }
        if (normalized.page() != null) {
            actionRequest = actionRequest.withPageRequest(page(normalized.page()));
        }
        if (!normalized.sorts().isEmpty()) {
            actionRequest = actionRequest.withSorts(List.of(sorts(normalized.sorts())));
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
        if (RESERVED_ACTION_PATHS.contains(actionCode)) {
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

    private List<DynamicQueryCondition> queryConditions(Collection<DynamicWebQueryCondition> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return List.of();
        }
        return conditions.stream()
                .map(condition -> new DynamicQueryCondition(
                        condition.fieldName(),
                        condition.operator(),
                        condition.values()
                ))
                .toList();
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
