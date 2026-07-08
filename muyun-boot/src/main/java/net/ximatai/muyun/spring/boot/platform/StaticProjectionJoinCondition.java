package net.ximatai.muyun.spring.boot.platform;

public record StaticProjectionJoinCondition(String leftAlias,
                                            String leftColumn,
                                            String rightAlias,
                                            String rightColumn) {
    public StaticProjectionJoinCondition {
        leftAlias = StaticProjectionSqlNames.requireAlias(leftAlias, "leftAlias");
        leftColumn = StaticProjectionSqlNames.requireColumn(leftColumn, "leftColumn");
        rightAlias = StaticProjectionSqlNames.requireAlias(rightAlias, "rightAlias");
        rightColumn = StaticProjectionSqlNames.requireColumn(rightColumn, "rightColumn");
    }
}
