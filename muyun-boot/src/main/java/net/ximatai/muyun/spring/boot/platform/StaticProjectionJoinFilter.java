package net.ximatai.muyun.spring.boot.platform;

public record StaticProjectionJoinFilter(String tableAlias,
                                         String columnName,
                                         Object value) {
    public StaticProjectionJoinFilter {
        tableAlias = StaticProjectionSqlNames.requireAlias(tableAlias, "tableAlias");
        columnName = StaticProjectionSqlNames.requireColumn(columnName, "columnName");
    }
}
