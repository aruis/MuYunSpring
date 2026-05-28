package net.ximatai.muyun.spring.ability.child;

import net.ximatai.muyun.spring.ability.AbilityException;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class StaticChildResolver {
    private static final Map<Class<?>, List<ChildRule>> RULES = new ConcurrentHashMap<>();

    private StaticChildResolver() {
    }

    public static List<ChildPlan> plans(Class<?> parentModelClass) {
        return rules(parentModelClass).stream()
                .map(ChildRule::plan)
                .toList();
    }

    public static ChildPlan singlePlan(Class<?> parentModelClass) {
        List<ChildPlan> plans = plans(parentModelClass);
        if (plans.size() != 1) {
            throw new AbilityException("expected exactly one child relation plan: " + parentModelClass.getName());
        }
        return plans.getFirst();
    }

    public static List<ChildRule> rules(Class<?> parentModelClass) {
        if (parentModelClass == null) {
            return List.of();
        }
        return RULES.computeIfAbsent(parentModelClass, StaticChildResolver::loadRules);
    }

    static void clearCacheForTests() {
        RULES.clear();
    }

    private static List<ChildRule> loadRules(Class<?> parentModelClass) {
        LinkedHashMap<String, ChildRule> rules = new LinkedHashMap<>();
        LinkedHashMap<String, Field> relationCodeFields = new LinkedHashMap<>();
        Class<?> current = parentModelClass;
        while (current != null && !Object.class.equals(current)) {
            for (Field field : current.getDeclaredFields()) {
                ChildRef childRef = field.getAnnotation(ChildRef.class);
                if (childRef == null) {
                    continue;
                }
                validateListField(parentModelClass, field, childRef);
                try {
                    field.setAccessible(true);
                } catch (RuntimeException e) {
                    throw new AbilityException("cannot access child relation field: "
                            + parentModelClass.getName() + "." + field.getName(), e);
                }
                String relationCode = childRef.relationCode().isBlank() ? field.getName() : childRef.relationCode();
                String childEntity = childRef.childEntity().isBlank()
                        ? defaultEntityCode(childRef.childModel())
                        : childRef.childEntity();
                ChildPlan plan = new ChildPlan(
                        relationCode,
                        childRef.parentEntity(),
                        childEntity,
                        childRef.childForeignKeyField(),
                        childRef.autoPopulate(),
                        childRef.autoDeleteWithParent()
                );
                Field previous = relationCodeFields.putIfAbsent(plan.relationCode(), field);
                if (previous != null) {
                    throw new AbilityException("duplicate child relationCode: "
                            + parentModelClass.getName() + "." + plan.relationCode());
                }
                rules.putIfAbsent(field.getName(), new ChildRule(field, plan, childRef.childModel()));
            }
            current = current.getSuperclass();
        }
        return List.copyOf(rules.values());
    }

    private static void validateListField(Class<?> parentModelClass, Field field, ChildRef childRef) {
        if (!List.class.isAssignableFrom(field.getType())) {
            throw new AbilityException("@ChildRef field must be List: " + parentModelClass.getName() + "." + field.getName());
        }
        Type genericType = field.getGenericType();
        if (!(genericType instanceof ParameterizedType parameterizedType)) {
            throw new AbilityException("@ChildRef field must declare child generic type: "
                    + parentModelClass.getName() + "." + field.getName());
        }
        Type actualType = parameterizedType.getActualTypeArguments()[0];
        if (!(actualType instanceof Class<?> actualClass) || !childRef.childModel().equals(actualClass)) {
            throw new AbilityException("@ChildRef childModel not assignable to field generic type: "
                    + parentModelClass.getName() + "." + field.getName());
        }
    }

    private static String defaultEntityCode(Class<? extends EntityContract> childModel) {
        String simpleName = childModel.getSimpleName();
        if (simpleName.isBlank()) {
            throw new AbilityException("child model simple name must not be blank");
        }
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }

    public record ChildRule(Field field, ChildPlan plan, Class<? extends EntityContract> childModel) {
    }
}
