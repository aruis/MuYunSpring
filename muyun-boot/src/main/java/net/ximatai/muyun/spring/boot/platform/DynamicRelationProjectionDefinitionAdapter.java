package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.ability.reference.ReferenceCardinality;
import net.ximatai.muyun.spring.ability.reference.ReferenceProjection;
import net.ximatai.muyun.spring.ability.reference.ReferenceTarget;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityReferenceDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.platform.module.ModuleEntryType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DynamicRelationProjectionDefinitionAdapter {
    private DynamicRelationProjectionDefinitionAdapter() {
    }

    public static List<StaticModuleDefinition> adapt(List<ModuleDefinition> definitions) {
        if (definitions == null || definitions.isEmpty()) {
            return List.of();
        }
        Map<String, ModuleDefinition> byAlias = new LinkedHashMap<>();
        for (ModuleDefinition definition : definitions) {
            if (definition == null) {
                continue;
            }
            byAlias.put(definition.moduleAlias(), definition);
        }
        return byAlias.values().stream()
                .map(definition -> adapt(definition, byAlias))
                .toList();
    }

    public static StaticModuleDefinition adapt(ModuleDefinition definition) {
        return adapt(definition, Map.of(definition.moduleAlias(), definition));
    }

    private static StaticModuleDefinition adapt(ModuleDefinition definition,
                                                Map<String, ModuleDefinition> definitionsByAlias) {
        if (definition == null) {
            throw new IllegalArgumentException("dynamic module definition must not be null");
        }
        EntityDefinition mainEntity = mainEntity(definition);
        return new StaticModuleDefinition(
                applicationAlias(definition.moduleAlias()),
                definition.moduleAlias(),
                definition.name(),
                null,
                ModuleEntryType.MODULE,
                null,
                null,
                Set.of(EntityCapability.CRUD),
                List.of(),
                List.of(mainEntity),
                null,
                references(definition, mainEntity, definitionsByAlias),
                readProjections(definition, mainEntity, definitionsByAlias)
        );
    }

    private static List<StaticModuleReferenceDefinition> references(ModuleDefinition definition,
                                                                    EntityDefinition mainEntity,
                                                                    Map<String, ModuleDefinition> definitionsByAlias) {
        return definition.references().stream()
                .filter(reference -> mainEntity.alias().equals(reference.sourceEntityAlias()))
                .filter(reference -> reference.cardinality() == ReferenceCardinality.ONE)
                .filter(reference -> targetMainEntity(reference, definitionsByAlias))
                .map(reference -> new StaticModuleReferenceDefinition(
                        referenceCode(reference.sourceField()),
                        reference.sourceField(),
                        reference.target().moduleAlias(),
                        "id"
                ))
                .toList();
    }

    private static List<StaticModuleReadProjectionDefinition> readProjections(ModuleDefinition definition,
                                                                              EntityDefinition mainEntity,
                                                                              Map<String, ModuleDefinition> definitionsByAlias) {
        return definition.references().stream()
                .filter(reference -> mainEntity.alias().equals(reference.sourceEntityAlias()))
                .filter(reference -> reference.cardinality() == ReferenceCardinality.ONE)
                .filter(reference -> targetMainEntity(reference, definitionsByAlias))
                .flatMap(reference -> reference.projections().stream()
                        .map(projection -> readProjection(reference, projection)))
                .toList();
    }

    private static StaticModuleReadProjectionDefinition readProjection(EntityReferenceDefinition reference,
                                                                       ReferenceProjection projection) {
        return new StaticModuleReadProjectionDefinition(
                referenceCode(reference.sourceField()) + "." + projection.targetField(),
                projection.outputField()
        );
    }

    private static boolean targetMainEntity(EntityReferenceDefinition reference,
                                            Map<String, ModuleDefinition> definitionsByAlias) {
        ReferenceTarget target = reference.target();
        ModuleDefinition targetDefinition = definitionsByAlias.get(target.moduleAlias());
        if (targetDefinition == null) {
            return false;
        }
        return target.entityAlias().equals(mainEntity(targetDefinition).alias());
    }

    private static EntityDefinition mainEntity(ModuleDefinition definition) {
        if (definition.entities().isEmpty()) {
            throw new IllegalArgumentException("dynamic module definition must declare at least one entity: "
                    + definition.moduleAlias());
        }
        if (definition.mainEntityAlias() == null || definition.mainEntityAlias().isBlank()) {
            return definition.entities().getFirst();
        }
        return definition.entities().stream()
                .filter(entity -> definition.mainEntityAlias().equals(entity.alias()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("dynamic module main entity is not declared: "
                        + definition.moduleAlias() + "." + definition.mainEntityAlias()));
    }

    private static String applicationAlias(String moduleAlias) {
        String normalized = PlatformNameRules.requireModuleAlias(moduleAlias);
        int index = normalized.indexOf('.');
        if (index < 0) {
            throw new IllegalArgumentException("dynamic module alias must include application alias: " + moduleAlias);
        }
        return normalized.substring(0, index);
    }

    private static String referenceCode(String sourceField) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < sourceField.length(); index++) {
            char ch = sourceField.charAt(index);
            if (Character.isUpperCase(ch)) {
                if (!result.isEmpty()) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }
}
