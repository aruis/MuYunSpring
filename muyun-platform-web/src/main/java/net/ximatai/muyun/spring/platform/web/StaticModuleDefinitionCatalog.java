package net.ximatai.muyun.spring.platform.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class StaticModuleDefinitionCatalog {
    private final List<StaticModuleDefinition> definitions;
    private final List<StaticModuleDefinitionScanner> scanners;
    private volatile List<StaticModuleDefinition> cachedDefinitions;
    private volatile Map<String, StaticModuleDefinition> cachedDefinitionMap;

    public StaticModuleDefinitionCatalog(List<StaticModuleDefinition> definitions) {
        this(definitions, List.of());
    }

    public StaticModuleDefinitionCatalog(List<StaticModuleDefinition> definitions,
                                         List<StaticModuleDefinitionScanner> scanners) {
        this.definitions = definitions == null ? List.of() : List.copyOf(definitions);
        this.scanners = scanners == null ? List.of() : List.copyOf(scanners);
    }

    public List<StaticModuleDefinition> definitions() {
        List<StaticModuleDefinition> current = cachedDefinitions;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (cachedDefinitions == null) {
                cachedDefinitions = loadDefinitions();
            }
            return cachedDefinitions;
        }
    }

    public Optional<StaticModuleDefinition> find(String moduleAlias) {
        if (moduleAlias == null || moduleAlias.isBlank()) {
            return Optional.empty();
        }
        Map<String, StaticModuleDefinition> current = cachedDefinitionMap;
        if (current == null) {
            synchronized (this) {
                if (cachedDefinitionMap == null) {
                    Map<String, StaticModuleDefinition> byAlias = new HashMap<>();
                    for (StaticModuleDefinition definition : definitions()) {
                        byAlias.put(definition.moduleAlias(), definition);
                    }
                    cachedDefinitionMap = Map.copyOf(byAlias);
                }
                current = cachedDefinitionMap;
            }
        }
        return Optional.ofNullable(current.get(moduleAlias.trim()));
    }

    private List<StaticModuleDefinition> loadDefinitions() {
        if (scanners.isEmpty()) {
            validateDefinitions(definitions);
            return definitions;
        }
        ArrayList<StaticModuleDefinition> all = new ArrayList<>(definitions);
        for (StaticModuleDefinitionScanner scanner : scanners) {
            all.addAll(scanner.scan());
        }
        validateDefinitions(all);
        return List.copyOf(all);
    }

    private void validateDefinitions(List<StaticModuleDefinition> definitions) {
        Set<String> modules = new HashSet<>();
        for (StaticModuleDefinition definition : definitions) {
            if (!modules.add(definition.moduleAlias())) {
                throw new IllegalStateException("duplicate static module definition: " + definition.moduleAlias());
            }
            Set<String> actions = new HashSet<>();
            for (StaticModuleActionDefinition action : definition.actions()) {
                if (!actions.add(action.actionCode())) {
                    throw new IllegalStateException("duplicate static module action definition: "
                            + definition.moduleAlias() + "." + action.actionCode());
                }
            }
        }
    }
}
