package net.ximatai.muyun.spring.common.model.capability;

import net.ximatai.muyun.spring.common.model.contract.EntityContract;

/**
 * Capability contract for records that have a stable display title.
 */
public interface TitledCapable extends EntityContract {
    String getTitle();
}
