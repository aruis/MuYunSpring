package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;

import java.util.List;

@FunctionalInterface
public interface DynamicOptionLoadPopulator {
    DynamicOptionLoadPopulator NONE = (entity, records) -> { };

    void populate(EntityDefinition entity, List<DynamicRecord> records);
}
