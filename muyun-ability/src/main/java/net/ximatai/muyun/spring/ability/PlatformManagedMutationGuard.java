package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.model.capability.PlatformManagedCapable;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

final class PlatformManagedMutationGuard {
    private static final String CREATE_MESSAGE = "ordinary mutation cannot create platform-managed record";
    private static final String MARK_MESSAGE = "ordinary mutation cannot mark record as platform-managed";
    private static final String UPDATE_MESSAGE = "ordinary mutation cannot update platform-managed record";
    private static final String DELETE_MESSAGE = "ordinary mutation cannot delete platform-managed record";
    private static final Set<String> TECHNICAL_FIELDS = Set.of(
            "id",
            "tenantId",
            "version",
            "deleted",
            "deletedAt",
            "createdBy",
            "createdAt",
            "updatedBy",
            "updatedAt"
    );
    private static final Set<String> PROTECTED_TECHNICAL_FIELDS = Set.of(
            "tenantId",
            "deleted",
            "deletedAt"
    );

    private PlatformManagedMutationGuard() {
    }

    @SuppressWarnings("unchecked")
    static <T extends EntityContract> void beforeInsert(CrudAbility<T> ability, T entity) {
        if (!(ability instanceof PlatformManagedProtectionAbility<?> platformManagedAbility)
                || PlatformManagedMutationContext.isPlatformManagedMutation()) {
            return;
        }
        PlatformManagedProtectionAbility typed = (PlatformManagedProtectionAbility) platformManagedAbility;
        if (isPlatformManaged(entity) && !typed.allowOrdinaryPlatformManagedInsert(entity)) {
            throw new PlatformException(CREATE_MESSAGE);
        }
    }

    @SuppressWarnings("unchecked")
    static <T extends EntityContract> UpdateDecision<T> beforeUpdate(CrudAbility<T> ability, T entity, T existing) {
        if (!(ability instanceof PlatformManagedProtectionAbility<?> platformManagedAbility)
                || PlatformManagedMutationContext.isPlatformManagedMutation()) {
            return UpdateDecision.standard();
        }
        if (existing == null) {
            return UpdateDecision.standard();
        }
        if (isPlatformManaged(existing)) {
            if (!changedProtectedTechnicalFields(entity, existing).isEmpty()) {
                throw new PlatformException(UPDATE_MESSAGE);
            }
            Set<String> changedFields = changedBusinessFields(entity, existing);
            if (!isAllowedUpdate(platformManagedAbility, changedFields)) {
                throw new PlatformException(UPDATE_MESSAGE);
            }
            return UpdateDecision.lightweight(permittedUpdateRecord(entity, existing, changedFields));
        }
        if (!isPlatformManaged(existing) && isPlatformManaged(entity)) {
            throw new PlatformException(MARK_MESSAGE);
        }
        return UpdateDecision.standard();
    }

    static <T extends EntityContract> void beforeDelete(CrudAbility<T> ability, T entity) {
        if (!(ability instanceof PlatformManagedProtectionAbility<?>)
                || PlatformManagedMutationContext.isPlatformManagedMutation()) {
            return;
        }
        if (isPlatformManaged(entity)) {
            throw new PlatformException(DELETE_MESSAGE);
        }
    }

    private static boolean isAllowedUpdate(PlatformManagedProtectionAbility<?> ability, Set<String> changedFields) {
        if (changedFields.contains("systemManaged")) {
            return false;
        }
        Set<String> allowedFields = new LinkedHashSet<>(ability.editablePlatformManagedFields());
        allowedFields.remove("systemManaged");
        return allowedFields.containsAll(changedFields);
    }

    private static <T extends EntityContract> Set<String> changedBusinessFields(T incoming, T existing) {
        Set<String> fields = new LinkedHashSet<>();
        if (incoming == null) {
            return fields;
        }
        Class<?> type = incoming.getClass();
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                if (shouldSkipField(field)) {
                    continue;
                }
                Object incomingValue = fieldValue(field, incoming);
                if (incomingValue == null) {
                    continue;
                }
                Object existingValue = fieldValue(field, existing);
                if ("systemManaged".equals(field.getName())
                        && Boolean.TRUE.equals(existingValue)
                        && Boolean.FALSE.equals(incomingValue)) {
                    continue;
                }
                if (!Objects.equals(incomingValue, existingValue)) {
                    fields.add(field.getName());
                }
            }
            type = type.getSuperclass();
        }
        return fields;
    }

    private static <T extends EntityContract> Set<String> changedProtectedTechnicalFields(T incoming, T existing) {
        Set<String> fields = new LinkedHashSet<>();
        if (incoming == null) {
            return fields;
        }
        for (String fieldName : PROTECTED_TECHNICAL_FIELDS) {
            Field field = findField(incoming.getClass(), fieldName);
            if (field == null) {
                continue;
            }
            Object incomingValue = fieldValue(field, incoming);
            if (incomingValue == null) {
                continue;
            }
            Object existingValue = fieldValue(field, existing);
            if (!Objects.equals(incomingValue, existingValue)) {
                fields.add(fieldName);
            }
        }
        return fields;
    }

    private static boolean shouldSkipField(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isStatic(modifiers)
                || Modifier.isTransient(modifiers)
                || TECHNICAL_FIELDS.contains(field.getName());
    }

    private static Object fieldValue(Field field, Object target) {
        try {
            field.setAccessible(true);
            return field.get(target);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Cannot read platform-managed field: " + field.getName(), ex);
        }
    }

    private static void copyFieldIfExists(Object source, Object target, String fieldName) {
        Field field = findField(source.getClass(), fieldName);
        if (field == null) {
            return;
        }
        try {
            field.setAccessible(true);
            field.set(target, field.get(source));
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Cannot copy platform-managed field: " + fieldName, ex);
        }
    }

    private static Field findField(Class<?> sourceType, String fieldName) {
        Class<?> type = sourceType;
        while (type != null && type != Object.class) {
            try {
                return type.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        return null;
    }

    private static boolean isPlatformManaged(Object entity) {
        return entity instanceof PlatformManagedCapable capable
                && Boolean.TRUE.equals(capable.getSystemManaged());
    }

    private static <T extends EntityContract> T permittedUpdateRecord(T entity,
                                                                      T existing,
                                                                      Collection<String> changedFields) {
        T permitted = newInstance(existing);
        copyFields(existing, permitted);
        permitted.setId(entity.getId());
        permitted.setTenantId(existing.getTenantId());
        permitted.setVersion(entity.getVersion());
        permitted.setUpdatedAt(entity.getUpdatedAt());
        permitted.setUpdatedBy(entity.getUpdatedBy());
        for (String field : changedFields) {
            copyFieldIfExists(entity, permitted, field);
        }
        return permitted;
    }

    private static void copyFields(Object source, Object target) {
        Class<?> type = source.getClass();
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    field.set(target, field.get(source));
                } catch (IllegalAccessException ex) {
                    throw new IllegalStateException("Cannot copy platform-managed record field: "
                            + field.getName(), ex);
                }
            }
            type = type.getSuperclass();
        }
    }

    private static <T extends EntityContract> T newInstance(T entity) {
        try {
            Constructor<?> constructor = entity.getClass().getDeclaredConstructor();
            constructor.setAccessible(true);
            @SuppressWarnings("unchecked")
            T created = (T) constructor.newInstance();
            return created;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Cannot create platform-managed lightweight update record: "
                    + entity.getClass().getName(), ex);
        }
    }

    record UpdateDecision<T extends EntityContract>(boolean lightweight, T record) {
        private static <T extends EntityContract> UpdateDecision<T> standard() {
            return new UpdateDecision<>(false, null);
        }

        private static <T extends EntityContract> UpdateDecision<T> lightweight(T record) {
            return new UpdateDecision<>(true, record);
        }
    }
}
