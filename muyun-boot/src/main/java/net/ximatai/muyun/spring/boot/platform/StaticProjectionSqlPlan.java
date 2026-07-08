package net.ximatai.muyun.spring.boot.platform;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record StaticProjectionSqlPlan(String baseSql,
                                      Map<String, Object> baseParams,
                                      Set<String> projectedFields,
                                      List<ViewFieldRef> relationOutputFields) {
    public StaticProjectionSqlPlan {
        if (baseSql == null || baseSql.isBlank()) {
            throw new IllegalArgumentException("projection SQL base query must not be blank");
        }
        baseParams = baseParams == null ? Map.of() : Map.copyOf(baseParams);
        projectedFields = projectedFields == null ? Set.of() : Set.copyOf(projectedFields);
        relationOutputFields = relationOutputFields == null ? List.of() : List.copyOf(relationOutputFields);
    }

    public boolean hasRelationProjection() {
        return !relationOutputFields.isEmpty();
    }
}
