package net.ximatai.muyun.spring.platform.ui;

import java.util.List;

public record PlatformResolvedPageConfig(
        List<PlatformResolvedUiField> uiFields,
        List<PlatformResolvedQueryItem> queryItems,
        List<PlatformResolvedFieldUiType> fieldUiTypes,
        List<PlatformAssociationBlock> associationBlocks,
        List<PlatformActionBlock> actionBlocks,
        List<PlatformTaskBlock> taskBlocks
) {
    public PlatformResolvedPageConfig(List<PlatformResolvedUiField> uiFields,
                                      List<PlatformResolvedQueryItem> queryItems) {
        this(uiFields, queryItems, List.of(), List.of(), List.of(), List.of());
    }

    public PlatformResolvedPageConfig(List<PlatformResolvedUiField> uiFields,
                                      List<PlatformResolvedQueryItem> queryItems,
                                      List<PlatformResolvedFieldUiType> fieldUiTypes) {
        this(uiFields, queryItems, fieldUiTypes, List.of(), List.of(), List.of());
    }

    public PlatformResolvedPageConfig {
        uiFields = uiFields == null ? List.of() : List.copyOf(uiFields);
        queryItems = queryItems == null ? List.of() : List.copyOf(queryItems);
        fieldUiTypes = fieldUiTypes == null ? List.of() : List.copyOf(fieldUiTypes);
        associationBlocks = associationBlocks == null ? List.of() : List.copyOf(associationBlocks);
        actionBlocks = actionBlocks == null ? List.of() : List.copyOf(actionBlocks);
        taskBlocks = taskBlocks == null ? List.of() : List.copyOf(taskBlocks);
    }

    public static PlatformResolvedPageConfig empty() {
        return new PlatformResolvedPageConfig(List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }
}
