package net.ximatai.muyun.spring.dynamic.runtime;

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

    public EntityOperations entity(String moduleAlias, String entityCode) {
        return new EntityOperations(this, moduleAlias, entityCode);
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

    public Criteria queryCriteria(String moduleAlias, String entityCode, Collection<DynamicQueryCondition> conditions) {
        return entityService(moduleAlias, entityCode).queryCriteria(conditions);
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

    public DynamicReferenceResolveResponse resolveReference(String moduleAlias,
                                                            String entityCode,
                                                            String sourceField,
                                                            DynamicReferenceResolveRequest request) {
        return entityService(moduleAlias, entityCode).resolveReference(sourceField, request);
    }

    private DynamicEntityService entityService(String moduleAlias, String entityCode) {
        return runtime.entityService(moduleAlias, entityCode);
    }

    public static final class EntityOperations {
        private final DynamicRecordService service;
        private final String moduleAlias;
        private final String entityCode;

        private EntityOperations(DynamicRecordService service, String moduleAlias, String entityCode) {
            this.service = service;
            this.moduleAlias = moduleAlias;
            this.entityCode = entityCode;
        }

        public DynamicRecord newRecord() {
            return service.newRecord(moduleAlias, entityCode);
        }

        public String create(DynamicRecord record) {
            return service.create(moduleAlias, entityCode, record);
        }

        public DynamicRecord select(String id) {
            return service.select(moduleAlias, entityCode, id);
        }

        public DynamicRecord selectIgnoreSoftDelete(String id) {
            return service.selectIgnoreSoftDelete(moduleAlias, entityCode, id);
        }

        public int update(DynamicRecord record) {
            return service.update(moduleAlias, entityCode, record);
        }

        public int delete(String id) {
            return service.delete(moduleAlias, entityCode, id);
        }

        public int deleteBatch(Collection<String> ids) {
            return service.deleteBatch(moduleAlias, entityCode, ids);
        }

        public List<DynamicRecord> list(Criteria criteria, PageRequest pageRequest, Sort... sorts) {
            return service.list(moduleAlias, entityCode, criteria, pageRequest, sorts);
        }

        public PageResult<DynamicRecord> page(Criteria criteria, PageRequest pageRequest, Sort... sorts) {
            return service.page(moduleAlias, entityCode, criteria, pageRequest, sorts);
        }

        public long count(Criteria criteria) {
            return service.count(moduleAlias, entityCode, criteria);
        }

        public List<DynamicRecord> sortedList(Criteria criteria) {
            return service.sortedList(moduleAlias, entityCode, criteria);
        }

        public void reorder(List<String> orderedIds) {
            service.reorder(moduleAlias, entityCode, orderedIds);
        }

        public void moveBefore(String id, String beforeId) {
            service.moveBefore(moduleAlias, entityCode, id, beforeId);
        }

        public void moveAfter(String id, String afterId) {
            service.moveAfter(moduleAlias, entityCode, id, afterId);
        }

        public List<DynamicRecord> children(String parentId) {
            return service.children(moduleAlias, entityCode, parentId);
        }

        public List<String> ancestorIds(String id) {
            return service.ancestorIds(moduleAlias, entityCode, id);
        }

        public List<String> ancestorIdsAndSelf(String id) {
            return service.ancestorIdsAndSelf(moduleAlias, entityCode, id);
        }

        public List<String> descendantIds(String id) {
            return service.descendantIds(moduleAlias, entityCode, id);
        }

        public int enable(String id) {
            return service.enable(moduleAlias, entityCode, id);
        }

        public int disable(String id) {
            return service.disable(moduleAlias, entityCode, id);
        }

        public boolean isEnabled(String id) {
            return service.isEnabled(moduleAlias, entityCode, id);
        }

        public Criteria enabledCriteria(Criteria criteria) {
            return service.enabledCriteria(moduleAlias, entityCode, criteria);
        }

        public Criteria queryCriteria(Collection<DynamicQueryCondition> conditions) {
            return service.queryCriteria(moduleAlias, entityCode, conditions);
        }

        public String title(String id) {
            return service.title(moduleAlias, entityCode, id);
        }

        public Map<String, String> titles(Collection<String> ids) {
            return service.titles(moduleAlias, entityCode, ids);
        }

        public Map<String, Map<String, Object>> projections(Collection<String> ids, Collection<String> fieldNames) {
            return service.projections(moduleAlias, entityCode, ids, fieldNames);
        }

        public PageResult<ReferenceOption> referenceOptions(Criteria criteria, PageRequest pageRequest) {
            return service.referenceOptions(moduleAlias, entityCode, criteria, pageRequest);
        }

        public DynamicReferenceResolveResponse resolveReference(String sourceField, DynamicReferenceResolveRequest request) {
            return service.resolveReference(moduleAlias, entityCode, sourceField, request);
        }
    }
}
