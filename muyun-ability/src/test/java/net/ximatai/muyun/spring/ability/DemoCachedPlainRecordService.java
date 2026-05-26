package net.ximatai.muyun.spring.ability;

final class DemoCachedPlainRecordService implements
        CrudAbility<DemoPlainRecord>,
        SoftDeleteAbility<DemoPlainRecord>,
        CacheAbility<DemoPlainRecord> {
    private final InMemoryBaseDao<DemoPlainRecord> dao = new InMemoryBaseDao<>();
    private int afterChangedCount;

    @Override
    public BaseDao<DemoPlainRecord, String> getDao() {
        return dao;
    }

    @Override
    public String getModuleAlias() {
        return "demo.cachedPlainRecord";
    }

    @Override
    public DemoPlainRecord copyForCache(DemoPlainRecord entity) {
        if (entity == null) {
            return null;
        }
        DemoPlainRecord copy = new DemoPlainRecord(entity.getTitle());
        copy.setId(entity.getId());
        copy.setTenantId(entity.getTenantId());
        copy.setVersion(entity.getVersion());
        copy.setDeleted(entity.getDeleted());
        copy.setCreatedBy(entity.getCreatedBy());
        copy.setCreatedAt(entity.getCreatedAt());
        copy.setUpdatedBy(entity.getUpdatedBy());
        copy.setUpdatedAt(entity.getUpdatedAt());
        return copy;
    }

    InMemoryBaseDao<DemoPlainRecord> rawDao() {
        return dao;
    }

    @Override
    public void afterChanged(DemoPlainRecord entity) {
        afterChangedCount++;
    }

    int afterChangedCount() {
        return afterChangedCount;
    }
}
