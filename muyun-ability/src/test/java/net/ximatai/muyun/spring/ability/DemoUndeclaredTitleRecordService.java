package net.ximatai.muyun.spring.ability;

final class DemoUndeclaredTitleRecordService implements
        CrudAbility<DemoUndeclaredTitleRecord>,
        ReferenceAbility<DemoUndeclaredTitleRecord> {
    private final InMemoryBaseDao<DemoUndeclaredTitleRecord> dao = new InMemoryBaseDao<>();

    @Override
    public BaseDao<DemoUndeclaredTitleRecord, String> getDao() {
        return dao;
    }

    @Override
    public String getModuleAlias() {
        return "demo.undeclaredTitleRecord";
    }
}
