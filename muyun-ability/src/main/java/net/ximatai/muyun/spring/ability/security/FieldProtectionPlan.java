package net.ximatai.muyun.spring.ability.security;

import java.util.List;

public record FieldProtectionPlan<T>(List<ProtectedFieldAccessor<T>> fields) {
    public FieldProtectionPlan {
        fields = fields == null ? List.of() : List.copyOf(fields);
    }

    public static <T> FieldProtectionPlan<T> empty() {
        return new FieldProtectionPlan<>(List.of());
    }

    public boolean isEmpty() {
        return fields.isEmpty();
    }
}
