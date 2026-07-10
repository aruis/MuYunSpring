package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.platform.module.ModuleKind;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ModuleUiDescriptorCompiler {
    private static final Set<String> PLATFORM_FIELD_NAMES = Set.of(
            PlatformAbilityFields.TITLE_FIELD,
            PlatformAbilityFields.ENABLED_FIELD,
            PlatformAbilityFields.TREE_PARENT_FIELD,
            PlatformAbilityFields.SORT_FIELD
    );

    private ModuleUiDescriptorCompiler() {
    }

    public static ResolvedModuleUiDescriptor compile(StaticModuleDefinition definition) {
        ModuleUiCompilationResult result = compileModule(definition);
        return result == null ? null : result.uiDescriptor();
    }

    public static ModuleUiCompilationResult compileModule(StaticModuleDefinition definition) {
        if (definition == null || definition.uiDefinition() == null) {
            return null;
        }
        validateFields(definition);
        return new ModuleUiCompilationResult(
                compile(definition.uiDefinition(), ModuleKind.STATIC, definition.title()),
                readModel(definition)
        );
    }

    public static ResolvedModuleUiDescriptor compile(ModuleUiDefinition definition) {
        if (definition == null) {
            return null;
        }
        return compile(definition, null, null);
    }

    public static ResolvedModuleUiDescriptor compile(ModuleUiDefinition definition,
                                                     ModuleKind moduleKind,
                                                     String title) {
        if (definition == null) {
            return null;
        }
        return compileResolved(definition, moduleKind, title);
    }

    private static ResolvedModuleUiDescriptor compileResolved(ModuleUiDefinition definition,
                                                              ModuleKind moduleKind,
                                                              String title) {
        return new ResolvedModuleUiDescriptor(
                ResolvedModuleUiDescriptor.SCHEMA_VERSION,
                definition.moduleAlias(),
                moduleKind,
                title,
                definition.views().stream()
                        .map(ModuleUiDescriptorCompiler::compileView)
                        .toList()
        );
    }

    public static void validate(ModuleUiDefinition definition, List<EntityDefinition> entities) {
        if (definition == null || entities == null || entities.isEmpty()) {
            return;
        }
        validateFields(definition, entities);
    }

    private static ResolvedViewDescriptor compileView(ViewDefinition view) {
        return new ResolvedViewDescriptor(
                view.viewCode(),
                view.viewKind(),
                view.clientType(),
                view.title(),
                view.fields().stream()
                        .map(ModuleUiDescriptorCompiler::compileField)
                        .toList()
        );
    }

    private static ResolvedViewFieldDescriptor compileField(ViewFieldDefinition field) {
        return new ResolvedViewFieldDescriptor(
                field.fieldRef(),
                field.label(),
                field.visible(),
                field.required(),
                field.readOnly(),
                field.uiType(),
                field.width(),
                field.align(),
                field.fixed()
        );
    }

    private static void validateFields(StaticModuleDefinition definition) {
        validateFields(definition.uiDefinition(), definition.entities(), definition.moduleAlias(),
                readProjectionOutputFields(definition));
    }

    private static void validateFields(ModuleUiDefinition definition, List<EntityDefinition> entityDefinitions) {
        validateFields(definition, entityDefinitions, definition.moduleAlias(), Set.of());
    }

    private static void validateFields(ModuleUiDefinition definition,
                                       List<EntityDefinition> entityDefinitions,
                                       String moduleAlias,
                                       Set<String> readProjectionOutputFields) {
        Map<String, EntityDefinition> entities = entitiesByAlias(entityDefinitions);
        if (entities.isEmpty()) {
            return;
        }
        EntityDefinition mainEntity = entityDefinitions.getFirst();
        for (ViewDefinition view : definition.views()) {
            for (ViewFieldDefinition field : view.fields()) {
                validateField(moduleAlias, view, field, entities, mainEntity, readProjectionOutputFields);
            }
        }
    }

    private static Map<String, EntityDefinition> entitiesByAlias(StaticModuleDefinition definition) {
        return entitiesByAlias(definition.entities());
    }

    private static Map<String, EntityDefinition> entitiesByAlias(List<EntityDefinition> entityDefinitions) {
        LinkedHashMap<String, EntityDefinition> entities = new LinkedHashMap<>();
        for (EntityDefinition entity : entityDefinitions) {
            if (entity.alias() != null && !entity.alias().isBlank()) {
                entities.put(entity.alias(), entity);
            }
        }
        return entities;
    }

    private static void validateField(String moduleAlias,
                                      ViewDefinition view,
                                      ViewFieldDefinition field,
                                      Map<String, EntityDefinition> entities,
                                      EntityDefinition mainEntity,
                                      Set<String> readProjectionOutputFields) {
        ViewFieldRef fieldRef = field.fieldRef();
        if (fieldRef.relationCode() == null && readProjectionOutputFields.contains(fieldRef.fieldName())) {
            return;
        }
        EntityDefinition entity = entity(moduleAlias, view, fieldRef, entities, mainEntity);
        if (entity == null) {
            return;
        }
        if (hasField(entity, fieldRef.fieldName()) || PLATFORM_FIELD_NAMES.contains(fieldRef.fieldName())) {
            return;
        }
        throw new IllegalArgumentException("static module UI field is not declared by model facts: "
                + fieldPath(moduleAlias, view, fieldRef));
    }

    private static EntityDefinition entity(ViewFieldRef fieldRef,
                                           Map<String, EntityDefinition> entities,
                                           EntityDefinition mainEntity) {
        if (fieldRef.relationCode() == null) {
            return mainEntity;
        }
        EntityDefinition entity = entities.get(fieldRef.relationCode());
        if (entity == null) {
            throw new IllegalArgumentException("static module UI relation is not declared by model facts: "
                    + fieldRef.relationCode());
        }
        return entity;
    }

    private static EntityDefinition entity(String moduleAlias,
                                           ViewDefinition view,
                                           ViewFieldRef fieldRef,
                                           Map<String, EntityDefinition> entities,
                                           EntityDefinition mainEntity) {
        if (fieldRef.relationCode() == null) {
            return mainEntity;
        }
        if (fieldRef.relationCode().contains(".")) {
            return null;
        }
        EntityDefinition entity = entities.get(fieldRef.relationCode());
        if (entity == null) {
            throw new IllegalArgumentException("static module UI relation is not declared by model facts: "
                    + moduleAlias + "." + view.viewCode() + "." + fieldRef.relationCode());
        }
        return entity;
    }

    private static String fieldPath(String moduleAlias, ViewDefinition view, ViewFieldRef fieldRef) {
        return moduleAlias + "." + view.viewCode() + "."
                + (fieldRef.relationCode() == null ? "" : fieldRef.relationCode() + ".")
                + fieldRef.fieldName();
    }

    private static boolean hasField(EntityDefinition entity, String fieldName) {
        return entity.fields().stream()
                .map(FieldDefinition::fieldName)
                .anyMatch(fieldName::equals);
    }

    private static ResolvedModuleReadModel readModel(StaticModuleDefinition definition) {
        if (definition.entities().isEmpty()) {
            return new ResolvedModuleReadModel(definition.moduleAlias(), null, List.of());
        }
        EntityDefinition mainEntity = definition.entities().getFirst();
        LinkedHashMap<String, ResolvedModuleReadField> fields = new LinkedHashMap<>();
        for (EntityDefinition entity : definition.entities()) {
            String relationCode = relationCode(entity, mainEntity);
            for (FieldDefinition field : entity.fields()) {
                putReadField(fields, new ResolvedModuleReadField(
                        entity.alias(),
                        relationCode,
                        field.fieldName(),
                        false
                ));
            }
        }
        for (StaticModuleReadProjectionDefinition projection : definition.readProjections()) {
            putReadField(fields, new ResolvedModuleReadField(
                    mainEntity.alias(),
                    null,
                    projection.outputField(),
                    true
            ));
        }
        for (ViewDefinition view : definition.uiDefinition().views()) {
            for (ViewFieldDefinition field : view.fields()) {
                ViewFieldRef fieldRef = field.fieldRef();
                if (fieldRef.relationCode() != null) {
                    putReadField(fields, new ResolvedModuleReadField(
                            fieldRef.relationCode(),
                            fieldRef.relationCode(),
                            fieldRef.fieldName(),
                            true
                    ));
                    continue;
                }
                if (!PLATFORM_FIELD_NAMES.contains(fieldRef.fieldName())) {
                    continue;
                }
                EntityDefinition entity = entity(fieldRef, entitiesByAlias(definition), mainEntity);
                putReadField(fields, new ResolvedModuleReadField(
                        entity == null ? mainEntity.alias() : entity.alias(),
                        fieldRef.relationCode(),
                        fieldRef.fieldName(),
                        true
                ));
            }
        }
        return new ResolvedModuleReadModel(
                definition.moduleAlias(),
                mainEntity.alias(),
                List.copyOf(fields.values())
        );
    }

    private static void putReadField(Map<String, ResolvedModuleReadField> fields,
                                     ResolvedModuleReadField field) {
        fields.putIfAbsent((field.entityAlias() == null ? "" : field.entityAlias())
                + ":" + (field.relationCode() == null ? "" : field.relationCode())
                + ":" + field.fieldName(), field);
    }

    private static String relationCode(EntityDefinition entity, EntityDefinition mainEntity) {
        if (entity.alias() == null || entity.alias().equals(mainEntity.alias())) {
            return null;
        }
        return entity.alias();
    }

    private static Set<String> readProjectionOutputFields(StaticModuleDefinition definition) {
        if (definition.readProjections().isEmpty()) {
            return Set.of();
        }
        return definition.readProjections().stream()
                .map(StaticModuleReadProjectionDefinition::outputField)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
