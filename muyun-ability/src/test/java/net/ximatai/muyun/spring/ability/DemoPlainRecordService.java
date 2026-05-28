package net.ximatai.muyun.spring.ability;


final class DemoPlainRecordService extends AbstractAbilityService<DemoPlainRecord> {
    DemoPlainRecordService() {
        super("demo.plainRecord", DemoPlainRecord.class, new InMemoryBaseDao<>());
    }

    @SuppressWarnings("unchecked")
    InMemoryBaseDao<DemoPlainRecord> rawDao() {
        return (InMemoryBaseDao<DemoPlainRecord>) getDao();
    }

}
