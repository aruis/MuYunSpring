package net.ximatai.muyun.spring.ability;


final class DemoPlainRecordService extends AbstractAbilityService<DemoPlainRecord> {
    DemoPlainRecordService() {
        super("demo.plainRecord", new InMemoryBaseDao<>());
    }

    @SuppressWarnings("unchecked")
    InMemoryBaseDao<DemoPlainRecord> rawDao() {
        return (InMemoryBaseDao<DemoPlainRecord>) getDao();
    }

}
