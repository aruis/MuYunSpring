package net.ximatai.muyun.spring.ability.reference;

public record ModuleReadProjection(String path,
                                   ReferencePath referencePath,
                                   String outputField,
                                   ProjectionType projectionType,
                                   boolean filterable,
                                   boolean sortable) {
    public ModuleReadProjection(String path, String outputField) {
        this(path, null, outputField, ProjectionType.FIELD, false, true);
    }

    public ModuleReadProjection {
        if ((path == null || path.isBlank()) && referencePath == null) {
            throw new IllegalArgumentException("module read projection path must not be blank");
        }
        path = path == null || path.isBlank() ? null : path.trim();
        projectionType = projectionType == null ? ProjectionType.FIELD : projectionType;
        if (outputField == null || outputField.isBlank()) {
            outputField = referencePath == null ? defaultOutputField(path) : referencePath.targetField().fieldName();
        } else {
            outputField = outputField.trim();
        }
    }

    public ModuleReadProjection(String path, String outputField, boolean filterable, boolean sortable) {
        this(path, null, outputField, ProjectionType.FIELD, filterable, sortable);
    }

    public static ModuleReadProjection of(String path) {
        return new ModuleReadProjection(path, null);
    }

    public static ModuleReadProjection of(String path, String outputField) {
        return new ModuleReadProjection(path, outputField);
    }

    public static ModuleReadProjection of(ReferencePath referencePath) {
        return new ModuleReadProjection(null, referencePath, null, ProjectionType.FIELD, false, true);
    }

    public static ModuleReadProjection of(ReferencePath referencePath, String outputField) {
        return new ModuleReadProjection(null, referencePath, outputField, ProjectionType.FIELD, false, true);
    }

    public static ModuleReadProjection filterable(String path, String outputField) {
        return new ModuleReadProjection(path, outputField, true, true);
    }

    public static ModuleReadProjection filterable(ReferencePath referencePath, String outputField) {
        return new ModuleReadProjection(null, referencePath, outputField, ProjectionType.FIELD, true, true);
    }

    public static ModuleReadProjection filterableOnly(ReferencePath referencePath, String outputField) {
        return new ModuleReadProjection(null, referencePath, outputField, ProjectionType.FIELD, true, false);
    }

    public static ModuleReadProjection sortableOnly(String path, String outputField) {
        return new ModuleReadProjection(path, outputField, false, true);
    }

    public static ModuleReadProjection sortableOnly(ReferencePath referencePath, String outputField) {
        return new ModuleReadProjection(null, referencePath, outputField, ProjectionType.FIELD, false, true);
    }

    public static ModuleReadProjection exists(ReferencePath referencePath, String outputField) {
        return new ModuleReadProjection(null, referencePath, outputField, ProjectionType.EXISTS, true, false);
    }

    public enum ProjectionType {
        FIELD,
        EXISTS
    }

    private static String defaultOutputField(String path) {
        int index = path.lastIndexOf('.');
        return index < 0 ? path : path.substring(index + 1);
    }
}
