package net.ximatai.muyun.spring.common.model.constraint;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StaticTenantUniqueConstraints {
    private StaticTenantUniqueConstraints() {
    }

    public static List<TenantUniqueConstraintDefinition> resolve(Class<?> modelClass) {
        if (modelClass == null) {
            return List.of();
        }
        Map<List<String>, TenantUniqueConstraintDefinition> constraints = new LinkedHashMap<>();
        Arrays.stream(modelClass.getAnnotationsByType(TenantUniqueConstraint.class))
                .map(annotation -> new TenantUniqueConstraintDefinition(List.of(annotation.fields()), annotation.message()))
                .forEach(constraint -> constraints.put(constraint.fieldNames(), constraint));
        Class<?> type = modelClass;
        while (type != null && type != Object.class) {
            for (java.lang.reflect.Field field : type.getDeclaredFields()) {
                net.ximatai.muyun.database.core.annotation.Column column =
                        field.getAnnotation(net.ximatai.muyun.database.core.annotation.Column.class);
                net.ximatai.muyun.database.core.annotation.Indexed indexed =
                        field.getAnnotation(net.ximatai.muyun.database.core.annotation.Indexed.class);
                if ((column != null && column.unique()) || (indexed != null && indexed.unique())) {
                    constraints.putIfAbsent(List.of(field.getName()),
                            new TenantUniqueConstraintDefinition(List.of(field.getName()), ""));
                }
            }
            type = type.getSuperclass();
        }
        return List.copyOf(constraints.values());
    }
}
