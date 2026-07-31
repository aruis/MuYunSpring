package net.ximatai.muyun.spring.platform.web;

public record ResolvedUiActionConfirmationDescriptor(String mode,
                                                     String requiredField) {
    public static final String TYPED_TEXT = "typedText";

    public ResolvedUiActionConfirmationDescriptor {
        mode = mode == null || mode.isBlank() ? TYPED_TEXT : mode.trim();
        if (!TYPED_TEXT.equals(mode)) {
            throw new IllegalArgumentException("unsupported UI action confirmation mode: " + mode);
        }
        if (requiredField == null || requiredField.isBlank()) {
            throw new IllegalArgumentException("confirmation requiredField must not be blank");
        }
        requiredField = requiredField.trim();
    }
}
