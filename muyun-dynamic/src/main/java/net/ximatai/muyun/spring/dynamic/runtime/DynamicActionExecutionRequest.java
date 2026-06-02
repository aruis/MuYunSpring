package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

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
        Collection<String> fieldNames,
        Map<String, Object> payload
) {
    public DynamicActionExecutionRequest {
        ids = ids == null ? List.of() : List.copyOf(ids);
        orderedIds = orderedIds == null ? List.of() : List.copyOf(orderedIds);
        sorts = sorts == null ? List.of() : List.copyOf(sorts);
        queryConditions = queryConditions == null ? List.of() : List.copyOf(queryConditions);
        fieldNames = fieldNames == null ? List.of() : List.copyOf(fieldNames);
        payload = immutablePayload(payload);
    }

    public DynamicActionExecutionRequest(String recordId,
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
                                         Collection<String> fieldNames) {
        this(recordId, record, ids, orderedIds, beforeId, afterId, parentId,
                criteria, pageRequest, sorts, queryConditions, fieldNames, Map.of());
    }

    public static DynamicActionExecutionRequest empty() {
        return new DynamicActionExecutionRequest(null, null, List.of(), List.of(), null, null, null,
                null, null, List.of(), List.of(), List.of(), Map.of());
    }

    public static DynamicActionExecutionRequest id(String id) {
        return empty().withRecordId(id);
    }

    public static DynamicActionExecutionRequest record(DynamicRecord record) {
        return empty().withRecord(record);
    }

    public DynamicActionExecutionRequest withRecordId(String recordId) {
        return new DynamicActionExecutionRequest(recordId, record, ids, orderedIds, beforeId, afterId, parentId,
                criteria, pageRequest, sorts, queryConditions, fieldNames, payload);
    }

    public DynamicActionExecutionRequest withRecord(DynamicRecord record) {
        return new DynamicActionExecutionRequest(recordId, record, ids, orderedIds, beforeId, afterId, parentId,
                criteria, pageRequest, sorts, queryConditions, fieldNames, payload);
    }

    public DynamicActionExecutionRequest withIds(Collection<String> ids) {
        return new DynamicActionExecutionRequest(recordId, record, ids, orderedIds, beforeId, afterId, parentId,
                criteria, pageRequest, sorts, queryConditions, fieldNames, payload);
    }

    public DynamicActionExecutionRequest withOrderedIds(List<String> orderedIds) {
        return new DynamicActionExecutionRequest(recordId, record, ids, orderedIds, beforeId, afterId, parentId,
                criteria, pageRequest, sorts, queryConditions, fieldNames, payload);
    }

    public DynamicActionExecutionRequest withBeforeId(String beforeId) {
        return new DynamicActionExecutionRequest(recordId, record, ids, orderedIds, beforeId, afterId, parentId,
                criteria, pageRequest, sorts, queryConditions, fieldNames, payload);
    }

    public DynamicActionExecutionRequest withAfterId(String afterId) {
        return new DynamicActionExecutionRequest(recordId, record, ids, orderedIds, beforeId, afterId, parentId,
                criteria, pageRequest, sorts, queryConditions, fieldNames, payload);
    }

    public DynamicActionExecutionRequest withParentId(String parentId) {
        return new DynamicActionExecutionRequest(recordId, record, ids, orderedIds, beforeId, afterId, parentId,
                criteria, pageRequest, sorts, queryConditions, fieldNames, payload);
    }

    public DynamicActionExecutionRequest withCriteria(Criteria criteria) {
        return new DynamicActionExecutionRequest(recordId, record, ids, orderedIds, beforeId, afterId, parentId,
                criteria, pageRequest, sorts, queryConditions, fieldNames, payload);
    }

    public DynamicActionExecutionRequest withPageRequest(PageRequest pageRequest) {
        return new DynamicActionExecutionRequest(recordId, record, ids, orderedIds, beforeId, afterId, parentId,
                criteria, pageRequest, sorts, queryConditions, fieldNames, payload);
    }

    public DynamicActionExecutionRequest withSorts(List<Sort> sorts) {
        return new DynamicActionExecutionRequest(recordId, record, ids, orderedIds, beforeId, afterId, parentId,
                criteria, pageRequest, sorts, queryConditions, fieldNames, payload);
    }

    public DynamicActionExecutionRequest withQueryConditions(Collection<DynamicQueryCondition> queryConditions) {
        return new DynamicActionExecutionRequest(recordId, record, ids, orderedIds, beforeId, afterId, parentId,
                criteria, pageRequest, sorts, queryConditions, fieldNames, payload);
    }

    public DynamicActionExecutionRequest withFieldNames(Collection<String> fieldNames) {
        return new DynamicActionExecutionRequest(recordId, record, ids, orderedIds, beforeId, afterId, parentId,
                criteria, pageRequest, sorts, queryConditions, fieldNames, payload);
    }

    public DynamicActionExecutionRequest withPayload(Map<String, Object> payload) {
        return new DynamicActionExecutionRequest(recordId, record, ids, orderedIds, beforeId, afterId, parentId,
                criteria, pageRequest, sorts, queryConditions, fieldNames, payload);
    }

    public DynamicActionExecutionRequest withPayloadValue(String key, Object value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("payload key must not be blank");
        }
        Map<String, Object> values = new LinkedHashMap<>(payload);
        values.put(key, value);
        return withPayload(values);
    }

    private static Map<String, Object> immutablePayload(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> copy.put(Objects.requireNonNull(key, "payload key must not be null"), value));
        return Collections.unmodifiableMap(copy);
    }
}
