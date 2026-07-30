package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.common.model.title.RecordLabelResolver;
import net.ximatai.muyun.spring.common.option.OptionFieldDefinition;
import net.ximatai.muyun.spring.common.option.OptionFieldResolver;
import net.ximatai.muyun.spring.common.option.OptionSelectionMode;
import net.ximatai.muyun.spring.ability.reference.ReferencePlan;
import net.ximatai.muyun.spring.ability.reference.ReferenceProjection;
import net.ximatai.muyun.spring.ability.reference.StaticReferenceResolver;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.platform.module.ModuleKind;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
        if (definition == null) {
            return null;
        }
        ModuleUiDefinition uiDefinition = definition.uiDefinition() == null
                ? new ModuleUiDefinition(definition.moduleAlias(), List.of(), List.of())
                : definition.uiDefinition();
        validateFields(uiDefinition, definition.entities(), definition.moduleAlias(), readOutputFields(definition));
        return new ModuleUiCompilationResult(
                compile(uiDefinition, ModuleKind.STATIC, definition.title(),
                        staticOptionFields(definition.modelClass()), staticRecordLabelField(definition)),
                readModel(definition, uiDefinition)
        );
    }

    public static ResolvedModuleUiDescriptor compile(ModuleUiDefinition definition) {
        if (definition == null) {
            return null;
        }
        return compile(definition, null, null, Map.of(), null);
    }

    public static ResolvedModuleUiDescriptor compile(ModuleUiDefinition definition,
                                                     ModuleKind moduleKind,
                                                     String title) {
        return compile(definition, moduleKind, title, Map.of(), null);
    }

    public static ResolvedModuleUiDescriptor compile(ModuleUiDefinition definition,
                                                     ModuleKind moduleKind,
                                                     String title,
                                                     Map<String, ResolvedOptionFieldDescriptor> optionFields) {
        if (definition == null) {
            return null;
        }
        return compile(definition, moduleKind, title, optionFields, null);
    }

    public static ResolvedModuleUiDescriptor compile(ModuleUiDefinition definition,
                                                     ModuleKind moduleKind,
                                                     String title,
                                                     Map<String, ResolvedOptionFieldDescriptor> optionFields,
                                                     String defaultRecordLabelField) {
        if (definition == null) return null;
        return compileResolved(definition, moduleKind, title, optionFields == null ? Map.of() : optionFields,
                defaultRecordLabelField);
    }

    private static ResolvedModuleUiDescriptor compileResolved(ModuleUiDefinition definition,
                                                              ModuleKind moduleKind,
                                                              String title,
                                                              Map<String, ResolvedOptionFieldDescriptor> optionFields,
                                                              String defaultRecordLabelField) {
        return new ResolvedModuleUiDescriptor(
                ResolvedModuleUiDescriptor.SCHEMA_VERSION,
                definition.moduleAlias(),
                moduleKind,
                title,
                definition.views().stream()
                        .map(view -> compileView(view, optionFields))
                        .toList(),
                definition.actions().stream()
                        .map(ModuleUiDescriptorCompiler::compileAction)
                        .toList(),
                defaultRecordLabelField
        );
    }

    private static ResolvedUiActionDescriptor compileAction(UiActionDefinition action) {
        UiActionConfirmationDefinition confirmation = action.confirmation();
        return new ResolvedUiActionDescriptor(
                action.actionCode(),
                confirmation == null ? null : new ResolvedUiActionConfirmationDescriptor(
                        ResolvedUiActionConfirmationDescriptor.TYPED_TEXT,
                        confirmation.requiredField())
        );
    }

    public static void validate(ModuleUiDefinition definition, List<EntityDefinition> entities) {
        if (definition == null || entities == null || entities.isEmpty()) {
            return;
        }
        validateFields(definition, entities);
    }

    private static ResolvedViewDescriptor compileView(ViewDefinition view,
                                                      Map<String, ResolvedOptionFieldDescriptor> optionFields) {
        return new ResolvedViewDescriptor(
                view.viewCode(),
                view.viewKind(),
                view.clientType(),
                view.title(),
                view.fields().stream()
                        .map(field -> compileField(field, optionFields))
                        .toList()
        );
    }

    private static ResolvedViewFieldDescriptor compileField(ViewFieldDefinition field,
                                                            Map<String, ResolvedOptionFieldDescriptor> optionFields) {
        return new ResolvedViewFieldDescriptor(
                field.fieldRef(),
                field.label(),
                field.visible(),
                field.required(),
                field.readOnly(),
                field.uiType(),
                field.width(),
                field.align(),
                field.fixed(),
                field.fieldRef().relationCode() == null ? optionFields.get(field.fieldRef().fieldName()) : null
        );
    }

    private static Map<String, ResolvedOptionFieldDescriptor> staticOptionFields(Class<?> modelClass) {
        if (modelClass == null) {
            return Map.of();
        }
        return OptionFieldResolver.resolve(modelClass).stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        OptionFieldDefinition::fieldName,
                        definition -> new ResolvedOptionFieldDescriptor(definition.binding(), definition.selectionMode(),
                                definition.hasTitleOutput() ? definition.titleOutputField() : null),
                        (left, right) -> left
                ));
    }

    private static String staticRecordLabelField(StaticModuleDefinition definition) {
        return RecordLabelResolver.resolveFieldName(definition.modelClass()).orElse(null);
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

    private static ResolvedModuleReadModel readModel(StaticModuleDefinition definition,
                                                     ModuleUiDefinition uiDefinition) {
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
        for (String outputField : referenceOutputFields(definition)) {
            putReadField(fields, new ResolvedModuleReadField(mainEntity.alias(), null, outputField, true));
        }
        for (ViewDefinition view : uiDefinition.views()) {
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

    private static Set<String> readOutputFields(StaticModuleDefinition definition) {
        LinkedHashMap<String, Boolean> fields = new LinkedHashMap<>();
        definition.readProjections().stream()
                .map(StaticModuleReadProjectionDefinition::outputField)
                .forEach(field -> fields.put(field, Boolean.TRUE));
        referenceOutputFields(definition).forEach(field -> fields.put(field, Boolean.TRUE));
        return java.util.Collections.unmodifiableSet(new LinkedHashSet<>(fields.keySet()));
    }

    private static Set<String> referenceOutputFields(StaticModuleDefinition definition) {
        if (definition.modelClass() == null) {
            return Set.of();
        }
        LinkedHashMap<String, Boolean> fields = new LinkedHashMap<>();
        for (ReferencePlan plan : StaticReferenceResolver.plans(definition.modelClass())) {
            for (ReferenceProjection projection : plan.projections()) {
                fields.put(projection.outputField(), Boolean.TRUE);
            }
        }
        return java.util.Collections.unmodifiableSet(new LinkedHashSet<>(fields.keySet()));
    }
}
