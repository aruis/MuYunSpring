package net.ximatai.muyun.spring.common.model.capability;

import net.ximatai.muyun.spring.common.model.contract.EntityContract;

/**
 * Capability contract for records ordered inside a business scope.
 */
public interface SortCapable extends EntityContract {
    Integer getSortOrder();

    void setSortOrder(Integer sortOrder);
}
