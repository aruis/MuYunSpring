package net.ximatai.muyun.spring.common.platform;

public enum PlatformActionGroup {
    CRUD(EntityCapability.CRUD),
    SORT(EntityCapability.SORT),
    TREE(EntityCapability.TREE),
    REFERENCE(EntityCapability.REFERENCE),
    ENABLE(EntityCapability.ENABLE);

    private final EntityCapability capability;

    PlatformActionGroup(EntityCapability capability) {
        this.capability = capability;
    }

    public EntityCapability capability() {
        return capability;
    }
}
