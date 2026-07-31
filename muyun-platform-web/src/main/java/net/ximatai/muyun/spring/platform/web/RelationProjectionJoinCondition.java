package net.ximatai.muyun.spring.platform.web;

public record RelationProjectionJoinCondition(String leftAlias,
                                              String leftColumn,
                                              String rightAlias,
                                              String rightColumn) {
    public RelationProjectionJoinCondition {
        leftAlias = RelationProjectionSqlNames.requireAlias(leftAlias, "leftAlias");
        leftColumn = RelationProjectionSqlNames.requireColumn(leftColumn, "leftColumn");
        rightAlias = RelationProjectionSqlNames.requireAlias(rightAlias, "rightAlias");
        rightColumn = RelationProjectionSqlNames.requireColumn(rightColumn, "rightColumn");
    }
}
