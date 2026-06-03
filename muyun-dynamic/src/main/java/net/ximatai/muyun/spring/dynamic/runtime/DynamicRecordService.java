package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.TransactionScopeSupport;
import net.ximatai.muyun.spring.ability.event.RuntimeMutationSource;
import net.ximatai.muyun.spring.ability.reference.ReferenceOption;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicActionDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicAssociationViewDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicEntityDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicModuleDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicReferenceDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicRelationDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicViewDescriptor;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionExecutorType;
import net.ximatai.muyun.spring.dynamic.metadata.EntityViewType;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinitionException;
import net.ximatai.muyun.spring.dynamic.openapi.DynamicOpenApiDocument;
import net.ximatai.muyun.spring.dynamic.openapi.DynamicOpenApiGenerator;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class DynamicRecordService {
    private static final DynamicOpenApiGenerator OPEN_API_GENERATOR = new DynamicOpenApiGenerator();

    private final DynamicRecordRuntime runtime;
    private final DynamicRecordEventPublisher eventPublisher;

    public DynamicRecordService(DynamicRecordRuntime runtime) {
        this.runtime = Objects.requireNonNull(runtime, "runtime must not be null");
        this.eventPublisher = new DynamicRecordEventPublisher(runtime.eventPublisher());
    }

    public DynamicRecord newRecord(String moduleAlias, String entityAlias) {
        return runtime.newRecord(moduleAlias, entityAlias);
    }

    public DynamicModuleDescriptor describe(String moduleAlias) {
        return runtime.describe(moduleAlias);
    }

    public DynamicOpenApiDocument openApi(String moduleAlias) {
        return OPEN_API_GENERATOR.generate(describe(moduleAlias));
    }

    public String mainEntityAlias(String moduleAlias) {
        return runtime.registry().requireModule(moduleAlias).mainEntityAlias();
    }

    public ModuleOperations module(String moduleAlias) {
        return new ModuleOperations(this, moduleAlias);
    }

    public DynamicEntityOperations entity(String moduleAlias, String entityAlias) {
        return new DynamicEntityOperations(this, moduleAlias, entityAlias);
    }

    public DynamicEntityOperations mainEntity(String moduleAlias) {
        return entity(moduleAlias, mainEntityAlias(moduleAlias));
    }

    public DynamicEntityDescriptor entityDescriptor(String moduleAlias, String entityAlias) {
        return findEntity(describe(moduleAlias), entityAlias);
    }

    public List<DynamicActionDescriptor> actions(String moduleAlias) {
        return describe(moduleAlias).actions();
    }

    public DynamicActionDescriptor action(String moduleAlias, String actionCode) {
        return findAction(describe(moduleAlias), actionCode);
    }

    public DynamicActionAvailability actionAvailability(String moduleAlias, String actionCode, DynamicRecord record) {
        DynamicModuleDescriptor descriptor = describe(moduleAlias);
        findAction(descriptor, actionCode);
        return entityService(moduleAlias, runtime.registry().requireModule(moduleAlias).mainEntityAlias())
                .actionAvailability(actionCode, record);
    }

    public DynamicActionExecutionResult executeAction(String moduleAlias,
                                                      String actionCode,
                                                      DynamicActionExecutionRequest request) {
        DynamicModuleDescriptor module = describe(moduleAlias);
        DynamicActionDescriptor action = findAction(module, actionCode);
        String entityAlias = runtime.registry().requireModule(moduleAlias).mainEntityAlias();
        return executeAction(moduleAlias, entityAlias, action, request);
    }

    public List<DynamicActionDescriptor> actions(String moduleAlias, String entityAlias) {
        return entityDescriptor(moduleAlias, entityAlias).actions();
    }

    public DynamicActionDescriptor action(String moduleAlias, String entityAlias, String actionCode) {
        return findAction(moduleAlias, entityDescriptor(moduleAlias, entityAlias), actionCode);
    }

    public DynamicActionAvailability actionAvailability(String moduleAlias,
                                                        String entityAlias,
                                                        String actionCode,
                                                        DynamicRecord record) {
        findAction(moduleAlias, entityDescriptor(moduleAlias, entityAlias), actionCode);
        return entityService(moduleAlias, entityAlias).actionAvailability(actionCode, record);
    }

    public DynamicActionExecutionResult executeAction(String moduleAlias,
                                                      String entityAlias,
                                                      String actionCode,
                                                      DynamicActionExecutionRequest request) {
        DynamicActionDescriptor action = findAction(moduleAlias, entityDescriptor(moduleAlias, entityAlias), actionCode);
        return executeAction(moduleAlias, entityAlias, action, request);
    }

    public List<DynamicViewDescriptor> views(String moduleAlias, String entityAlias) {
        return entityDescriptor(moduleAlias, entityAlias).views();
    }

    public DynamicViewDescriptor view(String moduleAlias, String entityAlias, EntityViewType viewType) {
        return findView(moduleAlias, entityDescriptor(moduleAlias, entityAlias), viewType);
    }

    public List<DynamicAssociationViewDescriptor> associationViews(String moduleAlias) {
        return describe(moduleAlias).associationViews();
    }

    public List<DynamicAssociationViewDescriptor> associationViews(String moduleAlias, String entityAlias) {
        return entityDescriptor(moduleAlias, entityAlias).associationViews();
    }

    public DynamicAssociationViewDescriptor associationView(String moduleAlias, String entityAlias, String viewCode) {
        return findAssociationView(moduleAlias, entityDescriptor(moduleAlias, entityAlias), viewCode);
    }

    public List<DynamicRelationDescriptor> relations(String moduleAlias) {
        return describe(moduleAlias).relations();
    }

    public List<DynamicReferenceDescriptor> references(String moduleAlias) {
        return describe(moduleAlias).references();
    }

    public List<DynamicReferenceDescriptor> references(String moduleAlias, String entityAlias) {
        return describe(moduleAlias).references().stream()
                .filter(reference -> reference.sourceEntityAlias().equals(entityAlias))
                .toList();
    }

    public DynamicReferenceDescriptor reference(String moduleAlias, String entityAlias, String sourceField) {
        return references(moduleAlias, entityAlias).stream()
                .filter(reference -> reference.sourceField().equals(sourceField))
                .findFirst()
                .orElseThrow(() -> new ModuleDefinitionException("unknown dynamic reference: "
                        + moduleAlias + "." + entityAlias + "." + sourceField));
    }

    public String create(String moduleAlias, String entityAlias, DynamicRecord record) {
        return create(moduleAlias, entityAlias, record, RuntimeMutationSource.BUSINESS, null);
    }

    String createFromAction(String moduleAlias, String entityAlias, DynamicRecord record, String traceId) {
        return create(moduleAlias, entityAlias, record, RuntimeMutationSource.ACTION, traceId);
    }

    private String create(String moduleAlias, String entityAlias, DynamicRecord record, RuntimeMutationSource mutationSource, String traceId) {
        String id = entityService(moduleAlias, entityAlias).insert(record);
        eventPublisher.created(eventContext(moduleAlias, entityAlias, mutationSource, traceId), id);
        return id;
    }

    public DynamicRecord select(String moduleAlias, String entityAlias, String id) {
        return entityService(moduleAlias, entityAlias).select(id);
    }

    public DynamicRecord selectIgnoreSoftDelete(String moduleAlias, String entityAlias, String id) {
        return entityService(moduleAlias, entityAlias).selectIgnoreSoftDelete(id);
    }

    public int update(String moduleAlias, String entityAlias, DynamicRecord record) {
        return update(moduleAlias, entityAlias, record, RuntimeMutationSource.BUSINESS, null);
    }

    int updateFromAction(String moduleAlias, String entityAlias, DynamicRecord record, String traceId) {
        return update(moduleAlias, entityAlias, record, RuntimeMutationSource.ACTION, traceId);
    }

    private int update(String moduleAlias, String entityAlias, DynamicRecord record, RuntimeMutationSource mutationSource, String traceId) {
        int updated = entityService(moduleAlias, entityAlias).update(record);
        if (updated > 0) {
            eventPublisher.updated(eventContext(moduleAlias, entityAlias, mutationSource, traceId), record.getId());
        }
        return updated;
    }

    public int delete(String moduleAlias, String entityAlias, String id) {
        return delete(moduleAlias, entityAlias, id, RuntimeMutationSource.BUSINESS, null);
    }

    int deleteFromAction(String moduleAlias, String entityAlias, String id, String traceId) {
        return delete(moduleAlias, entityAlias, id, RuntimeMutationSource.ACTION, traceId);
    }

    private int delete(String moduleAlias, String entityAlias, String id, RuntimeMutationSource mutationSource, String traceId) {
        int deleted = entityService(moduleAlias, entityAlias).delete(id);
        if (deleted > 0) {
            eventPublisher.deleted(eventContext(moduleAlias, entityAlias, mutationSource, traceId), id);
        }
        return deleted;
    }

    public int deleteBatch(String moduleAlias, String entityAlias, Collection<String> ids) {
        return deleteBatch(moduleAlias, entityAlias, ids, RuntimeMutationSource.BUSINESS, null);
    }

    private int deleteBatch(String moduleAlias, String entityAlias, Collection<String> ids, RuntimeMutationSource mutationSource, String traceId) {
        int deleted = entityService(moduleAlias, entityAlias).deleteBatch(ids);
        if (deleted > 0) {
            eventPublisher.deletedBatch(eventContext(moduleAlias, entityAlias, mutationSource, traceId),
                    List.copyOf(ids), deleted);
        }
        return deleted;
    }

    public List<DynamicRecord> list(String moduleAlias, String entityAlias, Criteria criteria, PageRequest pageRequest, Sort... sorts) {
        return entityService(moduleAlias, entityAlias).list(criteria, pageRequest, sorts);
    }

    public PageResult<DynamicRecord> page(String moduleAlias, String entityAlias, Criteria criteria, PageRequest pageRequest, Sort... sorts) {
        return entityService(moduleAlias, entityAlias).pageQuery(criteria, pageRequest, sorts);
    }

    public long count(String moduleAlias, String entityAlias, Criteria criteria) {
        return entityService(moduleAlias, entityAlias).count(criteria);
    }

    public List<DynamicRecord> sortedList(String moduleAlias, String entityAlias, Criteria criteria) {
        return entityService(moduleAlias, entityAlias).sortedList(criteria);
    }

    public void reorder(String moduleAlias, String entityAlias, List<String> orderedIds) {
        reorder(moduleAlias, entityAlias, orderedIds, RuntimeMutationSource.BUSINESS, null);
    }

    void reorderFromAction(String moduleAlias, String entityAlias, List<String> orderedIds, String traceId) {
        reorder(moduleAlias, entityAlias, orderedIds, RuntimeMutationSource.ACTION, traceId);
    }

    private void reorder(String moduleAlias, String entityAlias, List<String> orderedIds, RuntimeMutationSource mutationSource, String traceId) {
        entityService(moduleAlias, entityAlias).reorder(orderedIds);
        eventPublisher.reordered(eventContext(moduleAlias, entityAlias, mutationSource, traceId), orderedIds);
    }

    public void moveBefore(String moduleAlias, String entityAlias, String id, String beforeId) {
        moveBefore(moduleAlias, entityAlias, id, beforeId, RuntimeMutationSource.BUSINESS, null);
    }

    void moveBeforeFromAction(String moduleAlias, String entityAlias, String id, String beforeId, String traceId) {
        moveBefore(moduleAlias, entityAlias, id, beforeId, RuntimeMutationSource.ACTION, traceId);
    }

    private void moveBefore(String moduleAlias, String entityAlias, String id, String beforeId, RuntimeMutationSource mutationSource, String traceId) {
        entityService(moduleAlias, entityAlias).moveBefore(id, beforeId);
        eventPublisher.movedBefore(eventContext(moduleAlias, entityAlias, mutationSource, traceId), id, beforeId);
    }

    public void moveAfter(String moduleAlias, String entityAlias, String id, String afterId) {
        moveAfter(moduleAlias, entityAlias, id, afterId, RuntimeMutationSource.BUSINESS, null);
    }

    void moveAfterFromAction(String moduleAlias, String entityAlias, String id, String afterId, String traceId) {
        moveAfter(moduleAlias, entityAlias, id, afterId, RuntimeMutationSource.ACTION, traceId);
    }

    private void moveAfter(String moduleAlias, String entityAlias, String id, String afterId, RuntimeMutationSource mutationSource, String traceId) {
        entityService(moduleAlias, entityAlias).moveAfter(id, afterId);
        eventPublisher.movedAfter(eventContext(moduleAlias, entityAlias, mutationSource, traceId), id, afterId);
    }

    public List<DynamicRecord> children(String moduleAlias, String entityAlias, String parentId) {
        return entityService(moduleAlias, entityAlias).children(parentId);
    }

    public List<String> ancestorIds(String moduleAlias, String entityAlias, String id) {
        return entityService(moduleAlias, entityAlias).ancestorIds(id);
    }

    public List<String> ancestorIdsAndSelf(String moduleAlias, String entityAlias, String id) {
        return entityService(moduleAlias, entityAlias).ancestorIdsAndSelf(id);
    }

    public List<String> descendantIds(String moduleAlias, String entityAlias, String id) {
        return entityService(moduleAlias, entityAlias).descendantIds(id);
    }

    public int enable(String moduleAlias, String entityAlias, String id) {
        return enable(moduleAlias, entityAlias, id, RuntimeMutationSource.BUSINESS, null);
    }

    int enableFromAction(String moduleAlias, String entityAlias, String id, String traceId) {
        return enable(moduleAlias, entityAlias, id, RuntimeMutationSource.ACTION, traceId);
    }

    private int enable(String moduleAlias, String entityAlias, String id, RuntimeMutationSource mutationSource, String traceId) {
        int updated = entityService(moduleAlias, entityAlias).enable(id);
        if (updated > 0) {
            eventPublisher.enabled(eventContext(moduleAlias, entityAlias, mutationSource, traceId), id);
        }
        return updated;
    }

    public int disable(String moduleAlias, String entityAlias, String id) {
        return disable(moduleAlias, entityAlias, id, RuntimeMutationSource.BUSINESS, null);
    }

    int disableFromAction(String moduleAlias, String entityAlias, String id, String traceId) {
        return disable(moduleAlias, entityAlias, id, RuntimeMutationSource.ACTION, traceId);
    }

    private int disable(String moduleAlias, String entityAlias, String id, RuntimeMutationSource mutationSource, String traceId) {
        int updated = entityService(moduleAlias, entityAlias).disable(id);
        if (updated > 0) {
            eventPublisher.disabled(eventContext(moduleAlias, entityAlias, mutationSource, traceId), id);
        }
        return updated;
    }

    public boolean isEnabled(String moduleAlias, String entityAlias, String id) {
        return entityService(moduleAlias, entityAlias).isEnabled(id);
    }

    public Criteria enabledCriteria(String moduleAlias, String entityAlias, Criteria criteria) {
        return entityService(moduleAlias, entityAlias).enabledCriteria(criteria);
    }

    public Criteria queryCriteria(String moduleAlias, String entityAlias, Collection<DynamicQueryCondition> conditions) {
        return entityService(moduleAlias, entityAlias).queryCriteria(conditions);
    }

    public String title(String moduleAlias, String entityAlias, String id) {
        return entityService(moduleAlias, entityAlias).title(id);
    }

    public Map<String, String> titles(String moduleAlias, String entityAlias, Collection<String> ids) {
        return entityService(moduleAlias, entityAlias).titles(ids);
    }

    public Map<String, Map<String, Object>> projections(String moduleAlias,
                                                        String entityAlias,
                                                        Collection<String> ids,
                                                        Collection<String> fieldNames) {
        return entityService(moduleAlias, entityAlias).projections(ids, fieldNames);
    }

    public PageResult<ReferenceOption> referenceOptions(String moduleAlias,
                                                        String entityAlias,
                                                        Criteria criteria,
                                                        PageRequest pageRequest) {
        return entityService(moduleAlias, entityAlias).referenceOptions(criteria, pageRequest);
    }

    public DynamicReferenceResolveResponse resolveReference(String moduleAlias,
                                                            String entityAlias,
                                                            String sourceField,
                                                            DynamicReferenceResolveRequest request) {
        return entityService(moduleAlias, entityAlias).resolveReference(sourceField, request);
    }

    public DynamicReferenceResolveResponse resolveFieldReference(String moduleAlias,
                                                                 String entityAlias,
                                                                 String fieldName,
                                                                 DynamicReferenceResolveRequest request) {
        return resolveReference(moduleAlias, entityAlias, fieldName, request);
    }

    private DynamicActionExecutionResult executeAction(String moduleAlias,
                                                       String entityAlias,
                                                       DynamicActionDescriptor action,
                                                       DynamicActionExecutionRequest request) {
        DynamicActionExecutionRequest normalized = request == null ? DynamicActionExecutionRequest.empty() : request;
        DynamicRecord availabilityRecord = availabilityRecord(moduleAlias, entityAlias, normalized);
        DynamicActionAvailability availability = actionAvailability(moduleAlias, entityAlias, action.code(), availabilityRecord);
        String traceId = UUID.randomUUID().toString();
        DynamicActionExecutionContext context = executionContext(moduleAlias, entityAlias, action, normalized, availability, null, traceId);
        if (!availability.available()) {
            eventPublisher.actionFailed(context, DynamicActionExecutionException.STAGE_AVAILABILITY, availability.message(), null);
            throw new DynamicActionExecutionException(availability.message(), context,
                    DynamicActionExecutionException.STAGE_AVAILABILITY, null);
        }
        DynamicActionExecutionResult result;
        try {
            result = runtime.actionTransactionOperator().executeResult(context, () -> {
                if (!isInteractionOnlyAction(action)) {
                    validateBeforeActionExecute(moduleAlias, entityAlias, normalized, context);
                }
                DynamicActionResultBody body = executeActionValue(moduleAlias, entityAlias, action, normalized, context, traceId);
                DynamicActionExecutionContext completed = executionContext(moduleAlias, entityAlias, action, normalized, availability, body.value(), traceId);
                return new DynamicActionExecutionResult(completed, body.value(), body);
            });
        } catch (DynamicActionExecutionException e) {
            eventPublisher.actionFailed(context, e.failureStage(), e.getMessage(), failureError(e));
            throw e;
        } catch (RuntimeException e) {
            RuntimeException afterCommitFailure = afterCommitFailure(e);
            if (afterCommitFailure != null) {
                throw afterCommitFailure;
            }
            eventPublisher.actionFailed(context, DynamicActionExecutionException.STAGE_EXECUTE, e.getMessage(), e);
            throw e;
        }
        eventPublisher.actionExecuted(result.context(), result.body());
        return result;
    }

    private RuntimeException afterCommitFailure(RuntimeException error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof TransactionScopeSupport.AfterCommitActionException afterCommit) {
                return afterCommit.unwrap();
            }
            current = current.getCause();
        }
        return null;
    }

    private void validateBeforeActionExecute(String moduleAlias,
                                             String entityAlias,
                                             DynamicActionExecutionRequest request,
                                             DynamicActionExecutionContext context) {
        DynamicRecord record = availabilityRecord(moduleAlias, entityAlias, request);
        if (record != null && (record.getId() == null || record.getId().isBlank()) && request.recordId() != null) {
            record.setId(request.recordId());
        }
        if (record == null) {
            return;
        }
        DynamicFormulaRuntime formulaRuntime = new DynamicFormulaRuntime(
                moduleAlias, record.getEntity(), runtime.registry().requireModule(moduleAlias));
        if (!formulaRuntime.hasBeforeActionExecuteRules()) {
            return;
        }
        DynamicRecord existing = !shouldLoadExistingForActionRules(record)
                ? null
                : select(moduleAlias, entityAlias, record.getId());
        if (isActionRecordProbe(record) && existing != null) {
            record = existing;
            existing = null;
        }
        try {
            formulaRuntime.beforeActionExecute(record, existing);
        } catch (DynamicFormulaException e) {
            throw new DynamicActionExecutionException(e.getMessage(), context,
                    DynamicActionExecutionException.STAGE_BEFORE_EXECUTE_RULE, e);
        }
    }

    private boolean isActionRecordProbe(DynamicRecord record) {
        return record.explicitFieldCodes().isEmpty() && record.getChildren().isEmpty();
    }

    private boolean shouldLoadExistingForActionRules(DynamicRecord record) {
        return record.getId() != null
                && !record.getId().isBlank()
                && (isActionRecordProbe(record) || !record.getChildren().isEmpty());
    }

    private DynamicActionResultBody executeActionValue(String moduleAlias,
                                                       String entityAlias,
                                                       DynamicActionDescriptor action,
                                                       DynamicActionExecutionRequest request,
                                                       DynamicActionExecutionContext context,
                                                       String traceId) {
        if (action.executorType() == EntityActionExecutorType.STANDARD) {
            return new DynamicStandardActionExecutor(this, moduleAlias, entityAlias, traceId)
                    .execute(action.code(), request);
        }
        if (action.executorType() == EntityActionExecutorType.SERVICE) {
            DynamicActionExecutor executor;
            try {
                executor = runtime.actionExecutorRegistry().require(action.executorKey());
            } catch (IllegalArgumentException e) {
                throw new DynamicActionExecutionException(e.getMessage(), context, e);
            }
            try {
                return actionResultBody(executor.execute(context, request,
                        actionOperations(moduleAlias, entityAlias, traceId)));
            } catch (DynamicActionExecutionException e) {
                throw e;
            } catch (RuntimeException e) {
                throw new DynamicActionExecutionException(e.getMessage(), context, e);
            }
        }
        if (action.executorType() == EntityActionExecutorType.DIALOG) {
            return DynamicActionResultBody.dialog(dialogKey(action), action.title());
        }
        throw new DynamicActionExecutionException(
                "dynamic action executor is not supported: " + action.executorType(),
                context
        );
    }

    private String dialogKey(DynamicActionDescriptor action) {
        return requireText(action.executorKey(), "dialog executorKey");
    }

    private boolean isInteractionOnlyAction(DynamicActionDescriptor action) {
        return action.executorType() == EntityActionExecutorType.DIALOG;
    }

    private DynamicActionResultBody actionResultBody(Object value) {
        if (value instanceof DynamicActionResultBody body) {
            return body;
        }
        return DynamicActionResultBody.of(value);
    }

    private DynamicActionOperations actionOperations(String moduleAlias, String entityAlias, String traceId) {
        return new DynamicActionOperations() {
            @Override
            public DynamicRecord newRecord() {
                return DynamicRecordService.this.newRecord(moduleAlias, entityAlias);
            }

            @Override
            public DynamicRecord select(String id) {
                return DynamicRecordService.this.select(moduleAlias, entityAlias, id);
            }

            @Override
            public int update(DynamicRecord record) {
                return DynamicRecordService.this.update(moduleAlias, entityAlias, record,
                        RuntimeMutationSource.ACTION, traceId);
            }

            @Override
            public int delete(String id) {
                return DynamicRecordService.this.delete(moduleAlias, entityAlias, id,
                        RuntimeMutationSource.ACTION, traceId);
            }
        };
    }

    private DynamicRecord availabilityRecord(String moduleAlias, String entityAlias, DynamicActionExecutionRequest request) {
        if (request.record() != null) {
            return request.record();
        }
        if (request.recordId() == null || request.recordId().isBlank()) {
            return null;
        }
        DynamicRecord probe = newRecord(moduleAlias, entityAlias);
        probe.setId(request.recordId());
        return probe;
    }

    private DynamicActionExecutionContext executionContext(String moduleAlias,
                                                           String entityAlias,
                                                           DynamicActionDescriptor action,
                                                           DynamicActionExecutionRequest request,
                                                           DynamicActionAvailability availability) {
        return executionContext(moduleAlias, entityAlias, action, request, availability, null, UUID.randomUUID().toString());
    }

    private DynamicActionExecutionContext executionContext(String moduleAlias,
                                                           String entityAlias,
                                                           DynamicActionDescriptor action,
                                                           DynamicActionExecutionRequest request,
                                                           DynamicActionAvailability availability,
                                                           Object value,
                                                           String traceId) {
        String recordId = request.recordId();
        if ((recordId == null || recordId.isBlank()) && request.record() != null) {
            recordId = request.record().getId();
        }
        if ((recordId == null || recordId.isBlank()) && PlatformAction.CREATE.matches(action.code()) && value instanceof String id) {
            recordId = id;
        }
        return new DynamicActionExecutionContext(
                moduleAlias,
                entityAlias,
                action.code(),
                action,
                recordId,
                traceId,
                TenantContext.currentTenantId().orElse(null),
                TenantContext.isSystem(),
                availability
        );
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("dynamic action requires " + fieldName);
        }
        return value;
    }

    DynamicEntityService entityService(String moduleAlias, String entityAlias) {
        return runtime.entityService(moduleAlias, entityAlias);
    }

    private Throwable failureError(DynamicActionExecutionException exception) {
        return exception.getCause() == null ? exception : exception.getCause();
    }

    private DynamicRecordEventPublisher.DynamicRecordEventContext eventContext(String moduleAlias,
                                                                               String entityAlias,
                                                                               RuntimeMutationSource mutationSource,
                                                                               String traceId) {
        return new DynamicRecordEventPublisher.DynamicRecordEventContext(
                moduleAlias,
                entityAlias,
                traceId,
                TenantContext.currentTenantId().orElse(null),
                TenantContext.isSystem(),
                mutationSource
        );
    }

    private DynamicEntityDescriptor findEntity(DynamicModuleDescriptor descriptor, String entityAlias) {
        return descriptor.entities().stream()
                .filter(entity -> entity.entityAlias().equals(entityAlias))
                .findFirst()
                .orElseThrow(() -> new ModuleDefinitionException("unknown dynamic entity: "
                        + descriptor.moduleAlias() + "." + entityAlias));
    }

    private DynamicActionDescriptor findAction(DynamicModuleDescriptor module, String actionCode) {
        return module.actions().stream()
                .filter(action -> action.code().equals(actionCode))
                .findFirst()
                .orElseThrow(() -> new ModuleDefinitionException("unknown dynamic action: "
                        + module.moduleAlias() + "." + actionCode));
    }

    private DynamicActionDescriptor findAction(String moduleAlias, DynamicEntityDescriptor entity, String actionCode) {
        return entity.actions().stream()
                .filter(action -> action.code().equals(actionCode))
                .findFirst()
                .orElseThrow(() -> new ModuleDefinitionException("unknown dynamic action: "
                        + moduleAlias + "." + entity.entityAlias() + "." + actionCode));
    }

    private DynamicViewDescriptor findView(String moduleAlias, DynamicEntityDescriptor entity, EntityViewType viewType) {
        return entity.views().stream()
                .filter(view -> view.viewType() == viewType)
                .findFirst()
                .orElseThrow(() -> new ModuleDefinitionException("unknown dynamic view: "
                        + moduleAlias + "." + entity.entityAlias() + "." + viewType));
    }

    private DynamicAssociationViewDescriptor findAssociationView(String moduleAlias,
                                                                DynamicEntityDescriptor entity,
                                                                String viewCode) {
        return entity.associationViews().stream()
                .filter(view -> view.code().equals(viewCode))
                .findFirst()
                .orElseThrow(() -> new ModuleDefinitionException("unknown dynamic association view: "
                        + moduleAlias + "." + entity.entityAlias() + "." + viewCode));
    }

    public static final class ModuleOperations {
        private final DynamicRecordService service;
        private final String moduleAlias;

        private ModuleOperations(DynamicRecordService service, String moduleAlias) {
            this.service = service;
            this.moduleAlias = moduleAlias;
        }

        public DynamicModuleDescriptor describe() {
            return service.describe(moduleAlias);
        }

        public List<DynamicActionDescriptor> actions() {
            return service.actions(moduleAlias);
        }

        public DynamicActionDescriptor action(String actionCode) {
            return service.action(moduleAlias, actionCode);
        }

        public DynamicActionAvailability actionAvailability(String actionCode, DynamicRecord record) {
            return service.actionAvailability(moduleAlias, actionCode, record);
        }

        public DynamicActionExecutionResult executeAction(String actionCode, DynamicActionExecutionRequest request) {
            return service.executeAction(moduleAlias, actionCode, request);
        }

        public List<DynamicEntityDescriptor> entities() {
            return describe().entities();
        }

        public List<DynamicRelationDescriptor> relations() {
            return service.relations(moduleAlias);
        }

        public List<DynamicReferenceDescriptor> references() {
            return service.references(moduleAlias);
        }

        public List<DynamicAssociationViewDescriptor> associationViews() {
            return service.associationViews(moduleAlias);
        }

        public DynamicEntityOperations entity(String entityAlias) {
            return service.entity(moduleAlias, entityAlias);
        }
    }

}
