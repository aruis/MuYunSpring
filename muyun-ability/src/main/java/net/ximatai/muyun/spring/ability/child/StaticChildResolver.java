package net.ximatai.muyun.spring.ability.child;

import net.ximatai.muyun.spring.common.exception.PlatformException;
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
        return singleRule(parentModelClass).plan();
    }

    public static ChildRule singleRule(Class<?> parentModelClass) {
        if (parentModelClass == null) {
            throw new PlatformException("child parentModelClass must not be null");
        }
        List<ChildRule> rules = rules(parentModelClass);
        if (rules.isEmpty()) {
            throw new PlatformException("expected exactly one child relation plan: "
                    + parentModelClass.getName()
                    + ", actual relationCodes: [], add @ChildRef or use explicit childRelation(...)");
        }
        if (rules.size() != 1) {
            throw new PlatformException("expected exactly one child relation plan: "
                    + parentModelClass.getName()
                    + ", actual relationCodes: "
                    + rules.stream().map(rule -> rule.plan().relationCode()).toList()
                    + ", use childRelation(relationCode, ...) or childRelation(Class, relationCode, ...)");
        }
        return rules.getFirst();
    }

    public static ChildPlan plan(Class<?> parentModelClass, String relationCode) {
        return rule(parentModelClass, relationCode).plan();
    }

    public static ChildRule rule(Class<?> parentModelClass, String relationCode) {
        if (parentModelClass == null) {
            throw new PlatformException("child parentModelClass must not be null");
        }
        if (relationCode == null || relationCode.isBlank()) {
            throw new PlatformException("child relationCode must not be blank");
        }
        return rules(parentModelClass).stream()
                .filter(rule -> relationCode.equals(rule.plan().relationCode()))
                .findFirst()
                .orElseThrow(() -> new PlatformException("unknown child relationCode: "
                        + parentModelClass.getName() + "." + relationCode));
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
                    throw new PlatformException("cannot access child relation field: "
                            + parentModelClass.getName() + "." + field.getName(), e);
                }
                String relationCode = childRef.relationCode().isBlank() ? field.getName() : childRef.relationCode();
                String childEntity = childRef.childEntity().isBlank()
                        ? defaultEntityCode(childRef.childModel())
                        : childRef.childEntity();
                Field childForeignKeyField = validateChildForeignKeyField(
                        parentModelClass,
                        field,
                        childRef.childModel(),
                        childRef.childForeignKeyField()
                );
                String parentEntity = childRef.parentEntity().isBlank()
                        ? defaultEntityCode(parentModelClass)
                        : childRef.parentEntity();
                ChildPlan plan = new ChildPlan(
                        relationCode,
                        parentEntity,
                        childEntity,
                        childRef.childForeignKeyField(),
                        childRef.autoPopulate(),
                        childRef.autoDeleteWithParent()
                );
                Field previous = relationCodeFields.putIfAbsent(plan.relationCode(), field);
                if (previous != null) {
                    throw new PlatformException("duplicate child relationCode: "
                            + parentModelClass.getName() + "." + plan.relationCode());
                }
                rules.putIfAbsent(field.getName(), new ChildRule(field, plan, childRef.childModel(), childForeignKeyField));
            }
            current = current.getSuperclass();
        }
        return List.copyOf(rules.values());
    }

    private static void validateListField(Class<?> parentModelClass, Field field, ChildRef childRef) {
        if (!List.class.isAssignableFrom(field.getType())) {
            throw new PlatformException("@ChildRef field must be List: " + parentModelClass.getName() + "." + field.getName());
        }
        Type genericType = field.getGenericType();
        if (!(genericType instanceof ParameterizedType parameterizedType)) {
            throw new PlatformException("@ChildRef field must declare child generic type: "
                    + parentModelClass.getName() + "." + field.getName());
        }
        Type actualType = parameterizedType.getActualTypeArguments()[0];
        if (!(actualType instanceof Class<?> actualClass) || !childRef.childModel().equals(actualClass)) {
            throw new PlatformException("@ChildRef childModel not assignable to field generic type: "
                    + parentModelClass.getName() + "." + field.getName());
        }
    }

    private static Field validateChildForeignKeyField(Class<?> parentModelClass,
                                                      Field relationField,
                                                      Class<? extends EntityContract> childModel,
                                                      String childForeignKeyField) {
        if (childForeignKeyField == null || childForeignKeyField.isBlank()) {
            throw new PlatformException("@ChildRef childForeignKeyField must not be blank: "
                    + parentModelClass.getName() + "." + relationField.getName());
        }
        Field field = childField(childModel, childForeignKeyField);
        if (!String.class.equals(field.getType())) {
            throw new PlatformException("@ChildRef childForeignKeyField must be String: "
                    + childModel.getName() + "." + childForeignKeyField);
        }
        try {
            field.setAccessible(true);
        } catch (RuntimeException e) {
            throw new PlatformException("cannot access child foreign key field: "
                    + childModel.getName() + "." + childForeignKeyField, e);
        }
        return field;
    }

    private static Field childField(Class<?> childClass, String fieldName) {
        Class<?> current = childClass;
        while (current != null && !Object.class.equals(current)) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new PlatformException("cannot find child foreign key field: "
                + childClass.getName() + "." + fieldName);
    }

    private static String defaultEntityCode(Class<?> modelClass) {
        String simpleName = modelClass.getSimpleName();
        if (simpleName.isBlank()) {
            throw new PlatformException("model simple name must not be blank");
        }
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }

    public record ChildRule(Field field,
                            ChildPlan plan,
                            Class<? extends EntityContract> childModel,
                            Field childForeignKeyField) {
        @SuppressWarnings("unchecked")
        public <P extends EntityContract, C extends EntityContract> List<C> children(P parent) {
            if (parent == null) {
                return null;
            }
            try {
                return (List<C>) field.get(parent);
            } catch (IllegalAccessException e) {
                throw new PlatformException("cannot read child relation field: "
                        + parent.getClass().getName() + "." + field.getName(), e);
            }
        }

        public <P extends EntityContract, C extends EntityContract> void populate(P parent, List<C> children) {
            if (parent == null) {
                return;
            }
            try {
                field.set(parent, children);
            } catch (IllegalAccessException e) {
                throw new PlatformException("cannot write child relation field: "
                        + parent.getClass().getName() + "." + field.getName(), e);
            } catch (IllegalArgumentException e) {
                throw new PlatformException("cannot write child relation field: "
                        + parent.getClass().getName() + "." + field.getName(), e);
            }
        }

        public <C extends EntityContract> void setParentId(C child, String parentId) {
            if (child == null) {
                return;
            }
            try {
                childForeignKeyField.set(child, parentId);
            } catch (IllegalAccessException e) {
                throw new PlatformException("cannot write child foreign key field: "
                        + child.getClass().getName() + "." + plan.childForeignKeyField(), e);
            } catch (IllegalArgumentException e) {
                throw new PlatformException("cannot write child foreign key field: "
                        + child.getClass().getName() + "." + plan.childForeignKeyField(), e);
            }
        }
    }
}
