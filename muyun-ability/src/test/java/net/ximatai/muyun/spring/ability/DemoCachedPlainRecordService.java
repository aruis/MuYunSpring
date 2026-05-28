package net.ximatai.muyun.spring.ability;


final class DemoCachedPlainRecordService extends AbstractAbilityService<DemoPlainRecord> implements
        SoftDeleteAbility<DemoPlainRecord>,
        CacheAbility<DemoPlainRecord> {
    private int afterChangedCount;

    DemoCachedPlainRecordService() {
        super("demo.cachedPlainRecord", new InMemoryBaseDao<>());
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
