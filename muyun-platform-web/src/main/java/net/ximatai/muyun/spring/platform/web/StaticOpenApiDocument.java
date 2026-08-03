package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.spring.common.openapi.PlatformApiDocument;

import java.util.List;
import java.util.Map;

/** Static-module implementation of the shared module API document contract. */
public record StaticOpenApiDocument(String moduleAlias, String title, String basePath,
                                    List<Operation> operations, Map<String, Schema> schemas,
                                    Map<String, ErrorResponse> errors) implements PlatformApiDocument {
    public StaticOpenApiDocument {
        operations = operations == null ? List.of() : List.copyOf(operations);
        schemas = schemas == null ? Map.of() : Map.copyOf(schemas);
        errors = errors == null ? Map.of() : Map.copyOf(errors);
    }
}
