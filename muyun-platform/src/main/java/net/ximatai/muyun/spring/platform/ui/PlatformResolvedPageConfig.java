package net.ximatai.muyun.spring.platform.ui;

import java.util.List;

public record PlatformResolvedPageConfig(
        List<PlatformResolvedUiField> uiFields,
        List<PlatformResolvedQueryItem> queryItems,
        List<PlatformResolvedFieldUiType> fieldUiTypes,
        List<PlatformAssociationBlock> associationBlocks
) {
    public PlatformResolvedPageConfig(List<PlatformResolvedUiField> uiFields,
                                      List<PlatformResolvedQueryItem> queryItems) {
        this(uiFields, queryItems, List.of(), List.of());
    }

    public PlatformResolvedPageConfig(List<PlatformResolvedUiField> uiFields,
                                      List<PlatformResolvedQueryItem> queryItems,
                                      List<PlatformResolvedFieldUiType> fieldUiTypes) {
        this(uiFields, queryItems, fieldUiTypes, List.of());
    }

    public PlatformResolvedPageConfig {
        uiFields = uiFields == null ? List.of() : List.copyOf(uiFields);
        queryItems = queryItems == null ? List.of() : List.copyOf(queryItems);
        fieldUiTypes = fieldUiTypes == null ? List.of() : List.copyOf(fieldUiTypes);
        associationBlocks = associationBlocks == null ? List.of() : List.copyOf(associationBlocks);
    }

    public static PlatformResolvedPageConfig empty() {
        return new PlatformResolvedPageConfig(List.of(), List.of(), List.of(), List.of());
    }
}
