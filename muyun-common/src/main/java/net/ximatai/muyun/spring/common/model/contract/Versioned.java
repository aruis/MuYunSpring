package net.ximatai.muyun.spring.common.model.contract;

/**
 * Marks records protected by optimistic version control.
 */
public interface Versioned {
    Integer getVersion();

    void setVersion(Integer version);
}
