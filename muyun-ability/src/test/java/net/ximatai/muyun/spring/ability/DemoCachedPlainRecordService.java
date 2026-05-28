package net.ximatai.muyun.spring.ability;


final class DemoCachedPlainRecordService extends AbstractAbilityService<DemoPlainRecord> implements
        SoftDeleteAbility<DemoPlainRecord>,
        CacheAbility<DemoPlainRecord> {
    private int afterChangedCount;

    DemoCachedPlainRecordService() {
        super("demo.cachedPlainRecord", new InMemoryBaseDao<>());
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

    @SuppressWarnings("unchecked")
    InMemoryBaseDao<DemoPlainRecord> rawDao() {
        return (InMemoryBaseDao<DemoPlainRecord>) getDao();
    }

    @Override
    public void afterChanged(DemoPlainRecord entity) {
        afterChangedCount++;
    }

    int afterChangedCount() {
        return afterChangedCount;
    }
}
