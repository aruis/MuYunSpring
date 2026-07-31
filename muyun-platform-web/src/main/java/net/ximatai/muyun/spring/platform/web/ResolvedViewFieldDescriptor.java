package net.ximatai.muyun.spring.platform.web;

public record ResolvedViewFieldDescriptor(ViewFieldRef fieldRef,
                                          String label,
                                          UiRule<Boolean> visible,
                                          UiRule<Boolean> required,
                                          UiRule<Boolean> readOnly,
                                          String uiType,
                                          String width,
                                          String align,
                                          Boolean fixed,
                                          ResolvedOptionFieldDescriptor option) {
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
        align = align == null || align.isBlank() ? null : align.trim();
    }

    public ResolvedViewFieldDescriptor(ViewFieldRef fieldRef,
                                       String label,
                                       UiRule<Boolean> visible,
                                       UiRule<Boolean> required,
                                       UiRule<Boolean> readOnly,
                                       String uiType,
                                       String width,
                                       String align,
                                       Boolean fixed) {
        this(fieldRef, label, visible, required, readOnly, uiType, width, align, fixed, null);
    }
}
