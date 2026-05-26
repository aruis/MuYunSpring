package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.database.core.orm.MigrationResult;
import net.ximatai.muyun.spring.module.metadata.ModuleDefinition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record DynamicModulePublishResult(
        ModuleDefinition module,
        Map<String, MigrationResult> migrations
) {
    public DynamicModulePublishResult {
        migrations = migrations == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(migrations));
    }

    public boolean changed() {
        return migrations.values().stream().anyMatch(MigrationResult::isChanged);
    }
}
