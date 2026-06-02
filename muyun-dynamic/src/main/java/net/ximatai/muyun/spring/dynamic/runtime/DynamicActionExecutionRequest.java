package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;

import java.util.Collection;
import java.util.List;

public record DynamicActionExecutionRequest(
        String recordId,
        DynamicRecord record,
        Collection<String> ids,
        List<String> orderedIds,
        String beforeId,
        String afterId,
        String parentId,
        Criteria criteria,
        PageRequest pageRequest,
        List<Sort> sorts,
        Collection<DynamicQueryCondition> queryConditions,
        Collection<String> fieldNames
) {
    public DynamicActionExecutionRequest {
        ids = ids == null ? List.of() : List.copyOf(ids);
        orderedIds = orderedIds == null ? List.of() : List.copyOf(orderedIds);
        sorts = sorts == null ? List.of() : List.copyOf(sorts);
        queryConditions = queryConditions == null ? List.of() : List.copyOf(queryConditions);
        fieldNames = fieldNames == null ? List.of() : List.copyOf(fieldNames);
    }

    public static DynamicActionExecutionRequest empty() {
        return new DynamicActionExecutionRequest(null, null, List.of(), List.of(), null, null, null,
                null, null, List.of(), List.of(), List.of());
    }

    public static DynamicActionExecutionRequest id(String id) {
        return empty().withRecordId(id);
    }

    public static DynamicActionExecutionRequest record(DynamicRecord record) {
        return empty().withRecord(record);
    }

    public DynamicActionExecutionRequest withRecordId(String recordId) {
        return new DynamicActionExecutionRequest(recordId, record, ids, orderedIds, beforeId, afterId, parentId,
                criteria, pageRequest, sorts, queryConditions, fieldNames);
    }

    public DynamicActionExecutionRequest withRecord(DynamicRecord record) {
        return new DynamicActionExecutionRequest(recordId, record, ids, orderedIds, beforeId, afterId, parentId,
                criteria, pageRequest, sorts, queryConditions, fieldNames);
    }

    public DynamicActionExecutionRequest withIds(Collection<String> ids) {
        return new DynamicActionExecutionRequest(recordId, record, ids, orderedIds, beforeId, afterId, parentId,
                criteria, pageRequest, sorts, queryConditions, fieldNames);
    }

    public DynamicActionExecutionRequest withOrderedIds(List<String> orderedIds) {
        return new DynamicActionExecutionRequest(recordId, record, ids, orderedIds, beforeId, afterId, parentId,
                criteria, pageRequest, sorts, queryConditions, fieldNames);
    }

    public DynamicActionExecutionRequest withBeforeId(String beforeId) {
        return new DynamicActionExecutionRequest(recordId, record, ids, orderedIds, beforeId, afterId, parentId,
                criteria, pageRequest, sorts, queryConditions, fieldNames);
    }

    public DynamicActionExecutionRequest withAfterId(String afterId) {
        return new DynamicActionExecutionRequest(recordId, record, ids, orderedIds, beforeId, afterId, parentId,
                criteria, pageRequest, sorts, queryConditions, fieldNames);
    }

    public DynamicActionExecutionRequest withParentId(String parentId) {
        return new DynamicActionExecutionRequest(recordId, record, ids, orderedIds, beforeId, afterId, parentId,
                criteria, pageRequest, sorts, queryConditions, fieldNames);
    }

    public DynamicActionExecutionRequest withCriteria(Criteria criteria) {
        return new DynamicActionExecutionRequest(recordId, record, ids, orderedIds, beforeId, afterId, parentId,
                criteria, pageRequest, sorts, queryConditions, fieldNames);
    }

    public DynamicActionExecutionRequest withPageRequest(PageRequest pageRequest) {
        return new DynamicActionExecutionRequest(recordId, record, ids, orderedIds, beforeId, afterId, parentId,
                criteria, pageRequest, sorts, queryConditions, fieldNames);
    }

    public DynamicActionExecutionRequest withSorts(List<Sort> sorts) {
        return new DynamicActionExecutionRequest(recordId, record, ids, orderedIds, beforeId, afterId, parentId,
                criteria, pageRequest, sorts, queryConditions, fieldNames);
    }

    public DynamicActionExecutionRequest withQueryConditions(Collection<DynamicQueryCondition> queryConditions) {
        return new DynamicActionExecutionRequest(recordId, record, ids, orderedIds, beforeId, afterId, parentId,
                criteria, pageRequest, sorts, queryConditions, fieldNames);
    }

    public DynamicActionExecutionRequest withFieldNames(Collection<String> fieldNames) {
        return new DynamicActionExecutionRequest(recordId, record, ids, orderedIds, beforeId, afterId, parentId,
                criteria, pageRequest, sorts, queryConditions, fieldNames);
    }
}
