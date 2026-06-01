package net.ximatai.muyun.spring.dynamic.metadata;

public record FieldBehaviorDefinition(
        String defaultValue,
        String validationRegex,
        boolean copyable,
        boolean writeProtected
) {
    public static final FieldBehaviorDefinition DEFAULT = new FieldBehaviorDefinition(null, null, true, false);

    public FieldBehaviorDefinition {
        if (validationRegex != null && validationRegex.isBlank()) {
            validationRegex = null;
        }
    }
}
