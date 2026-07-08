package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.common.util.PlatformNameRules;

import java.util.List;

public record RelationProjectionJoinStep(String schemaName,
                                         String tableName,
                                         String tableAlias,
                                         List<RelationProjectionJoinCondition> conditions,
                                         List<RelationProjectionJoinFilter> filters) {
    public RelationProjectionJoinStep {
        schemaName = schemaName == null || schemaName.isBlank() ? "public"
                : PlatformNameRules.requireDatabaseName(schemaName, "schemaName");
        tableName = PlatformNameRules.requireDatabaseName(tableName, "tableName");
        tableAlias = PlatformNameRules.requireIdentifier(tableAlias, "tableAlias");
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        filters = filters == null ? List.of() : List.copyOf(filters);
        if (conditions.isEmpty()) {
            throw new IllegalArgumentException("projection join step conditions must not be empty: " + tableAlias);
        }
    }
}
