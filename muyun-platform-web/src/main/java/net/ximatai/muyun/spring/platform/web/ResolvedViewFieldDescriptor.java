package net.ximatai.muyun.spring.platform.web;

public record ResolvedViewFieldDescriptor(ViewFieldRef fieldRef,
                                          String label,
                                          UiRule<Boolean> visible,
                                          UiRule<Boolean> required,
                                          UiRule<Boolean> readOnly,
                                          String uiType,
                                          String width,
                                          Integer columnSpan,
                                          String align,
                                          Boolean fixed,
                                          ResolvedOptionFieldDescriptor option,
                                          ResolvedReferenceFieldDescriptor reference) {
    public ResolvedViewFieldDescriptor {
        if (fieldRef == null) {
            throw new IllegalArgumentException("resolved view field ref must not be null");
        }
        label = label == null || label.isBlank() ? null : label.trim();
        visible = visible == null ? UiRule.constant(Boolean.TRUE) : visible;
        required = required == null ? UiRule.constant(Boolean.FALSE) : required;
        readOnly = readOnly == null ? UiRule.constant(Boolean.FALSE) : readOnly;
        uiType = uiType == null || uiType.isBlank() ? null : uiType.trim();
        width = width == null || width.isBlank() ? null : width.trim();
        columnSpan = columnSpan == null ? 1 : requireColumnSpan(columnSpan);
        align = align == null || align.isBlank() ? null : align.trim();
    }

    public ResolvedViewFieldDescriptor(ViewFieldRef fieldRef,
                                       String label,
                                       UiRule<Boolean> visible,
                                       UiRule<Boolean> required,
                                       UiRule<Boolean> readOnly,
                                       String uiType,
                                       String width,
                                       Integer columnSpan,
                                       String align,
                                       Boolean fixed) {
        this(fieldRef, label, visible, required, readOnly, uiType, width, columnSpan, align, fixed, null, null);
    }

    /** Source-compatible constructor for descriptors with option metadata only. */
    public ResolvedViewFieldDescriptor(ViewFieldRef fieldRef,
                                       String label,
                                       UiRule<Boolean> visible,
                                       UiRule<Boolean> required,
                                       UiRule<Boolean> readOnly,
                                       String uiType,
                                       String width,
                                       Integer columnSpan,
                                       String align,
                                       Boolean fixed,
                                       ResolvedOptionFieldDescriptor option) {
        this(fieldRef, label, visible, required, readOnly, uiType, width, columnSpan, align, fixed, option, null);
    }

    /** Source-compatible constructor for descriptors created before column spans were introduced. */
    public ResolvedViewFieldDescriptor(ViewFieldRef fieldRef,
                                       String label,
                                       UiRule<Boolean> visible,
                                       UiRule<Boolean> required,
                                       UiRule<Boolean> readOnly,
                                       String uiType,
                                       String width,
                                       String align,
                                       Boolean fixed) {
        this(fieldRef, label, visible, required, readOnly, uiType, width, 1, align, fixed, null, null);
    }

    private static int requireColumnSpan(int value) {
        if (value < 1 || value > 2) {
            throw new IllegalArgumentException("columnSpan must be between 1 and 2");
        }
        return value;
    }
}
