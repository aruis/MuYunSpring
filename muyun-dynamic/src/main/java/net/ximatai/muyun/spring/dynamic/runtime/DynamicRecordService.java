package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.TransactionScopeSupport;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.ability.event.RuntimeMutationSource;
import net.ximatai.muyun.spring.ability.reference.ReferenceOption;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.platform.ActionAccessMode;
import net.ximatai.muyun.spring.common.platform.ActionAuthorizationResult;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicy;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicyService;
import net.ximatai.muyun.spring.common.platform.ActionDefaultGrantPolicy;
import net.ximatai.muyun.spring.common.platform.AllowAllActionExecutionPolicyService;
import net.ximatai.muyun.spring.common.platform.AllowAllDataScopeCriteriaService;
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaResult;
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaService;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
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

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class DynamicRecordService {
    private static final DynamicOpenApiGenerator OPEN_API_GENERATOR = new DynamicOpenApiGenerator();

    private final DynamicRecordRuntime runtime;
    private final DynamicRecordEventPublisher eventPublisher;
    private final ActionExecutionPolicyService actionExecutionPolicyService;
    private final DataScopeCriteriaService dataScopeCriteriaService;

    public DynamicRecordService(DynamicRecordRuntime runtime) {
        this(runtime, new AllowAllActionExecutionPolicyService());
    }

    public DynamicRecordService(DynamicRecordRuntime runtime,
                                ActionExecutionPolicyService actionExecutionPolicyService) {
        this(runtime, actionExecutionPolicyService, new AllowAllDataScopeCriteriaService());
    }

    public DynamicRecordService(DynamicRecordRuntime runtime,
                                ActionExecutionPolicyService actionExecutionPolicyService,
                                DataScopeCriteriaService dataScopeCriteriaService) {
        this.runtime = Objects.requireNonNull(runtime, "runtime must not be null");
        this.eventPublisher = new DynamicRecordEventPublisher(runtime.eventPublisher());
        this.actionExecutionPolicyService = Objects.requireNonNull(actionExecutionPolicyService,
                "actionExecutionPolicyService must not be null");
        this.dataScopeCriteriaService = Objects.requireNonNull(dataScopeCriteriaService,
                "dataScopeCriteriaService must not be null");
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
        if (mutationSource == RuntimeMutationSource.BUSINESS) {
            actionExecutionPolicyService.requireAuthorized(ActionExecutionContext.ofPlatformAction(
                    moduleAlias,
                    PlatformAction.CREATE,
                    Set.of(),
                    CurrentUserContext.currentUser()
            ));
        }
        String id = entityService(moduleAlias, entityAlias).insert(record);
        eventPublisher.created(eventContext(moduleAlias, entityAlias, mutationSource, traceId), id);
        return id;
    }

    public DynamicRecord select(String moduleAlias, String entityAlias, String id) {
        Criteria base = Criteria.of().eq("id", id);
        DataScopeCriteriaResult scope = readScope(moduleAlias, PlatformAction.VIEW.code(), base);
        return withTenantScope(scope, () -> {
            if (!scope.restricted()) {
                return entityService(moduleAlias, entityAlias).select(id);
            }
            boolean visible = !entityService(moduleAlias, entityAlias).list(scope.criteria(), new PageRequest(0, 1)).isEmpty();
            return visible ? entityService(moduleAlias, entityAlias).select(id) : null;
        });
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
        DataScopeCriteriaResult mutationScope = DataScopeCriteriaResult.unrestricted(Criteria.of());
        if (mutationSource == RuntimeMutationSource.BUSINESS) {
            Set<String> recordIds = normalizeRecordId(record == null ? null : record.getId());
            mutationScope = requireBusinessRecordMutation(moduleAlias, entityAlias, PlatformAction.UPDATE, recordIds);
        }
        int updated = withTenantScope(mutationScope, () -> entityService(moduleAlias, entityAlias).update(record));
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
        DataScopeCriteriaResult mutationScope = DataScopeCriteriaResult.unrestricted(Criteria.of());
        if (mutationSource == RuntimeMutationSource.BUSINESS) {
            Set<String> recordIds = normalizeRecordId(id);
            mutationScope = requireBusinessRecordMutation(moduleAlias, entityAlias, PlatformAction.DELETE, recordIds);
        }
        int deleted = withTenantScope(mutationScope, () -> entityService(moduleAlias, entityAlias).delete(id));
        if (deleted > 0) {
            eventPublisher.deleted(eventContext(moduleAlias, entityAlias, mutationSource, traceId), id);
        }
        return deleted;
    }

    public int deleteBatch(String moduleAlias, String entityAlias, Collection<String> ids) {
        return deleteBatch(moduleAlias, entityAlias, ids, RuntimeMutationSource.BUSINESS, null);
    }

    private int deleteBatch(String moduleAlias, String entityAlias, Collection<String> ids, RuntimeMutationSource mutationSource, String traceId) {
        DataScopeCriteriaResult mutationScope = DataScopeCriteriaResult.unrestricted(Criteria.of());
        if (mutationSource == RuntimeMutationSource.BUSINESS) {
            Set<String> normalized = normalizeRecordIds(ids);
            mutationScope = requireBusinessRecordMutation(moduleAlias, entityAlias, PlatformAction.DELETE, normalized);
        }
        int deleted = withTenantScope(mutationScope, () -> entityService(moduleAlias, entityAlias).deleteBatch(ids));
        if (deleted > 0) {
            eventPublisher.deletedBatch(eventContext(moduleAlias, entityAlias, mutationSource, traceId),
                    List.copyOf(ids), deleted);
        }
        return deleted;
    }

    public List<DynamicRecord> list(String moduleAlias, String entityAlias, Criteria criteria, PageRequest pageRequest, Sort... sorts) {
        DataScopeCriteriaResult scope = readScope(moduleAlias, PlatformAction.QUERY, criteria);
        return withTenantScope(scope, () -> entityService(moduleAlias, entityAlias).list(scope.criteria(),
                pageRequest, sorts));
    }

    public PageResult<DynamicRecord> page(String moduleAlias, String entityAlias, Criteria criteria, PageRequest pageRequest, Sort... sorts) {
        DataScopeCriteriaResult scope = readScope(moduleAlias, PlatformAction.QUERY, criteria);
        return withTenantScope(scope, () -> entityService(moduleAlias, entityAlias).pageQuery(scope.criteria(),
                pageRequest, sorts));
    }

    public long count(String moduleAlias, String entityAlias, Criteria criteria) {
        DataScopeCriteriaResult scope = readScope(moduleAlias, PlatformAction.QUERY, criteria);
        return withTenantScope(scope, () -> entityService(moduleAlias, entityAlias).count(scope.criteria()));
    }

    public List<DynamicRecord> sortedList(String moduleAlias, String entityAlias, Criteria criteria) {
        DataScopeCriteriaResult scope = readScope(moduleAlias, PlatformAction.QUERY, criteria);
        return withTenantScope(scope, () -> entityService(moduleAlias, entityAlias).sortedList(scope.criteria()));
    }

    public void reorder(String moduleAlias, String entityAlias, List<String> orderedIds) {
        reorder(moduleAlias, entityAlias, orderedIds, RuntimeMutationSource.BUSINESS, null);
    }

    void reorderFromAction(String moduleAlias, String entityAlias, List<String> orderedIds, String traceId) {
        reorder(moduleAlias, entityAlias, orderedIds, RuntimeMutationSource.ACTION, traceId);
    }

    private void reorder(String moduleAlias, String entityAlias, List<String> orderedIds, RuntimeMutationSource mutationSource, String traceId) {
        DataScopeCriteriaResult mutationScope = DataScopeCriteriaResult.unrestricted(Criteria.of());
        if (requiresStandardSortScopeCheck(mutationSource)) {
            mutationScope = sortMutationScope(moduleAlias, entityAlias, normalizeRecordIds(orderedIds),
                    ignored -> normalizeRecordIds(orderedIds));
        }
        withTenantScope(mutationScope, () -> {
            entityService(moduleAlias, entityAlias).reorder(orderedIds);
            return null;
        });
        eventPublisher.reordered(eventContext(moduleAlias, entityAlias, mutationSource, traceId), orderedIds);
    }

    public void moveBefore(String moduleAlias, String entityAlias, String id, String beforeId) {
        moveBefore(moduleAlias, entityAlias, id, beforeId, RuntimeMutationSource.BUSINESS, null);
    }

    void moveBeforeFromAction(String moduleAlias, String entityAlias, String id, String beforeId, String traceId) {
        moveBefore(moduleAlias, entityAlias, id, beforeId, RuntimeMutationSource.ACTION, traceId);
    }

    private void moveBefore(String moduleAlias, String entityAlias, String id, String beforeId, RuntimeMutationSource mutationSource, String traceId) {
        DataScopeCriteriaResult mutationScope = DataScopeCriteriaResult.unrestricted(Criteria.of());
        if (requiresStandardSortScopeCheck(mutationSource)) {
            mutationScope = sortMutationScope(moduleAlias, entityAlias, normalizeRecordIds(Arrays.asList(id, beforeId)),
                    scope -> sortScopeRecordIds(moduleAlias, entityAlias, id, beforeId));
        }
        withTenantScope(mutationScope, () -> {
            entityService(moduleAlias, entityAlias).moveBefore(id, beforeId);
            return null;
        });
        eventPublisher.movedBefore(eventContext(moduleAlias, entityAlias, mutationSource, traceId), id, beforeId);
    }

    public void moveAfter(String moduleAlias, String entityAlias, String id, String afterId) {
        moveAfter(moduleAlias, entityAlias, id, afterId, RuntimeMutationSource.BUSINESS, null);
    }

    void moveAfterFromAction(String moduleAlias, String entityAlias, String id, String afterId, String traceId) {
        moveAfter(moduleAlias, entityAlias, id, afterId, RuntimeMutationSource.ACTION, traceId);
    }

    private void moveAfter(String moduleAlias, String entityAlias, String id, String afterId, RuntimeMutationSource mutationSource, String traceId) {
        DataScopeCriteriaResult mutationScope = DataScopeCriteriaResult.unrestricted(Criteria.of());
        if (requiresStandardSortScopeCheck(mutationSource)) {
            mutationScope = sortMutationScope(moduleAlias, entityAlias, normalizeRecordIds(Arrays.asList(id, afterId)),
                    scope -> sortScopeRecordIds(moduleAlias, entityAlias, id, afterId));
        }
        withTenantScope(mutationScope, () -> {
            entityService(moduleAlias, entityAlias).moveAfter(id, afterId);
            return null;
        });
        eventPublisher.movedAfter(eventContext(moduleAlias, entityAlias, mutationSource, traceId), id, afterId);
    }

    public void moveInTree(String moduleAlias, String entityAlias, String id, String previousId, String nextId, String parentId) {
        moveInTree(moduleAlias, entityAlias, id, previousId, nextId, parentId, RuntimeMutationSource.BUSINESS, null);
    }

    private void moveInTree(String moduleAlias,
                            String entityAlias,
                            String id,
                            String previousId,
                            String nextId,
                            String parentId,
                            RuntimeMutationSource mutationSource,
                            String traceId) {
        if (mutationSource == RuntimeMutationSource.BUSINESS) {
            DataScopeCriteriaResult mutationScope = sortMutationScope(moduleAlias, entityAlias,
                    treeSortExplicitRecordIds(id, previousId, nextId, parentId),
                    scope -> treeSortScopeRecordIds(moduleAlias, entityAlias, id, previousId, nextId, parentId));
            withTenantScope(mutationScope, () -> {
                entityService(moduleAlias, entityAlias).moveInTree(id, previousId, nextId, parentId);
                return null;
            });
        } else {
            entityService(moduleAlias, entityAlias).moveInTree(id, previousId, nextId, parentId);
        }
        eventPublisher.movedInTree(eventContext(moduleAlias, entityAlias, mutationSource, traceId),
                id, previousId, nextId, parentId);
    }

    public List<DynamicRecord> children(String moduleAlias, String entityAlias, String parentId) {
        DataScopeCriteriaResult scope = readScope(moduleAlias, PlatformAction.TREE, Criteria.of());
        return withTenantScope(scope, () -> {
            if (!scope.restricted()) {
                return entityService(moduleAlias, entityAlias).children(parentId);
            }
            return entityService(moduleAlias, entityAlias).children(scope.criteria(), parentId);
        });
    }

    public List<String> ancestorIds(String moduleAlias, String entityAlias, String id) {
        requireCapability(moduleAlias, entityAlias, EntityCapability.TREE);
        DataScopeCriteriaResult scope = readScope(moduleAlias, PlatformAction.TREE, Criteria.of().eq("id", id));
        if (!recordVisible(moduleAlias, entityAlias, scope, id)) {
            return List.of();
        }
        List<String> ids = withTenantScope(scope, () -> entityService(moduleAlias, entityAlias).ancestorIds(id));
        return visibleTreeIds(moduleAlias, entityAlias, ids);
    }

    public List<String> ancestorIdsAndSelf(String moduleAlias, String entityAlias, String id) {
        requireCapability(moduleAlias, entityAlias, EntityCapability.TREE);
        DataScopeCriteriaResult scope = readScope(moduleAlias, PlatformAction.TREE, Criteria.of().eq("id", id));
        if (!recordVisible(moduleAlias, entityAlias, scope, id)) {
            return List.of();
        }
        List<String> ids = withTenantScope(scope, () -> entityService(moduleAlias, entityAlias).ancestorIdsAndSelf(id));
        return visibleTreeIds(moduleAlias, entityAlias, ids);
    }

    public List<String> descendantIds(String moduleAlias, String entityAlias, String id) {
        requireCapability(moduleAlias, entityAlias, EntityCapability.TREE);
        DataScopeCriteriaResult scope = readScope(moduleAlias, PlatformAction.TREE, Criteria.of().eq("id", id));
        if (!recordVisible(moduleAlias, entityAlias, scope, id)) {
            return List.of();
        }
        List<String> ids = withTenantScope(scope, () -> entityService(moduleAlias, entityAlias).descendantIds(id));
        return visibleTreeIds(moduleAlias, entityAlias, ids);
    }

    public int enable(String moduleAlias, String entityAlias, String id) {
        return enable(moduleAlias, entityAlias, id, RuntimeMutationSource.BUSINESS, null);
    }

    int enableFromAction(String moduleAlias, String entityAlias, String id, String traceId) {
        return enable(moduleAlias, entityAlias, id, RuntimeMutationSource.ACTION, traceId);
    }

    private int enable(String moduleAlias, String entityAlias, String id, RuntimeMutationSource mutationSource, String traceId) {
        DataScopeCriteriaResult mutationScope = DataScopeCriteriaResult.unrestricted(Criteria.of());
        if (mutationSource == RuntimeMutationSource.BUSINESS) {
            mutationScope = requireBusinessRecordMutation(moduleAlias, entityAlias, PlatformAction.ENABLE, normalizeRecordId(id));
        }
        int updated = withTenantScope(mutationScope, () -> entityService(moduleAlias, entityAlias).enable(id));
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
        DataScopeCriteriaResult mutationScope = DataScopeCriteriaResult.unrestricted(Criteria.of());
        if (mutationSource == RuntimeMutationSource.BUSINESS) {
            mutationScope = requireBusinessRecordMutation(moduleAlias, entityAlias, PlatformAction.DISABLE, normalizeRecordId(id));
        }
        int updated = withTenantScope(mutationScope, () -> entityService(moduleAlias, entityAlias).disable(id));
        if (updated > 0) {
            eventPublisher.disabled(eventContext(moduleAlias, entityAlias, mutationSource, traceId), id);
        }
        return updated;
    }

    public boolean isEnabled(String moduleAlias, String entityAlias, String id) {
        requireCapability(moduleAlias, entityAlias, EntityCapability.ENABLE);
        DataScopeCriteriaResult scope = readScope(moduleAlias, PlatformAction.VIEW, Criteria.of().eq("id", id));
        if (!recordVisible(moduleAlias, entityAlias, scope, id)) {
            return false;
        }
        return withTenantScope(scope, () -> entityService(moduleAlias, entityAlias).isEnabled(id));
    }

    public Criteria enabledCriteria(String moduleAlias, String entityAlias, Criteria criteria) {
        return entityService(moduleAlias, entityAlias).enabledCriteria(criteria);
    }

    public Criteria queryCriteria(String moduleAlias, String entityAlias, Collection<DynamicQueryCondition> conditions) {
        return entityService(moduleAlias, entityAlias).queryCriteria(conditions);
    }

    public String title(String moduleAlias, String entityAlias, String id) {
        requireCapability(moduleAlias, entityAlias, EntityCapability.REFERENCE);
        DataScopeCriteriaResult scope = readScope(moduleAlias, PlatformAction.VIEW, Criteria.of().eq("id", id));
        if (!recordVisible(moduleAlias, entityAlias, scope, id)) {
            return null;
        }
        return withTenantScope(scope, () -> entityService(moduleAlias, entityAlias).title(id));
    }

    public Map<String, String> titles(String moduleAlias, String entityAlias, Collection<String> ids) {
        requireCapability(moduleAlias, entityAlias, EntityCapability.REFERENCE);
        DataScopeCriteriaResult scope = readScope(moduleAlias, PlatformAction.VIEW, idsCriteria(ids));
        Set<String> visibleIds = visibleRecordIds(moduleAlias, entityAlias, scope, ids);
        return withTenantScope(scope, () -> entityService(moduleAlias, entityAlias).titles(visibleIds));
    }

    public Map<String, Map<String, Object>> projections(String moduleAlias,
                                                        String entityAlias,
                                                        Collection<String> ids,
                                                        Collection<String> fieldNames) {
        requireCapability(moduleAlias, entityAlias, EntityCapability.REFERENCE);
        DataScopeCriteriaResult scope = readScope(moduleAlias, PlatformAction.VIEW, idsCriteria(ids));
        Set<String> visibleIds = visibleRecordIds(moduleAlias, entityAlias, scope, ids);
        return withTenantScope(scope, () -> entityService(moduleAlias, entityAlias).projections(visibleIds, fieldNames));
    }

    public PageResult<ReferenceOption> referenceOptions(String moduleAlias,
                                                        String entityAlias,
                                                        Criteria criteria,
                                                        PageRequest pageRequest) {
        DataScopeCriteriaResult scope = readScope(moduleAlias, PlatformAction.REFERENCE, criteria);
        return withTenantScope(scope, () -> entityService(moduleAlias, entityAlias)
                .referenceOptions(scope.criteria(), pageRequest));
    }

    private DataScopeCriteriaResult readScope(String moduleAlias, PlatformAction action, Criteria criteria) {
        return readScope(moduleAlias, action.executionPolicy(), criteria);
    }

    private DataScopeCriteriaResult readScope(String moduleAlias, String actionCode, Criteria criteria) {
        return readScope(moduleAlias, ActionExecutionContext.ofActionCode(
                moduleAlias, actionCode, Set.of(), CurrentUserContext.currentUser()).actionPolicy(), criteria);
    }

    private DataScopeCriteriaResult readScope(String moduleAlias, ActionExecutionPolicy policy, Criteria criteria) {
        return dataScopeCriteriaService.resolveReadScope(moduleAlias, policy,
                criteria == null ? Criteria.of() : criteria,
                CurrentUserContext.currentUser());
    }

    private Criteria idsCriteria(Collection<String> ids) {
        Set<String> normalized = normalizeRecordIds(ids);
        if (normalized.isEmpty()) {
            return Criteria.of().raw(net.ximatai.muyun.database.core.orm.SqlRawCondition.of("1 = 0", Map.of()));
        }
        return normalized.size() == 1
                ? Criteria.of().eq("id", normalized.iterator().next())
                : Criteria.of().in("id", List.copyOf(normalized));
    }

    private boolean recordVisible(String moduleAlias, String entityAlias, DataScopeCriteriaResult scope, String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        return !withTenantScope(scope, () -> entityService(moduleAlias, entityAlias)
                .list(scope.criteria(), new PageRequest(0, 1))).isEmpty();
    }

    private Set<String> visibleRecordIds(String moduleAlias,
                                         String entityAlias,
                                         DataScopeCriteriaResult scope,
                                         Collection<String> ids) {
        Set<String> normalized = normalizeRecordIds(ids);
        if (normalized.isEmpty()) {
            return Set.of();
        }
        Set<String> loaded = withTenantScope(scope, () -> entityService(moduleAlias, entityAlias)
                .list(scope.criteria(), new PageRequest(0, normalized.size()))
                .stream()
                .map(DynamicRecord::getId)
                .filter(normalized::contains)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)));
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        normalized.stream()
                .filter(loaded::contains)
                .forEach(ordered::add);
        return ordered;
    }

    private <R> R withTenantScope(DataScopeCriteriaResult scope, Supplier<R> supplier) {
        if (scope.crossTenant()) {
            try (TenantContext.Scope ignored = TenantContext.bypassTenantFilter("data scope allows cross-tenant read")) {
                return supplier.get();
            }
        }
        return supplier.get();
    }

    private List<String> visibleTreeIds(String moduleAlias, String entityAlias, Collection<String> ids) {
        DataScopeCriteriaResult scope = readScope(moduleAlias, PlatformAction.TREE, idsCriteria(ids));
        return List.copyOf(visibleRecordIds(moduleAlias, entityAlias, scope, ids));
    }

    private DataScopeCriteriaResult sortMutationScope(String moduleAlias,
                                                      String entityAlias,
                                                      Set<String> explicitRecordIds,
                                                      java.util.function.Function<DataScopeCriteriaResult, Set<String>> scopeCollector) {
        requireRecordAction(moduleAlias, PlatformAction.SORT, explicitRecordIds);
        DataScopeCriteriaResult explicitScope = requireRecordDataScope(moduleAlias, entityAlias,
                PlatformAction.SORT, explicitRecordIds);
        Set<String> scopedRecordIds = withTenantScope(explicitScope, () -> scopeCollector.apply(explicitScope));
        return requireRecordDataScope(moduleAlias, entityAlias, PlatformAction.SORT, scopedRecordIds);
    }

    private Set<String> sortScopeRecordIds(String moduleAlias, String entityAlias, String id, String targetId) {
        Set<String> recordIds = new LinkedHashSet<>(normalizeRecordIds(Arrays.asList(id, targetId)));
        DynamicEntityService service = entityService(moduleAlias, entityAlias);
        DynamicRecord moving = service.select(id);
        DynamicRecord target = targetId == null || targetId.isBlank() ? null : service.select(targetId);
        if (moving == null || target == null) {
            return recordIds;
        }
        service.validateSortScope(moving, target);
        service.sortedList(service.sortScope(moving)).stream()
                .map(DynamicRecord::getId)
                .forEach(recordIds::add);
        return recordIds;
    }

    private Set<String> treeSortScopeRecordIds(String moduleAlias,
                                               String entityAlias,
                                               String id,
                                               String previousId,
                                               String nextId,
                                               String parentId) {
        Set<String> recordIds = new LinkedHashSet<>(normalizeRecordIds(Arrays.asList(id, previousId, nextId)));
        DynamicEntityService service = entityService(moduleAlias, entityAlias);
        DynamicRecord moving = service.select(id);
        if (moving == null) {
            return recordIds;
        }
        String targetParentId = normalizeTreeParentId(parentId);
        if (targetParentId == null) {
            targetParentId = neighborParentId(service, previousId);
        }
        if (targetParentId == null) {
            targetParentId = neighborParentId(service, nextId);
        }
        if (targetParentId == null) {
            targetParentId = normalizeTreeParentId(moving.parentId());
        }
        if (targetParentId == null) {
            targetParentId = TreeAbility.ROOT_ID;
        }
        if (!TreeAbility.ROOT_ID.equals(targetParentId)) {
            recordIds.add(targetParentId);
        }
        service.children(targetParentId).stream()
                .map(DynamicRecord::getId)
                .forEach(recordIds::add);
        return recordIds;
    }

    private String neighborParentId(DynamicEntityService service, String neighborId) {
        if (neighborId == null || neighborId.isBlank()) {
            return null;
        }
        DynamicRecord neighbor = service.select(neighborId);
        return neighbor == null ? null : normalizeTreeParentId(neighbor.parentId());
    }

    private String normalizeTreeParentId(String parentId) {
        return parentId == null || parentId.isBlank() ? null : parentId;
    }

    private boolean requiresStandardSortScopeCheck(RuntimeMutationSource mutationSource) {
        return mutationSource == RuntimeMutationSource.BUSINESS || mutationSource == RuntimeMutationSource.ACTION;
    }

    private Set<String> treeSortExplicitRecordIds(String id, String previousId, String nextId, String parentId) {
        LinkedHashSet<String> ids = new LinkedHashSet<>(normalizeRecordIds(Arrays.asList(id, previousId, nextId)));
        String normalizedParentId = normalizeTreeParentId(parentId);
        if (normalizedParentId != null && !TreeAbility.ROOT_ID.equals(normalizedParentId)) {
            ids.add(normalizedParentId);
        }
        return java.util.Collections.unmodifiableSet(ids);
    }

    private DataScopeCriteriaResult requireBusinessRecordMutation(String moduleAlias,
                                                                  String entityAlias,
                                                                  PlatformAction action,
                                                                  Set<String> recordIds) {
        requireRecordAction(moduleAlias, action, recordIds);
        return requireRecordDataScope(moduleAlias, entityAlias, action, recordIds);
    }

    private void requireRecordAction(String moduleAlias, PlatformAction action, Set<String> recordIds) {
        actionExecutionPolicyService.requireRecordAction(ActionExecutionContext.ofPlatformAction(
                moduleAlias,
                action,
                recordIds,
                CurrentUserContext.currentUser()
        ));
    }

    private DataScopeCriteriaResult requireRecordDataScope(String moduleAlias,
                                                           String entityAlias,
                                                           PlatformAction action,
                                                           Set<String> recordIds) {
        if (!supportsCapability(moduleAlias, entityAlias, EntityCapability.DATA_SCOPE)) {
            return DataScopeCriteriaResult.unrestricted(Criteria.of());
        }
        Set<String> normalized = normalizeRecordIds(recordIds);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("record action requires record ids: " + moduleAlias + "." + action.code());
        }
        ActionExecutionContext context = ActionExecutionContext.ofPlatformAction(
                moduleAlias,
                action,
                normalized,
                CurrentUserContext.currentUser()
        );
        if (!context.actionPolicy().requiresDataScope()) {
            return DataScopeCriteriaResult.unrestricted(Criteria.of());
        }
        return requireActionRecordDataScope(moduleAlias, entityAlias, context.actionPolicy(), normalized);
    }

    private DataScopeCriteriaResult requireActionRecordDataScope(String moduleAlias,
                                                                 String entityAlias,
                                                                 ActionExecutionPolicy policy,
                                                                 Collection<String> recordIds) {
        if (!supportsCapability(moduleAlias, entityAlias, EntityCapability.DATA_SCOPE)) {
            return DataScopeCriteriaResult.unrestricted(Criteria.of());
        }
        Set<String> normalized = normalizeRecordIds(recordIds);
        if (!policy.requiresDataScope() || normalized.isEmpty()) {
            return DataScopeCriteriaResult.unrestricted(Criteria.of());
        }
        Criteria idCriteria = normalized.size() == 1
                ? Criteria.of().eq("id", normalized.iterator().next())
                : Criteria.of().in("id", List.copyOf(normalized));
        DataScopeCriteriaResult scope = readScope(moduleAlias, policy, idCriteria);
        long visible = withTenantScope(scope, () -> entityService(moduleAlias, entityAlias)
                .list(scope.criteria(), new PageRequest(0, normalized.size()))
                .stream()
                .map(DynamicRecord::getId)
                .filter(normalized::contains)
                .distinct()
                .count());
        if (visible != normalized.size()) {
            throw new PlatformException("record data permission denied: " + moduleAlias + "." + policy.actionCode());
        }
        return scope;
    }

    private Set<String> normalizeRecordIds(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        ids.stream()
                .filter(id -> id != null && !id.isBlank())
                .map(String::trim)
                .forEach(normalized::add);
        return java.util.Collections.unmodifiableSet(normalized);
    }

    private Set<String> normalizeRecordId(String id) {
        return normalizeRecordIds(id == null ? null : java.util.Collections.singletonList(id));
    }

    public DynamicReferenceResolveResponse resolveReference(String moduleAlias,
                                                            String entityAlias,
                                                            String sourceField,
                                                            DynamicReferenceResolveRequest request) {
        DynamicReferenceDescriptor reference = reference(moduleAlias, entityAlias, sourceField);
        DynamicReferenceResolveRequest normalized = request == null
                ? DynamicReferenceResolveRequest.query(null)
                : request;
        DataScopeCriteriaResult scope = readScope(reference.targetModuleAlias(), PlatformAction.REFERENCE, normalized.criteria());
        return withTenantScope(scope, () -> entityService(moduleAlias, entityAlias)
                .resolveReference(sourceField, normalized.withCriteria(scope.criteria())));
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
        ActionExecutionPolicy policy = actionPolicy(action);
        ActionAuthorizationResult authorization = actionExecutionPolicyService.authorize(ActionExecutionContext.ofPolicy(
                moduleAlias,
                policy,
                actionRecordIds(normalized),
                CurrentUserContext.currentUser()
        ));
        DataScopeCriteriaResult actionScope = requireActionRecordDataScope(moduleAlias, entityAlias, policy, actionRecordIds(normalized));
        DynamicActionAvailability availability = withTenantScope(actionScope, () -> {
            DynamicRecord availabilityRecord = availabilityRecord(moduleAlias, entityAlias, normalized);
            return actionAvailability(moduleAlias, entityAlias, action.code(), availabilityRecord);
        });
        String traceId = UUID.randomUUID().toString();
        DynamicActionExecutionContext context = executionContext(moduleAlias, entityAlias, action, normalized,
                availability, null, traceId, authorization);
        if (!availability.available()) {
            eventPublisher.actionFailed(context, DynamicActionExecutionException.STAGE_AVAILABILITY, availability.message(), null);
            throw new DynamicActionExecutionException(availability.message(), context,
                    DynamicActionExecutionException.STAGE_AVAILABILITY, null);
        }
        DynamicActionExecutionResult result;
        try {
            result = withTenantScope(actionScope, () -> runtime.actionTransactionOperator().executeResult(context, () -> {
                if (!isInteractionOnlyAction(action)) {
                    validateBeforeActionExecute(moduleAlias, entityAlias, normalized, context);
                }
                DynamicActionResultBody body = executeActionValue(moduleAlias, entityAlias, action, normalized, context, traceId, policy);
                DynamicActionExecutionContext completed = executionContext(moduleAlias, entityAlias, action, normalized,
                        availability, body.value(), traceId, authorization);
                return new DynamicActionExecutionResult(completed, body.value(), body);
            }));
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

    private Set<String> actionRecordIds(DynamicActionExecutionRequest request) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        collectId(ids, request.recordId());
        if (request.record() != null) {
            collectId(ids, request.record().getId());
        }
        request.ids().forEach(id -> collectId(ids, id));
        request.orderedIds().forEach(id -> collectId(ids, id));
        collectId(ids, request.beforeId());
        collectId(ids, request.afterId());
        collectId(ids, request.parentId());
        return java.util.Collections.unmodifiableSet(ids);
    }

    private ActionExecutionPolicy actionPolicy(DynamicActionDescriptor action) {
        return new ActionExecutionPolicy(
                action.code(),
                toPlatformLevel(action.actionLevel()),
                toAccessMode(action.accessMode()),
                action.actionAuth(),
                action.dataAuth(),
                action.defaultGrantPolicy(),
                action.authInheritActionCode()
        );
    }

    private PlatformActionLevel toPlatformLevel(net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel level) {
        if (level == null) {
            return PlatformActionLevel.DEFAULT;
        }
        return switch (level) {
            case LIST -> PlatformActionLevel.LIST;
            case RECORD -> PlatformActionLevel.RECORD;
            case BATCH -> PlatformActionLevel.BATCH;
            case ANY -> PlatformActionLevel.ANY;
        };
    }

    private ActionAccessMode toAccessMode(net.ximatai.muyun.spring.dynamic.metadata.EntityActionAccessMode accessMode) {
        if (accessMode == null) {
            return ActionAccessMode.AUTH_REQUIRED;
        }
        return switch (accessMode) {
            case AUTH_REQUIRED -> ActionAccessMode.AUTH_REQUIRED;
            case LOGIN_REQUIRED -> ActionAccessMode.LOGIN_REQUIRED;
            case ANONYMOUS_ALLOWED -> ActionAccessMode.ANONYMOUS_ALLOWED;
        };
    }

    private void collectId(Set<String> ids, String id) {
        if (id != null && !id.isBlank()) {
            ids.add(id.trim());
        }
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
                                                       String traceId,
                                                       ActionExecutionPolicy policy) {
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
                        actionOperations(moduleAlias, entityAlias, traceId, policy)));
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

    private DynamicActionOperations actionOperations(String moduleAlias,
                                                     String entityAlias,
                                                     String traceId,
                                                     ActionExecutionPolicy policy) {
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
                DataScopeCriteriaResult scope = requireActionRecordDataScope(moduleAlias, entityAlias, policy,
                        normalizeRecordId(record == null ? null : record.getId()));
                return withTenantScope(scope, () -> DynamicRecordService.this.update(moduleAlias, entityAlias, record,
                        RuntimeMutationSource.ACTION, traceId));
            }

            @Override
            public int delete(String id) {
                DataScopeCriteriaResult scope = requireActionRecordDataScope(moduleAlias, entityAlias, policy, normalizeRecordId(id));
                return withTenantScope(scope, () -> DynamicRecordService.this.delete(moduleAlias, entityAlias, id,
                        RuntimeMutationSource.ACTION, traceId));
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
        return executionContext(moduleAlias, entityAlias, action, request, availability, null, UUID.randomUUID().toString(), null);
    }

    private DynamicActionExecutionContext executionContext(String moduleAlias,
                                                           String entityAlias,
                                                           DynamicActionDescriptor action,
                                                           DynamicActionExecutionRequest request,
                                                           DynamicActionAvailability availability,
                                                           Object value,
                                                           String traceId,
                                                           ActionAuthorizationResult authorization) {
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
                TenantContext.systemReason().orElse(null),
                authorization == null ? null : authorization.operatorId(),
                authorization == null ? null : authorization.operatorType(),
                authorization == null ? null : authorization.decision(),
                authorization == null ? null : authorization.permissionCode(),
                authorization == null ? null : authorization.permissionActionCode(),
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

    private boolean supportsCapability(String moduleAlias, String entityAlias, EntityCapability capability) {
        return findEntity(describe(moduleAlias), entityAlias).capabilities().contains(capability.name());
    }

    private void requireCapability(String moduleAlias, String entityAlias, EntityCapability capability) {
        if (!supportsCapability(moduleAlias, entityAlias, capability)) {
            throw new PlatformException("dynamic entity does not support capability: " + capability);
        }
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
                TenantContext.systemReason().orElse(null),
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
