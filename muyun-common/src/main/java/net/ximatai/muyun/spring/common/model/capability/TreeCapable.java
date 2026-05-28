package net.ximatai.muyun.spring.common.model.capability;

/**
 * Capability contract for records arranged in a parent-child tree.
 */
public interface TreeCapable extends SortCapable {
    String getParentId();

    void setParentId(String parentId);
}
