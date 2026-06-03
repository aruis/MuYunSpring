package net.ximatai.muyun.spring.dynamic.openapi;

import java.util.List;
import java.util.Map;

public record DynamicOpenApiDocument(
        String moduleAlias,
        String title,
        String basePath,
        List<Operation> operations,
        Map<String, Schema> schemas,
        Map<String, ErrorResponse> errors
) {
    public DynamicOpenApiDocument {
        operations = operations == null ? List.of() : List.copyOf(operations);
        schemas = schemas == null ? Map.of() : Map.copyOf(schemas);
        errors = errors == null ? Map.of() : Map.copyOf(errors);
    }

    public record Operation(
            String method,
            String path,
            String operationId,
            String summary,
            String requestSchema,
            String responseSchema,
            String actionCode,
            String permissionCode,
            List<String> errorCodes
    ) {
        public Operation {
            errorCodes = errorCodes == null ? List.of() : List.copyOf(errorCodes);
        }
    }

    public record Schema(
            String name,
            String type,
            String format,
            List<String> required,
            Map<String, Property> properties,
            Property items,
            Map<String, String> valueShapeByResultType
    ) {
        public Schema {
            required = required == null ? List.of() : List.copyOf(required);
            properties = properties == null ? Map.of() : Map.copyOf(properties);
            valueShapeByResultType = valueShapeByResultType == null
                    ? Map.of()
                    : Map.copyOf(valueShapeByResultType);
        }

        public Schema(String name,
                      String type,
                      String format,
                      List<String> required,
                      Map<String, Property> properties,
                      Property items) {
            this(name, type, format, required, properties, items, Map.of());
        }
    }

    public record Property(
            String type,
            String format,
            boolean required,
            boolean nullable,
            boolean multiple,
            String optionSourceType,
            String optionSource,
            String referenceModuleAlias,
            String referenceEntityAlias,
            String itemType,
            List<String> companionFields
    ) {
        public Property {
            companionFields = companionFields == null ? List.of() : List.copyOf(companionFields);
        }
    }

    public record ErrorResponse(
            String code,
            int status,
            String schemaName
    ) {
    }
}
