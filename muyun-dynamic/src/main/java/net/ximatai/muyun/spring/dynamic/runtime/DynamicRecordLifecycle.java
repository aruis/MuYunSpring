package net.ximatai.muyun.spring.dynamic.runtime;

public interface DynamicRecordLifecycle {
    DynamicRecordLifecycle NONE = new DynamicRecordLifecycle() {
    };

    default void beforeInsert(DynamicRecord record) {
    }

    default void beforeUpdate(DynamicRecord record) {
    }

    default void beforeDelete(String id) {
    }

    default void afterSelect(DynamicRecord record) {
    }
}
