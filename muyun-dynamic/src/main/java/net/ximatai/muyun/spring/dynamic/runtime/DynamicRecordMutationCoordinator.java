package net.ximatai.muyun.spring.dynamic.runtime;

public interface DynamicRecordMutationCoordinator {
    DynamicRecordMutationCoordinator NONE = new DynamicRecordMutationCoordinator() {
    };

    default void beforeCreate(String moduleAlias, String entityAlias, DynamicRecord record) {
    }

    default void afterCreate(String moduleAlias, String entityAlias, DynamicRecord record, String id) {
    }

    default void beforeUpdate(String moduleAlias, String entityAlias, DynamicRecord before, DynamicRecord incoming) {
    }

    default void afterUpdate(String moduleAlias, String entityAlias, DynamicRecord before, DynamicRecord updated) {
    }

    default void beforeDelete(String moduleAlias, String entityAlias, DynamicRecord before) {
    }

    default void afterDelete(String moduleAlias, String entityAlias, DynamicRecord before) {
    }
}
