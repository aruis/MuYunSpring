package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.reference.ReferenceOption;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicActionDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicAssociationViewDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicEntityDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicModuleDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicReferenceDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicRelationDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicViewDescriptor;
import net.ximatai.muyun.spring.dynamic.metadata.EntityViewType;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinitionException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DynamicRecordService {
    private final DynamicRecordRuntime runtime;

    public DynamicRecordService(DynamicRecordRuntime runtime) {
        this.runtime = Objects.requireNonNull(runtime, "runtime must not be null");
    }

    public DynamicRecord newRecord(String moduleAlias, String entityAlias) {
        return runtime.newRecord(moduleAlias, entityAlias);
    }

    public DynamicModuleDescriptor describe(String moduleAlias) {
        return runtime.describe(moduleAlias);
    }

    public ModuleOperations module(String moduleAlias) {
        return new ModuleOperations(this, moduleAlias);
    }

    public EntityOperations entity(String moduleAlias, String entityAlias) {
        return new EntityOperations(this, moduleAlias, entityAlias);
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
        return entityService(moduleAlias, entityAlias).insert(record);
    }

    public DynamicRecord select(String moduleAlias, String entityAlias, String id) {
        return entityService(moduleAlias, entityAlias).select(id);
    }

    public DynamicRecord selectIgnoreSoftDelete(String moduleAlias, String entityAlias, String id) {
        return entityService(moduleAlias, entityAlias).selectIgnoreSoftDelete(id);
    }

    public int update(String moduleAlias, String entityAlias, DynamicRecord record) {
        return entityService(moduleAlias, entityAlias).update(record);
    }

    public int delete(String moduleAlias, String entityAlias, String id) {
        return entityService(moduleAlias, entityAlias).delete(id);
    }

    public int deleteBatch(String moduleAlias, String entityAlias, Collection<String> ids) {
        return entityService(moduleAlias, entityAlias).deleteBatch(ids);
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
        entityService(moduleAlias, entityAlias).reorder(orderedIds);
    }

    public void moveBefore(String moduleAlias, String entityAlias, String id, String beforeId) {
        entityService(moduleAlias, entityAlias).moveBefore(id, beforeId);
    }

    public void moveAfter(String moduleAlias, String entityAlias, String id, String afterId) {
        entityService(moduleAlias, entityAlias).moveAfter(id, afterId);
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
        return entityService(moduleAlias, entityAlias).enable(id);
    }

    public int disable(String moduleAlias, String entityAlias, String id) {
        return entityService(moduleAlias, entityAlias).disable(id);
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

    private DynamicEntityService entityService(String moduleAlias, String entityAlias) {
        return runtime.entityService(moduleAlias, entityAlias);
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

        public EntityOperations entity(String entityAlias) {
            return service.entity(moduleAlias, entityAlias);
        }
    }

    public static final class EntityOperations {
        private final DynamicRecordService service;
        private final String moduleAlias;
        private final String entityAlias;

        private EntityOperations(DynamicRecordService service, String moduleAlias, String entityAlias) {
            this.service = service;
            this.moduleAlias = moduleAlias;
            this.entityAlias = entityAlias;
        }

        public DynamicRecord newRecord() {
            return service.newRecord(moduleAlias, entityAlias);
        }

        public DynamicEntityDescriptor describe() {
            return service.entityDescriptor(moduleAlias, entityAlias);
        }

        public List<DynamicActionDescriptor> actions() {
            return service.actions(moduleAlias, entityAlias);
        }

        public DynamicActionDescriptor action(String actionCode) {
            return service.action(moduleAlias, entityAlias, actionCode);
        }

        public DynamicActionAvailability actionAvailability(String actionCode, DynamicRecord record) {
            return service.actionAvailability(moduleAlias, entityAlias, actionCode, record);
        }

        public List<DynamicReferenceDescriptor> references() {
            return service.references(moduleAlias, entityAlias);
        }

        public DynamicReferenceDescriptor reference(String sourceField) {
            return service.reference(moduleAlias, entityAlias, sourceField);
        }

        public List<DynamicViewDescriptor> views() {
            return service.views(moduleAlias, entityAlias);
        }

        public DynamicViewDescriptor view(EntityViewType viewType) {
            return service.view(moduleAlias, entityAlias, viewType);
        }

        public List<DynamicAssociationViewDescriptor> associationViews() {
            return service.associationViews(moduleAlias, entityAlias);
        }

        public DynamicAssociationViewDescriptor associationView(String viewCode) {
            return service.associationView(moduleAlias, entityAlias, viewCode);
        }

        public String create(DynamicRecord record) {
            return service.create(moduleAlias, entityAlias, record);
        }

        public DynamicRecord select(String id) {
            return service.select(moduleAlias, entityAlias, id);
        }

        public DynamicRecord selectIgnoreSoftDelete(String id) {
            return service.selectIgnoreSoftDelete(moduleAlias, entityAlias, id);
        }

        public int update(DynamicRecord record) {
            return service.update(moduleAlias, entityAlias, record);
        }

        public int delete(String id) {
            return service.delete(moduleAlias, entityAlias, id);
        }

        public int deleteBatch(Collection<String> ids) {
            return service.deleteBatch(moduleAlias, entityAlias, ids);
        }

        public List<DynamicRecord> list(Criteria criteria, PageRequest pageRequest, Sort... sorts) {
            return service.list(moduleAlias, entityAlias, criteria, pageRequest, sorts);
        }

        public PageResult<DynamicRecord> page(Criteria criteria, PageRequest pageRequest, Sort... sorts) {
            return service.page(moduleAlias, entityAlias, criteria, pageRequest, sorts);
        }

        public long count(Criteria criteria) {
            return service.count(moduleAlias, entityAlias, criteria);
        }

        public List<DynamicRecord> sortedList(Criteria criteria) {
            return service.sortedList(moduleAlias, entityAlias, criteria);
        }

        public void reorder(List<String> orderedIds) {
            service.reorder(moduleAlias, entityAlias, orderedIds);
        }

        public void moveBefore(String id, String beforeId) {
            service.moveBefore(moduleAlias, entityAlias, id, beforeId);
        }

        public void moveAfter(String id, String afterId) {
            service.moveAfter(moduleAlias, entityAlias, id, afterId);
        }

        public List<DynamicRecord> children(String parentId) {
            return service.children(moduleAlias, entityAlias, parentId);
        }

        public List<String> ancestorIds(String id) {
            return service.ancestorIds(moduleAlias, entityAlias, id);
        }

        public List<String> ancestorIdsAndSelf(String id) {
            return service.ancestorIdsAndSelf(moduleAlias, entityAlias, id);
        }

        public List<String> descendantIds(String id) {
            return service.descendantIds(moduleAlias, entityAlias, id);
        }

        public int enable(String id) {
            return service.enable(moduleAlias, entityAlias, id);
        }

        public int disable(String id) {
            return service.disable(moduleAlias, entityAlias, id);
        }

        public boolean isEnabled(String id) {
            return service.isEnabled(moduleAlias, entityAlias, id);
        }

        public Criteria enabledCriteria(Criteria criteria) {
            return service.enabledCriteria(moduleAlias, entityAlias, criteria);
        }

        public Criteria queryCriteria(Collection<DynamicQueryCondition> conditions) {
            return service.queryCriteria(moduleAlias, entityAlias, conditions);
        }

        public String title(String id) {
            return service.title(moduleAlias, entityAlias, id);
        }

        public Map<String, String> titles(Collection<String> ids) {
            return service.titles(moduleAlias, entityAlias, ids);
        }

        public Map<String, Map<String, Object>> projections(Collection<String> ids, Collection<String> fieldNames) {
            return service.projections(moduleAlias, entityAlias, ids, fieldNames);
        }

        public PageResult<ReferenceOption> referenceOptions(Criteria criteria, PageRequest pageRequest) {
            return service.referenceOptions(moduleAlias, entityAlias, criteria, pageRequest);
        }

        public DynamicReferenceResolveResponse resolveReference(String sourceField, DynamicReferenceResolveRequest request) {
            return service.resolveReference(moduleAlias, entityAlias, sourceField, request);
        }

        public DynamicReferenceResolveResponse resolveFieldReference(String fieldName, DynamicReferenceResolveRequest request) {
            return service.resolveFieldReference(moduleAlias, entityAlias, fieldName, request);
        }
    }
}
