package net.ximatai.muyun.spring.boot.platform;

public record RelationProjectionJoinFilter(String tableAlias,
                                           String columnName,
                                           Object value) {
    public RelationProjectionJoinFilter {
        tableAlias = RelationProjectionSqlNames.requireAlias(tableAlias, "tableAlias");
        columnName = RelationProjectionSqlNames.requireColumn(columnName, "columnName");
    }
}
