package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.database.core.orm.MigrationResult;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record DynamicModulePublishResult(
        ModuleDefinition module,
        Map<String, MigrationResult> migrations,
        boolean dryRun
) {
    public DynamicModulePublishResult(ModuleDefinition module, Map<String, MigrationResult> migrations) {
        this(module, migrations, migrations != null && !migrations.isEmpty()
                && migrations.values().stream().allMatch(MigrationResult::isDryRun));
    }

    public DynamicModulePublishResult {
        migrations = migrations == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(migrations));
    }

    public boolean changed() {
        return migrations.values().stream().anyMatch(MigrationResult::isChanged);
    }

    public boolean hasNonAdditiveChanges() {
        return migrations.values().stream().anyMatch(MigrationResult::hasNonAdditiveChanges);
    }

    public Map<String, List<String>> statementsByEntity() {
        return migrations.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().getStatements(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }
}
