package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;

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
        return ability(moduleAlias, entityCode).insert(record);
    }

    public DynamicRecord select(String moduleAlias, String entityCode, String id) {
        return ability(moduleAlias, entityCode).select(id);
    }

    public int update(String moduleAlias, String entityCode, DynamicRecord record) {
        return ability(moduleAlias, entityCode).update(record);
    }

    public int delete(String moduleAlias, String entityCode, String id) {
        return ability(moduleAlias, entityCode).delete(id);
    }

    public List<DynamicRecord> list(String moduleAlias, String entityCode, Criteria criteria, PageRequest pageRequest, Sort... sorts) {
        return ability(moduleAlias, entityCode).list(criteria, pageRequest, sorts);
    }

    public PageResult<DynamicRecord> page(String moduleAlias, String entityCode, Criteria criteria, PageRequest pageRequest, Sort... sorts) {
        return ability(moduleAlias, entityCode).pageQuery(criteria, pageRequest, sorts);
    }

    public long count(String moduleAlias, String entityCode, Criteria criteria) {
        return ability(moduleAlias, entityCode).count(criteria);
    }

    public List<DynamicRecord> sortedList(String moduleAlias, String entityCode, Criteria criteria) {
        return ability(moduleAlias, entityCode).sortedList(criteria);
    }

    public void reorder(String moduleAlias, String entityCode, List<String> orderedIds) {
        ability(moduleAlias, entityCode).reorder(orderedIds);
    }

    public void moveBefore(String moduleAlias, String entityCode, String id, String beforeId) {
        ability(moduleAlias, entityCode).moveBefore(id, beforeId);
    }

    public void moveAfter(String moduleAlias, String entityCode, String id, String afterId) {
        ability(moduleAlias, entityCode).moveAfter(id, afterId);
    }

    public String title(String moduleAlias, String entityCode, String id) {
        return ability(moduleAlias, entityCode).title(id);
    }

    public Map<String, String> titles(String moduleAlias, String entityCode, Collection<String> ids) {
        return ability(moduleAlias, entityCode).titles(ids);
    }

    public PageResult<DynamicReferenceOption> referenceOptions(String moduleAlias,
                                                              String entityCode,
                                                              Criteria criteria,
                                                              PageRequest pageRequest) {
        return ability(moduleAlias, entityCode).referenceOptions(criteria, pageRequest);
    }

    private DynamicRecordAbility ability(String moduleAlias, String entityCode) {
        return runtime.ability(moduleAlias, entityCode);
    }
}
