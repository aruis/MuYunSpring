package net.ximatai.muyun.spring.common.model;

public interface TreeCapable extends SortCapable {
    String getParentId();

    void setParentId(String parentId);
}
