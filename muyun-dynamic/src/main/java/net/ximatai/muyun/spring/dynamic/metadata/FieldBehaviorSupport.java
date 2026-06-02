package net.ximatai.muyun.spring.dynamic.metadata;

public final class FieldBehaviorSupport {
    private FieldBehaviorSupport() {
    }

    public static Object parseDefaultValue(FieldType type, String value) {
        return DynamicFieldValueSupport.parseDefaultValue(type, value);
    }

    public static void validateBehavior(FieldType type, FieldBehaviorDefinition behavior, String fieldCode) {
        if (behavior.validationRegex() != null) {
            if (type != FieldType.STRING && type != FieldType.TEXT) {
                throw new IllegalArgumentException("validationRegex requires string field: " + fieldCode);
            }
            java.util.regex.Pattern.compile(behavior.validationRegex());
        }
        if (behavior.defaultValue() != null) {
            Object parsed = parseDefaultValue(type, behavior.defaultValue());
            if (behavior.validationRegex() != null
                    && parsed instanceof String text
                    && !text.matches(behavior.validationRegex())) {
                throw new IllegalArgumentException("defaultValue does not match validationRegex: " + fieldCode);
            }
        }
    }

}
