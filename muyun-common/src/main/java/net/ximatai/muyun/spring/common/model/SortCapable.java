package net.ximatai.muyun.spring.common.model;

public interface SortCapable extends EntityContract {
    Integer getSortOrder();

    void setSortOrder(Integer sortOrder);
}
