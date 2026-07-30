package net.ximatai.muyun.spring.ability.reference;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.List;

/** Compiles inverse read associations without granting aggregate ownership semantics. */
public final class StaticReferencedByResolver {
    private StaticReferencedByResolver() {
    }

    public static List<ReferencedByPlan> plans(Class<?> targetModel) {
        if (targetModel == null) return List.of();
        String targetAlias = entityAlias(targetModel);
        return fields(targetModel).stream()
                .filter(field -> field.getAnnotation(ReferencedBy.class) != null)
                .map(field -> plan(targetModel, targetAlias, field))
                .toList();
    }

    public static void writeLoadedValue(EntityContract target, String fieldName, List<? extends EntityContract> values) {
        if (target == null) {
            return;
        }
        Field field = field(target.getClass(), fieldName);
        try {
            if (!field.canAccess(target)) {
                field.setAccessible(true);
            }
            field.set(target, values == null ? List.of() : List.copyOf(values));
        } catch (IllegalAccessException | IllegalArgumentException exception) {
            throw new PlatformException("Cannot populate @ReferencedBy field: "
                    + target.getClass().getName() + "." + fieldName, exception);
        }
    }

    private static ReferencedByPlan plan(Class<?> targetModel, String targetAlias, Field field) {
        if (!java.lang.reflect.Modifier.isTransient(field.getModifiers())
                || !List.class.isAssignableFrom(field.getType()) || !(field.getGenericType() instanceof ParameterizedType type)
                || !(type.getActualTypeArguments()[0] instanceof Class<?> source)
                || !EntityContract.class.isAssignableFrom(source)) {
            throw new PlatformException("@ReferencedBy field must declare transient List<EntityContract>: "
                    + targetModel.getName() + "." + field.getName());
        }
        ReferencedBy annotation = field.getAnnotation(ReferencedBy.class);
        List<ReferencePlan> matches = StaticReferenceResolver.plans(source).stream()
                .filter(reference -> targetAlias.equals(reference.target().entityAlias()))
                .filter(reference -> annotation.sourceField().isBlank()
                        || annotation.sourceField().equals(reference.sourceField()))
                .toList();
        if (matches.size() != 1) {
            throw new PlatformException("@ReferencedBy requires exactly one @ReferenceTo from " + source.getName()
                    + " to " + targetModel.getName() + "; specify sourceField when ambiguous");
        }
        return new ReferencedByPlan(field.getName(), source, matches.getFirst().sourceField());
    }

    private static String entityAlias(Class<?> type) {
        String name = type.getSimpleName();
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private static List<Field> fields(Class<?> modelClass) {
        List<Field> fields = new java.util.ArrayList<>();
        Class<?> current = modelClass;
        while (current != null && !Object.class.equals(current)) {
            fields.addAll(List.of(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }

    private static Field field(Class<?> modelClass, String fieldName) {
        Class<?> current = modelClass;
        while (current != null && !Object.class.equals(current)) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new PlatformException("@ReferencedBy output field is unavailable: "
                + modelClass.getName() + "." + fieldName);
    }

    public record ReferencedByPlan(String fieldName, Class<?> sourceModel, String sourceField) {
    }
}
