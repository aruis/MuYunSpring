package net.ximatai.muyun.spring.platform.web;

public record ViewFieldDefinition(ViewFieldRef fieldRef,
                                  String label,
                                  UiRule<Boolean> visible,
                                  UiRule<Boolean> required,
                                  UiRule<Boolean> readOnly,
                                  String uiType,
                                  String width,
                                  Integer columnSpan,
                                  String align,
                                  Boolean fixed,
                                  BooleanStatusPresentation booleanStatus) {
    public ViewFieldDefinition {
        if (fieldRef == null) {
            throw new IllegalArgumentException("view field ref must not be null");
        }
        label = label == null || label.isBlank() ? null : label.trim();
        visible = visible == null ? UiRule.constant(Boolean.TRUE) : visible;
        required = required == null ? UiRule.constant(Boolean.FALSE) : required;
        readOnly = readOnly == null ? UiRule.constant(Boolean.FALSE) : readOnly;
        uiType = uiType == null || uiType.isBlank() ? null : uiType.trim();
        width = width == null || width.isBlank() ? null : width.trim();
        columnSpan = columnSpan == null ? 1 : requireColumnSpan(columnSpan);
        align = align == null || align.isBlank() ? null : align.trim();
        if (booleanStatus != null && !"booleanStatus".equals(uiType)) {
            throw new IllegalArgumentException("boolean status presentation requires uiType booleanStatus");
        }
        if ("booleanStatus".equals(uiType) && booleanStatus == null) {
            throw new IllegalArgumentException("uiType booleanStatus requires boolean status presentation");
        }
    }

    public static Builder field(String fieldName) {
        return new Builder(ViewFieldRef.main(fieldName));
    }

    public static Builder field(String relationCode, String fieldName) {
        return new Builder(ViewFieldRef.relation(relationCode, fieldName));
    }

    public static final class Builder {
        private final ViewFieldRef fieldRef;
        private String label;
        private UiRule<Boolean> visible = UiRule.constant(Boolean.TRUE);
        private UiRule<Boolean> required = UiRule.constant(Boolean.FALSE);
        private UiRule<Boolean> readOnly = UiRule.constant(Boolean.FALSE);
        private String uiType;
        private String width;
        private Integer columnSpan = 1;
        private String align;
        private Boolean fixed;
        private BooleanStatusPresentation booleanStatus;

        private Builder(ViewFieldRef fieldRef) {
            this.fieldRef = fieldRef;
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder required() {
            this.required = UiRule.constant(Boolean.TRUE);
            return this;
        }

        public Builder visible(UiRule<Boolean> visible) {
            this.visible = visible == null ? UiRule.constant(Boolean.TRUE) : visible;
            return this;
        }

        public Builder hidden() {
            this.visible = UiRule.constant(Boolean.FALSE);
            return this;
        }

        public Builder readOnly() {
            this.readOnly = UiRule.constant(Boolean.TRUE);
            return this;
        }

        public Builder uiType(String uiType) {
            this.uiType = uiType;
            return this;
        }

        /** Renders this business boolean with declared labels instead of lifecycle labels. */
        public Builder booleanStatus(String trueLabel, String falseLabel) {
            return booleanStatus(trueLabel, falseLabel, BooleanStatusTone.SUCCESS, BooleanStatusTone.NEUTRAL);
        }

        public Builder booleanStatus(String trueLabel, String falseLabel,
                                     BooleanStatusTone trueTone, BooleanStatusTone falseTone) {
            this.uiType = "booleanStatus";
            this.booleanStatus = new BooleanStatusPresentation(trueLabel, falseLabel, trueTone, falseTone);
            return this;
        }

        /** Renders a read-only collection of {@code { id, title, color }} reference summaries. */
        public Builder tagList() {
            this.uiType = "tagList";
            return this;
        }

        public Builder width(String width) {
            this.width = width;
            return this;
        }

        /** Sets the field's span in the standard two-column form and detail grid. */
        public Builder columnSpan(int columnSpan) {
            this.columnSpan = columnSpan;
            return this;
        }

        public Builder align(String align) {
            this.align = align;
            return this;
        }

        public Builder fixed() {
            this.fixed = Boolean.TRUE;
            return this;
        }

        public ViewFieldDefinition build() {
            return new ViewFieldDefinition(fieldRef, label, visible, required, readOnly,
                    uiType, width, columnSpan, align, fixed, booleanStatus);
        }
    }

    private static int requireColumnSpan(int value) {
        if (value < 1 || value > 2) {
            throw new IllegalArgumentException("columnSpan must be between 1 and 2");
        }
        return value;
    }
}
