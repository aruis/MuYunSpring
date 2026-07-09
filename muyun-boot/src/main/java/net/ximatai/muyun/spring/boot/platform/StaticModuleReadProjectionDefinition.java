package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.common.util.PlatformNameRules;

public record StaticModuleReadProjectionDefinition(String path,
                                                   String outputField,
                                                   boolean filterable,
                                                   boolean sortable) {
    public StaticModuleReadProjectionDefinition(String path, String outputField) {
        this(path, outputField, false, true);
    }

    public StaticModuleReadProjectionDefinition {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("static module read projection path must not be blank");
        }
        path = path.trim();
        if (!path.contains(".")) {
            throw new IllegalArgumentException("static module read projection path requires reference field path: " + path);
        }
        outputField = outputField == null || outputField.isBlank() ? defaultOutputField(path) : outputField.trim();
        outputField = PlatformNameRules.requireFieldName(outputField, "readProjectionOutputField");
    }

    private static String defaultOutputField(String path) {
        int index = path.lastIndexOf('.');
        return index < 0 ? path : path.substring(index + 1);
    }
}
