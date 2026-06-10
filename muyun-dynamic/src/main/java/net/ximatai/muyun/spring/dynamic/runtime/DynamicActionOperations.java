package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.spring.common.platform.PlatformAction;

public interface DynamicActionOperations {
    DynamicRecord newRecord();

    default DynamicRecord newRecord(String moduleAlias, String entityAlias) {
        throw new UnsupportedOperationException("cross-module newRecord is not supported by this action operations");
    }

    DynamicRecord select(String id);

    default DynamicRecord select(String moduleAlias, String entityAlias, String id) {
        throw new UnsupportedOperationException("cross-module select is not supported by this action operations");
    }

    default void requireAction(String moduleAlias, PlatformAction action) {
        throw new UnsupportedOperationException("cross-module action authorization is not supported by this action operations");
    }

    int update(DynamicRecord record);

    int delete(String id);
}
