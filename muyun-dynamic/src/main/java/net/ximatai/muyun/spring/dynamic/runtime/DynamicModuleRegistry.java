package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinitionException;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinitionValidator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class DynamicModuleRegistry {
    private final ModuleDefinitionValidator validator;
    private final Map<String, ModuleDefinition> modules = new LinkedHashMap<>();

    public DynamicModuleRegistry() {
        this(new ModuleDefinitionValidator());
    }

    public DynamicModuleRegistry(ModuleDefinitionValidator validator) {
        this.validator = Objects.requireNonNull(validator, "validator must not be null");
    }

    public void register(ModuleDefinition module) {
        validator.validate(module);
        if (modules.containsKey(module.moduleAlias())) {
            throw new ModuleDefinitionException("duplicate module alias: " + module.moduleAlias());
        }
        modules.put(module.moduleAlias(), module);
    }

    public void publish(ModuleDefinition module) {
        validator.validate(module);
        modules.put(module.moduleAlias(), module);
    }

    public Optional<ModuleDefinition> findModule(String moduleAlias) {
        return Optional.ofNullable(modules.get(moduleAlias));
    }

    public boolean containsModule(String moduleAlias) {
        return modules.containsKey(moduleAlias);
    }

    public ModuleDefinition requireModule(String moduleAlias) {
        return findModule(moduleAlias)
                .orElseThrow(() -> new ModuleDefinitionException("unknown module alias: " + moduleAlias));
    }

    public EntityDefinition requireEntity(String moduleAlias, String entityCode) {
        return requireModule(moduleAlias).entities().stream()
                .filter(entity -> entity.code().equals(entityCode))
                .findFirst()
                .orElseThrow(() -> new ModuleDefinitionException("unknown entity: " + moduleAlias + "." + entityCode));
    }

    public List<ModuleDefinition> modules() {
        return List.copyOf(modules.values());
    }
}
