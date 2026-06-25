package net.ximatai.muyun.spring.common.model.capability;

/**
 * Contract for records contributed and maintained by the platform.
 */
public interface PlatformManagedCapable {
    Boolean getSystemManaged();

    void setSystemManaged(Boolean systemManaged);
}
