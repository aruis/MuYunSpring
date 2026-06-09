package net.ximatai.muyun.spring.dynamic.runtime;

public interface DynamicRecordMutationCoordinator {
    DynamicRecordMutationCoordinator NONE = new DynamicRecordMutationCoordinator() {
    };

    default void beforeCreate(String moduleAlias, String entityAlias, DynamicRecord record) {
    }

    default void afterCreate(String moduleAlias, String entityAlias, DynamicRecord record, String id) {
    }

    default void beforeRelationChildCreate(String moduleAlias,
                                           String parentEntityAlias,
                                           String relationCode,
                                           String childEntityAlias,
                                           DynamicRecord parent,
                                           DynamicRecord child) {
    }

    default void afterRelationChildCreate(String moduleAlias,
                                          String parentEntityAlias,
                                          String relationCode,
                                          String childEntityAlias,
                                          DynamicRecord parent,
                                          DynamicRecord child,
                                          String id) {
    }

    default void beforeUpdate(String moduleAlias, String entityAlias, DynamicRecord before, DynamicRecord incoming) {
    }

    default void afterUpdate(String moduleAlias, String entityAlias, DynamicRecord before, DynamicRecord updated) {
    }

    default void beforeRelationChildUpdate(String moduleAlias,
                                           String parentEntityAlias,
                                           String relationCode,
                                           String childEntityAlias,
                                           DynamicRecord parentBefore,
                                           DynamicRecord parentIncoming,
                                           DynamicRecord childBefore,
                                           DynamicRecord childIncoming) {
    }

    default void afterRelationChildUpdate(String moduleAlias,
                                          String parentEntityAlias,
                                          String relationCode,
                                          String childEntityAlias,
                                          DynamicRecord parentBefore,
                                          DynamicRecord parentUpdated,
                                          DynamicRecord childBefore,
                                          DynamicRecord childUpdated) {
    }

    default void beforeRelationChildDelete(String moduleAlias,
                                           String parentEntityAlias,
                                           String relationCode,
                                           String childEntityAlias,
                                           DynamicRecord parentBefore,
                                           DynamicRecord childBefore) {
    }

    default void afterRelationChildDelete(String moduleAlias,
                                          String parentEntityAlias,
                                          String relationCode,
                                          String childEntityAlias,
                                          DynamicRecord parentBefore,
                                          DynamicRecord childBefore) {
    }

    default void beforeDelete(String moduleAlias, String entityAlias, DynamicRecord before) {
    }

    default void afterDelete(String moduleAlias, String entityAlias, DynamicRecord before) {
    }
}
