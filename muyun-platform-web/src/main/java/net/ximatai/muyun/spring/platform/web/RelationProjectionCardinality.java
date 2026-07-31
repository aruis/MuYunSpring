package net.ximatai.muyun.spring.platform.web;

public enum RelationProjectionCardinality {
    ONE_TO_ONE,
    MANY_TO_ONE,
    ONE_TO_MANY,
    MANY_TO_MANY;

    public boolean safeForPageJoin() {
        return this == ONE_TO_ONE || this == MANY_TO_ONE;
    }
}
