package net.ximatai.muyun.spring.boot.dynamic;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicActionDescriptor;
import net.ximatai.muyun.spring.dynamic.metadata.DynamicQueryOperator;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionAvailability;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionContext;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionException;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionResult;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionResultBody;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicReferenceMatchMode;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicReferenceResolveMode;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

record DynamicRecordPayload(String id, Integer version, Map<String, Object> values) {
    DynamicRecordPayload {
        values = values == null ? Map.of() : Map.copyOf(values);
    }

    static DynamicRecordPayload empty() {
        return new DynamicRecordPayload(null, null, Map.of());
    }
}

record DynamicQueryRequest(List<DynamicWebQueryCondition> conditions,
                           DynamicWebPageRequest page,
                           List<DynamicWebSort> sorts) {
    DynamicQueryRequest {
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        sorts = sorts == null ? List.of() : List.copyOf(sorts);
    }

    static DynamicQueryRequest empty() {
        return new DynamicQueryRequest(List.of(), DynamicWebPageRequest.DEFAULT, List.of());
    }
}

record DynamicWebQueryCondition(String fieldName, DynamicQueryOperator operator, List<Object> values) {
    DynamicWebQueryCondition {
        values = values == null ? List.of() : List.copyOf(values);
    }
}

record DynamicWebPageRequest(int pageNum, int pageSize) {
    static final DynamicWebPageRequest DEFAULT = new DynamicWebPageRequest(1, 20);
}

record DynamicWebSort(String field, boolean desc) {
}

record DynamicWebActionRequest(String recordId,
                               DynamicRecordPayload record,
                               List<String> ids,
                               List<String> orderedIds,
                               String beforeId,
                               String afterId,
                               String parentId,
                               List<DynamicWebQueryCondition> conditions,
                               DynamicWebPageRequest page,
                               List<DynamicWebSort> sorts,
                               List<String> fieldNames,
                               Map<String, Object> payload) {
    DynamicWebActionRequest {
        ids = ids == null ? List.of() : List.copyOf(ids);
        orderedIds = orderedIds == null ? List.of() : List.copyOf(orderedIds);
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        sorts = sorts == null ? List.of() : List.copyOf(sorts);
        fieldNames = fieldNames == null ? List.of() : List.copyOf(fieldNames);
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }

    static DynamicWebActionRequest empty() {
        return new DynamicWebActionRequest(null, null, List.of(), List.of(), null, null, null,
                List.of(), null, List.of(), List.of(), Map.of());
    }
}

record DynamicWebReferenceRequest(DynamicReferenceResolveMode mode,
                                  DynamicReferenceMatchMode matchMode,
                                  String fuzzy,
                                  List<Object> values,
                                  List<DynamicWebQueryCondition> conditions,
                                  DynamicWebPageRequest page,
                                  Boolean includeProjections) {
    DynamicWebReferenceRequest {
        values = values == null ? List.of() : List.copyOf(values);
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        includeProjections = includeProjections == null || includeProjections;
    }

    static DynamicWebReferenceRequest empty() {
        return new DynamicWebReferenceRequest(null, null, null, List.of(), List.of(),
                DynamicWebPageRequest.DEFAULT, true);
    }
}

record RecordIdResponse(String id) {
}

record CountResponse(int count) {
}

record DynamicRecordResponse(String id,
                             Integer version,
                             Map<String, Object> values,
                             Map<String, List<DynamicRecordResponse>> children) {
    static DynamicRecordResponse from(DynamicRecord record) {
        if (record == null) {
            return null;
        }
        Map<String, List<DynamicRecordResponse>> childResponses = record.getChildren().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue() == null
                                ? List.of()
                                : entry.getValue().stream().map(DynamicRecordResponse::from).toList()
                ));
        return new DynamicRecordResponse(record.getId(), record.getVersion(), record.getValues(), childResponses);
    }
}

record DynamicPageResponse(List<Object> records,
                           long total,
                           int pageNum,
                           int pageSize,
                           long pages,
                           boolean totalKnown) {
    static DynamicPageResponse from(PageResult<?> page) {
        return new DynamicPageResponse(
                page.getRecords().stream().map(DynamicWebValues::webValue).toList(),
                page.getTotal(),
                page.getPageNum(),
                page.getPageSize(),
                page.getPages(),
                page.isTotalKnown()
        );
    }
}

record DynamicWebActionExecutionResponse(DynamicWebActionContext context, DynamicWebActionResultBody body) {
    static DynamicWebActionExecutionResponse from(DynamicActionExecutionResult result) {
        return new DynamicWebActionExecutionResponse(
                DynamicWebActionContext.from(result.context()),
                DynamicWebActionResultBody.from(result.body())
        );
    }
}

record DynamicWebActionContext(String moduleAlias,
                               String actionCode,
                               String actionLevel,
                               String executorType,
                               String recordId,
                               String traceId) {
    static DynamicWebActionContext from(DynamicActionExecutionContext context) {
        if (context == null) {
            return null;
        }
        return new DynamicWebActionContext(
                context.moduleAlias(),
                context.actionCode(),
                context.action().actionLevel().name(),
                context.action().executorType().name(),
                context.recordId(),
                context.traceId()
        );
    }
}

record DynamicWebActionAvailabilityResponse(DynamicActionDescriptor action,
                                            boolean available,
                                            String message) {
    static DynamicWebActionAvailabilityResponse from(DynamicActionDescriptor action,
                                                    DynamicActionAvailability availability) {
        return new DynamicWebActionAvailabilityResponse(action, availability.available(), availability.message());
    }
}

record DynamicWebActionResultBody(String type,
                                  Object value,
                                  String message,
                                  boolean refresh,
                                  String redirectTo) {
    static DynamicWebActionResultBody from(DynamicActionResultBody body) {
        return new DynamicWebActionResultBody(
                body.type().name(),
                DynamicWebValues.webValue(body.value()),
                body.message(),
                body.refresh(),
                body.redirectTo()
        );
    }
}

record DynamicWebError(String code,
                       int status,
                       String message,
                       String traceId) {
    static DynamicWebError badRequest(String message) {
        return new DynamicWebError("DYNAMIC_BAD_REQUEST", 400, message, null);
    }

    static DynamicWebError conflict(String message) {
        return new DynamicWebError("DYNAMIC_CONFLICT", 409, message, null);
    }
}

record DynamicWebActionError(String code,
                             int status,
                             String message,
                             String failureStage,
                             String traceId,
                             DynamicWebActionContext context) {
    static DynamicWebActionError from(DynamicActionExecutionException exception) {
        DynamicWebActionContext context = DynamicWebActionContext.from(exception.context());
        return new DynamicWebActionError(
                "DYNAMIC_ACTION_FAILED",
                400,
                exception.getMessage(),
                exception.failureStage(),
                context == null ? null : context.traceId(),
                context
        );
    }
}

final class DynamicWebValues {
    private DynamicWebValues() {
    }

    static Object webValue(Object value) {
        if (value instanceof DynamicRecord record) {
            return DynamicRecordResponse.from(record);
        }
        if (value instanceof PageResult<?> page) {
            return DynamicPageResponse.from(page);
        }
        if (value instanceof Criteria) {
            throw new IllegalArgumentException("dynamic web response does not expose internal Criteria");
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(DynamicWebValues::webValue).toList();
        }
        if (value instanceof Map<?, ?> map) {
            LinkedHashMap<String, Object> converted = new LinkedHashMap<>();
            map.forEach((key, item) -> converted.put(String.valueOf(key), webValue(item)));
            return converted;
        }
        return value;
    }
}
