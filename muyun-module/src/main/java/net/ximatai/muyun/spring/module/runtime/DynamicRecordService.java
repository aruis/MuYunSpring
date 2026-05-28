package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.reference.ReferenceOption;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DynamicRecordService {
    private final DynamicRecordRuntime runtime;

    public DynamicRecordService(DynamicRecordRuntime runtime) {
        this.runtime = Objects.requireNonNull(runtime, "runtime must not be null");
    }

    public DynamicRecord newRecord(String moduleAlias, String entityCode) {
        return runtime.newRecord(moduleAlias, entityCode);
    }

    public String create(String moduleAlias, String entityCode, DynamicRecord record) {
        return entityService(moduleAlias, entityCode).insert(record);
    }

    public DynamicRecord select(String moduleAlias, String entityCode, String id) {
        return entityService(moduleAlias, entityCode).select(id);
    }

    public DynamicRecord selectIgnoreSoftDelete(String moduleAlias, String entityCode, String id) {
        return entityService(moduleAlias, entityCode).selectIgnoreSoftDelete(id);
    }

    public int update(String moduleAlias, String entityCode, DynamicRecord record) {
        return entityService(moduleAlias, entityCode).update(record);
    }

    public int delete(String moduleAlias, String entityCode, String id) {
        return entityService(moduleAlias, entityCode).delete(id);
    }

    public int deleteBatch(String moduleAlias, String entityCode, Collection<String> ids) {
        return entityService(moduleAlias, entityCode).deleteBatch(ids);
    }

    public List<DynamicRecord> list(String moduleAlias, String entityCode, Criteria criteria, PageRequest pageRequest, Sort... sorts) {
        return entityService(moduleAlias, entityCode).list(criteria, pageRequest, sorts);
    }

    public PageResult<DynamicRecord> page(String moduleAlias, String entityCode, Criteria criteria, PageRequest pageRequest, Sort... sorts) {
        return entityService(moduleAlias, entityCode).pageQuery(criteria, pageRequest, sorts);
    }

    public long count(String moduleAlias, String entityCode, Criteria criteria) {
        return entityService(moduleAlias, entityCode).count(criteria);
    }

    public List<DynamicRecord> sortedList(String moduleAlias, String entityCode, Criteria criteria) {
        return entityService(moduleAlias, entityCode).sortedList(criteria);
    }

    public void reorder(String moduleAlias, String entityCode, List<String> orderedIds) {
        entityService(moduleAlias, entityCode).reorder(orderedIds);
    }

    public void moveBefore(String moduleAlias, String entityCode, String id, String beforeId) {
        entityService(moduleAlias, entityCode).moveBefore(id, beforeId);
    }

    public void moveAfter(String moduleAlias, String entityCode, String id, String afterId) {
        entityService(moduleAlias, entityCode).moveAfter(id, afterId);
    }

    public List<DynamicRecord> children(String moduleAlias, String entityCode, String parentId) {
        return entityService(moduleAlias, entityCode).children(parentId);
    }

    public List<String> ancestorIds(String moduleAlias, String entityCode, String id) {
        return entityService(moduleAlias, entityCode).ancestorIds(id);
    }

    public List<String> ancestorIdsAndSelf(String moduleAlias, String entityCode, String id) {
        return entityService(moduleAlias, entityCode).ancestorIdsAndSelf(id);
    }

    public List<String> descendantIds(String moduleAlias, String entityCode, String id) {
        return entityService(moduleAlias, entityCode).descendantIds(id);
    }

    public int enable(String moduleAlias, String entityCode, String id) {
        return entityService(moduleAlias, entityCode).enable(id);
    }

    public int disable(String moduleAlias, String entityCode, String id) {
        return entityService(moduleAlias, entityCode).disable(id);
    }

    public boolean isEnabled(String moduleAlias, String entityCode, String id) {
        return entityService(moduleAlias, entityCode).isEnabled(id);
    }

    public Criteria enabledCriteria(String moduleAlias, String entityCode, Criteria criteria) {
        return entityService(moduleAlias, entityCode).enabledCriteria(criteria);
    }

    public String title(String moduleAlias, String entityCode, String id) {
        return entityService(moduleAlias, entityCode).title(id);
    }

    public Map<String, String> titles(String moduleAlias, String entityCode, Collection<String> ids) {
        return entityService(moduleAlias, entityCode).titles(ids);
    }

    public Map<String, Map<String, Object>> projections(String moduleAlias,
                                                        String entityCode,
                                                        Collection<String> ids,
                                                        Collection<String> fieldNames) {
        return entityService(moduleAlias, entityCode).projections(ids, fieldNames);
    }

    public PageResult<ReferenceOption> referenceOptions(String moduleAlias,
                                                        String entityCode,
                                                        Criteria criteria,
                                                        PageRequest pageRequest) {
        return entityService(moduleAlias, entityCode).referenceOptions(criteria, pageRequest);
    }

    private DynamicEntityService entityService(String moduleAlias, String entityCode) {
        return runtime.entityService(moduleAlias, entityCode);
    }
}
