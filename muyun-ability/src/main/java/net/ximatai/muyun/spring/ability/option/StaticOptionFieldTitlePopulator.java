package net.ximatai.muyun.spring.ability.option;

import java.util.List;

public interface StaticOptionFieldTitlePopulator {
    StaticOptionFieldTitlePopulator NONE = (modelClass, entity) -> {
    };

    void populate(Class<?> modelClass, Object entity);

    default void populateAll(Class<?> modelClass, List<?> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        entities.forEach(entity -> populate(modelClass, entity));
    }
}
