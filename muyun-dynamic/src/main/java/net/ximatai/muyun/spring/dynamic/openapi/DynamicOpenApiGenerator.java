package net.ximatai.muyun.spring.dynamic.openapi;

import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.platform.PlatformPermissionCode;
import net.ximatai.muyun.spring.common.web.PlatformWebPathRules;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicActionDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicEntityDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicModuleDescriptor;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionCategory;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

public class DynamicOpenApiGenerator {
    private static final String METHOD_GET = "GET";
    private static final String METHOD_POST = "POST";
    private static final List<String> DEFAULT_ERRORS = List.of(
            "DYNAMIC_BAD_REQUEST",
            "DYNAMIC_UI_VALIDATION",
            "DYNAMIC_ATTACHMENT_ERROR",
            "DYNAMIC_DUPLICATE_CHECK_ERROR",
            "DYNAMIC_ACTION_FAILED",
            "DYNAMIC_CONFLICT"
    );

    public DynamicOpenApiDocument generate(DynamicModuleDescriptor descriptor) {
        return generate(descriptor, action -> true);
    }

    public DynamicOpenApiDocument generate(DynamicModuleDescriptor descriptor,
                                           Predicate<PlatformAction> standardActionVisible) {
        Objects.requireNonNull(descriptor, "descriptor must not be null");
        Objects.requireNonNull(standardActionVisible, "standardActionVisible must not be null");
        DynamicEntityDescriptor mainEntity = requireMainEntity(descriptor);
        String basePath = "/" + descriptor.moduleAlias();
        Map<String, DynamicOpenApiDocument.Schema> schemas = schemas(mainEntity);
        return new DynamicOpenApiDocument(
                descriptor.moduleAlias(),
                descriptor.title(),
                basePath,
                operations(descriptor, mainEntity, basePath, standardActionVisible),
                schemas,
                errors()
        );
    }

    private DynamicEntityDescriptor requireMainEntity(DynamicModuleDescriptor descriptor) {
        return descriptor.entities().stream()
                .filter(entity -> entity.entityAlias().equals(descriptor.mainEntityAlias()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("dynamic OpenAPI main entity not found: "
                        + descriptor.moduleAlias() + "." + descriptor.mainEntityAlias()));
    }

    private List<DynamicOpenApiDocument.Operation> operations(DynamicModuleDescriptor descriptor,
                                                              DynamicEntityDescriptor mainEntity,
                                                              String basePath,
                                                              Predicate<PlatformAction> standardActionVisible) {
        List<DynamicOpenApiDocument.Operation> operations = new ArrayList<>();
        operations.add(getOperation(descriptor.moduleAlias(), basePath + "/describe", "describe" + upperModuleName(descriptor.moduleAlias()),
                "Describe " + descriptor.title(), null, "DynamicModuleDescriptor", null));
        operations.add(getOperation(descriptor.moduleAlias(), basePath + "/openapi", operationId(descriptor, "openApi"),
                "OpenAPI " + descriptor.title(), null, "DynamicOpenApiDocument", null));
        if (standardActionVisible.test(PlatformAction.QUERY)) {
            operations.add(operation(descriptor.moduleAlias(), basePath + "/query", operationId(descriptor, "query"),
                    "Query " + mainEntity.title(), "WebQueryRequest", "WebPageResponse", PlatformAction.QUERY.code()));
            operations.add(operation(descriptor.moduleAlias(), basePath + "/query/summary",
                    operationId(descriptor, "querySummary"),
                    "Summary " + mainEntity.title(), "WebQueryRequest", "DynamicSummaryItemList",
                    PlatformAction.QUERY.code()));
            operations.add(operation(descriptor.moduleAlias(), basePath + "/view/{id}/associations/{viewCode}/query",
                    operationId(descriptor, "queryAssociation"),
                    "Query association " + mainEntity.title(), "WebQueryRequest", "WebPageResponse",
                    PlatformAction.QUERY.code()));
        }
        if (standardActionVisible.test(PlatformAction.VIEW)) {
            operations.add(getOperation(descriptor.moduleAlias(), basePath + "/view/{id}", operationId(descriptor, "view"),
                    "View " + mainEntity.title(), null, "DynamicRecordResponse", PlatformAction.VIEW.code()));
            operations.add(operation(descriptor.moduleAlias(), basePath + "/view/{id}/attachments/query",
                    operationId(descriptor, "queryAttachments"),
                    "Query attachments " + mainEntity.title(), null, "RecordAttachmentList", PlatformAction.VIEW.code()));
            operations.add(operation(descriptor.moduleAlias(), basePath + "/view/{id}/attachments/{attachmentId}/preview-ticket",
                    operationId(descriptor, "attachmentPreviewTicket"),
                    "Issue attachment preview ticket " + mainEntity.title(), null, "RecordAttachmentAccess",
                    PlatformAction.VIEW.code()));
            operations.add(operation(descriptor.moduleAlias(), basePath + "/view/{id}/attachments/{attachmentId}/download-ticket",
                    operationId(descriptor, "attachmentDownloadTicket"),
                    "Issue attachment download ticket " + mainEntity.title(), null, "RecordAttachmentAccess",
                    PlatformAction.VIEW.code()));
        }
        if (standardActionVisible.test(PlatformAction.CREATE)) {
            operations.add(operation(descriptor.moduleAlias(), basePath + "/insert", operationId(descriptor, "insert"),
                    "Insert " + mainEntity.title(), "DynamicRecordSaveRequest", "DynamicRecordResponse", PlatformAction.CREATE.code()));
        }
        if (standardActionVisible.test(PlatformAction.UPDATE)) {
            operations.add(operation(descriptor.moduleAlias(), basePath + "/update/{id}", operationId(descriptor, "update"),
                    "Update " + mainEntity.title(), "DynamicRecordSaveRequest", "DynamicRecordResponse", PlatformAction.UPDATE.code()));
            operations.add(operation(descriptor.moduleAlias(), basePath + "/view/{id}/attachments/add",
                    operationId(descriptor, "addAttachment"),
                    "Add attachment " + mainEntity.title(), "RecordAttachmentCommand", "RecordAttachmentList",
                    PlatformAction.UPDATE.code()));
            operations.add(operation(descriptor.moduleAlias(), basePath + "/view/{id}/attachments/upload-ticket",
                    operationId(descriptor, "attachmentUploadTicket"),
                    "Issue attachment upload ticket " + mainEntity.title(), null, "RecordAttachmentAccess",
                    PlatformAction.UPDATE.code()));
            operations.add(operation(descriptor.moduleAlias(), basePath + "/view/{id}/attachments/update/{attachmentId}",
                    operationId(descriptor, "updateAttachment"),
                    "Update attachment " + mainEntity.title(), "RecordAttachmentCommand", "RecordAttachmentList",
                    PlatformAction.UPDATE.code()));
            operations.add(operation(descriptor.moduleAlias(), basePath + "/view/{id}/attachments/delete/{attachmentId}",
                    operationId(descriptor, "deleteAttachment"),
                    "Delete attachment " + mainEntity.title(), null, "RecordAttachmentList",
                    PlatformAction.UPDATE.code()));
        }
        if (standardActionVisible.test(PlatformAction.DELETE)) {
            operations.add(operation(descriptor.moduleAlias(), basePath + "/delete/{id}", operationId(descriptor, "delete"),
                    "Delete " + mainEntity.title(), null, "WebCountResponse", PlatformAction.DELETE.code()));
        }
        boolean exchangeSupported = mainEntity.capabilities().contains(EntityCapability.EXCHANGE.name());
        if (exchangeSupported && standardActionVisible.test(PlatformAction.IMPORT)) {
            operations.add(operation(descriptor.moduleAlias(), basePath + "/exchange/template",
                    operationId(descriptor, "exchangeTemplate"),
                    "Download exchange template " + mainEntity.title(), "DynamicExchangeTemplateRequest",
                    "binary", PlatformAction.IMPORT.code()));
            operations.add(operation(descriptor.moduleAlias(), basePath + "/import/parse", operationId(descriptor, "importParse"),
                    "Parse import workbook " + mainEntity.title(), "DynamicImportParseRequest",
                    "DynamicImportParseResult", PlatformAction.IMPORT.code()));
            operations.add(operation(descriptor.moduleAlias(), basePath + "/import/execute", operationId(descriptor, "importExecute"),
                    "Execute import workbook " + mainEntity.title(), "DynamicImportExecuteMultipartRequest",
                    "DynamicImportUploadResult", PlatformAction.IMPORT.code()));
            operations.add(operation(descriptor.moduleAlias(), basePath + "/import/error-file/{token}",
                    operationId(descriptor, "importErrorFile"),
                    "Download import error workbook " + mainEntity.title(), null,
                    "binary", PlatformAction.IMPORT.code()));
        }
        if (exchangeSupported && standardActionVisible.test(PlatformAction.EXPORT)) {
            operations.add(operation(descriptor.moduleAlias(), basePath + "/export/data",
                    operationId(descriptor, "exportData"),
                    "Export data " + mainEntity.title(), "WebQueryRequest", "binary", PlatformAction.EXPORT.code()));
        }
        if (mainEntity.capabilities().contains(EntityCapability.ENABLE.name())
                && standardActionVisible.test(PlatformAction.ENABLE)) {
            operations.add(operation(descriptor.moduleAlias(), basePath + "/enable/{id}", operationId(descriptor, "enable"),
                    "Enable " + mainEntity.title(), null, "WebCountResponse", PlatformAction.ENABLE.code()));
        }
        if (mainEntity.capabilities().contains(EntityCapability.ENABLE.name())
                && standardActionVisible.test(PlatformAction.DISABLE)) {
            operations.add(operation(descriptor.moduleAlias(), basePath + "/disable/{id}", operationId(descriptor, "disable"),
                    "Disable " + mainEntity.title(), null, "WebCountResponse", PlatformAction.DISABLE.code()));
        }
        if (mainEntity.capabilities().contains(EntityCapability.SORT.name())
                && standardActionVisible.test(PlatformAction.SORT)) {
            String sortRequestSchema = mainEntity.capabilities().contains(EntityCapability.TREE.name())
                    ? "TreeSortWebRequest"
                    : "SortWebRequest";
            operations.add(operation(descriptor.moduleAlias(), basePath + "/sort/{id}", operationId(descriptor, "sort"),
                    "Sort " + mainEntity.title(), sortRequestSchema, "WebCountResponse", PlatformAction.SORT.code()));
        }
        if (mainEntity.capabilities().contains(EntityCapability.TREE.name())
                && standardActionVisible.test(PlatformAction.TREE)) {
            operations.add(getOperation(descriptor.moduleAlias(), basePath + "/tree", operationId(descriptor, "tree"),
                    "Tree " + mainEntity.title(), null, "WebListResponse", PlatformAction.TREE.code()));
            operations.add(getOperation(descriptor.moduleAlias(), basePath + "/tree/{id}", operationId(descriptor, "treeNode"),
                    "Tree node " + mainEntity.title(), null, "WebListResponse", PlatformAction.TREE.code()));
        }
        operations.add(getOperation(descriptor.moduleAlias(), basePath + "/actions", operationId(descriptor, "actions"),
                "List module actions", null, "DynamicActionDescriptorList", null));
        operations.add(getOperation(descriptor.moduleAlias(), basePath + "/actions/{recordId}", operationId(descriptor, "recordActions"),
                "List record actions", null, "DynamicWebActionAvailabilityList", null));
        descriptor.actions().stream()
                .filter(DynamicActionDescriptor::enabled)
                .filter(action -> action.category() != EntityActionCategory.STANDARD)
                .filter(action -> !PlatformWebPathRules.isReservedWebActionCode(action.code()))
                .filter(action -> action.actionLevel() != null)
                .forEach(action -> operations.addAll(actionOperations(descriptor, action, basePath)));
        descriptor.actions().stream()
                .filter(DynamicActionDescriptor::enabled)
                .filter(action -> action.actionLevel() == EntityActionLevel.RECORD
                        || action.actionLevel() == EntityActionLevel.ANY)
                .filter(action -> action.category() != EntityActionCategory.STANDARD)
                .filter(action -> !PlatformWebPathRules.isReservedWebActionCode(action.code()))
                .forEach(action -> operations.add(operationWithPermissionCode(descriptor.moduleAlias(),
                        basePath + "/" + action.code() + "/duplicate/check",
                        operationId(descriptor, "duplicateCheck" + upperName(action.code())),
                        "Duplicate check " + action.title(),
                        "DynamicWebDuplicateCheckRequest",
                        "RecordDuplicateCheckResult",
                        action.code(),
                        actionPermissionCode(descriptor.moduleAlias(), action))));
        if (standardActionVisible.test(PlatformAction.REFERENCE)) {
            mainEntity.fields().stream()
                    .filter(field -> field.reference() != null)
                    .forEach(field -> operations.add(operation(descriptor.moduleAlias(), basePath + "/references/" + field.fieldName() + "/resolve",
                            operationId(descriptor, "resolve" + upperName(field.fieldName())),
                            "Resolve reference " + field.title(),
                            "DynamicWebReferenceRequest",
                            "DynamicReferenceResolveResponse",
                            PlatformAction.REFERENCE.code())));
        }
        return List.copyOf(operations);
    }

    private List<DynamicOpenApiDocument.Operation> actionOperations(DynamicModuleDescriptor descriptor,
                                                                    DynamicActionDescriptor action,
                                                                    String basePath) {
        return switch (action.actionLevel()) {
            case LIST -> List.of(actionOperation(descriptor, action, basePath + "/" + action.code(), "list"));
            case RECORD -> List.of(actionOperation(descriptor, action, basePath + "/" + action.code() + "/{recordId}", "record"));
            case BATCH -> List.of(actionOperation(descriptor, action, basePath + "/" + action.code() + "/batch", "batch"));
            case ANY -> List.of(
                    actionOperation(descriptor, action, basePath + "/" + action.code(), "list"),
                    actionOperation(descriptor, action, basePath + "/" + action.code() + "/{recordId}", "record"),
                    actionOperation(descriptor, action, basePath + "/" + action.code() + "/batch", "batch")
            );
        };
    }

    private DynamicOpenApiDocument.Operation actionOperation(DynamicModuleDescriptor descriptor,
                                                            DynamicActionDescriptor action,
                                                            String path,
                                                            String scope) {
        return operationWithPermissionCode(descriptor.moduleAlias(), path,
                operationId(descriptor, scope + upperName(action.code())),
                action.title(),
                "DynamicWebActionRequest",
                "DynamicWebActionExecutionResponse",
                action.code(),
                actionPermissionCode(descriptor.moduleAlias(), action));
    }

    private String actionPermissionCode(String moduleAlias, DynamicActionDescriptor action) {
        if (action.permission() != null) {
            return action.permission().permissionCode();
        }
        String permissionActionCode = action.authInheritActionCode() == null
                ? PlatformAction.permissionActionCodeOf(action.code())
                : PlatformAction.permissionActionCodeOf(action.authInheritActionCode());
        return PlatformPermissionCode.action(moduleAlias, permissionActionCode);
    }

    private DynamicOpenApiDocument.Operation operation(String moduleAlias,
                                                       String path,
                                                       String operationId,
                                                       String summary,
                                                       String requestSchema,
                                                       String responseSchema,
                                                       String actionCode) {
        return operation(METHOD_POST, moduleAlias, path, operationId, summary, requestSchema, responseSchema, actionCode);
    }

    private DynamicOpenApiDocument.Operation getOperation(String moduleAlias,
                                                          String path,
                                                          String operationId,
                                                          String summary,
                                                          String requestSchema,
                                                          String responseSchema,
                                                          String actionCode) {
        return operation(METHOD_GET, moduleAlias, path, operationId, summary, requestSchema, responseSchema, actionCode);
    }

    private DynamicOpenApiDocument.Operation operation(String method,
                                                       String moduleAlias,
                                                       String path,
                                                       String operationId,
                                                       String summary,
                                                       String requestSchema,
                                                       String responseSchema,
                                                       String actionCode) {
        return operationWithPermissionCode(method, moduleAlias, path, operationId, summary, requestSchema, responseSchema,
                actionCode, null);
    }

    private DynamicOpenApiDocument.Operation operationWithPermissionCode(String moduleAlias,
                                                                         String path,
                                                                         String operationId,
                                                                         String summary,
                                                                         String requestSchema,
                                                                         String responseSchema,
                                                                         String actionCode,
                                                                         String permissionCode) {
        return operationWithPermissionCode(METHOD_POST, moduleAlias, path, operationId, summary, requestSchema,
                responseSchema, actionCode, permissionCode);
    }

    private DynamicOpenApiDocument.Operation operationWithPermissionCode(String method,
                                                                         String moduleAlias,
                                                                         String path,
                                                                         String operationId,
                                                                         String summary,
                                                                         String requestSchema,
                                                                         String responseSchema,
                                                                         String actionCode,
                                                                         String permissionCode) {
        String effectivePermissionCode = permissionCode == null && actionCode != null
                ? PlatformPermissionCode.action(moduleAlias, PlatformAction.permissionActionCodeOf(actionCode))
                : permissionCode;
        return new DynamicOpenApiDocument.Operation(
                method,
                path,
                operationId,
                summary,
                requestSchema,
                responseSchema,
                actionCode,
                effectivePermissionCode,
                DEFAULT_ERRORS
        );
    }

    private Map<String, DynamicOpenApiDocument.Schema> schemas(DynamicEntityDescriptor entity) {
        return new DynamicOpenApiSchemaFactory().schemas(entity);
    }

    private Map<String, DynamicOpenApiDocument.ErrorResponse> errors() {
        return Map.of(
                "DYNAMIC_BAD_REQUEST", new DynamicOpenApiDocument.ErrorResponse("DYNAMIC_BAD_REQUEST", 400, "DynamicWebError"),
                "DYNAMIC_UI_VALIDATION", new DynamicOpenApiDocument.ErrorResponse("DYNAMIC_UI_VALIDATION", 400, "DynamicWebError"),
                "DYNAMIC_ATTACHMENT_ERROR", new DynamicOpenApiDocument.ErrorResponse("DYNAMIC_ATTACHMENT_ERROR", 400, "DynamicWebError"),
                "DYNAMIC_DUPLICATE_CHECK_ERROR", new DynamicOpenApiDocument.ErrorResponse("DYNAMIC_DUPLICATE_CHECK_ERROR", 400, "DynamicWebError"),
                "DYNAMIC_ACTION_FAILED", new DynamicOpenApiDocument.ErrorResponse("DYNAMIC_ACTION_FAILED", 400, "DynamicWebActionError"),
                "DYNAMIC_CONFLICT", new DynamicOpenApiDocument.ErrorResponse("DYNAMIC_CONFLICT", 409, "DynamicWebError")
        );
    }

    private String operationId(DynamicModuleDescriptor descriptor, String suffix) {
        return lowerName(descriptor.moduleAlias()) + upperName(suffix);
    }

    private String upperModuleName(String value) {
        return upperName(lowerName(value));
    }

    private String upperName(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = lowerName(value);
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private String lowerName(String value) {
        StringBuilder result = new StringBuilder();
        boolean upperNext = false;
        for (char c : value.toCharArray()) {
            if (c == '.' || c == '_' || c == '-') {
                upperNext = result.length() > 0;
                continue;
            }
            result.append(upperNext ? Character.toUpperCase(c) : c);
            upperNext = false;
        }
        return result.toString();
    }
}
