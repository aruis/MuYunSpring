package net.ximatai.muyun.spring.dynamic.openapi;

import net.ximatai.muyun.spring.common.option.OptionBinding;
import net.ximatai.muyun.spring.common.option.OptionSelectionMode;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicActionDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicEntityDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicFieldDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicReferenceDescriptor;
import net.ximatai.muyun.spring.dynamic.metadata.FieldType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class DynamicOpenApiSchemaFactory {
    Map<String, DynamicOpenApiDocument.Schema> schemas(DynamicEntityDescriptor entity) {
        Map<String, DynamicOpenApiDocument.Schema> schemas = new LinkedHashMap<>();
        schemas.put(schemaName(entity.entityAlias(), "Values"), valuesSchema(entity));
        schemas.put(schemaName(entity.entityAlias(), "Record"), recordSchema(entity));
        schemas.put("DynamicRecordPayload", recordPayloadSchema(entity));
        schemas.put("DynamicRecordResponse", recordResponseSchema(entity));
        schemas.put("WebQueryRequest", queryRequestSchema("WebQueryRequest", "WebQueryCondition", "WebPageRequest", "WebSort"));
        schemas.put("WebQueryCondition", queryConditionSchema("WebQueryCondition"));
        schemas.put("WebPageRequest", pageRequestSchema("WebPageRequest"));
        schemas.put("WebSort", sortSchema("WebSort"));
        schemas.put("DynamicWebActionRequest", actionRequestSchema());
        schemas.put("DynamicWebReferenceRequest", referenceRequestSchema());
        schemas.put("WebPageResponse", pageResponseSchema("WebPageResponse"));
        schemas.put("DynamicPageResponse", pageResponseSchema("DynamicPageResponse"));
        schemas.put("DynamicWebActionExecutionResponse", actionExecutionResponseSchema());
        schemas.put("DynamicReferenceResolveResponse", referenceResolveResponseSchema());
        schemas.put("DynamicModuleDescriptor", moduleDescriptorSchema());
        schemas.put("DynamicActionDescriptor", actionDescriptorSchema());
        schemas.put("DynamicEntityDescriptor", entityDescriptorSchema());
        schemas.put("DynamicFieldDescriptor", fieldDescriptorSchema());
        schemas.put("DynamicFieldCompanionDescriptor", fieldCompanionDescriptorSchema());
        schemas.put("DynamicFieldQueryDescriptor", fieldQueryDescriptorSchema());
        schemas.put("DynamicFormulaRuleDescriptor", formulaRuleDescriptorSchema());
        schemas.put("DynamicRelationDescriptor", relationDescriptorSchema());
        schemas.put("DynamicReferenceDescriptor", referenceDescriptorSchema());
        schemas.put("DynamicReferenceProjectionDescriptor", referenceProjectionDescriptorSchema());
        schemas.put("DynamicAssociationViewDescriptor", associationViewDescriptorSchema());
        schemas.put("DynamicViewDescriptor", viewDescriptorSchema());
        schemas.put("DynamicViewFieldDescriptor", viewFieldDescriptorSchema());
        schemas.put("DynamicOpenApiDocument", openApiDocumentSchema());
        schemas.put("DynamicOpenApiOperation", openApiOperationSchema());
        schemas.put("DynamicOpenApiSchema", openApiSchemaSchema());
        schemas.put("DynamicOpenApiProperty", openApiPropertySchema());
        schemas.put("DynamicOpenApiErrorResponse", openApiErrorResponseSchema());
        schemas.put("DynamicActionDescriptorList", arraySchema("DynamicActionDescriptorList", "DynamicActionDescriptor"));
        schemas.put("WebListResponse", listResponseSchema("WebListResponse"));
        schemas.put("DynamicWebActionAvailabilityList", arraySchema("DynamicWebActionAvailabilityList", "DynamicWebActionAvailabilityResponse"));
        schemas.put("DynamicWebActionAvailabilityResponse", actionAvailabilityResponseSchema());
        schemas.put("DynamicWebActionContext", actionContextSchema());
        schemas.put("DynamicWebActionResultBody", actionResultBodySchema());
        schemas.put("DynamicActionDialog", actionDialogSchema());
        schemas.put("DynamicReferenceResolveItem", referenceResolveItemSchema());
        schemas.put("DynamicReferenceResolveResult", referenceResolveResultSchema());
        schemas.put("WebCountResponse", countResponseSchema("WebCountResponse"));
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

    private DynamicOpenApiDocument.Schema actionDescriptorSchema() {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("code", stringProperty(false));
        properties.put("kind", stringProperty(false));
        properties.put("title", stringProperty(false));
        properties.put("enabled", booleanProperty(false));
        properties.put("style", stringProperty(false));
        properties.put("actionLevel", stringProperty(false));
        properties.put("category", stringProperty(false));
        properties.put("accessMode", stringProperty(false));
        properties.put("actionAuth", booleanProperty(false));
        properties.put("dataAuth", booleanProperty(false));
        properties.put("authInheritActionCode", stringProperty(true));
        properties.put("availabilityCondition", booleanProperty(false));
        properties.put("unavailableMessage", stringProperty(true));
        properties.put("executorType", stringProperty(false));
        properties.put("executorKey", stringProperty(true));
        return new DynamicOpenApiDocument.Schema("DynamicActionDescriptor", "object", null,
                List.of("code", "kind", "title", "enabled", "actionLevel", "executorType"), properties, null);
    }

    private DynamicOpenApiDocument.Schema entityDescriptorSchema() {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("entityAlias", stringProperty(false));
        properties.put("title", stringProperty(false));
        properties.put("capabilities", arrayProperty("string"));
        properties.put("fields", arrayProperty("DynamicFieldDescriptor"));
        properties.put("formulaRules", arrayProperty("DynamicFormulaRuleDescriptor"));
        properties.put("actions", arrayProperty("DynamicActionDescriptor"));
        properties.put("views", arrayProperty("DynamicViewDescriptor"));
        properties.put("associationViews", arrayProperty("DynamicAssociationViewDescriptor"));
        return new DynamicOpenApiDocument.Schema("DynamicEntityDescriptor", "object", null,
                List.of("entityAlias", "title"), properties, null);
    }

    private DynamicOpenApiDocument.Schema fieldDescriptorSchema() {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("fieldName", stringProperty(false));
        properties.put("type", stringProperty(false));
        properties.put("title", stringProperty(false));
        properties.put("required", booleanProperty(false));
        properties.put("unique", booleanProperty(false));
        properties.put("indexed", booleanProperty(false));
        properties.put("sortable", booleanProperty(false));
        properties.put("titleField", booleanProperty(false));
        properties.put("length", integerProperty(true));
        properties.put("precision", integerProperty(true));
        properties.put("scale", integerProperty(true));
        properties.put("optionBinding", objectProperty("object"));
        properties.put("selectionMode", stringProperty(true));
        properties.put("reference", objectProperty("DynamicReferenceDescriptor"));
        properties.put("companions", arrayProperty("DynamicFieldCompanionDescriptor"));
        properties.put("query", objectProperty("DynamicFieldQueryDescriptor"));
        properties.put("defaultValue", stringProperty(true));
        properties.put("validationRegex", stringProperty(true));
        properties.put("copyable", booleanProperty(false));
        properties.put("writeProtected", booleanProperty(false));
        return new DynamicOpenApiDocument.Schema("DynamicFieldDescriptor", "object", null,
                List.of("fieldName", "type", "title"), properties, null);
    }

    private DynamicOpenApiDocument.Schema fieldCompanionDescriptorSchema() {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("fieldName", stringProperty(false));
        properties.put("kind", stringProperty(false));
        properties.put("role", stringProperty(false));
        properties.put("requiredWhenOwnerPresent", booleanProperty(false));
        properties.put("requiredWhenOwnerUpdated", booleanProperty(false));
        return new DynamicOpenApiDocument.Schema("DynamicFieldCompanionDescriptor", "object", null,
                List.of("fieldName", "kind", "role"), properties, null);
    }

    private DynamicOpenApiDocument.Schema fieldQueryDescriptorSchema() {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("queryable", booleanProperty(false));
        properties.put("defaultOperator", stringProperty(true));
        properties.put("operators", arrayProperty("string"));
        return new DynamicOpenApiDocument.Schema("DynamicFieldQueryDescriptor", "object", null,
                List.of("queryable"), properties, null);
    }

    private DynamicOpenApiDocument.Schema formulaRuleDescriptorSchema() {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("code", stringProperty(false));
        properties.put("expression", stringProperty(false));
        properties.put("kind", stringProperty(false));
        properties.put("phase", stringProperty(false));
        properties.put("targetField", stringProperty(true));
        properties.put("severity", stringProperty(false));
        properties.put("messageTemplate", stringProperty(true));
        properties.put("stopOnError", booleanProperty(false));
        properties.put("enabled", booleanProperty(false));
        properties.put("sortOrder", integerProperty(false));
        return new DynamicOpenApiDocument.Schema("DynamicFormulaRuleDescriptor", "object", null,
                List.of("code", "expression", "kind", "phase"), properties, null);
    }

    private DynamicOpenApiDocument.Schema relationDescriptorSchema() {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("code", stringProperty(false));
        properties.put("parentEntityAlias", stringProperty(false));
        properties.put("childEntityAlias", stringProperty(false));
        properties.put("childForeignKeyField", stringProperty(false));
        properties.put("autoPopulate", booleanProperty(false));
        properties.put("autoDeleteWithParent", booleanProperty(false));
        return new DynamicOpenApiDocument.Schema("DynamicRelationDescriptor", "object", null,
                List.of("code", "parentEntityAlias", "childEntityAlias"), properties, null);
    }

    private DynamicOpenApiDocument.Schema referenceDescriptorSchema() {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("sourceEntityAlias", stringProperty(false));
        properties.put("sourceField", stringProperty(false));
        properties.put("targetModuleAlias", stringProperty(false));
        properties.put("targetEntityAlias", stringProperty(false));
        properties.put("cardinality", stringProperty(false));
        properties.put("autoTitle", booleanProperty(false));
        properties.put("titleOutputField", stringProperty(true));
        properties.put("projections", arrayProperty("DynamicReferenceProjectionDescriptor"));
        return new DynamicOpenApiDocument.Schema("DynamicReferenceDescriptor", "object", null,
                List.of("sourceEntityAlias", "sourceField", "targetModuleAlias", "targetEntityAlias"),
                properties, null);
    }

    private DynamicOpenApiDocument.Schema referenceProjectionDescriptorSchema() {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("targetField", stringProperty(false));
        properties.put("outputField", stringProperty(false));
        return new DynamicOpenApiDocument.Schema("DynamicReferenceProjectionDescriptor", "object", null,
                List.of("targetField", "outputField"), properties, null);
    }

    private DynamicOpenApiDocument.Schema associationViewDescriptorSchema() {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("code", stringProperty(false));
        properties.put("sourceEntityAlias", stringProperty(false));
        properties.put("targetModuleAlias", stringProperty(false));
        properties.put("targetEntityAlias", stringProperty(false));
        properties.put("displayMode", stringProperty(false));
        properties.put("relationCode", stringProperty(true));
        properties.put("referenceField", stringProperty(true));
        properties.put("viewType", stringProperty(true));
        properties.put("queryable", booleanProperty(false));
        return new DynamicOpenApiDocument.Schema("DynamicAssociationViewDescriptor", "object", null,
                List.of("code", "sourceEntityAlias", "targetModuleAlias", "targetEntityAlias"), properties, null);
    }

    private DynamicOpenApiDocument.Schema viewDescriptorSchema() {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("viewType", stringProperty(false));
        properties.put("title", stringProperty(false));
        properties.put("fields", arrayProperty("DynamicViewFieldDescriptor"));
        return new DynamicOpenApiDocument.Schema("DynamicViewDescriptor", "object", null,
                List.of("viewType", "title"), properties, null);
    }

    private DynamicOpenApiDocument.Schema viewFieldDescriptorSchema() {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("fieldName", stringProperty(false));
        properties.put("title", stringProperty(false));
        properties.put("visible", booleanProperty(false));
        properties.put("controlType", stringProperty(false));
        properties.put("companions", arrayProperty("DynamicFieldCompanionDescriptor"));
        properties.put("readOnly", booleanProperty(false));
        properties.put("required", booleanProperty(false));
        return new DynamicOpenApiDocument.Schema("DynamicViewFieldDescriptor", "object", null,
                List.of("fieldName", "title", "visible", "controlType"), properties, null);
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
        properties.put("valueShapeByResultType", objectProperty("string"));
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

    private DynamicOpenApiDocument.Schema queryRequestSchema(String name,
                                                             String conditionSchema,
                                                             String pageSchema,
                                                             String sortSchema) {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("conditions", arrayProperty(conditionSchema));
        properties.put("page", objectProperty(pageSchema));
        properties.put("sorts", arrayProperty(sortSchema));
        return new DynamicOpenApiDocument.Schema(name, "object", null, List.of(), properties, null);
    }

    private DynamicOpenApiDocument.Schema queryConditionSchema(String name) {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("fieldName", stringProperty(false));
        properties.put("operator", stringProperty(true));
        properties.put("values", arrayProperty("object"));
        return new DynamicOpenApiDocument.Schema(name, "object", null, List.of(), properties, null);
    }

    private DynamicOpenApiDocument.Schema pageRequestSchema(String name) {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("pageNum", new DynamicOpenApiDocument.Property("integer", "int32", false, false,
                false, null, null, null, null, null, List.of()));
        properties.put("pageSize", new DynamicOpenApiDocument.Property("integer", "int32", false, false,
                false, null, null, null, null, null, List.of()));
        return new DynamicOpenApiDocument.Schema(name, "object", null, List.of(), properties, null);
    }

    private DynamicOpenApiDocument.Schema sortSchema(String name) {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("field", stringProperty(false));
        properties.put("desc", new DynamicOpenApiDocument.Property("boolean", null, false, false,
                false, null, null, null, null, null, List.of()));
        return new DynamicOpenApiDocument.Schema(name, "object", null, List.of(), properties, null);
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
        properties.put("conditions", arrayProperty("WebQueryCondition"));
        properties.put("page", objectProperty("WebPageRequest"));
        properties.put("sorts", arrayProperty("WebSort"));
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
        properties.put("conditions", arrayProperty("WebQueryCondition"));
        properties.put("page", objectProperty("WebPageRequest"));
        properties.put("includeProjections", new DynamicOpenApiDocument.Property("boolean", null, false, true,
                false, null, null, null, null, null, List.of()));
        return new DynamicOpenApiDocument.Schema("DynamicWebReferenceRequest", "object", null, List.of(), properties, null);
    }

    private DynamicOpenApiDocument.Schema pageResponseSchema(String name) {
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
        return new DynamicOpenApiDocument.Schema(name, "object", null, List.of(), properties, null);
    }

    private DynamicOpenApiDocument.Schema listResponseSchema(String name) {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("records", arrayProperty("DynamicRecordResponse"));
        return new DynamicOpenApiDocument.Schema(name, "object", null, List.of(), properties, null);
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
        properties.put("actionLevel", stringProperty(false));
        properties.put("executorType", stringProperty(false));
        properties.put("recordId", stringProperty(true));
        properties.put("traceId", stringProperty(true));
        return new DynamicOpenApiDocument.Schema("DynamicWebActionContext", "object", null,
                List.of("moduleAlias", "actionCode", "actionLevel", "executorType"), properties, null);
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
                List.of("type", "refresh"), properties, null, actionResultValueShapeByType());
    }

    private DynamicOpenApiDocument.Schema actionDialogSchema() {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("dialogKey", stringProperty(false));
        properties.put("title", stringProperty(true));
        return new DynamicOpenApiDocument.Schema("DynamicActionDialog", "object", null,
                List.of("dialogKey"), properties, null);
    }

    private Map<String, String> actionResultValueShapeByType() {
        return Map.of(
                "VALUE", "scalar",
                "RECORD_ID", "string",
                "RECORD", "DynamicRecordResponse",
                "LIST", "array",
                "PAGE", "DynamicPageResponse",
                "COUNT", "integer",
                "OBJECT", "object",
                "DIALOG", "DynamicActionDialog",
                "NONE", "null"
        );
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
        properties.put("options", arrayProperty("DynamicReferenceResolveItem"));
        properties.put("results", arrayProperty("DynamicReferenceResolveResult"));
        properties.put("offset", new DynamicOpenApiDocument.Property("integer", "int32", false, false,
                false, null, null, null, null, null, List.of()));
        properties.put("limit", new DynamicOpenApiDocument.Property("integer", "int32", false, false,
                false, null, null, null, null, null, List.of()));
        properties.put("total", new DynamicOpenApiDocument.Property("integer", "int64", false, false,
                false, null, null, null, null, null, List.of()));
        return new DynamicOpenApiDocument.Schema("DynamicReferenceResolveResponse", "object", null,
                List.of(), properties, null);
    }

    private DynamicOpenApiDocument.Schema referenceResolveItemSchema() {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("id", stringProperty(false));
        properties.put("title", stringProperty(false));
        properties.put("matchedBy", stringProperty(true));
        properties.put("projections", objectProperty("object"));
        return new DynamicOpenApiDocument.Schema("DynamicReferenceResolveItem", "object", null,
                List.of("id", "title"), properties, null);
    }

    private DynamicOpenApiDocument.Schema referenceResolveResultSchema() {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("input", objectProperty("object"));
        properties.put("status", stringProperty(false));
        properties.put("matchedBy", stringProperty(true));
        properties.put("item", objectProperty("DynamicReferenceResolveItem"));
        properties.put("candidates", arrayProperty("DynamicReferenceResolveItem"));
        return new DynamicOpenApiDocument.Schema("DynamicReferenceResolveResult", "object", null,
                List.of("status"), properties, null);
    }

    private DynamicOpenApiDocument.Schema countResponseSchema(String name) {
        Map<String, DynamicOpenApiDocument.Property> properties = new LinkedHashMap<>();
        properties.put("count", new DynamicOpenApiDocument.Property("integer", "int32", false, false,
                false, null, null, null, null, null, List.of()));
        return new DynamicOpenApiDocument.Schema(name, "object", null,
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

    private DynamicOpenApiDocument.Property booleanProperty(boolean nullable) {
        return new DynamicOpenApiDocument.Property("boolean", null, false, nullable,
                false, null, null, null, null, null, List.of());
    }

    private DynamicOpenApiDocument.Property integerProperty(boolean nullable) {
        return new DynamicOpenApiDocument.Property("integer", "int32", false, nullable,
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
