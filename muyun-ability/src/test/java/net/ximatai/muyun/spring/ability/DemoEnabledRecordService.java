package net.ximatai.muyun.spring.ability;


final class DemoEnabledRecordService implements
        CrudAbility<DemoEnabledRecord>,
        SoftDeleteAbility<DemoEnabledRecord>,
        EnableAbility<DemoEnabledRecord> {
    private final InMemoryBaseDao<DemoEnabledRecord> dao = new InMemoryBaseDao<>();

    @Override
    public BaseDao<DemoEnabledRecord, String> getDao() {
        return dao;
    }

    @Override
    public String getModuleAlias() {
        return "demo.enabledRecord";
    }
}
