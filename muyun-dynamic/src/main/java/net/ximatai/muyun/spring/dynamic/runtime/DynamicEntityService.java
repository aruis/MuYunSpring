package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.child.ChildPlan;
import net.ximatai.muyun.spring.ability.CacheAbility;
import net.ximatai.muyun.spring.ability.child.ChildAbility;
import net.ximatai.muyun.spring.ability.child.ChildRelation;
import net.ximatai.muyun.spring.ability.child.ChildrenAbility;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceCardinality;
import net.ximatai.muyun.spring.ability.reference.ReferenceOption;
import net.ximatai.muyun.spring.ability.reference.ReferencePlan;
import net.ximatai.muyun.spring.ability.reference.ReferenceTarget;
import net.ximatai.muyun.spring.ability.reference.ReferencerAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.formula.FormulaRuntimeReport;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionKind;
import net.ximatai.muyun.spring.dynamic.metadata.EntityCapability;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityReferenceDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityRelationDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityStandardActionCatalog;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinitionException;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public class DynamicEntityService implements
        CrudAbility<DynamicRecord>,
        SoftDeleteAbility<DynamicRecord>,
        ChildAbility<DynamicRecord>,
        ChildrenAbility<DynamicRecord>,
        ReferencerAbility<DynamicRecord>,
        CacheAbility<DynamicRecord> {
    private final DynamicRecordDao dao;
    private final String moduleAlias;
    private final DynamicRecordLifecycle lifecycle;
    private final ModuleDefinition module;
    private final Function<String, DynamicEntityService> relationServiceResolver;
    private final String cacheNamespace;
    private final DynamicFieldValueValidator fieldValueValidator;

    public DynamicEntityService(DynamicRecordDao dao, String moduleAlias) {
        this(dao, moduleAlias, DynamicRecordLifecycle.NONE);
    }

    public DynamicEntityService(DynamicRecordDao dao, String moduleAlias, DynamicRecordLifecycle lifecycle) {
        this(dao, moduleAlias, lifecycle, null, entityAlias -> {
            throw new IllegalStateException("dynamic relation service resolver is not configured");
        });
    }

    public DynamicEntityService(DynamicRecordDao dao,
                                String moduleAlias,
                                DynamicRecordLifecycle lifecycle,
                                ModuleDefinition module,
                                Function<String, DynamicEntityService> relationServiceResolver) {
        this(dao, moduleAlias, lifecycle, module, relationServiceResolver, null);
    }

    public DynamicEntityService(DynamicRecordDao dao,
                                String moduleAlias,
                                DynamicRecordLifecycle lifecycle,
                                ModuleDefinition module,
                                Function<String, DynamicEntityService> relationServiceResolver,
                                String cacheNamespacePrefix) {
        this(dao, moduleAlias, lifecycle, module, relationServiceResolver, cacheNamespacePrefix, DynamicFieldValueValidator.NONE);
    }

    public DynamicEntityService(DynamicRecordDao dao,
                                String moduleAlias,
                                DynamicRecordLifecycle lifecycle,
                                ModuleDefinition module,
                                Function<String, DynamicEntityService> relationServiceResolver,
                                String cacheNamespacePrefix,
                                DynamicFieldValueValidator fieldValueValidator) {
        this.dao = Objects.requireNonNull(dao, "dao must not be null");
        this.moduleAlias = requireModuleAlias(moduleAlias);
        this.lifecycle = lifecycle == null ? DynamicRecordLifecycle.NONE : lifecycle;
        this.module = module;
        this.relationServiceResolver = Objects.requireNonNull(relationServiceResolver, "relationServiceResolver must not be null");
        this.cacheNamespace = resolveCacheNamespace(cacheNamespacePrefix);
        this.fieldValueValidator = Objects.requireNonNull(fieldValueValidator, "fieldValueValidator must not be null");
    }

    @Override
    public BaseDao<DynamicRecord, String> getDao() {
        return dao;
    }

    DynamicRecordDao dynamicDao() {
        return dao;
    }

    @Override
    public String getModuleAlias() {
        return moduleAlias;
    }

    public ReferenceTarget referenceTarget() {
        return ReferenceTarget.of(moduleAlias, dao.getEntity().alias());
    }

    public DynamicActionAvailability actionAvailability(String actionCode, DynamicRecord record) {
        if (record != null) {
            requireSameEntity(record);
        }
        EntityActionDefinition action = actionDefinition(actionCode);
        if (!action.enabled()) {
            return DynamicActionAvailability.unavailable(action.actionCode(), disabledActionMessage(action));
        }
        if (!action.hasAvailabilityCondition()) {
            return DynamicActionAvailability.available(action.actionCode());
        }
        DynamicRecord existing = record != null && record.getId() != null && !record.getId().isBlank()
                ? activeRaw(record.getId())
                : null;
        return new DynamicActionAvailabilityRuntime(dao.getEntity(), module).evaluate(action, record, existing);
    }

    @Override
    public String cacheNamespace() {
        return cacheNamespace;
    }

    @Override
    public DynamicRecord copyForCache(DynamicRecord entity) {
        return entity == null ? null : entity.copy();
    }

    @Override
    public void beforeInsert(DynamicRecord record) {
        rejectWriteProtectedFields(record);
        record.applyDefaultsForInsert();
        prepareDynamicAbilityDefaults(record);
        lifecycle.beforeInsert(record);
        record.formulaReport(formulaRuntime().beforeInsert(record));
        validateChildPayload(record);
        record.validateForInsert();
        validateFieldValues(record);
        validateReferenceValues(record);
        validateTreePlacement(record);
    }

    @Override
    public void beforeUpdate(DynamicRecord record) {
        rejectWriteProtectedFields(record);
        lifecycle.beforeUpdate(record);
        DynamicFormulaRuntime formulaRuntime = formulaRuntime();
        if (formulaRuntime.hasBeforeUpdateRules(record)) {
            FormulaRuntimeReport report = formulaRuntime.beforeUpdate(
                    record,
                    activeRaw(record.getId()),
                    existingChildrenForFormula(record)
            );
            record.formulaReport(report);
        } else {
            record.formulaReport(new FormulaRuntimeReport());
        }
        validateChildPayload(record);
        record.validateForUpdate();
        validateFieldValues(record);
        validateReferenceValues(record);
        validateTreePlacement(record);
    }

    @Override
    public void beforeDelete(String id) {
        lifecycle.beforeDelete(id);
    }

    @Override
    public void afterSelect(DynamicRecord record) {
        lifecycle.afterSelect(record);
    }

    @Override
    public void afterReferenceSelect(DynamicRecord record) {
        populateReferenceTitles(record);
    }

    @Override
    public void afterChanged(DynamicRecord record) {
        if (dao.getEntity().supports(EntityCapability.REFERENCE) && record != null && record.getId() != null) {
            referenceRuntime().clearReferenceReferrers(record.getId());
        }
    }

    @Override
    public Integer expectedVersionForUpdate(DynamicRecord record) {
        if (record.getVersion() != null) {
            return record.getVersion();
        }
        DynamicRecord current = activeRaw(record.getId());
        if (current == null) {
            throw new IllegalArgumentException("dynamic record not found: " + record.getId());
        }
        return current.getVersion();
    }

    private void prepareDynamicAbilityDefaults(DynamicRecord record) {
        if (dao.getEntity().supports(EntityCapability.TREE)
                && (record.parentId() == null || record.parentId().isBlank())) {
            record.parentId(DynamicTreeRuntime.ROOT_ID);
        }
        if (dao.getEntity().supports(EntityCapability.ENABLE) && record.enabled() == null) {
            record.enabled(Boolean.TRUE);
        }
        if (dao.getEntity().supports(EntityCapability.SORT) && record.sortOrder() == null) {
            record.sortOrder(nextSortOrder(record));
        }
    }

    private int nextSortOrder(DynamicRecord record) {
        Criteria scope = sortScope(record);
        int maxOrder = sortedList(scope).stream()
                .map(DynamicRecord::sortOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
        if (maxOrder > Integer.MAX_VALUE - SortAbility.SORT_STEP) {
            List<String> orderedIds = sortedList(scope).stream()
                    .map(DynamicRecord::getId)
                    .toList();
            if (!orderedIds.isEmpty()) {
                reorder(orderedIds);
                maxOrder = sortedList(scope).stream()
                        .map(DynamicRecord::sortOrder)
                        .filter(Objects::nonNull)
                        .max(Integer::compareTo)
                        .orElse(0);
            }
        }
        if (maxOrder > Integer.MAX_VALUE - SortAbility.SORT_STEP) {
            throw new PlatformException("Cannot allocate dynamic sort order; sort scope is too large");
        }
        return maxOrder + SortAbility.SORT_STEP;
    }

    public int enable(String id) {
        requireCapability(EntityCapability.ENABLE);
        return updateEnabled(id, Boolean.TRUE);
    }

    public int disable(String id) {
        requireCapability(EntityCapability.ENABLE);
        return updateEnabled(id, Boolean.FALSE);
    }

    public boolean isEnabled(String id) {
        requireCapability(EntityCapability.ENABLE);
        DynamicRecord entity = selectActiveRaw(id);
        return entity != null && Boolean.TRUE.equals(entity.enabled());
    }

    public Criteria enabledCriteria(Criteria criteria) {
        requireCapability(EntityCapability.ENABLE);
        Criteria scoped = Criteria.of();
        if (criteria != null && !criteria.isEmpty()) {
            scoped.andGroup(criteria.getRoot());
        }
        scoped.eq(PlatformAbilityFields.ENABLED_FIELD, Boolean.TRUE);
        return scoped;
    }

    public Criteria queryCriteria(Collection<DynamicQueryCondition> conditions) {
        return new DynamicQueryCriteriaBuilder(dao.getEntity()).build(conditions);
    }

    public List<DynamicRecord> sortedList(Criteria criteria) {
        requireCapability(EntityCapability.SORT);
        if (dao.getEntity().supports(EntityCapability.TREE)) {
            return treeRuntime().sortedList(criteria).stream().map(DynamicTreeRecord::record).toList();
        }
        return sortRuntime().sortedList(criteria).stream().map(DynamicSortRecord::record).toList();
    }

    public void reorder(List<String> orderedIds) {
        requireCapability(EntityCapability.SORT);
        if (dao.getEntity().supports(EntityCapability.TREE)) {
            treeRuntime().reorder(orderedIds);
            return;
        }
        sortRuntime().reorder(orderedIds);
    }

    public void moveBefore(String id, String beforeId) {
        requireCapability(EntityCapability.SORT);
        if (dao.getEntity().supports(EntityCapability.TREE)) {
            treeRuntime().moveBefore(id, beforeId);
            return;
        }
        sortRuntime().moveBefore(id, beforeId);
    }

    public void moveAfter(String id, String afterId) {
        requireCapability(EntityCapability.SORT);
        if (dao.getEntity().supports(EntityCapability.TREE)) {
            treeRuntime().moveAfter(id, afterId);
            return;
        }
        sortRuntime().moveAfter(id, afterId);
    }

    public List<DynamicRecord> children(String parentId) {
        requireCapability(EntityCapability.TREE);
        return treeRuntime().children(parentId).stream().map(DynamicTreeRecord::record).toList();
    }

    @Override
    public List<DynamicRecord> selectChildRows(Criteria criteria) {
        List<DynamicRecord> rows;
        if (dao.getEntity().supports(EntityCapability.SORT)) {
            rows = sortedList(criteria);
        } else {
            rows = ChildAbility.super.selectChildRows(criteria);
        }
        rows.forEach(this::afterReferenceSelect);
        rows.forEach(this::refreshReferenceDependencies);
        return rows;
    }

    public List<String> ancestorIds(String id) {
        requireCapability(EntityCapability.TREE);
        return treeRuntime().ancestorIds(id);
    }

    public List<String> ancestorIdsAndSelf(String id) {
        requireCapability(EntityCapability.TREE);
        return treeRuntime().ancestorIdsAndSelf(id);
    }

    public List<String> descendantIds(String id) {
        requireCapability(EntityCapability.TREE);
        return treeRuntime().descendantIds(id);
    }

    public void validateTreePlacement(DynamicRecord record) {
        if (dao.getEntity().supports(EntityCapability.TREE)) {
            treeRuntime().validateTreePlacement(new DynamicTreeRecord(record));
        }
    }

    public Criteria sortScope(DynamicRecord record) {
        if (dao.getEntity().supports(EntityCapability.TREE)) {
            return treeRuntime().sortScope(new DynamicTreeRecord(record));
        }
        return Criteria.of();
    }

    public void validateSortScope(DynamicRecord left, DynamicRecord right) {
        if (dao.getEntity().supports(EntityCapability.TREE)) {
            treeRuntime().validateSortScope(new DynamicTreeRecord(left), new DynamicTreeRecord(right));
        }
    }

    public String title(String id) {
        requireCapability(EntityCapability.REFERENCE);
        return referenceRuntime().title(id);
    }

    public String referenceTitle(DynamicRecord entity) {
        requireCapability(EntityCapability.REFERENCE);
        return entity == null ? null : entity.title();
    }

    public Map<String, String> titles(Collection<String> ids) {
        requireCapability(EntityCapability.REFERENCE);
        return referenceRuntime().titles(ids);
    }

    public Map<String, Map<String, Object>> projections(Collection<String> ids, Collection<String> fieldNames) {
        requireCapability(EntityCapability.REFERENCE);
        if (ids == null || ids.isEmpty() || fieldNames == null || fieldNames.isEmpty()) {
            return Map.of();
        }
        LinkedHashSet<String> normalizedIds = new LinkedHashSet<>(ids);
        LinkedHashSet<String> normalizedFields = new LinkedHashSet<>(fieldNames);
        List<DynamicRecord> records = getDao().query(
                activeCriteria(Criteria.of().in(StandardEntitySchema.ID_FIELD, List.copyOf(normalizedIds))),
                new PageRequest(0, Integer.MAX_VALUE)
        );
        Map<String, Map<String, Object>> loaded = new LinkedHashMap<>();
        for (DynamicRecord record : records) {
            Map<String, Object> values = new LinkedHashMap<>();
            for (String fieldName : normalizedFields) {
                values.put(fieldName, record.getValue(fieldName));
            }
            loaded.put(record.getId(), Collections.unmodifiableMap(new LinkedHashMap<>(values)));
        }
        Map<String, Map<String, Object>> ordered = new LinkedHashMap<>();
        for (String id : normalizedIds) {
            if (loaded.containsKey(id)) {
                ordered.put(id, loaded.get(id));
            }
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(ordered));
    }

    public PageResult<ReferenceOption> referenceOptions(Criteria criteria, PageRequest pageRequest) {
        requireCapability(EntityCapability.REFERENCE);
        return referenceRuntime().referenceOptions(criteria, pageRequest);
    }

    public DynamicReferenceResolveResponse resolveReference(String sourceField,
                                                            DynamicReferenceResolveRequest request) {
        ReferencePlan plan = referencePlan(sourceField);
        return new DynamicReferenceResolver(this, plan, referenceService(plan.target())).resolve(request);
    }

    @Override
    public List<ChildRelation<? extends EntityContract, DynamicRecord>> childRelations() {
        if (module == null) {
            return List.of();
        }
        List<ChildRelation<? extends EntityContract, DynamicRecord>> relations = new ArrayList<>();
        for (EntityRelationDefinition relation : module.relations()) {
            if (dao.getEntity().alias().equals(relation.parentEntityAlias())) {
                relations.add(toChildRelation(relation.plan()));
            }
        }
        return List.copyOf(relations);
    }

    @Override
    public Map<ReferenceTarget, Set<String>> collectReferenceIdsByTarget(DynamicRecord record) {
        if (record == null || module == null) {
            return Map.of();
        }
        requireSameEntity(record);
        Map<ReferenceTarget, Set<String>> ids = new LinkedHashMap<>();
        for (ReferencePlan plan : referencePlans()) {
            Object value = record.getValue(plan.sourceField());
            List<String> values = plan.normalizeValues(value);
            if (!values.isEmpty()) {
                ids.computeIfAbsent(plan.target(), ignored -> new LinkedHashSet<>())
                        .addAll(values);
            }
        }
        Map<ReferenceTarget, Set<String>> copy = new LinkedHashMap<>();
        ids.forEach((target, values) -> copy.put(target, Collections.unmodifiableSet(new LinkedHashSet<>(values))));
        return Collections.unmodifiableMap(copy);
    }

    private void populateReferenceTitles(DynamicRecord record) {
        if (record == null || module == null) {
            return;
        }
        requireSameEntity(record);
        for (ReferencePlan plan : referencePlans()) {
            if (!plan.autoTitle() && plan.projections().isEmpty()) {
                continue;
            }
            List<String> ids = plan.normalizeValues(record.getValue(plan.sourceField()));
            if (ids.isEmpty()) {
                if (plan.autoTitle()) {
                    record.putVirtualValue(plan.titleOutputField(), null);
                }
                clearReferenceProjectionValues(record, plan);
                continue;
            }
            DynamicEntityService targetService = referenceService(plan.target());
            if (plan.autoTitle()) {
                Map<String, String> titles = targetService.titles(ids);
                record.putVirtualValue(plan.titleOutputField(), referenceTitleValue(ids, titles, plan));
            }
            populateReferenceProjectionValues(record, targetService, ids, plan);
        }
    }

    private List<ReferencePlan> referencePlans() {
        if (module == null) {
            return List.of();
        }
        return module.references().stream()
                .filter(reference -> dao.getEntity().alias().equals(reference.sourceEntityAlias()))
                .map(EntityReferenceDefinition::plan)
                .toList();
    }

    private ReferencePlan referencePlan(String sourceField) {
        if (sourceField == null || sourceField.isBlank()) {
            throw new ModuleDefinitionException("reference sourceField must not be blank");
        }
        return referencePlans().stream()
                .filter(plan -> sourceField.equals(plan.sourceField()))
                .findFirst()
                .orElseThrow(() -> new ModuleDefinitionException("unknown dynamic reference: "
                        + moduleAlias + "." + dao.getEntity().alias() + "." + sourceField));
    }

    void requireSameEntityAliasForReference(ReferencePlan plan) {
        if (plan == null) {
            throw new IllegalArgumentException("reference plan must not be null");
        }
        referencePlan(plan.sourceField());
    }

    private DynamicEntityService referenceService(ReferenceTarget target) {
        if (!moduleAlias.equals(target.moduleAlias())) {
            throw new IllegalArgumentException("cross module dynamic reference title is not supported: " + target.qualifiedName());
        }
        return relationServiceResolver.apply(target.entityAlias());
    }

    private Object referenceTitleValue(List<String> ids, Map<String, String> titles, ReferencePlan plan) {
        if (plan.cardinality() == ReferenceCardinality.MANY) {
            return ids.stream()
                    .map(titles::get)
                    .filter(Objects::nonNull)
                    .toList();
        }
        return titles.get(ids.getFirst());
    }

    private void populateReferenceProjectionValues(DynamicRecord record,
                                                   DynamicEntityService targetService,
                                                   List<String> ids,
                                                   ReferencePlan plan) {
        if (plan.projections().isEmpty()) {
            return;
        }
        Map<String, Map<String, Object>> loaded = targetService.projections(ids, projectionSourceFields(plan));
        for (net.ximatai.muyun.spring.ability.reference.ReferenceProjection projection : plan.projections()) {
            record.putVirtualValue(projection.outputField(), referenceProjectionValue(ids, loaded, plan, projection.targetField()));
        }
    }

    private void clearReferenceProjectionValues(DynamicRecord record, ReferencePlan plan) {
        for (net.ximatai.muyun.spring.ability.reference.ReferenceProjection projection : plan.projections()) {
            record.putVirtualValue(projection.outputField(), null);
        }
    }

    private List<String> projectionSourceFields(ReferencePlan plan) {
        return plan.projections().stream()
                .map(net.ximatai.muyun.spring.ability.reference.ReferenceProjection::targetField)
                .distinct()
                .toList();
    }

    private Object referenceProjectionValue(List<String> ids,
                                            Map<String, Map<String, Object>> loaded,
                                            ReferencePlan plan,
                                            String sourceField) {
        if (plan.cardinality() == ReferenceCardinality.MANY) {
            return ids.stream()
                    .map(id -> fieldValue(loaded, id, sourceField))
                    .filter(Objects::nonNull)
                    .toList();
        }
        return fieldValue(loaded, ids.getFirst(), sourceField);
    }

    private Object fieldValue(Map<String, Map<String, Object>> loaded, String id, String sourceField) {
        Map<String, Object> fields = loaded.get(id);
        return fields == null ? null : fields.get(sourceField);
    }

    DynamicRecord activeRaw(String id) {
        return getDao().query(activeCriteria(Criteria.of().eq(StandardEntitySchema.ID_FIELD, id)), new PageRequest(0, 1))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private DynamicTreeRuntime treeRuntime() {
        return new DynamicTreeRuntime(this);
    }

    private DynamicSortRuntime sortRuntime() {
        return new DynamicSortRuntime(this);
    }

    private DynamicReferenceRuntime referenceRuntime() {
        return new DynamicReferenceRuntime(this);
    }

    private DynamicFormulaRuntime formulaRuntime() {
        return new DynamicFormulaRuntime(moduleAlias, dao.getEntity(), module);
    }

    private EntityActionDefinition actionDefinition(String actionCode) {
        if (actionCode == null || actionCode.isBlank()) {
            throw new IllegalArgumentException("dynamic action code must not be blank");
        }
        EntityDefinition entity = dao.getEntity();
        EntityActionDefinition configured = configuredAction(actionCode);
        if (configured != null) {
            return configured;
        }
        EntityActionKind standardKind = EntityStandardActionCatalog.standardKind(entity, actionCode);
        if (standardKind == null) {
            throw new IllegalArgumentException("unknown dynamic action: " + moduleAlias + "." + entity.alias() + "." + actionCode);
        }
        return EntityStandardActionCatalog.from(entity).stream()
                .filter(action -> action.actionCode().equals(actionCode))
                .findFirst()
                .orElseThrow();
    }

    private String disabledActionMessage(EntityActionDefinition action) {
        return action.unavailableMessage() == null ? "action is disabled" : action.unavailableMessage();
    }

    private EntityActionDefinition configuredAction(String actionCode) {
        if (module == null) {
            return null;
        }
        return module.actions().stream()
                .filter(action -> dao.getEntity().alias().equals(action.entityAlias()))
                .filter(action -> action.actionCode().equals(actionCode))
                .findFirst()
                .orElse(null);
    }

    private Map<String, List<DynamicRecord>> existingChildrenForFormula(DynamicRecord record) {
        if (module == null || record == null || record.getId() == null || record.getId().isBlank()) {
            return Map.of();
        }
        Map<String, List<DynamicRecord>> values = new LinkedHashMap<>();
        for (EntityRelationDefinition relation : module.relations()) {
            if (!dao.getEntity().alias().equals(relation.parentEntityAlias())
                    || !record.getChildren().containsKey(relation.code())
                    || record.getChildren(relation.code()) == null) {
                continue;
            }
            DynamicEntityService childService = relationServiceResolver.apply(relation.childEntityAlias());
            values.put(relation.code(), childService.selectChildRows(
                    Criteria.of().eq(relation.childForeignKeyField(), record.getId())
            ));
        }
        return values;
    }

    private void requireCapability(EntityCapability capability) {
        if (!dao.getEntity().supports(capability)) {
            throw new PlatformException("dynamic entity does not support capability: " + capability);
        }
    }

    private int updateEnabled(String id, Boolean enabled) {
        DynamicRecord entity = selectActiveRaw(id);
        if (entity == null) {
            return 0;
        }
        entity.enabled(enabled);
        return update(entity);
    }

    private ChildRelation<DynamicRecord, DynamicRecord> toChildRelation(ChildPlan plan) {
        DynamicEntityService childService = relationServiceResolver.apply(plan.childEntityAlias());
        ChildRelation<DynamicRecord, DynamicRecord> childRelation = new ChildRelation<>(
                childService,
                (child, parentId) -> child.putPlatformValue(plan.childForeignKeyField(), parentId),
                plan.childForeignKeyField(),
                parent -> parent.getChildren(plan.relationCode())
        );
        if (plan.autoPopulate()) {
            childRelation.autoPopulate((parent, children) -> parent.setChildren(plan.relationCode(), children));
        }
        if (plan.autoDeleteWithParent()) {
            childRelation.autoDeleteWithParent();
        }
        return childRelation;
    }

    private void validateChildPayload(DynamicRecord record) {
        requireSameEntity(record);
        if (module == null || record.getChildren().isEmpty()) {
            return;
        }
        Map<String, EntityRelationDefinition> relations = new LinkedHashMap<>();
        for (EntityRelationDefinition relation : module.relations()) {
            if (dao.getEntity().alias().equals(relation.parentEntityAlias())) {
                relations.put(relation.code(), relation);
            }
        }
        for (Map.Entry<String, List<DynamicRecord>> entry : record.getChildren().entrySet()) {
            EntityRelationDefinition relation = relations.get(entry.getKey());
            if (relation == null) {
                throw new IllegalArgumentException("unknown dynamic child relation: " + entry.getKey());
            }
            List<DynamicRecord> children = entry.getValue();
            if (children == null) {
                continue;
            }
            for (DynamicRecord child : children) {
                if (!relation.childEntityAlias().equals(child.getEntity().alias())) {
                    throw new IllegalArgumentException("dynamic child entity mismatch: " + child.getEntity().alias());
                }
            }
        }
    }

    private void validateFieldValues(DynamicRecord record) {
        requireSameEntity(record);
        for (FieldDefinition field : dao.getEntity().fields()) {
            if (record.getValues().containsKey(field.code())) {
                fieldValueValidator.validate(moduleAlias, dao.getEntity(), field, record.getValue(field.code()));
            }
        }
    }

    private void validateReferenceValues(DynamicRecord record) {
        requireSameEntity(record);
        if (module == null) {
            return;
        }
        Map<String, FieldDefinition> fields = dao.getEntity().fields().stream()
                .collect(java.util.stream.Collectors.toMap(FieldDefinition::fieldName, Function.identity()));
        for (ReferencePlan plan : referencePlans()) {
            if (!record.isExplicitlySet(plan.sourceField())) {
                continue;
            }
            FieldDefinition field = fields.get(plan.sourceField());
            List<String> ids = plan.normalizeValues(record.getValue(plan.sourceField()));
            if (field != null && field.isRequired() && ids.isEmpty()) {
                throw new IllegalArgumentException("required dynamic reference field must not be blank: " + plan.sourceField());
            }
            validateReferenceIds(plan, ids);
        }
    }

    private void validateReferenceIds(ReferencePlan plan, List<String> ids) {
        if (ids.isEmpty() || !moduleAlias.equals(plan.target().moduleAlias())) {
            return;
        }
        DynamicEntityService targetService = relationServiceResolver.apply(plan.target().entityAlias());
        for (String id : ids) {
            if (targetService.activeRaw(id) == null) {
                throw new IllegalArgumentException("dynamic reference target not found: "
                        + plan.target().qualifiedName() + "." + id);
            }
        }
    }

    private void rejectWriteProtectedFields(DynamicRecord record) {
        requireSameEntity(record);
        for (FieldDefinition field : dao.getEntity().fields()) {
            if (field.behavior().writeProtected() && record.isExplicitlySet(field.code())) {
                throw new IllegalArgumentException("dynamic field is write protected: " + field.code());
            }
        }
    }

    private void requireSameEntity(DynamicRecord record) {
        if (!dao.getEntity().alias().equals(record.getEntity().alias())) {
            throw new IllegalArgumentException("dynamic record entity mismatch: " + record.getEntity().alias());
        }
    }

    private String requireModuleAlias(String value) {
        Objects.requireNonNull(value, "moduleAlias must not be null");
        if (!value.contains(".")) {
            throw new IllegalArgumentException("dynamic moduleAlias must be a platform module alias: " + value);
        }
        return value;
    }

    private String resolveCacheNamespace(String cacheNamespacePrefix) {
        String suffix = moduleAlias + "." + dao.getEntity().alias();
        if (cacheNamespacePrefix != null && !cacheNamespacePrefix.isBlank()) {
            return cacheNamespacePrefix + "::" + suffix;
        }
        return suffix + "::" + System.identityHashCode(dao);
    }
}
