package net.ximatai.muyun.spring.common.model.constraint;

import java.util.Arrays;
import java.util.List;

public final class StaticTenantUniqueConstraints {
    private StaticTenantUniqueConstraints() {
    }

    public static List<TenantUniqueConstraintDefinition> resolve(Class<?> modelClass) {
        if (modelClass == null) {
            return List.of();
        }
        return Arrays.stream(modelClass.getAnnotationsByType(TenantUniqueConstraint.class))
                .map(annotation -> new TenantUniqueConstraintDefinition(List.of(annotation.fields()), annotation.message()))
                .toList();
    }
}
