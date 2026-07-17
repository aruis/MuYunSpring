package net.ximatai.muyun.spring.boot.platform;

import java.util.Optional;

public record RecordReadPostTransform(String transformType,
                                      String fieldName) {
    public static final String FIELD_PROTECTION = "fieldProtection";
    public static final String OPTION_TITLE = "optionTitle";
    private static final String SEPARATOR = ":";

    public RecordReadPostTransform {
        if (transformType == null || transformType.isBlank()) {
            throw new IllegalArgumentException("record read post transform type must not be blank");
        }
        if (fieldName == null || fieldName.isBlank()) {
            throw new IllegalArgumentException("record read post transform field name must not be blank");
        }
        transformType = transformType.trim();
        fieldName = fieldName.trim();
    }

    public static RecordReadPostTransform fieldProtection(String fieldName) {
        return new RecordReadPostTransform(FIELD_PROTECTION, fieldName);
    }

    public static RecordReadPostTransform optionTitle(String fieldName) {
        return new RecordReadPostTransform(OPTION_TITLE, fieldName);
    }

    public static Optional<RecordReadPostTransform> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String trimmed = value.trim();
        int separator = trimmed.indexOf(SEPARATOR);
        if (separator <= 0 || separator == trimmed.length() - 1 || separator != trimmed.lastIndexOf(SEPARATOR)) {
            return Optional.empty();
        }
        return Optional.of(new RecordReadPostTransform(
                trimmed.substring(0, separator),
                trimmed.substring(separator + 1)
        ));
    }

    public boolean isFieldProtection() {
        return FIELD_PROTECTION.equals(transformType);
    }

    public boolean isOptionTitle() {
        return OPTION_TITLE.equals(transformType);
    }

    public String serialize() {
        return transformType + SEPARATOR + fieldName;
    }
}
