package net.ximatai.muyun.spring.boot.platform;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record RelationProjectionSqlPlan(String baseSql,
                                        Map<String, Object> baseParams,
                                        Set<String> projectedFields,
                                        Set<String> responseFields,
                                        List<ViewFieldRef> relationOutputFields,
                                        net.ximatai.muyun.database.core.metadata.DBInfo.Type databaseType) {
    public RelationProjectionSqlPlan {
        if (baseSql == null || baseSql.isBlank()) {
            throw new IllegalArgumentException("projection SQL base query must not be blank");
        }
        baseParams = baseParams == null ? Map.of() : Map.copyOf(baseParams);
        projectedFields = projectedFields == null ? Set.of() : Set.copyOf(projectedFields);
        responseFields = responseFields == null ? Set.of() : Set.copyOf(responseFields);
        relationOutputFields = relationOutputFields == null ? List.of() : List.copyOf(relationOutputFields);
        databaseType = databaseType == null
                ? net.ximatai.muyun.database.core.metadata.DBInfo.Type.POSTGRESQL
                : databaseType;
    }

    public boolean hasRelationProjection() {
        return !relationOutputFields.isEmpty();
    }
}
