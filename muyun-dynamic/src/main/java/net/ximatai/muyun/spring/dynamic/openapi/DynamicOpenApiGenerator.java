package net.ximatai.muyun.spring.dynamic.openapi;

import net.ximatai.muyun.spring.common.option.OptionBinding;
import net.ximatai.muyun.spring.common.option.OptionSelectionMode;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicActionDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicEntityDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicFieldDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicModuleDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicReferenceDescriptor;
import net.ximatai.muyun.spring.dynamic.metadata.DynamicActionPathRules;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionCategory;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;
import net.ximatai.muyun.spring.dynamic.metadata.FieldType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DynamicOpenApiGenerator {
    private static final String METHOD_POST = "POST";
    private static final List<String> DEFAULT_ERRORS = List.of(
            "DYNAMIC_BAD_REQUEST",
            "DYNAMIC_ACTION_FAILED",
            "DYNAMIC_CONFLICT"
    );

    public DynamicOpenApiDocument generate(DynamicModuleDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "descriptor must not be null");
        DynamicEntityDescriptor mainEntity = requireMainEntity(descriptor);
        String basePath = "/" + descriptor.moduleAlias();
        Map<String, DynamicOpenApiDocument.Schema> schemas = schemas(mainEntity);
        return new DynamicOpenApiDocument(
                descriptor.moduleAlias(),
                descriptor.title(),
                basePath,
                operations(descriptor, mainEntity, basePath),
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
                                                              String basePath) {
        List<DynamicOpenApiDocument.Operation> operations = new ArrayList<>();
        String entitySchema = schemaName(mainEntity.entityAlias(), "Record");
        operations.add(operation(basePath + "/describe", "describe" + upperModuleName(descriptor.moduleAlias()),
                "Describe " + descriptor.title(), null, "DynamicModuleDescriptor", null));
        operations.add(operation(basePath + "/openapi", operationId(descriptor, "openApi"),
                "OpenAPI " + descriptor.title(), null, "DynamicOpenApiDocument", null));
        operations.add(operation(basePath + "/query", operationId(descriptor, "query"),
                "Query " + mainEntity.title(), "DynamicQueryRequest", "DynamicPageResponse", null));
        operations.add(operation(basePath + "/view/{recordId}", operationId(descriptor, "view"),
                "View " + mainEntity.title(), null, "DynamicRecordResponse", null));
        operations.add(operation(basePath + "/insert", operationId(descriptor, "insert"),
                "Insert " + mainEntity.title(), "DynamicRecordPayload", "RecordIdResponse", null));
        operations.add(operation(basePath + "/update/{recordId}", operationId(descriptor, "update"),
                "Update " + mainEntity.title(), "DynamicRecordPayload", "CountResponse", null));
        operations.add(operation(basePath + "/delete/{recordId}", operationId(descriptor, "delete"),
                "Delete " + mainEntity.title(), null, "CountResponse", null));
        operations.add(operation(basePath + "/actions", operationId(descriptor, "actions"),
                "List module actions", null, "DynamicActionDescriptorList", null));
        operations.add(operation(basePath + "/actions/{recordId}", operationId(descriptor, "recordActions"),
                "List record actions", null, "DynamicWebActionAvailabilityList", null));
        descriptor.actions().stream()
                .filter(DynamicActionDescriptor::enabled)
                .filter(action -> action.category() != EntityActionCategory.STANDARD)
                .filter(action -> !DynamicActionPathRules.isReservedWebActionCode(action.code()))
                .filter(action -> action.actionLevel() != null)
                .forEach(action -> operations.addAll(actionOperations(descriptor, action, basePath)));
        mainEntity.fields().stream()
                .filter(field -> field.reference() != null)
                .forEach(field -> operations.add(operation(basePath + "/references/" + field.fieldName() + "/resolve",
                        operationId(descriptor, "resolve" + upperName(field.fieldName())),
                        "Resolve reference " + field.title(),
                        "DynamicWebReferenceRequest",
                        "DynamicReferenceResolveResponse",
                        null)));
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
        return operation(path,
                operationId(descriptor, scope + upperName(action.code())),
                action.title(),
                "DynamicWebActionRequest",
                "DynamicWebActionExecutionResponse",
                action.code());
    }

    private DynamicOpenApiDocument.Operation operation(String path,
                                                       String operationId,
                                                       String summary,
                                                       String requestSchema,
                                                       String responseSchema,
                                                       String actionCode) {
        return new DynamicOpenApiDocument.Operation(
                METHOD_POST,
                path,
                operationId,
                summary,
                requestSchema,
                responseSchema,
                actionCode,
                DEFAULT_ERRORS
        );
    }

    private Map<String, DynamicOpenApiDocument.Schema> schemas(DynamicEntityDescriptor entity) {
        Map<String, DynamicOpenApiDocument.Schema> schemas = new LinkedHashMap<>();
        schemas.put(schemaName(entity.entityAlias(), "Values"), valuesSchema(entity));
        schemas.put(schemaName(entity.entityAlias(), "Record"), recordSchema(entity));
        schemas.put("DynamicRecordPayload", recordPayloadSchema(entity));
        schemas.put("DynamicRecordResponse", recordResponseSchema(entity));
        schemas.put("DynamicQueryRequest", queryRequestSchema());
        schemas.put("DynamicWebQueryCondition", queryConditionSchema());
        schemas.put("DynamicWebPageRequest", pageRequestSchema());
        schemas.put("DynamicWebSort", sortSchema());
        schemas.put("DynamicWebActionRequest", actionRequestSchema());
        schemas.put("DynamicWebReferenceRequest", referenceRequestSchema());
        schemas.put("DynamicPageResponse", pageResponseSchema());
        schemas.put("DynamicWebActionExecutionResponse", actionExecutionResponseSchema());
        schemas.put("DynamicReferenceResolveResponse", referenceResolveResponseSchema());
        schemas.put("DynamicModuleDescriptor", moduleDescriptorSchema());
        schemas.put("DynamicOpenApiDocument", openApiDocumentSchema());
        schemas.put("DynamicOpenApiOperation", openApiOperationSchema());
        schemas.put("DynamicOpenApiSchema", openApiSchemaSchema());
        schemas.put("DynamicOpenApiProperty", openApiPropertySchema());
        schemas.put("DynamicOpenApiErrorResponse", openApiErrorResponseSchema());
        schemas.put("DynamicActionDescriptorList", arraySchema("DynamicActionDescriptorList", "DynamicActionDescriptor"));
        schemas.put("DynamicWebActionAvailabilityList", arraySchema("DynamicWebActionAvailabilityList", "DynamicWebActionAvailabilityResponse"));
        schemas.put("DynamicWebActionAvailabilityResponse", actionAvailabilityResponseSchema());
        schemas.put("DynamicWebActionContext", actionContextSchema());
        schemas.put("DynamicWebActionResultBody", actionResultBodySchema());
        schemas.put("RecordIdResponse", recordIdResponseSchema());
        schemas.put("CountResponse", countResponseSchema());
        schemas.put("DynamicWebError", errorSchema("DynamicWebError", false));
        schemas.put("DynamicWebActionError", errorSchema("DynamicWebActionError", true));
        return Map.copyOf(schemas);
    }

    private DynamicOpenApiDocument.Schema recordSchema(DynamicEntityDescriptor entity) {
        Map<String, DynamicOpenApiDocument.Property> properties = recordEnvelopeProperties(entity);
        return new DynamicOpenApiDocument.Schema(schemaName(entity.entityAlias(), "Record"),
                "object", null, List.of(), properties, null);
    }

    private DynamicOpenApiDocument.Schema valuesSchema(DynamicEntityDescriptor entity) {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        for (DynamicFieldDescriptor field : entity.fields()) {
            properties.put(field.fieldName(), property(field));
            if (field.required()) {
                required.add(field.fieldName());
            }
        }
        return new DynamicOpenApiDocument.Schema(schemaName(entity.entityAlias(), "Values"),
                "object", null, required, properties, null);
    }

    private DynamicOpenApiDocument.Schema recordPayloadSchema(DynamicEntityDescriptor entity) {
        Map<String, DynamicOpenApiDocument.Property> properties = recordEnvelopeProperties(entity);
        return new DynamicOpenApiDocument.Schema("DynamicRecordPayload", "object", null,
                List.of(), properties, null);
    }

    private DynamicOpenApiDocument.Schema recordResponseSchema(DynamicEntityDescriptor entity) {
        Map<String, DynamicOpenApiDocument.Property> properties = recordEnvelopeProperties(entity);
        properties.put("children", new DynamicOpenApiDocument.Property("object", null, false, true,
                false, null, null, null, null, null, List.of()));
        return new DynamicOpenApiDocument.Schema("DynamicRecordResponse", "object", null,
                List.of(), properties, null);
    }

    private Map<String, DynamicOpenApiDocument.Property> recordEnvelopeProperties(DynamicEntityDescriptor entity) {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("id", new DynamicOpenApiDocument.Property("string", null, false, true,
                false, null, null, null, null, null, List.of()));
        properties.put("version", new DynamicOpenApiDocument.Property("integer", "int32", false, true,
                false, null, null, null, null, null, List.of()));
        properties.put("values", new DynamicOpenApiDocument.Property(schemaName(entity.entityAlias(), "Values"),
                null, false, false, false, null, null, null, null, null, List.of()));
        return properties;
    }

    private DynamicOpenApiDocument.Property property(DynamicFieldDescriptor field) {
        FieldShape shape = fieldShape(field.type(), field.selectionMode());
        OptionBinding optionBinding = field.optionBinding();
        DynamicReferenceDescriptor reference = field.reference();
        return new DynamicOpenApiDocument.Property(
                shape.type(),
                shape.format(),
                field.required(),
                !field.required(),
                OptionSelectionMode.MULTIPLE == field.selectionMode(),
                optionBinding == null ? null : optionBinding.sourceType(),
                optionBinding == null ? null : optionBinding.source(),
                reference == null ? null : reference.targetModuleAlias(),
                reference == null ? null : reference.targetEntityAlias(),
                null,
                field.companions().stream()
                        .map(companion -> companion.fieldName())
                        .toList()
        );
    }

    private FieldShape fieldShape(FieldType type, OptionSelectionMode selectionMode) {
        if (OptionSelectionMode.MULTIPLE == selectionMode) {
            return new FieldShape("array", null);
        }
        return switch (type) {
            case STRING, TEXT -> new FieldShape("string", null);
            case INTEGER -> new FieldShape("integer", "int32");
            case LONG -> new FieldShape("integer", "int64");
            case BOOLEAN -> new FieldShape("boolean", null);
            case DATE -> new FieldShape("string", "date");
            case TIMESTAMP, ZONED_TIMESTAMP -> new FieldShape("string", "date-time");
            case DECIMAL -> new FieldShape("number", "decimal");
            case JSON -> new FieldShape("object", null);
        };
    }

    private DynamicOpenApiDocument.Schema objectSchema(String name) {
        return new DynamicOpenApiDocument.Schema(name, "object", null, List.of(), Map.of(), null);
    }

    private DynamicOpenApiDocument.Schema moduleDescriptorSchema() {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("moduleAlias", stringProperty(false));
        properties.put("title", stringProperty(false));
        properties.put("mainEntityAlias", stringProperty(false));
        properties.put("actions", arrayProperty("DynamicActionDescriptor"));
        properties.put("entities", arrayProperty("DynamicEntityDescriptor"));
        properties.put("relations", arrayProperty("DynamicRelationDescriptor"));
        properties.put("references", arrayProperty("DynamicReferenceDescriptor"));
        properties.put("associationViews", arrayProperty("DynamicAssociationViewDescriptor"));
        return new DynamicOpenApiDocument.Schema("DynamicModuleDescriptor", "object", null,
                List.of("moduleAlias", "title", "mainEntityAlias"), properties, null);
    }

    private DynamicOpenApiDocument.Schema openApiDocumentSchema() {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("moduleAlias", stringProperty(false));
        properties.put("title", stringProperty(false));
        properties.put("basePath", stringProperty(false));
        properties.put("operations", arrayProperty("DynamicOpenApiOperation"));
        properties.put("schemas", objectProperty("DynamicOpenApiSchema"));
        properties.put("errors", objectProperty("DynamicOpenApiErrorResponse"));
        return new DynamicOpenApiDocument.Schema("DynamicOpenApiDocument", "object", null,
                List.of("moduleAlias", "title", "basePath"), properties, null);
    }

    private DynamicOpenApiDocument.Schema openApiOperationSchema() {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("method", stringProperty(false));
        properties.put("path", stringProperty(false));
        properties.put("operationId", stringProperty(false));
        properties.put("summary", stringProperty(true));
        properties.put("requestSchema", stringProperty(true));
        properties.put("responseSchema", stringProperty(true));
        properties.put("actionCode", stringProperty(true));
        properties.put("errorCodes", arrayProperty("string"));
        return new DynamicOpenApiDocument.Schema("DynamicOpenApiOperation", "object", null,
                List.of("method", "path", "operationId"), properties, null);
    }

    private DynamicOpenApiDocument.Schema openApiSchemaSchema() {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("name", stringProperty(false));
        properties.put("type", stringProperty(false));
        properties.put("format", stringProperty(true));
        properties.put("required", arrayProperty("string"));
        properties.put("properties", objectProperty("DynamicOpenApiProperty"));
        properties.put("items", objectProperty("DynamicOpenApiProperty"));
        return new DynamicOpenApiDocument.Schema("DynamicOpenApiSchema", "object", null,
                List.of("name", "type"), properties, null);
    }

    private DynamicOpenApiDocument.Schema openApiPropertySchema() {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("type", stringProperty(false));
        properties.put("format", stringProperty(true));
        properties.put("required", new DynamicOpenApiDocument.Property("boolean", null, false, false,
                false, null, null, null, null, null, List.of()));
        properties.put("nullable", new DynamicOpenApiDocument.Property("boolean", null, false, false,
                false, null, null, null, null, null, List.of()));
        properties.put("multiple", new DynamicOpenApiDocument.Property("boolean", null, false, false,
                false, null, null, null, null, null, List.of()));
        properties.put("optionSourceType", stringProperty(true));
        properties.put("optionSource", stringProperty(true));
        properties.put("referenceModuleAlias", stringProperty(true));
        properties.put("referenceEntityAlias", stringProperty(true));
        properties.put("itemType", stringProperty(true));
        properties.put("companionFields", arrayProperty("string"));
        return new DynamicOpenApiDocument.Schema("DynamicOpenApiProperty", "object", null,
                List.of("type"), properties, null);
    }

    private DynamicOpenApiDocument.Schema openApiErrorResponseSchema() {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("code", stringProperty(false));
        properties.put("status", new DynamicOpenApiDocument.Property("integer", "int32", false, false,
                false, null, null, null, null, null, List.of()));
        properties.put("schemaName", stringProperty(false));
        return new DynamicOpenApiDocument.Schema("DynamicOpenApiErrorResponse", "object", null,
                List.of("code", "status", "schemaName"), properties, null);
    }

    private DynamicOpenApiDocument.Schema queryRequestSchema() {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("conditions", arrayProperty("DynamicWebQueryCondition"));
        properties.put("page", objectProperty("DynamicWebPageRequest"));
        properties.put("sorts", arrayProperty("DynamicWebSort"));
        return new DynamicOpenApiDocument.Schema("DynamicQueryRequest", "object", null, List.of(), properties, null);
    }

    private DynamicOpenApiDocument.Schema queryConditionSchema() {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("fieldName", stringProperty(false));
        properties.put("operator", stringProperty(false));
        properties.put("values", arrayProperty("object"));
        return new DynamicOpenApiDocument.Schema("DynamicWebQueryCondition", "object", null, List.of(), properties, null);
    }

    private DynamicOpenApiDocument.Schema pageRequestSchema() {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("pageNum", new DynamicOpenApiDocument.Property("integer", "int32", false, false,
                false, null, null, null, null, null, List.of()));
        properties.put("pageSize", new DynamicOpenApiDocument.Property("integer", "int32", false, false,
                false, null, null, null, null, null, List.of()));
        return new DynamicOpenApiDocument.Schema("DynamicWebPageRequest", "object", null, List.of(), properties, null);
    }

    private DynamicOpenApiDocument.Schema sortSchema() {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("field", stringProperty(false));
        properties.put("desc", new DynamicOpenApiDocument.Property("boolean", null, false, false,
                false, null, null, null, null, null, List.of()));
        return new DynamicOpenApiDocument.Schema("DynamicWebSort", "object", null, List.of(), properties, null);
    }

    private DynamicOpenApiDocument.Schema actionRequestSchema() {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("recordId", stringProperty(true));
        properties.put("record", objectProperty("DynamicRecordPayload"));
        properties.put("ids", arrayProperty("string"));
        properties.put("orderedIds", arrayProperty("string"));
        properties.put("beforeId", stringProperty(true));
        properties.put("afterId", stringProperty(true));
        properties.put("parentId", stringProperty(true));
        properties.put("conditions", arrayProperty("DynamicWebQueryCondition"));
        properties.put("page", objectProperty("DynamicWebPageRequest"));
        properties.put("sorts", arrayProperty("DynamicWebSort"));
        properties.put("fieldNames", arrayProperty("string"));
        properties.put("payload", objectProperty("object"));
        return new DynamicOpenApiDocument.Schema("DynamicWebActionRequest", "object", null, List.of(), properties, null);
    }

    private DynamicOpenApiDocument.Schema referenceRequestSchema() {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("mode", stringProperty(true));
        properties.put("matchMode", stringProperty(true));
        properties.put("fuzzy", stringProperty(true));
        properties.put("values", arrayProperty("object"));
        properties.put("conditions", arrayProperty("DynamicWebQueryCondition"));
        properties.put("page", objectProperty("DynamicWebPageRequest"));
        properties.put("includeProjections", new DynamicOpenApiDocument.Property("boolean", null, false, true,
                false, null, null, null, null, null, List.of()));
        return new DynamicOpenApiDocument.Schema("DynamicWebReferenceRequest", "object", null, List.of(), properties, null);
    }

    private DynamicOpenApiDocument.Schema pageResponseSchema() {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("records", arrayProperty("DynamicRecordResponse"));
        properties.put("total", new DynamicOpenApiDocument.Property("integer", "int64", false, false,
                false, null, null, null, null, null, List.of()));
        properties.put("pageNum", new DynamicOpenApiDocument.Property("integer", "int32", false, false,
                false, null, null, null, null, null, List.of()));
        properties.put("pageSize", new DynamicOpenApiDocument.Property("integer", "int32", false, false,
                false, null, null, null, null, null, List.of()));
        properties.put("pages", new DynamicOpenApiDocument.Property("integer", "int64", false, false,
                false, null, null, null, null, null, List.of()));
        properties.put("totalKnown", new DynamicOpenApiDocument.Property("boolean", null, false, false,
                false, null, null, null, null, null, List.of()));
        return new DynamicOpenApiDocument.Schema("DynamicPageResponse", "object", null, List.of(), properties, null);
    }

    private DynamicOpenApiDocument.Schema actionExecutionResponseSchema() {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("context", objectProperty("DynamicWebActionContext"));
        properties.put("body", objectProperty("DynamicWebActionResultBody"));
        return new DynamicOpenApiDocument.Schema("DynamicWebActionExecutionResponse", "object", null,
                List.of(), properties, null);
    }

    private DynamicOpenApiDocument.Schema actionContextSchema() {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("moduleAlias", stringProperty(false));
        properties.put("actionCode", stringProperty(false));
        properties.put("recordId", stringProperty(true));
        properties.put("traceId", stringProperty(true));
        return new DynamicOpenApiDocument.Schema("DynamicWebActionContext", "object", null,
                List.of("moduleAlias", "actionCode"), properties, null);
    }

    private DynamicOpenApiDocument.Schema actionResultBodySchema() {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("type", stringProperty(false));
        properties.put("value", objectProperty("object"));
        properties.put("message", stringProperty(true));
        properties.put("refresh", new DynamicOpenApiDocument.Property("boolean", null, false, false,
                false, null, null, null, null, null, List.of()));
        properties.put("redirectTo", stringProperty(true));
        return new DynamicOpenApiDocument.Schema("DynamicWebActionResultBody", "object", null,
                List.of("type", "refresh"), properties, null);
    }

    private DynamicOpenApiDocument.Schema actionAvailabilityResponseSchema() {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("action", objectProperty("DynamicActionDescriptor"));
        properties.put("available", new DynamicOpenApiDocument.Property("boolean", null, false, false,
                false, null, null, null, null, null, List.of()));
        properties.put("message", stringProperty(true));
        return new DynamicOpenApiDocument.Schema("DynamicWebActionAvailabilityResponse", "object", null,
                List.of("action", "available"), properties, null);
    }

    private DynamicOpenApiDocument.Schema referenceResolveResponseSchema() {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("status", stringProperty(false));
        properties.put("mode", stringProperty(true));
        properties.put("options", arrayProperty("ReferenceOption"));
        properties.put("records", arrayProperty("DynamicRecordResponse"));
        properties.put("pageNum", new DynamicOpenApiDocument.Property("integer", "int32", false, false,
                false, null, null, null, null, null, List.of()));
        properties.put("pageSize", new DynamicOpenApiDocument.Property("integer", "int32", false, false,
                false, null, null, null, null, null, List.of()));
        properties.put("total", new DynamicOpenApiDocument.Property("integer", "int64", false, false,
                false, null, null, null, null, null, List.of()));
        return new DynamicOpenApiDocument.Schema("DynamicReferenceResolveResponse", "object", null,
                List.of(), properties, null);
    }

    private DynamicOpenApiDocument.Schema recordIdResponseSchema() {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("id", stringProperty(false));
        return new DynamicOpenApiDocument.Schema("RecordIdResponse", "object", null,
                List.of("id"), properties, null);
    }

    private DynamicOpenApiDocument.Schema countResponseSchema() {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("count", new DynamicOpenApiDocument.Property("integer", "int32", false, false,
                false, null, null, null, null, null, List.of()));
        return new DynamicOpenApiDocument.Schema("CountResponse", "object", null,
                List.of("count"), properties, null);
    }

    private DynamicOpenApiDocument.Schema arraySchema(String name, String itemName) {
        return new DynamicOpenApiDocument.Schema(name, "array", null, List.of(), Map.of(),
                new DynamicOpenApiDocument.Property(itemName, null, false, false,
                        false, null, null, null, null, null, List.of()));
    }

    private DynamicOpenApiDocument.Property stringProperty(boolean nullable) {
        return new DynamicOpenApiDocument.Property("string", null, false, nullable,
                false, null, null, null, null, null, List.of());
    }

    private DynamicOpenApiDocument.Property objectProperty(String schemaName) {
        return new DynamicOpenApiDocument.Property(schemaName, null, false, true,
                false, null, null, null, null, null, List.of());
    }

    private DynamicOpenApiDocument.Property arrayProperty(String itemName) {
        return new DynamicOpenApiDocument.Property("array", null, false, false,
                true, null, null, null, null, itemName, List.of());
    }

    private DynamicOpenApiDocument.Schema errorSchema(String name, boolean action) {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("code", new DynamicOpenApiDocument.Property("string", null, true, false,
                false, null, null, null, null, null, List.of()));
        properties.put("status", new DynamicOpenApiDocument.Property("integer", "int32", true, false,
                false, null, null, null, null, null, List.of()));
        properties.put("message", new DynamicOpenApiDocument.Property("string", null, true, false,
                false, null, null, null, null, null, List.of()));
        properties.put("traceId", new DynamicOpenApiDocument.Property("string", null, false, true,
                false, null, null, null, null, null, List.of()));
        if (action) {
            properties.put("failureStage", new DynamicOpenApiDocument.Property("string", null, true, false,
                    false, null, null, null, null, null, List.of()));
            properties.put("context", new DynamicOpenApiDocument.Property("object", null, false, true,
                    false, null, null, null, null, null, List.of()));
        }
        return new DynamicOpenApiDocument.Schema(name, "object", null,
                action ? List.of("code", "status", "message", "failureStage") : List.of("code", "status", "message"),
                properties, null);
    }

    private Map<String, DynamicOpenApiDocument.ErrorResponse> errors() {
        return Map.of(
                "DYNAMIC_BAD_REQUEST", new DynamicOpenApiDocument.ErrorResponse("DYNAMIC_BAD_REQUEST", 400, "DynamicWebError"),
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

    private String schemaName(String entityAlias, String suffix) {
        return upperName(entityAlias) + suffix;
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

    private record FieldShape(String type, String format) {
    }
}
