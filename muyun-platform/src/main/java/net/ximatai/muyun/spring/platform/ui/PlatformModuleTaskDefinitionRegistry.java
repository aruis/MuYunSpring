package net.ximatai.muyun.spring.platform.ui;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class PlatformModuleTaskDefinitionRegistry {
    private final CopyOnWriteArrayList<PlatformModuleTaskDefinition> definitions = new CopyOnWriteArrayList<>();

    public List<PlatformModuleTaskDefinition> listEnabled(String moduleAlias) {
        return definitions.stream()
                .filter(PlatformModuleTaskDefinition::enabled)
                .filter(definition -> definition.moduleAlias().equals(moduleAlias))
                .sorted(Comparator.comparing(PlatformModuleTaskDefinition::sortOrder)
                        .thenComparing(PlatformModuleTaskDefinition::taskCode))
                .toList();
    }

    public void register(PlatformModuleTaskDefinition definition) {
        if (definition == null) {
            return;
        }
        definitions.removeIf(existing -> existing.moduleAlias().equals(definition.moduleAlias())
                && existing.taskCode().equals(definition.taskCode()));
        definitions.add(definition);
    }

    public void replace(String moduleAlias, List<PlatformModuleTaskDefinition> moduleDefinitions) {
        if (moduleDefinitions != null) {
            moduleDefinitions.stream()
                    .filter(definition -> definition != null && !Objects.equals(definition.moduleAlias(), moduleAlias))
                    .findFirst()
                    .ifPresent(definition -> {
                        throw new IllegalArgumentException("Module task definition does not belong to module: "
                                + moduleAlias);
                    });
        }
        definitions.removeIf(existing -> existing.moduleAlias().equals(moduleAlias));
        if (moduleDefinitions != null) {
            moduleDefinitions.forEach(this::register);
        }
    }

    public void clear() {
        definitions.clear();
    }
}
