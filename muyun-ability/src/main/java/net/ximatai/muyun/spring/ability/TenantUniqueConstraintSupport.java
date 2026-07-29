package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.common.exception.ErrorScope;
import net.ximatai.muyun.spring.common.exception.PlatformErrorCodes;
import net.ximatai.muyun.spring.common.exception.PlatformErrors;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.model.constraint.StaticTenantUniqueConstraints;
import net.ximatai.muyun.spring.common.model.constraint.TenantUniqueConstraintDefinition;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class TenantUniqueConstraintSupport {
    private TenantUniqueConstraintSupport() {
    }

    static <T extends EntityContract> void validate(CrudAbility<T> ability, T entity) {
        if (ability == null || entity == null) {
            return;
        }
        constraints(ability).forEach(constraint -> validate(ability, entity, constraint));
    }

    static <T extends EntityContract> RuntimeException translatePersistFailure(CrudAbility<T> ability,
                                                                                RuntimeException failure) {
        if (!isDatabaseUniqueViolation(failure)) {
            return failure;
        }
        List<TenantUniqueConstraintDefinition> constraints = constraints(ability);
        if (constraints.isEmpty()) {
            return failure;
        }
        TenantUniqueConstraintDefinition constraint = constraints.size() == 1 ? constraints.getFirst() : null;
        return tenantUniqueConflict(ability, constraint, failure);
    }

    private static <T extends EntityContract> void validate(CrudAbility<T> ability,
                                                             T entity,
                                                             TenantUniqueConstraintDefinition constraint) {
        Criteria criteria = Criteria.of();
        if (entity.getTenantId() != null && !entity.getTenantId().isBlank()) {
            criteria.eq(StandardEntitySchema.TENANT_ID_FIELD, entity.getTenantId());
        }
        for (String field : constraint.fieldNames()) {
            Object value = value(ability, entity, field);
            if (value == null) {
                return;
            }
            criteria.eq(field, value);
        }
        boolean duplicated = ability.getDao().query(criteria, PageRequest.of(1, 2)).stream()
                .anyMatch(existing -> !Objects.equals(existing.getId(), entity.getId()));
        if (duplicated) {
            throw tenantUniqueConflict(ability, constraint, null);
        }
    }

    private static <T extends EntityContract> PlatformException tenantUniqueConflict(CrudAbility<T> ability,
                                                                                        TenantUniqueConstraintDefinition constraint,
                                                                                        Throwable cause) {
        String message = constraint == null ? "tenant unique constraint violated" : constraint.violationMessage();
        Map<String, Object> details = constraint == null
                ? Map.of("moduleAlias", ability.getModuleAlias())
                : Map.of("moduleAlias", ability.getModuleAlias(), "fields", constraint.fieldNames());
        ErrorScope scope = ErrorScope.module(ability.getModuleAlias());
        return cause == null
                ? PlatformErrors.conflict(PlatformErrorCodes.CONFLICT_UNIQUE, message, scope, details)
                : PlatformErrors.conflict(PlatformErrorCodes.CONFLICT_UNIQUE, message, cause, scope, details);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T extends EntityContract> List<TenantUniqueConstraintDefinition> constraints(CrudAbility<T> ability) {
        if (ability instanceof TenantUniqueConstraintProvider provider) {
            return provider.tenantUniqueConstraints();
        }
        return StaticTenantUniqueConstraints.resolve(ability.modelClass());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T extends EntityContract> Object value(CrudAbility<T> ability, T entity, String fieldName) {
        if (ability instanceof TenantUniqueConstraintProvider provider) {
            return provider.tenantUniqueConstraintValue(entity, fieldName);
        }
        Class<?> type = entity.getClass();
        while (type != null && type != Object.class) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(entity);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("cannot read tenant unique constraint field: " + fieldName, exception);
            }
        }
        throw new IllegalArgumentException("unknown tenant unique constraint field: "
                + entity.getClass().getName() + "." + fieldName);
    }

    private static boolean isDatabaseUniqueViolation(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof SQLException sqlException && "23505".equals(sqlException.getSQLState())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
