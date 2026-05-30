package net.ximatai.muyun.spring.dynamic.descriptor;

import net.ximatai.muyun.spring.dynamic.metadata.EntityActionKind;

public enum DynamicActionKind {
    RECORD,
    COLLECTION,
    QUERY,
    TREE,
    SORT,
    REFERENCE,
    STATE,
    CUSTOM;

    static DynamicActionKind from(EntityActionKind kind) {
        return DynamicActionKind.valueOf(kind.name());
    }
}
