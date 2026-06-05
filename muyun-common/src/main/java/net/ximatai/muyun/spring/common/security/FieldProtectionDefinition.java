package net.ximatai.muyun.spring.common.security;

public record FieldProtectionDefinition(
        FieldEncryptionMode encryptionMode,
        FieldSignatureMode signatureMode,
        FieldMaskingPolicy maskingPolicy
) {
    public static final FieldProtectionDefinition NONE = new FieldProtectionDefinition(
            FieldEncryptionMode.NONE,
            FieldSignatureMode.NONE,
            FieldMaskingPolicy.NONE
    );

    public FieldProtectionDefinition {
        encryptionMode = encryptionMode == null ? FieldEncryptionMode.NONE : encryptionMode;
        signatureMode = signatureMode == null ? FieldSignatureMode.NONE : signatureMode;
        maskingPolicy = maskingPolicy == null ? FieldMaskingPolicy.NONE : maskingPolicy;
    }

    public boolean hasStorageProtection() {
        return encryptionMode.enabled() || signatureMode.enabled();
    }

    public boolean hasOutputProtection() {
        return maskingPolicy.enabled();
    }

    public boolean enabled() {
        return hasStorageProtection() || hasOutputProtection();
    }
}
