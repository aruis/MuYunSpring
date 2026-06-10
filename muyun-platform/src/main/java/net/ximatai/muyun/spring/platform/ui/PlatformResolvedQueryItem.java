package net.ximatai.muyun.spring.platform.ui;

import net.ximatai.muyun.spring.dynamic.metadata.DynamicQueryOperator;

public record PlatformResolvedQueryItem(
        String queryTemplateId,
        String queryItemId,
        String parentId,
        PlatformQueryGroupOperator groupOperator,
        String moduleMetadataFieldId,
        String relationAlias,
        String metadataAlias,
        String fieldName,
        String fieldTitle,
        String fieldTypeAlias,
        DynamicQueryOperator operator,
        String defaultValue,
        Boolean allowExternalValue,
        String externalValueKey,
        String timeZone
) {
}
