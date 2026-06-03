package net.ximatai.muyun.spring.dynamic.openapi;

import net.ximatai.muyun.spring.dynamic.descriptor.DynamicActionDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicEntityDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicModuleDescriptor;
import net.ximatai.muyun.spring.dynamic.metadata.DynamicActionPathRules;
import net.ximatai.muyun.spring.dynamic.metadata.EntityCapability;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionCategory;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;

import java.util.ArrayList;
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
        operations.add(operation(basePath + "/describe", "describe" + upperModuleName(descriptor.moduleAlias()),
                "Describe " + descriptor.title(), null, "DynamicModuleDescriptor", null));
        operations.add(operation(basePath + "/openapi", operationId(descriptor, "openApi"),
                "OpenAPI " + descriptor.title(), null, "DynamicOpenApiDocument", null));
        operations.add(operation(basePath + "/query", operationId(descriptor, "query"),
                "Query " + mainEntity.title(), "WebQueryRequest", "WebPageResponse", null));
        operations.add(operation(basePath + "/view/{id}", operationId(descriptor, "view"),
                "View " + mainEntity.title(), null, "DynamicRecordResponse", null));
        operations.add(operation(basePath + "/insert", operationId(descriptor, "insert"),
                "Insert " + mainEntity.title(), "DynamicRecordPayload", "DynamicRecordResponse", null));
        operations.add(operation(basePath + "/update/{id}", operationId(descriptor, "update"),
                "Update " + mainEntity.title(), "DynamicRecordPayload", "DynamicRecordResponse", null));
        operations.add(operation(basePath + "/delete/{id}", operationId(descriptor, "delete"),
                "Delete " + mainEntity.title(), null, "WebCountResponse", null));
        if (mainEntity.capabilities().contains(EntityCapability.ENABLE.name())) {
            operations.add(operation(basePath + "/enable/{id}", operationId(descriptor, "enable"),
                    "Enable " + mainEntity.title(), null, "WebCountResponse", null));
            operations.add(operation(basePath + "/disable/{id}", operationId(descriptor, "disable"),
                    "Disable " + mainEntity.title(), null, "WebCountResponse", null));
        }
        if (mainEntity.capabilities().contains(EntityCapability.SORT.name())) {
            String sortRequestSchema = mainEntity.capabilities().contains(EntityCapability.TREE.name())
                    ? "TreeSortWebRequest"
                    : "SortWebRequest";
            operations.add(operation(basePath + "/sort/{id}", operationId(descriptor, "sort"),
                    "Sort " + mainEntity.title(), sortRequestSchema, "WebCountResponse", null));
        }
        if (mainEntity.capabilities().contains(EntityCapability.TREE.name())) {
            operations.add(operation(basePath + "/tree", operationId(descriptor, "tree"),
                    "Tree " + mainEntity.title(), null, "WebListResponse", null));
            operations.add(operation(basePath + "/tree/{id}", operationId(descriptor, "treeNode"),
                    "Tree node " + mainEntity.title(), null, "WebListResponse", null));
        }
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
        return new DynamicOpenApiSchemaFactory().schemas(entity);
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
