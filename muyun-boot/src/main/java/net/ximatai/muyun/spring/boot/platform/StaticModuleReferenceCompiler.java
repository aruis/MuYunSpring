package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.ability.reference.ModuleReference;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class StaticModuleReferenceCompiler {
    private StaticModuleReferenceCompiler() {
    }

    public static List<StaticModuleReferenceDefinition> compile(Class<?> modelClass) {
        if (modelClass == null || modelClass == Object.class) {
            return List.of();
        }
        List<StaticModuleReferenceDefinition> references = new ArrayList<>();
        for (Field field : declaredFields(modelClass)) {
            ModuleReference reference = field.getAnnotation(ModuleReference.class);
            if (reference == null) {
                continue;
            }
            references.add(new StaticModuleReferenceDefinition(
                    referenceCode(field.getName(), reference),
                    field.getName(),
                    targetModuleAlias(reference),
                    reference.targetField()
            ));
        }
        return List.copyOf(references);
    }

    private static List<Field> declaredFields(Class<?> modelClass) {
        List<Field> fields = new ArrayList<>();
        addDeclaredFields(modelClass, fields);
        return fields;
    }

    private static void addDeclaredFields(Class<?> modelClass, List<Field> fields) {
        if (modelClass == null || modelClass == Object.class) {
            return;
        }
        addDeclaredFields(modelClass.getSuperclass(), fields);
        Collections.addAll(fields, modelClass.getDeclaredFields());
    }

    private static String referenceCode(String fieldName, ModuleReference reference) {
        if (reference.code() != null && !reference.code().isBlank()) {
            return PlatformNameRules.requireIdentifier(reference.code(), "referenceCode");
        }
        if (fieldName.endsWith("Id") && fieldName.length() > 2) {
            String base = fieldName.substring(0, fieldName.length() - 2);
            return Character.toLowerCase(base.charAt(0)) + base.substring(1);
        }
        return PlatformNameRules.requireIdentifier(fieldName, "referenceCode");
    }

    private static String targetModuleAlias(ModuleReference reference) {
        boolean hasTargetClass = reference.target() != null && reference.target() != Void.class;
        boolean hasTargetAlias = reference.targetModuleAlias() != null && !reference.targetModuleAlias().isBlank();
        if (hasTargetClass == hasTargetAlias) {
            throw new IllegalArgumentException("ModuleReference requires exactly one of target or targetModuleAlias");
        }
        if (hasTargetAlias) {
            return reference.targetModuleAlias();
        }
        try {
            Field moduleAliasField = reference.target().getField("MODULE_ALIAS");
            if (!Modifier.isStatic(moduleAliasField.getModifiers()) || moduleAliasField.getType() != String.class) {
                throw new IllegalArgumentException("ModuleReference target MODULE_ALIAS must be public static String: "
                        + reference.target().getName());
            }
            return (String) moduleAliasField.get(null);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalArgumentException("ModuleReference target requires public MODULE_ALIAS: "
                    + reference.target().getName(), ex);
        }
    }
}
