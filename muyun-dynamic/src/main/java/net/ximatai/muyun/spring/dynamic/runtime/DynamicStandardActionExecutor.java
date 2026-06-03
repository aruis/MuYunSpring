package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;

final class DynamicStandardActionExecutor {
    private final DynamicRecordService service;
    private final String moduleAlias;
    private final String entityAlias;
    private final DynamicRecordService.EntityOperations operations;
    private final String traceId;

    DynamicStandardActionExecutor(DynamicRecordService service, String moduleAlias, String entityAlias, String traceId) {
        this.service = service;
        this.moduleAlias = moduleAlias;
        this.entityAlias = entityAlias;
        this.operations = service.entity(moduleAlias, entityAlias);
        this.traceId = traceId;
    }

    DynamicActionResultBody execute(String actionCode, DynamicActionExecutionRequest request) {
        return switch (actionCode) {
            case "create" -> DynamicActionResultBody.createdRecordId(
                    service.createFromAction(moduleAlias, entityAlias, requireRecord(request, actionCode), traceId));
            case "select" -> DynamicActionResultBody.of(operations.select(requireRecordId(request, actionCode)));
            case "update" -> countResult(service.updateFromAction(moduleAlias, entityAlias, requireRecord(request, actionCode), traceId));
            case "delete" -> countResult(service.deleteFromAction(moduleAlias, entityAlias, requireRecordId(request, actionCode), traceId));
            case "list" -> DynamicActionResultBody.of(operations.list(criteria(request), requirePageRequest(request, actionCode), sorts(request)));
            case "page" -> DynamicActionResultBody.of(operations.page(criteria(request), requirePageRequest(request, actionCode), sorts(request)));
            case "count" -> new DynamicActionResultBody(DynamicActionResultType.COUNT,
                    operations.count(criteria(request)), null, false, null);
            case "queryCriteria" -> DynamicActionResultBody.of(operations.queryCriteria(request.queryConditions()));
            case "sortedList" -> DynamicActionResultBody.of(operations.sortedList(criteria(request)));
            case "reorder" -> {
                service.reorderFromAction(moduleAlias, entityAlias, request.orderedIds(), traceId);
                yield DynamicActionResultBody.refreshed();
            }
            case "moveBefore" -> {
                service.moveBeforeFromAction(moduleAlias, entityAlias, requireRecordId(request, actionCode),
                        requireText(request.beforeId(), "beforeId"), traceId);
                yield DynamicActionResultBody.refreshed();
            }
            case "moveAfter" -> {
                service.moveAfterFromAction(moduleAlias, entityAlias, requireRecordId(request, actionCode),
                        requireText(request.afterId(), "afterId"), traceId);
                yield DynamicActionResultBody.refreshed();
            }
            case "children" -> DynamicActionResultBody.of(operations.children(request.parentId()));
            case "ancestorIds" -> DynamicActionResultBody.of(operations.ancestorIds(requireRecordId(request, actionCode)));
            case "ancestorIdsAndSelf" -> DynamicActionResultBody.of(operations.ancestorIdsAndSelf(requireRecordId(request, actionCode)));
            case "descendantIds" -> DynamicActionResultBody.of(operations.descendantIds(requireRecordId(request, actionCode)));
            case "title" -> DynamicActionResultBody.of(operations.title(requireRecordId(request, actionCode)));
            case "titles" -> DynamicActionResultBody.of(operations.titles(request.ids()));
            case "projections" -> DynamicActionResultBody.of(operations.projections(request.ids(), request.fieldNames()));
            case "referenceOptions" -> DynamicActionResultBody.of(operations.referenceOptions(criteria(request), requirePageRequest(request, actionCode)));
            case "enable" -> countResult(service.enableFromAction(moduleAlias, entityAlias, requireRecordId(request, actionCode), traceId));
            case "disable" -> countResult(service.disableFromAction(moduleAlias, entityAlias, requireRecordId(request, actionCode), traceId));
            case "isEnabled" -> DynamicActionResultBody.of(operations.isEnabled(requireRecordId(request, actionCode)));
            case "enabledCriteria" -> DynamicActionResultBody.of(operations.enabledCriteria(criteria(request)));
            default -> throw new IllegalArgumentException("unknown standard dynamic action: "
                    + moduleAlias + "." + entityAlias + "." + actionCode);
        };
    }

    private DynamicActionResultBody countResult(int count) {
        return DynamicActionResultBody.changedCount(count);
    }

    private DynamicRecord requireRecord(DynamicActionExecutionRequest request, String actionCode) {
        if (request.record() == null) {
            throw new IllegalArgumentException("dynamic action requires record: " + actionCode);
        }
        return request.record();
    }

    private String requireRecordId(DynamicActionExecutionRequest request, String actionCode) {
        if (request.recordId() != null && !request.recordId().isBlank()) {
            return request.recordId();
        }
        if (request.record() != null && request.record().getId() != null && !request.record().getId().isBlank()) {
            return request.record().getId();
        }
        throw new IllegalArgumentException("dynamic action requires recordId: " + actionCode);
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("dynamic action requires " + fieldName);
        }
        return value;
    }

    private Criteria criteria(DynamicActionExecutionRequest request) {
        return request.criteria() == null ? Criteria.of() : request.criteria();
    }

    private PageRequest requirePageRequest(DynamicActionExecutionRequest request, String actionCode) {
        if (request.pageRequest() == null) {
            throw new IllegalArgumentException("dynamic action requires pageRequest: " + actionCode);
        }
        return request.pageRequest();
    }

    private Sort[] sorts(DynamicActionExecutionRequest request) {
        return request.sorts().toArray(Sort[]::new);
    }
}
