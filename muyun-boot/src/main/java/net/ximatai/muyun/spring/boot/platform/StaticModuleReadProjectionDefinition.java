package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.ability.reference.ModuleReferencePath;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;

public record StaticModuleReadProjectionDefinition(String path,
                                                   ModuleReferencePath referencePath,
                                                   String outputField,
                                                   boolean filterable,
                                                   boolean sortable) {
    public StaticModuleReadProjectionDefinition(String path, String outputField) {
        this(path, null, outputField, false, true);
    }

    public StaticModuleReadProjectionDefinition(ModuleReferencePath referencePath, String outputField) {
        this(null, referencePath, outputField, false, true);
    }

    public StaticModuleReadProjectionDefinition {
        if ((path == null || path.isBlank()) && referencePath == null) {
            throw new IllegalArgumentException("static module read projection path must not be blank");
        }
        path = path == null || path.isBlank() ? null : path.trim();
        if (referencePath == null && !path.contains(".")) {
            throw new IllegalArgumentException("static module read projection path requires reference field path: " + path);
        }
        outputField = outputField == null || outputField.isBlank()
                ? defaultOutputField(path, referencePath)
                : outputField.trim();
        outputField = PlatformNameRules.requireFieldName(outputField, "readProjectionOutputField");
    }

    public StaticModuleReadProjectionDefinition(String path,
                                                String outputField,
                                                boolean filterable,
                                                boolean sortable) {
        this(path, null, outputField, filterable, sortable);
    }

    private static String defaultOutputField(String path, ModuleReferencePath referencePath) {
        if (referencePath != null) {
            return referencePath.targetField().fieldName();
        }
        int index = path.lastIndexOf('.');
        return index < 0 ? path : path.substring(index + 1);
    }
}
