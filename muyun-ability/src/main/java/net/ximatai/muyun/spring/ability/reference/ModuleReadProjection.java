package net.ximatai.muyun.spring.ability.reference;

public record ModuleReadProjection(String path,
                                   String outputField,
                                   boolean filterable,
                                   boolean sortable) {
    public ModuleReadProjection(String path, String outputField) {
        this(path, outputField, false, true);
    }

    public ModuleReadProjection {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("module read projection path must not be blank");
        }
        path = path.trim();
        if (outputField == null || outputField.isBlank()) {
            outputField = defaultOutputField(path);
        } else {
            outputField = outputField.trim();
        }
    }

    public static ModuleReadProjection of(String path) {
        return new ModuleReadProjection(path, null);
    }

    public static ModuleReadProjection of(String path, String outputField) {
        return new ModuleReadProjection(path, outputField);
    }

    public static ModuleReadProjection filterable(String path, String outputField) {
        return new ModuleReadProjection(path, outputField, true, true);
    }

    public static ModuleReadProjection sortableOnly(String path, String outputField) {
        return new ModuleReadProjection(path, outputField, false, true);
    }

    private static String defaultOutputField(String path) {
        int index = path.lastIndexOf('.');
        return index < 0 ? path : path.substring(index + 1);
    }
}
