package net.ximatai.muyun.spring.common.model.capability;

import net.ximatai.muyun.spring.common.model.contract.EntityContract;

/**
 * Capability contract for records that can be explicitly enabled or disabled.
 */
public interface EnabledCapable extends EntityContract {
    Boolean getEnabled();

    void setEnabled(Boolean enabled);
}
