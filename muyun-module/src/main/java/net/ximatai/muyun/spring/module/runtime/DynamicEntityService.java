package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.ChildPlan;
import net.ximatai.muyun.spring.ability.CacheAbility;
import net.ximatai.muyun.spring.ability.ChildAbility;
import net.ximatai.muyun.spring.ability.ChildRelation;
import net.ximatai.muyun.spring.ability.ChildrenAbility;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.ReferenceAbility;
import net.ximatai.muyun.spring.ability.ReferenceCardinality;
import net.ximatai.muyun.spring.ability.ReferenceOption;
import net.ximatai.muyun.spring.ability.ReferencePlan;
import net.ximatai.muyun.spring.ability.ReferenceTarget;
import net.ximatai.muyun.spring.ability.ReferencerAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.model.EntityContract;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.module.metadata.EntityCapability;
import net.ximatai.muyun.spring.module.metadata.EntityReferenceDefinition;
import net.ximatai.muyun.spring.module.metadata.EntityRelationDefinition;
import net.ximatai.muyun.spring.module.metadata.ModuleDefinition;

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
        TreeAbility<DynamicRecord>,
        ReferenceAbility<DynamicRecord>,
        ReferencerAbility<DynamicRecord>,
        CacheAbility<DynamicRecord> {
    private final DynamicRecordDao dao;
    private final String moduleAlias;
    private final DynamicRecordLifecycle lifecycle;
    private final ModuleDefinition module;
    private final Function<String, DynamicEntityService> relationServiceResolver;
    private final String cacheNamespace;

    public DynamicEntityService(DynamicRecordDao dao, String moduleAlias) {
        this(dao, moduleAlias, DynamicRecordLifecycle.NONE);
    }

    public DynamicEntityService(DynamicRecordDao dao, String moduleAlias, DynamicRecordLifecycle lifecycle) {
        this(dao, moduleAlias, lifecycle, null, entityCode -> {
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
        this.dao = Objects.requireNonNull(dao, "dao must not be null");
        this.moduleAlias = requireModuleAlias(moduleAlias);
        this.lifecycle = lifecycle == null ? DynamicRecordLifecycle.NONE : lifecycle;
        this.module = module;
        this.relationServiceResolver = Objects.requireNonNull(relationServiceResolver, "relationServiceResolver must not be null");
        this.cacheNamespace = resolveCacheNamespace(cacheNamespacePrefix);
    }

    @Override
    public BaseDao<DynamicRecord, String> getDao() {
        return dao;
    }

    @Override
    public String getModuleAlias() {
        return moduleAlias;
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
        lifecycle.beforeInsert(record);
        validateChildPayload(record);
        record.validateForInsert();
    }

    @Override
    public void beforeUpdate(DynamicRecord record) {
        lifecycle.beforeUpdate(record);
        validateChildPayload(record);
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

    @Override
    public boolean shouldPrepareTreeDefault(DynamicRecord record) {
        return dao.getEntity().supports(EntityCapability.TREE);
    }

    @Override
    public boolean shouldPrepareEnabledDefault(DynamicRecord record) {
        return dao.getEntity().supports(EntityCapability.TREE);
    }

    public List<DynamicRecord> list(Criteria criteria, PageRequest pageRequest, Sort... sorts) {
        return getDao().query(activeCriteria(criteria), pageRequest, sorts);
    }

    public List<DynamicRecord> sortedList(Criteria criteria) {
        requireCapability(EntityCapability.SORT);
        return TreeAbility.super.sortedList(criteria);
    }

    public void reorder(List<String> orderedIds) {
        requireCapability(EntityCapability.SORT);
        TreeAbility.super.reorder(orderedIds);
    }

    public void moveBefore(String id, String beforeId) {
        requireCapability(EntityCapability.SORT);
        TreeAbility.super.moveBefore(id, beforeId);
    }

    public void moveAfter(String id, String afterId) {
        requireCapability(EntityCapability.SORT);
        TreeAbility.super.moveAfter(id, afterId);
    }

    @Override
    public List<DynamicRecord> children(String parentId) {
        requireCapability(EntityCapability.TREE);
        return TreeAbility.super.children(parentId);
    }

    @Override
    public List<String> ancestorIds(String id) {
        requireCapability(EntityCapability.TREE);
        return TreeAbility.super.ancestorIds(id);
    }

    @Override
    public List<String> ancestorIdsAndSelf(String id) {
        requireCapability(EntityCapability.TREE);
        return TreeAbility.super.ancestorIdsAndSelf(id);
    }

    @Override
    public List<String> descendantIds(String id) {
        requireCapability(EntityCapability.TREE);
        return TreeAbility.super.descendantIds(id);
    }

    @Override
    public void validateTreePlacement(DynamicRecord record) {
        if (dao.getEntity().supports(EntityCapability.TREE)) {
            TreeAbility.super.validateTreePlacement(record);
        }
    }

    @Override
    public Criteria sortScope(DynamicRecord record) {
        if (dao.getEntity().supports(EntityCapability.TREE)) {
            return TreeAbility.super.sortScope(record);
        }
        return Criteria.of();
    }

    @Override
    public void validateSortScope(DynamicRecord left, DynamicRecord right) {
        if (dao.getEntity().supports(EntityCapability.TREE)) {
            TreeAbility.super.validateSortScope(left, right);
        }
    }

    public String title(String id) {
        requireCapability(EntityCapability.REFERENCE);
        return ReferenceAbility.super.title(id);
    }

    @Override
    public DynamicRecord selectReferenceRaw(String id) {
        requireCapability(EntityCapability.REFERENCE);
        return activeRaw(id);
    }

    @Override
    public String referenceTitle(DynamicRecord entity) {
        requireCapability(EntityCapability.REFERENCE);
        return entity == null ? null : entity.getTitle();
    }

    public Map<String, String> titles(Collection<String> ids) {
        requireCapability(EntityCapability.REFERENCE);
        return ReferenceAbility.super.titles(ids);
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
        return ReferenceAbility.super.referenceOptions(criteria, pageRequest);
    }

    @Override
    public List<ChildRelation<? extends EntityContract, DynamicRecord>> childRelations() {
        if (module == null) {
            return List.of();
        }
        List<ChildRelation<? extends EntityContract, DynamicRecord>> relations = new ArrayList<>();
        for (EntityRelationDefinition relation : module.relations()) {
            if (dao.getEntity().code().equals(relation.parentEntity())) {
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
                .filter(reference -> dao.getEntity().code().equals(reference.sourceEntity()))
                .map(EntityReferenceDefinition::plan)
                .toList();
    }

    private DynamicEntityService referenceService(ReferenceTarget target) {
        if (!moduleAlias.equals(target.moduleAlias())) {
            throw new IllegalArgumentException("cross module dynamic reference title is not supported: " + target.qualifiedName());
        }
        return relationServiceResolver.apply(target.entityCode());
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
        for (net.ximatai.muyun.spring.ability.ReferenceProjection projection : plan.projections()) {
            record.putVirtualValue(projection.outputField(), referenceProjectionValue(ids, loaded, plan, projection.targetField()));
        }
    }

    private void clearReferenceProjectionValues(DynamicRecord record, ReferencePlan plan) {
        for (net.ximatai.muyun.spring.ability.ReferenceProjection projection : plan.projections()) {
            record.putVirtualValue(projection.outputField(), null);
        }
    }

    private List<String> projectionSourceFields(ReferencePlan plan) {
        return plan.projections().stream()
                .map(net.ximatai.muyun.spring.ability.ReferenceProjection::targetField)
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

    private DynamicRecord activeRaw(String id) {
        return getDao().query(activeCriteria(Criteria.of().eq(StandardEntitySchema.ID_FIELD, id)), new PageRequest(0, 1))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private void requireCapability(EntityCapability capability) {
        if (!dao.getEntity().supports(capability)) {
            throw new IllegalStateException("dynamic entity does not support capability: " + capability);
        }
    }

    private ChildRelation<DynamicRecord, DynamicRecord> toChildRelation(ChildPlan plan) {
        DynamicEntityService childService = relationServiceResolver.apply(plan.childEntity());
        ChildRelation<DynamicRecord, DynamicRecord> childRelation = new ChildRelation<>(
                childService,
                (child, parentId) -> child.setValue(plan.childForeignKeyField(), parentId),
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
            if (dao.getEntity().code().equals(relation.parentEntity())) {
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
                if (!relation.childEntity().equals(child.getEntity().code())) {
                    throw new IllegalArgumentException("dynamic child entity mismatch: " + child.getEntity().code());
                }
            }
        }
    }

    private void requireSameEntity(DynamicRecord record) {
        if (!dao.getEntity().code().equals(record.getEntity().code())) {
            throw new IllegalArgumentException("dynamic record entity mismatch: " + record.getEntity().code());
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
        String suffix = moduleAlias + "." + dao.getEntity().code();
        if (cacheNamespacePrefix != null && !cacheNamespacePrefix.isBlank()) {
            return cacheNamespacePrefix + "::" + suffix;
        }
        return suffix + "::" + System.identityHashCode(dao);
    }
}
