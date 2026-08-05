package net.ximatai.muyun.spring.platform.ui;

public record PlatformResolvedUiField(
        String uiConfigId,
        String moduleMetadataFieldId,
        String relationAlias,
        String metadataAlias,
        String fieldName,
        String columnName,
        String fieldTitle,
        String fieldSpecAlias,
        String fieldForm,
        String fieldUiControlAlias,
        Boolean visible,
        Boolean readOnly,
        Boolean requiredOverride,
        String placeholder,
        String defaultValue,
        Integer width,
        String align,
        PlatformUiFixedPosition fixedPosition
) {
}
