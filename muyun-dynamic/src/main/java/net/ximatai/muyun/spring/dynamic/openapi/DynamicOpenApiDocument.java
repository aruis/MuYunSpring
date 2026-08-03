package net.ximatai.muyun.spring.dynamic.openapi;

import net.ximatai.muyun.spring.common.openapi.PlatformApiDocument;

import java.util.List;
import java.util.Map;

public record DynamicOpenApiDocument(
        String moduleAlias,
        String title,
        String basePath,
        List<Operation> operations,
        Map<String, Schema> schemas,
        Map<String, ErrorResponse> errors
) implements PlatformApiDocument {
    public DynamicOpenApiDocument {
        operations = operations == null ? List.of() : List.copyOf(operations);
        schemas = schemas == null ? Map.of() : Map.copyOf(schemas);
        errors = errors == null ? Map.of() : Map.copyOf(errors);
    }

}
