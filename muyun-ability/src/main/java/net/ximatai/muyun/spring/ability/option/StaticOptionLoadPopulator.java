package net.ximatai.muyun.spring.ability.option;

import java.util.List;

/** Populates static {@code @OptionLoad} projections after a record is read. */
public interface StaticOptionLoadPopulator {
    StaticOptionLoadPopulator NONE = (modelClass, entity) -> {
    };

    void populate(Class<?> modelClass, Object entity);

    default void populateAll(Class<?> modelClass, List<?> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        entities.forEach(entity -> populate(modelClass, entity));
    }
}
