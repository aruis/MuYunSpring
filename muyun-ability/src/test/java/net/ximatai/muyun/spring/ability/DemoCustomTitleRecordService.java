package net.ximatai.muyun.spring.ability;

final class DemoCustomTitleRecordService implements
        CrudAbility<DemoCustomTitleRecord>,
        ReferenceAbility<DemoCustomTitleRecord> {
    private final InMemoryBaseDao<DemoCustomTitleRecord> dao = new InMemoryBaseDao<>();

    @Override
    public BaseDao<DemoCustomTitleRecord, String> getDao() {
        return dao;
    }

    @Override
    public String getModuleAlias() {
        return "demo.customTitleRecord";
    }
}
