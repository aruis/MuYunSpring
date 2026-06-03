package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceOption;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicActionDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicAssociationViewDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicEntityDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicReferenceDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicViewDescriptor;
import net.ximatai.muyun.spring.dynamic.metadata.EntityViewType;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class DynamicEntityOperations implements
        TreeAbility<DynamicRecord>,
        EnableAbility<DynamicRecord>,
        SoftDeleteAbility<DynamicRecord> {
    private final DynamicRecordService service;
    private final String moduleAlias;
    private final String entityAlias;

    DynamicEntityOperations(DynamicRecordService service, String moduleAlias, String entityAlias) {
        this.service = service;
        this.moduleAlias = moduleAlias;
        this.entityAlias = entityAlias;
    }

    @Override
    public BaseDao<DynamicRecord, String> getDao() {
        return service.entityService(moduleAlias, entityAlias).getDao();
    }

    @Override
    public String getModuleAlias() {
        return moduleAlias;
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

    public DynamicActionExecutionResult executeAction(String actionCode, DynamicActionExecutionRequest request) {
        return service.executeAction(moduleAlias, entityAlias, actionCode, request);
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

    @Override
    public String insert(DynamicRecord record) {
        return create(record);
    }

    @Override
    public DynamicRecord select(String id) {
        return service.select(moduleAlias, entityAlias, id);
    }

    @Override
    public DynamicRecord selectIgnoreSoftDelete(String id) {
        return service.selectIgnoreSoftDelete(moduleAlias, entityAlias, id);
    }

    @Override
    public int update(DynamicRecord record) {
        return service.update(moduleAlias, entityAlias, record);
    }

    @Override
    public int delete(String id) {
        return service.delete(moduleAlias, entityAlias, id);
    }

    @Override
    public int deleteBatch(Collection<String> ids) {
        return service.deleteBatch(moduleAlias, entityAlias, ids);
    }

    public List<DynamicRecord> list(Criteria criteria, PageRequest pageRequest, Sort... sorts) {
        return service.list(moduleAlias, entityAlias, criteria, pageRequest, sorts);
    }

    public PageResult<DynamicRecord> page(Criteria criteria, PageRequest pageRequest, Sort... sorts) {
        return service.page(moduleAlias, entityAlias, criteria, pageRequest, sorts);
    }

    @Override
    public PageResult<DynamicRecord> pageQuery(Criteria criteria, PageRequest pageRequest, Sort... sorts) {
        return page(criteria, pageRequest, sorts);
    }

    @Override
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

    @Override
    public int enable(String id) {
        return service.enable(moduleAlias, entityAlias, id);
    }

    @Override
    public int disable(String id) {
        return service.disable(moduleAlias, entityAlias, id);
    }

    @Override
    public boolean isEnabled(String id) {
        return service.isEnabled(moduleAlias, entityAlias, id);
    }

    @Override
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
