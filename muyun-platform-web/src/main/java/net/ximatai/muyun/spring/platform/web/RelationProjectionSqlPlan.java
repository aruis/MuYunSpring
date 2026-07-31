package net.ximatai.muyun.spring.platform.web;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record RelationProjectionSqlPlan(String baseSql,
                                        Map<String, Object> baseParams,
                                        Set<String> queryableFields,
                                        Set<String> sortableFields,
                                        Set<String> responseFields,
                                        List<ViewFieldRef> relationOutputFields,
                                        net.ximatai.muyun.database.core.metadata.DBInfo.Type databaseType,
                                        ProjectionGraph projectionGraph) {
    public RelationProjectionSqlPlan(String baseSql,
                                     Map<String, Object> baseParams,
                                     Set<String> queryableFields,
                                     Set<String> responseFields,
                                     List<ViewFieldRef> relationOutputFields,
                                     net.ximatai.muyun.database.core.metadata.DBInfo.Type databaseType) {
        this(baseSql, baseParams, queryableFields, queryableFields, responseFields, relationOutputFields, databaseType,
                null);
    }

    public RelationProjectionSqlPlan(String baseSql,
                                     Map<String, Object> baseParams,
                                     Set<String> queryableFields,
                                     Set<String> sortableFields,
                                     Set<String> responseFields,
                                     List<ViewFieldRef> relationOutputFields,
                                     net.ximatai.muyun.database.core.metadata.DBInfo.Type databaseType) {
        this(baseSql, baseParams, queryableFields, sortableFields, responseFields, relationOutputFields, databaseType,
                null);
    }

    public RelationProjectionSqlPlan {
        if (baseSql == null || baseSql.isBlank()) {
            throw new IllegalArgumentException("projection SQL base query must not be blank");
        }
        baseParams = baseParams == null ? Map.of() : Map.copyOf(baseParams);
        queryableFields = queryableFields == null ? Set.of() : Set.copyOf(queryableFields);
        sortableFields = sortableFields == null ? Set.of() : Set.copyOf(sortableFields);
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
