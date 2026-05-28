package net.ximatai.muyun.spring.ability;


final class DemoPlainRecordService implements CrudAbility<DemoPlainRecord> {
    private final InMemoryBaseDao<DemoPlainRecord> dao = new InMemoryBaseDao<>();

    @Override
    public BaseDao<DemoPlainRecord, String> getDao() {
        return dao;
    }

    InMemoryBaseDao<DemoPlainRecord> rawDao() {
        return dao;
    }

    @Override
    public String getModuleAlias() {
        return "demo.plainRecord";
    }
}
